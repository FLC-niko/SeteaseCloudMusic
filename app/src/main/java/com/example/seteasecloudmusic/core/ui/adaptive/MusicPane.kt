package com.example.seteasecloudmusic.core.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MusicPaneHeader(title: String, subtitle: String? = null, onClose: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onClose != null) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15151A), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, fontSize = 13.sp, color = Color(0xFF81818B))
            }
        }
    }
}

@Composable
fun MusicDetailPlaceholder(title: String = "你的音乐，尽在这里", description: String = "选择左侧歌单，开始探索与聆听") {
    Box(Modifier.fillMaxSize().padding(20.dp).background(Color.White.copy(alpha = 0.65f), RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.MusicNote, null, Modifier.size(72.dp), tint = Color(0xFFFA233B).copy(alpha = 0.7f))
            Spacer(Modifier.height(24.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFF26262D))
            Spacer(Modifier.height(10.dp))
            Text(description, fontSize = 14.sp, color = Color(0xFF85858E))
        }
    }
}
