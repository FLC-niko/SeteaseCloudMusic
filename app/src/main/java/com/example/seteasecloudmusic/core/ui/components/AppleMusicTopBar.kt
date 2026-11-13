package com.example.seteasecloudmusic.core.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import kotlin.math.roundToInt

/**
 * 顶部大标题 + 头像连续联动尺寸常量，确保列表顶部占位与浮层完全对齐。
 */
object AppleMusicTopBarDefaults {
    /** 展开态位于状态栏下方的垂直偏移，随上滑无缝收缩至 0.dp */
    val ExpandedTopOffset: Dp = 20.dp
    /** 顶栏统一行高 */
    val BarHeight: Dp = 52.dp
    /** 列表顶部透明占位高度（从 contentPadding.top = statusBarHeight + 8.dp 起算） */
    val HeaderSpacerHeight: Dp = 68.dp
    /** 大标题与头像完全收缩衔接到顶部的滑动距离 */
    val CollapseThreshold: Dp = 64.dp
}

/**
 * 连续一体式 Apple Music 顶栏：大标题与右侧头像作为同一个实体，
 * 随手指上滑同步向上移动并按比例缩小，直接无缝衔接到顶部渐进式高斯模糊导航栏，
 * 不再通过两个分离组件交叉淡入淡出。
 */
@Composable
fun AppleMusicMorphingTopBar(
    title: String,
    collapseFractionProvider: () -> Float,
    statusBarHeight: Dp,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    expandedTopOffset: Dp = AppleMusicTopBarDefaults.ExpandedTopOffset,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val density = LocalDensity.current
    val expandedOffsetPx = remember(density, expandedTopOffset) {
        with(density) { expandedTopOffset.toPx() }
    }
    val blurTotalHeight = statusBarHeight + 80.dp

    Box(modifier = modifier.fillMaxWidth()) {
        if (backdrop != null) {
            ProgressiveTopBlur(
                backdrop = backdrop,
                totalHeight = blurTotalHeight,
                statusBarHeight = statusBarHeight,
                alphaProvider = {
                    val f = collapseFractionProvider().coerceIn(0f, 1f)
                    ((f - 0.05f) / 0.55f).coerceIn(0f, 1f)
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarHeight)
                .offset {
                    val f = collapseFractionProvider().coerceIn(0f, 1f)
                    IntOffset(x = 0, y = lerp(expandedOffsetPx, 0f, f).roundToInt())
                }
                .height(AppleMusicTopBarDefaults.BarHeight)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // 唯一标题文本：从 34sp 随滑动连续缩放到 21sp，左锚点对齐
            Text(
                text = title,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111111),
                letterSpacing = (-0.8).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(end = 56.dp)
                    .graphicsLayer {
                        val f = collapseFractionProvider().coerceIn(0f, 1f)
                        val scale = lerp(1f, 21f / 34f, f)
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
            )

            // 唯一头像/右侧组件：从 46dp 随滑动同步缩小到 34dp，右锚点对齐
            if (trailingContent != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .graphicsLayer {
                            val f = collapseFractionProvider().coerceIn(0f, 1f)
                            val scale = lerp(1f, 34f / 46f, f)
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(1f, 0.5f)
                        }
                ) {
                    trailingContent()
                }
            }
        }
    }
}

/**
 * 监听 LazyListState 的滑动距离并计算出符合 Apple Music 规范的折叠进度 (0f..1f)。
 *
 * @param lazyListState 列表滑动状态
 * @param collapseThresholdDp 折叠完成所需的滚动距离，默认 80.dp
 */
@Composable
fun rememberAppleMusicCollapseFraction(
    lazyListState: LazyListState,
    collapseThresholdDp: Dp = 80.dp
): State<Float> {
    val density = LocalDensity.current
    val thresholdPx = remember(density, collapseThresholdDp) {
        with(density) { collapseThresholdDp.toPx() }
    }
    return remember(lazyListState, thresholdPx) {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex == 0) {
                (lazyListState.firstVisibleItemScrollOffset.toFloat() / thresholdPx).coerceIn(0f, 1f)
            } else {
                1f
            }
        }
    }
}

/**
 * Apple Music 风格大标题（靠左对齐，随动自然微缩至小标题尺寸）。
 *
 * @param title 标题文字（如“首页”、“我的”）
 * @param collapseFraction 折叠进度 (0f..1f)
 * @param modifier 外部修饰符
 * @param trailingContent 大标题右侧操作组件（如头像）
 */
