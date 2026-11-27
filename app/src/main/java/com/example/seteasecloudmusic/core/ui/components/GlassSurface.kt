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
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.util.lerp
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import com.example.seteasecloudmusic.core.ui.theme.LiquidGlassAppearance
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
 * 液态玻璃（Liquid Glass）统一光学参数中心。
 *
 * 组合 vibrancy 提鲜、高斯模糊、可选高频噪点抖动、边缘透镜折射、外阴影与
 * 重力联动 3D SDF 棱边高光（BloomStroke）。
 */
object GlassDefaults {
    /** 基础模糊半径（dp，约对应 RenderEffect sigma 4.dp * 0.45） */
    val BlurRadius: Dp = 1.8.dp

    /** 透镜折射高度（dp，越大边缘玻璃厚度感越强） */
    val LensRefractionHeight: Dp = 24.dp

    /** 透镜折射量（dp，越大边缘内容弯曲扭曲越明显） */
    val LensRefractionAmount: Dp = 24.dp

    /** 表面半透明铺底透明度：主导航条与常规卡片主用值 */
    const val SurfaceAlpha: Float = 0.14f

    /** 搜索栏、播放栏等亮面玻璃面板铺底透明度 */
    const val SurfaceAlphaBright: Float = 0.20f

    /** 传统玻璃描边透明度常量 */
    const val BorderAlpha: Float = 0.62f

    /** 外阴影弥散半径（dp） */
    val OuterShadowRadius: Dp = 10.dp

    /** 浅色背景玻璃外阴影透明度 */
    const val OuterShadowAlphaLight: Float = 0.16f

    /** 深色背景玻璃外阴影透明度 */
    const val OuterShadowAlphaDark: Float = 0.10f

