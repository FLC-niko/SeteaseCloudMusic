package com.example.seteasecloudmusic.core.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.intellij.lang.annotations.Language
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 3D 倒角法线高光光源配置，支持跟随设备重力旋转或保持固定方向。
 *
 * @property extraDegrees 相对设备重力方向的额外偏转角（度）
 * @property z 光源 Z 轴深度分量（负值位于屏幕前方，绝对值越小边缘高光越集中）
 * @property color 光源高光颜色
 * @property intensity 高光强度倍率
 * @property followGravity 是否随设备倾斜改变光源方向
 * @property staticDirX 固定光源 X 分量（仅 `followGravity = false` 时使用）
 * @property staticDirY 固定光源 Y 分量（仅 `followGravity = false` 时使用）
 */
@Immutable
data class BloomLightSource(
    val extraDegrees: Float = 0f,
    val z: Float = -0.05f,
    val color: Color = Color.White,
    val intensity: Float = 1f,
    val followGravity: Boolean = true,
    val staticDirX: Float = 0f,
    val staticDirY: Float = 0.196f
)

/**
 * 3D SDF 玻璃棱边高光样式配置。
 *
 * @property strokeWidth 边缘描边宽度（dp）
 * @property innerBlurRadius 内部 3D 半球倒角法线深度（dp），决定高光衰减宽度与折角焦散范围
 * @property strokeColor 基础边缘反光颜色
 * @property primaryLight 主高光光源
 * @property secondaryLight 辅助高光光源（可固定或反相旋转）
 * @property dualPeak 是否启用双方向轴向衰减（产生对角棱边双峰焦散）
 * @property blendMode 混合模式（默认 [BlendMode.Plus] 加法高光）
 * @property alpha 整体高光透明度
 * @property lightSurfaceSubtleOutlineAlpha 浅色背景下的细轮廓补偿透明度，防止浅底融边
 */
@Immutable
data class BloomStrokeStyle(
    val strokeWidth: Dp = 1.dp,
    val innerBlurRadius: Dp = 2.dp,
    val strokeColor: Color = Color.White.copy(alpha = 0.12f),
    val primaryLight: BloomLightSource = BloomLightSource(
        extraDegrees = 0f,
        z = -0.05f,
        color = Color.White,
        intensity = 1f,
        followGravity = true
    ),
    val secondaryLight: BloomLightSource = BloomLightSource(
        extraDegrees = 180f,
        z = -0.5f,
        color = Color.White,
        intensity = 0.4f,
        followGravity = false,
        staticDirX = 0f,
        staticDirY = 0.196f
    ),
    val dualPeak: Boolean = true,
    val blendMode: BlendMode = BlendMode.Plus,
    val alpha: Float = 1f,
    val lightSurfaceSubtleOutlineAlpha: Float = 0f
)

/** 各形态液态玻璃容器的 [BloomStrokeStyle] 默认预设集。 */
object BloomStrokeDefaults {
    /** 浅色背景外框预设（带 0.06f 暗轮廓补偿） */
    val OuterLight: BloomStrokeStyle = BloomStrokeStyle(
        strokeWidth = 1.dp,
        innerBlurRadius = 2.dp,
        strokeColor = Color.White.copy(alpha = 0.14f),
        primaryLight = BloomLightSource(
            extraDegrees = 0f,
            z = -0.05f,
            color = Color.White,
            intensity = 1.0f,
            followGravity = true
        ),
        secondaryLight = BloomLightSource(
            extraDegrees = 180f,
            z = -0.35f,
            color = Color.White,
            intensity = 0.52f,
            followGravity = true
        ),
        dualPeak = true,
        blendMode = BlendMode.Plus,
        lightSurfaceSubtleOutlineAlpha = 0.06f
    )

