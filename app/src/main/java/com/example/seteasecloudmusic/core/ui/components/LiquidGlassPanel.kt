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

/** 浮层玻璃采样源 CompositionLocal；被采样子树内部不可直接作为采样目标，避免循环采样。 */
val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * 浮层液态玻璃面板修饰符（用于弹窗、占位卡片与半屏浮层）。
 * 当 [backdrop] 为空时降级为渐变不透明面板，并保留重力 3D 高光棱边。
 *
 * @param cornerRadius 面板圆角半径（默认 28.dp）
 * @param shadowElevation 外阴影弥散半径（默认 18.dp）
 */
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
                        blur(6.dp.toPx()) // 面板高斯模糊半径（6.dp）
                        noiseDither(GlassDefaults.NoiseDitherCoeff)
                        // 边缘透镜折射：折射高度 16.dp、折射量 20.dp，开启色散
                        lens(16.dp.toPx(), 20.dp.toPx(), chromaticAberration = true)
                    },
                    onDrawSurface = {
                        // 深色底 0.66 / 浅色底 0.50 铺底，兼顾透背景与文字对比度
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
