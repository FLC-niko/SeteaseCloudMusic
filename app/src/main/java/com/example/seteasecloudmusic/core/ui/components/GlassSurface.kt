package com.example.seteasecloudmusic.core.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle

/**
 * 液态玻璃（Liquid Glass）统一光学参数中心，对齐 LSPosed v2.2.1 液态玻璃底栏效果。
 *
 * 采用「vibrancy 提鲜 + 4.dp 高斯模糊 + 可选噪点抖动 + 边缘透镜折射 + 柔弥散外阴影 +
 * 重力联动 3D SDF 棱边高光（BloomStroke）」构成完整物理玻璃质感。
 */
object GlassDefaults {
    /** 基础模糊半径：对齐 LSPosed 外框 RenderEffect sigma (4.dp * 0.45 ≈ 1.8.dp)，保持通透清澈 */
    val BlurRadius: Dp = 1.8.dp

    /** 透镜折射高度（越大边缘的玻璃厚度感越强） */
    val LensRefractionHeight: Dp = 24.dp

    /** 透镜折射量（越大边缘内容扭曲越明显） */
    val LensRefractionAmount: Dp = 24.dp

    /** 表面半透明铺底透明度：主导航条/卡片的主用值 */
    const val SurfaceAlpha: Float = 0.14f

    /** 搜索栏、播放栏等亮面玻璃面板铺底透明度 */
    const val SurfaceAlphaBright: Float = 0.20f

    /** 兼容保留的描边透明度常量 */
    const val BorderAlpha: Float = 0.62f

    /** 外阴影弥散半径 */
    val OuterShadowRadius: Dp = 10.dp

    /** 浅色背景玻璃外阴影透明度 */
    const val OuterShadowAlphaLight: Float = 0.16f

    /** 深色背景玻璃外阴影透明度 */
    const val OuterShadowAlphaDark: Float = 0.10f

    /** 模糊后像素防色带高频抖动系数 */
    const val NoiseDitherCoeff: Float = 0.0045f
}

/**
 * 钳制透镜折射高度，确保高度不超过组件短边 40%（`0.40 * minDimension < 0.50 * minDimension`），
 * 防止较窄胶囊条因中心线处 `sign(centeredCoord.y)` 突变出现横向折痕。
 */
internal fun clampLensRefractionHeightPx(desiredPx: Float, minDimensionPx: Float): Float {
    if (desiredPx <= 0f || minDimensionPx <= 0f) return 0f
    return minOf(desiredPx, minDimensionPx * 0.40f)
}

/**
 * 为任意 Modifier 叠加 LSPosed 风格液态玻璃背景：
 * 色彩提鲜 + 高斯模糊 + 可选防色带噪点 + 透镜折射 + 柔阴影 + 3D 重力棱边高光（BloomStroke）。
 */
fun Modifier.liquidGlass(
    backdrop: Backdrop,
    cornerRadius: Dp,
    surfaceAlpha: Float = GlassDefaults.SurfaceAlpha,
    refractionHeight: Dp = GlassDefaults.LensRefractionHeight,
    refractionAmount: Dp = GlassDefaults.LensRefractionAmount,
    blurRadius: Dp = GlassDefaults.BlurRadius,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    isDarkSurface: Boolean = false,
    surfaceColor: Color = if (isDarkSurface) Color(0xFF1C1C24) else Color(0xFFF7F7FA),
    enableShadow: Boolean = true,
    shadowRadius: Dp = GlassDefaults.OuterShadowRadius,
    enableBloomStroke: Boolean = true,
    bloomExtraDegrees: Float = -45f,
    bloomStyle: BloomStrokeStyle = if (isDarkSurface) BloomStrokeDefaults.OuterDark else BloomStrokeDefaults.OuterLight,
    enableNoiseDither: Boolean = false,
    depthEffect: Boolean = false,
    chromaticAberration: Boolean = false,
    exportedBackdrop: LayerBackdrop? = null
): Modifier {
    val shadowSpec = if (enableShadow && shadowRadius > 0.dp) {
        Shadow(
            radius = shadowRadius,
            offset = DpOffset(0.dp, 2.dp),
            color = Color.Black.copy(
                alpha = if (isDarkSurface) GlassDefaults.OuterShadowAlphaDark else GlassDefaults.OuterShadowAlphaLight
            )
        )
    } else null

    return this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedRectangle(cornerRadius) },
            highlight = null,
            shadow = if (shadowSpec != null) ({ shadowSpec }) else null,
            effects = {
                vibrancy()
                if (blurRadius > 0.dp) {
                    blur(blurRadius.toPx())
                }
                if (enableNoiseDither) {
                    noiseDither(GlassDefaults.NoiseDitherCoeff)
                }
                if (refractionHeight > 0.dp && refractionAmount > 0.dp) {
                    val rawHeightPx = refractionHeight.toPx()
                    val effectiveHeightPx = clampLensRefractionHeightPx(rawHeightPx, size.minDimension)
                    if (effectiveHeightPx > 0f) {
                        val scaleRatio = (effectiveHeightPx / rawHeightPx).coerceIn(0f, 1f)
                        lens(
                            refractionHeight = effectiveHeightPx,
                            refractionAmount = refractionAmount.toPx() * scaleRatio,
                            depthEffect = depthEffect,
                            chromaticAberration = chromaticAberration
                        )
                    }
                }
            },
            layerBlock = layerBlock,
            exportedBackdrop = exportedBackdrop,
            onDrawSurface = {
                val effectiveAlpha = if (isRenderEffectSupported()) {
                    surfaceAlpha
                } else {
                    maxOf(surfaceAlpha, if (isDarkSurface) 0.72f else 0.76f)
                }
                if (effectiveAlpha > 0f) {
                    drawRect(color = surfaceColor.copy(alpha = effectiveAlpha))
                }
            }
        )
        .then(
            if (enableBloomStroke) {
                Modifier.bloomStroke(
                    cornerRadius = cornerRadius,
                    style = bloomStyle,
                    extraDegrees = bloomExtraDegrees
                )
            } else {
                Modifier
            }
        )
}

