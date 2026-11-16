package com.example.seteasecloudmusic.feature.search.data

import com.example.seteasecloudmusic.core.network.ApiHttpException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.net.SocketTimeoutException

class SongUrlFallbackTest {

    @Test
    fun resolveSongUrlWithFallback_succeedsImmediatelyWithWeapi() = runBlocking {
        val attempted = mutableListOf<String?>()
        var legacyCalled = false

        val response = SearchRepositoryImpl.resolveSongUrlWithFallback(
            fetchSongUrlV1 = { crypto ->
                attempted += crypto
                songResponse(code = 200, url = "http://m801.music.126.net/ok.flac")
            },
            fetchLegacySongUrl = {
                legacyCalled = true
                songResponse(code = 200, url = "http://m801.music.126.net/legacy.mp3")
            }
        )

        assertEquals(listOf("weapi"), attempted)
        assertEquals(false, legacyCalled)
        assertEquals("http://m801.music.126.net/ok.flac", response.data.first().url)
    }

    @Test
    fun resolveSongUrlWithFallback_fallsBackToApiOn460RiskControl() = runBlocking {
        val attempted = mutableListOf<String?>()

        val response = SearchRepositoryImpl.resolveSongUrlWithFallback(
            fetchSongUrlV1 = { crypto ->
                attempted += crypto
                if (crypto == "weapi") {
                    throw riskControlException()
                }
                songResponse(code = 200, url = "http://m801.music.126.net/api.flac")
            },
            fetchLegacySongUrl = {
                error("legacy should not be called")
            }
        )

        assertEquals(listOf("weapi", "api"), attempted)
        assertEquals("http://m801.music.126.net/api.flac", response.data.first().url)
    }

    @Test
    fun resolveSongUrlWithFallback_fallsBackToLegacyWhenAllV1Fail() = runBlocking {
        val attempted = mutableListOf<String?>()
        var legacyCalled = false

        val response = SearchRepositoryImpl.resolveSongUrlWithFallback(
            fetchSongUrlV1 = { crypto ->
                attempted += crypto
                throw riskControlException()
            },
            fetchLegacySongUrl = {
                legacyCalled = true
                songResponse(code = 200, url = "http://m801.music.126.net/legacy.mp3")
            }
        )

        assertEquals(listOf("weapi", "api", null), attempted)
        assertEquals(true, legacyCalled)
        assertEquals("http://m801.music.126.net/legacy.mp3", response.data.first().url)
    }

    @Test
    fun resolveSongUrlWithFallback_failsFastOnConnectivityTimeout() = runBlocking {
        val attempted = mutableListOf<String?>()

        try {
            SearchRepositoryImpl.resolveSongUrlWithFallback(
                fetchSongUrlV1 = { crypto ->
                    attempted += crypto
                    throw SocketTimeoutException("read timed out")
                },
                fetchLegacySongUrl = {
                    error("legacy should not be called")
                }
            )
            fail("Expected SocketTimeoutException")
        } catch (_: SocketTimeoutException) {
            assertEquals(listOf("weapi"), attempted)
        }
    }

    @Test
    fun resolveSongUrlWithFallback_doesNotRetryWhenTrackIsCleanlyUnplayable200() = runBlocking {
        val attempted = mutableListOf<String?>()

        val response = SearchRepositoryImpl.resolveSongUrlWithFallback(
            fetchSongUrlV1 = { crypto ->
                attempted += crypto
                songResponse(code = 200, url = null)
            },
            fetchLegacySongUrl = {
                error("legacy should not be called")
            }
        )

        assertEquals(listOf("weapi"), attempted)
        assertEquals(null, response.data.first().url)
    }

    private fun songResponse(code: Int, url: String?) = SongResponse(
        data = listOf(SongUrlItemResponse(id = 27562048L, url = url, code = code)),
        code = code
    )

    private fun riskControlException() = ApiHttpException(
        httpStatusCode = 400,
        apiErrorCode = -460,
        apiMessage = "检测到您的网络环境存在风险，请稍后再试",
        message = "HTTP 400 (code=-460): 检测到您的网络环境存在风险，请稍后再试"
    )
}
