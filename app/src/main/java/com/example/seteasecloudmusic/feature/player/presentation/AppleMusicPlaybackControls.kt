package com.example.seteasecloudmusic.feature.player.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.seteasecloudmusic.core.player.PlaybackMode
import com.example.seteasecloudmusic.core.settings.OnlineAudioQuality
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 1:1 复刻截图 Apple Music 底部播放控制器：
 * 1. 无滑块圆角胶囊进度条（7dp -> 12dp 按压弹性膨胀、左已播时间、中 Dolby Atmos 位置可点击切换音质、右剩余时间 `-3:48`）
 * 2. 核心三键（双圆角三角 520ms 跑马切歌动效、圆角双竖条暂停 ↔ 圆角三角播放收缩弹性变形）
 * 3. 系统级实时同步胶囊音量条（左无波纹小喇叭 + 按压变粗胶囊滑轨 + 右三波纹大喇叭）
 * 4. 底部三功能键（激活态圆形镂空歌词按钮、3 环 AirPlay 音频输出切换、待播列表）
 */
@Composable
fun AppleMusicPlaybackControls(
    currentPositionMs: Int,
    durationMs: Int,
    isPlaying: Boolean,
    isLyricsPage: Boolean,
    hasTrack: Boolean,
    currentAudioQuality: OnlineAudioQuality = OnlineAudioQuality.DOLBY,
    isLocalTrack: Boolean = false,
    playbackMode: PlaybackMode = PlaybackMode.SEQUENTIAL,
    onAudioQualityClick: () -> Unit = {},
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onToggleLyrics: () -> Unit,
    onTogglePlaybackMode: () -> Unit = {},
    onQueueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
    ) {
        AppleMusicProgressSlider(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            hasTrack = hasTrack,
            currentAudioQuality = currentAudioQuality,
            isLocalTrack = isLocalTrack,
            onAudioQualityClick = onAudioQualityClick,
            onSeekTo = onSeekTo
        )

        Spacer(modifier = Modifier.height(18.dp))

        AppleMusicMainTransportButtons(
            isPlaying = isPlaying,
            hasTrack = hasTrack,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious
        )

        Spacer(modifier = Modifier.height(22.dp))

        AppleMusicVolumeSlider()

        Spacer(modifier = Modifier.height(16.dp))

        AppleMusicBottomToolbar(
            isLyricsPage = isLyricsPage,
            hasTrack = hasTrack,
            playbackMode = playbackMode,
            onToggleLyrics = onToggleLyrics,
            onTogglePlaybackMode = onTogglePlaybackMode,
            onQueueClick = onQueueClick
        )

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun AppleMusicProgressSlider(
    currentPositionMs: Int,
    durationMs: Int,
    isPlaying: Boolean,
    hasTrack: Boolean,
    currentAudioQuality: OnlineAudioQuality,
    isLocalTrack: Boolean,
    onAudioQualityClick: () -> Unit,
    onSeekTo: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val onSeekUpdated by rememberUpdatedState(onSeekTo)

    val smoothPositionMs = rememberInterpolatedPlaybackTimeMs(
        positionMs = currentPositionMs,
        durationMs = durationMs,
        isPlaying = isPlaying
    )

    var isPressed by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var pendingSeekMs by remember(durationMs) { mutableStateOf<Int?>(null) }

    LaunchedEffect(pendingSeekMs) {
        if (pendingSeekMs != null) {
            delay(850)
            pendingSeekMs = null
        }
    }

    val validDuration = hasTrack && durationMs > 0
    val activePressed = validDuration && isPressed

    val liveMs = pendingSeekMs?.takeIf { abs(smoothPositionMs - it) > 900 } ?: smoothPositionMs
    val normalFraction = if (validDuration) {
        (liveMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val displayFraction = if (activePressed) dragFraction else normalFraction
    val displayCurrentMs = if (activePressed) {
        (dragFraction * durationMs).roundToInt().coerceIn(0, durationMs)
    } else {
        liveMs.coerceAtLeast(0)
    }

    val scale by animateFloatAsState(
        targetValue = if (activePressed) 1.035f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 210f),
        label = "progressScale"
    )
    val thickness by animateDpAsState(
        targetValue = if (activePressed) 12.dp else 7.dp,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 210f),
        label = "progressThickness"
    )
    val trackAlpha by animateFloatAsState(
        targetValue = if (activePressed) 0.85f else 0.62f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 220f),
        label = "trackAlpha"
    )
    val timeAlpha by animateFloatAsState(
        targetValue = if (activePressed) 0.72f else 0.46f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 220f),
        label = "timeAlpha"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .pointerInput(validDuration, durationMs) {
                    if (!validDuration) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        down.consume()
                        isPressed = true
                        pendingSeekMs = null
                        val width = size.width.toFloat().coerceAtLeast(1f)
                        dragFraction = (down.position.x / width).coerceIn(0f, 1f)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                        var committed = true
                        try {
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val current = event.changes.firstOrNull() ?: break
                                current.consume()
                                dragFraction = (current.position.x / width).coerceIn(0f, 1f)
                            } while (event.changes.any { it.pressed })
                        } catch (_: Exception) {
                            committed = false
                        } finally {
                            if (committed && validDuration) {
                                val target = (dragFraction * durationMs).roundToInt().coerceIn(0, durationMs)
                                pendingSeekMs = target
                                onSeekUpdated(target)
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            isPressed = false
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = trackAlpha
                    }
            ) {
                val trackH = thickness.toPx().coerceIn(0f, size.height)
                val top = (size.height - trackH) * 0.5f
                val outline = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(0f, top, size.width, top + trackH),
                            cornerRadius = CornerRadius(trackH * 0.5f, trackH * 0.5f)
                        )
                    )
                }
                clipPath(outline) {
                    drawRect(
                        color = Color.White.copy(alpha = 0.34f),
                        topLeft = Offset(0f, top),
                        size = Size(size.width, trackH)
                    )
                    val activeW = size.width * displayFraction.coerceIn(0f, 1f)
                    if (activeW > 0f) {
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(0f, top),
                            size = Size(activeW, trackH)
                        )
                    }
                }
            }
        }

        // 左已播时间 · 居中 Dolby Atmos 槽位音质切换按钮 · 右剩余时间 (-3:48)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 22.dp)
                .padding(horizontal = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = timeAlpha
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasTrack) formatAppleMusicTime(displayCurrentMs) else "0:00",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )

                Text(
                    text = if (validDuration) {
                        val remaining = (durationMs - displayCurrentMs).coerceAtLeast(0)
                        "-${formatAppleMusicTime(remaining)}"
                    } else "-0:00",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }

            if (hasTrack) {
                val badgeTint = Color.White.copy(alpha = 0.52f)
                val badgeLabel = if (isLocalTrack) "本地无损" else currentAudioQuality.badgeText
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !isLocalTrack,
                            onClick = onAudioQualityClick
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeLabel,
                        color = badgeTint,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.1).sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun AppleMusicMainTransportButtons(
    isPlaying: Boolean,
    hasTrack: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    var previousClickCount by remember { mutableIntStateOf(0) }
    var nextClickCount by remember { mutableIntStateOf(0) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = hasTrack,
                    onClick = {
                        previousClickCount++
                        onPrevious()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            AppleMusicSkipIcon(
                forward = false,
                clickCount = previousClickCount,
                size = 42.dp,
                tint = if (hasTrack) Color.White else Color.White.copy(alpha = 0.35f)
            )
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = hasTrack,
                    onClick = onPlayPause
                ),
            contentAlignment = Alignment.Center
        ) {
            AppleMusicPlayPauseAnimatedIcon(
                isPlaying = isPlaying,
                size = 48.dp,
                tint = if (hasTrack) Color.White else Color.White.copy(alpha = 0.35f)
            )
        }

        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = hasTrack,
                    onClick = {
                        nextClickCount++
                        onNext()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            AppleMusicSkipIcon(
                forward = true,
                clickCount = nextClickCount,
                size = 42.dp,
                tint = if (hasTrack) Color.White else Color.White.copy(alpha = 0.35f)
            )
        }
    }
}

