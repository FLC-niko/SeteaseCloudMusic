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
import androidx.compose.ui.unit.DpOffset
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.shadow.Shadow
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
    return this
        .then(
            if (backdrop != null) {
                Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    highlight = null,
                    shadow = {
                        Shadow(
                            radius = shadowElevation,
                            offset = DpOffset(0.dp, 2.dp),
                            color = Color.Black.copy(alpha = if (dark) 0.14f else 0.18f)
                        )
                    },
                    effects = {
                        vibrancy()
                        blur(6.dp.toPx())
                        noiseDither(GlassDefaults.NoiseDitherCoeff)
                        lens(16.dp.toPx(), 20.dp.toPx(), chromaticAberration = true)
                    },
                    onDrawSurface = {
                        drawRect(containerColor.copy(alpha = if (dark) 0.66f else 0.50f))
                        drawRect(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (dark) 0.03f else 0.20f),
                                    Color.Transparent,
                                    Color.White.copy(alpha = if (dark) 0.02f else 0.06f)
                                )
                            )
                        )
                    }
                )
            } else {
                Modifier
                    .shadow(
                        shadowElevation,
                        shape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.10f),
                        spotColor = Color.Black.copy(alpha = 0.18f)
                    )
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                containerColor.copy(alpha = 0.96f),
                                containerColor.copy(alpha = 0.90f)
                            )
                        ),
                        shape
                    )
            }
        )
        .bloomStroke(
            cornerRadius = cornerRadius,
            style = if (dark) BloomStrokeDefaults.PanelDark else BloomStrokeDefaults.PanelLight,
            extraDegrees = -45f
        )
        .clip(shape)
}

@Composable
fun LiquidGlassPanel(
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalGlassBackdrop.current,
    containerColor: Color = AppTheme.colors.glassPanelContainer,
    cornerRadius: Dp = 28.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.liquidGlassPanel(backdrop, containerColor, cornerRadius), content = content)
}
