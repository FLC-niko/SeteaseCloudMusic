package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import com.example.seteasecloudmusic.core.ui.components.LiquidGlassPanel
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.shapes.RoundedRectangle
import com.example.seteasecloudmusic.core.player.PlaybackState
import com.example.seteasecloudmusic.core.settings.OnlineAudioQuality
import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
    val lyrics by viewModel.parsedLyrics.collectAsStateWithLifecycle()
    val isLyricsLoading by viewModel.isLyricsLoading.collectAsStateWithLifecycle()
    val quality by viewModel.currentAudioQuality.collectAsStateWithLifecycle()
    val likedTrackIds by viewModel.likedTrackIds.collectAsStateWithLifecycle()

    ExpandedNativePlayerContent(state, lyrics, quality, onClose, artworkAlpha, onArtworkBoundsChanged,
        viewModel::onPlayPause, viewModel::onNext, { viewModel.onPrevious() }, viewModel::seekTo,
        onQuality, onQueue, isLyricsLoading, likedTrackIds, viewModel::toggleTrackLiked,
        viewModel::togglePlaybackMode)
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
    onQuality: () -> Unit,
    onQueue: () -> Unit,
    isLyricsLoading: Boolean = false,
    likedTrackIds: Set<Long> = emptySet(),
    onToggleStar: (Long) -> Unit = {},
    onToggleMode: () -> Unit = {}
) {
    val sharedBackground = LocalPlayerBackdrop.current
    val background = sharedBackground ?: rememberLayerBackdrop { drawContent() }
    val track = state.currentTrack
    val playing = state.status == PlayerStatus.PLAYING
    val window = LocalAdaptiveWindow.current
    val controlsInSecondary = window.isTabletop || window.height < 480.dp
    val local = track?.let {
        it.id < 0 || it.playableUrl?.let { url ->
            url.startsWith("content:") || url.startsWith("file:") || url.startsWith("/")
        } == true
    } == true
    val isStarred = track?.id?.let { it in likedTrackIds } == true
    val isExplicit = remember(track?.id, track?.title, lyrics) { isExplicitTrack(track, lyrics) }
    val sharedArtwork = LocalPlayerArtwork.current

    val controls: @Composable () -> Unit = {
        AppleMusicPlaybackControls(
            currentPositionMs = state.currentPositionMs,
            durationMs = state.durationMs,
            isPlaying = playing,
            isLyricsPage = true,
            hasTrack = track != null,
            currentAudioQuality = quality,
            isLocalTrack = local,
            playbackMode = state.playbackMode,
            onTogglePlaybackMode = onToggleMode,
            onAudioQualityClick = onQuality,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            onSeekTo = onSeek,
            onToggleLyrics = {},
            onQueueClick = onQueue
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(if (sharedBackground == null) Color(0xFF101014) else Color.Transparent)
            .pointerInput(Unit) { detectTapGestures(onTap = {}) }
    ) {
        if (sharedBackground == null) PlayerBackdrop(track, Modifier.layerBackdrop(background), playing)

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // 顶部居中收起小白条（仅此区域支持下拉或点按收起）
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 120.dp, height = 32.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    )
                    .pointerInput(onClose) {
                        var dragged = 0f
                        detectVerticalDragGestures(
                            onDragStart = { dragged = 0f },
                            onVerticalDrag = { change, amount ->
                                change.consume()
                                dragged += amount
                            },
                            onDragEnd = {
                                if (dragged > 80.dp.toPx()) onClose()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(width = 42.dp, height = 4.5.dp)
                        .background(Color.White.copy(alpha = 0.34f), RoundedCornerShape(2.25.dp))
                )
            }

            AdaptiveTwoPane(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                primaryFraction = if (window.height < 480.dp) 0.36f else if (window.width < 760.dp) 0.50f else 0.44f,
                gap = 28.dp,
                primary = {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BoxWithConstraints(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            val coverSize = minOf(maxWidth - 16.dp, maxHeight - 12.dp, 400.dp).coerceAtLeast(0.dp)
                            val playbackScale by rememberAppleMusicPlaybackScale(playing)
                            Box(
                                Modifier
                                    .size(coverSize)
                                    .graphicsLayer {
                                        scaleX = playbackScale
                                        scaleY = playbackScale
                                        alpha = artworkAlpha.value
                                    }
                                    .shadow(24.dp, RoundedRectangle(24.dp))
                                    .clip(RoundedRectangle(24.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .onGloballyPositioned { onArtworkBoundsChanged(it.boundsInRoot()) },
                                contentAlignment = Alignment.Center
                            ) {
                                val coverUrl = track?.coverUrl
                                if (!coverUrl.isNullOrBlank()) {
                                    val normalizedUrl = remember(coverUrl) { normalizeHighResCoverUrl(coverUrl) }
                                    val painter = sharedArtwork?.takeIf { it.first == normalizedUrl }?.second
                                    if (painter != null) {
                                        Image(
                                            painter = painter,
                                            contentDescription = "专辑封面",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        AsyncImage(
                                            model = rememberHighResArtworkRequest(coverUrl),
                                            contentDescription = "专辑封面",
                                            modifier = Modifier.fillMaxSize(),
                                            filterQuality = FilterQuality.High,
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                } else {
                                    Icon(
                                        Icons.Filled.MusicNote,
                                        null,
                                        Modifier.size(64.dp),
                                        tint = Color.White.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }

                        PlayerTrackCaption(
                            track = track,
                            isStarred = isStarred,
                            isExplicit = isExplicit,
                            onToggleStar = { track?.let { onToggleStar(it.id) } },
                            onMore = onQuality
                        )

                        if (!controlsInSecondary) {
                            LiquidGlassPanel(
                                Modifier.align(Alignment.CenterHorizontally).widthIn(max = 620.dp)
                                    .fillMaxWidth().padding(bottom = 12.dp).heightIn(max = 260.dp),
                                backdrop = background, containerColor = Color(0xFF1C1C24), cornerRadius = 26.dp
                            ) {
                                Box(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 4.dp)) { controls() }
                            }
                        }
                    }
                },
                secondary = {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(end = 20.dp)
                    ) {
                        if (controlsInSecondary) {
                            LiquidGlassPanel(
                                Modifier.align(Alignment.CenterHorizontally).widthIn(max = 620.dp)
                                    .fillMaxWidth().padding(bottom = 12.dp).heightIn(max = 240.dp),
                                backdrop = background, containerColor = Color(0xFF1C1C24), cornerRadius = 26.dp
                            ) {
                                Box(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 4.dp)) { controls() }
                            }
                        }
                        when {
                            lyrics.lines.isNotEmpty() -> {
                                val index = lyrics.lines
                                    .indexOfLast { it.startTime <= state.currentPositionMs }
                                    .coerceAtLeast(0)
                                LyricsColumn(
                                    lyrics = lyrics,
                                    activeLineIndex = index,
                                    currentTimeMs = state.currentPositionMs,
                                    isPlaying = playing,
                                    onLineClick = onSeek,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                )
                            }
                            isLyricsLoading && track != null -> {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.78f))
                                }
                            }
                            else -> {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (track == null) "选择一首音乐，开始聆听" else "暂无歌词",
                                        color = Color.White.copy(alpha = 0.45f)
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun PlayerTrackCaption(
    track: Track?,
    isStarred: Boolean,
    isExplicit: Boolean,
    onToggleStar: () -> Unit,
    onMore: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    track?.title ?: "未在播放",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isExplicit) {
                    Spacer(Modifier.width(6.dp))
                    AppleMusicExplicitBadge(size = 15.dp)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                track?.artists?.joinToString(" / ") { it.name } ?: "从音乐库中挑选喜欢的音乐",
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (track != null) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleStar),
                contentAlignment = Alignment.Center
            ) {
                AppleMusicHeartIcon(isLiked = isStarred, size = 23.dp)
            }
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onMore),
                contentAlignment = Alignment.Center
            ) {
                AppleMusicMoreIcon(size = 23.dp)
            }
        }
    }
}