/**
 * 1:1 复刻截图播放/暂停圆角变形动效：
 * - 暂停态：两条微圆角矩形竖条（匹配截图 1/2/4 的扁顶微圆角矩形，而非胶囊半圆）
 * - 播放态：圆角顶点等边向右三角形
 */
@Composable
fun AppleMusicPlayPauseAnimatedIcon(
    isPlaying: Boolean,
    size: Dp = 48.dp,
    tint: Color = Color.White
) {
    val scale = remember { Animatable(1f) }
    val pauseAlpha = remember { Animatable(if (isPlaying) 1f else 0f) }
    val playAlpha = remember { Animatable(if (isPlaying) 0f else 1f) }
    var firstRender by remember { mutableStateOf(true) }

    LaunchedEffect(isPlaying) {
        if (firstRender) {
            firstRender = false
            pauseAlpha.snapTo(if (isPlaying) 1f else 0f)
            playAlpha.snapTo(if (isPlaying) 0f else 1f)
            return@LaunchedEffect
        }
        coroutineScope {
            launch {
                scale.animateTo(0.38f, tween(65, easing = EaseOutQuad))
                scale.animateTo(1f, spring(dampingRatio = 0.64f, stiffness = 240f))
            }
            launch {
                if (isPlaying) {
                    playAlpha.animateTo(0f, tween(75, easing = LinearEasing))
                } else {
                    pauseAlpha.animateTo(0f, tween(75, easing = LinearEasing))
                }
            }
            launch {
                if (isPlaying) {
                    pauseAlpha.animateTo(1f, tween(180, delayMillis = 20, easing = LinearEasing))
                } else {
                    playAlpha.animateTo(1f, tween(180, delayMillis = 20, easing = LinearEasing))
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        contentAlignment = Alignment.Center
    ) {
        // 暂停态双微圆角竖条
        if (pauseAlpha.value > 0.001f) {
            Canvas(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer { alpha = pauseAlpha.value }
            ) {
                val w = this.size.width
                val h = this.size.height
                val barW = w * 0.24f
                val barH = h * 0.78f
                val r = CornerRadius(barW * 0.24f, barW * 0.24f)
                val topY = (h - barH) * 0.5f

                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.19f, topY),
                    size = Size(barW, barH),
                    cornerRadius = r
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.57f, topY),
                    size = Size(barW, barH),
                    cornerRadius = r
                )
            }
        }

        // 播放态圆角顶点实心三角（无描边纯填充）
        if (playAlpha.value > 0.001f) {
            val solidColor = tint.copy(alpha = 1f)
            Canvas(
                modifier = Modifier
                    .size(size)
                    .offset(x = 2.dp)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        alpha = playAlpha.value * tint.alpha
                    }
            ) {
                val w = this.size.width
                val h = this.size.height
                val cornerRadius = w * 0.11f
                val triangle = Path().apply {
                    moveTo(w * 0.18f, h * 0.12f)
                    lineTo(w * 0.86f, h * 0.50f)
                    lineTo(w * 0.18f, h * 0.88f)
                    close()
                }
                drawRoundedFilledPath(
                    path = triangle,
                    color = solidColor,
                    cornerRadiusPx = cornerRadius
                )
            }
        }
    }
}

