package com.example.seteasecloudmusic.feature.main.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 底部导航栏与迷你播放条共用的水平居中框架。
 *
 * @param horizontalPadding 左右基础外边距（dp）
 */
internal fun Modifier.horizontalChromeFrame(horizontalPadding: Dp = 20.dp): Modifier =
    // 宽屏或折叠屏展开态下限制最大居中宽度为 620.dp
    padding(horizontal = horizontalPadding).widthIn(max = 620.dp).fillMaxWidth()

/**
 * 迷你播放条在紧凑态下的水平缩进框架：在 620.dp 最大宽度内部对称预留两侧按钮及间距宽度。
 *
 * @param progress 紧凑收缩进度（0f 展开 ~ 1f 收起对齐两侧圆形按钮）
 * @param horizontalPadding 外层水平边距（dp）
 * @param buttonSize 两侧圆形按钮尺寸（dp）
 * @param buttonGap 迷你播放条与两侧按钮之间的间距（dp）
 */
internal fun Modifier.horizontalMiniPlayerFrame(
    progress: Float,
    horizontalPadding: Dp = 20.dp,
    buttonSize: Dp = 54.dp,
    buttonGap: Dp = 10.dp
): Modifier = horizontalChromeFrame(horizontalPadding)
    .padding(horizontal = (buttonSize + buttonGap) * progress.coerceIn(0f, 1f))

/**
 * 布局阶段读取水平收缩进度的重载版本：与 [horizontalMiniPlayerFrame] 像素几何一致，
 * 避免在组合阶段读取动画状态而触发父容器与内部 liquidGlass 重组。
 *
 * @param progressProvider 紧凑收缩进度提供器（0f ~ 1f）
 * @param horizontalPadding 外层水平边距（dp）
 * @param buttonSize 两侧圆形按钮尺寸（dp）
 * @param buttonGap 迷你播放条与两侧按钮之间的间距（dp）
 */
internal fun Modifier.horizontalMiniPlayerFrame(
    progressProvider: () -> Float,
    horizontalPadding: Dp = 20.dp,
    buttonSize: Dp = 54.dp,
    buttonGap: Dp = 10.dp
): Modifier = horizontalChromeFrame(horizontalPadding).layout { measurable, constraints ->
    val insetPx = ((buttonSize + buttonGap).toPx() * progressProvider().coerceIn(0f, 1f)).roundToInt()
    val horizontalInset = insetPx * 2
    val childMaxWidth = (constraints.maxWidth - horizontalInset).coerceAtLeast(0)
    val childMinWidth = (constraints.minWidth - horizontalInset).coerceAtLeast(0).coerceAtMost(childMaxWidth)
    val placeable = measurable.measure(
        constraints.copy(minWidth = childMinWidth, maxWidth = childMaxWidth)
    )
    layout(
        width = constraints.constrainWidth(placeable.width + horizontalInset),
        height = placeable.height
    ) {
        placeable.placeRelative(insetPx, 0)
    }
}
