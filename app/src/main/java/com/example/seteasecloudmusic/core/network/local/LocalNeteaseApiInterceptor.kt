package com.example.seteasecloudmusic.core.network.local

import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * 在本地直连模式下拦截指向主/备 API 代理节点的请求，交由 [NeteaseEndpointRegistry] 直接请求网易云官方上游并构造响应。
 */
internal class LocalNeteaseApiInterceptor(
    primaryBaseUrl: String,
    backupBaseUrl: String,
    private val registry: NeteaseEndpointRegistry,
    private val isLocalModeEnabled: () -> Boolean = { true },
    private val logDebug: (String) -> Unit = {}
) : Interceptor {

    private val primaryUrl: HttpUrl? = primaryBaseUrl.trim().takeIf { it.isNotEmpty() }?.toHttpUrlOrNull()
    private val backupUrl: HttpUrl? = backupBaseUrl.trim().takeIf { it.isNotEmpty() }?.toHttpUrlOrNull()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (!isLocalModeEnabled() || !matchesConfiguredApiHost(originalRequest.url)) {
            return chain.proceed(originalRequest)
        }

        val apiPath = extractRelativeApiPath(originalRequest.url)
        if (!registry.isSupported(apiPath)) {
            return chain.proceed(originalRequest)
        }

        val startNs = System.nanoTime()
        val params = collectParams(originalRequest)
        val rawCookie = originalRequest.header("Cookie") ?: params["cookie"]

        val result = registry.dispatch(
            path = apiPath,
            queryParams = params,
            rawCookie = rawCookie
        ) ?: return chain.proceed(originalRequest)

        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
        logDebug("LOCAL <-- ${result.httpStatus} ${originalRequest.method} $apiPath (${elapsedMs}ms)")

        val builder = Response.Builder()
            .request(originalRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(result.httpStatus)
            .message(httpReasonPhrase(result.httpStatus))
            .body(result.bodyJson.toResponseBody(JSON_MEDIA_TYPE))

        for (cookie in result.setCookies) {
            if (cookie.isNotBlank()) {
                builder.addHeader("Set-Cookie", cookie)
            }
        }
        return builder.build()
    }

    private fun matchesConfiguredApiHost(url: HttpUrl): Boolean {
        val p = primaryUrl
        val b = backupUrl
        val matchPrimary = p != null && url.host.equals(p.host, ignoreCase = true) && url.port == p.port
        val matchBackup = b != null && url.host.equals(b.host, ignoreCase = true) && url.port == b.port
        return matchPrimary || matchBackup
    }

    // 剥离 BaseUrl 中可能包含的路径前缀，得到用于路由匹配的相对路径
    private fun extractRelativeApiPath(url: HttpUrl): String {
        val rawPath = url.encodedPath
        val prefix = matchingPrefix(url)
        val stripped = if (prefix.isNotEmpty() && (rawPath == prefix || rawPath.startsWith("$prefix/"))) {
            rawPath.removePrefix(prefix).ifEmpty { "/" }
        } else {
            rawPath
        }
        return NeteaseEndpointRegistry.normalizePath(stripped)
    }

    private fun matchingPrefix(url: HttpUrl): String {
        primaryUrl?.let { p ->
            if (url.host.equals(p.host, ignoreCase = true) && url.port == p.port) {
                return p.encodedPath.trimEnd('/')
            }
        }
        backupUrl?.let { b ->
            if (url.host.equals(b.host, ignoreCase = true) && url.port == b.port) {
                return b.encodedPath.trimEnd('/')
            }
        }
        return ""
    }

    // 合并 URL Query 参数与 POST FormBody 字段，FormBody 同名字段优先级更高
    private fun collectParams(request: okhttp3.Request): Map<String, String> {
        val params = LinkedHashMap<String, String>()
        val url = request.url
        for (i in 0 until url.querySize) {
            val name = url.queryParameterName(i)
            val value = url.queryParameterValue(i) ?: ""
            params[name] = value
        }
        val body = request.body
        if (body is FormBody) {
            for (i in 0 until body.size) {
                params[body.name(i)] = body.value(i)
            }
        }
        return params
    }

    private fun httpReasonPhrase(code: Int): String = when (code) {
        200 -> "OK"
        400 -> "Bad Request"
        401 -> "Unauthorized"
        403 -> "Forbidden"
        404 -> "Not Found"
        502 -> "Bad Gateway"
        else -> if (code in 500..599) "Server Error" else "HTTP $code"
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
