package com.example.seteasecloudmusic.core.common

import java.io.EOFException
import java.io.FileNotFoundException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * 友好错误信息结构，用于在界面展示结构化、通俗易懂的提示与行动引导。
 */
data class FriendlyError(
    val title: String,
    val description: String,
    val suggestion: String
)

/**
 * 将异常解析为普通用户能看懂的友好错误提示。
 *
 * 过滤掉如 "Failed to connect to ...", "UnknownHostException", "HTTP 502" 等极客、晦涩的底层技术信息，
 * 转译为通俗、明确的原因和后续操作指引。
 *
 * @param actionDescription 本次操作的业务名称，例如 "获取二维码"、"加载推荐歌单"、"搜索歌曲" 等。
 */
fun Throwable?.toUserFriendlyMessage(actionDescription: String = "操作"): String {
    if (this == null) return "$actionDescription 失败，请稍后重试"

    val rawMessage = this.message.orEmpty()
    val fullText = "${this::class.java.simpleName}: $rawMessage".lowercase()

    // 1. 无网络 / 无法解析主机名 (Airplane mode, Wi-Fi 断开, DNS 失败等)
    if (this is UnknownHostException || fullText.contains("unknownhostexception") || fullText.contains("unable to resolve host")) {
        return "网络未连接，请检查你的 Wi-Fi 或移动数据设置"
    }

    // 2. 连接服务器失败 / 连接被拒 (服务未开启、IP不可达、防火墙拦截等)
    if (this is ConnectException || fullText.contains("connectexception") ||
        fullText.contains("failed to connect") || fullText.contains("fail to connect") ||
        fullText.contains("connection refused")
    ) {
        return "无法连接到服务器，请检查网络连接或稍后重试"
    }

    // 3. 超时 (读写超时、连接超时)
    if (this is SocketTimeoutException || fullText.contains("sockettimeoutexception") ||
        fullText.contains("timed out") || fullText.contains("timeout")
    ) {
        return "网络连接超时，请检查网络信号后重试"
    }

    // 4. 网络路由不可达 / 网络断开
    if (this is NoRouteToHostException || fullText.contains("noroutetohostexception") || fullText.contains("network is unreachable")) {
        return "当前网络不可达，请检查网络设置"
    }

    // 5. Socket 中断 / 管道损坏
    if (this is SocketException || fullText.contains("socketexception") ||
        fullText.contains("broken pipe") || fullText.contains("connection reset") ||
        fullText.contains("connection abort")
    ) {
        return "网络连接不稳定或已中断，请重试"
    }

    // 6. SSL 证书 / 安全握手异常
    if (this is SSLException || fullText.contains("sslexception") || fullText.contains("sslhandshakeexception") || fullText.contains("certpathvalidator")) {
        return "网络安全连接失败，请检查手机系统时间和网络环境"
    }

    // 7. EOF 异常
    if (this is EOFException || fullText.contains("eofexception")) {
        return "网络传输意外中断，请稍后重试"
    }

    // 8. 404 / 文件未找到
    if (this is FileNotFoundException || fullText.contains("http 404") || fullText.contains("code: 404")) {
        return "请求的内容未找到或已被下线"
    }

    // 9. HTTP 401 / 403
    if (fullText.contains("http 401") || fullText.contains("http 403") || fullText.contains("code: 401") || fullText.contains("code: 403")) {
        return "登录状态已失效或暂无权限，请重新登录"
    }

    // 10. HTTP 5xx 服务端错误
    if (fullText.contains("http 500") || fullText.contains("http 502") || fullText.contains("http 503") || fullText.contains("http 504") ||
        fullText.contains("code: 500") || fullText.contains("code: 502") || fullText.contains("code: 503") || fullText.contains("code: 504")
    ) {
        return "服务器开小差了，请稍后再试"
    }

    // 11. 常见业务错误转译
    if (fullText.contains("no playable url")) {
        return "暂无可用音源播放"
    }
    if (fullText.contains("track is not playable")) {
        return "该歌曲暂时无法播放"
    }
    if (fullText.contains("api error code") || fullText.contains("api error")) {
        return "服务响应异常，请稍后重试"
    }

    // 12. 如果原始异常信息中含有中文，且不包含技术性的异常栈类名，则优先使用原生的中文业务提示
    val containsChinese = rawMessage.any { it in '\u4e00'..'\u9fa5' }
    val containsTechnicalNoise = rawMessage.contains("Exception") ||
            rawMessage.contains("http") ||
            rawMessage.contains("/") ||
            rawMessage.contains("failed to", ignoreCase = true) ||
            rawMessage.contains("error code", ignoreCase = true)

    if (containsChinese && !containsTechnicalNoise) {
        return rawMessage
    }

    // 13. 兜底友善信息
    return "$actionDescription 失败，请检查网络后重试"
}

