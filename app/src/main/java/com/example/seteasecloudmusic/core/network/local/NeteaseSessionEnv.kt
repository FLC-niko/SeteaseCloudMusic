package com.example.seteasecloudmusic.core.network.local

import android.content.Context
import com.example.seteasecloudmusic.core.network.CookieSanitizer
import com.google.gson.JsonObject
import java.net.URLEncoder
import java.security.SecureRandom
import java.util.concurrent.ThreadLocalRandom

internal interface NeteaseTokenStore {
    fun getOrCreateDeviceId(): String
    fun getAnonymousToken(): String?
    fun saveAnonymousToken(token: String)

    fun getOrCreateNtesNuid(): String = NeteaseRequestContextBuilder.generateNtesNuid()
    fun getOrCreateNtesNnid(nowMs: Long = System.currentTimeMillis()): String = "${getOrCreateNtesNuid()},$nowMs"
    fun getOrCreateWnmcid(nowMs: Long = System.currentTimeMillis()): String = NeteaseRequestContextBuilder.generateWnmcid(nowMs)
    fun getOrCreateClientSign(): String = SharedPreferencesNeteaseTokenStore.generateClientSign()
    fun getPreLoginCookies(): Map<String, String> = emptyMap()
    fun mergePreLoginCookies(setCookies: List<String>) {}
    fun clearPreLoginCookies() {}
}

