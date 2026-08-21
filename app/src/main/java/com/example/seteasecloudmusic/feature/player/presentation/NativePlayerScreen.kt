package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.core.player.PlaybackState
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics
import com.example.seteasecloudmusic.feature.player.util.LyricParser

@Composable
fun NativePlayerScreen(
    playerViewModel: PlayerViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackState by playerViewModel.playbackState.collectAsState()
    val lyricResponseResult by playerViewModel.lyricState.collectAsState()

    val currentPosition = playbackState.currentPositionMs
    val isPlaying = playbackState.status == PlayerStatus.PLAYING
    val track = playbackState.currentTrack

    // 解析歌词
    val parsedLyrics: ParsedLyrics = remember(lyricResponseResult) {
        LyricParser.parseLyricResponse(lyricResponseResult?.getOrNull())
    }

    // 计算当前高亮歌词行索引
    val activeLineIndex = remember(parsedLyrics, currentPosition) {
        if (parsedLyrics.lines.isEmpty()) {
            -1
        } else {
            val idx = parsedLyrics.lines.indexOfLast { it.startTime <= currentPosition }
            idx.coerceAtLeast(0)
        }
    }

    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16161A))
            .offset { IntOffset(0, offsetY.toInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY > 200.dp.toPx()) {
                            onClose()
                        } else {
                            offsetY = 0f
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount > 0f || offsetY > 0f) {
                            offsetY = (offsetY + dragAmount).coerceAtLeast(0f)
                        }
                    }
                )
            }
    ) {
        // ── Layer 0: 沉浸式专辑封面高斯模糊背景 ──
        if (track?.coverUrl != null) {
            AsyncImage(
                model = track.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 50.dp)
            )
        }

        // ── Layer 1: 渐变暗色遮罩层，提升歌词可读性 ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.6f),
                        0.4f to Color.Black.copy(alpha = 0.45f),
                        0.8f to Color.Black.copy(alpha = 0.75f),
                        1.0f to Color.Black.copy(alpha = 0.95f)
                    )
                )
        )

        // ── Layer 2: 核心交互层 ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // 顶部下拉指示条与返回按钮
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .height(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .background(
                                Color.White.copy(alpha = 0.5f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = "收起",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // 歌曲基本信息（标题、歌手）
            if (track != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = track.coverUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = track.artists.joinToString(" / ") { it.name }.ifBlank { "未知艺术家" },
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 歌词滚动区域
            if (parsedLyrics.lines.isNotEmpty()) {
                LyricsColumn(
                    lyrics = parsedLyrics,
                    activeLineIndex = activeLineIndex,
                    currentTimeMs = currentPosition,
                    onLineClick = { seekMs -> playerViewModel.seekTo(seekMs) },
                    modifier = Modifier.weight(1f)
                )
            } else if (lyricResponseResult == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.8f))
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无歌词",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 16.sp
                    )
                }
            }

            // 底部原生控制栏
            PlayerControls(
                currentPositionMs = currentPosition,
                durationMs = playbackState.durationMs,
                isPlaying = isPlaying,
                playbackMode = playbackState.playbackMode,
                onPlayPause = { playerViewModel.onPlayPause() },
                onNext = { playerViewModel.onNext() },
                onPrevious = { playerViewModel.onPrevious() },
                onSeekTo = { posMs -> playerViewModel.seekTo(posMs) },
                onTogglePlaybackMode = { playerViewModel.togglePlaybackMode() }
            )
        }
    }
}