/**
 * 将可能包含极客/底层异常的原始字符串转换为用户友好的提示信息。
 */
fun String?.toUserFriendlyMessage(fallbackAction: String = "操作"): String {
    if (this.isNullOrBlank()) return "$fallbackAction 失败，请稍后重试"

    val lower = this.lowercase()
    if (lower.contains("failed to connect") || lower.contains("fail to connect") || lower.contains("connection refused")) {
        return "无法连接到服务器，请检查网络连接或稍后重试"
    }
    if (lower.contains("unable to resolve host") || lower.contains("unknownhostexception")) {
        return "网络未连接，请检查你的 Wi-Fi 或移动数据设置"
    }
    if (lower.contains("timed out") || lower.contains("timeout")) {
        return "网络连接超时，请检查网络后重试"
    }
    if (lower.contains("network is unreachable") || lower.contains("noroutetohost")) {
        return "网络不可达，请检查设备网络设置"
    }
    if (lower.contains("http 401") || lower.contains("http 403")) {
        return "登录状态已失效，请重新登录"
    }
    if (lower.contains("http 404")) {
        return "请求的内容未找到"
    }
    if (lower.contains("http 50") || lower.contains("502 bad gateway") || lower.contains("500 internal")) {
        return "服务器开小差了，请稍后重试"
    }
    if (lower.contains("api error code") || lower.contains("api error")) {
        return "服务响应异常，请稍后重试"
    }
    if (lower.contains("no playable url")) {
        return "暂无可用音源播放"
    }

    val containsChinese = this.any { it in '\u4e00'..'\u9fa5' }
    val containsTechnicalNoise = this.contains("Exception") ||
            this.contains("http://", ignoreCase = true) ||
            this.contains("https://", ignoreCase = true) ||
            this.contains("failed to", ignoreCase = true) ||
            this.contains("fail to", ignoreCase = true)

    if (containsChinese && !containsTechnicalNoise) {
        return this
    }

    return "$fallbackAction 失败，请检查网络后重试"
}

/**
 * 解析为包含标题、详细说明和行动建议的友好错误模型。
 */
fun Throwable?.toFriendlyError(actionDescription: String = "加载内容"): FriendlyError {
    val userMsg = this.toUserFriendlyMessage(actionDescription)
    val isNetworkIssue = this is UnknownHostException ||
            this is ConnectException ||
            this is SocketTimeoutException ||
            this is NoRouteToHostException ||
            this is SocketException ||
            (this?.message.orEmpty().lowercase().let {
                it.contains("connect") || it.contains("host") || it.contains("timeout") || it.contains("network")
            })

    return if (isNetworkIssue) {
        FriendlyError(
            title = "网络连接异常",
            description = userMsg,
            suggestion = "请检查手机是否开启 Wi-Fi 或移动数据，然后点击重试"
        )
    } else {
        FriendlyError(
            title = "$actionDescription 失败",
            description = userMsg,
            suggestion = "请稍后点击下方按钮重试"
        )
    }
}
