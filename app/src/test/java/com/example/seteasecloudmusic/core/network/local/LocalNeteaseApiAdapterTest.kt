package com.example.seteasecloudmusic.core.network.local

import com.google.gson.JsonParser
import okhttp3.Call
import okhttp3.Connection
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNeteaseApiAdapterTest {

    @Test
    fun registry_coversAllAppEndpoints() {
        val expectedPaths = setOf(
            "/album", "/artist/album", "/artist/desc", "/artist/detail", "/artist/new/song",
            "/artist/songs", "/artist/sublist", "/artist/top/song", "/captcha/sent", "/cloudsearch",
            "/homepage/block/page", "/like", "/likelist", "/login/cellphone", "/login/qr/check",
            "/login/qr/create", "/login/qr/key", "/logout", "/lyric", "/lyric/new",
            "/personal/fm/mode", "/personal_fm",
            "/personalized", "/personalized/newsong", "/playlist/create", "/playlist/detail",
            "/playlist/tracks", "/playmode/intelligence/list", "/recommend/resource",
            "/recommend/songs", "/scrobble", "/search/hot", "/search/hot/detail",
            "/search/suggest", "/simi/artist", "/simi/song", "/song/order/update",
            "/song/url", "/song/url/v1", "/top/artists", "/top/playlist", "/top/song",
            "/toplist/detail", "/user/account", "/user/playlist"
        )

        assertEquals(45, expectedPaths.size)
        assertEquals(expectedPaths, NeteaseEndpointRegistry.HANDLERS.keys)
    }

    @Test
    fun personalFmAndMode_mapToRadioGetWithExpectedCryptoAndParams() {
        val transport = RecordingTransport { req ->
            UpstreamHttpResponse(200, """{"code":200,"data":[{"id":101,"name":"DJ Song"}]}""")
        }
        val interceptor = buildInterceptor(transport, tokenStore = InMemoryTokenStore(anon = "anon"))

        val fmRes = interceptor.intercept(
            FakeChain(
                Request.Builder()
                    .url("http://57.158.26.135:3000/personal_fm")
                    .header("Cookie", "MUSIC_U=u")
                    .build()
            )
        )
        assertEquals(200, fmRes.code)
        assertEquals("https://music.163.com/weapi/v1/radio/get", transport.requests[0].url)

        val modeRes = interceptor.intercept(
            FakeChain(
                Request.Builder()
                    .url("http://57.158.26.135:3000/personal/fm/mode?mode=AIDJ&limit=10")
                    .header("Cookie", "MUSIC_U=u")
                    .build()
            )
        )
        assertEquals(200, modeRes.code)
        assertEquals("https://interface.music.163.com/eapi/v1/radio/get", transport.requests[1].url)
    }

    @Test
    fun songUrlV1_usesWeapiAndPassesSkyImmerseType() {
        val transport = RecordingTransport { req ->
            assertEquals("https://music.163.com/weapi/song/enhance/player/url/v1", req.url)
            assertTrue(req.formFields.containsKey("params"))
            assertTrue(req.formFields.containsKey("encSecKey"))
            UpstreamHttpResponse(
                httpCode = 200,
                bodyText = """{"code":200,"data":[{"id":29379544,"url":"http://m8.music.126.net/a.flac"}]}"""
            )
        }
        val interceptor = buildInterceptor(transport)

        val response = interceptor.intercept(
            FakeChain(
                Request.Builder()
                    .url("http://57.158.26.135:3000/song/url/v1?id=29379544&level=sky&crypto=weapi")
                    .header("Cookie", "MUSIC_U=user_token; __csrf=csrf1")
                    .build()
            )
        )

        assertEquals(200, response.code)
        val body = response.body!!.string()
        assertTrue(body.contains("http://m8.music.126.net/a.flac"))
    }

    @Test
    fun unauthenticatedGuest_registersAnonymousTokenOnceAndCaches() {
        val requestedUrls = mutableListOf<String>()
        val transport = RecordingTransport { req ->
            requestedUrls += req.url
            if (req.url.endsWith("/weapi/register/anonimous")) {
                UpstreamHttpResponse(
                    httpCode = 200,
                    bodyText = """{"code":200,"userId":999}""",
                    setCookies = listOf("MUSIC_A=anon_secret_val; Max-Age=31536000; Path=/")
                )
            } else {
                assertTrue(req.headers["Cookie"].orEmpty().contains("MUSIC_A=anon_secret_val"))
                UpstreamHttpResponse(
                    httpCode = 200,
                    bodyText = """{"code":200,"result":[]}"""
                )
            }
        }
        val tokenStore = InMemoryTokenStore()
        val interceptor = buildInterceptor(transport, tokenStore = tokenStore)

        val r1 = interceptor.intercept(
            FakeChain(Request.Builder().url("http://57.158.26.135:3000/personalized?limit=8").build())
        )
        val r2 = interceptor.intercept(
            FakeChain(Request.Builder().url("http://57.158.26.135:3000/toplist/detail").build())
        )

        assertEquals(200, r1.code)
        assertEquals(200, r2.code)
        assertEquals("anon_secret_val", tokenStore.getAnonymousToken())
        assertEquals(
            listOf(
                "https://music.163.com/weapi/register/anonimous",
                "https://music.163.com/weapi/personalized/playlist",
                "https://music.163.com/weapi/toplist/detail"
            ),
            requestedUrls
        )
    }

    @Test
    fun qrLoginEndpoints_keyCreateAndCheckPreserveExpectedPayloadsAndStatus801() {
        val transport = RecordingTransport { req ->
            when {
                req.url.endsWith("/eapi/login/qrcode/unikey") ->
                    UpstreamHttpResponse(200, """{"code":200,"unikey":"key-abc-123"}""")
                req.url.endsWith("/eapi/login/qrcode/client/login") ->
                    UpstreamHttpResponse(
                        httpCode = 200,
                        bodyText = """{"code":803,"message":"授权登陆成功"}""",
                        setCookies = listOf("MUSIC_U=new_user_u; Domain=.music.163.com; Path=/")
                    )
                else -> error("Unexpected upstream call: ${req.url}")
            }
        }
        val interceptor = buildInterceptor(transport, tokenStore = InMemoryTokenStore(anon = "anon"))

        val keyRes = interceptor.intercept(
            FakeChain(Request.Builder().url("http://57.158.26.135:3000/login/qr/key?timestamp=1").post(FormBody.Builder().build()).build())
        )
        assertEquals(200, keyRes.code)
        val keyJson = JsonParser.parseString(keyRes.body!!.string()).asJsonObject
        assertEquals("key-abc-123", keyJson.getAsJsonObject("data").get("unikey").asString)

        val createRes = interceptor.intercept(
            FakeChain(Request.Builder().url("http://57.158.26.135:3000/login/qr/create?key=key-abc-123&qrimg=1").post(FormBody.Builder().build()).build())
        )
        assertEquals(200, createRes.code)
        val createJson = JsonParser.parseString(createRes.body!!.string()).asJsonObject
        val qrData = createJson.getAsJsonObject("data")
        assertEquals("https://music.163.com/login?codekey=key-abc-123", qrData.get("qrurl").asString)
        assertTrue(qrData.get("qrimg").asString.startsWith("data:image/png;base64,"))
        assertFalse(transport.requests.any { it.url.contains("create") })

        val checkRes = interceptor.intercept(
            FakeChain(Request.Builder().url("http://57.158.26.135:3000/login/qr/check?key=key-abc-123").post(FormBody.Builder().build()).build())
        )
        assertEquals(200, checkRes.code)
        val checkJson = JsonParser.parseString(checkRes.body!!.string()).asJsonObject
        assertEquals(803, checkJson.get("code").asInt)
        assertTrue(checkJson.get("cookie").asString.contains("MUSIC_U=new_user_u"))
        assertFalse(checkJson.get("cookie").asString.contains("Domain="))
    }

    @Test
    fun playlistTracks_wrapsBodyAndSongUrlSortsByRequestedIds() {
        val transport = RecordingTransport { req ->
            when {
                req.url.endsWith("/eapi/playlist/manipulate/tracks") ->
                    UpstreamHttpResponse(200, """{"code":200,"count":1}""")
                req.url.endsWith("/eapi/song/enhance/player/url") ->
                    UpstreamHttpResponse(
                        200,
                        """{"code":200,"data":[{"id":2,"url":"u2"},{"id":1,"url":"u1"}]}"""
                    )
                else -> error("Unexpected url ${req.url}")
            }
        }
        val interceptor = buildInterceptor(transport, tokenStore = InMemoryTokenStore(anon = "anon"))

        val ptRes = interceptor.intercept(
            FakeChain(
                Request.Builder()
                    .url("http://57.158.26.135:3000/playlist/tracks?op=add&pid=10&tracks=100,200")
                    .header("Cookie", "MUSIC_U=u")
                    .build()
            )
        )
        val ptJson = JsonParser.parseString(ptRes.body!!.string()).asJsonObject
        assertEquals(200, ptJson.get("status").asInt)
        assertEquals(1, ptJson.getAsJsonObject("body").get("count").asInt)

        val urlRes = interceptor.intercept(
            FakeChain(
                Request.Builder()
                    .url("http://57.158.26.135:3000/song/url?id=1,2")
                    .header("Cookie", "MUSIC_U=u")
                    .build()
            )
        )
        val urlJson = JsonParser.parseString(urlRes.body!!.string()).asJsonObject
        val arr = urlJson.getAsJsonArray("data")
        assertEquals(1L, arr[0].asJsonObject.get("id").asLong)
        assertEquals(2L, arr[1].asJsonObject.get("id").asLong)
    }

    @Test
    fun formEncodedPostAndOfficialDomainPassthroughWorkCorrectly() {
        val transport = RecordingTransport { req ->
            assertEquals("https://interface.music.163.com/eapi/playlist/manipulate/tracks", req.url)
            UpstreamHttpResponse(200, """{"code":200}""")
        }
        var passedThroughUrl: String? = null
        val interceptor = buildInterceptor(transport, tokenStore = InMemoryTokenStore(anon = "anon"))

        val orderRes = interceptor.intercept(
            FakeChain(
                Request.Builder()
                    .url("http://57.158.26.135:3000/song/order/update")
                    .header("Cookie", "MUSIC_U=u")
                    .post(FormBody.Builder().add("pid", "77").add("ids", "[1,2]").build())
                    .build()
            )
        )
        assertEquals(200, orderRes.code)

        val directChain = FakeChain(
            Request.Builder().url("https://clientlog3.music.163.com/api/clientlog/encrypt/upload").build()
        ) { req ->
            passedThroughUrl = req.url.toString()
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }
        val directRes = interceptor.intercept(directChain)
        assertEquals(200, directRes.code)
        assertEquals("https://clientlog3.music.163.com/api/clientlog/encrypt/upload", passedThroughUrl)
        assertNotNull(passedThroughUrl)
    }

    @Test
    fun riskControl460_mapsToHttp400PreservingJsonPayload() {
        val transport = RecordingTransport {
            UpstreamHttpResponse(
                httpCode = 200,
                bodyText = """{"code":-460,"message":"检测到您的网络环境存在风险，请稍后再试"}"""
            )
        }
        val interceptor = buildInterceptor(transport, tokenStore = InMemoryTokenStore(anon = "anon"))

        val res = interceptor.intercept(
            FakeChain(
                Request.Builder()
                    .url("http://57.158.26.135:3000/song/url/v1?id=1&level=hires")
                    .header("Cookie", "MUSIC_U=u")
                    .build()
            )
        )

        assertEquals(400, res.code)
        val obj = JsonParser.parseString(res.body!!.string()).asJsonObject
        assertEquals(-460, obj.get("code").asInt)
    }

    private fun buildInterceptor(
        transport: NeteaseUpstreamTransport,
        tokenStore: NeteaseTokenStore = InMemoryTokenStore()
    ): LocalNeteaseApiInterceptor {
        val client = LocalNeteaseClient(transport = transport, tokenStore = tokenStore)
        val registry = NeteaseEndpointRegistry(client = client)
        return LocalNeteaseApiInterceptor(
            primaryBaseUrl = "http://57.158.26.135:3000/",
            backupBaseUrl = "http://104.214.177.182:3000/",
            registry = registry
        )
    }

    private class InMemoryTokenStore(
        private val deviceId: String = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123",
        private var anon: String? = null
    ) : NeteaseTokenStore {
        override fun getOrCreateDeviceId(): String = deviceId
        override fun getAnonymousToken(): String? = anon
        override fun saveAnonymousToken(token: String) {
            anon = token
        }
    }

    private class RecordingTransport(
        private val responder: (UpstreamHttpRequest) -> UpstreamHttpResponse
    ) : NeteaseUpstreamTransport {
        val requests = mutableListOf<UpstreamHttpRequest>()
        override fun postForm(request: UpstreamHttpRequest): UpstreamHttpResponse {
            requests += request
            return responder(request)
        }
    }

    private class FakeChain(
        private val req: Request,
        private val next: (Request) -> Response = { error("Unexpected network fallback for ${it.url}") }
    ) : Interceptor.Chain {
        override fun request(): Request = req
        override fun proceed(request: Request): Response = next(request)
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
