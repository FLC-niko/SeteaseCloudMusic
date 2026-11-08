package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.core.player.PlaybackState
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.core.settings.OnlineAudioQuality
import com.example.seteasecloudmusic.core.ui.adaptive.AdaptiveModalSheet
import com.example.seteasecloudmusic.core.ui.components.LiquidGlassPanel
import com.kyant.backdrop.Backdrop

/** Expanded lyrics keep related actions in a floating bar; narrow fold segments retain every action. */
@Composable
internal fun ExpandedPlaybackBar(
    state: PlaybackState,
    quality: OnlineAudioQuality,
    backdrop: Backdrop,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Int) -> Unit,
    onQuality: () -> Unit,
    onToggleMode: () -> Unit,
    onToggleLyrics: () -> Unit,
    onQueue: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = state.currentTrack
    val hasTrack = track != null
    val playing = state.status == PlayerStatus.PLAYING
    var volumeOpen by remember { mutableStateOf(false) }
    var previousClicks by remember { mutableIntStateOf(0) }
    var nextClicks by remember { mutableIntStateOf(0) }
    val local = track?.let { it.id < 0 || it.playableUrl?.let { url ->
        url.startsWith("content:") || url.startsWith("file:") || url.startsWith("/")
    } == true } == true

    LiquidGlassPanel(modifier.testTag("player_controls"), backdrop, Color(0xFF1C1C24), 28.dp) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            val wide = maxWidth >= 700.dp
            val showQueue = maxWidth >= 360.dp
            val showVolume = maxWidth >= 320.dp
            val trackLabel: @Composable (Modifier) -> Unit = { labelModifier ->
                Row(labelModifier.testTag("player_track_caption"), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(track?.coverUrl, "专辑封面", Modifier.size(34.dp).clip(RoundedCornerShape(7.dp)),
                        contentScale = ContentScale.Crop)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(track?.title ?: "未在播放", color = Color.White, fontSize = 14.sp, lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track?.artists?.joinToString(" / ") { it.name }.orEmpty(), color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp, lineHeight = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Column {
                if (!wide) trackLabel(Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 8.dp))
                AppleMusicProgressSlider(state.currentPositionMs, state.durationMs, playing, hasTrack, quality,
                    local, onQuality, onSeek)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (wide) BarAction("切换播放模式", hasTrack, onToggleMode) {
                        AppleMusicPlaybackModeIcon(state.playbackMode, size = 22.dp, tint = Color.White)
                    }
                    BarAction("上一首", hasTrack, { previousClicks++; onPrevious() }) {
                        AppleMusicSkipIcon(false, previousClicks, size = 28.dp, tint = Color.White)
                    }
                    BarAction(if (playing) "暂停" else "播放", hasTrack, onPlayPause) {
                        AppleMusicPlayPauseAnimatedIcon(playing, size = 30.dp, tint = Color.White)
                    }
                    BarAction("下一首", hasTrack, { nextClicks++; onNext() }) {
                        AppleMusicSkipIcon(true, nextClicks, size = 28.dp, tint = Color.White)
                    }
                    if (wide) {
                        trackLabel(Modifier.weight(1f).padding(horizontal = 12.dp))
                        AppleMusicVolumeSlider(Modifier.width(110.dp).padding(horizontal = 4.dp))
                    } else {
                        Spacer(Modifier.weight(1f))
                        if (showVolume) BarAction("音量", true, { volumeOpen = true }) {
                            AppleMusicVolumeHighIcon(size = 22.dp, tint = Color.White)
                        }
                    }
                    AppleMusicAudioOutputButton(Modifier.size(44.dp), Color.White)
                    BarAction("歌词", hasTrack, onToggleLyrics) {
                        AppleMusicLyricsToggleIcon(true, buttonSize = 40.dp, iconSize = 22.dp)
                    }
                    if (wide || showQueue) BarAction("待播清单", true, onQueue) {
                        AppleMusicQueueIcon(size = 22.dp, tint = Color.White)
                    }
                    BarAction("更多播放操作", true, onMore) { AppleMusicMoreIcon(size = 23.dp, tint = Color.White) }
                }
            }
        }
    }
    if (volumeOpen) AdaptiveModalSheet({ volumeOpen = false }, Color(0xFF1C1C24)) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Text("音量", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(20.dp))
            AppleMusicVolumeSlider()
        }
    }
}

@Composable
private fun BarAction(label: String, enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.size(44.dp).graphicsLayer { alpha = if (enabled) 1f else 0.35f }.clip(CircleShape).semantics { contentDescription = label }
        .clickable(remember { MutableInteractionSource() }, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center) { content() }
}
