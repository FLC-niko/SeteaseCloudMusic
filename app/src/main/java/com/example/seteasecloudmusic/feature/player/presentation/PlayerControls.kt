package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seteasecloudmusic.core.player.PlaybackMode

@Composable
fun PlayerControls(
    currentPositionMs: Int,
    durationMs: Int,
    isPlaying: Boolean,
    playbackMode: PlaybackMode = PlaybackMode.SEQUENTIAL,
    dominantColor: Color = Color.Transparent,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onTogglePlaybackMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val progress = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        // 1. 进度条
        ThinProgressBar(
            progress = progress,
            onSeekTo = onSeekTo,
            durationMs = durationMs
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 2. 时间指示（当前时间 / 剩余时间）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPositionMs),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
            Text(
                text = if (durationMs > 0) "-" + formatTime((durationMs - currentPositionMs).coerceAtLeast(0)) else "--:--",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 核心主控制按键（模式切换、上一首、播放/暂停、下一首、模式提示按钮）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 播放模式切换按钮（顺序循环 / 随机播放）
            IconButton(
                onClick = onTogglePlaybackMode,
                modifier = Modifier.size(46.dp)
            ) {
                Icon(
                    imageVector = if (playbackMode == PlaybackMode.SHUFFLE) Icons.Filled.Shuffle else Icons.Filled.Repeat,
                    contentDescription = if (playbackMode == PlaybackMode.SHUFFLE) "当前：随机播放" else "当前：顺序播放",
                    tint = if (playbackMode == PlaybackMode.SHUFFLE) Color(0xFFFA233B) else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // 上一首
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "上一首",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            // 播放 / 暂停
            val playScale by animateFloatAsState(
                targetValue = if (isPlaying) 1.0f else 1.05f,
                animationSpec = spring(stiffness = 400f),
                label = "playScale"
            )
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            // 下一首
            IconButton(
                onClick = onNext,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "下一首",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            // 模式状态辅助胶囊按钮（点击也可直接切换）
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onTogglePlaybackMode),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (playbackMode == PlaybackMode.SHUFFLE) "随机" else "顺序",
                    color = if (playbackMode == PlaybackMode.SHUFFLE) Color(0xFFFA233B) else Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun ThinProgressBar(
    progress: Float,
    onSeekTo: (Int) -> Unit,
    durationMs: Int
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val displayProgress = if (isDragging) dragProgress else progress

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragProgress = (dragProgress + dragAmount.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeekTo((dragProgress * durationMs).toInt())
                    }
                )
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.CenterStart)
        ) {
            // 背景轨道（白色半透明）
            drawRoundRect(
                color = Color.White.copy(0.3f),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
            // 进度填充（白色）
            drawRoundRect(
                color = Color.White,
                size = Size(displayProgress * size.width, size.height),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
        // 拖拽时显示小圆点 thumb
        if (isDragging) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(displayProgress)
                    .size(14.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

private fun formatTime(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
