package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.core.ui.adaptive.AdaptiveTwoPane
import com.example.seteasecloudmusic.core.ui.adaptive.LocalAdaptiveWindow
import com.example.seteasecloudmusic.feature.player.util.LyricParser

@Composable
internal fun ExpandedNativePlayer(
    viewModel: PlayerViewModel,
    onClose: () -> Unit,
    artworkAlpha: State<Float>,
    onArtworkBoundsChanged: (Rect) -> Unit,
    onQuality: () -> Unit,
    onQueue: () -> Unit
) {
    val state by viewModel.playbackState.collectAsStateWithLifecycle()
    val lyricResult by viewModel.lyricState.collectAsStateWithLifecycle()
    val quality by viewModel.currentAudioQuality.collectAsStateWithLifecycle()
    val lyrics = remember(lyricResult) { LyricParser.parseLyricResponse(lyricResult?.getOrNull()) }
    val track = state.currentTrack
    val playing = state.status == PlayerStatus.PLAYING
    val window = LocalAdaptiveWindow.current
    val controlsInSecondary = window.isTabletop || window.height < 480.dp
    val local = track?.let { it.id < 0 || it.playableUrl?.let { url -> url.startsWith("content:") || url.startsWith("file:") || url.startsWith("/") } == true } == true

    val controls: @Composable () -> Unit = {
        PlayerControls(
            currentPositionMs = state.currentPositionMs, durationMs = state.durationMs, isPlaying = playing,
            playbackMode = state.playbackMode, isLocalTrack = local, currentQuality = quality,
            hasTrack = track != null, isLyricsPage = true,
            onPlayPause = viewModel::onPlayPause, onNext = viewModel::onNext,
            onPrevious = { viewModel.onPrevious() }, onSeekTo = viewModel::seekTo,
            onTogglePlaybackMode = viewModel::togglePlaybackMode,
            onSelectQualityClick = onQuality, onQueueClick = onQueue
        )
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF121217)).pointerInput(Unit) { detectTapGestures(onTap = {}) }) {
        AsyncImage(track?.coverUrl, null, Modifier.fillMaxSize().graphicsLayer { alpha = 0.12f }, contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF01B1B23), Color(0xE614141B)))))
        AdaptiveTwoPane(
            modifier = Modifier.fillMaxSize().systemBarsPadding(), primaryFraction = if (window.height < 480.dp) 0.34f else if (window.width < 760.dp) 0.54f else 0.44f, gap = 28.dp,
            primary = {
                Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("正在播放", Modifier.weight(1f), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        IconButton(onClose) { Icon(Icons.Filled.Close, "关闭播放器", tint = Color.White) }
                    }
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val size = minOf(maxWidth - 16.dp, maxHeight - 12.dp, 420.dp).coerceAtLeast(0.dp)
                        Box(Modifier.size(size).shadow(24.dp, RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp)).background(Color(0xFF30303A))
                            .onGloballyPositioned { onArtworkBoundsChanged(it.boundsInRoot()) }
                            .graphicsLayer { alpha = artworkAlpha.value }, contentAlignment = Alignment.Center) {
                            if (track?.coverUrl != null) AsyncImage(track.coverUrl, "专辑封面", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Icon(Icons.Filled.MusicNote, null, Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.3f))
                        }
                    }
                    PlayerTrackCaption(track)
                    if (!controlsInSecondary) {
                        Box(Modifier.heightIn(max = 250.dp).verticalScroll(rememberScrollState())) { controls() }
                    }
                }
            },
            secondary = {
                Column(Modifier.fillMaxSize().padding(end = 20.dp)) {
                    if (controlsInSecondary) {
                        Box(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) { controls() }
                    } else {
                        Text("歌词", Modifier.padding(top = 28.dp, bottom = 12.dp, start = 18.dp), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                    if (lyrics.lines.isNotEmpty()) {
                        val index = lyrics.lines.indexOfLast { it.startTime <= state.currentPositionMs }.coerceAtLeast(0)
                        LyricsColumn(lyrics, index, state.currentPositionMs, playing, viewModel::seekTo, Modifier.weight(1f).fillMaxWidth())
                    } else {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(if (track == null) "选择一首音乐，开始聆听" else "暂无歌词", color = Color.White.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun PlayerTrackCaption(track: Track?) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(track?.title ?: "未在播放", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(5.dp))
        Text(track?.artists?.joinToString(" / ") { it.name } ?: "从音乐库中挑选喜欢的音乐", color = Color.White.copy(alpha = 0.55f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