private const val AppleMusicSkipDurationMillis = 520

private data class AppleMusicSkipTriangle(val position: Float, val scale: Float, val alpha: Float)
private data class AppleMusicSkipFrame(
    val entering: AppleMusicSkipTriangle,
    val moving: AppleMusicSkipTriangle,
    val exiting: AppleMusicSkipTriangle
)

private val AppleMusicSkipSamples = listOf(
    0 to AppleMusicSkipFrame(
        AppleMusicSkipTriangle(-0.5f, 0f, 0f),
        AppleMusicSkipTriangle(0f, 1f, 1f),
        AppleMusicSkipTriangle(1f, 1f, 1f)
    ),
    20 to AppleMusicSkipFrame(
        AppleMusicSkipTriangle(-0.492f, 0.1f, 0.28f),
        AppleMusicSkipTriangle(0.111f, 1f, 1f),
        AppleMusicSkipTriangle(1.063f, 0.625f, 0.61f)
    ),
    40 to AppleMusicSkipFrame(
        AppleMusicSkipTriangle(-0.428f, 0.2f, 0.42f),
        AppleMusicSkipTriangle(0.196f, 0.975f, 1f),
        AppleMusicSkipTriangle(1.126f, 0.475f, 0.44f)
    ),
    60 to AppleMusicSkipFrame(
        AppleMusicSkipTriangle(-0.285f, 0.375f, 0.66f),
        AppleMusicSkipTriangle(0.393f, 0.925f, 1f),
        AppleMusicSkipTriangle(1.265f, 0.225f, 0.21f)
    ),
    80 to AppleMusicSkipFrame(
        AppleMusicSkipTriangle(-0.219f, 0.45f, 0.78f),
        AppleMusicSkipTriangle(0.478f, 0.9f, 1f),
        AppleMusicSkipTriangle(1.326f, 0.125f, 0.14f)
    ),
    100 to AppleMusicSkipFrame(
        AppleMusicSkipTriangle(-0.154f, 0.525f, 0.84f),
        AppleMusicSkipTriangle(0.561f, 0.9f, 1f),
        AppleMusicSkipTriangle(1.36f, 0f, 0f)
    ),
    160 to AppleMusicSkipFrame(
        AppleMusicSkipTriangle(-0.026f, 0.725f, 0.95f),
        AppleMusicSkipTriangle(0.771f, 0.875f, 1f),
        AppleMusicSkipTriangle(1.36f, 0f, 0f)
    ),
    200 to AppleMusicSkipFrame(
        AppleMusicSkipTriangle(0.049f, 0.875f, 0.99f),
        AppleMusicSkipTriangle(0.922f, 0.9f, 1f),
        AppleMusicSkipTriangle(1.36f, 0f, 0f)
    ),
    260 to AppleMusicSkipFrame(
        AppleMusicSkipTriangle(0.069f, 1f, 1f),
        AppleMusicSkipTriangle(1.031f, 0.95f, 1f),
        AppleMusicSkipTriangle(1.36f, 0f, 0f)
    ),
    320 to AppleMusicSkipFrame(
        AppleMusicSkipTriangle(0.056f, 1f, 1f),
        AppleMusicSkipTriangle(1.043f, 0.975f, 1f),
        AppleMusicSkipTriangle(1.36f, 0f, 0f)
    ),
    420 to AppleMusicSkipFrame(
        AppleMusicSkipTriangle(0.014f, 1f, 1f),
        AppleMusicSkipTriangle(1.028f, 1f, 1f),
        AppleMusicSkipTriangle(1.36f, 0f, 0f)
    ),
    AppleMusicSkipDurationMillis to AppleMusicSkipFrame(
        AppleMusicSkipTriangle(0f, 1f, 1f),
        AppleMusicSkipTriangle(1f, 1f, 1f),
        AppleMusicSkipTriangle(1.36f, 0f, 0f)
    )
)