@Composable
fun AppleMusicLargeTitle(
    title: String,
    collapseFraction: Float,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    // 平滑连续缩小：从 1.0f 缩小到 0.62f（34sp -> 21sp），始终严格以左侧为锚点缩放
    val titleScale = 1f - collapseFraction * 0.38f
    // 当完全缩小到位时淡出，由顶栏左侧同等大小的固定标题平滑接管
    val titleAlpha = (1f - ((collapseFraction - 0.70f) / 0.30f)).coerceIn(0f, 1f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111111),
            letterSpacing = (-0.8).sp,
            modifier = Modifier.graphicsLayer {
                alpha = titleAlpha
                scaleX = titleScale
                scaleY = titleScale
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
        )
        if (trailingContent != null && titleAlpha > 0.05f) {
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha
                }
            ) {
                trailingContent()
            }
        }
    }
}

/**
 * 顶部渐进式高斯模糊遮罩（顶部模糊较深，底部模糊较浅，过渡自然，无状态栏白色底罩）。
 *
 * 通过三层不同半径的高斯模糊（深 -> 中 -> 浅）与平滑 S 型透明度遮罩垂直叠合，
 * 避免单层模糊半透明渐变产生的重影感，同时不叠加任何白色雾罩或状态栏纯白背景。
 *
 * @param backdrop 底层页面内容采样源，须置于被采样容器之外以避免循环采样
 * @param totalHeight 模糊区域总高度（建议包含状态栏、顶栏内容及底部羽化延伸区）
 * @param statusBarHeight 系统状态栏高度
 * @param alphaProvider 整体透明度提供器（在绘制阶段读取，避免滚动期频繁重组）
 * @param deepBlurRadius 顶部最深模糊半径（默认 26.dp）
 * @param mediumBlurRadius 中段过渡模糊半径（默认 13.dp）
 * @param shallowBlurRadius 底部浅层模糊半径（默认 4.5.dp）
 */
@Composable
fun ProgressiveTopBlur(
    backdrop: Backdrop?,
    totalHeight: Dp,
    statusBarHeight: Dp,
    modifier: Modifier = Modifier,
    alphaProvider: () -> Float = { 1f },
    deepBlurRadius: Dp = 26.dp,
    mediumBlurRadius: Dp = 13.dp,
    shallowBlurRadius: Dp = 4.5.dp
) {
    if (backdrop == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    val density = LocalDensity.current
    val statusBarFraction = remember(statusBarHeight, totalHeight, density) {
        val totalPx = with(density) { totalHeight.toPx() }.coerceAtLeast(1f)
        val sbPx = with(density) { statusBarHeight.toPx() }.coerceAtLeast(0f)
        (sbPx / totalPx).coerceIn(0f, 0.72f)
    }

    val tier1Mask = remember(statusBarFraction) {
        val hold = (statusBarFraction + (1f - statusBarFraction) * 0.40f).coerceIn(0.32f, 0.78f)
        Brush.verticalGradient(
            0.0f to Color.Black,
            hold to Color.Black,
            lerp(hold, 1.0f, 0.28f) to Color.Black.copy(alpha = 0.84f),
            lerp(hold, 1.0f, 0.56f) to Color.Black.copy(alpha = 0.48f),
            lerp(hold, 1.0f, 0.80f) to Color.Black.copy(alpha = 0.16f),
            1.0f to Color.Transparent
        )
    }

    val tier2HeightFraction = remember(statusBarFraction) {
        (statusBarFraction + (1f - statusBarFraction) * 0.78f).coerceIn(0.55f, 0.88f)
    }
    val tier2Mask = remember(statusBarFraction, tier2HeightFraction) {
        val hold = ((statusBarFraction + (1f - statusBarFraction) * 0.16f) / tier2HeightFraction)
            .coerceIn(0.18f, 0.55f)
        val end = 0.90f
        Brush.verticalGradient(
            0.0f to Color.Black,
            hold to Color.Black,
            lerp(hold, end, 0.30f) to Color.Black.copy(alpha = 0.80f),
            lerp(hold, end, 0.62f) to Color.Black.copy(alpha = 0.36f),
            end to Color.Transparent,
            1.0f to Color.Transparent
        )
    }

    val tier3HeightFraction = remember(statusBarFraction) {
        (statusBarFraction + (1f - statusBarFraction) * 0.54f).coerceIn(0.42f, 0.76f)
    }
    val tier3Mask = remember(statusBarFraction, tier3HeightFraction) {
        val hold = (statusBarFraction / tier3HeightFraction).coerceIn(0.20f, 0.65f)
        val end = 0.88f
        Brush.verticalGradient(
            0.0f to Color.Black,
            hold to Color.Black,
            lerp(hold, end, 0.32f) to Color.Black.copy(alpha = 0.78f),
            lerp(hold, end, 0.64f) to Color.Black.copy(alpha = 0.34f),
            end to Color.Transparent,
            1.0f to Color.Transparent
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
            .graphicsLayer {
                alpha = alphaProvider().coerceIn(0f, 1f)
            }
    ) {
        ProgressiveBlurTier(
            backdrop = backdrop,
            blurRadius = shallowBlurRadius,
            heightFraction = 1.0f,
            maskBrush = tier1Mask
        )
        ProgressiveBlurTier(
            backdrop = backdrop,
            blurRadius = mediumBlurRadius,
            heightFraction = tier2HeightFraction,
            maskBrush = tier2Mask
        )
        ProgressiveBlurTier(
            backdrop = backdrop,
            blurRadius = deepBlurRadius,
            heightFraction = tier3HeightFraction,
            maskBrush = tier3Mask
        )
    }
}

@Composable
private fun ProgressiveBlurTier(
    backdrop: Backdrop,
    blurRadius: Dp,
    heightFraction: Float,
    maskBrush: Brush
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(heightFraction)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = maskBrush,
                    blendMode = BlendMode.DstIn
                )
            }
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = {
                    blur(blurRadius.toPx(), TileMode.Clamp)
                }
            )
    )
}

