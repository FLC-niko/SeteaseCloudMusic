package com.example.seteasecloudmusic.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.effects.runtimeShaderEffect
import kotlinx.coroutines.CoroutineScope
import org.intellij.lang.annotations.Language

@Language("AGSL")
private const val NOISE_DITHER_SHADER = """
uniform shader child;
uniform float noise_coeff;

half4 main(float2 xy) {
    float3 d = float3(
        dot(xy, float2(6.9898, 78.233)),
        dot(xy, float2(7.9898, 78.233)),
        dot(xy, float2(8.9898, 78.233))
    );
    float3 n = (fract(sin(d) * float3(43734.5453, 43745.5453, 43767.5453)) - 0.5) * noise_coeff;
    half4 color = child.eval(xy);
    color.rg += half2(n.x);
    color.rb += half2(n.y);
    color.gb += half2(n.z);
    return color;
}
"""

/**
 * 像素级高频抖动噪点效果，用于消除高斯模糊渐变产生的 8-bit 色带条纹。
 *
 * @param coeff 噪点强度系数（默认 0.0045f，越大颗粒感越明显；<= 0 时跳过着色器）
 */
fun BackdropEffectScope.noiseDither(coeff: Float = 0.0045f) {
    if (coeff <= 0f) return
    runtimeShaderEffect(
        key = "NoiseDither",
        shaderString = NOISE_DITHER_SHADER,
        uniformShaderName = "child"
    ) {
        setFloatUniform("noise_coeff", coeff)
    }
}

/**
 * 交互高光状态占位类（当前为空操作实现）。
 */
@Suppress("UNUSED_PARAMETER")
@Stable
class InteractiveHighlightState(
    scope: CoroutineScope
) {
    fun onPress(position: Offset) = Unit
    fun onMove(position: Offset) = Unit
    fun onRelease() = Unit
}

@Composable
fun rememberInteractiveHighlightState(): InteractiveHighlightState {
    val scope = rememberCoroutineScope()
    return remember(scope) { InteractiveHighlightState(scope) }
}

/**
 * 按压交互高光修饰符（当前为空操作，直接返回自身）。
 */
@Suppress("UNUSED_PARAMETER")
fun Modifier.interactiveGlassHighlight(
    shape: Shape,
    enabled: Boolean = true,
    isDarkSurface: Boolean = false,
    state: InteractiveHighlightState? = null
): Modifier = this
