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

    companion object {
        private const val PREFS_NAME = "netease_local_session"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_ANONYMOUS_TOKEN = "anonymous_token"

        internal fun generateDeviceId(): String {
            val hexChars = "0123456789ABCDEF"
            val rnd = SecureRandom()
            val out = CharArray(52)
            for (i in out.indices) {
                out[i] = hexChars[rnd.nextInt(hexChars.length)]
            }
            return String(out)
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
 * Mirrors Cookie enrichment, client header assembly, and UA selection from `@neteasecloudmusicapienhanced/api/util/request.js`.
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
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0"
    private const val UA_LINUX =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36"
    private const val UA_API_PC =
        "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/3.0.18.203152"
    private const val UA_API_ANDROID =
        "NeteaseMusic/9.1.65.240927161425(9001065);Dalvik/2.1.0 (Linux; U; Android 14; 23013RK75C Build/UKQ1.230804.001)"
    private const val UA_API_IPHONE =
        "NeteaseMusic 9.0.90/5038 (iPhone; iOS 16.2; zh_CN)"

    private val random = SecureRandom()

    private val wnmcid: String by lazy {
        val prefix = CharArray(6) { LOWER_ALPHA[random.nextInt(LOWER_ALPHA.length)] }.concatToString()
        "$prefix.${System.currentTimeMillis()}.01.0"
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
        nowMs: Long = System.currentTimeMillis()
    ): LinkedHashMap<String, String> {
        val input = CookieSanitizer.parsePairs(rawCookie)
        val ntesNuid = input["_ntes_nuid"] ?: randomHex(64)
        val osProfile = OS_MAP[input["os"]] ?: OS_MAP[defaultOsKey] ?: OS_MAP.getValue("pc")

        val processed = LinkedHashMap<String, String>(input.size + 12)
        processed.putAll(input)
        processed["__remember_me"] = "true"
        processed["ntes_kaola_ad"] = "1"
        processed["_ntes_nuid"] = ntesNuid
        processed["_ntes_nnid"] = input["_ntes_nnid"] ?: "$ntesNuid,$nowMs"
        processed["WNMCID"] = input["WNMCID"] ?: wnmcid
        processed["WEVNSM"] = input["WEVNSM"] ?: "1.0.0"
        processed["osver"] = input["osver"] ?: osProfile.osver
        processed["deviceId"] = input["deviceId"] ?: deviceId
        processed["os"] = input["os"] ?: osProfile.os
        processed["channel"] = input["channel"] ?: osProfile.channel
        processed["appver"] = input["appver"] ?: osProfile.appver

        if ("login" !in uri) {
            processed["NMTID"] = randomHex(32)
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

    fun generateRandomChineseIp(): String {
        // Domestic ISP ranges used only for X-Real-IP when explicitly requested
        val prefixes = intArrayOf(36, 42, 58, 59, 60, 111, 112, 113, 114, 115, 116, 117, 118, 120, 121, 122, 123, 180, 182, 220, 221, 222)
        val r = ThreadLocalRandom.current()
        val a = prefixes[r.nextInt(prefixes.size)]
        val b = r.nextInt(1, 255)
        val c = r.nextInt(1, 255)
        val d = r.nextInt(1, 255)
        return "$a.$b.$c.$d"
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
