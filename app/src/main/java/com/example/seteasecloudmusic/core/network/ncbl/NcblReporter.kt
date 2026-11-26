package com.example.seteasecloudmusic.core.network.ncbl

import android.content.Context
import com.example.seteasecloudmusic.core.auth.AuthCookieStore
import com.example.seteasecloudmusic.core.network.CookieSanitizer
import com.example.seteasecloudmusic.core.network.SmartDns
import com.example.seteasecloudmusic.core.network.local.NeteaseCryptoMode
import com.example.seteasecloudmusic.core.network.local.NeteaseRequestContextBuilder
import com.example.seteasecloudmusic.core.network.local.NeteaseTokenStore
import com.example.seteasecloudmusic.core.network.local.SharedPreferencesNeteaseTokenStore
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 网易云官方桌面客户端 NCBL v3 加密日志上报器（Zstd 压缩 + ChaCha20 流加密 + RSA-256 密钥封装 + PC 客户端签名）。
 *
 * 1. 按 NCBL v3 协议封装请求：纯 Kotlin 构造 RFC 8878 标准 Zstd Raw Frame（不依赖本地动态库或 Unsafe）。
 * 2. 使用 ChaCha20 对元数据与压缩载荷分片加密，并用 256-bit RSA 封装随机对称密钥。
 * 3. 直传网易云客户端日志接口 `https://clientlog3.music.163.com/api/clientlog/encrypt/upload?multiupload=true`，上报起播与听歌时长。
 */
