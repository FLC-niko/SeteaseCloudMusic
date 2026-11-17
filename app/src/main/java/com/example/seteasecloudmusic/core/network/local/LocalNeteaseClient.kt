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
 * Executes Netease API upstream calls locally, handling crypto mode selection,
 * anonymous token lazy registration/persistence, header assembly, and response normalization.
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

        var cookieMap = NeteaseRequestContextBuilder.processCookieMap(
            rawCookie = rawCookie,
            uri = upstreamPath,
            deviceId = deviceId,
            anonymousToken = tokenStore.getAnonymousToken(),
            nowMs = clockMs()
        )

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
                    ?: NeteaseRequestContextBuilder.chooseUserAgent(NeteaseCryptoMode.API, "iphone")

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

    private fun resolveClientIp(queryParams: Map<String, String>): String? {
        queryParams["realIP"]?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        if (queryParams["randomCNIP"]?.equals("true", ignoreCase = true) == true) {
            return NeteaseRequestContextBuilder.generateRandomChineseIp()
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
        private const val REGISTER_ANONYMOUS_PATH = "/api/register/anonimous"
        private val DOMAIN_ATTR_REGEX = Regex("\\s*Domain=[^(;|$)]+;*", RegexOption.IGNORE_CASE)
        private val SPECIAL_STATUS_CODES = setOf(201, 302, 400, 502, 800, 801, 802, 803)
    }
}
