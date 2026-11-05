package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.core.common.toCoverThumbnailUrl
import com.example.seteasecloudmusic.core.player.PlaybackMode
import com.example.seteasecloudmusic.core.settings.OnlineAudioQuality
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import kotlinx.coroutines.isActive
import kotlin.math.sin

/** 低分辨率模糊封面流光，暂停时停在当前相位，不额外运行逐帧动画。 */
@Composable
internal fun AppleMusicBackground(coverUrl: String?, isPlaying: Boolean) {
    val phase = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) while (isActive) {
            phase.animateTo(1f, tween(((1f - phase.value) * 32000).toInt().coerceAtLeast(1), easing = LinearEasing))
            phase.snapTo(0f)
        }
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF191920)), contentAlignment = Alignment.Center) {
        if (!coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = coverUrl.toCoverThumbnailUrl(300), contentDescription = null, contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.8f) }),
                modifier = Modifier.fillMaxSize(1.5f / 6f).graphicsLayer {
                    scaleX = 6f
                    scaleY = 6f
                    rotationZ = sin(phase.value * (2f * Math.PI.toFloat())) * 12f
                }.blur(10.dp)
            )
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            Color.Black.copy(alpha = 0.36f), Color.Black.copy(alpha = 0.26f), Color.Black.copy(alpha = 0.70f)
        ))))
    }
}

/** 共享原生控制逻辑，沿用音质、队列、歌词入口与对称的触控区域。 */
@Composable
internal fun AppleMusicPlaybackControls(
    currentPositionMs: Int,
    durationMs: Int,
    isPlaying: Boolean,
    isLyricsPage: Boolean,
    hasTrack: Boolean,
    currentAudioQuality: OnlineAudioQuality,
    isLocalTrack: Boolean,
    onAudioQualityClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onToggleLyrics: () -> Unit,
    onQueueClick: () -> Unit,
    playbackMode: PlaybackMode = PlaybackMode.SEQUENTIAL,
    onTogglePlaybackMode: () -> Unit = {}
) {
    PlayerControls(currentPositionMs, durationMs, isPlaying,
        playbackMode = playbackMode, isLocalTrack = isLocalTrack, currentQuality = currentAudioQuality,
        hasTrack = hasTrack, isLyricsPage = isLyricsPage,
        onPlayPause = onPlayPause, onNext = onNext, onPrevious = onPrevious, onSeekTo = onSeekTo,
        onTogglePlaybackMode = onTogglePlaybackMode, onSelectQualityClick = onAudioQualityClick,
        onToggleLyrics = onToggleLyrics, onQueueClick = onQueueClick)
}

@Composable
internal fun AppleMusicExplicitBadge(size: Dp) {
    Box(Modifier.size(size).background(Color.White.copy(alpha = 0.55f), RoundedCornerShape(2.dp)), contentAlignment = Alignment.Center) {
        Text("E", color = Color(0xFF24242A), fontSize = (size.value * 0.68f).sp, maxLines = 1)
    }
}

@Composable
internal fun AppleMusicHeartIcon(isLiked: Boolean, size: Dp, tint: Color = if (isLiked) Color(0xFFFA526A) else Color.White.copy(alpha = 0.88f)) {
    Icon(if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
        if (isLiked) "取消喜欢" else "喜欢", Modifier.size(size), tint = tint)
}

@Composable
internal fun AppleMusicMoreIcon(size: Dp, tint: Color = Color.White.copy(alpha = 0.88f)) {
    Icon(Icons.Filled.MoreHoriz, "更多", Modifier.size(size), tint = tint)
}
