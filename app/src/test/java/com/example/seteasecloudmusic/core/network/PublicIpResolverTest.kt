package com.example.seteasecloudmusic.core.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class PublicIpResolverTest {

    @Test
    fun parsers_distinguishMainlandChinaFromOverseasAndSar() {
        val mainlandIpip = PublicIpResolver.parseIpipJson(
            """{"ret":"ok","data":{"ip":"113.108.186.12","location":["中国","广东","深圳","","电信"]}}"""
        )
        assertNotNull(mainlandIpip)
        assertEquals("113.108.186.12", mainlandIpip!!.ip)
        assertTrue(mainlandIpip.isMainlandChina)

        val hkIpip = PublicIpResolver.parseIpipJson(
            """{"ret":"ok","data":{"ip":"203.198.23.10","location":["中国","香港","","",""]}}"""
        )
        assertNotNull(hkIpip)
        assertFalse(hkIpip!!.isMainlandChina)

        val mainlandPconline = PublicIpResolver.parsePconlineJson(
            """{"ip":"183.14.132.45","pro":"广东省","city":"深圳市","addr":"广东省深圳市 电信","err":""}"""
        )
        assertNotNull(mainlandPconline)
        assertTrue(mainlandPconline!!.isMainlandChina)

        val jpIpSb = PublicIpResolver.parseIpSbJson(
            """{"ip":"133.130.100.10","country_code":"JP","region":"Tokyo"}"""
        )
        assertNotNull(jpIpSb)
        assertFalse(jpIpSb!!.isMainlandChina)
    }

    @Test
    fun isValidPublicIpv4_rejectsPrivateAndInvalidAddresses() {
        assertTrue(PublicIpResolver.isValidPublicIpv4("116.25.146.177"))
        assertFalse(PublicIpResolver.isValidPublicIpv4("127.0.0.1"))
        assertFalse(PublicIpResolver.isValidPublicIpv4("10.0.0.1"))
        assertFalse(PublicIpResolver.isValidPublicIpv4("192.168.1.1"))
        assertFalse(PublicIpResolver.isValidPublicIpv4("172.16.0.1"))
        assertFalse(PublicIpResolver.isValidPublicIpv4("100.64.0.1"))
        assertFalse(PublicIpResolver.isValidPublicIpv4("999.1.1.1"))
        assertFalse(PublicIpResolver.isValidPublicIpv4("not-an-ip"))
    }

    @Test
    fun resolver_cachesMainlandRealIpAndRefreshesAfterNetworkInvalidation() {
        val probeCount = AtomicInteger(0)
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val count = probeCount.incrementAndGet()
                val ip = if (count == 1) "113.108.186.88" else "101.81.200.99"
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(
                        """{"ret":"ok","data":{"ip":"$ip","location":["中国","广东","深圳","","电信"]}}"""
                            .toResponseBody("application/json".toMediaType())
                    )
                    .build()
            }
            .build()

        val resolver = PublicIpResolver(
            httpClient = client,
            fallbackIpProvider = { "116.25.146.10" }
        )

        val first = resolver.refreshNowBlocking()
        val second = resolver.resolveProxyClientIp(allowBriefWait = true)
        assertEquals("113.108.186.88", first)
        assertEquals("113.108.186.88", second)
        assertEquals(1, probeCount.get())

        resolver.invalidateCache()
        val afterSwitch = resolver.refreshNowBlocking()
        assertEquals("101.81.200.99", afterSwitch)
        assertEquals(2, probeCount.get())
    }

    @Test
    fun resolver_fallsBackToStableCnIpOnOverseasVpnOrProbeFailure() {
        val probeCount = AtomicInteger(0)
        val overseasClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                probeCount.incrementAndGet()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(
                        """{"ret":"ok","data":{"ip":"57.158.26.135","location":["美国","弗吉尼亚","","",""]}}"""
                            .toResponseBody("application/json".toMediaType())
                    )
                    .build()
            }
            .build()

        val overseasResolver = PublicIpResolver(
            httpClient = overseasClient,
            fallbackIpProvider = { "116.25.146.77" }
        )
        assertEquals("116.25.146.77", overseasResolver.refreshNowBlocking())
        // 明确探测到海外 IP 时应在首个探测源直接中止并走兜底 IP
        assertEquals(1, probeCount.get())

        val failingClient = OkHttpClient.Builder()
            .addInterceptor { throw IOException("timeout") }
            .build()
        val failingResolver = PublicIpResolver(
            httpClient = failingClient,
            fallbackIpProvider = { "116.25.146.88" }
        )
        assertEquals("116.25.146.88", failingResolver.refreshNowBlocking())
    }
}
