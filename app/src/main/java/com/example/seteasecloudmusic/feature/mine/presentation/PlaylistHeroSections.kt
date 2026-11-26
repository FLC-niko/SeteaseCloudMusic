package com.example.seteasecloudmusic.feature.mine.presentation

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.core.common.toCoverThumbnailUrl
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import com.example.seteasecloudmusic.feature.home.presentation.ScrollingPosterWall
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.player.presentation.AppleMusicPlayTriangleIcon
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur

// 播放胶囊按钮强调色
private val PlaylistAccentColor = Color(0xFFFA233B)

// 全屏封面底部边缘模糊遮罩（在 68%~100% 高度区间内过渡到不透明模糊）
private val BottomEdgeBlurMask = Brush.verticalGradient(
    0.00f to Color.Transparent,
    0.68f to Color.Transparent,
    0.80f to Color.Black.copy(alpha = 0.65f),
    0.88f to Color.Black,
    1.00f to Color.Black
)

// 将 0.68..1.00 区间重映射到仅占底部 32% 高度的局部坐标系，缩小离屏模糊渲染面积
private const val BottomEdgeBlurHeightFraction = 0.32f // 底部模糊层占封面高度比例
private val BottomEdgeBlurMaskLocal = Brush.verticalGradient(
    0.000f to Color.Transparent,
    0.375f to Color.Black.copy(alpha = 0.65f),
    0.625f to Color.Black,
    1.000f to Color.Black
)

/** 歌单展示标题（将后缀型喜欢的音乐歌单统一规范为「我喜欢的音乐」）。 */
val PlaylistDetail.displayTitle: String
    get() = if (name.endsWith("喜欢的音乐")) "我喜欢的音乐" else name

/**
 * 风格 1：大封面墙（居中大封面立体投影、主标题与圆角三角胶囊播放键）。
 */
