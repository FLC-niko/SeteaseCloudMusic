package com.example.seteasecloudmusic.feature.player.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.RoundedCorner
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import coil.size.Precision
import coil.transition.Transition

/**
 * 共享专辑封面 Painter（参照 MeiloX-main LocalPlayerArtwork）：
 * 由顶层导航容器按当前曲目 URL 统一持有同一份 [AsyncImagePainter]，
 * 同时供给 MiniPlayer、全屏播放器常驻封面、以及双向过渡飞行封面层，
 * 确保在 p=0 与 p=1 交接帧零闪烁、零重复解码。
 */
val LocalPlayerArtwork = staticCompositionLocalOf<Pair<String?, AsyncImagePainter>?> { null }

internal const val PlayerBackgroundHandoffEnd = 0.99f
internal const val PlayerMiniContentFadeEnd = 0.80f
internal const val PlayerContentAppearStart = 0.24f
internal const val PlayerContentAppearEnd = 0.90f

internal fun easeInCubic(value: Float): Float = value.coerceIn(0f, 1f).let { it * it * it }

internal fun easeOutCubic(value: Float): Float = 1f - easeInCubic(1f - value)

internal fun mix(start: Float, end: Float, progress: Float): Float =
    start + (end - start) * progress

internal fun Rect.isUsable(): Boolean = width > 0f && height > 0f

internal fun playerBackgroundAlpha(progress: Float): Float =
    easeInCubic(progress / PlayerBackgroundHandoffEnd)

internal fun playerMiniContentAlpha(progress: Float): Float =
    1f - easeOutCubic(progress / PlayerMiniContentFadeEnd)

internal fun playerContentAlpha(progress: Float): Float = easeInCubic(
    (progress - PlayerContentAppearStart) / (PlayerContentAppearEnd - PlayerContentAppearStart)
)

/**
 * 容器边界双向线性插值（参照 MeiloX-main playerContainerRect）
 */
internal fun playerContainerRect(source: Rect, target: Rect, progress: Float): Rect {
    val p = progress.coerceIn(0f, 1f)
    return Rect(
        left = mix(source.left, target.left, p),
        top = mix(source.top, target.top, p),
        right = mix(source.right, target.right, p),
        bottom = mix(source.bottom, target.bottom, p)
    )
}

/**
 * 容器圆角双向插值（参照 MeiloX-main playerContainerCornerRadius）：
 * 从迷你播放条胶囊半高平滑过渡至屏幕物理圆角，完全展开 (p >= 0.999f) 时归零贴合全屏。
 */
internal fun playerContainerCornerRadius(source: Float, target: Float, progress: Float): Float =
    if (progress >= 0.999f) 0f else mix(source, target, progress.coerceAtLeast(0f))

/**
 * 迷你播放器 <-> 全屏大封面 双向弧线飞行轨迹（1:1 参照 MeiloX-main playerArtworkRect）：
 * - X 轴采用 easeOutCubic 先快后稳地向中心靠拢（收起时反向先稳后快归位至迷你栏左侧）
 * - Y 轴采用 mix(easeInCubic(p), p, 0.4f) 形成自然抛物弧线而非生硬斜线
 */
internal fun playerArtworkRect(source: Rect, target: Rect, progress: Float): Rect {
    val p = progress.coerceIn(0f, 1f)
    val centerX = mix(source.center.x, target.center.x, easeOutCubic(p))
    val centerY = mix(source.center.y, target.center.y, mix(easeInCubic(p), p, 0.4f))
    val scale = mix(1f, target.width / source.width.coerceAtLeast(1f), p)
    val width = source.width * scale
    val height = mix(source.height, target.height, p)
    return Rect(
        left = centerX - width / 2f,
        top = centerY - height / 2f,
        right = centerX + width / 2f,
        bottom = centerY + height / 2f
    )
}

