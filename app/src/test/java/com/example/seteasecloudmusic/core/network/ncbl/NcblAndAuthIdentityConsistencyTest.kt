package com.example.seteasecloudmusic.core.network.ncbl

import com.example.seteasecloudmusic.core.auth.AuthCookieStore
import com.example.seteasecloudmusic.core.network.local.NeteaseCryptoMode
import com.example.seteasecloudmusic.core.network.local.NeteaseRequestContextBuilder
import com.example.seteasecloudmusic.core.network.local.NeteaseTokenStore
import com.example.seteasecloudmusic.core.network.local.SharedPreferencesNeteaseTokenStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NcblAndAuthIdentityConsistencyTest {

    @Test
    fun ncblContext_alignsDeviceIdentityAndVersionWithMainNetworkStack() {
        val fakeStore = object : NeteaseTokenStore {
            override fun getOrCreateDeviceId(): String = "DEV_52_STABLE_ID"
            override fun getAnonymousToken(): String? = null
            override fun saveAnonymousToken(token: String) {}
            override fun getOrCreateNtesNuid(): String = "nuid_stable_hex"
            override fun getOrCreateNtesNnid(nowMs: Long): String = "nuid_stable_hex,1700000000000"
            override fun getOrCreateWnmcid(nowMs: Long): String = "abcdef.1700000000000.01.0"
            override fun getOrCreateClientSign(): String = "02:AA:BB:CC:DD:EE@@@SERIALHEX@@@@@@HASHHEX"
        }

        val ctx = NcblReporter.buildNcblContext(
            rawCookie = "MUSIC_U=real_user_token; __csrf=csrf_123; NMTID=nmtid_999",
            tokenStore = fakeStore,
            now = 1700000005000L
        )

        assertNotNull(ctx)
        val nonNullCtx = ctx!!
        val pcProfile = NeteaseRequestContextBuilder.OS_MAP.getValue("pc")
        val expectedUa = NeteaseRequestContextBuilder.chooseUserAgent(NeteaseCryptoMode.API, "pc")

        // 版本号、系统信息与 UA 必须与主网络栈 PC 配置完全一致
        assertEquals(pcProfile.appver, nonNullCtx.appver)
        assertEquals(expectedUa, nonNullCtx.userAgent)
        assertTrue(nonNullCtx.cleanCookieStr.contains("appver=${pcProfile.appver}"))
        assertTrue(nonNullCtx.cleanCookieStr.contains("os=${pcProfile.os}"))
        assertTrue(nonNullCtx.cleanCookieStr.contains("osver=${pcProfile.osver}"))

        // 设备标识必须复用本机持久化身份，不能再为空或写死固定值
        assertTrue(nonNullCtx.cleanCookieStr.contains("deviceId=DEV_52_STABLE_ID"))
        assertTrue(nonNullCtx.cleanCookieStr.contains("_ntes_nuid=nuid_stable_hex"))
        assertTrue(nonNullCtx.cleanCookieStr.contains("_ntes_nnid=nuid_stable_hex,1700000000000"))
        assertTrue(nonNullCtx.cleanCookieStr.contains("WNMCID=abcdef.1700000000000.01.0"))
        assertTrue(nonNullCtx.cleanCookieStr.contains("clientSign=02:AA:BB:CC:DD:EE@@@SERIALHEX@@@@@@HASHHEX"))

        // 加密载荷里的 metaJson 与 Cookie 字段保持一致
        assertTrue(nonNullCtx.metaJson.contains("\"deviceId\":\"DEV_52_STABLE_ID\""))
        assertTrue(nonNullCtx.metaJson.contains("\"_ntes_nuid\":\"nuid_stable_hex\""))
        assertTrue(nonNullCtx.metaJson.contains("\"appver\":\"${pcProfile.appver}\""))
    }

    @Test
    fun ncblContext_returnsNullWithoutMusicU() {
        val fakeStore = object : NeteaseTokenStore {
            override fun getOrCreateDeviceId(): String = "DEV"
            override fun getAnonymousToken(): String? = null
            override fun saveAnonymousToken(token: String) {}
        }

        assertNull(NcblReporter.buildNcblContext(rawCookie = null, tokenStore = fakeStore))
        assertNull(NcblReporter.buildNcblContext(rawCookie = "__csrf=only_csrf; MUSIC_A=guest", tokenStore = fakeStore))
    }

    @Test
    fun generateClientSign_producesPerDeviceUniqueValidFormat() {
        val sign1 = SharedPreferencesNeteaseTokenStore.generateClientSign()
        val sign2 = SharedPreferencesNeteaseTokenStore.generateClientSign()

        assertNotEquals(sign1, sign2)
        assertTrue(sign1.contains("@@@"))
        assertTrue(sign1.contains("@@@@@@"))
        // 不再使用旧版全员写死的硬编码 MAC
        assertFalse(sign1.startsWith("18:C0:4D:B9:8F:FE"))
    }

    @Test
    fun authCookieStore_requiresNonEmptyMusicUForValidLoginState() {
        assertTrue(AuthCookieStore.hasValidLoginCookie("MUSIC_U=valid_token; __csrf=abc"))
        assertFalse(AuthCookieStore.hasValidLoginCookie(null as String?))
        assertFalse(AuthCookieStore.hasValidLoginCookie(""))
        assertFalse(AuthCookieStore.hasValidLoginCookie("__csrf=only_csrf; NMTID=123"))
        assertFalse(AuthCookieStore.hasValidLoginCookie("MUSIC_A=guest_token"))
        assertFalse(AuthCookieStore.hasValidLoginCookie("MUSIC_U=; __csrf=abc"))
    }
}
