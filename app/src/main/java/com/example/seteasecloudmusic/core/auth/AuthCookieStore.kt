package com.example.seteasecloudmusic.core.auth

import android.content.Context
import com.example.seteasecloudmusic.core.network.CookieSanitizer

/**
 * 全局统一的登录 Cookie 与登录态读写入口。
 *
 * 将历史遗留的双存储（`auth_cookies` 与 `auth_prefs`）收口到同一套读写、清洗与判定规则，
 * 避免各模块自行读取 SharedPreferences 导致“是否已登录”口径不一致或退出清理不彻底。
 */
object AuthCookieStore {
    private const val COOKIE_PREF_NAME = "auth_cookies"
    private const val COOKIE_KEY = "cookie_string"

    private const val AUTH_PREF_NAME = "auth_prefs"
    private const val AUTH_COOKIE_KEY = "cookie"
    private const val AUTH_USER_ID_KEY = "user_id"

    /**
     * 读取当前已保存的登录 Cookie（优先 `auth_cookies`，兜底 `auth_prefs`）。
     */
    fun getCookie(context: Context): String? {
        val primary = context.getSharedPreferences(COOKIE_PREF_NAME, Context.MODE_PRIVATE)
            .getString(COOKIE_KEY, null)
            ?.takeIf { it.isNotBlank() }
        if (primary != null) {
            return primary
        }
        return context.getSharedPreferences(AUTH_PREF_NAME, Context.MODE_PRIVATE)
            .getString(AUTH_COOKIE_KEY, null)
            ?.takeIf { it.isNotBlank() }
    }

    /**
     * 统一判断指定 Cookie 字符串是否包含有效的正式登录凭证（`MUSIC_U`）。
     */
    fun hasValidLoginCookie(rawCookie: String?): Boolean {
        if (rawCookie.isNullOrBlank()) return false
        val pairs = CookieSanitizer.parsePairs(rawCookie)
        return !pairs["MUSIC_U"].isNullOrBlank()
    }

    /**
     * 统一判断当前本地是否持有有效的正式登录态（必须包含非空 `MUSIC_U`）。
     */
    fun hasValidLoginCookie(context: Context): Boolean {
        return hasValidLoginCookie(getCookie(context))
    }

    /**
     * 仅在持有有效登录凭证时返回当前登录用户 ID；未登录或凭证失效时返回 null。
     */
    fun getLoggedInUserId(context: Context): Long? {
        if (!hasValidLoginCookie(context)) return null
        val userId = context.getSharedPreferences(AUTH_PREF_NAME, Context.MODE_PRIVATE)
            .getLong(AUTH_USER_ID_KEY, -1L)
        return userId.takeIf { it > 0L }
    }

    /**
     * 合并并同步持久化登录 Cookie 到两处存储，过滤冗余 Set-Cookie 属性并保留有效键值对。
     */
    fun mergeAndSaveCookie(context: Context, incomingCookie: String?): String? {
        val incoming = incomingCookie?.takeIf { it.isNotBlank() } ?: return getCookie(context)
        val merged = CookieSanitizer.merge(getCookie(context), incoming)
            .ifBlank { CookieSanitizer.sanitize(incoming).ifBlank { incoming } }

        writeRawCookie(context, merged)
        return merged
    }

    /**
     * 直接清洗并同步写入登录 Cookie（用于明确替换或初始化会话 Cookie）。
     */
    fun saveSanitizedCookie(context: Context, cookie: String?) {
        if (cookie.isNullOrBlank()) return
        val clean = CookieSanitizer.sanitize(cookie).ifBlank { cookie }
        writeRawCookie(context, clean)
    }

    /**
     * 仅清除双存储中的 Cookie 键值（用于拦截器或局部重置）。
     */
    fun clearCookies(context: Context) {
        context.getSharedPreferences(COOKIE_PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(COOKIE_KEY)
            .apply()
        context.getSharedPreferences(AUTH_PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(AUTH_COOKIE_KEY)
            .apply()
    }

    /**
     * 彻底清除全部登录态与 Cookie 存储（用于退出登录或切回游客）。
     */
    fun clearAllSessionAndCookies(context: Context) {
        context.getSharedPreferences(COOKIE_PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(COOKIE_KEY)
            .apply()
        context.getSharedPreferences(AUTH_PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    private fun writeRawCookie(context: Context, cleanCookie: String) {
        context.getSharedPreferences(COOKIE_PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(COOKIE_KEY, cleanCookie)
            .apply()
        context.getSharedPreferences(AUTH_PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(AUTH_COOKIE_KEY, cleanCookie)
            .apply()
    }
}