/**
 * 迷你播放器 <-> 歌词页顶部小封面 顶边锚定双向轨迹（1:1 参照 MeiloX-main playerTopAnchoredArtworkRect）：
 * 当全屏播放器处于歌词页时，顶部 54dp 小封面紧随收起/展开容器顶边同步移动，绝不脱离播放页壳体。
 */
internal fun playerTopAnchoredArtworkRect(
    source: Rect,
    target: Rect,
    sourceContainer: Rect,
    targetContainer: Rect,
    progress: Float
): Rect {
    val p = progress.coerceIn(0f, 1f)
    val artwork = playerArtworkRect(source, target, p)
    val container = playerContainerRect(sourceContainer, targetContainer, p)
    val top = container.top + mix(
        source.top - sourceContainer.top,
        target.top - targetContainer.top,
        p
    )
    return Rect(
        left = artwork.left,
        top = top,
        right = artwork.right,
        bottom = top + artwork.height
    )
}

internal fun appleMusicLyricSpring() = spring<Float>(
    dampingRatio = 1.08f,
    stiffness = 680f,
    visibilityThreshold = 0.0001f
)

internal fun appleMusicControlsSpring(visible: Boolean) = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = if (visible) 420f else 180f,
    visibilityThreshold = 0.001f
)

internal fun appleMusicAlbumInfoAlpha(progress: Float): Float =
    (1f - progress.coerceIn(0f, 1f) * 1.25f).coerceIn(0f, 1f)

internal fun appleMusicHeaderInfoAlpha(progress: Float): Float =
    ((progress.coerceIn(0f, 1f) - 0.12f) / 0.88f).coerceIn(0f, 1f)

internal fun appleMusicLyricsAlpha(progress: Float): Float =
    ((progress.coerceIn(0f, 1f) - 0.08f) / 0.82f).coerceIn(0f, 1f)

internal fun appleMusicLyricsTranslationY(
    progress: Float,
    albumInformationBottom: Float,
    lyricViewportTop: Float
): Float = (1f - progress.coerceIn(0f, 1f)) *
    (albumInformationBottom - lyricViewportTop).coerceAtLeast(0f)

internal fun appleMusicInfoOffset(
    albumOrigin: Offset,
    headerOrigin: Offset,
    progress: Float,
    fromAlbum: Boolean,
    placementOrigin: Offset = if (fromAlbum) albumOrigin else headerOrigin
): Offset {
    val p = progress.coerceIn(0f, 1f)
    val shared = Offset(
        x = lerp(albumOrigin.x, headerOrigin.x, p),
        y = lerp(albumOrigin.y, headerOrigin.y, p)
    )
    return shared - placementOrigin
}

internal data class AppleMusicArtworkFrame(
    val bounds: Rect,
    val cornerRadius: Float,
    val shadowElevation: Float
)

/**
 * 全屏播放页内「大封面专辑页 <-> 歌词页顶栏小封面」连续插值几何帧
 * （1:1 参照 MeiloX-main appleMusicArtworkFrame）
 */
internal fun appleMusicArtworkFrame(
    normalBounds: Rect,
    headerBounds: Rect,
    normalRadius: Float,
    headerRadius: Float,
    normalShadow: Float,
    playbackScale: Float,
    progress: Float
): AppleMusicArtworkFrame {
    val p = progress.coerceIn(0f, 1f)
    val width = lerp(normalBounds.width, headerBounds.width, p)
    val height = lerp(normalBounds.height, headerBounds.height, p)
    val scale = lerp(playbackScale, 1f, p)
    val left = lerp(normalBounds.left, headerBounds.left, p) + width * (1f - scale) / 2f
    val top = lerp(normalBounds.top, headerBounds.top, p) + height * (1f - scale) / 2f
    return AppleMusicArtworkFrame(
        bounds = Rect(left, top, left + width * scale, top + height * scale),
        cornerRadius = lerp(normalRadius, headerRadius, p) * scale,
        shadowElevation = normalShadow * (1f - p) * scale
    )
}

