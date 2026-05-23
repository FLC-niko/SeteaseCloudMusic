package com.example.seteasecloudmusic.feature.player.presentation

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

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
            .background(dominantColor)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .navigationBarsPadding()
    ) {
        // ── 进度条区域 ──
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
                text = formatTime(durationMs),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 自定义细线进度条
        ThinProgressBar(
            progress = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f,
            onSeekTo = onSeekTo,
            durationMs = durationMs
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── 主控制按钮行（⏮ / ▶⏸ / ⏭）──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ⏮ 上一曲
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "上一曲",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // ▶/⏸ 播放暂停（大白圆）
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.White, CircleShape)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = dominantColor,
                    modifier = Modifier.size(36.dp)
                )
            }

            // ⏭ 下一曲
            IconButton(
                onClick = onNext,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "下一曲",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── 音量条（简化版）──
        VolumeBar()

        Spacer(modifier = Modifier.height(12.dp))

        // ── 底部功能按钮行（歌词 / 设备 / 播放列表）──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 歌词图标（类似信息符号，中间有单边双引号 "）
            IconButton(
                onClick = onLyricsClick,
                modifier = Modifier.size(40.dp)
            ) {
                Canvas(modifier = Modifier.size(24.dp)) {
                    // 绘制圆角矩形（类似对话框/信息框）
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.7f),
                        cornerRadius = CornerRadius(5.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // 绘制左双引号 " （两个逗号形状上下排列）
                    // 上引号
                    drawCircle(
                        color = Color.White.copy(0.7f),
                        radius = 2.dp.toPx(),
                        center = Offset(size.width * 0.4f, size.height * 0.38f)
                    )
                    // 下引号
                    drawCircle(
                        color = Color.White.copy(0.7f),
                        radius = 2.dp.toPx(),
                        center = Offset(size.width * 0.4f, size.height * 0.58f)
                    )
                    // 连接尾巴（向右下弯曲）
                    drawLine(
                        color = Color.White.copy(0.7f),
                        start = Offset(size.width * 0.4f, size.height * 0.38f),
                        end = Offset(size.width * 0.55f, size.height * 0.28f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(0.7f),
                        start = Offset(size.width * 0.4f, size.height * 0.58f),
                        end = Offset(size.width * 0.55f, size.height * 0.48f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }

            // 设备图标（带声波的喇叭）
            IconButton(
                onClick = { /* TODO: 设备功能 */ },
                modifier = Modifier.size(40.dp)
            ) {
                Canvas(modifier = Modifier.size(24.dp)) {
                    // 绘制喇叭主体（梯形）
                    val path = Path().apply {
                        moveTo(size.width * 0.2f, size.height * 0.35f)
                        lineTo(size.width * 0.45f, size.height * 0.35f)
                        lineTo(size.width * 0.7f, size.height * 0.15f)
                        lineTo(size.width * 0.7f, size.height * 0.85f)
                        lineTo(size.width * 0.45f, size.height * 0.65f)
                        lineTo(size.width * 0.2f, size.height * 0.65f)
                        close()
                    }
                    drawPath(path, Color.White.copy(0.7f))
                    // 绘制声波线
                    drawArc(
                        color = Color.White.copy(0.7f),
                        startAngle = -50f,
                        sweepAngle = 100f,
                        useCenter = false,
                        style = Stroke(1.5.dp.toPx()),
                        topLeft = Offset(size.width * 0.65f, size.height * 0.2f),
                        size = Size(size.width * 0.3f, size.height * 0.6f)
                    )
                    drawArc(
                        color = Color.White.copy(0.5f),
                        startAngle = -50f,
                        sweepAngle = 100f,
                        useCenter = false,
                        style = Stroke(1.5.dp.toPx()),
                        topLeft = Offset(size.width * 0.75f, size.height * 0.1f),
                        size = Size(size.width * 0.3f, size.height * 0.8f)
                    )
                }
            }

            // 播放列表图标（带横线的圆角矩形）
            IconButton(
                onClick = { /* TODO: 播放列表功能 */ },
                modifier = Modifier.size(40.dp)
            ) {
                Canvas(modifier = Modifier.size(24.dp)) {
                    // 绘制圆角矩形边框
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.7f),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // 绘制内部横线（列表项）
                    val lineStartX = size.width * 0.25f
                    val lineEndX = size.width * 0.75f
                    drawLine(
                        color = Color.White.copy(0.7f),
                        start = Offset(lineStartX, size.height * 0.35f),
                        end = Offset(lineEndX, size.height * 0.35f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(0.7f),
                        start = Offset(lineStartX, size.height * 0.55f),
                        end = Offset(lineEndX, size.height * 0.55f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(0.7f),
                        start = Offset(lineStartX, size.height * 0.75f),
                        end = Offset(lineEndX, size.height * 0.75f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
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
                .height(3.dp)
                .align(Alignment.CenterStart)
        ) {
            // 背景轨道（白色半透明）
            drawRoundRect(
                color = Color.White.copy(0.3f),
                cornerRadius = CornerRadius(1.5.dp.toPx())
            )
            // 进度填充（白色）
            drawRoundRect(
                color = Color.White,
                size = Size(displayProgress * size.width, size.height),
                cornerRadius = CornerRadius(1.5.dp.toPx())
            )
        }
        // 拖拽时显示小圆点 thumb
        if (isDragging) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(displayProgress)
                    .size(12.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

@Composable
private fun VolumeBar() {
    // 简化版音量条，可根据需要扩展
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(1.5.dp))
    )
}

private fun formatTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