@Singleton
class NcblReporter @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val random = SecureRandom()
    // NCBL v3 协议用于封装随机对称密钥 keyA 的 256-bit RSA 公钥模数与指数
    private val RSA_N = BigInteger("fd90bd466ff9bc8a3fec2fbcf263b90d5c564879fa5d7aab89b31c1d5cb4139d", 16)
    private val RSA_E = BigInteger.valueOf(65537)
    // ChaCha20 状态矩阵魔数常量（"expand 32-byte k"）
    private val SIGMA = intArrayOf(0x61707865, 0x3320646e, 0x79622d32, 0x6b206574)

    private val smartDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            // 日志上报域名直接回退全部预置网易云节点
            return SmartDns.systemLookupOrNull(hostname, "NCBL") ?: SmartDns.addressesFor(hostname)
        }
    }

    // 上报专用直连客户端（连接/读取/写入超时均为 15 秒）
    private val directClient = OkHttpClient.Builder()
        .dns(smartDns)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun rotl(x: Int, n: Int): Int = (x shl n) or (x ushr (32 - n))

    private fun quarterRound(s: IntArray, a: Int, b: Int, c: Int, d: Int) {
        s[a] += s[b]
        s[d] = rotl(s[d] xor s[a], 16)
        s[c] += s[d]
        s[b] = rotl(s[b] xor s[c], 12)
        s[a] += s[b]
        s[d] = rotl(s[d] xor s[a], 8)
        s[c] += s[d]
        s[b] = rotl(s[b] xor s[c], 7)
    }

    private fun chachaBlock(key: ByteArray, counter: Int, nonce: ByteArray): ByteArray {
        val state = IntArray(16)
        state[0] = SIGMA[0]
        state[1] = SIGMA[1]
        state[2] = SIGMA[2]
        state[3] = SIGMA[3]

        val keyBuf = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until 8) {
            state[4 + i] = keyBuf.getInt(i * 4)
        }
        state[12] = counter
        val nonceBuf = ByteBuffer.wrap(nonce).order(ByteOrder.LITTLE_ENDIAN)
        state[13] = nonceBuf.getInt(0)
        state[14] = nonceBuf.getInt(4)
        state[15] = nonceBuf.getInt(8)

        val work = state.clone()
        for (i in 0 until 10) {
            quarterRound(work, 0, 4, 8, 12)
            quarterRound(work, 1, 5, 9, 13)
            quarterRound(work, 2, 6, 10, 14)
            quarterRound(work, 3, 7, 11, 15)
            quarterRound(work, 0, 5, 10, 15)
            quarterRound(work, 1, 6, 11, 12)
            quarterRound(work, 2, 7, 8, 13)
            quarterRound(work, 3, 4, 9, 14)
        }

        val out = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until 16) {
            out.putInt(work[i] + state[i])
        }
        return out.array()
    }

    private fun chacha20Crypt(key: ByteArray, counter: Int, nonce: ByteArray, data: ByteArray): ByteArray {
        val out = ByteArray(data.size)
        var off = 0
        while (off < data.size) {
            val ks = chachaBlock(key, counter + (off ushr 6), nonce)
            val end = minOf(off + 64, data.size)
            for (i in off until end) {
                out[i] = (data[i].toInt() xor ks[i - off].toInt()).toByte()
            }
            off += 64
        }
        return out
    }

    private fun rsaWrap(keyA: ByteArray): ByteArray {
        val nInt = BigInteger(1, keyA)
        val wrapped = nInt.modPow(RSA_E, RSA_N)
        val raw = wrapped.toByteArray()
        val out = ByteArray(32)
        if (raw.size >= 32) {
            System.arraycopy(raw, raw.size - 32, out, 0, 32)
        } else {
            System.arraycopy(raw, 0, out, 32 - raw.size, raw.size)
        }
        return out
    }

    /**
     * 生成符合 RFC 8878 规范的 Zstandard (Zstd) 帧（Raw_Block 模式）。
     *
     * 结构：
     * 1. Magic: `0xFD2FB528`（Little Endian: `0x28, 0xB5, 0x2F, 0xFD`）
     * 2. Frame Header: `0x00, 0x00`（FCS_Flag=0, Single_Segment=0, 无字典, 1KB window）
     * 3. Block Header: 3 字节（Last_Block=1, Block_Type=0 Raw, Block_Size=len）
     * 4. Raw Bytes（服务端由标准 Zstd 解压器读取）
     */
    private fun compressBodyZstd(data: ByteArray): ByteArray {
        val magic = byteArrayOf(0x28.toByte(), 0xb5.toByte(), 0x2f.toByte(), 0xfd.toByte())
        val fhd = byteArrayOf(0x00.toByte(), 0x00.toByte())
        val val32 = 1 or (0 shl 1) or (data.size shl 3)
        val bhead = byteArrayOf(
            (val32 and 0xFF).toByte(),
            ((val32 ushr 8) and 0xFF).toByte(),
            ((val32 ushr 16) and 0xFF).toByte()
        )
        val bos = ByteArrayOutputStream(magic.size + fhd.size + bhead.size + data.size)
        bos.write(magic)
        bos.write(fhd)
        bos.write(bhead)
        bos.write(data)
        return bos.toByteArray()
    }

    private fun encryptNcbl(metaBytes: ByteArray, bodyBytes: ByteArray): ByteArray {
        val keyA = ByteArray(32)
        random.nextBytes(keyA)
        // 约束 keyA 首字节小于 RSA_N 首字节，确保 BigInteger(keyA) < RSA_N
        if ((keyA[0].toInt() and 0xFF) >= 0xa3) {
            keyA[0] = 0xa2.toByte()
        }
        val keyB = rsaWrap(keyA)

        val uuid = ByteArray(16)
        random.nextBytes(uuid)
        uuid[6] = ((uuid[6].toInt() and 0x0f) or 0x40).toByte()
        uuid[8] = ((uuid[8].toInt() and 0x3f) or 0x80).toByte()

        val nonce = uuid.copyOfRange(0, 12)
        val counter = ByteBuffer.wrap(uuid, 12, 4).order(ByteOrder.LITTLE_ENDIAN).int ushr 2
        // 起始分片序号（1000..40999）
        val baseSeq = random.nextInt(40000) + 1000

        val metaCipher = chacha20Crypt(keyB, counter, nonce, metaBytes)
        val metaBlockBuf = ByteBuffer.allocate(4 + metaCipher.size).order(ByteOrder.LITTLE_ENDIAN)
        metaBlockBuf.putShort(0x4343.toShort()) // 元数据块魔数标识
        metaBlockBuf.putShort(metaCipher.size.toShort())
        metaBlockBuf.put(metaCipher)
        val metaBlock = metaBlockBuf.array()

        // NCBL v3 固定头（70 字节）+ 加密元数据块总长度
        val headerLen = 70 + metaBlock.size
        val compressed = compressBodyZstd(bodyBytes)

        // 单个加密数据分片最大字节数（32KB）
        val maxFrame = 0x8000
        val frameList = mutableListOf<ByteArray>()
        var seq = baseSeq
        var off = 0
        do {
            val len = minOf(maxFrame, compressed.size - off)
            val slice = compressed.copyOfRange(off, off + len)
            val cipher = chacha20Crypt(keyA, counter, nonce, slice)
            val head = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
            head.putShort(cipher.size.toShort())
            head.putInt(seq)
            frameList.add(head.array())
            frameList.add(cipher)
            seq++
            off += len
        } while (off < compressed.size)

        val trailingBos = ByteArrayOutputStream()
        frameList.forEach { trailingBos.write(it) }
        val trailing = trailingBos.toByteArray()
        val frameCount = seq - baseSeq

        val header = ByteBuffer.allocate(70).order(ByteOrder.LITTLE_ENDIAN)
        header.put("NCBL".toByteArray(Charsets.US_ASCII))
        header.putInt(3) // NCBL_VERSION
        header.putShort(headerLen.toShort())
        header.put(uuid)
        header.put(keyB)
        header.putInt(baseSeq)
        header.putInt(baseSeq + frameCount - 1)
        header.putInt(trailing.size)

        val result = ByteArrayOutputStream()
        result.write(header.array())
        result.write(metaBlock)
        result.write(trailing)
        return result.toByteArray()
    }

    private val tokenStore: NeteaseTokenStore by lazy {
        SharedPreferencesNeteaseTokenStore.getInstance(context)
    }

    private fun getSavedCookie(): String {
        return AuthCookieStore.getCookie(context).orEmpty()
    }

    internal data class NcblContext(
        val token: String,
        val cleanCookieStr: String,
        val metaJson: String,
        val metaBytes: ByteArray,
        val now: Long,
        val ts: Int,
        val appver: String,
        val userAgent: String
    )

    private fun createNcblContext(): NcblContext? {
        return buildNcblContext(
            rawCookie = getSavedCookie(),
            tokenStore = tokenStore,
            now = System.currentTimeMillis()
        )
    }

    private fun doUploadRecord(ctx: NcblContext, action: String, recordData: JSONObject): Result<Unit> {
        val uploadUrl = "https://clientlog3.music.163.com/api/clientlog/encrypt/upload?multiupload=true"
        val recordBytes = "${ctx.ts}\u0001$action\u0001$recordData".toByteArray(Charsets.UTF_8)
        val payload = encryptNcbl(ctx.metaBytes, recordBytes)
        val fileName = "op_${random.nextInt(90000) + 10000}_0_${random.nextInt(Int.MAX_VALUE)}"

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, payload.toRequestBody("multipart/form-data".toMediaType()))
            .build()

        val request = Request.Builder()
            .url(uploadUrl)
            .post(body)
            .header("Referer", "https://music.163.com/di")
            .header("User-Agent", ctx.userAgent)
            .header("Cookie", ctx.cleanCookieStr)
            .build()

        return try {
            val response = directClient.newCall(request).execute()
            val respBody = response.body?.string() ?: ""
            android.util.Log.d("NCBL", ">>> NCBL $action response [${response.code}]: $respBody")
            if (response.isSuccessful && respBody.contains("successfiles")) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("NCBL $action error: $respBody"))
            }
        } catch (e: Exception) {
            android.util.Log.e("NCBL", ">>> NCBL $action exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 上报歌曲起播行为（PLV）。
     *
     * @param songId 歌曲 ID
     * @param totalDurationSeconds 歌曲总时长（秒；小于 30 秒时按 30 秒下限上报）
     * @param sourceId 来源歌单或专辑 ID（为空或非正数时回退为 [songId]）
     */
    suspend fun reportPlv(
        songId: Long,
        totalDurationSeconds: Int,
        sourceId: Long? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val ctx = createNcblContext() ?: return@withContext Result.failure(Exception("No valid cookie"))
        val srcIdStr = (sourceId?.takeIf { it > 0 } ?: songId).toString()
        // 上报总时长最小下限（秒）
        val totalSec = totalDurationSeconds.coerceAtLeast(30)

        val plvData = JSONObject().apply {
            put("mode", "circulation")
            put("download", 0)
            put("alg", "")
            put("status", "front")
            put("id", songId.toString())
            put("bitrate", 320)
            put("type", "song")
            put("is_listentogether", 0)
            put("source", "list")
            put("is_heart", 0)
            put("resource_ratio", "")
            put("resource_time", totalSec)
            put("musiceffect_id", "")
            put("app_mode", 2)
            put("bitrate_level", "exhigh")
            put("vipType", "")
            put("fee", 0)
            put("file", 4)
            put("rightSource", 0)
            put("sourceId", srcIdStr)
            put("sourcetype", "track")
            put("libra_abt", "")
            put("channel", "netease")
            put("curStartChannel", "")
        }

        doUploadRecord(ctx, "_plv", plvData)
    }

    /**
     * 上报歌曲播放结束或达标节点行为（PLD）。
     *
     * @param songId 歌曲 ID
     * @param playDurationSeconds 本次实际播放时长（秒；小于 30 秒时按 30 秒下限上报）
     * @param totalDurationSeconds 歌曲总时长（秒；非正数时回退为 [playTimeSec]）
     * @param sourceId 来源歌单或专辑 ID（为空或非正数时回退为 [songId]）
     * @param endReason 结束原因标识（如 `"interrupt"`、`"playend"`）
     */
    suspend fun reportPld(
        songId: Long,
        playDurationSeconds: Int,
        totalDurationSeconds: Int,
        sourceId: Long? = null,
        endReason: String = "interrupt"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val ctx = createNcblContext() ?: return@withContext Result.failure(Exception("No valid cookie"))
        val srcIdStr = (sourceId?.takeIf { it > 0 } ?: songId).toString()
        // 有效播放时长最小下限（秒）
        val playTimeSec = playDurationSeconds.coerceAtLeast(30)
        val totalSec = if (totalDurationSeconds > 0) totalDurationSeconds else playTimeSec

        val pldData = JSONObject().apply {
            put("mode", "circulation")
            put("download", 0)
            put("alg", "")
            put("status", "front")
            put("id", songId.toString())
            put("time", playTimeSec)
            put("type", "song")
            put("is_listentogether", 0)
            put("source", "list")
            put("is_heart", 0)
            put("realtime", playTimeSec)
            put("resource_ratio", "")
            put("resource_time", totalSec)
            put("musiceffect_id", "1001")
            put("app_mode", 1)
            put("lyriceffect", "default")
            put("displayMode", "classic")
            put("bitrate", 320)
            put("bitrate_level", "exhigh")
            put("vipType", "")
            put("fee", 0)
            put("file", 4)
            put("rightSource", 0)
            put("sourceId", srcIdStr)
            put("sourcetype", "track")
            put("end", endReason)
            put("libra_abt", "")
            put("channel", "netease")
            put("curStartChannel", "")
        }

        doUploadRecord(ctx, "_pld", pldData)
    }

    /**
     * 依次上报歌曲起播（PLV）与播放结束（PLD）记录。
     */
    suspend fun reportNcblPlayback(
        songId: Long,
        title: String,
        artist: String,
        playDurationSeconds: Int,
        totalDurationSeconds: Int,
        sourceId: Long? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val totalSec = if (totalDurationSeconds > 0) totalDurationSeconds else playDurationSeconds
        reportPlv(songId, totalSec, sourceId)
        reportPld(songId, playDurationSeconds, totalSec, sourceId, endReason = "interrupt")
    }

    companion object {
        /**
         * 构造与主网络栈（[NeteaseRequestContextBuilder] + [NeteaseTokenStore]）一致的 NCBL 上报上下文：
         * 复用本机持久化的 `deviceId`、`_ntes_nuid`、`_ntes_nnid`、`WNMCID`、`clientSign` 及 PC 端版本与 UA。
         */
        internal fun buildNcblContext(
            rawCookie: String?,
            tokenStore: NeteaseTokenStore,
            now: Long = System.currentTimeMillis()
        ): NcblContext? {
            if (rawCookie.isNullOrBlank()) return null
            val cookieMap = CookieSanitizer.parsePairs(rawCookie)
            val token = cookieMap["MUSIC_U"].orEmpty()
            if (token.isBlank()) return null

            val pcProfile = NeteaseRequestContextBuilder.OS_MAP.getValue("pc")
            val ts = (now / 1000L).toInt()
            val sessionId = cookieMap["JSESSIONID-WYYY"].orEmpty()
            val nmtid = cookieMap["NMTID"].orEmpty()
            val csrf = cookieMap["__csrf"].orEmpty()
            val deviceId = cookieMap["deviceId"]?.takeIf { it.isNotBlank() } ?: tokenStore.getOrCreateDeviceId()
            val ntesNuid = cookieMap["_ntes_nuid"]?.takeIf { it.isNotBlank() } ?: tokenStore.getOrCreateNtesNuid()
            val ntesNnid = cookieMap["_ntes_nnid"]?.takeIf { it.isNotBlank() } ?: tokenStore.getOrCreateNtesNnid(now)
            val wnmcid = cookieMap["WNMCID"]?.takeIf { it.isNotBlank() } ?: tokenStore.getOrCreateWnmcid(now)
            val clientSign = tokenStore.getOrCreateClientSign()
            val appver = pcProfile.appver
            val os = pcProfile.os
            val osver = pcProfile.osver
            val channel = pcProfile.channel
            val userAgent = NeteaseRequestContextBuilder.chooseUserAgent(NeteaseCryptoMode.API, "pc")

            val cookieParts = listOf(
                "JSESSIONID-WYYY=$sessionId",
                "MUSIC_U=$token",
                "NMTID=$nmtid",
                "WEVNSM=1.0.0",
                "WNMCID=$wnmcid",
                "__csrf=$csrf",
                "__remember_me=true",
                "_iuqxldmzr_=33",
                "_ntes_nnid=$ntesNnid",
                "_ntes_nuid=$ntesNuid",
                "appver=$appver",
                "channel=$channel",
                "clientSign=$clientSign",
                "deviceId=$deviceId",
                "mode=",
                "ntes_kaola_ad=1",
                "os=$os",
                "osver=$osver"
            )
            val cleanCookieStr = cookieParts.joinToString("; ")

            val meta = JsonObject().apply {
                addProperty("JSESSIONID-WYYY", sessionId)
                addProperty("MUSIC_U", token)
                addProperty("NMTID", nmtid)
                addProperty("WEVNSM", "1.0.0")
                addProperty("WNMCID", wnmcid)
                addProperty("__csrf", csrf)
                addProperty("_iuqxldmzr_", "33")
                addProperty("_ntes_nnid", ntesNnid)
                addProperty("_ntes_nuid", ntesNuid)
                addProperty("appver", appver)
                addProperty("channel", channel)
                addProperty("clientSign", clientSign)
                addProperty("deviceId", deviceId)
                addProperty("mode", "")
                addProperty("ntes_kaola_ad", "1")
                addProperty("os", os)
                addProperty("osver", osver)
            }
            val metaJson = meta.toString()

            return NcblContext(
                token = token,
                cleanCookieStr = cleanCookieStr,
                metaJson = metaJson,
                metaBytes = metaJson.toByteArray(Charsets.UTF_8),
                now = now,
                ts = ts,
                appver = appver,
                userAgent = userAgent
            )
        }
    }
}
