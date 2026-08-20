package com.example.seteasecloudmusic.core.network.interceptor

import android.content.Context
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

    /** SharedPreferences 的文件名，用来存储 Cookie */
    private val COOKIE_PREF_NAME = "auth_cookies"

    /** Cookie 在 SharedPreferences 中的 key 名称 */
    private val COOKIE_KEY = "cookie_string"

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
        // 从 SharedPreferences 读取存储的 Cookie
        val savedCookie = getSavedCookie()

        // 如果没有 Cookie（用户未登录），直接返回原始请求
        if (savedCookie.isNullOrEmpty()) {
            return request
        }

        // 构建 Cookie 请求头
        // request.newBuilder() 创建一个请求的"修改器"
        // .header("Cookie", savedCookie) 添加 Cookie 头
        // .build() 完成修改，生成新请求对象
        return request.newBuilder()
            .header("Cookie", savedCookie)
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
        // 保存响应头里的 Set-Cookie；后续响应体 cookie 会合并进来。
        val headerCookie = extractCookieFromHeaders(response)
        if (!headerCookie.isNullOrBlank()) {
            saveCookie(headerCookie)
        }

        // 某些登录接口（例如二维码登录）会把更完整的 cookie 放在响应体里。
        val bodyCookie = extractCookieFromBody(response)
        if (!bodyCookie.isNullOrBlank()) {
            saveCookie(bodyCookie)
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
        // 获取 SharedPreferences 对象
        // MODE_PRIVATE 表示只有这个 App 能访问
        val prefs = context.getSharedPreferences(COOKIE_PREF_NAME, Context.MODE_PRIVATE)

        // 读取 Cookie，如果没有则返回 null
        return prefs.getString(COOKIE_KEY, null)
    }

    /**
     * saveCookie - 将 Cookie 保存到本地存储
     *
     * 【SharedPreferences.Editor】
     * SharedPreferences 本身只能读，要写入需要先获取 Editor。
     * apply() 会异步保存（不会阻塞线程），推荐使用。
     */
    private fun saveCookie(cookie: String) {
        val prefs = context.getSharedPreferences(COOKIE_PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val mergedCookie = mergeCookies(getSavedCookie(), cookie)

        // 存入 Cookie
        editor.putString(COOKIE_KEY, mergedCookie)

        // 异步保存到磁盘
        // 注意：用 apply() 而不是 commit()
        // apply() 在后台执行，不会卡住 UI
        editor.apply()
    }

    private fun mergeCookies(existingCookie: String?, newCookie: String): String {
        val cookiePairs = linkedMapOf<String, String>()

        fun append(cookie: String?) {
            cookie
                ?.split(";")
                ?.map { it.trim() }
                ?.filter { it.contains("=") }
                ?.forEach { pair ->
                    val key = pair.substringBefore("=").trim()
                    if (key.isNotBlank()) {
                        cookiePairs[key] = pair
                    }
                }
        }

        append(existingCookie)
        append(newCookie)

        return cookiePairs.values.joinToString("; ")
    }

    // ==================== 公开的辅助方法 ====================

    /**
     * clearCookie - 清除保存的 Cookie
     *
     * 【什么时候调用？】
     * 用户退出登录时，需要清除身份信息。
     * 可以在 AuthRepository 的 logout 方法中调用。
     */
    fun clearCookie() {
        val prefs = context.getSharedPreferences(COOKIE_PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(COOKIE_KEY).apply()
    }

    /**
     * hasCookie - 检查是否有保存的 Cookie
     *
     * 【用途】
     * 判断用户是否已登录（有 Cookie ≈ 已登录）。
     */
    fun hasCookie(): Boolean {
        return !getSavedCookie().isNullOrEmpty()
    }
}