/**
 * 与 [liquidGlass] 配套的 3D 重力高光棱边描边（升级自原先平铺白边，接入全局重力扫光）。
 */
fun Modifier.glassBorder(
    cornerRadius: Dp,
    width: Dp = 1.dp,
    color: Color = Color.White.copy(alpha = 0.14f),
    isDarkSurface: Boolean = false,
    extraDegrees: Float = -45f
): Modifier {
    val base = if (isDarkSurface) BloomStrokeDefaults.OuterDark else BloomStrokeDefaults.OuterLight
    return this.bloomStroke(
        cornerRadius = cornerRadius,
        style = base.copy(strokeWidth = width, strokeColor = color),
        extraDegrees = extraDegrees
    )
}

/**
 * 液态玻璃卡片容器：玻璃背景 + 防色带噪点 + 重力 3D 棱边高光 + 可选指尖聚光灯 + 内容槽位。
 */
@Composable
fun GlassSurface(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    surfaceAlpha: Float = GlassDefaults.SurfaceAlpha,
    borderWidth: Dp = 1.dp,
    isDarkSurface: Boolean = AppTheme.isDark,
    interactiveHighlight: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val baseBloom = if (isDarkSurface) BloomStrokeDefaults.OuterDark else BloomStrokeDefaults.OuterLight
    Box(
        modifier = modifier
            .liquidGlass(
                backdrop = backdrop,
                cornerRadius = cornerRadius,
                surfaceAlpha = surfaceAlpha,
                isDarkSurface = isDarkSurface,
                enableShadow = true,
                enableBloomStroke = borderWidth > 0.dp,
                bloomStyle = if (borderWidth > 0.dp) baseBloom.copy(strokeWidth = borderWidth) else baseBloom,
                enableNoiseDither = true
            )
            .interactiveGlassHighlight(
                shape = RoundedRectangle(cornerRadius),
                enabled = interactiveHighlight,
                isDarkSurface = isDarkSurface
            ),
        content = content
    )
}

/**
 * 圆形液态玻璃图标按钮（折射 + 模糊 + 鲜艳度 + 重力 3D 棱边高光 + 指尖聚光灯 + 按压微缩回弹）。
 *
 * 适用于顶部返回、更多菜单等小尺寸圆形悬浮按钮；当 [backdrop] 为空时平滑退化为半透明磨砂圆钮。
 */
@Composable
fun LiquidGlassCircleButton(
    onClick: () -> Unit,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    isDarkSurface: Boolean = AppTheme.isDark,
    enabled: Boolean = true,
    interactiveHighlight: Boolean = true,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 700f
        ),
        label = "liquidGlassCirclePressScale"
    )
    val indication = LocalIndication.current
    val radius = size / 2
    val compactBloom = if (isDarkSurface) {
        BloomStrokeDefaults.CompactControl.copy(lightSurfaceSubtleOutlineAlpha = 0f)
    } else {
        BloomStrokeDefaults.CompactControl
    }

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            )
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .then(
                if (backdrop != null) {
                    Modifier.liquidGlass(
                        backdrop = backdrop,
                        cornerRadius = radius,
                        surfaceAlpha = if (isDarkSurface) 0.14f else 0.22f,
                        blurRadius = GlassDefaults.BlurRadius,
                        refractionHeight = 10.dp,
                        refractionAmount = 14.dp,
                        isDarkSurface = isDarkSurface,
                        enableShadow = true,
                        shadowRadius = 8.dp,
                        enableBloomStroke = true,
                        bloomStyle = compactBloom
                    )
                } else {
                    Modifier
                        .background(
                            color = if (isDarkSurface) {
                                Color(0xFF1C1C24).copy(alpha = 0.72f)
                            } else {
                                Color.White.copy(alpha = 0.74f)
                            },
                            shape = CircleShape
                        )
                        .bloomStroke(
                            cornerRadius = radius,
                            style = compactBloom
                        )
                }
            )
            .interactiveGlassHighlight(
                shape = CircleShape,
                enabled = enabled && interactiveHighlight,
                isDarkSurface = isDarkSurface
            )
            .clip(CircleShape)
            .indication(interactionSource = interactionSource, indication = indication)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}
