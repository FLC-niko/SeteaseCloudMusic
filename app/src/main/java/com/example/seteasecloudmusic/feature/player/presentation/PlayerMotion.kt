package com.example.seteasecloudmusic.feature.player.presentation

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView

internal fun mix(source: Float, target: Float, progress: Float): Float =
    source + (target - source) * progress.coerceIn(0f, 1f)

internal fun Rect.isUsable(): Boolean =
    left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite() && width > 1f && height > 1f

internal fun playerContainerRect(source: Rect, target: Rect, progress: Float): Rect = Rect(
    mix(source.left, target.left, progress), mix(source.top, target.top, progress),
    mix(source.right, target.right, progress), mix(source.bottom, target.bottom, progress)
)

internal fun playerArtworkRect(source: Rect, target: Rect, progress: Float): Rect =
    playerContainerRect(source, target, progress)

/** 顶栏封面跟随展开容器的顶部，避免缩回迷你播放器时向屏幕中央漂移。 */
internal fun playerTopAnchoredArtworkRect(
    source: Rect, target: Rect, sourceContainer: Rect, targetContainer: Rect, progress: Float
): Rect {
    val container = playerContainerRect(sourceContainer, targetContainer, progress)
    val left = container.left + mix(source.left - sourceContainer.left, target.left - targetContainer.left, progress)
    val top = container.top + mix(source.top - sourceContainer.top, target.top - targetContainer.top, progress)
    return Rect(left, top, left + mix(source.width, target.width, progress), top + mix(source.height, target.height, progress))
}

internal fun playerContainerCornerRadius(source: Float, target: Float, progress: Float): Float = mix(source, target, progress)
internal fun playerBackgroundAlpha(progress: Float): Float = (progress / 0.65f).coerceIn(0f, 1f)
internal fun playerContentAlpha(progress: Float): Float = ((progress - 0.20f) / 0.65f).coerceIn(0f, 1f)
internal fun playerMiniContentAlpha(progress: Float): Float = (1f - progress / 0.25f).coerceIn(0f, 1f)

@Composable
internal fun rememberPlayerScreenCornerRadiusPx(bounds: Rect): Float {
    val view = LocalView.current
    return if (Build.VERSION.SDK_INT >= 31 && bounds.isUsable()) {
        view.rootWindowInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius?.toFloat() ?: 0f
    } else 0f
}

internal data class PlayerArtworkFrame(val bounds: Rect, val cornerRadius: Float, val shadowElevation: Float)

internal fun appleMusicArtworkFrame(
    normalBounds: Rect, headerBounds: Rect, normalRadius: Float, headerRadius: Float,
    normalShadow: Float, playbackScale: Float, progress: Float
): PlayerArtworkFrame {
    val scale = playbackScale.coerceIn(0.01f, 1f)
    val halfWidth = normalBounds.width * scale * 0.5f
    val halfHeight = normalBounds.height * scale * 0.5f
    val center = normalBounds.center
    val album = Rect(center.x - halfWidth, center.y - halfHeight, center.x + halfWidth, center.y + halfHeight)
    return PlayerArtworkFrame(
        playerArtworkRect(album, headerBounds, progress),
        mix(normalRadius * scale, headerRadius, progress), mix(normalShadow * scale, 0f, progress)
    )
}

/** 在绘制阶段读取动画值，避免每帧重新组合歌词页面。 */
internal fun Modifier.playerArtworkMotion(frame: () -> PlayerArtworkFrame): Modifier = graphicsLayer {
    val bounds = frame().bounds
    transformOrigin = TransformOrigin(0f, 0f)
    translationX = bounds.left
    translationY = bounds.top
    scaleX = bounds.width / size.width.coerceAtLeast(1f)
    scaleY = bounds.height / size.height.coerceAtLeast(1f)
}

internal fun appleMusicLyricSpring() = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 280f)

@Composable
internal fun rememberAppleMusicPlaybackScale(isPlaying: Boolean): State<Float> = animateFloatAsState(
    if (isPlaying) 1f else 0.78f,
    spring(dampingRatio = 0.85f, stiffness = 210f), label = "albumPlaybackScale"
)

internal fun appleMusicAlbumInfoAlpha(progress: Float): Float = (1f - progress / 0.55f).coerceIn(0f, 1f)
internal fun appleMusicHeaderInfoAlpha(progress: Float): Float = ((progress - 0.35f) / 0.65f).coerceIn(0f, 1f)
internal fun appleMusicLyricsAlpha(progress: Float): Float = ((progress - 0.20f) / 0.80f).coerceIn(0f, 1f)
internal fun appleMusicLyricsTranslationY(progress: Float, albumInformationBottom: Float, lyricViewportTop: Float): Float =
    (albumInformationBottom - lyricViewportTop).coerceAtLeast(0f) * (1f - progress.coerceIn(0f, 1f))

internal fun appleMusicInfoOffset(albumOrigin: Offset, headerOrigin: Offset, progress: Float, fromAlbum: Boolean): Offset {
    val origin = if (fromAlbum) albumOrigin else headerOrigin
    return Offset(mix(albumOrigin.x, headerOrigin.x, progress), mix(albumOrigin.y, headerOrigin.y, progress)) - origin
}
