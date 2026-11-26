package com.example.seteasecloudmusic.core.network.local

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

internal data class LocalApiResult(
    val httpStatus: Int,
    val bodyJson: String,
    val setCookies: List<String> = emptyList()
)

/**
 * 在本地直连执行网易云上游接口请求，负责加密模式选择、匿名游客令牌懒注册与持久化、
 * 请求头/会话 Cookie 组装以及响应规范化。
 */
internal class LocalNeteaseClient(
    private val transport: NeteaseUpstreamTransport,
    private val tokenStore: NeteaseTokenStore,
    private val clockMs: () -> Long = System::currentTimeMillis
) {
    private val gson = Gson()
    private val anonLock = Any()

    fun request(
        upstreamPath: String,
        data: JsonObject,
        queryParams: Map<String, String>,
        rawCookie: String?,
        defaultCrypto: NeteaseCryptoMode = NeteaseCryptoMode.EAPI
    ): LocalApiResult {
        val crypto = NeteaseCryptoMode.fromQuery(queryParams["crypto"]) ?: defaultCrypto
        val deviceId = tokenStore.getOrCreateDeviceId()
        val nowMs = clockMs()
        val effectiveRawCookie = mergePreLoginCookiesIfUnauthenticated(rawCookie)

        var cookieMap = NeteaseRequestContextBuilder.processCookieMap(
            rawCookie = effectiveRawCookie,
            uri = upstreamPath,
            deviceId = deviceId,
            anonymousToken = tokenStore.getAnonymousToken(),
            nowMs = nowMs,
            ntesNuid = tokenStore.getOrCreateNtesNuid(),
            ntesNnid = tokenStore.getOrCreateNtesNnid(nowMs),
            wnmcidOverride = tokenStore.getOrCreateWnmcid(nowMs)
        )

        // 无登录态且无游客令牌时，先加锁懒注册匿名游客令牌 MUSIC_A
        if (cookieMap["MUSIC_U"].isNullOrBlank() &&
            cookieMap["MUSIC_A"].isNullOrBlank() &&
            upstreamPath != REGISTER_ANONYMOUS_PATH
        ) {
            val freshAnon = ensureAnonymousToken(deviceId)
            if (!freshAnon.isNullOrBlank()) {
                cookieMap["MUSIC_A"] = freshAnon
            }
        }

        val headers = LinkedHashMap<String, String>()
        val clientIp = resolveClientIp(queryParams)
        if (!clientIp.isNullOrBlank()) {
            headers["X-Real-IP"] = clientIp
            headers["X-Forwarded-For"] = clientIp
        }

        val csrfToken = cookieMap["__csrf"] ?: ""
        val upstreamReq = buildEncryptedRequest(
            upstreamPath = upstreamPath,
            data = data,
            queryParams = queryParams,
            cookieMap = cookieMap,
            csrfToken = csrfToken,
            crypto = crypto
        )
        headers.putAll(upstreamReq.headers)

        val upstreamRes = transport.postForm(
            UpstreamHttpRequest(
                url = upstreamReq.url,
                headers = headers,
                formFields = upstreamReq.formFields
            )
        )

        val cleanedCookies = stripDomainFromSetCookies(upstreamRes.setCookies)
        if (cleanedCookies.isNotEmpty()) {
            if (extractCookieValue(cleanedCookies, "MUSIC_U") != null) {
                tokenStore.clearPreLoginCookies()
            } else {
                tokenStore.mergePreLoginCookies(cleanedCookies)
            }
        }
        val parsedBody = parseBodyJson(upstreamRes.bodyText, encryptedResponse = upstreamReq.encryptedResponse)
        val bodyCode = parsedBody?.get("code")
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
            ?.asInt

        var mappedStatus = bodyCode ?: upstreamRes.httpCode
        if (bodyCode != null && bodyCode in SPECIAL_STATUS_CODES) {
            mappedStatus = 200
        }
        if (mappedStatus !in 101..599) {
            mappedStatus = 400
        }

        val normalizedJson = parsedBody?.toString() ?: upstreamRes.bodyText.ifBlank { "{}" }
        return LocalApiResult(
            httpStatus = mappedStatus,
            bodyJson = normalizedJson,
            setCookies = cleanedCookies
        )
    }

    private fun ensureAnonymousToken(deviceId: String): String? {
        tokenStore.getAnonymousToken()?.let { return it }
        synchronized(anonLock) {
            tokenStore.getAnonymousToken()?.let { return it }
            return runCatching {
                val data = JsonObject().apply {
                    addProperty("username", NeteaseCrypto.generateAnonymousUsername(deviceId))
                }
                val res = request(
                    upstreamPath = REGISTER_ANONYMOUS_PATH,
                    data = data,
                    queryParams = emptyMap(),
                    rawCookie = null,
                    defaultCrypto = NeteaseCryptoMode.WEAPI
                )
                extractCookieValue(res.setCookies, "MUSIC_A")?.also { token ->
                    tokenStore.saveAnonymousToken(token)
                }
            }.getOrNull()
        }
    }

    private data class BuiltUpstreamRequest(
        val url: String,
        val headers: Map<String, String>,
        val formFields: Map<String, String>,
        val encryptedResponse: Boolean
    )

    /**
     * 按 [crypto] 模式组装上游请求 URL、Header 与加密表单字段（WEAPI / LINUXAPI / EAPI / API）。
     */
    private fun buildEncryptedRequest(
        upstreamPath: String,
        data: JsonObject,
        queryParams: Map<String, String>,
        cookieMap: Map<String, String>,
        csrfToken: String,
        crypto: NeteaseCryptoMode
    ): BuiltUpstreamRequest {
        val headers = LinkedHashMap<String, String>()
        val explicitUa = queryParams["ua"]?.takeIf { it.isNotBlank() }

        return when (crypto) {
            NeteaseCryptoMode.WEAPI -> {
                val payloadObj = data.deepCopy().apply {
                    addProperty("csrf_token", csrfToken)
                }
                val encrypted = NeteaseCrypto.weapi(gson.toJson(payloadObj))
                headers["Referer"] = NeteaseRequestContextBuilder.DOMAIN
                headers["User-Agent"] = explicitUa
                    ?: NeteaseRequestContextBuilder.chooseUserAgent(NeteaseCryptoMode.WEAPI)
                headers["Cookie"] = NeteaseRequestContextBuilder.encodeCookieHeader(cookieMap)

                BuiltUpstreamRequest(
                    url = "${NeteaseRequestContextBuilder.DOMAIN}/weapi/${upstreamPath.removePrefix("/api/")}",
                    headers = headers,
                    formFields = mapOf(
                        "params" to encrypted.params,
                        "encSecKey" to encrypted.encSecKey
                    ),
                    encryptedResponse = false
                )
            }

            NeteaseCryptoMode.LINUXAPI -> {
                val wrapper = JsonObject().apply {
                    addProperty("method", "POST")
                    addProperty("url", "${NeteaseRequestContextBuilder.DOMAIN}$upstreamPath")
                    add("params", data.deepCopy())
                }
                headers["User-Agent"] = explicitUa
                    ?: NeteaseRequestContextBuilder.chooseUserAgent(NeteaseCryptoMode.LINUXAPI, "linux")
                headers["Cookie"] = NeteaseRequestContextBuilder.encodeCookieHeader(cookieMap)

                BuiltUpstreamRequest(
                    url = "${NeteaseRequestContextBuilder.DOMAIN}/api/linux/forward",
                    headers = headers,
                    formFields = mapOf(
                        "eparams" to NeteaseCrypto.linuxapi(gson.toJson(wrapper))
                    ),
                    encryptedResponse = false
                )
            }

            NeteaseCryptoMode.EAPI, NeteaseCryptoMode.API -> {
                val now = clockMs()
                val headerJson = NeteaseRequestContextBuilder.buildEapiHeaderJson(
                    processedCookie = cookieMap,
                    requestId = NeteaseRequestContextBuilder.generateRequestId(now),
                    nowMs = now
                )
                headers["Cookie"] = NeteaseRequestContextBuilder.eapiHeaderCookieString(headerJson)
                headers["User-Agent"] = explicitUa
                    ?: NeteaseRequestContextBuilder.chooseUserAgent(
                        NeteaseCryptoMode.API,
                        NeteaseRequestContextBuilder.resolveUaTypeForOs(cookieMap["os"])
                    )

                if (crypto == NeteaseCryptoMode.EAPI) {
                    val encryptResponse = queryParams["e_r"]?.equals("true", ignoreCase = true) ?: false
                    val payloadObj = data.deepCopy().apply {
                        add("header", headerJson)
                        addProperty("e_r", encryptResponse)
                    }
                    BuiltUpstreamRequest(
                        url = "${NeteaseRequestContextBuilder.API_DOMAIN}/eapi/${upstreamPath.removePrefix("/api/")}",
                        headers = headers,
                        formFields = mapOf(
                            "params" to NeteaseCrypto.eapi(upstreamPath, gson.toJson(payloadObj))
                        ),
                        encryptedResponse = encryptResponse
                    )
                } else {
                    val fields = LinkedHashMap<String, String>()
                    for ((key, el) in data.entrySet()) {
                        fields[key] = elementToFormValue(el)
                    }
                    BuiltUpstreamRequest(
                        url = "${NeteaseRequestContextBuilder.API_DOMAIN}$upstreamPath",
                        headers = headers,
                        formFields = fields,
                        encryptedResponse = false
                    )
                }
            }
        }
    }

    private fun mergePreLoginCookiesIfUnauthenticated(rawCookie: String?): String? {
        val explicitPairs = com.example.seteasecloudmusic.core.network.CookieSanitizer.parsePairs(rawCookie)
        if (!explicitPairs["MUSIC_U"].isNullOrBlank()) {
            return rawCookie
        }
        val preLogin = tokenStore.getPreLoginCookies()
        if (preLogin.isEmpty()) {
            return rawCookie
        }
        val merged = LinkedHashMap<String, String>(preLogin.size + explicitPairs.size)
        merged.putAll(preLogin)
        merged.putAll(explicitPairs)
        return merged.entries.joinToString("; ") { (k, v) -> "$k=$v" }
    }

    private fun resolveClientIp(queryParams: Map<String, String>): String? {
        queryParams["realIP"]?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        if (queryParams["randomCNIP"]?.equals("true", ignoreCase = true) == true) {
            return NeteaseRequestContextBuilder.sessionChineseIp()
        }
        return null
    }

    private fun parseBodyJson(rawBody: String, encryptedResponse: Boolean): JsonObject? {
        if (rawBody.isBlank()) return null
        val decoded = if (encryptedResponse && !rawBody.trimStart().startsWith("{")) {
            runCatching { NeteaseCrypto.eapiResDecrypt(rawBody) }.getOrNull() ?: rawBody
        } else {
            rawBody
        }
        return runCatching {
            val el = JsonParser.parseString(decoded)
            if (el.isJsonObject) el.asJsonObject else null
        }.getOrNull()
    }

    private fun stripDomainFromSetCookies(rawCookies: List<String>): List<String> {
        return rawCookies.map { line ->
            line.replace(DOMAIN_ATTR_REGEX, "")
        }
    }

    private fun extractCookieValue(setCookies: List<String>, key: String): String? {
        for (line in setCookies) {
            val pair = line.substringBefore(";").trim()
            val eqIdx = pair.indexOf('=')
            if (eqIdx > 0 && pair.substring(0, eqIdx).trim() == key) {
                return pair.substring(eqIdx + 1).trim().takeIf { it.isNotEmpty() }
            }
        }
        return null
    }

    private fun elementToFormValue(element: JsonElement): String = when {
        element.isJsonNull -> ""
        element.isJsonPrimitive -> element.asString
        element is JsonObject || element is JsonArray -> element.toString()
        else -> element.toString()
    }

    companion object {
        // 匿名游客令牌注册上游接口路径
        private const val REGISTER_ANONYMOUS_PATH = "/api/register/anonimous"
        // 剥离 Set-Cookie 中的 Domain 属性，避免本地构造响应时域受限
        private val DOMAIN_ATTR_REGEX = Regex("\\s*Domain=[^(;|$)]+;*", RegexOption.IGNORE_CASE)
        // 网易云业务层特殊状态码：命中时对外 HTTP 状态仍映射为 200，交由上层解析 body.code（如二维码扫码状态 800..803）
        private val SPECIAL_STATUS_CODES = setOf(201, 302, 400, 502, 800, 801, 802, 803)
    }
}
