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
import com.example.seteasecloudmusic.core.ui.components.LiquidGlassPanel
import com.example.seteasecloudmusic.core.ui.components.liquidGlass
import com.example.seteasecloudmusic.core.player.PlaybackState
import com.example.seteasecloudmusic.core.settings.OnlineAudioQuality
import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.shapes.RoundedRectangle
import androidx.compose.material.icons.filled.ExpandMore
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
    ExpandedNativePlayerContent(state, lyrics, quality, onClose, artworkAlpha, onArtworkBoundsChanged,
        viewModel::onPlayPause, viewModel::onNext, { viewModel.onPrevious() }, viewModel::seekTo,
        viewModel::togglePlaybackMode, onQuality, onQueue)
}

@Composable
internal fun ExpandedNativePlayerContent(
    state: PlaybackState,
    lyrics: ParsedLyrics,
    quality: OnlineAudioQuality,
    onClose: () -> Unit,
    artworkAlpha: State<Float>,
    onArtworkBoundsChanged: (Rect) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Int) -> Unit,
    onMode: () -> Unit,
    onQuality: () -> Unit,
    onQueue: () -> Unit
) {
    val track = state.currentTrack
    val playing = state.status == PlayerStatus.PLAYING
    val sharedBackground = LocalPlayerBackdrop.current
    val background = sharedBackground ?: rememberLayerBackdrop { drawContent() }
    val window = LocalAdaptiveWindow.current
    val controlsInSecondary = window.isTabletop || window.height < 480.dp
    val local = track?.let { it.id < 0 || it.playableUrl?.let { url -> url.startsWith("content:") || url.startsWith("file:") || url.startsWith("/") } == true } == true

    val controls: @Composable () -> Unit = {
        PlayerControls(
            currentPositionMs = state.currentPositionMs, durationMs = state.durationMs, isPlaying = playing,
            playbackMode = state.playbackMode, isLocalTrack = local, currentQuality = quality,
            hasTrack = track != null, isLyricsPage = true,
            onPlayPause = onPlayPause, onNext = onNext,
            onPrevious = onPrevious, onSeekTo = onSeek,
            onTogglePlaybackMode = onMode,
            onSelectQualityClick = onQuality, onQueueClick = onQueue
        )
    }
    Box(Modifier.fillMaxSize().background(if (sharedBackground == null) Color(0xFF121217) else Color.Transparent)
        .pointerInput(Unit) { detectTapGestures(onTap = {}) }) {
        if (sharedBackground == null) PlayerBackdrop(track, Modifier.layerBackdrop(background))
        AdaptiveTwoPane(
            modifier = Modifier.fillMaxSize().systemBarsPadding(), primaryFraction = if (window.height < 480.dp) 0.34f else if (window.width < 760.dp) 0.54f else 0.44f, gap = 28.dp,
            primary = {
                Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("正在播放", Modifier.weight(1f), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        IconButton(onClose, Modifier.size(44.dp).liquidGlass(background, 22.dp, surfaceAlpha = 0.06f)) {
                            Icon(Icons.Filled.ExpandMore, "收起播放器", tint = Color.White)
                        }
                    }
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val size = minOf(maxWidth - 16.dp, maxHeight - 12.dp, 420.dp).coerceAtLeast(0.dp)
                        Box(Modifier.size(size).shadow(24.dp, RoundedRectangle(24.dp))
                            .clip(RoundedRectangle(24.dp)).background(Color.White.copy(alpha = 0.07f))
                            .onGloballyPositioned { onArtworkBoundsChanged(it.boundsInRoot()) }
                            .graphicsLayer { alpha = artworkAlpha.value }, contentAlignment = Alignment.Center) {
                            if (track?.coverUrl != null) AsyncImage(track.coverUrl, "专辑封面", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Icon(Icons.Filled.MusicNote, null, Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.3f))
                        }
                    }
                    PlayerTrackCaption(track)
                    if (!controlsInSecondary) {
                        LiquidGlassPanel(Modifier.fillMaxWidth().padding(bottom = 12.dp).heightIn(max = 250.dp),
                            backdrop = background, containerColor = Color(0xFF1C1C24), cornerRadius = 26.dp) {
                            Box(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 4.dp)) { controls() }
                        }
                    }
                }
            },
            secondary = {
                Column(Modifier.fillMaxSize().padding(end = 20.dp)) {
                    if (controlsInSecondary) {
                        LiquidGlassPanel(Modifier.align(Alignment.CenterHorizontally).widthIn(max = 620.dp)
                            .fillMaxWidth().padding(top = 8.dp, bottom = 8.dp).heightIn(max = 240.dp),
                            backdrop = background, containerColor = Color(0xFF1C1C24), cornerRadius = 26.dp) {
                            Box(Modifier.verticalScroll(rememberScrollState())) { controls() }
                        }
                    } else {
                        Text("歌词", Modifier.padding(top = 28.dp, bottom = 12.dp, start = 18.dp), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                    if (lyrics.lines.isNotEmpty()) {
                        val index = lyrics.lines.indexOfLast { it.startTime <= state.currentPositionMs }.coerceAtLeast(0)
                        LyricsColumn(lyrics, index, state.currentPositionMs, playing, onSeek, Modifier.weight(1f).fillMaxWidth())
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