private fun appleMusicSkipFrame(fraction: Float): AppleMusicSkipFrame {
    val millis = fraction.coerceIn(0f, 1f) * AppleMusicSkipDurationMillis
    val next = AppleMusicSkipSamples.indexOfFirst { it.first >= millis }.coerceAtLeast(0)
    if (next == 0) return AppleMusicSkipSamples.first().second
    val (startTime, start) = AppleMusicSkipSamples[next - 1]
    val (endTime, end) = AppleMusicSkipSamples[next]
    val progress = ((millis - startTime) / (endTime - startTime).coerceAtLeast(1)).coerceIn(0f, 1f)
    fun interpolate(a: AppleMusicSkipTriangle, b: AppleMusicSkipTriangle) = AppleMusicSkipTriangle(
        position = lerp(a.position, b.position, progress),
        scale = lerp(a.scale, b.scale, progress),
        alpha = lerp(a.alpha, b.alpha, progress)
    )
    return AppleMusicSkipFrame(
        entering = interpolate(start.entering, end.entering),
        moving = interpolate(start.moving, end.moving),
        exiting = interpolate(start.exiting, end.exiting)
    )
}

@Composable
fun AppleMusicSkipIcon(
    forward: Boolean,
    clickCount: Int,
    size: Dp = 42.dp,
    tint: Color = Color.White
) {
    val travel = remember { Animatable(clickCount.toFloat()) }
    LaunchedEffect(clickCount) {
        travel.animateTo(clickCount.toFloat(), tween(AppleMusicSkipDurationMillis, easing = LinearEasing))
    }
    val frame by remember { derivedStateOf { appleMusicSkipFrame(travel.value % 1f) } }

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { scaleX = if (forward) 1f else -1f }
    ) {
        val solidColor = tint.copy(alpha = 1f)
        repeat(3) { triangleIndex ->
            Canvas(
                modifier = Modifier
                    .size(size * 0.56f)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        val pose = when (triangleIndex) {
                            0 -> frame.entering
                            1 -> frame.moving
                            else -> frame.exiting
                        }
                        compositingStrategy = CompositingStrategy.Offscreen
                        scaleX = pose.scale
                        scaleY = pose.scale
                        alpha = pose.alpha * tint.alpha
                        translationX = (pose.position - 0.5f) * (size * 0.44f).toPx()
                    }
            ) {
                val w = this.size.width
                val h = this.size.height
                val cornerR = w * 0.13f
                val triPath = Path().apply {
                    moveTo(w * 0.12f, h * 0.10f)
                    lineTo(w * 0.90f, h * 0.50f)
                    lineTo(w * 0.12f, h * 0.90f)
                    close()
                }
                drawRoundedFilledPath(
                    path = triPath,
                    color = solidColor,
                    cornerRadiusPx = cornerR
                )
            }
        }
    }
}