    /** 深色背景外框预设 */
    val OuterDark: BloomStrokeStyle = BloomStrokeStyle(
        strokeWidth = 1.dp,
        innerBlurRadius = 2.dp,
        strokeColor = Color.White.copy(alpha = 0.16f),
        primaryLight = BloomLightSource(
            extraDegrees = 0f,
            z = -0.05f,
            color = Color.White,
            intensity = 1.0f,
            followGravity = true
        ),
        secondaryLight = BloomLightSource(
            extraDegrees = 180f,
            z = -0.35f,
            color = Color.White,
            intensity = 0.52f,
            followGravity = true
        ),
        dualPeak = true,
        blendMode = BlendMode.Plus,
        lightSurfaceSubtleOutlineAlpha = 0f
    )

    /** 分段滑块激活态水滴棱边预设（副光 z=-0.50，强度 0.45） */
    val DropletPill: BloomStrokeStyle = BloomStrokeStyle(
        strokeWidth = 1.dp,
        innerBlurRadius = 2.dp,
        strokeColor = Color.White.copy(alpha = 0.16f),
        primaryLight = BloomLightSource(
            extraDegrees = 0f,
            z = -0.05f,
            color = Color.White,
            intensity = 1.0f,
            followGravity = true
        ),
        secondaryLight = BloomLightSource(
            extraDegrees = 180f,
            z = -0.50f,
            color = Color.White,
            intensity = 0.45f,
            followGravity = true
        ),
        dualPeak = true,
        blendMode = BlendMode.Plus,
        lightSurfaceSubtleOutlineAlpha = 0.04f
    )

    /** 浅色浮层面板预设（倒角深度 2.5.dp，主光强度 0.95） */
    val PanelLight: BloomStrokeStyle = BloomStrokeStyle(
        strokeWidth = 1.dp,
        innerBlurRadius = 2.5.dp,
        strokeColor = Color.White.copy(alpha = 0.16f),
        primaryLight = BloomLightSource(
            extraDegrees = 0f,
            z = -0.05f,
            color = Color.White,
            intensity = 0.95f,
            followGravity = true
        ),
        secondaryLight = BloomLightSource(
            extraDegrees = 180f,
            z = -0.35f,
            color = Color.White,
            intensity = 0.52f,
            followGravity = true
        ),
        dualPeak = true,
        blendMode = BlendMode.Plus,
        lightSurfaceSubtleOutlineAlpha = 0.06f
    )

    /** 深色浮层面板预设 */
    val PanelDark: BloomStrokeStyle = BloomStrokeStyle(
        strokeWidth = 1.dp,
        innerBlurRadius = 2.5.dp,
        strokeColor = Color.White.copy(alpha = 0.16f),
        primaryLight = BloomLightSource(
            extraDegrees = 0f,
            z = -0.05f,
            color = Color.White,
            intensity = 0.95f,
            followGravity = true
        ),
        secondaryLight = BloomLightSource(
            extraDegrees = 180f,
            z = -0.35f,
            color = Color.White,
            intensity = 0.52f,
            followGravity = true
        ),
        dualPeak = true,
        blendMode = BlendMode.Plus,
        lightSurfaceSubtleOutlineAlpha = 0f
    )

    /** 浅色/深色弹窗面板预设 */
    val DialogLight: BloomStrokeStyle = PanelLight.copy(
        strokeWidth = 1.dp,
        innerBlurRadius = 2.5.dp,
        lightSurfaceSubtleOutlineAlpha = 0.06f
    )

    val DialogDark: BloomStrokeStyle = PanelDark.copy(
        strokeWidth = 1.dp,
        innerBlurRadius = 2.5.dp,
        lightSurfaceSubtleOutlineAlpha = 0f
    )

    /** 紧凑小尺寸按钮预设（描边 0.85.dp、倒角深度 1.75.dp） */
    val CompactControl: BloomStrokeStyle = BloomStrokeStyle(
        strokeWidth = 0.85.dp,
        innerBlurRadius = 1.75.dp,
        strokeColor = Color.White.copy(alpha = 0.16f),
        primaryLight = BloomLightSource(
            extraDegrees = 0f,
            z = -0.05f,
            color = Color.White,
            intensity = 1.0f,
            followGravity = true
        ),
        secondaryLight = BloomLightSource(
            extraDegrees = 180f,
            z = -0.35f,
            color = Color.White,
            intensity = 0.52f,
            followGravity = true
        ),
        dualPeak = true,
        blendMode = BlendMode.Plus,
        lightSurfaceSubtleOutlineAlpha = 0.06f
    )
}

