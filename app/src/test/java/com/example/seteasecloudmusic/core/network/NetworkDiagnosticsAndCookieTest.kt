package com.example.seteasecloudmusic.core.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkDiagnosticsAndCookieTest {

    @Test
    fun parsePairs_stripsSetCookieAttributesAndEmptyDeletions() {
        val raw = "MUSIC_U=old; Max-Age=2592000; Expires=Thu, 01-Jan-2026 00:00:00 GMT; " +
            "Path=/; Domain=.music.163.com; HttpOnly=; Secure=; SameSite=Lax; " +
            "__csrf=token123; MUSIC_A_T=; MUSIC_U=fresh; invalid_token"

        val pairs = CookieSanitizer.parsePairs(raw)

        assertEquals(mapOf("MUSIC_U" to "fresh", "__csrf" to "token123"), pairs)
        assertFalse(pairs.containsKey("Max-Age"))
        assertFalse(pairs.containsKey("Expires"))
        assertFalse(pairs.containsKey("Path"))
        assertFalse(pairs.containsKey("MUSIC_A_T"))
    }

    @Test
    fun sanitize_deduplicatesAndCompactsCookieHeader() {
        val raw = "MUSIC_U=first; path=/; __csrf=csrf1; MUSIC_U=second; max-age=600"

        val sanitized = CookieSanitizer.sanitize(raw)

        assertEquals("MUSIC_U=second; __csrf=csrf1", sanitized)
    }

    @Test
    fun merge_updatesExistingTokensDeletesClearedKeysAndStripsAttributes() {
        val existing = "MUSIC_U=old_u; __csrf=csrf1; NMTID=keep"
        val incoming = "MUSIC_U=new_u; Path=/; Expires=Wed, 01-Jan-2026 00:00:00 GMT; __csrf=; MUSIC_R_T=rt99"

        val merged = CookieSanitizer.merge(existing, incoming)

        assertEquals("MUSIC_U=new_u; NMTID=keep; MUSIC_R_T=rt99", merged)
    }

    @Test
    fun sanitizeUrlForLog_redactsCookieQueryAndPreservesApiParams() {
        val urlWithOtherParams =
            "http://57.158.26.135:3000/song/url/v1?id=64377&level=hires&cookie=MUSIC_U=secret_token"
                .toHttpUrl()
        val urlCookieOnly = "http://57.158.26.135:3000/login/status?cookie=123456".toHttpUrl()
        val urlWithoutCookie = "http://57.158.26.135:3000/song/url/v1?id=64377&level=hires".toHttpUrl()

        assertEquals(
            "http://57.158.26.135:3000/song/url/v1?id=64377&level=hires&cookie=<redacted:20>",
            CookieSanitizer.sanitizeUrlForLog(urlWithOtherParams)
        )
        assertEquals(
            "http://57.158.26.135:3000/login/status?cookie=<redacted:6>",
            CookieSanitizer.sanitizeUrlForLog(urlCookieOnly)
        )
        assertEquals(urlWithoutCookie.toString(), CookieSanitizer.sanitizeUrlForLog(urlWithoutCookie))
    }

    @Test
    fun createApiDispatcher_raisesPerHostLimitAboveOkHttpDefault() {
        val dispatcher = NetworkModule.createApiDispatcher()

        assertEquals(64, dispatcher.maxRequests)
        assertEquals(16, dispatcher.maxRequestsPerHost)
        assertTrue(dispatcher.maxRequestsPerHost > 5)
    }
}
