package com.example.seteasecloudmusic.core.common

import com.example.seteasecloudmusic.core.network.ApiHttpException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorMessageResolverTest {

    @Test
    fun apiHttp400RiskControl_showsRiskMessageInsteadOfNetworkPrompt() {
        val err = ApiHttpException(
            httpStatusCode = 400,
            apiErrorCode = -460,
            apiMessage = "检测到您的网络环境存在风险，请稍后再试",
            message = "HTTP 400 (code=-460): 检测到您的网络环境存在风险，请稍后再试"
        )

        assertEquals("检测到您的网络环境存在风险，请稍后再试", err.toUserFriendlyMessage("播放歌曲"))
        val friendly = err.toFriendlyError("播放歌曲")
        assertEquals("播放歌曲 失败", friendly.title)
        assertFalse(friendly.suggestion.contains("Wi-Fi"))
    }

    @Test
    fun apiHttp400WithoutChineseBody_doesNotMisreportAsNetworkError() {
        val err = ApiHttpException(
            httpStatusCode = 400,
            apiErrorCode = 1001,
            apiMessage = "Bad parameter",
            message = "HTTP 400 (code=1001): Bad parameter"
        )

        assertEquals("播放歌曲 失败，请稍后重试", err.toUserFriendlyMessage("播放歌曲"))
        assertEquals("播放歌曲 失败，请稍后重试", "HTTP 400 Bad Request".toUserFriendlyMessage("播放歌曲"))
    }

    @Test
    fun networkExceptions_stillReportNetworkGuidance() {
        assertEquals(
            "网络连接超时，请检查网络信号后重试",
            SocketTimeoutException("timeout").toUserFriendlyMessage("播放歌曲")
        )
        assertEquals(
            "网络未连接，请检查你的 Wi-Fi 或移动数据设置",
            UnknownHostException("57.158.26.135").toUserFriendlyMessage("播放歌曲")
        )
    }
}