@Language("AGSL")
private const val BLOOM_STROKE_HEADER = """
uniform float2 halfView;
uniform float4 cornerRadii;
uniform float strokeWidth;
uniform float innerBlurRadius;
uniform float innerBlurRadiusSq;
uniform float highlightAlpha;

layout(color) uniform half4 strokeColor;
uniform float strokeAlphaMul;

uniform float3 lightDir1;
layout(color) uniform half4 lightColor1;
uniform float lightIntensity1;

uniform float3 lightDir2;
layout(color) uniform half4 lightColor2;
uniform float lightIntensity2;
"""

@Language("AGSL")
private const val BLOOM_STROKE_AXIS_UNIFORMS = """
uniform float2 axis1;
uniform float2 axis2;
"""

@Language("AGSL")
private const val BLOOM_STROKE_BODY = """
float pickRadius(float2 fragCoord, float4 radii) {
    float2 up = fragCoord.y < halfView.y ? radii.xy : radii.zw;
    return fragCoord.x < halfView.x ? up.x : up.y;
}

float roundedBoxSDF(float2 pos, float2 halfSize, float radius) {
    radius = min(radius, min(halfSize.x, halfSize.y));
    float2 d = pos - halfSize + radius;
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - radius;
}

float3 getNormal(float2 fragCoord, float sdf, float R) {
    if (sdf <= -innerBlurRadius) {
        return float3(0.0, 0.0, -1.0);
    }

    float2 xy = fragCoord - halfView;
    float2 xy_a = abs(xy);
    float t = smoothstep(-innerBlurRadius, 0.0, sdf) * innerBlurRadius;
    float z = sqrt(max(innerBlurRadiusSq - t * t, 0.0));
    float3 coord = float3(xy_a, -z);

    float2 corner = halfView - R;
    corner.x = min(corner.x, xy_a.x);
    corner.y = min(corner.y, xy_a.y);

    float2 diff = coord.xy - corner.xy;
    float diffLen = length(diff);
    float2 dir = diffLen > 1e-4 ? diff / diffLen : float2(0.0);
    corner += dir * (R - innerBlurRadius);

    if (any(lessThan(xy_a, corner))) {
        return float3(0.0, 0.0, -1.0);
    }

    float2 signal = sign(xy);
    float3 delta = coord - float3(corner, 0.0);
    float deltaLen = length(delta);
    float3 n = deltaLen > 1e-4 ? delta / deltaLen : float3(0.0, 0.0, -1.0);
    n.xy *= signal;
    return n;
}

half4 main(float2 fragCoord) {
    float2 xy = abs(fragCoord - halfView);

    float originRadius = pickRadius(fragCoord, cornerRadii);
    float R = max(originRadius, innerBlurRadius);

    if (all(lessThan(xy, halfView - R))) {
        return half4(0.0);
    }

    float sdf = roundedBoxSDF(xy, halfView, originRadius);
    float maxDepth = max(innerBlurRadius, strokeWidth) + 1.0;
    if (sdf < -maxDepth || sdf > 1.0) {
        return half4(0.0);
    }

    half outMask = half(smoothstep(0.0, -1.0, sdf));
    float strokeAlpha = smoothstep(-strokeWidth, -strokeWidth + 1.0, sdf);

    half3 rgb = strokeColor.rgb * half(strokeAlphaMul * strokeAlpha * strokeAlpha);

    float3 n = getNormal(fragCoord, sdf, R);
"""

