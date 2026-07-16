package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.example.seteasecloudmusic.core.player.QueueRepeatMode
import kotlinx.coroutines.launch

@Composable
fun PlayerControls(
    currentPositionMs: Int,
    durationMs: Int,
    isPlaying: Boolean,
    dominantColor: Color,
    shuffleEnabled: Boolean,
    repeatMode: QueueRepeatMode,
    volume: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onDeviceClick: () -> Unit,
    onQueueClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLyricsClick: () -> Unit = {},
    lyricsActive: Boolean = false
) {
    val functionIconTint = Color.White.copy(alpha = 0.65f)
    val activeFunctionIconTint = Color.White
    val activeFunctionBackground = dominantColor.copy(alpha = 0.28f)
    val shuffleTint by animateColorAsState(
        targetValue = if (shuffleEnabled) activeFunctionIconTint else Color.White.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 180),
        label = "shuffle_tint"
    )
    val repeatTint by animateColorAsState(
        targetValue = if (repeatMode == QueueRepeatMode.OFF) {
            Color.White.copy(alpha = 0.5f)
        } else {
            activeFunctionIconTint
        },
        animationSpec = tween(durationMillis = 180),
        label = "repeat_tint"
    )

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
            AppleMusicIconButton(
                onClick = onShuffleClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = if (shuffleEnabled) "关闭随机播放" else "开启随机播放",
                    tint = shuffleTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            // ⏮ 上一曲
            AppleMusicIconButton(
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
            AppleMusicIconButton(
                onClick = onPlayPause,
                pressedScale = 0.86f,
                modifier = Modifier.size(64.dp)
            ) {
                AnimatedPlayPauseIcon(
                    isPlaying = isPlaying,
                    size = 54.dp
                )
            }

            // ⏭ 下一曲
            AppleMusicIconButton(
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
            AppleMusicIconButton(
                onClick = onRepeatClick,
                modifier = Modifier.size(32.dp)
            ) {
                AnimatedContent(
                    targetState = repeatMode,
                    transitionSpec = {
                        (fadeIn(tween(150)) + scaleIn(tween(180), initialScale = 0.68f))
                            .togetherWith(fadeOut(tween(90)) + scaleOut(tween(110), targetScale = 1.18f))
                    },
                    contentAlignment = Alignment.Center,
                    label = "repeat_mode_icon"
                ) { mode ->
                    Icon(
                        imageVector = if (mode == QueueRepeatMode.ONE) {
                            Icons.Filled.RepeatOne
                        } else {
                            Icons.Filled.Repeat
                        },
                        contentDescription = when (mode) {
                            QueueRepeatMode.OFF -> "开启列表循环"
                            QueueRepeatMode.ALL -> "开启单曲循环"
                            QueueRepeatMode.ONE -> "关闭循环播放"
                        },
                        tint = repeatTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── 音量条 ──
        AppleMusicVolumeBar(
            volume = volume,
            onVolumeChange = onVolumeChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── 底部功能按钮行（歌词 / AirPlay设备 / 播放列表）──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 歌词图标
            AppleMusicIconButton(
                onClick = onLyricsClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (lyricsActive) activeFunctionBackground else Color.Transparent,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Article,
                    contentDescription = "歌词",
                    tint = if (lyricsActive) activeFunctionIconTint else functionIconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            // AirPlay 设备图标
            AppleMusicIconButton(
                onClick = onDeviceClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SpeakerGroup,
                    contentDescription = "播放设备",
                    tint = functionIconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 播放列表图标
            AppleMusicIconButton(
                onClick = onQueueClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "播放列表",
                    tint = functionIconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** Apple Music 风格的按压反馈：按下迅速收缩，松手后用低阻尼弹簧回弹。 */
@Composable
fun AppleMusicIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pressedScale: Float = 0.90f,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedStateScale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = if (isPressed) 900f else 560f
        ),
        label = "apple_music_button_scale"
    )
    val clickPulseScale = remember { Animatable(1f) }
    val animationScope = rememberCoroutineScope()
    val alpha by animateFloatAsState(
        targetValue = when {
            !enabled -> 0.38f
            isPressed -> 0.72f
            else -> 1f
        },
        animationSpec = spring(stiffness = 900f),
        label = "apple_music_button_alpha"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                val combinedScale = pressedStateScale * clickPulseScale.value
                scaleX = combinedScale
                scaleY = combinedScale
                this.alpha = alpha
            }
            .clickable(
                enabled = enabled,
                role = Role.Button,
                indication = null,
                interactionSource = interactionSource,
                onClick = {
                    animationScope.launch {
                        clickPulseScale.snapTo(1f)
                        clickPulseScale.animateTo(
                            targetValue = pressedScale,
                            animationSpec = tween(
                                durationMillis = 65,
                                easing = FastOutLinearInEasing
                            )
                        )
                        clickPulseScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = 0.58f,
                                stiffness = 520f
                            )
                        )
                    }
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun AnimatedPlayPauseIcon(
    isPlaying: Boolean,
    size: Dp,
    tint: Color = Color.White
) {
    AnimatedContent(
        targetState = isPlaying,
        transitionSpec = {
            (fadeIn(
                animationSpec = tween(
                    durationMillis = 170,
                    delayMillis = 35,
                    easing = FastOutSlowInEasing
                )
            ) + scaleIn(
                initialScale = 0.58f,
                animationSpec = tween(
                    durationMillis = 210,
                    delayMillis = 20,
                    easing = FastOutSlowInEasing
                )
            )).togetherWith(
                fadeOut(animationSpec = tween(durationMillis = 100)) +
                    scaleOut(
                        targetScale = 1.24f,
                        animationSpec = tween(
                            durationMillis = 135,
                            easing = FastOutSlowInEasing
                        )
                    )
            )
        },
        contentAlignment = Alignment.Center,
        label = "play_pause_icon_transition"
    ) { playing ->
        Icon(
            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (playing) "暂停" else "播放",
            tint = tint,
            modifier = Modifier.size(size)
        )
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
    val displayProgress = if (isDragging) dragProgress else progress.coerceIn(0f, 1f)

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
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(displayProgress, 0f..1f, 0)
                    stateDescription = "已播放 ${formatTime(currentPositionMs)}，共 ${formatTime(durationMs)}"
                    setProgress { target ->
                        if (durationMs <= 0) return@setProgress false
                        onSeekTo((target.coerceIn(0f, 1f) * durationMs).toInt())
                        true
                    }
                }
                .pointerInput(durationMs, onSeekTo) {
                    detectTapGestures { offset ->
                        if (durationMs > 0) {
                            val tapped = (offset.x / size.width).coerceIn(0f, 1f)
                            onSeekTo((tapped * durationMs).toInt())
                        }
                    }
                }
                .pointerInput(durationMs, onSeekTo) {
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
                            if (durationMs > 0) {
                                onSeekTo((dragProgress * durationMs).toInt())
                            }
                        },
                        onDragCancel = { isDragging = false }
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
private fun AppleMusicVolumeBar(
    volume: Float,
    onVolumeChange: (Float) -> Unit
) {
    var isAdjusting by remember { mutableStateOf(false) }
    val safeVolume = volume.coerceIn(0f, 1f)
    val barHeight by animateDpAsState(
        targetValue = if (isAdjusting) 7.dp else 3.dp,
        animationSpec = spring(stiffness = 600f),
        label = "volume_bar_height"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppleMusicIconButton(
            onClick = { onVolumeChange(0f) },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                contentDescription = "静音",
                tint = Color.White.copy(alpha = 0.42f),
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(safeVolume, 0f..1f, 20)
                    stateDescription = "音量 ${(safeVolume * 100).toInt()}%"
                    setProgress { target ->
                        onVolumeChange(target.coerceIn(0f, 1f))
                        true
                    }
                }
                .pointerInput(onVolumeChange) {
                    detectTapGestures { offset ->
                        onVolumeChange((offset.x / size.width).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(onVolumeChange) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isAdjusting = true
                            onVolumeChange((offset.x / size.width).coerceIn(0f, 1f))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            onVolumeChange((change.position.x / size.width).coerceIn(0f, 1f))
                        },
                        onDragEnd = { isAdjusting = false },
                        onDragCancel = { isAdjusting = false }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .background(Color.White.copy(alpha = 0.20f), RoundedCornerShape(50))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(safeVolume)
                    .height(barHeight)
                    .background(Color.White.copy(alpha = 0.62f), RoundedCornerShape(50))
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        AppleMusicIconButton(
            onClick = { onVolumeChange(1f) },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "最大音量",
                tint = Color.White.copy(alpha = 0.42f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun formatTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
