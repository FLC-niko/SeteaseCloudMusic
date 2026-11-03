package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seteasecloudmusic.core.player.PlaybackMode
import com.example.seteasecloudmusic.core.settings.OnlineAudioQuality

@Composable
fun PlayerControls(
    currentPositionMs: Int,
    durationMs: Int,
    isPlaying: Boolean,
    playbackMode: PlaybackMode = PlaybackMode.SEQUENTIAL,
    isLocalTrack: Boolean = false,
    currentQuality: OnlineAudioQuality = OnlineAudioQuality.STANDARD,
    dominantColor: Color = Color.Transparent,
    hasTrack: Boolean = true,
    isLyricsPage: Boolean = false,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onTogglePlaybackMode: () -> Unit = {},
    onSelectQualityClick: () -> Unit = {},
    onToggleLyrics: (() -> Unit)? = null,
    onQueueClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val rawProgress = if (hasTrack && durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val displayProgress = if (isDragging && hasTrack) dragProgress else rawProgress
    val displayCurrentMs = if (isDragging && hasTrack) {
        (dragProgress * durationMs).toInt().coerceIn(0, durationMs)
    } else if (hasTrack) {
        currentPositionMs
    } else {
        0
    }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val narrow = maxWidth < 340.dp
        val modeButton: @Composable () -> Unit = {
            IconButton(onTogglePlaybackMode, enabled = hasTrack, modifier = Modifier.size(48.dp)) {
                Icon(if (playbackMode == PlaybackMode.SHUFFLE) Icons.Filled.Shuffle else Icons.Filled.Repeat,
                    "切换播放模式", tint = Color.White.copy(alpha = if (hasTrack) 0.72f else 0.25f), modifier = Modifier.size(22.dp))
            }
        }
        val qualityButton: @Composable () -> Unit = {
            IconButton(onSelectQualityClick, enabled = hasTrack && !isLocalTrack, modifier = Modifier.size(48.dp)) {
                Text(if (isLocalTrack) "本地" else currentQuality.title, color = Color.White.copy(alpha = if (hasTrack) 0.72f else 0.25f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = if (narrow) 12.dp else 24.dp, vertical = 8.dp)) {
            SmoothProgressBar(displayProgress, isDragging, durationMs, hasTrack && durationMs > 0, onSeekTo) { dragging, fraction ->
                isDragging = dragging
                dragProgress = fraction
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (hasTrack) formatTime(displayCurrentMs) else "00:00", color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
                Text(if (hasTrack && durationMs > 0) "-" + formatTime((durationMs - displayCurrentMs).coerceAtLeast(0)) else "--:--", color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                if (!narrow) modeButton()
                IconButton(onPrevious, enabled = hasTrack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.SkipPrevious, "上一首", Modifier.size(32.dp), tint = Color.White.copy(alpha = if (hasTrack) 1f else 0.25f))
                }
                IconButton(onPlayPause, enabled = hasTrack, modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))) {
                    Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (isPlaying) "暂停" else "播放", Modifier.size(36.dp), tint = Color.White.copy(alpha = if (hasTrack) 1f else 0.25f))
                }
                IconButton(onNext, enabled = hasTrack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.SkipNext, "下一首", Modifier.size(32.dp), tint = Color.White.copy(alpha = if (hasTrack) 1f else 0.25f))
                }
                if (!narrow) qualityButton()
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                if (narrow) modeButton()
                IconButton({ onToggleLyrics?.invoke() }, enabled = hasTrack && onToggleLyrics != null,
                    modifier = Modifier.size(48.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).background(if (isLyricsPage) Color.White.copy(alpha = 0.12f) else Color.Transparent)) {
                    Icon(Icons.Filled.ChatBubble, "歌词", Modifier.size(21.dp), tint = Color.White.copy(alpha = if (hasTrack) 0.75f else 0.25f))
                }
                if (narrow) qualityButton()
                else Icon(Icons.Filled.Headphones, "音频输出", Modifier.size(21.dp), tint = Color.White.copy(alpha = 0.35f))
                IconButton({ onQueueClick?.invoke() }, enabled = onQueueClick != null, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, "待播清单", Modifier.size(23.dp), tint = Color.White.copy(alpha = 0.75f))
                }
            }
        }
    }

}

/**
 * 极简丝滑进度条：
 * 1. 扩大点击与拖拽热区至 32dp，零死角响应触摸与任意位置点击即跳；
 * 2. 使用底层手势队列实时监听触点变化，手指滑动实时渲染；
 * 3. 拖拽触碰时轨道高度微动增强视觉反馈。
 */
@Composable
private fun SmoothProgressBar(
    progress: Float,
    isDragging: Boolean,
    durationMs: Int,
    enabled: Boolean = true,
    onSeekTo: (Int) -> Unit,
    onDragUpdate: (Boolean, Float) -> Unit
) {
    val trackHeight = if (isDragging && enabled) 6.dp else 4.dp
    val thumbRadius = if (isDragging && enabled) 8.dp else 5.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .then(
                if (enabled && durationMs > 0) {
                    Modifier.pointerInput(durationMs) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val width = size.width.toFloat()
                            if (width <= 0f) return@awaitEachGesture

                            val initialFraction = (down.position.x / width).coerceIn(0f, 1f)
                            onDragUpdate(true, initialFraction)

                            var currentFraction = initialFraction
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (change.changedToUp()) {
                                    val finalFraction = (change.position.x / width).coerceIn(0f, 1f)
                                    onDragUpdate(false, finalFraction)
                                    onSeekTo((finalFraction * durationMs).toInt())
                                    change.consume()
                                    break
                                } else {
                                    currentFraction = (change.position.x / width).coerceIn(0f, 1f)
                                    onDragUpdate(true, currentFraction)
                                    change.consume()
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(trackHeight)) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val activeWidth = canvasWidth * progress.coerceIn(0f, 1f)

            // 背景底轨
            drawRoundRect(
                color = Color.White.copy(alpha = if (enabled) 0.25f else 0.12f),
                topLeft = Offset(0f, 0f),
                size = Size(canvasWidth, canvasHeight),
                cornerRadius = CornerRadius(canvasHeight / 2, canvasHeight / 2)
            )

            // 激活进度轨
            if (activeWidth > 0f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = if (enabled) 1f else 0.3f),
                    topLeft = Offset(0f, 0f),
                    size = Size(activeWidth, canvasHeight),
                    cornerRadius = CornerRadius(canvasHeight / 2, canvasHeight / 2)
                )
            }

            // 拖拽手柄小圆点（不可用时隐藏，避免暗示可拖动）
            if (enabled) {
                drawCircle(
                    color = Color.White,
                    radius = thumbRadius.toPx(),
                    center = Offset(activeWidth, canvasHeight / 2)
                )
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
