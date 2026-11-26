package com.example.seteasecloudmusic.feature.player.presentation.mesh

import androidx.compose.runtime.withFrameNanos
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * 基于 Compose `withFrameNanos` 的网格动画节流帧循环。
 *
 * @param intervalNanos 目标帧间隔（ns，例如 33_333_333 对应约 30 fps；越大越省电）
 * @param needsAnimation 每帧检查是否仍需继续调度动画
 */
internal suspend fun runMeshFrames(
    intervalNanos: Long,
    needsAnimation: () -> Boolean,
    nanoTime: () -> Long = System::nanoTime,
    onFrame: (Long) -> Unit,
) {
    require(intervalNanos > 0L)
    var nextFrameNanos: Long? = null
    while (coroutineContext.isActive && needsAnimation()) {
        withFrameNanos { frameTime ->
            val now = nanoTime()
            var deadline = (nextFrameNanos ?: now) + intervalNanos
            // 掉帧超过一个周期时向前对齐到下一节拍，避免补帧追赶
            if (now - deadline >= intervalNanos) {
                deadline += ((now - deadline) / intervalNanos + 1L) * intervalNanos
            }
            nextFrameNanos = deadline
            onFrame(frameTime)
        }
        if (!coroutineContext.isActive || !needsAnimation()) return
        val remainingNanos = checkNotNull(nextFrameNanos) - nanoTime()
        delay((remainingNanos / 1_000_000L).coerceAtLeast(0L))
    }
}

