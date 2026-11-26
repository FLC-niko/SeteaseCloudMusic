package com.example.seteasecloudmusic.feature.auth.presentation

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seteasecloudmusic.core.settings.PlayerSettingsManager
import com.example.seteasecloudmusic.core.ui.adaptive.AdaptiveAlertDialog
import com.example.seteasecloudmusic.core.ui.components.TopAmbientPresets
import com.example.seteasecloudmusic.core.ui.components.formatTopAmbientColorLabel
import com.example.seteasecloudmusic.core.ui.components.resolveAmbientTopColors
import com.example.seteasecloudmusic.core.ui.theme.AppTheme

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

            // 2D 饱和度 / 明度调色盘
            SaturationValueColorPad(
                hue = hue,
                saturation = saturation,
                brightness = brightness,
                selectedColor = Color(selectedColorArgb),
                onChanged = { newSat, newVal ->
                    saturation = newSat
                    brightness = newVal
                    exactOverrideArgb = null
                }
            )

            // 色相调节滑条
            HueSpectrumSlider(
                hue = hue,
                onHueChanged = { newHue ->
                    hue = newHue
                    exactOverrideArgb = null
                }
            )
        }
    }
}

@Composable
private fun SaturationValueColorPad(
    hue: Float,
    saturation: Float,
    brightness: Float,
    selectedColor: Color,
    onChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnChanged by rememberUpdatedState(onChanged)
    val pureHueColor = remember(hue) { Color.hsv(hue.coerceIn(0f, 360f), 1f, 1f) }
    val borderColor = AppTheme.colors.coverBorder

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    updateSvFromPosition(down.position, size, currentOnChanged)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        change.consume()
                        updateSvFromPosition(change.position, size, currentOnChanged)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, pureHueColor)
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black)
                )
            )

            val cx = (saturation.coerceIn(0f, 1f)) * size.width
            val cy = (1f - brightness.coerceIn(0f, 1f)) * size.height
            val center = Offset(cx, cy)
            val radius = 10.dp.toPx()

            drawCircle(
                color = selectedColor,
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = radius + 1.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = radius,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
    }
}

@Composable
private fun HueSpectrumSlider(
    hue: Float,
    onHueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnHueChanged by rememberUpdatedState(onHueChanged)
    val rainbowColors = remember {
        listOf(
            Color(0xFFFF0000),
            Color(0xFFFFFF00),
            Color(0xFF00FF00),
            Color(0xFF00FFFF),
            Color(0xFF0000FF),
            Color(0xFFFF00FF),
            Color(0xFFFF0000)
        )
    }
    val borderColor = AppTheme.colors.coverBorder

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(CircleShape)
            .border(1.dp, borderColor, CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    updateHueFromPosition(down.position, size, currentOnHueChanged)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        change.consume()
                        updateHueFromPosition(change.position, size, currentOnHueChanged)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(brush = Brush.horizontalGradient(colors = rainbowColors))
            val radius = 11.dp.toPx()
            val usableWidth = (size.width - radius * 2f).coerceAtLeast(1f)
            val cx = radius + (hue.coerceIn(0f, 360f) / 360f) * usableWidth
            val center = Offset(cx, size.height / 2f)
            val thumbColor = Color.hsv(hue.coerceIn(0f, 360f), 1f, 1f)

            drawCircle(
                color = thumbColor,
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.30f),
                radius = radius + 0.5.dp.toPx(),
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = radius - 1.dp.toPx(),
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
    }
}

private fun updateSvFromPosition(
    position: Offset,
    size: IntSize,
    onChanged: (Float, Float) -> Unit
) {
    val w = size.width.toFloat().coerceAtLeast(1f)
    val h = size.height.toFloat().coerceAtLeast(1f)
    val sat = (position.x / w).coerceIn(0f, 1f)
    val valBright = (1f - position.y / h).coerceIn(0f, 1f)
    onChanged(sat, valBright)
}

private fun updateHueFromPosition(
    position: Offset,
    size: IntSize,
    onHueChanged: (Float) -> Unit
) {
    val w = size.width.toFloat().coerceAtLeast(1f)
    val newHue = ((position.x / w) * 360f).coerceIn(0f, 360f)
    onHueChanged(newHue)
}
