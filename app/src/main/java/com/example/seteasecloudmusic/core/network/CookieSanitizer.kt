package com.example.seteasecloudmusic.core.network

import okhttp3.HttpUrl
import java.util.Locale

/**
 * Normalizes persisted NetEase cookie strings and redacts cookie query parameters from logs.
 */
internal object CookieSanitizer {
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
     * Parses cookie name-value pairs in order, stripping Set-Cookie attributes and empty-value deletions.
     */
    fun parsePairs(rawCookie: String?): LinkedHashMap<String, String> {
        return linkedMapOf<String, String>().also { appendPairs(rawCookie, it) }
    }

    /**
     * Returns a deduplicated Cookie string containing only valid key=value session pairs.
     */
    fun sanitize(rawCookie: String?): String {
        return formatPairs(parsePairs(rawCookie))
    }

    /**
     * Merges [incomingCookie] updates on top of [existingCookie] while discarding Set-Cookie metadata.
     */
    fun merge(existingCookie: String?, incomingCookie: String?): String {
        return linkedMapOf<String, String>().apply {
            appendPairs(existingCookie, this)
            appendPairs(incomingCookie, this)
        }.let(::formatPairs)
    }

    /**
     * Redacts `cookie` query values for debug logging while preserving non-sensitive API parameters.
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