internal fun Modifier.playerArtworkMotion(
    frame: () -> AppleMusicArtworkFrame
): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        frame()
        placeable.placeWithLayer(0, 0) {
            val current = frame()
            transformOrigin = TransformOrigin(0f, 0f)
            translationX = current.bounds.left
            translationY = current.bounds.top
            scaleX = current.bounds.width / size.width.coerceAtLeast(1f)
            scaleY = current.bounds.height / size.height.coerceAtLeast(1f)
        }
    }
}

private val AppleMusicPauseEasing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)

/**
 * Apple Music 播放/暂停封面呼吸缩放（1:1 参照 MeiloX-main rememberAppleMusicPlaybackScale）：
 * 播放态弹性放大至 1.0f，暂停态柔和收窄至 0.78f。
 */
@Composable
internal fun rememberAppleMusicPlaybackScale(
    isPlaying: Boolean,
    active: Boolean = true
): State<Float> {
    val scale = remember { Animatable(if (isPlaying) 1f else 0.78f) }
    LaunchedEffect(isPlaying, active) {
        val target = if (isPlaying) 1f else 0.78f
        if (!active) {
            scale.snapTo(target)
        } else if (isPlaying) {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = 165f,
                    visibilityThreshold = 0.001f
                )
            )
        } else {
            scale.animateTo(
                targetValue = 0.78f,
                animationSpec = tween(durationMillis = 480, easing = AppleMusicPauseEasing)
            )
        }
    }
    return scale.asState()
}

@Composable
internal fun rememberPlayerScreenCornerRadiusPx(hostBounds: Rect): Float {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    return remember(context, view, hostBounds, configuration.screenWidthDp, configuration.screenHeightDp) {
        val fallbackPx = with(density) { 34.dp.toPx() }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return@remember fallbackPx
        }
        val activity = context.findActivity()
        if (activity == null || activity.isInMultiWindowMode || activity.isInPictureInPictureMode) {
            0f
        } else {
            val insets = view.rootWindowInsets
            val maxCorner = listOf(
                RoundedCorner.POSITION_TOP_LEFT,
                RoundedCorner.POSITION_TOP_RIGHT,
                RoundedCorner.POSITION_BOTTOM_LEFT,
                RoundedCorner.POSITION_BOTTOM_RIGHT
            ).mapNotNull { pos -> insets?.getRoundedCorner(pos)?.radius }
                .maxOrNull()
                ?.toFloat()
            maxCorner ?: fallbackPx
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * 归一化高分辨率专辑封面 URL（若带有列表缩略图 `?param=` 参数则还原为原图地址）
 */
fun normalizeHighResCoverUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    if (url.startsWith("http://") || url.startsWith("https://")) {
        val httpsUrl = url.replaceFirst("http://", "https://")
        return if (httpsUrl.contains("?param=")) {
            httpsUrl.substringBefore("?")
        } else {
            httpsUrl
        }
    }
    return url
}

/**
 * 构建全屏与迷你播放器共享的高清封面请求：
 * - 固定请求 1200x1200 原生高清尺寸，杜绝 MiniPlayer (38dp) 首次绘制把共享 Painter 降采样为 114x114 导致展开瞬间模糊
 * - 独立 `memoryCacheKey` 并禁用低清 `placeholderMemoryCacheKey`，杜绝后台通知 (320px) 或流体背景 (256px) 缩略图污染大封面
 */
@Composable
fun rememberHighResArtworkRequest(coverUrl: String?): ImageRequest? {
    val context = LocalContext.current
    val normalized = remember(coverUrl) { normalizeHighResCoverUrl(coverUrl) }
    return remember(context, normalized) {
        normalized?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .size(1200, 1200)
                .precision(Precision.INEXACT)
                .memoryCacheKey("$url#player_hq")
                .placeholderMemoryCacheKey(null as String?)
                .transitionFactory(Transition.Factory.NONE)
                .build()
        }
    }
}


