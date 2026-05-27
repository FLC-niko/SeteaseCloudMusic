package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SpeakerGroup
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlayerControls(
    currentPositionMs: Int,
    durationMs: Int,
    isPlaying: Boolean,
    dominantColor: Color,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onLyricsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 8.dp)
            .navigationBarsPadding()
    ) {
        // ── 进度条区域 ──
        AppleMusicProgressBar(
            progress = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f,
            onSeekTo = onSeekTo,
            durationMs = durationMs,
            currentPositionMs = currentPositionMs
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── 主控制按钮行（Shuffle / ⏮ / ▶⏸ / ⏭ / Repeat）──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle 随机播放
            IconButton(
                onClick = { /* TODO: 随机播放功能 */ },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = "随机播放",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }

            // ⏮ 上一曲
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "上一曲",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            // ▶/⏸ 播放暂停（无白色圆背景，直接用大图标）
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                )
            }

            // ⏭ 下一曲
            IconButton(
                onClick = onNext,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "下一曲",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            // Repeat 循环
            IconButton(
                onClick = { /* TODO: 循环模式切换 */ },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Repeat,
                    contentDescription = "循环播放",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── 音量条 ──
        AppleMusicVolumeBar()

        Spacer(modifier = Modifier.height(24.dp))

        // ── 底部功能按钮行（歌词 / AirPlay设备 / 播放列表）──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 歌词图标
            IconButton(
                onClick = onLyricsClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Article,
                    contentDescription = "歌词",
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(22.dp)
                )
            }

            // AirPlay 设备图标
            IconButton(
                onClick = { /* TODO: 设备功能 */ },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SpeakerGroup,
                    contentDescription = "播放设备",
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(22.dp)
                )
            }

            // 播放列表图标
            IconButton(
                onClick = { /* TODO: 播放列表功能 */ },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "播放列表",
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── Apple Music 风格进度条 ──
@Composable
private fun AppleMusicProgressBar(
    progress: Float,
    onSeekTo: (Int) -> Unit,
    durationMs: Int,
    currentPositionMs: Int
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val displayProgress = if (isDragging) dragProgress else progress

    // 拖拽时进度条变粗
    val barHeight by animateDpAsState(
        targetValue = if (isDragging) 7.dp else 4.dp,
        animationSpec = spring(stiffness = 500f),
        label = "bar_height"
    )

    Column {
        // 进度条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val tapped = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeekTo((tapped * durationMs).toInt())
                    }
                }
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
                    .height(barHeight)
                    .align(Alignment.CenterStart)
            ) {
                val cornerPx = size.height / 2f
                // 背景轨道
                drawRoundRect(
                    color = Color.White.copy(0.2f),
                    cornerRadius = CornerRadius(cornerPx)
                )
                // 进度填充
                if (displayProgress > 0f) {
                    drawRoundRect(
                        color = Color.White.copy(0.9f),
                        size = Size(displayProgress * size.width, size.height),
                        cornerRadius = CornerRadius(cornerPx)
                    )
                }
            }

            // 拖拽时显示 thumb
            if (isDragging) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(displayProgress.coerceIn(0.01f, 1f))
                        .align(Alignment.CenterStart)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(Color.White, CircleShape)
                            .align(Alignment.CenterEnd)
                    )
                }
            }
        }

        // 时间标签
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayTime = if (isDragging) (dragProgress * durationMs).toInt() else currentPositionMs
            Text(
                text = formatTime(displayTime),
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.2).sp
            )
            Text(
                text = "-${formatTime((durationMs - displayTime).coerceAtLeast(0))}",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.2).sp
            )
        }
    }
}

// ── Apple Music 风格音量条 ──
@Composable
private fun AppleMusicVolumeBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeDown,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(1.5.dp))
        ) {
            // 默认 50% 音量指示
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(1.5.dp))
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun formatTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