@Language("AGSL")
private const val BLOOM_STROKE_DUAL_LIGHT = """
    float falloff1 = max(dot(float3(axis1, 0.0), n), 0.0);
    float light1 = clamp(dot(n, lightDir1) * falloff1, 0.0, 1.0);
    rgb += half(light1 * light1 * lightIntensity1) * lightColor1.rgb;

    float falloff2 = max(dot(float3(axis2, 0.0), n), 0.0);
    float light2 = clamp(dot(n, lightDir2) * falloff2, 0.0, 1.0);
    rgb += half(light2 * light2 * lightIntensity2) * lightColor2.rgb;
"""

@Language("AGSL")
private const val BLOOM_STROKE_SINGLE_LIGHT = """
    float l1 = dot(n.xy, lightDir1.xy);
    rgb += half(l1 * l1 * lightIntensity1) * lightColor1.rgb;
    float l2 = dot(n.xy, lightDir2.xy);
    rgb += half(l2 * l2 * lightIntensity2) * lightColor2.rgb;
"""

@Language("AGSL")
private const val BLOOM_STROKE_FOOTER = """
    half3 finalRgb = clamp(rgb * half(highlightAlpha) * outMask, 0.0, 1.0);
    half finalAlpha = max(finalRgb.r, max(finalRgb.g, finalRgb.b));
    return half4(finalRgb, finalAlpha);
}
"""

private val BLOOM_SHADER_DUAL = buildString {
    append(BLOOM_STROKE_HEADER)
    append(BLOOM_STROKE_AXIS_UNIFORMS)
    append(BLOOM_STROKE_BODY)
    append(BLOOM_STROKE_DUAL_LIGHT)
    append(BLOOM_STROKE_FOOTER)
}

private val BLOOM_SHADER_SINGLE = buildString {
    append(BLOOM_STROKE_HEADER)
    append(BLOOM_STROKE_BODY)
    append(BLOOM_STROKE_SINGLE_LIGHT)
    append(BLOOM_STROKE_FOOTER)
}

/**
 * 为圆角矩形叠加 3D SDF 棱边高光与重力扫光（API < 33 降级为线性渐变描边）。
 *
 * @param extraDegrees 初始光源相对重力方向的偏转角（默认 -45° 斜向高光）
 */
fun Modifier.bloomStroke(
    cornerRadius: Dp,
    style: BloomStrokeStyle = BloomStrokeDefaults.OuterLight,
    extraDegrees: Float = -45f,
    alphaProvider: (() -> Float)? = null
): Modifier = this then BloomStrokeElement(
    topStart = cornerRadius,
    topEnd = cornerRadius,
    bottomStart = cornerRadius,
    bottomEnd = cornerRadius,
    style = style,
    extraDegrees = extraDegrees,
    alphaProvider = alphaProvider
)

/**
 * 支持四角独立圆角半径的 [bloomStroke] 重载。
 */
fun Modifier.bloomStroke(
    topStart: Dp,
    topEnd: Dp,
    bottomStart: Dp,
    bottomEnd: Dp,
    style: BloomStrokeStyle = BloomStrokeDefaults.OuterLight,
    extraDegrees: Float = -45f,
    alphaProvider: (() -> Float)? = null
): Modifier = this then BloomStrokeElement(
    topStart = topStart,
    topEnd = topEnd,
    bottomStart = bottomStart,
    bottomEnd = bottomEnd,
    style = style,
    extraDegrees = extraDegrees,
    alphaProvider = alphaProvider
)

