package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seteasecloudmusic.feature.player.domain.model.LyricLine
import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics

@Composable
fun LyricsColumn(
    lyrics: ParsedLyrics,
    activeLineIndex: Int,
    currentTimeMs: Int,
    onLineClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // 自动滚动到当前歌词行
    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex in lyrics.lines.indices) {
            runCatching {
                listState.animateScrollToItem(
                    index = activeLineIndex,
                    scrollOffset = -180
                )
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 120.dp)
    ) {
        itemsIndexed(
            items = lyrics.lines,
            key = { index, line -> "${line.startTime}_$index" }
        ) { index, line ->
            if (!line.isBG) {
                LyricLineItem(
                    line = line,
                    isActive = index == activeLineIndex,
                    onClick = { onLineClick?.invoke(line.startTime) }
                )
            }
        }
    }
}

@Composable
private fun LyricLineItem(
    line: LyricLine,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.35f,
        animationSpec = tween(300),
        label = "lyricAlpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.04f else 1f,
        animationSpec = spring(stiffness = 300f, dampingRatio = 0.7f),
        label = "lyricScale"
    )

    val lyricText = line.words.joinToString("") { it.word }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 28.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
    ) {
        Text(
            text = lyricText,
            color = Color.White.copy(alpha = alpha),
            fontSize = if (isActive) 22.sp else 16.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            lineHeight = if (isActive) 30.sp else 22.sp
        )

        // 翻译歌词
        if (line.translatedLyric.isNotBlank()) {
            Text(
                text = line.translatedLyric,
                color = Color.White.copy(alpha = alpha * 0.6f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
