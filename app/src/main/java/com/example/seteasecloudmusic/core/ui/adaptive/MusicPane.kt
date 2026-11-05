package com.example.seteasecloudmusic.core.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seteasecloudmusic.core.ui.components.LiquidGlassPanel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle

@Composable
fun MusicPaneHeader(title: String, subtitle: String? = null, onClose: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        val sideControls = LocalAdaptiveWindow.current.useNavigationRail && !LocalAdaptiveWindow.current.sideControlsAtStart
        val back: @Composable () -> Unit = {
            IconButton(onClick = { onClose?.invoke() }, modifier = Modifier.size(44.dp)
                .background(Color.White.copy(alpha = 0.6f), RoundedRectangle(22.dp))
                .border(0.75.dp, Color.White.copy(alpha = 0.85f), RoundedRectangle(22.dp))) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color(0xFF393941), modifier = Modifier.size(22.dp))
            }
        }
        if (onClose != null && !sideControls) { back(); Spacer(Modifier.width(8.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15151A), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, fontSize = 13.sp, color = Color(0xFF81818B))
            }
        }
        if (onClose != null && sideControls) { Spacer(Modifier.width(8.dp)); back() }
    }
}

@Composable
fun MusicDetailPlaceholder(title: String = "你的音乐，尽在这里", description: String = "选择左侧歌单，开始探索与聆听") {
    val background = rememberLayerBackdrop { drawContent() }
    BoxWithConstraints(Modifier.fillMaxSize().testTag("music_detail_placeholder")) {
        val compact = maxHeight < 300.dp
        Canvas(Modifier.fillMaxSize().layerBackdrop(background)) {
            drawRect(Color.White)
            drawRect(Brush.radialGradient(listOf(Color(0xFFFCE4EB), Color.Transparent),
                center = Offset(size.width * 0.32f, size.height * 0.48f), radius = 210.dp.toPx()))
            drawRect(Brush.radialGradient(listOf(Color(0xFFE8E8FC).copy(alpha = 0.75f), Color.Transparent),
                center = Offset(size.width * 0.75f, size.height * 0.58f), radius = 180.dp.toPx()))
        }
        LiquidGlassPanel(Modifier.align(Alignment.Center).padding(20.dp).widthIn(max = 340.dp).fillMaxWidth()
            .heightIn(max = (maxHeight - 40.dp).coerceAtLeast(0.dp)), backdrop = background) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.MusicNote, null, Modifier.size(if (compact) 40.dp else 56.dp),
                    tint = Color(0xFFFA233B).copy(alpha = 0.8f))
                Spacer(Modifier.height(16.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp,
                    textAlign = TextAlign.Center, color = Color(0xFF26262D))
                Spacer(Modifier.height(10.dp))
                Text(description, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, color = Color(0xFF85858E))
            }
        }
    }
}