private class BloomStrokeElement(
    val topStart: Dp,
    val topEnd: Dp,
    val bottomStart: Dp,
    val bottomEnd: Dp,
    val style: BloomStrokeStyle,
    val extraDegrees: Float,
    val alphaProvider: (() -> Float)?
) : ModifierNodeElement<BloomStrokeNode>() {

    override fun create(): BloomStrokeNode = BloomStrokeNode(
        topStart = topStart,
        topEnd = topEnd,
        bottomStart = bottomStart,
        bottomEnd = bottomEnd,
        style = style,
        extraDegrees = extraDegrees,
        alphaProvider = alphaProvider
    )

    override fun update(node: BloomStrokeNode) {
        val shaderModeChanged = node.style.dualPeak != style.dualPeak
        node.topStart = topStart
        node.topEnd = topEnd
        node.bottomStart = bottomStart
        node.bottomEnd = bottomEnd
        node.style = style
        node.extraDegrees = extraDegrees
        node.alphaProvider = alphaProvider
        if (shaderModeChanged) {
            node.resetShaderCache()
        }
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "bloomStroke"
        properties["style"] = style
        properties["extraDegrees"] = extraDegrees
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BloomStrokeElement) return false
        return topStart == other.topStart &&
            topEnd == other.topEnd &&
            bottomStart == other.bottomStart &&
            bottomEnd == other.bottomEnd &&
            style == other.style &&
            extraDegrees == other.extraDegrees &&
            alphaProvider == other.alphaProvider
    }

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + extraDegrees.hashCode()
        result = 31 * result + (alphaProvider?.hashCode() ?: 0)
        return result
    }
}

private class BloomStrokeNode(
    var topStart: Dp,
    var topEnd: Dp,
    var bottomStart: Dp,
    var bottomEnd: Dp,
    var style: BloomStrokeStyle,
    var extraDegrees: Float,
    var alphaProvider: (() -> Float)?
) : DrawModifierNode, CompositionLocalConsumerModifierNode, Modifier.Node() {

    private var cachedShader: RuntimeShader? = null
    private var cachedDualPeak: Boolean = true
    private val frameworkPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.FILL
    }

    fun resetShaderCache() {
        cachedShader = null
    }

    override fun ContentDrawScope.draw() {
        drawContent()

        if (size.width < 1f || size.height < 1f) return
        val effectiveAlpha = (alphaProvider?.invoke() ?: style.alpha).coerceIn(0f, 1f)
        if (effectiveAlpha <= 0.001f) return

        val gravityAngle = currentValueOf(LocalDeviceTiltState).quantizedAngleRad

        // 浅色底背景物理轮廓补偿（极淡暗描边，避免纯白底上边缘融化）
        if (style.lightSurfaceSubtleOutlineAlpha > 0.001f) {
            val outlineR = min(topStart.toPx(), min(size.width, size.height) * 0.5f)
            drawRoundRect(
                color = Color.Black.copy(alpha = style.lightSurfaceSubtleOutlineAlpha * effectiveAlpha),
                cornerRadius = CornerRadius(outlineR, outlineR),
                style = Stroke(width = 0.5.dp.toPx())
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            drawRuntimeBloom(gravityAngle, effectiveAlpha)
        } else {
            drawFallbackBloom(gravityAngle, effectiveAlpha)
        }
    }

    private fun ContentDrawScope.drawRuntimeBloom(gravityAngle: Float, effectiveAlpha: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val shader = cachedShader?.takeIf { cachedDualPeak == style.dualPeak }
            ?: RuntimeShader(if (style.dualPeak) BLOOM_SHADER_DUAL else BLOOM_SHADER_SINGLE).also {
                cachedShader = it
                cachedDualPeak = style.dualPeak
            }

        val halfW = size.width * 0.5f
        val halfH = size.height * 0.5f
        val maxRadius = min(halfW, halfH)
        val rTopLeft = topStart.toPx().coerceIn(0f, maxRadius)
        val rTopRight = topEnd.toPx().coerceIn(0f, maxRadius)
        val rBottomLeft = bottomStart.toPx().coerceIn(0f, maxRadius)
        val rBottomRight = bottomEnd.toPx().coerceIn(0f, maxRadius)

        val innerBlurPx = style.innerBlurRadius.toPx().coerceAtLeast(0.5f)
        val strokeWidthPx = style.strokeWidth.toPx().coerceAtLeast(0.5f)

        shader.setFloatUniform("halfView", halfW, halfH)
        shader.setFloatUniform("cornerRadii", rTopLeft, rTopRight, rBottomLeft, rBottomRight)
        shader.setFloatUniform("strokeWidth", strokeWidthPx)
        shader.setFloatUniform("innerBlurRadius", innerBlurPx)
        shader.setFloatUniform("innerBlurRadiusSq", innerBlurPx * innerBlurPx)
        shader.setFloatUniform("highlightAlpha", effectiveAlpha)

        shader.setColorUniform("strokeColor", style.strokeColor.copy(alpha = 1f).toArgb())
        shader.setFloatUniform("strokeAlphaMul", style.strokeColor.alpha)

        applyLightUniform(shader, "1", style.primaryLight, gravityAngle, extraDegrees, style.dualPeak)
        applyLightUniform(shader, "2", style.secondaryLight, gravityAngle, extraDegrees, style.dualPeak)

        frameworkPaint.shader = shader
        frameworkPaint.blendMode = style.blendMode.toAndroidBlendMode()

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, frameworkPaint)
        }
    }

    private fun ContentDrawScope.drawFallbackBloom(gravityAngle: Float, effectiveAlpha: Float) {
        val rad = gravityAngle + Math.toRadians(extraDegrees.toDouble()).toFloat()
        val halfW = size.width * 0.5f
        val halfH = size.height * 0.5f
        val dirX = cos(rad)
        val dirY = sin(rad)
        val span = min(halfW, halfH)
        val start = Offset(halfW - dirX * span, halfH - dirY * span)
        val end = Offset(halfW + dirX * span, halfH + dirY * span)

        val peakAlpha = (0.48f * effectiveAlpha).coerceIn(0f, 1f)
        val dimAlpha = (0.10f * effectiveAlpha).coerceIn(0f, 1f)
        val midAlpha = (0.28f * effectiveAlpha).coerceIn(0f, 1f)
        val r = min(topStart.toPx(), span)

        drawRoundRect(
            brush = Brush.linearGradient(
                0f to style.primaryLight.color.copy(alpha = peakAlpha),
                0.5f to style.strokeColor.copy(alpha = dimAlpha),
                1f to style.secondaryLight.color.copy(alpha = midAlpha),
                start = start,
                end = end
            ),
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(width = style.strokeWidth.toPx()),
            blendMode = style.blendMode
        )
    }

    override fun onDetach() {
        cachedShader = null
        frameworkPaint.shader = null
    }
}

