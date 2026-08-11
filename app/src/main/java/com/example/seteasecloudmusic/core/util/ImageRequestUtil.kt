package com.example.seteasecloudmusic.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil.request.ImageRequest

/**
 * 统一封面图片请求构建工具。
 *
 * 所有列表封面、海报格统一走此入口，保证：
 * 1. 按目标尺寸请求采样，避免小封面解码大位图（滑动抖动的主要来源之一）。
 * 2. 高速滚动列表关闭 crossfade（默认），避免列表滚动时额外绘制压力；
 *    需要淡入过渡的大图场景可单独传 [crossfade] = true。
 *
 * @param imageUrl 图片地址（空/空串返回 null）
 * @param targetSize 目标显示尺寸（px 按 density 换算）
 * @param crossfade 是否开启淡入过渡，默认关闭
 */
@Composable
fun rememberCoverRequest(
    imageUrl: String?,
    targetSize: Dp,
    crossfade: Boolean = false
): ImageRequest? {
    if (imageUrl.isNullOrBlank()) return null

    val context = LocalContext.current
    val density = LocalDensity.current
    val targetSizePx = with(density) { targetSize.roundToPx() }.coerceAtLeast(1)
    return remember(imageUrl, targetSizePx, crossfade) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .size(targetSizePx, targetSizePx)
            .crossfade(crossfade)
            .build()
    }
}
