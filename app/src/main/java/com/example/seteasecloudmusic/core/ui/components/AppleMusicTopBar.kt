package com.example.seteasecloudmusic.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur

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
        if (trailingContent != null) {
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
 * Apple Music 风格纯透明渐变模糊导航栏。
 *
 * 纯透明模糊：不叠加任何白色雾罩（不泛白），通过垂直渐变遮罩将模糊强度在底部优雅融化到 0。
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
    val blurAlpha = if (showBackButton) {
        (0.20f + collapseFraction * 0.80f).coerceIn(0f, 1f)
    } else {
        ((collapseFraction - 0.40f) / 0.60f).coerceIn(0f, 1f)
    }

    // 小标题淡入：当大标题缩小接近顶部时，在左侧平滑淡入无缝接替
    val titleAlpha = if (showBackButton) {
        collapseFraction.coerceIn(0f, 1f)
    } else {
        ((collapseFraction - 0.68f) / 0.32f).coerceIn(0f, 1f)
    }

    val barHeight = statusBarHeight + 52.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
    ) {
        // 顶栏背景过渡层：若提供了 backdrop 则应用实时液态模糊；若未提供（如为了顶级滑动性能），采用轻量级半透明磨砂渐变遮罩
        if (blurAlpha > 0f) {
            if (backdrop != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = blurAlpha
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.Black,
                                    0.60f to Color.Black.copy(alpha = 0.90f),
                                    0.85f to Color.Black.copy(alpha = 0.35f),
                                    1.0f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawPlainBackdrop(
                                backdrop = backdrop,
                                shape = { RectangleShape },
                                effects = {
                                    blur(16f.dp.toPx())
                                }
                            )
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = blurAlpha }
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.White.copy(alpha = 0.96f),
                                0.65f to Color.White.copy(alpha = 0.90f),
                                0.88f to Color.White.copy(alpha = 0.45f),
                                1.0f to Color.Transparent
                            )
                        )
                )
            }
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
                // 左侧返回按钮（二级页面）
                if (showBackButton && onBackClick != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.06f))
                            .border(0.5.dp, Color.Black.copy(alpha = 0.12f), CircleShape)
                            .clickable(onClick = onBackClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
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
            if (trailingContent != null) {
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
