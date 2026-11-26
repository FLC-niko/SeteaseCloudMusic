package com.example.seteasecloudmusic.core.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seteasecloudmusic.core.ui.components.LiquidGlassCircleButton
import com.example.seteasecloudmusic.core.ui.components.LiquidGlassPanel
import com.example.seteasecloudmusic.core.ui.components.LocalGlassBackdrop
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Music

/** 保留返回时的列表位置；详情覆盖后，底页退出绘制、触控和无障碍树。 */
@Composable
fun RetainedMusicFeed(visible: Boolean, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()
        .then(if (visible) Modifier else Modifier.clearAndSetSemantics {})
        .layout { measurable, constraints ->
            val page = measurable.measure(constraints)
            layout(page.width, page.height) { if (visible) page.placeRelative(0, 0) }
        }) { content() }
}

@Composable
fun MusicPaneHeader(title: String, subtitle: String? = null, onClose: (() -> Unit)? = null, onMore: (() -> Unit)? = null) {
    val backdrop = LocalGlassBackdrop.current
    val colors = AppTheme.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        val sideControls = LocalAdaptiveWindow.current.useNavigationRail && !LocalAdaptiveWindow.current.sideControlsAtStart
        val back: @Composable () -> Unit = {
            LiquidGlassCircleButton(
                onClick = { onClose?.invoke() },
                backdrop = backdrop,
                size = 44.dp,
                contentDescription = "返回"
            ) {
                Icon(MiuixIcons.Back, "返回", tint = colors.iconPrimary, modifier = Modifier.size(22.dp))
            }
        }
        if (onClose != null && !sideControls) { back(); Spacer(Modifier.width(8.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, fontSize = 13.sp, color = colors.textSecondary)
            }
        }
        if (onMore != null) {
            Spacer(Modifier.width(8.dp))
            LiquidGlassCircleButton(
                onClick = onMore,
                backdrop = backdrop,
                size = 44.dp,
                contentDescription = "更多操作"
            ) {
                Icon(MiuixIcons.More, "更多操作", Modifier.size(22.dp), tint = colors.iconPrimary)
            }
        }
        if (onClose != null && sideControls) { Spacer(Modifier.width(8.dp)); back() }
    }
}

@Composable
fun MusicDetailPlaceholder(title: String = "你的音乐，尽在这里", description: String = "选择左侧歌单，开始探索与聆听") {
    val background = rememberLayerBackdrop { drawContent() }
    val isDark = AppTheme.isDark
    val colors = AppTheme.colors
    val firstGlow = if (isDark) Color(0xFF5A2038).copy(alpha = 0.42f) else Color(0xFFFCE4EB)
    val secondGlow = if (isDark) Color(0xFF1C2E5A).copy(alpha = 0.36f) else Color(0xFFE8E8FC).copy(alpha = 0.75f)
    BoxWithConstraints(Modifier.fillMaxSize().testTag("music_detail_placeholder")) {
        val compact = maxHeight < 300.dp
        Canvas(Modifier.fillMaxSize().layerBackdrop(background)) {
            drawRect(colors.background)
            drawRect(Brush.radialGradient(listOf(firstGlow, Color.Transparent),
                center = Offset(size.width * 0.32f, size.height * 0.48f), radius = 210.dp.toPx()))
            drawRect(Brush.radialGradient(listOf(secondGlow, Color.Transparent),
                center = Offset(size.width * 0.75f, size.height * 0.58f), radius = 180.dp.toPx()))
        }
        LiquidGlassPanel(Modifier.align(Alignment.Center).padding(20.dp).widthIn(max = 340.dp).fillMaxWidth()
            .heightIn(max = (maxHeight - 40.dp).coerceAtLeast(0.dp)), backdrop = background) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(MiuixIcons.Music, null, Modifier.size(if (compact) 40.dp else 56.dp),
                    tint = colors.accent.copy(alpha = 0.85f))
                Spacer(Modifier.height(16.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp,
                    textAlign = TextAlign.Center, color = colors.textPrimary)
                Spacer(Modifier.height(10.dp))
                Text(description, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, color = colors.textSecondary)
            }
        }
    }
}
