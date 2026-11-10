package com.example.seteasecloudmusic.core.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity

/** 仅裁掉滚动内容侵入状态栏的部分，保留页面背景、布局和标题原有动效。 */
fun Modifier.clipBelowStatusBar(): Modifier = composed {
    val statusBars = WindowInsets.statusBars
    val density = LocalDensity.current
    var originY by remember { mutableFloatStateOf(0f) }
    onGloballyPositioned { originY = it.positionInWindow().y }
        .drawWithContent {
            val safeTop = (statusBars.getTop(density) - originY).coerceIn(0f, size.height)
            clipRect(top = safeTop) { this@drawWithContent.drawContent() }
        }
}