    /** 高频噪点抖动强度，用于抑制高斯模糊后的 8-bit 色带条纹 */
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
 * 解析后的液态玻璃光学参数。
 */
@Immutable
data class ResolvedLiquidGlassParams(
    val surfaceAlpha: Float,
    val fallbackSurfaceAlpha: Float,
    val blurRadius: Dp,
    val refractionHeight: Dp,
    val refractionAmount: Dp,
    val subtleOutlineAlphaBoost: Float
)

/**
 * 根据全局 [LiquidGlassAppearance] 解析标准液态玻璃表面参数；
 * `opacityLevel == 0f` 时保持原有默认参数零回归。
 */
fun resolveLiquidGlassParams(
    baseSurfaceAlpha: Float,
    baseBlurRadius: Dp,
    baseRefractionHeight: Dp,
    baseRefractionAmount: Dp,
    isDarkSurface: Boolean,
    appearance: LiquidGlassAppearance
): ResolvedLiquidGlassParams {
    val level = appearance.clampedLevel
    val defaultFallback = maxOf(baseSurfaceAlpha, if (isDarkSurface) 0.72f else 0.76f)
    if (level == 0f) {
        return ResolvedLiquidGlassParams(
            surfaceAlpha = baseSurfaceAlpha,
            fallbackSurfaceAlpha = defaultFallback,
            blurRadius = baseBlurRadius,
            refractionHeight = baseRefractionHeight,
            refractionAmount = baseRefractionAmount,
            subtleOutlineAlphaBoost = 0f
        )
    }

    return if (level < 0f) {
        val clearT = -level
        val surfaceAlpha = lerp(
            baseSurfaceAlpha,
            baseSurfaceAlpha * if (isDarkSurface) 0.14f else 0.08f,
            clearT
        )
        val fallbackAlpha = maxOf(
            surfaceAlpha,
            lerp(
                if (isDarkSurface) 0.72f else 0.76f,
                if (isDarkSurface) 0.46f else 0.50f,
                clearT
            )
        )
        val blurRadius = if (baseBlurRadius > 0.dp) {
            lerp(baseBlurRadius.value, 0f, clearT).dp
        } else {
            0.dp
        }
        ResolvedLiquidGlassParams(
            surfaceAlpha = surfaceAlpha,
            fallbackSurfaceAlpha = fallbackAlpha,
            blurRadius = blurRadius,
            refractionHeight = baseRefractionHeight * lerp(1f, 1.10f, clearT),
            refractionAmount = baseRefractionAmount * lerp(1f, 1.22f, clearT),
            subtleOutlineAlphaBoost = if (isDarkSurface) 0f else 0.03f * clearT
        )
    } else {
        val tintedT = level
        val maxTintedAlpha = if (isDarkSurface) 0.74f else 0.80f
        val surfaceAlpha = lerp(
            baseSurfaceAlpha,
            maxOf(baseSurfaceAlpha, maxTintedAlpha),
            tintedT
        )
        val fallbackAlpha = maxOf(
            surfaceAlpha,
            lerp(
                if (isDarkSurface) 0.72f else 0.76f,
                if (isDarkSurface) 0.90f else 0.94f,
                tintedT
            )
        )
        val blurCurve = tintedT * (0.78f + 0.22f * tintedT)
        val blurRadius = if (baseBlurRadius > 0.dp) {
            lerp(baseBlurRadius.value, maxOf(baseBlurRadius.value, 14f), blurCurve).dp
        } else {
            lerp(0f, 12f, blurCurve).dp
        }
        ResolvedLiquidGlassParams(
            surfaceAlpha = surfaceAlpha,
            fallbackSurfaceAlpha = fallbackAlpha,
            blurRadius = blurRadius,
            refractionHeight = baseRefractionHeight * lerp(1f, 0.72f, tintedT),
            refractionAmount = baseRefractionAmount * lerp(1f, 0.52f, tintedT),
            subtleOutlineAlphaBoost = 0f
        )
    }
}

/**
 * 根据全局 [LiquidGlassAppearance] 解析浮层面板（[liquidGlassPanel]）光学参数。
 */
fun resolveLiquidGlassPanelParams(
    isDarkSurface: Boolean,
    appearance: LiquidGlassAppearance
): ResolvedLiquidGlassParams {
    val level = appearance.clampedLevel
    val baseAlpha = if (isDarkSurface) 0.66f else 0.50f
    val baseBlur = 6.dp
    val baseRefractionHeight = 16.dp
    val baseRefractionAmount = 20.dp
    val defaultFallback = maxOf(baseAlpha, if (isDarkSurface) 0.72f else 0.76f)
    if (level == 0f) {
        return ResolvedLiquidGlassParams(
            surfaceAlpha = baseAlpha,
            fallbackSurfaceAlpha = defaultFallback,
            blurRadius = baseBlur,
            refractionHeight = baseRefractionHeight,
            refractionAmount = baseRefractionAmount,
            subtleOutlineAlphaBoost = 0f
        )
    }

    return if (level < 0f) {
        val clearT = -level
        val surfaceAlpha = lerp(baseAlpha, if (isDarkSurface) 0.30f else 0.20f, clearT)
        ResolvedLiquidGlassParams(
            surfaceAlpha = surfaceAlpha,
            fallbackSurfaceAlpha = maxOf(surfaceAlpha, lerp(defaultFallback, if (isDarkSurface) 0.46f else 0.50f, clearT)),
            blurRadius = lerp(baseBlur.value, 1.2f, clearT).dp,
            refractionHeight = lerp(baseRefractionHeight.value, 18f, clearT).dp,
            refractionAmount = lerp(baseRefractionAmount.value, 24f, clearT).dp,
            subtleOutlineAlphaBoost = if (isDarkSurface) 0f else 0.03f * clearT
        )
    } else {
        val tintedT = level
        val surfaceAlpha = lerp(baseAlpha, if (isDarkSurface) 0.88f else 0.86f, tintedT)
        ResolvedLiquidGlassParams(
            surfaceAlpha = surfaceAlpha,
            fallbackSurfaceAlpha = maxOf(surfaceAlpha, lerp(defaultFallback, if (isDarkSurface) 0.92f else 0.94f, tintedT)),
            blurRadius = lerp(baseBlur.value, 18f, tintedT).dp,
            refractionHeight = lerp(baseRefractionHeight.value, 12f, tintedT).dp,
            refractionAmount = lerp(baseRefractionAmount.value, 12f, tintedT).dp,
            subtleOutlineAlphaBoost = 0f
        )
    }
}

/**
 * 为任意 Modifier 叠加液态玻璃背景：
 * 色彩提鲜 + 高斯模糊 + 可选防色带噪点 + 透镜折射 + 外阴影 + 3D 重力棱边高光（BloomStroke）。
 */
@Composable
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
    exportedBackdrop: LayerBackdrop? = null,
    appearance: LiquidGlassAppearance = AppTheme.liquidGlass
): Modifier {
    val resolved = resolveLiquidGlassParams(
        baseSurfaceAlpha = surfaceAlpha,
        baseBlurRadius = blurRadius,
        baseRefractionHeight = refractionHeight,
        baseRefractionAmount = refractionAmount,
        isDarkSurface = isDarkSurface,
        appearance = appearance
    )
    val effectiveBloomStyle = if (enableBloomStroke && resolved.subtleOutlineAlphaBoost > 0f) {
        bloomStyle.copy(
            lightSurfaceSubtleOutlineAlpha = (bloomStyle.lightSurfaceSubtleOutlineAlpha + resolved.subtleOutlineAlphaBoost)
                .coerceIn(0f, 1f)
        )
    } else {
        bloomStyle
    }

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
                if (resolved.blurRadius > 0.dp) {
                    blur(resolved.blurRadius.toPx())
                }
                if (enableNoiseDither) {
                    noiseDither(GlassDefaults.NoiseDitherCoeff)
                }
                if (resolved.refractionHeight > 0.dp && resolved.refractionAmount > 0.dp) {
                    val rawHeightPx = resolved.refractionHeight.toPx()
                    val effectiveHeightPx = clampLensRefractionHeightPx(rawHeightPx, size.minDimension)
                    if (effectiveHeightPx > 0f) {
                        val scaleRatio = (effectiveHeightPx / rawHeightPx).coerceIn(0f, 1f)
                        lens(
                            refractionHeight = effectiveHeightPx,
                            refractionAmount = resolved.refractionAmount.toPx() * scaleRatio,
                            depthEffect = depthEffect,
                            chromaticAberration = chromaticAberration
                        )
                    }
                }
            },
            layerBlock = layerBlock,
            exportedBackdrop = exportedBackdrop,
            onDrawSurface = {
                // 不支持 RenderEffect 的设备提高底色不透明度以保证前景文字可读性
                val effectiveAlpha = if (isRenderEffectSupported()) {
                    resolved.surfaceAlpha
                } else {
                    resolved.fallbackSurfaceAlpha
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
                    style = effectiveBloomStyle,
                    extraDegrees = bloomExtraDegrees
                )
            } else {
                Modifier
            }
        )
}

/**
 * 与 [liquidGlass] 配套的 3D 重力高光棱边描边，接入全局重力扫光方向。
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
 * 圆形液态玻璃图标按钮（折射 + 模糊 + 鲜艳度 + 重力 3D 棱边高光 + 按压缩放）。
 *
 * 适用于顶部返回、更多菜单等小尺寸圆形悬浮按钮；当 [backdrop] 为空时降级为半透明圆角背景。
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
    // 按压缩放至 0.92f，使用无回弹刚性弹簧（stiffness = 700）快速响应
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