private fun applyLightUniform(
    shader: RuntimeShader,
    suffix: String,
    light: BloomLightSource,
    gravityAngle: Float,
    containerExtraDegrees: Float,
    dualPeak: Boolean
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val (nx, ny, nz) = if (light.followGravity) {
        rotateLightDirection(gravityAngle, containerExtraDegrees + light.extraDegrees, light.z)
    } else {
        val len = sqrt(light.staticDirX * light.staticDirX + light.staticDirY * light.staticDirY + light.z * light.z)
        if (len > 1e-6f) {
            Triple(light.staticDirX / len, light.staticDirY / len, light.z / len)
        } else {
            Triple(0f, 0f, -1f)
        }
    }

    shader.setFloatUniform("lightDir$suffix", nx, ny, nz)
    shader.setColorUniform("lightColor$suffix", light.color.copy(alpha = 1f).toArgb())
    shader.setFloatUniform("lightIntensity$suffix", light.color.alpha * light.intensity)

    if (dualPeak) {
        val xyLen = sqrt(nx * nx + ny * ny)
        if (xyLen > 0.001f) {
            shader.setFloatUniform("axis$suffix", nx / xyLen, ny / xyLen)
        } else {
            shader.setFloatUniform("axis$suffix", 0f, if (suffix == "1") -1f else 1f)
        }
    }
}

private fun BlendMode.toAndroidBlendMode(): android.graphics.BlendMode = when (this) {
    BlendMode.Plus -> android.graphics.BlendMode.PLUS
    BlendMode.Screen -> android.graphics.BlendMode.SCREEN
    BlendMode.Overlay -> android.graphics.BlendMode.OVERLAY
    BlendMode.Modulate -> android.graphics.BlendMode.MODULATE
    else -> android.graphics.BlendMode.SRC_OVER
}