@Composable
fun PlaylistHeroAppleMusicSection(
    detail: PlaylistDetail,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    coverWidthFraction: Float = 0.68f,
    showArtwork: Boolean = true,
    showControls: Boolean = true,
    titleAlphaProvider: () -> Float = { 1f },
    onTitlePositioned: ((LayoutCoordinates) -> Unit)? = null,
    onTitleLineCount: ((Int) -> Unit)? = null
) {
    val totalCount = detail.trackCount.coerceAtLeast(detail.tracks.size)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 居中大正方形封面与立体投影
        if (showArtwork) Box(
            modifier = Modifier
                .fillMaxWidth(coverWidthFraction)
                .aspectRatio(1f)
                .shadow(
                    elevation = 26.dp, // 封面投影高度（dp）
                    shape = RoundedCornerShape(18.dp), // 封面圆角半径
                    clip = false,
                    spotColor = Color.Black.copy(alpha = 0.72f),
                    ambientColor = Color.Black.copy(alpha = 0.45f)
                )
                .clip(RoundedCornerShape(18.dp))
                .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            if (!detail.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = detail.coverUrl.toCoverThumbnailUrl(480), // 大封面采样尺寸（px）
                    contentDescription = detail.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                CoverPlaceholderBox()
            }
        }

        if (!showControls) return@Column

        Spacer(modifier = Modifier.height(18.dp))

        PlaylistHeroTitleAndPlayButton(
            detail = detail,
            totalCount = totalCount,
            isDark = isDark,
            onPlayAll = onPlayAll,
            titleAlphaProvider = titleAlphaProvider,
            onTitlePositioned = onTitlePositioned,
            onTitleLineCount = onTitleLineCount,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 风格 2：静态海报墙（4x4 多封面拼贴网格，底部使用取色渐变遮罩衔接背景）。
 */
@Composable
fun PlaylistHeroPosterWallSection(
    detail: PlaylistDetail,
    dominantDarkColor: Color,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    isDark: Boolean = true,
    showControls: Boolean = true,
    titleAlphaProvider: () -> Float = { 1f },
    onTitlePositioned: ((LayoutCoordinates) -> Unit)? = null,
    onTitleLineCount: ((Int) -> Unit)? = null
) {
    val wallCovers = remember(detail) {
        val tracksCovers = detail.tracks.mapNotNull { it.coverUrl?.takeIf(String::isNotBlank) }
        if (tracksCovers.isNotEmpty()) tracksCovers else listOfNotNull(detail.coverUrl)
    }
    val totalCount = detail.trackCount.coerceAtLeast(detail.tracks.size)
    val maskColor = dominantDarkColor

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 4x4 顶格海报拼贴网格
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            PosterCoverGrid(
                covers = wallCovers,
                dominantDarkColor = dominantDarkColor,
                modifier = Modifier.fillMaxWidth()
            )

            // 底部多段渐变遮罩：将网格底缘过渡至页面背景色，下延 4dp 覆盖像素接缝
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp) // 底部渐变遮罩高度
                    .align(Alignment.BottomCenter)
                    .offset(y = 4.dp)
                    .background(
                        Brush.verticalGradient(
                            0.00f to Color.Transparent,
                            0.12f to maskColor.copy(alpha = 0.03f),
                            0.24f to maskColor.copy(alpha = 0.12f),
                            0.36f to maskColor.copy(alpha = 0.28f),
                            0.48f to maskColor.copy(alpha = 0.48f),
                            0.60f to maskColor.copy(alpha = 0.68f),
                            0.72f to maskColor.copy(alpha = 0.84f),
                            0.84f to maskColor.copy(alpha = 0.95f),
                            0.92f to maskColor,
                            1.00f to maskColor
                        )
                    )
            )
        }

        if (!showControls) return@Column

        Spacer(modifier = Modifier.height(12.dp))

        PlaylistHeroTitleAndPlayButton(
            detail = detail,
            totalCount = totalCount,
            isDark = isDark,
            onPlayAll = onPlayAll,
            titleAlphaProvider = titleAlphaProvider,
            onTitlePositioned = onTitlePositioned,
            onTitleLineCount = onTitleLineCount,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 风格 3：动态海报墙（30° 倾斜交替多列滚动封面墙，叠加底缘模糊与渐变遮罩，标题与播放键位于过渡区上方）。
 */
@Composable
fun PlaylistHeroDynamicPosterWallSection(
    detail: PlaylistDetail,
    dominantDarkColor: Color,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    isDark: Boolean = true,
    showControls: Boolean = true,
    artworkHeight: Dp? = null,
    isAnimating: Boolean = true,
    titleAlphaProvider: () -> Float = { 1f },
    onTitlePositioned: ((LayoutCoordinates) -> Unit)? = null,
    onTitleLineCount: ((Int) -> Unit)? = null
) {
    val wallCovers = remember(detail) {
        val tracksCovers = detail.tracks.mapNotNull { it.coverUrl?.takeIf(String::isNotBlank) }
        if (tracksCovers.isNotEmpty()) tracksCovers else listOfNotNull(detail.coverUrl)
    }
    val totalCount = detail.trackCount.coerceAtLeast(detail.tracks.size)
    val maskColor = dominantDarkColor
    // 结合背景亮度判断前景色，避免浅色取色背景上出现低对比度白字
    val effectiveDark = isDark && maskColor.luminance() < 0.7f
    // 动态海报墙容器默认高度（沉浸顶栏模式 380dp + 状态栏，非全宽模式 300dp）
    val artworkBoxHeight = artworkHeight ?: if (topInset > 0.dp) 380.dp + topInset else 300.dp
    val artworkShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    val smoothScrimBrush = remember(maskColor) {
        buildSmoothHeroScrimBrush(maskColor)
    }
    val maskColorState = rememberUpdatedState(maskColor)
    val wallBackdrop = rememberLayerBackdrop {
        drawRect(maskColorState.value)
        drawContent()
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(artworkBoxHeight)
                .align(Alignment.TopCenter)
                .background(maskColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 8.dp) // 底部留白 8dp 配合外延遮罩，避免弹性缩放时露出边缘接缝
                    .then(
                        if (topInset > 0.dp) Modifier.clipToBounds() else Modifier.clip(artworkShape)
                    )
            ) {
                ScrollingPosterWall(
                    covers = wallCovers,
                    isAnimating = isAnimating,
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(wallBackdrop)
                )

                BottomBackdropBlurOverlay(
                    backdrop = wallBackdrop,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
                TopProtectionScrim(topInset = topInset)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = 4.dp) // 下延 4dp 覆盖底部接缝
                    .background(brush = smoothScrimBrush)
            )
        }

        if (showControls) {
            // 标题与播放按钮叠加在封面底缘上方 58dp 处，并限制最小顶部留白防止遮挡顶栏
            val contentTopPadding = (artworkBoxHeight - 58.dp)
                .coerceAtLeast(if (topInset > 0.dp) topInset + 160.dp else 180.dp)
            PlaylistHeroTitleAndPlayButton(
                detail = detail,
                totalCount = totalCount,
                isDark = effectiveDark,
                onPlayAll = onPlayAll,
                titleAlphaProvider = titleAlphaProvider,
                onTitlePositioned = onTitlePositioned,
                onTitleLineCount = onTitleLineCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = contentTopPadding, bottom = 6.dp)
            )
        }
    }
}

