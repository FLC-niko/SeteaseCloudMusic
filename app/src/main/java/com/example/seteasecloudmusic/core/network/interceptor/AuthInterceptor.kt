package com.example.seteasecloudmusic.core.network.interceptor

import android.content.Context
import com.example.seteasecloudmusic.core.auth.AuthCookieStore
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject

/**
 * 认证 Cookie 拦截器：
 * 1. 请求前从 [AuthCookieStore] 注入持久化登录 Cookie（未登录时回退内存中的预登录临时 Cookie）。
 * 2. 响应后从登录/验证码接口的 `Set-Cookie` 头或响应体 `cookie` 字段提取并保存会话 Cookie。
 */
class AuthInterceptor(private val context: Context) : Interceptor {

    /**
     * 仅驻留内存的预登录临时会话 Cookie（如 `/captcha/sent` 下发的 `NMTID` / `__csrf`），
     * 用于保证发验证码与提交登录处于同一会话上下文，同时避免污染已登录态存储。
     */
    private val preLoginCookieLock = Any()
    private val preLoginCookies = LinkedHashMap<String, String>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestWithCookie = addCookiesToRequest(originalRequest)
        val response = chain.proceed(requestWithCookie)
        saveCookiesFromResponse(response)
        return response
    }

    /**
     * 为请求注入 Cookie：网易云官方域名仅设置 `Cookie` 头；Node 代理域名同步附加清洗后的 `?cookie=` 参数。
     */
    private fun addCookiesToRequest(request: Request): Request {
        val savedCookie = getSavedCookie()
        val effectiveCookie = if (!savedCookie.isNullOrEmpty()) {
            savedCookie
        } else {
            getPreLoginCookieString()
        }

        if (effectiveCookie.isNullOrEmpty()) {
            return request
        }

        val originalUrl = request.url
        val host = originalUrl.host

        // 官方网易云接口只接受标准 Cookie 请求头，不附加 ?cookie= 查询参数
        if (host.contains("163.com") || host.contains("netease.com")) {
            return request.newBuilder()
                .header("Cookie", effectiveCookie)
                .build()
        }

        // Node API 代理服务：附加清洗去重后的会话 Cookie，避免 Set-Cookie 属性冗余拉长请求 URL
        val cleanCookie = com.example.seteasecloudmusic.core.network.CookieSanitizer
            .sanitize(effectiveCookie)
            .ifBlank { effectiveCookie }
        val updatedUrl = if (originalUrl.queryParameter("cookie") == null) {
            originalUrl.newBuilder()
                .addQueryParameter("cookie", cleanCookie)
                .build()
        } else {
            originalUrl
        }

        return request.newBuilder()
            .url(updatedUrl)
            .header("Cookie", effectiveCookie)
            .build()
    }

    /**
     * 从响应头 `Set-Cookie` 与 `/login/` 响应体提取并保存 Cookie。
     */
    private fun saveCookiesFromResponse(response: Response) {
        val path = response.request.url.encodedPath

        // 仅在登录接口或含 MUSIC_U 时更新持久化 Cookie，避免普通接口覆盖登录态
        val headerCookie = extractCookieFromHeaders(response)
        if (!headerCookie.isNullOrBlank()) {
            if (path.contains("login") || headerCookie.contains("MUSIC_U")) {
                saveCookie(headerCookie)
                if (headerCookie.contains("MUSIC_U")) {
                    clearPreLoginCookies()
                }
            } else if (getSavedCookie().isNullOrBlank() && path.contains("captcha")) {
                savePreLoginCookies(headerCookie)
            }
        }

        // 二维码等登录接口会在 JSON 响应体中返回完整 cookie 字符串
        val bodyCookie = extractCookieFromBody(response)
        if (!bodyCookie.isNullOrBlank()) {
            saveCookie(bodyCookie)
            if (bodyCookie.contains("MUSIC_U")) {
                clearPreLoginCookies()
            }
        }
    }

    private fun extractCookieFromHeaders(response: Response): String? {
        val cookiePairs = response.headers.values("Set-Cookie")
            .map { it.substringBefore(";").trim() }
            .filter { it.contains("=") }

        if (cookiePairs.isEmpty()) {
            return null
        }

        return cookiePairs.joinToString("; ")
    }

    private fun extractCookieFromBody(response: Response): String? {
        val path = response.request.url.encodedPath
        if (!path.startsWith("/login/")) {
            return null
        }

        return runCatching {
            // 最多预览 1MB 响应体提取登录返回的 cookie 字段
            val body = response.peekBody(1024L * 1024L).string()
            if (body.isBlank()) {
                return null
            }

            JSONObject(body)
                .optString("cookie")
                .takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun getSavedCookie(): String? {
        return AuthCookieStore.getCookie(context)
    }

    private fun saveCookie(cookie: String) {
        AuthCookieStore.mergeAndSaveCookie(context, cookie)
    }

    private fun getPreLoginCookieString(): String? = synchronized(preLoginCookieLock) {
        if (preLoginCookies.isEmpty()) return null
        preLoginCookies.entries.joinToString("; ") { (k, v) -> "$k=$v" }
    }

    private fun savePreLoginCookies(rawCookie: String) {
        val pairs = com.example.seteasecloudmusic.core.network.CookieSanitizer.parsePairs(rawCookie)
        if (pairs.isEmpty()) return
        synchronized(preLoginCookieLock) {
            for ((k, v) in pairs) {
                if (k != "MUSIC_U" && v.isNotBlank()) {
                    preLoginCookies[k] = v
                }
            }
        }
    }

    private fun clearPreLoginCookies() {
        synchronized(preLoginCookieLock) {
            preLoginCookies.clear()
        }
    }

    /**
     * 清除全部持久化登录 Cookie 与内存中的预登录临时 Cookie。
     */
    fun clearCookie() {
        clearPreLoginCookies()
        AuthCookieStore.clearCookies(context)
    }

    /**
     * 判断是否持有有效的登录 Cookie（必须包含 `MUSIC_U`）。
     */
    fun hasCookie(): Boolean {
        return AuthCookieStore.hasValidLoginCookie(context)
    }
}