internal class SharedPreferencesNeteaseTokenStore(
    private val context: Context
) : NeteaseTokenStore {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Volatile
    private var cachedDeviceId: String? = null

    @Volatile
    private var cachedAnonymousToken: String? = null

    @Volatile
    private var cachedNtesNuid: String? = null

    @Volatile
    private var cachedNtesNnid: String? = null

    @Volatile
    private var cachedWnmcid: String? = null

    @Volatile
    private var cachedClientSign: String? = null

    private val preLoginCookieLock = Any()
    private val preLoginCookies = LinkedHashMap<String, String>()

    override fun getOrCreateDeviceId(): String {
        cachedDeviceId?.let { return it }
        synchronized(this) {
            cachedDeviceId?.let { return it }
            val existing = prefs.getString(KEY_DEVICE_ID, null)?.takeIf { it.isNotBlank() }
            if (existing != null) {
                cachedDeviceId = existing
                return existing
            }
            val generated = generateDeviceId()
            prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
            cachedDeviceId = generated
            return generated
        }
    }

    override fun getOrCreateNtesNuid(): String {
        cachedNtesNuid?.let { return it }
        synchronized(this) {
            cachedNtesNuid?.let { return it }
            val existing = prefs.getString(KEY_NTES_NUID, null)?.takeIf { it.isNotBlank() }
            if (existing != null) {
                cachedNtesNuid = existing
                return existing
            }
            val generated = NeteaseRequestContextBuilder.generateNtesNuid()
            prefs.edit().putString(KEY_NTES_NUID, generated).apply()
            cachedNtesNuid = generated
            return generated
        }
    }

    override fun getOrCreateNtesNnid(nowMs: Long): String {
        cachedNtesNnid?.let { return it }
        synchronized(this) {
            cachedNtesNnid?.let { return it }
            val existing = prefs.getString(KEY_NTES_NNID, null)?.takeIf { it.isNotBlank() }
            if (existing != null) {
                cachedNtesNnid = existing
                return existing
            }
            val generated = "${getOrCreateNtesNuid()},$nowMs"
            prefs.edit().putString(KEY_NTES_NNID, generated).apply()
            cachedNtesNnid = generated
            return generated
        }
    }

    override fun getOrCreateWnmcid(nowMs: Long): String {
        cachedWnmcid?.let { return it }
        synchronized(this) {
            cachedWnmcid?.let { return it }
            val existing = prefs.getString(KEY_WNMCID, null)?.takeIf { it.isNotBlank() }
            if (existing != null) {
                cachedWnmcid = existing
                return existing
            }
            val generated = NeteaseRequestContextBuilder.generateWnmcid(nowMs)
            prefs.edit().putString(KEY_WNMCID, generated).apply()
            cachedWnmcid = generated
            return generated
        }
    }

    override fun getOrCreateClientSign(): String {
        cachedClientSign?.let { return it }
        synchronized(this) {
            cachedClientSign?.let { return it }
            val existing = prefs.getString(KEY_CLIENT_SIGN, null)?.takeIf { it.isNotBlank() }
            if (existing != null) {
                cachedClientSign = existing
                return existing
            }
            val generated = generateClientSign()
            prefs.edit().putString(KEY_CLIENT_SIGN, generated).apply()
            cachedClientSign = generated
            return generated
        }
    }

    override fun getAnonymousToken(): String? {
        cachedAnonymousToken?.let { return it }
        val stored = prefs.getString(KEY_ANONYMOUS_TOKEN, null)?.takeIf { it.isNotBlank() }
        if (stored != null) {
            cachedAnonymousToken = stored
        }
        return stored
    }

    override fun saveAnonymousToken(token: String) {
        val clean = token.trim()
        if (clean.isEmpty()) return
        cachedAnonymousToken = clean
        prefs.edit().putString(KEY_ANONYMOUS_TOKEN, clean).apply()
    }

    override fun getPreLoginCookies(): Map<String, String> = synchronized(preLoginCookieLock) {
        LinkedHashMap(preLoginCookies)
    }

    override fun mergePreLoginCookies(setCookies: List<String>) {
        if (setCookies.isEmpty()) return
        synchronized(preLoginCookieLock) {
            for (line in setCookies) {
                val parsed = CookieSanitizer.parsePairs(line)
                for ((k, v) in parsed) {
                    if (k != "MUSIC_U" && v.isNotBlank()) {
                        preLoginCookies[k] = v
                    }
                }
            }
        }
    }

    override fun clearPreLoginCookies() {
        synchronized(preLoginCookieLock) {
            preLoginCookies.clear()
        }
    }

    companion object {
        private const val PREFS_NAME = "netease_local_session"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_ANONYMOUS_TOKEN = "anonymous_token"
        private const val KEY_NTES_NUID = "ntes_nuid"
        private const val KEY_NTES_NNID = "ntes_nnid"
        private const val KEY_WNMCID = "wnmcid"
        private const val KEY_CLIENT_SIGN = "client_sign"

        @Volatile
        private var INSTANCE: SharedPreferencesNeteaseTokenStore? = null

        /**
         * 获取进程内共享的设备身份存储单例，确保主网络栈与 NCBL 听歌上报复用同一套设备标识。
         */
        fun getInstance(context: Context): SharedPreferencesNeteaseTokenStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SharedPreferencesNeteaseTokenStore(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        internal fun generateDeviceId(): String {
            val hexChars = "0123456789ABCDEF"
            val rnd = SecureRandom()
            val out = CharArray(52)
            for (i in out.indices) {
                out[i] = hexChars[rnd.nextInt(hexChars.length)]
            }
            return String(out)
        }

        /**
         * 为当前设备生成唯一且持久化的桌面端硬件指纹（`clientSign`），避免所有用户共用固定指纹。
         */
        internal fun generateClientSign(): String {
            val rnd = SecureRandom()
            val macBytes = ByteArray(6).also(rnd::nextBytes)
            // 置为本地管理单播 MAC 位（unicast + locally administered）
            macBytes[0] = ((macBytes[0].toInt() and 0xFE) or 0x02).toByte()
            val mac = macBytes.joinToString(":") { "%02X".format(it) }

            val diskBytes = ByteArray(24).also(rnd::nextBytes)
            val diskSerial = diskBytes.joinToString("") { "%02X".format(it) }

            val hashBytes = ByteArray(32).also(rnd::nextBytes)
            val hashHex = hashBytes.joinToString("") { "%02x".format(it) }

            return "$mac@@@$diskSerial@@@@@@$hashHex"
        }
    }
}

internal enum class NeteaseCryptoMode {
    WEAPI,
    EAPI,
    API,
    LINUXAPI;

    companion object {
        fun fromQuery(raw: String?): NeteaseCryptoMode? = when (raw?.trim()?.lowercase()) {
            "weapi" -> WEAPI
            "eapi" -> EAPI
            "api" -> API
            "linuxapi" -> LINUXAPI
            else -> null
        }
    }
}

internal data class OsProfile(
    val os: String,
    val appver: String,
    val osver: String,
    val channel: String
)

/**
 * 对齐 `@neteasecloudmusicapienhanced/api/util/request.js` 的 Cookie 补全、客户端请求头组装与 UA 选择逻辑。
 */
internal object NeteaseRequestContextBuilder {

    const val DOMAIN = "https://music.163.com"
    const val API_DOMAIN = "https://interface.music.163.com"

    private const val LOWER_ALPHA = "abcdefghijklmnopqrstuvwxyz"

    val OS_MAP: Map<String, OsProfile> = mapOf(
        "pc" to OsProfile(
            os = "pc",
            appver = "3.1.17.204416",
            osver = "Microsoft-Windows-10-Professional-build-19045-64bit",
            channel = "netease"
        ),
        "linux" to OsProfile(
            os = "linux",
            appver = "1.2.1.0428",
            osver = "Deepin 20.9",
            channel = "netease"
        ),
        "android" to OsProfile(
            os = "android",
            appver = "8.20.20.231215173437",
            osver = "14",
            channel = "xiaomi"
        ),
        "iphone" to OsProfile(
            os = "iPhone OS",
            appver = "9.0.90",
            osver = "16.2",
            channel = "distribution"
        )
    )

    private const val UA_WEAPI_PC =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0"
    private const val UA_LINUX =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36"
    private const val UA_API_PC =
        "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/3.1.17.204416"
    private const val UA_API_ANDROID =
        "NeteaseMusic/9.1.65.240927161425(9001065);Dalvik/2.1.0 (Linux; U; Android 14; 23013RK75C Build/UKQ1.230804.001)"
    private const val UA_API_IPHONE =
        "NeteaseMusic 9.0.90/5038 (iPhone; iOS 16.2; zh_CN)"

    private val random = SecureRandom()

    private val wnmcid: String by lazy { generateWnmcid() }

    private val stableSessionChineseIp: String by lazy { generateRandomChineseIp() }

    fun generateNtesNuid(): String = randomHex(64)

    fun generateWnmcid(nowMs: Long = System.currentTimeMillis()): String {
        val prefix = CharArray(6) { LOWER_ALPHA[random.nextInt(LOWER_ALPHA.length)] }.concatToString()
        return "$prefix.$nowMs.01.0"
    }

    fun resolveUaTypeForOs(osValue: String?): String = when (osValue?.trim()?.lowercase()) {
        "android" -> "android"
        "iphone os", "iphone", "ios" -> "iphone"
        "linux" -> "linux"
        else -> "pc"
    }

    fun chooseUserAgent(crypto: NeteaseCryptoMode, uaType: String = "pc"): String = when (crypto) {
        NeteaseCryptoMode.WEAPI -> UA_WEAPI_PC
        NeteaseCryptoMode.LINUXAPI -> UA_LINUX
        NeteaseCryptoMode.EAPI, NeteaseCryptoMode.API -> when (uaType.lowercase()) {
            "android" -> UA_API_ANDROID
            "iphone" -> UA_API_IPHONE
            else -> UA_API_PC
        }
    }

    fun generateRequestId(nowMs: Long = System.currentTimeMillis()): String {
        val suffix = random.nextInt(1000).toString().padStart(4, '0')
        return "${nowMs}_$suffix"
    }

    fun generateChainId(rawCookie: String?, nowMs: Long = System.currentTimeMillis()): String {
        val pairs = CookieSanitizer.parsePairs(rawCookie)
        val deviceId = pairs["sDeviceId"] ?: "unknown-${random.nextInt(1_000_000)}"
        return "v1_${deviceId}_web_login_$nowMs"
    }

    fun processCookieMap(
        rawCookie: String?,
        uri: String,
        deviceId: String,
        anonymousToken: String?,
        defaultOsKey: String = "pc",
        nowMs: Long = System.currentTimeMillis(),
        ntesNuid: String? = null,
        ntesNnid: String? = null,
        wnmcidOverride: String? = null
    ): LinkedHashMap<String, String> {
        val input = CookieSanitizer.parsePairs(rawCookie)
        val resolvedNtesNuid = input["_ntes_nuid"] ?: ntesNuid?.takeIf { it.isNotBlank() } ?: randomHex(64)
        val osProfile = OS_MAP[input["os"]] ?: OS_MAP[defaultOsKey] ?: OS_MAP.getValue("pc")

        val processed = LinkedHashMap<String, String>(input.size + 12)
        processed.putAll(input)
        processed["__remember_me"] = "true"
        processed["ntes_kaola_ad"] = "1"
        processed["_ntes_nuid"] = resolvedNtesNuid
        processed["_ntes_nnid"] = input["_ntes_nnid"]
            ?: ntesNnid?.takeIf { it.isNotBlank() }
            ?: "$resolvedNtesNuid,$nowMs"
        processed["WNMCID"] = input["WNMCID"] ?: wnmcidOverride?.takeIf { it.isNotBlank() } ?: wnmcid
        processed["WEVNSM"] = input["WEVNSM"] ?: "1.0.0"
        processed["osver"] = input["osver"] ?: osProfile.osver
        processed["deviceId"] = input["deviceId"] ?: deviceId
        processed["os"] = input["os"] ?: osProfile.os
        processed["channel"] = input["channel"] ?: osProfile.channel
        processed["appver"] = input["appver"] ?: osProfile.appver

        if ("login" !in uri) {
            processed["NMTID"] = input["NMTID"] ?: randomHex(32)
        }
        if (processed["MUSIC_U"].isNullOrBlank() && !anonymousToken.isNullOrBlank()) {
            processed["MUSIC_A"] = processed["MUSIC_A"] ?: anonymousToken
        }
        return processed
    }

    fun encodeCookieHeader(cookieMap: Map<String, String>): String {
        return cookieMap.entries.joinToString("; ") { (k, v) ->
            "${urlEncode(k)}=${urlEncode(v)}"
        }
    }

    fun buildEapiHeaderJson(
        processedCookie: Map<String, String>,
        requestId: String = generateRequestId(),
        nowMs: Long = System.currentTimeMillis()
    ): JsonObject {
        val header = JsonObject()
        header.addProperty("osver", processedCookie["osver"] ?: "")
        header.addProperty("deviceId", processedCookie["deviceId"] ?: "")
        header.addProperty("os", processedCookie["os"] ?: "")
        header.addProperty("appver", processedCookie["appver"] ?: "")
        header.addProperty("versioncode", processedCookie["versioncode"] ?: "140")
        header.addProperty("mobilename", processedCookie["mobilename"] ?: "")
        header.addProperty("buildver", processedCookie["buildver"] ?: (nowMs.toString().take(10)))
        header.addProperty("resolution", processedCookie["resolution"] ?: "1920x1080")
        header.addProperty("__csrf", processedCookie["__csrf"] ?: "")
        header.addProperty("channel", processedCookie["channel"] ?: "")
        header.addProperty("requestId", requestId)
        processedCookie["MUSIC_U"]?.takeIf { it.isNotEmpty() }?.let { header.addProperty("MUSIC_U", it) }
        processedCookie["MUSIC_A"]?.takeIf { it.isNotEmpty() }?.let { header.addProperty("MUSIC_A", it) }
        return header
    }

    fun eapiHeaderCookieString(header: JsonObject): String {
        return header.entrySet().joinToString("; ") { (k, el) ->
            val v = if (el.isJsonPrimitive) el.asString else ""
            "${urlEncode(k)}=${urlEncode(v)}"
        }
    }

    /**
     * 返回本次应用会话内固定不变的中国大陆运营商 IPv4 地址，
     * 作为外部代理模式的兜底 IP，避免连续请求之间 IP 频繁跨省跳变。
     */
    fun sessionChineseIp(): String = stableSessionChineseIp

    fun generateRandomChineseIp(): String {
        // 经确认的中国大陆常用运营商 /24 网段（电信 / 联通 / 移动）
        val prefixes = arrayOf(
            "116.25.146",
            "113.108.186",
            "183.14.132",
            "114.242.248",
            "218.107.132",
            "101.81.200",
            "125.118.140",
            "182.138.102",
            "117.136.38",
            "223.104.63"
        )
        val r = ThreadLocalRandom.current()
        val prefix = prefixes[r.nextInt(prefixes.size)]
        val host = r.nextInt(2, 254)
        return "$prefix.$host"
    }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun randomHex(length: Int): String {
        val hex = "0123456789abcdef"
        val bytes = ByteArray((length + 1) / 2)
        random.nextBytes(bytes)
        val out = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            out.append(hex[v ushr 4]).append(hex[v and 0x0F])
        }
        return out.substring(0, length)
    }
}