/**
 * 风格 4：全屏封面（顶部全宽封面，底部通过渐变融入取色背景，保留歌单名与播放键）。
 */
@Composable
fun PlaylistHeroFullCoverSection(
    detail: PlaylistDetail,
    dominantDarkColor: Color,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    isDark: Boolean = true,
    showControls: Boolean = true,
    artworkHeight: Dp? = null,
    titleAlphaProvider: () -> Float = { 1f },
    onTitlePositioned: ((LayoutCoordinates) -> Unit)? = null,
    onTitleLineCount: ((Int) -> Unit)? = null
) {
    val effectiveCoverUrl = remember(detail.coverUrl, detail.tracks) {
        detail.coverUrl?.takeIf { it.isNotBlank() }
            ?: detail.tracks.firstOrNull { !it.coverUrl.isNullOrBlank() }?.coverUrl
    }
    val totalCount = detail.trackCount.coerceAtLeast(detail.tracks.size)
    val maskColor = dominantDarkColor
    val effectiveDark = isDark && maskColor.luminance() < 0.7f
    // 全屏封面默认高度（沉浸顶栏模式 380dp + 状态栏，非全宽模式 300dp）
    val artworkBoxHeight = artworkHeight ?: if (topInset > 0.dp) 380.dp + topInset else 300.dp
    val artworkShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    val smoothScrimBrush = remember(maskColor) {
        buildSmoothHeroScrimBrush(maskColor)
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // 顶部全宽封面容器（底部留白 8dp 配合遮罩下延，避免弹性缩放时露出底边接缝）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(artworkBoxHeight)
                .align(Alignment.TopCenter)
                .background(maskColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 8.dp)
                    .then(
                        if (topInset > 0.dp) Modifier.clipToBounds() else Modifier.clip(artworkShape)
                    )
            ) {
                if (!effectiveCoverUrl.isNullOrBlank()) {
                    val modelUrl = effectiveCoverUrl.toCoverThumbnailUrl(720) // 全屏封面采样尺寸（px）
                    AsyncImage(
                        model = modelUrl,
                        contentDescription = detail.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        AsyncImage(
                            model = modelUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(10.dp, BlurredEdgeTreatment.Rectangle) // 底部边缘模糊半径（仅 Android 12+）
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(brush = BottomEdgeBlurMask, blendMode = BlendMode.DstIn)
                                }
                        )
                    }
                } else {
                    CoverPlaceholderBox()
                }

                TopProtectionScrim(topInset = topInset)
            }

            // 底部 smoothstep 曲线垂直渐变遮罩（下延 4dp 覆盖容器底边）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = 4.dp)
                    .background(brush = smoothScrimBrush)
            )
        }

        if (showControls) {
            val contentTopPadding = (artworkBoxHeight - 58.dp)
                .coerceAtLeast(if (topInset > 0.dp) topInset + 160.dp else 180.dp)
            PlaylistHeroTitleAndPlayButton(
                detail = detail,
                totalCount = totalCount,
                isDark = effectiveDark,
                onPlayAll = onPlayAll,
                titleAlphaProvider = titleAlphaProvider,
                onTitlePositioned = onTitlePositioned,
                onTitleLineCount = onTitleLineCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = contentTopPadding, bottom = 6.dp)
            )
        }
    }
}

