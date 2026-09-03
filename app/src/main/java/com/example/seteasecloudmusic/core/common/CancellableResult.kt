package com.example.seteasecloudmusic.core.common

import kotlinx.coroutines.CancellationException

/**
 * 将普通异常转换为 Result，但保留协程取消信号。
 *
 * Kotlin 标准库的 [runCatching] 会把 CancellationException 也转换成失败结果，
 * 这会让已经离开页面的请求继续执行并回写状态。
 */
suspend inline fun <T> runCatchingCancellable(
    crossinline block: suspend () -> T
): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
