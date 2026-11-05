package com.example.seteasecloudmusic.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle

/** Overlay sampling source. Content inside this source must use a separate background layer. */
val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/** A readable floating panel using the same optical material as the compact navigation. */
fun Modifier.liquidGlassPanel(
    backdrop: Backdrop?,
    containerColor: Color = Color.White,
    cornerRadius: Dp = 28.dp,
    shadowElevation: Dp = 18.dp
): Modifier {
    val dark = containerColor.luminance() < 0.5f
    val shape = RoundedRectangle(cornerRadius)
    val highlights = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = if (dark) 0.28f else 0.82f),
            Color.White.copy(alpha = if (dark) 0.04f else 0.16f),
            Color.White.copy(alpha = if (dark) 0.14f else 0.48f))
    )
    return this
        .shadow(shadowElevation, shape, clip = false,
            ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.18f))
        .then(if (backdrop != null) Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            shadow = null,
            effects = {
                vibrancy()
                blur(18.dp.toPx())
                lens(8.dp.toPx(), 18.dp.toPx(), chromaticAberration = true)
            },
            onDrawSurface = {
                drawRect(containerColor.copy(alpha = if (dark) 0.78f else 0.64f))
                drawRect(Brush.verticalGradient(listOf(
                    Color.White.copy(alpha = if (dark) 0.03f else 0.20f), Color.Transparent,
                    Color.White.copy(alpha = if (dark) 0.02f else 0.06f))))
            }
        ) else Modifier.background(Brush.verticalGradient(listOf(
            containerColor.copy(alpha = 0.96f), containerColor.copy(alpha = 0.90f))), shape))
        .border(0.75.dp, highlights, shape)
        .clip(shape)
}

@Composable
fun LiquidGlassPanel(
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalGlassBackdrop.current,
    containerColor: Color = Color.White,
    cornerRadius: Dp = 28.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.liquidGlassPanel(backdrop, containerColor, cornerRadius), content = content)
}