/** 歌单 Hero 标题与全曲播放胶囊按钮。 */
@Composable
private fun PlaylistHeroTitleAndPlayButton(
    detail: PlaylistDetail,
    totalCount: Int,
    isDark: Boolean,
    onPlayAll: () -> Unit,
    titleAlphaProvider: () -> Float,
    onTitlePositioned: ((LayoutCoordinates) -> Unit)?,
    onTitleLineCount: ((Int) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = detail.displayTitle,
            style = MiuixTheme.textStyles.title2.copy(
                fontSize = 22.sp, // Hero 标题字号（吸顶后缩放到 16sp）
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF111111)
            ),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = if (onTitleLineCount != null) {
                { result -> onTitleLineCount(result.lineCount) }
            } else {
                null
            },
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .graphicsLayer { alpha = titleAlphaProvider() }
                .let { base ->
                    if (onTitlePositioned != null) base.onGloballyPositioned(onTitlePositioned) else base
                }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            onClick = onPlayAll,
            enabled = totalCount > 0,
            shape = RoundedCornerShape(24.dp),
            color = PlaylistAccentColor,
            border = null,
            shadowElevation = 2.dp,
            modifier = Modifier.height(48.dp) // 播放胶囊按钮高度
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 38.dp), // 播放按钮左右内边距
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                AppleMusicPlayTriangleIcon(
                    tint = Color.White,
                    size = 15.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "播放",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

/** 底部局部实时模糊覆盖层（仅覆盖底部 32% 高度，Android 12+ 启用）。 */
@Composable
private fun BottomBackdropBlurOverlay(
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(BottomEdgeBlurHeightFraction)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(brush = BottomEdgeBlurMaskLocal, blendMode = BlendMode.DstIn)
            }
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = { blur(10.dp.toPx(), TileMode.Clamp) } // 底部边缘模糊半径
            )
    )
}

/** 顶部状态栏与操作按钮区域的半透明暗角保护遮罩。 */
@Composable
private fun TopProtectionScrim(topInset: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(topInset + 72.dp) // 顶部暗角遮罩高度（状态栏高度 + 72dp）
            .background(
                Brush.verticalGradient(
                    0.0f to Color.Black.copy(alpha = 0.22f), // 顶部暗角最大透明度
                    1.0f to Color.Transparent
                )
            )
    )
}

@Composable
private fun CoverPlaceholderBox() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF28364A), Color(0xFF161F2E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = MiuixIcons.Music,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(68.dp)
        )
    }
}

/** 构建从 62% 到 92% 高度按 smoothstep 曲线过渡至目标背景色的垂直渐变画刷。 */
private fun buildSmoothHeroScrimBrush(targetColor: Color): Brush {
    val startPos = 0.62f // 遮罩渐变起始位置（占封面高度比例）
    val endPos = 0.92f // 遮罩达到完全不透明的位置比例
    val steps = 12 // smoothstep 分段采样数（越大过渡越平滑）
    val stops = Array(steps + 2) { index ->
        when (index) {
            0 -> 0.00f to Color.Transparent
            steps + 1 -> 1.00f to targetColor
            else -> {
                val t = (index - 1).toFloat() / (steps - 1).toFloat()
                val eased = t * t * (3f - 2f * t)
                val pos = startPos + (endPos - startPos) * t
                pos to targetColor.copy(alpha = eased)
            }
        }
    }
    return Brush.verticalGradient(colorStops = stops)
}

/**
 * 4x4 拼贴封面网格（1:1 正方形单元格与主题适配网格线）。
 */
@Composable
private fun PosterCoverGrid(
    covers: List<String>,
    dominantDarkColor: Color,
    modifier: Modifier = Modifier
) {
    val displayItems = remember(covers) {
        val total = 16
        List(total) { index ->
            if (covers.isNotEmpty()) covers[index % covers.size].toCoverThumbnailUrl(180) else null // 单格缩略图采样尺寸（px）
        }
    }
    val gridLineColor = if (AppTheme.isDark) Color(0xFF1C1C1E) else Color.White

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
    ) {
        repeat(4) { rowIndex ->
            if (rowIndex > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(gridLineColor)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(4) { colIndex ->
                    val itemIndex = rowIndex * 4 + colIndex
                    val url = displayItems.getOrNull(itemIndex)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clipToBounds()
                            .background(dominantDarkColor.copy(alpha = 0.20f))
                            .drawWithContent {
                                drawContent()
                                // 列间 1dp 竖向分割线（前 3 列右侧绘制；第 4 行底部截断 24dp 避免遮罩外露出线头）
                                if (colIndex < 3) {
                                    val strokePx = 1.dp.toPx()
                                    val bottomCutoff = if (rowIndex == 3) 24.dp.toPx() else 0f
                                    val lineBottom = (size.height - bottomCutoff).coerceAtLeast(0f)
                                    if (lineBottom > 0f) {
                                        drawLine(
                                            color = gridLineColor,
                                            start = Offset(size.width - strokePx / 2, 0f),
                                            end = Offset(size.width - strokePx / 2, lineBottom),
                                            strokeWidth = strokePx
                                        )
                                    }
                                }
                            }
                    ) {
                        if (!url.isNullOrBlank()) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}