/**
 * Apple Music 风格纯透明渐进式高斯模糊导航栏。
 *
 * 顶部状态栏区域采用较深的高斯模糊，向下平滑过渡至浅层模糊并在底部自然融化至 0，
 * 不叠加任何白色雾罩或状态栏纯白背景。
 * 标题布局：保持在左侧（左对齐，不居中跳变），与大标题左对齐自然衔接。
 *
 * @param title 小标题文字
 * @param collapseFraction 折叠进度 (0f..1f)，二级页面常驻时可设为 1f
 * @param statusBarHeight 状态栏高度
 * @param modifier 外部修饰符
 * @param backdrop 背景采样 Backdrop
 * @param showBackButton 是否展示左侧 Apple 风格圆形玻璃返回按钮
 * @param onBackClick 返回按钮点击回调
 * @param trailingContent 顶栏右侧操作组件（如微型头像）
 */
@Composable
fun AppleMusicCollapsedTopBar(
    title: String,
    collapseFraction: Float,
    statusBarHeight: Dp,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    showBackButton: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val fullBlurAlpha = if (showBackButton) {
        (collapseFraction / 0.45f).coerceIn(0f, 1f)
    } else {
        ((collapseFraction - 0.16f) / 0.54f).coerceIn(0f, 1f)
    }

    // 详情页首帧（封面直通顶部状态栏）在未滑动前先保留一层轻柔的顶部状态栏渐进模糊，
    // 随上滑平滑过渡到完整的顶栏渐进模糊。
    val restingStatusBlurAlpha = if (showBackButton) {
        (1f - fullBlurAlpha).coerceIn(0f, 1f)
    } else {
        0f
    }

    // 小标题淡入：当大标题缩小接近顶部时，在左侧平滑淡入无缝接替
    val titleAlpha = if (showBackButton) {
        collapseFraction.coerceIn(0f, 1f)
    } else {
        ((collapseFraction - 0.68f) / 0.32f).coerceIn(0f, 1f)
    }

    val blurTotalHeight = statusBarHeight + 80.dp

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        if (backdrop != null && restingStatusBlurAlpha > 0.005f) {
            ProgressiveTopBlur(
                backdrop = backdrop,
                totalHeight = statusBarHeight + 22.dp,
                statusBarHeight = statusBarHeight,
                alphaProvider = { restingStatusBlurAlpha },
                deepBlurRadius = 16.dp,
                mediumBlurRadius = 8.dp,
                shallowBlurRadius = 3.dp
            )
        }

        if (backdrop != null && fullBlurAlpha > 0.005f) {
            ProgressiveTopBlur(
                backdrop = backdrop,
                totalHeight = blurTotalHeight,
                statusBarHeight = statusBarHeight,
                alphaProvider = { fullBlurAlpha }
            )
        }

        // 导航栏内容区域（状态栏高度之下）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarHeight)
                .height(52.dp)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧液态玻璃返回按钮（二级页面）
                if (showBackButton && onBackClick != null) {
                    LiquidGlassCircleButton(
                        onClick = onBackClick,
                        backdrop = backdrop,
                        size = 36.dp,
                        isDarkSurface = false,
                        contentDescription = "返回"
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color(0xFF111111),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                }

                // 标题文本：始终保持靠左对齐，不居中，字号 21sp 与缩小后的大标题无缝吻合
                Text(
                    text = title,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111),
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.graphicsLayer {
                        alpha = titleAlpha
                    }
                )
            }

            // 右侧操作项（如头像）
            if (trailingContent != null && titleAlpha > 0.05f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .graphicsLayer {
                            alpha = titleAlpha
                        }
                ) {
                    trailingContent()
                }
            }
        }
    }
}