/**
 * Apple Music 音量胶囊条（支持点按 + 拖拽 + 硬件音量键实时同步）
 */
@Composable
fun AppleMusicVolumeSlider(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember {
        (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1).toFloat()
    }

    var currentVolumeFraction by remember {
        val cur = (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 8).toFloat()
        mutableFloatStateOf((cur / maxVolume).coerceIn(0f, 1f))
    }
    var isPressed by remember { mutableStateOf(false) }

    DisposableEffect(context, audioManager) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (!isPressed && audioManager != null) {
                    val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                    currentVolumeFraction = (cur / maxVolume).coerceIn(0f, 1f)
                }
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    val thickness by animateDpAsState(
        targetValue = if (isPressed) 11.dp else 6.5.dp,
        animationSpec = spring(dampingRatio = 0.64f, stiffness = 220f),
        label = "volumeThickness"
    )
    val trackAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 0.72f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 220f),
        label = "volumeTrackAlpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppleMusicVolumeLowIcon(size = 15.dp, tint = Color.White.copy(alpha = 0.55f))

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        down.consume()
                        isPressed = true
                        val width = size.width.toFloat().coerceAtLeast(1f)
                        fun applyX(x: Float) {
                            val frac = (x / width).coerceIn(0f, 1f)
                            currentVolumeFraction = frac
                            audioManager?.let { am ->
                                val targetVol = (frac * maxVolume).roundToInt()
                                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                            }
                        }
                        applyX(down.position.x)
                        try {
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val current = event.changes.firstOrNull() ?: break
                                current.consume()
                                applyX(current.position.x)
                            } while (event.changes.any { it.pressed })
                        } finally {
                            isPressed = false
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .graphicsLayer { alpha = trackAlpha }
            ) {
                val trackH = thickness.toPx().coerceIn(0f, size.height)
                val top = (size.height - trackH) * 0.5f
                val outline = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(0f, top, size.width, top + trackH),
                            cornerRadius = CornerRadius(trackH * 0.5f, trackH * 0.5f)
                        )
                    )
                }
                clipPath(outline) {
                    drawRect(
                        color = Color.White.copy(alpha = 0.28f),
                        topLeft = Offset(0f, top),
                        size = Size(size.width, trackH)
                    )
                    val activeW = size.width * currentVolumeFraction.coerceIn(0f, 1f)
                    if (activeW > 0f) {
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(0f, top),
                            size = Size(activeW, trackH)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        AppleMusicVolumeHighIcon(size = 18.dp, tint = Color.White.copy(alpha = 0.55f))
    }
}

@Composable
fun AppleMusicBottomToolbar(
    isLyricsPage: Boolean,
    hasTrack: Boolean,
    playbackMode: PlaybackMode = PlaybackMode.SEQUENTIAL,
    onToggleLyrics: () -> Unit,
    onTogglePlaybackMode: () -> Unit = {},
    onQueueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 歌词按钮（激活态为 42dp 半透明白圆 + 镂空气泡双引号，无缺口）
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = hasTrack,
                    onClick = onToggleLyrics
                ),
            contentAlignment = Alignment.Center
        ) {
            AppleMusicLyricsToggleIcon(
                isActive = isLyricsPage,
                buttonSize = 42.dp,
                iconSize = 23.dp,
                inactiveTint = if (hasTrack) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.28f)
            )
        }

        // 2. 播放模式切换按钮（循环 ↔ 随机，替换原 AirPlay 图标）
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = hasTrack,
                    onClick = onTogglePlaybackMode
                ),
            contentAlignment = Alignment.Center
        ) {
            AppleMusicPlaybackModeIcon(
                mode = playbackMode,
                size = 25.dp,
                tint = if (hasTrack) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.28f)
            )
        }

        // 3. 待播队列按钮
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onQueueClick
                ),
            contentAlignment = Alignment.Center
        ) {
            AppleMusicQueueIcon(
                size = 22.dp,
                tint = Color.White.copy(alpha = 0.55f)
            )
        }
    }
}

private fun openSystemAudioOutputSwitcher(context: Context) {
    val mediaOutputIntent = Intent("com.android.settings.panel.action.MEDIA_OUTPUT").apply {
        putExtra("com.android.settings.panel.extra.PACKAGE_NAME", context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val volumePanelIntent = Intent("android.settings.panel.action.VOLUME").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (runCatching { context.startActivity(mediaOutputIntent) }.isFailure) {
        if (runCatching { context.startActivity(volumePanelIntent) }.isFailure) {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            am?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
        }
    }
}

private fun formatAppleMusicTime(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
