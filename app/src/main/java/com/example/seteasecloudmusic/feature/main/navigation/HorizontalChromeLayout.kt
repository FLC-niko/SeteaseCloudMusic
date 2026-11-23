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

/** Navigation and playback share the same centred width, including in a fold's display segment. */
internal fun Modifier.horizontalChromeFrame(horizontalPadding: Dp = 20.dp): Modifier =
    padding(horizontal = horizontalPadding).widthIn(max = 620.dp).fillMaxWidth()

/** Reserve button space inside the capped frame, so the width limit cannot consume the inset. */
internal fun Modifier.horizontalMiniPlayerFrame(
    progress: Float,
    horizontalPadding: Dp = 20.dp,
    buttonSize: Dp = 54.dp,
    buttonGap: Dp = 10.dp
): Modifier = horizontalChromeFrame(horizontalPadding)
    .padding(horizontal = (buttonSize + buttonGap) * progress.coerceIn(0f, 1f))

/**
 * 布局阶段读取水平收缩进度的重载版本：与 [horizontalMiniPlayerFrame] 像素几何完全一致，
 * 但不会在组合阶段读取动画状态，避免触发父容器与内部 liquidGlass 重组重建 ShapeProvider。
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
