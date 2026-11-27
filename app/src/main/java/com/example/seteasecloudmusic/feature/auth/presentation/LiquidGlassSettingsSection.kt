package com.example.seteasecloudmusic.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seteasecloudmusic.R
import com.example.seteasecloudmusic.core.settings.PlayerSettingsManager
import com.example.seteasecloudmusic.core.ui.components.GlassDefaults
import com.example.seteasecloudmusic.core.ui.components.liquidGlass
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import com.example.seteasecloudmusic.core.ui.theme.LiquidGlassAppearance
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 滑块上「默认（0f）」关键点所在的归一化位置（25%）：
 * 将左侧 [0f, 0.25f] 分配给通透区间（-1f..0f），右侧 [0.25f, 1f] 分配给磨砂区间（0f..1f），
 * 让两侧光学变化幅度在滑块物理行程上更加均衡。
 */
internal const val DEFAULT_LIQUID_GLASS_SLIDER_POSITION: Float = 0.25f

/**
 * 将光学调节值（`[-1f, 1f]`）映射为滑块位置（`[0f, 1f]`），其中 `0f -> 0.25f`。
 */
internal fun opacityLevelToSliderPosition(opacityLevel: Float): Float {
    if (opacityLevel.isNaN()) return DEFAULT_LIQUID_GLASS_SLIDER_POSITION
    val clamped = opacityLevel.coerceIn(
        PlayerSettingsManager.MIN_LIQUID_GLASS_OPACITY,
        PlayerSettingsManager.MAX_LIQUID_GLASS_OPACITY
    )
    return if (clamped <= 0f) {
        DEFAULT_LIQUID_GLASS_SLIDER_POSITION * (1f + clamped)
    } else {
        DEFAULT_LIQUID_GLASS_SLIDER_POSITION + (1f - DEFAULT_LIQUID_GLASS_SLIDER_POSITION) * clamped
    }
}

/**
 * 将滑块位置（`[0f, 1f]`）反向映射为光学调节值（`[-1f, 1f]`），并在 `0.25f` 默认档位附近提供磁吸死区。
 */
internal fun sliderPositionToOpacityLevel(
    sliderPosition: Float,
    centerDeadzone: Float = 0.035f
): Float {
    if (sliderPosition.isNaN()) return PlayerSettingsManager.DEFAULT_LIQUID_GLASS_OPACITY
    val clamped = sliderPosition.coerceIn(0f, 1f)
    if (abs(clamped - DEFAULT_LIQUID_GLASS_SLIDER_POSITION) <= centerDeadzone) {
        return PlayerSettingsManager.DEFAULT_LIQUID_GLASS_OPACITY
    }
    return if (clamped < DEFAULT_LIQUID_GLASS_SLIDER_POSITION) {
        ((clamped - DEFAULT_LIQUID_GLASS_SLIDER_POSITION) / DEFAULT_LIQUID_GLASS_SLIDER_POSITION)
            .coerceIn(PlayerSettingsManager.MIN_LIQUID_GLASS_OPACITY, 0f)
    } else {
        ((clamped - DEFAULT_LIQUID_GLASS_SLIDER_POSITION) / (1f - DEFAULT_LIQUID_GLASS_SLIDER_POSITION))
            .coerceIn(0f, PlayerSettingsManager.MAX_LIQUID_GLASS_OPACITY)
    }
}

/**
 * 将原始滑块值钳制到 `[-1f, 1f]` 并对中心默认档位（`0f`）应用吸附死区。
 */
internal fun snapLiquidGlassLevel(
    rawLevel: Float,
    centerDeadzone: Float = 0.06f
): Float {
    if (rawLevel.isNaN()) return PlayerSettingsManager.DEFAULT_LIQUID_GLASS_OPACITY
    val clamped = rawLevel.coerceIn(
        PlayerSettingsManager.MIN_LIQUID_GLASS_OPACITY,
        PlayerSettingsManager.MAX_LIQUID_GLASS_OPACITY
    )
    return if (abs(clamped) <= centerDeadzone) {
        PlayerSettingsManager.DEFAULT_LIQUID_GLASS_OPACITY
    } else {
        clamped
    }
}

/**
 * 将液态玻璃调节值格式化为简洁状态文案（通透 XX% / 默认 / 磨砂 XX%）。
 */
internal fun formatLiquidGlassLevelLabel(opacityLevel: Float): String {
    val clamped = if (opacityLevel.isNaN()) {
        0f
    } else {
        opacityLevel.coerceIn(
            PlayerSettingsManager.MIN_LIQUID_GLASS_OPACITY,
            PlayerSettingsManager.MAX_LIQUID_GLASS_OPACITY
        )
    }
    return when {
        abs(clamped) <= 0.04f -> "默认"
        clamped < 0f -> "通透 ${((-clamped) * 100f).roundToInt().coerceIn(1, 100)}%"
        else -> "磨砂 ${(clamped * 100f).roundToInt().coerceIn(1, 100)}%"
    }
}

