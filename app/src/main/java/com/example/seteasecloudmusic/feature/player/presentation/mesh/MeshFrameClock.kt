package com.example.seteasecloudmusic.feature.player.presentation.mesh

import androidx.compose.runtime.withFrameNanos
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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

