package com.example.seteasecloudmusic.feature.auth.presentation

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seteasecloudmusic.core.settings.PlayerSettingsManager
import com.example.seteasecloudmusic.core.ui.adaptive.AdaptiveAlertDialog
import com.example.seteasecloudmusic.core.ui.components.TopAmbientPresets
import com.example.seteasecloudmusic.core.ui.components.formatTopAmbientColorLabel
import com.example.seteasecloudmusic.core.ui.components.resolveAmbientTopColors
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import top.yukonga.miuix.kmp.basic.HsvHueSlider
import top.yukonga.miuix.kmp.basic.HsvSaturationSlider
import top.yukonga.miuix.kmp.basic.HsvValueSlider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check

@Composable
internal fun TopAmbientColorPickerDialog(
    currentColorArgb: Int,
    isAmbientEnabled: Boolean,
    onApply: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val initialHsv = remember(currentColorArgb) {
        FloatArray(3).also { AndroidColor.colorToHSV(currentColorArgb, it) }
    }
    var hue by remember(currentColorArgb) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(currentColorArgb) { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember(currentColorArgb) { mutableFloatStateOf(initialHsv[2]) }
    var exactOverrideArgb by remember(currentColorArgb) { mutableStateOf<Int?>(currentColorArgb) }

    val hsvColorArgb = remember(hue, saturation, brightness) {
        AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness))
    }
    val selectedColorArgb = exactOverrideArgb ?: hsvColorArgb

    val selectExactColor: (Int) -> Unit = { argb ->
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(argb, hsv)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        exactOverrideArgb = argb
    }

    val colors = AppTheme.colors
    val isDark = AppTheme.isDark
    val (previewStart, previewMid) = resolveAmbientTopColors(selectedColorArgb, isDark)
    val selectedLabel = formatTopAmbientColorLabel(selectedColorArgb)
    val isDefault = selectedColorArgb == PlayerSettingsManager.DEFAULT_TOP_AMBIENT_COLOR_ARGB

    AdaptiveAlertDialog(
        onDismissRequest = onDismiss,
        title = "顶部氛围调色盘",
        confirmText = if (isAmbientEnabled) "应用" else "开启并应用",
        confirmColor = colors.accent,
        dismissText = "取消",
        onConfirm = { onApply(selectedColorArgb) },
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 实时效果预览卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, colors.coverBorder, RoundedCornerShape(18.dp))
                    .drawBehind {
                        drawRect(colors.background)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    previewStart,
                                    previewMid,
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height
                            ),
                            size = Size(size.width, size.height)
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                    Text(
                        text = "首页 · 效果预览",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "应用于首页、电台、我的与搜索页顶部",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.surface.copy(alpha = 0.78f))
                        .border(1.dp, colors.coverBorder, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = selectedLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }
            }

            // 推荐预设色板
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "精选色板",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
                if (!isDefault) {
                    Text(
                        text = "恢复默认",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectExactColor(PlayerSettingsManager.DEFAULT_TOP_AMBIENT_COLOR_ARGB) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopAmbientPresets.forEach { preset ->
                    val isSelected = selectedColorArgb == preset.colorArgb
                    val swatchColor = Color(preset.colorArgb)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) colors.accent else colors.coverBorder,
                                shape = CircleShape
                            )
                            .clickable { selectExactColor(preset.colorArgb) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = MiuixIcons.Basic.Check,
                                contentDescription = preset.name,
                                tint = if (swatchColor.luminance() > 0.55f) Color(0xFF1C1C1E) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Miuix 原生 HSV 滑块调节（色相 / 饱和度 / 明度）
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "色相",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
                HsvHueSlider(
                    currentHue = hue.coerceIn(0f, 360f),
                    onHueChanged = { normalizedHue ->
                        hue = (normalizedHue * 360f).coerceIn(0f, 360f)
                        exactOverrideArgb = null
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "饱和度",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
                HsvSaturationSlider(
                    currentHue = hue.coerceIn(0f, 360f),
                    currentSaturation = saturation.coerceIn(0f, 1f),
                    onSaturationChanged = { newSaturation ->
                        saturation = newSaturation.coerceIn(0f, 1f)
                        exactOverrideArgb = null
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "明度",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
                HsvValueSlider(
                    currentHue = hue.coerceIn(0f, 360f),
                    currentSaturation = saturation.coerceIn(0f, 1f),
                    currentValue = brightness.coerceIn(0f, 1f),
                    onValueChanged = { newBrightness ->
                        brightness = newBrightness.coerceIn(0f, 1f)
                        exactOverrideArgb = null
                    }
                )
            }
        }
    }
}
