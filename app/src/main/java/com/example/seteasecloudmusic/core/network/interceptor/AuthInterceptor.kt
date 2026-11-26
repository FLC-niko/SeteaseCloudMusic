package com.example.seteasecloudmusic.core.network.interceptor

import android.content.Context
import com.example.seteasecloudmusic.core.auth.AuthCookieStore
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject

/**
 * AuthInterceptor - 认证拦截器
 *
 * 【什么是拦截器？】
 * 拦截器就像一个"中间人"，它站在你和服务器之间：
 * - 你发请求 → 拦截器检查/修改请求 → 请求发送到服务器
 * - 服务器返回响应 → 拦截器检查/修改响应 → 你收到响应
 *
 * 【这个拦截器的作用】
 * 网易云音乐 API 使用 Cookie 来维持登录状态。
 * 这个拦截器会自动：
 * 1. 发请求前：把存储的 Cookie 添加到请求头（告诉服务器"我是谁")
 * 2. 收响应后：保存服务器返回的新 Cookie（记住登录状态）
 */
class AuthInterceptor(private val context: Context) : Interceptor {

    // ==================== 常量定义 ====================

    /**
     * 仅驻留内存的预登录临时会话 Cookie（如 /captcha/sent 下发的 NMTID / __csrf 等），
     * 用于保证发验证码与提交登录处于同一会话上下文，同时不污染已登录态存储。
     */
    private val preLoginCookieLock = Any()
    private val preLoginCookies = LinkedHashMap<String, String>()

    // ==================== 核心拦截方法 ====================

    /**
     * intercept - 拦截器的主方法
     *
     * 【参数 chain】
     * chain 是"链条"，代表整个请求-响应流程。
     * 调用 chain.proceed(request) 就像说"请继续执行这个请求"。
     *
     * 【流程图解】
     * ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
     * │  获取原始请求 │ → │  添加Cookie  │ → │  发送请求   │
     * └─────────────┘    └─────────────┘    └─────────────┘
     *                                           ↓
     * ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
     * │  返回响应   │ ← │  保存新Cookie │ ← │  收到响应   │
     * └─────────────┘    └─────────────┘    └─────────────┘
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        // 第一步：获取原始请求
        // chain.request() 返回即将发送的请求对象
        val originalRequest = chain.request()

        // 第二步：添加 Cookie 到请求头
        // 如果用户已登录，我们需要把 Cookie 带上
        val requestWithCookie = addCookiesToRequest(originalRequest)

        // 第三步：发送请求
        // chain.proceed() 会把请求真正发送到服务器
        // 这个方法返回服务器给的 Response（响应）
        val response = chain.proceed(requestWithCookie)

        // 第四步：保存响应中的新 Cookie
        // 登录成功后，服务器会在响应头里返回 Cookie
        // 我们需要把它保存起来，下次请求继续用
        saveCookiesFromResponse(response)

        // 第五步：返回响应给调用者
        // 最终响应会回到调用 API 的地方（比如你的 UseCase）
        return response
    }

    // ==================== 辅助方法 ====================

    /**
     * addCookiesToRequest - 给请求添加 Cookie 头
     *
     * 【HTTP Cookie 头格式】
     * Cookie: name1=value1; name2=value2
     *
     * 【为什么要添加？】
     * 用户登录后，服务器会给一个 Cookie 作为"身份证明"。
     * 每次请求都带上这个 Cookie，服务器就知道你是谁了。
     */
    private fun addCookiesToRequest(request: Request): Request {
        // 从 SharedPreferences 读取存储的 Cookie；若未登录则回退内存中的预登录会话 Cookie
        val savedCookie = getSavedCookie()
        val effectiveCookie = if (!savedCookie.isNullOrEmpty()) {
            savedCookie
        } else {
            getPreLoginCookieString()
        }

        // 如果没有任何可用 Cookie，直接返回原始请求
        if (effectiveCookie.isNullOrEmpty()) {
            return request
        }

        val originalUrl = request.url
        val host = originalUrl.host

        // 官方网易云接口（如 clientlog3.music.163.com）只接受标准 Cookie 请求头，不能附加 ?cookie= 查询参数
        if (host.contains("163.com") || host.contains("netease.com")) {
            return request.newBuilder()
                .header("Cookie", effectiveCookie)
                .build()
        }

        // Node.js API 代理服务：仅附带清洗去重后的有效会话 Cookie，避免约 4KB Set-Cookie 属性冗余拖慢每个请求 URL
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
     * saveCookiesFromResponse - 从响应中提取并保存 Cookie
     *
     * 【服务器如何返回 Cookie？】
     * 登录成功时，响应头里会有：
     * Set-Cookie: MUSIC_U=xxx; path=/; domain=.music.163.com
     *
     * 【为什么要保存？】
     * 下次请求时需要带上这个 Cookie，所以要存起来。
     */
    private fun saveCookiesFromResponse(response: Response) {
        val path = response.request.url.encodedPath

        // 仅在登录/认证接口或响应头明确包含 MUSIC_U 时更新持久化登录态 Cookie，避免普通请求冲刷掉核心登录态
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

        // 某些登录接口（例如二维码登录）会把更完整的 cookie 放在响应体里。
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
            val body = response.peekBody(1024L * 1024L).string()
            if (body.isBlank()) {
                return null
            }

            JSONObject(body)
                .optString("cookie")
                .takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    // ==================== Cookie 存取方法 ====================

    /**
     * getSavedCookie - 从本地存储读取 Cookie
     *
     * 【SharedPreferences 是什么？】
     * Android 提供的轻量数据存储方式，类似一个小字典：
     * - 存：putString(key, value)
     * - 取：getString(key, 默认值)
     *
     * 【为什么用它存 Cookie？】
     * Cookie 需要持久化保存，用户关闭 App 后下次还能用。
     */
    private fun getSavedCookie(): String? {
        return AuthCookieStore.getCookie(context)
    }

    /**
     * saveCookie - 将 Cookie 合并并同步保存到统一存储中
     */
    private fun saveCookie(cookie: String) {
        AuthCookieStore.mergeAndSaveCookie(context, cookie)
    }

    // ==================== 公开的辅助方法 ====================

    /**
     * clearCookie - 清除全部登录 Cookie 与内存中的预登录临时 Cookie
     */
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

    fun clearCookie() {
        clearPreLoginCookies()
        AuthCookieStore.clearCookies(context)
    }

    /**
     * hasCookie - 统一判断是否持有有效的登录 Cookie（必须包含 `MUSIC_U`）。
     */
    fun hasCookie(): Boolean {
        return AuthCookieStore.hasValidLoginCookie(context)
    }
}
