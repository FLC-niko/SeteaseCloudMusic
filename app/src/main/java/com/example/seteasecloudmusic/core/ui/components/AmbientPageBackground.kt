package com.example.seteasecloudmusic.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.example.seteasecloudmusic.core.settings.PlayerSettingsManager
import com.example.seteasecloudmusic.core.ui.theme.AppColorPalette
import com.example.seteasecloudmusic.core.ui.theme.AppTheme

@Immutable
data class TopAmbientPreset(
    val name: String,
    val colorArgb: Int
)

val TopAmbientPresets: List<TopAmbientPreset> = listOf(
    TopAmbientPreset("樱花粉", PlayerSettingsManager.DEFAULT_TOP_AMBIENT_COLOR_ARGB),
    TopAmbientPreset("薰衣紫", 0xFFECE8FF.toInt()),
    TopAmbientPreset("晴空蓝", 0xFFE2F1FF.toInt()),
    TopAmbientPreset("薄荷绿", 0xFFDFF8EE.toInt()),
    TopAmbientPreset("暖杏橘", 0xFFFFEDE0.toInt()),
    TopAmbientPreset("奶油黄", 0xFFFFF6D6.toInt()),
    TopAmbientPreset("玫瑰红", 0xFFFFD6E0.toInt())
)

/**
 * 将调色盘选中的基础颜色解析为适用于当前浅/深色模式的顶部渐变双色 `(warmTopStart, warmTopMid)`。
 */
fun resolveAmbientTopColors(colorArgb: Int, isDark: Boolean): Pair<Color, Color> {
    if (colorArgb == PlayerSettingsManager.DEFAULT_TOP_AMBIENT_COLOR_ARGB) {
        return if (isDark) {
            Color(0xFF5A1828).copy(alpha = 0.42f) to Color(0xFF241B4B).copy(alpha = 0.28f)
        } else {
            Color(0xFFFFE8ED).copy(alpha = 0.82f) to Color(0xFFF2EEFF).copy(alpha = 0.32f)
        }
    }

    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(colorArgb, hsl)
    val isNeutral = hsl[1] < 0.05f

    return if (isDark) {
        val startHsl = floatArrayOf(
            hsl[0],
            if (isNeutral) 0f else (hsl[1] * 0.65f + 0.20f).coerceIn(0.35f, 0.72f),
            (hsl[2] * 0.12f + 0.15f).coerceIn(0.15f, 0.27f)
        )
        val midHsl = floatArrayOf(
            (hsl[0] + 28f) % 360f,
            if (isNeutral) 0f else (startHsl[1] * 0.85f).coerceIn(0f, 0.65f),
            (startHsl[2] - 0.02f).coerceIn(0.13f, 0.24f)
        )
        Color(ColorUtils.HSLToColor(startHsl)).copy(alpha = 0.45f) to
            Color(ColorUtils.HSLToColor(midHsl)).copy(alpha = 0.28f)
    } else {
        val startHsl = floatArrayOf(
            hsl[0],
            if (isNeutral) 0f else hsl[1].coerceIn(0.28f, 1.0f),
            (hsl[2] * 0.18f + 0.80f).coerceIn(0.80f, 0.955f)
        )
        val midHsl = floatArrayOf(
            (hsl[0] + 22f) % 360f,
            if (isNeutral) 0f else (startHsl[1] * 0.75f).coerceIn(0f, 0.85f),
            (startHsl[2] + 0.03f).coerceIn(0.88f, 0.96f)
        )
        Color(ColorUtils.HSLToColor(startHsl)).copy(alpha = 0.84f) to
            Color(ColorUtils.HSLToColor(midHsl)).copy(alpha = 0.34f)
    }
}

fun formatTopAmbientColorLabel(colorArgb: Int): String {
    TopAmbientPresets.firstOrNull { it.colorArgb == colorArgb }?.let { return it.name }
    return "#${(colorArgb and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')}"
}

/**
 * 顶部氛围渐变背景（根据设置中的开关与调色盘实时变化，深色模式自动适配）。
 *
 * @param baseColor 底层页面底色（默认使用 [AppColorPalette.background]）
 * @param includeSideGlows 是否叠加铺满整屏的左右冷/暖对冲微光（“我的”页用于衬托液态玻璃卡片）
 */
@Composable
fun AmbientTopPinkBackground(
    modifier: Modifier = Modifier,
    baseColor: Color = AppTheme.colors.background,
    includeSideGlows: Boolean = false
) {
    val isDark = AppTheme.isDark
    val ambient = AppTheme.topAmbient
    val (warmTopStart, warmTopMid) = resolveAmbientTopColors(ambient.colorArgb, isDark)
    val leftGlow = if (isDark) Color(0xFF10243C).copy(alpha = 0.26f) else Color(0xFFE9F5FF).copy(alpha = 0.30f)
    val rightGlow = if (isDark) Color(0xFF3B1E18).copy(alpha = 0.20f) else Color(0xFFFFF1E8).copy(alpha = 0.24f)

    // 多层氛围背景合并到同一个节点的 drawBehind 里，减少额外布局节点与绘制指令。
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(baseColor)
                if (!ambient.enabled) return@drawBehind
                // 顶部暖色渐晕：显式限定在 0..420dp，避免按整个画布高度拉伸
                val warmBandHeight = 420.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            warmTopStart,
                            warmTopMid,
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = warmBandHeight
                    ),
                    size = Size(size.width, warmBandHeight)
                )
                if (includeSideGlows) {
                    // 左右冷/暖对冲微光，铺满整屏
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                leftGlow,
                                Color.Transparent,
                                rightGlow
                            )
                        )
                    )
                }
            }
    )
}