/**
 * 设置页液态玻璃配置卡片：
 * 遵循 Miuix [SliderPreference] 统一风格，固定右侧状态栏宽度以避免拖动时左侧文字换行抖动，
 * 并把默认关键点设于 25% 位置以均衡通透与磨砂两端的视觉变化幅度。
 */
@Composable
internal fun LiquidGlassSettingsCard(
    opacityLevel: Float,
    onOpacityChange: (Float) -> Unit,
    onOpacityChangeFinished: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val clampedLevel = if (opacityLevel.isNaN()) 0f else opacityLevel.coerceIn(-1f, 1f)
    val sliderPosition = opacityLevelToSliderPosition(clampedLevel)
    val statusLabel = formatLiquidGlassLevelLabel(clampedLevel)

    var latestLevel by remember { mutableFloatStateOf(clampedLevel) }
    LaunchedEffect(clampedLevel) {
        latestLevel = clampedLevel
    }

    Card(
        colors = CardDefaults.defaultColors(color = colors.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        SliderPreference(
            value = sliderPosition,
            onValueChange = { rawPos ->
                val level = sliderPositionToOpacityLevel(rawPos)
                latestLevel = level
                onOpacityChange(level)
            },
            title = "外观通透度",
            summary = "调节玻璃雾度与通透感",
            valueText = null,
            endActions = {
                // 固定宽度 + 等宽数字 + 单行不换行，彻底杜绝拖动时左侧 summary 折行抖动
                Text(
                    text = statusLabel,
                    style = MiuixTheme.textStyles.body2.copy(fontFeatureSettings = "tnum"),
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.width(74.dp)
                )
            },
            bottomAction = {
                LiquidGlassLivePreview(
                    opacityLevel = clampedLevel,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                )
            },
            valueRange = 0f..1f,
            onValueChangeFinished = {
                onOpacityChangeFinished(latestLevel)
            },
            sliderColors = SliderDefaults.sliderColors(),
            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
            showKeyPoints = true,
            keyPoints = listOf(DEFAULT_LIQUID_GLASS_SLIDER_POSITION),
            magnetThreshold = 0.035f,
            insideMargin = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
        )

        LiquidGlassScaleLabelsRow(
            clampedLevel = clampedLevel,
            onSelectLevel = { target ->
                latestLevel = target
                onOpacityChange(target)
                onOpacityChangeFinished(target)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
    }
}

/**
 * 滑块下方刻度标签行：「默认」标签与 Miuix Slider 上 25% 关键点圆点水平精确对齐。
 */
@Composable
private fun LiquidGlassScaleLabelsRow(
    clampedLevel: Float,
    onSelectLevel: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val isClearActive = clampedLevel < -0.35f
    val isTintedActive = clampedLevel > 0.35f
    val isDefaultActive = !isClearActive && !isTintedActive

    val labels = listOf(
        Triple("通透", PlayerSettingsManager.MIN_LIQUID_GLASS_OPACITY, isClearActive),
        Triple("默认", PlayerSettingsManager.DEFAULT_LIQUID_GLASS_OPACITY, isDefaultActive),
        Triple("磨砂", PlayerSettingsManager.MAX_LIQUID_GLASS_OPACITY, isTintedActive)
    )

    Layout(
        modifier = modifier,
        content = {
            labels.forEach { (label, target, isActive) ->
                Text(
                    text = label,
                    style = MiuixTheme.textStyles.footnote1,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isActive) MiuixTheme.colorScheme.primary else colors.textSecondary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectLevel(target) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    ) { measurables, constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(looseConstraints) }
        val clearPlaceable = placeables[0]
        val defaultPlaceable = placeables[1]
        val tintedPlaceable = placeables[2]

        val width = constraints.maxWidth
        val height = maxOf(clearPlaceable.height, defaultPlaceable.height, tintedPlaceable.height)
        val thumbRadiusPx = (SliderDefaults.MinHeight / 2f).toPx()
        val availableWidthPx = (width - thumbRadiusPx * 2f).coerceAtLeast(0f)
        val defaultDotCenterX = thumbRadiusPx + DEFAULT_LIQUID_GLASS_SLIDER_POSITION * availableWidthPx
        val defaultX = (defaultDotCenterX - defaultPlaceable.width * 0.5f)
            .roundToInt()
            .coerceIn(0, (width - defaultPlaceable.width).coerceAtLeast(0))

        layout(width, height) {
            clearPlaceable.placeRelative(0, (height - clearPlaceable.height) / 2)
            defaultPlaceable.placeRelative(defaultX, (height - defaultPlaceable.height) / 2)
            tintedPlaceable.placeRelative(
                (width - tintedPlaceable.width).coerceAtLeast(0),
                (height - tintedPlaceable.height) / 2
            )
        }
    }
}

/**
 * 无边框平滑圆角实时预览视口：底层渲染高对比度色块与封面拼贴，前景悬浮真实 `liquidGlass` 迷你播控条与导航胶囊。
 */
@Composable
private fun LiquidGlassLivePreview(
    opacityLevel: Float,
    modifier: Modifier = Modifier
) {
    val isDark = AppTheme.isDark
    val colors = AppTheme.colors
    val previewAppearance = remember(opacityLevel) {
        LiquidGlassAppearance(opacityLevel = opacityLevel)
    }
    val frameBg = if (isDark) Color(0xFF101014) else Color(0xFFF4F4F8)
    val frameBgState = rememberUpdatedState(frameBg)
    val previewBackdrop = rememberLayerBackdrop {
        drawRect(frameBgState.value)
        drawContent()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(144.dp)
            .squircleClip(16.dp)
            .background(frameBg)
            .squircleBorder(0.75.dp, colors.coverBorder, 16.dp)
    ) {
        // 1. 底层被采样内容（同级兄弟节点，禁止将液态玻璃放入此层内部）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(previewBackdrop)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 左侧暖金唱片卡
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .squircleClip(12.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    if (isDark) Color(0xFF7B5728) else Color(0xFFE7C793),
                                    if (isDark) Color(0xFF4B351A) else Color(0xFFCFA56A)
                                )
                            )
                        )
                        .padding(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 48.dp, height = 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = if (isDark) 0.34f else 0.72f))
                        )
                        Text(
                            text = "DAILY MIX",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFFFFE4BA) else Color(0xFF2B1D0E),
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                // 中央品红渐变封面卡
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .squircleClip(12.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    if (isDark) Color(0xFF9C225E) else Color(0xFFFF7EB6),
                                    if (isDark) Color(0xFF531861) else Color(0xFF9B4DFF),
                                    if (isDark) Color(0xFF231845) else Color(0xFF5E39B8)
                                )
                            )
                        )
                        .padding(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (isDark) 0.16f else 0.28f))
                    )
                    Text(
                        text = "WONDERY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.88f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // 右侧青蓝高光卡
                Box(
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxHeight()
                        .squircleClip(12.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    if (isDark) Color(0xFF1D626E) else Color(0xFF8BE5E8),
                                    if (isDark) Color(0xFF162C3C) else Color(0xFF4CA8C9)
                                )
                            )
                        )
                )
            }

            // 底部文字行占位（透出玻璃底边时与真实首页一致）
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Updated Weekly",
                    fontSize = 9.sp,
                    color = colors.textSecondary.copy(alpha = 0.72f)
                )
                Text(
                    text = "For You",
                    fontSize = 9.sp,
                    color = colors.textSecondary.copy(alpha = 0.72f)
                )
            }
        }

        // 2. 前景液态玻璃控件预览（实时响应 previewAppearance）
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            // 迷你播放条胶囊
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .liquidGlass(
                        backdrop = previewBackdrop,
                        cornerRadius = 19.dp,
                        surfaceAlpha = GlassDefaults.SurfaceAlphaBright,
                        refractionHeight = 14.dp,
                        refractionAmount = 16.dp,
                        isDarkSurface = isDark,
                        enableShadow = true,
                        shadowRadius = 8.dp,
                        enableNoiseDither = true,
                        appearance = previewAppearance
                    )
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF22B573), Color(0xFF0E4D37))
                            )
                        )
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My kind of vibe",
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Setease Radio",
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    painter = painterResource(id = R.drawable.ic_mini_play),
                    contentDescription = null,
                    tint = colors.iconPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_mini_playlist),
                    contentDescription = null,
                    tint = colors.iconSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }

            // 底部导航胶囊
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .liquidGlass(
                        backdrop = previewBackdrop,
                        cornerRadius = 23.dp,
                        surfaceAlpha = GlassDefaults.SurfaceAlpha,
                        refractionHeight = 16.dp,
                        refractionAmount = 18.dp,
                        isDarkSurface = isDark,
                        enableShadow = true,
                        shadowRadius = 8.dp,
                        enableNoiseDither = true,
                        appearance = previewAppearance
                    )
                    .padding(horizontal = 5.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val previewTabs = listOf(
                    Triple("首页", R.drawable.ic_nav_home_filled, true),
                    Triple("电台", R.drawable.ic_nav_radio, false),
                    Triple("我的", R.drawable.ic_nav_mine, false),
                    Triple("搜索", R.drawable.ic_nav_search, false)
                )
                previewTabs.forEach { (title, iconRes, selected) ->
                    val pillColor = if (selected) {
                        if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.08f)
                    } else {
                        Color.Transparent
                    }
                    val tintColor = if (selected) colors.accent else colors.iconPrimary
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .background(pillColor),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = tintColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = title,
                            fontSize = 8.5.sp,
                            lineHeight = 10.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = tintColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
