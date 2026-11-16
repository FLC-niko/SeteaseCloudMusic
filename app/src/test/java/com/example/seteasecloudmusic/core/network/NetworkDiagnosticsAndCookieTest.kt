package com.example.seteasecloudmusic.core.network

import okhttp3.Call
import okhttp3.Connection
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

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

    @Test
    fun failoverInterceptor_retriesBackupOnTimeoutStaysDuringCooldownAndRecovers() {
        var nowMs = 1_000L
        val warnings = mutableListOf<String>()
        val interceptor = ApiEndpointFailoverInterceptor(
            primaryBaseUrl = "http://57.158.26.135:3000/",
            backupBaseUrl = "http://104.214.177.182:3000/",
            cooldownMs = 60_000L,
            clockMs = { nowMs },
            logWarning = { warnings += it }
        )

        val attemptedUrls = mutableListOf<String>()
        var failPrimary = true
        val responder: (Request) -> Response = { req ->
            attemptedUrls += req.url.toString()
            if (req.url.host == "57.158.26.135" && failPrimary) {
                throw SocketTimeoutException("read timed out")
            }
            buildJsonResponse(req, 200, "{}")
        }

        val r1 = interceptor.intercept(
            FakeChain(
                Request.Builder().url("http://57.158.26.135:3000/song/url/v1?id=64377&level=hires").build(),
                responder
            )
        )
        assertEquals(200, r1.code)
        assertEquals(
            listOf(
                "http://57.158.26.135:3000/song/url/v1?id=64377&level=hires",
                "http://104.214.177.182:3000/song/url/v1?id=64377&level=hires"
            ),
            attemptedUrls
        )
        assertEquals(1, warnings.size)

        // Cooldown window should route straight to backup without probing failing primary
        nowMs += 10_000L
        val r2 = interceptor.intercept(
            FakeChain(
                Request.Builder().url("http://57.158.26.135:3000/inner/version").build(),
                responder
            )
        )
        assertEquals(200, r2.code)
        assertEquals("http://104.214.177.182:3000/inner/version", attemptedUrls.last())

        // After cooldown expires, primary is probed again when healthy
        nowMs += 60_000L
        failPrimary = false
        val r3 = interceptor.intercept(
            FakeChain(
                Request.Builder().url("http://57.158.26.135:3000/inner/version").build(),
                responder
            )
        )
        assertEquals(200, r3.code)
        assertEquals("http://57.158.26.135:3000/inner/version", attemptedUrls.last())
    }

    @Test
    fun failoverInterceptor_retriesBackupOn5xxClosesFailedBodyAndIgnoresNeteaseDomains() {
        val interceptor = ApiEndpointFailoverInterceptor(
            primaryBaseUrl = "http://57.158.26.135:3000/",
            backupBaseUrl = "http://104.214.177.182:3000/",
            logWarning = {}
        )

        var closed502Body = false
        val attemptedHosts = mutableListOf<String>()
        val responder: (Request) -> Response = { req ->
            attemptedHosts += req.url.host
            if (req.url.host == "57.158.26.135") {
                buildJsonResponse(req, 502, "bad gateway") { closed502Body = true }
            } else {
                buildJsonResponse(req, 200, """{"code":200}""")
            }
        }

        val res = interceptor.intercept(
            FakeChain(
                Request.Builder()
                    .url("http://57.158.26.135:3000/song/url/v1?id=3448488384&level=hires&randomCNIP=true")
                    .build(),
                responder
            )
        )
        assertEquals(200, res.code)
        assertTrue(closed502Body)
        assertEquals(listOf("57.158.26.135", "104.214.177.182"), attemptedHosts)

        val directNeteaseRes = interceptor.intercept(
            FakeChain(
                Request.Builder().url("https://clientlog3.music.163.com/weapi/feedback/weblog").build(),
                responder
            )
        )
        assertEquals(200, directNeteaseRes.code)
        assertEquals("clientlog3.music.163.com", attemptedHosts.last())
    }

    @Test
    fun failoverInterceptor_fallsBackToPrimaryIfBackupFailsDuringCooldown() {
        var nowMs = 10_000L
        val interceptor = ApiEndpointFailoverInterceptor(
            primaryBaseUrl = "http://57.158.26.135:3000/",
            backupBaseUrl = "http://104.214.177.182:3000/",
            cooldownMs = 60_000L,
            clockMs = { nowMs },
            logWarning = {}
        )

        var step = 0
        val attemptedHosts = mutableListOf<String>()
        val responder: (Request) -> Response = { req ->
            attemptedHosts += req.url.host
            step++
            when (step) {
                1 -> throw IOException("primary down")
                2 -> buildJsonResponse(req, 200, "{}")
                3 -> throw IOException("backup down")
                else -> buildJsonResponse(req, 200, "{}")
            }
        }

        val first = interceptor.intercept(
            FakeChain(Request.Builder().url("http://57.158.26.135:3000/inner/version").build(), responder)
        )
        assertEquals(200, first.code)

        nowMs += 5_000L
        val recovered = interceptor.intercept(
            FakeChain(Request.Builder().url("http://57.158.26.135:3000/inner/version").build(), responder)
        )
        assertEquals(200, recovered.code)
        assertEquals(
            listOf("57.158.26.135", "104.214.177.182", "104.214.177.182", "57.158.26.135"),
            attemptedHosts
        )
    }

    private fun buildJsonResponse(
        request: Request,
        code: Int,
        payload: String,
        onClose: (() -> Unit)? = null
    ): Response {
        val body = object : ResponseBody() {
            override fun contentType(): MediaType = "application/json".toMediaType()
            override fun contentLength(): Long = payload.length.toLong()
            override fun source(): BufferedSource = Buffer().writeUtf8(payload)
            override fun close() {
                onClose?.invoke()
            }
        }
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Error")
            .body(body)
            .build()
    }

    private class FakeChain(
        private val initialRequest: Request,
        private val responder: (Request) -> Response
    ) : Interceptor.Chain {
        override fun request(): Request = initialRequest
        override fun proceed(request: Request): Response = responder(request)
        override fun connection(): Connection? = null
        override fun call(): Call = throw UnsupportedOperationException()
        override fun connectTimeoutMillis(): Int = 10_000
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun readTimeoutMillis(): Int = 15_000
        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun writeTimeoutMillis(): Int = 15_000
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    }
}
