package com.example.seteasecloudmusic.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle

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
 * Apple Music 风格大标题（放置在可滚动容器的第一项，随动滚动并优雅微缩淡出）。
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
    // 在前 20% 保持清晰，20%~100% 随滑动逐渐平滑淡出
    val titleAlpha = (1f - ((collapseFraction - 0.20f) / 0.80f)).coerceIn(0f, 1f)
    // 经典 Apple Music 0.88 比例微缩，锚点在左侧居中，确保左对齐文字绝不发生向右漂移
    val titleScale = 1f - collapseFraction * 0.12f

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
 * Apple Music 风格顶部固定导航栏（覆盖状态栏，随滑动展示毛玻璃背景、居中小标题由下至上升起）。
 *
 * @param title 小标题文字
 * @param collapseFraction 折叠进度 (0f..1f)
 * @param statusBarHeight 状态栏高度
 * @param modifier 外部修饰符
 * @param backdrop 可选的 Backdrop 纹理
 * @param surfaceColor 顶栏背景色
 * @param surfaceAlpha 顶栏最大不透明度
 * @param trailingContent 顶栏折叠后右侧操作组件（如微型头像）
 */
@Composable
fun AppleMusicCollapsedTopBar(
    title: String,
    collapseFraction: Float,
    statusBarHeight: Dp,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    surfaceColor: Color = Color.White,
    surfaceAlpha: Float = 0.82f,
    trailingContent: (@Composable () -> Unit)? = null
) {
    // 导航栏背景在 15%~100% 渐进淡入
    val bgAlpha = ((collapseFraction - 0.15f) / 0.85f).coerceIn(0f, 1f)
    // 小标题与右侧动作项在 55%~100% 伴随微位移优雅淡入
    val titleAlpha = ((collapseFraction - 0.55f) / 0.45f).coerceIn(0f, 1f)
    val density = LocalDensity.current
    val translateY = with(density) { ((1f - titleAlpha) * 8.dp.toPx()) }

    val barHeight = statusBarHeight + 50.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
    ) {
        // 背景层（支持 Backdrop 毛玻璃或纯色磨砂材质）
        val bgModifier = if (backdrop != null) {
            Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(0.dp) },
                    effects = {
                        vibrancy()
                        blur(2f.dp.toPx())
                        lens(16f.dp.toPx(), 32f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(surfaceColor.copy(alpha = surfaceAlpha * bgAlpha))
                    }
                )
        } else {
            Modifier
                .fillMaxSize()
                .background(surfaceColor.copy(alpha = surfaceAlpha * bgAlpha))
        }

        Box(
            modifier = bgModifier.graphicsLayer {
                alpha = bgAlpha
            }
        )

        // 底部分割线（极细浅色分割线）
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    alpha = bgAlpha
                },
            thickness = 0.5.dp,
            color = Color(0x1F000000)
        )

        // 导航栏内容区域（状态栏高度之下）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarHeight)
                .height(50.dp)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111111),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha
                    translationY = translateY
                }
            )

            if (trailingContent != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .graphicsLayer {
                            alpha = titleAlpha
                            this.translationY = translateY
                        }
                ) {
                    trailingContent()
                }
            }
        }
    }
}
