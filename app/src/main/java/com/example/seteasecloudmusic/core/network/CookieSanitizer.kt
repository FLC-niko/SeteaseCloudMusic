package com.example.seteasecloudmusic.core.network

import okhttp3.HttpUrl
import java.util.Locale

/**
 * 规范化网易云持久化 Cookie 字符串（剥离 Set-Cookie 属性与空值键），并对日志 URL 中的 `cookie` 参数脱敏。
 */
internal object CookieSanitizer {
    // 需要过滤的 Set-Cookie 响应头属性键（统一小写匹配）
    private val SET_COOKIE_ATTRIBUTE_KEYS = setOf(
        "path",
        "expires",
        "max-age",
        "domain",
        "httponly",
        "secure",
        "samesite",
        "priority",
        "partitioned"
    )

    /**
     * 按出现顺序解析 Cookie 键值对，跳过 Set-Cookie 属性，并在值为空时移除对应键。
     */
    fun parsePairs(rawCookie: String?): LinkedHashMap<String, String> {
        return linkedMapOf<String, String>().also { appendPairs(rawCookie, it) }
    }

    /**
     * 返回去重且仅含有效 `key=value` 会话键值对的 Cookie 字符串。
     */
    fun sanitize(rawCookie: String?): String {
        return formatPairs(parsePairs(rawCookie))
    }

    /**
     * 将 [incomingCookie] 增量合并到 [existingCookie] 上，同时丢弃 Set-Cookie 元数据。
     */
    fun merge(existingCookie: String?, incomingCookie: String?): String {
        return linkedMapOf<String, String>().apply {
            appendPairs(existingCookie, this)
            appendPairs(incomingCookie, this)
        }.let(::formatPairs)
    }

    /**
     * 对调试日志中的 URL 进行脱敏：保留其余 API 参数，将 `cookie` 查询参数替换为长度占位符。
     */
    fun sanitizeUrlForLog(url: HttpUrl): String {
        val cookieValues = url.queryParameterValues("cookie")
        if (cookieValues.isEmpty()) return url.toString()
        val redactedLength = cookieValues.sumOf { it?.length ?: 0 }
        val base = url.newBuilder().removeAllQueryParameters("cookie").build().toString()
        val separator = if ('?' in base) '&' else '?'
        return "${base}${separator}cookie=<redacted:$redactedLength>"
    }

    private fun appendPairs(rawCookie: String?, destination: LinkedHashMap<String, String>) {
        rawCookie
            ?.split(";")
            ?.forEach { token ->
                val trimmed = token.trim()
                if (trimmed.isEmpty()) return@forEach
                val eqIndex = trimmed.indexOf('=')
                if (eqIndex <= 0) return@forEach
                val key = trimmed.substring(0, eqIndex).trim()
                val value = trimmed.substring(eqIndex + 1).trim()
                if (key.isEmpty() || key.lowercase(Locale.ROOT) in SET_COOKIE_ATTRIBUTE_KEYS) {
                    return@forEach
                }
                if (value.isEmpty()) {
                    destination.remove(key)
                } else {
                    destination[key] = value
                }
            }
    }

    private fun formatPairs(pairs: Map<String, String>): String {
        return pairs.entries.joinToString("; ") { (k, v) -> "$k=$v" }
    }
}
