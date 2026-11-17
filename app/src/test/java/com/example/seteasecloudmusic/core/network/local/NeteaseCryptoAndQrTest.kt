package com.example.seteasecloudmusic.core.network.local

import okio.ByteString.Companion.decodeBase64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

class NeteaseCryptoAndQrTest {

    @Test
    fun eapi_matchesNodeReferenceVector() {
        val payload = NeteaseCrypto.eapi("/api/song/lyric", """{"id":29379544,"lv":-1}""")
        assertEquals(
            "9B1595479B79266065903BA79E9883029338432CD7D98D1A3626E997A2C53121620A42261C437BE9DD74818BC92CF07FB3520EA9AF1E32351CDBF2B2FD20D243551C13335B6E791FB5DA07F46820E2B4FA22272AF8B27E723B2BD993FFCE493C6AA3B102FBE7296AB0DB9EA5C46AD12B",
            payload
        )
    }

    @Test
    fun eapiResDecrypt_matchesNodeReferenceVector() {
        val decrypted = NeteaseCrypto.eapiResDecrypt(
            "E879BF9CE6AE320A55D1DA6AC1D8B823A871F700A5A8FA0EA80D444039C2AA09"
        )
        assertEquals("""{"code":200,"ok":true}""", decrypted)
    }

    @Test
    fun linuxapi_matchesNodeReferenceVector() {
        val payload = NeteaseCrypto.linuxapi(
            """{"method":"POST","url":"https://music.163.com/api/v1/album/1","params":{}}"""
        )
        assertEquals(
            "A0D9583F4C5FF68DE851D2893A49DE98FAFB24399F27B4F7E74C64B6FC49A965CFA972FA5EA3D6247CD6247C8198CB87BF9A1F89B78E5A74B84BE1EBE7554006452F437AE6C6F7E62DC5B8BD7AFC748C",
            payload
        )
    }

    @Test
    fun weapi_matchesNodeReferenceVectorForParamsAndRsaEncSecKey() {
        val payload = NeteaseCrypto.weapi(
            plainJson = """{"id":1}""",
            secretKey = "1234567890abcdef"
        )
        assertEquals("TAaGwBJ5s08jtjgpqVB93lJAHbr+eZAorZX3I5kImKo=", payload.params)
        assertEquals(
            "bc05a756cb87b1a2674efda9e90a27d641dcbbe2e6b7be3b61193e47315c298183d409578ac80502bd441d82bf1611d6d9238bcaf64794d096417b726373fdd8cf4a9e9d31d198d6d785ffa3d0a2e8c621b4d6c019fabe0ca6f3a815d9f2a4875a0bcab968f331f394566e1162603ed1dd002aa89b47fd8dcbc77d2c6976efed",
            payload.encSecKey
        )
    }

    @Test
    fun anonymousUsername_matchesNodeReferenceVector() {
        assertEquals(
            "QTFCMkMzRDQgTDFEUzZZaXJhT3B4SXVXTGduRXJxdz09",
            NeteaseCrypto.generateAnonymousUsername("A1B2C3D4")
        )
    }

    @Test
    fun qrCodeGenerator_outputsValidPngDataUrl() {
        val dataUrl = LocalQrCodeGenerator.toDataUrl("https://music.163.com/login?codekey=abc-123", sizePx = 128)
        assertTrue(dataUrl.startsWith("data:image/png;base64,"))
        val raw = dataUrl.removePrefix("data:image/png;base64,").decodeBase64()!!.toByteArray()

        // PNG magic signature + IHDR dimensions
        assertEquals(0x89.toByte(), raw[0])
        assertEquals('P'.code.toByte(), raw[1])
        assertEquals('N'.code.toByte(), raw[2])
        assertEquals('G'.code.toByte(), raw[3])
        val width = ByteBuffer.wrap(raw, 16, 4).int
        val height = ByteBuffer.wrap(raw, 20, 4).int
        assertEquals(128, width)
        assertEquals(128, height)
    }

    @Test
    fun processCookieMap_populatesDefaultsAndOmitsNmtidOnLoginRoutes() {
        val loginCookies = NeteaseRequestContextBuilder.processCookieMap(
            rawCookie = "MUSIC_U=token123; __csrf=csrf9",
            uri = "/api/w/login/cellphone",
            deviceId = "DEV52",
            anonymousToken = "ANON"
        )
        assertFalse(loginCookies.containsKey("NMTID"))
        assertFalse(loginCookies.containsKey("MUSIC_A"))
        assertEquals("token123", loginCookies["MUSIC_U"])
        assertEquals("DEV52", loginCookies["deviceId"])

        val guestCookies = NeteaseRequestContextBuilder.processCookieMap(
            rawCookie = null,
            uri = "/api/personalized/playlist",
            deviceId = "DEV52",
            anonymousToken = "ANON_TOKEN"
        )
        assertTrue(guestCookies.containsKey("NMTID"))
        assertEquals("ANON_TOKEN", guestCookies["MUSIC_A"])
    }
}
