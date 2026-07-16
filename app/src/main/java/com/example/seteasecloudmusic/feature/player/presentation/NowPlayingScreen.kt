package com.example.seteasecloudmusic.feature.player.presentation

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.media.MediaRouter2
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.core.player.QueueRepeatMode
import com.example.seteasecloudmusic.feature.player.presentation.lyric.FlamingoLyricData
import com.example.seteasecloudmusic.feature.player.presentation.lyric.FlamingoLyricView
import com.example.seteasecloudmusic.feature.player.presentation.lyric.LyricDataAdapter
import com.example.seteasecloudmusic.feature.player.presentation.lyric.LyricUIConfig
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun NowPlayingScreen(
    presentationProgress: Float = 1f,
    sourceBarBounds: Rect = Rect.Zero,
    sourceArtworkBounds: Rect = Rect.Zero,
    onClose: () -> Unit
) {
    val viewModel: PlayerViewModel = hiltViewModel()
    val playbackState by viewModel.playbackState.collectAsState()
    val lyricsState by viewModel.lyricsState.collectAsState()
    val isCurrentTrackFavorite by viewModel.isCurrentTrackFavorite.collectAsState()
    var showMoreSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }

    LaunchedEffect(playbackState.currentTrack?.id) {
        playbackState.currentTrack?.let { viewModel.loadLyrics(it.id) }
    }

    val flamingoData = remember(lyricsState) {
        val state = lyricsState
        if (state is LyricsUiState.Success) {
            LyricDataAdapter.toFlamingoFormat(state.lyrics)
        } else {
            FlamingoLyricData(emptyList(), emptyList())
        }
    }

    val track = playbackState.currentTrack
    val coverUrl = track?.coverUrl ?: track?.album?.coverUrl
    val songName = track?.title ?: "未知歌曲"
    val artistName = track?.artists?.joinToString(", ") { it.name } ?: "未知歌手"
    val isPlaying = playbackState.status == PlayerStatus.PLAYING

    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val clampedPresentationProgress = presentationProgress.coerceIn(0f, 1f)
    val contentRevealProgress = (
        (clampedPresentationProgress - 0.08f) / 0.56f
        ).coerceIn(0f, 1f)
    val pageProgress = (
        pagerState.currentPage + pagerState.currentPageOffsetFraction
        ).coerceIn(0f, 1f)

    BackHandler {
        if (pageProgress > 0.5f) {
            coroutineScope.launch { pagerState.animateScrollToPage(0) }
        } else {
            onClose()
        }
    }

    val context = LocalContext.current
    var dominantColor by remember { mutableStateOf(Color(0xFF241F23)) }
    var mutedColor by remember { mutableStateOf(Color(0xFF151517)) }
    var accentColor by remember { mutableStateOf(Color(0xFFBEB4AA)) }

    LaunchedEffect(coverUrl) {
        if (coverUrl == null) {
            dominantColor = Color(0xFF241F23)
            mutedColor = Color(0xFF151517)
            accentColor = Color(0xFFBEB4AA)
            return@LaunchedEffect
        }

        try {
            val request = ImageRequest.Builder(context)
                .data(coverUrl)
                .size(260, 260)
                .allowHardware(false)
                .build()
            val bitmap = (ImageLoader(context).execute(request).drawable as? BitmapDrawable)?.bitmap
            if (bitmap != null) {
                val palette = Palette.from(bitmap).generate()
                val fallbackTop = 0xFF241F23.toInt()
                val fallbackBottom = 0xFF151517.toInt()
                val top = palette.getDarkVibrantColor(
                    palette.getDarkMutedColor(fallbackTop)
                )
                dominantColor = Color(top)
                mutedColor = Color(
                    palette.getMutedColor(
                        palette.getDarkMutedColor(fallbackBottom)
                    )
                )
                accentColor = Color(
                    palette.getVibrantColor(
                        palette.getLightVibrantColor(0xFFBEB4AA.toInt())
                    )
                )
            }
        } catch (_: Exception) {
            dominantColor = Color(0xFF241F23)
            mutedColor = Color(0xFF151517)
            accentColor = Color(0xFFBEB4AA)
        }
    }

    val animatedDominantColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "palette_dominant"
    )
    val animatedMutedColor by animateColorAsState(
        targetValue = mutedColor,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "palette_muted"
    )
    val backgroundTopColor = lerp(
        animatedDominantColor.copy(alpha = 0.92f),
        animatedDominantColor,
        pageProgress
    )
    val backgroundBottomColor = lerp(
        Color(0xFF09090B),
        animatedMutedColor,
        pageProgress
    )

    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDraggingToDismiss by remember { mutableStateOf(false) }
    var isDragDismissalRunning by remember { mutableStateOf(false) }
    var dismissalStartOffsetY by remember { mutableFloatStateOf(0f) }
    var dismissalPathLinearFactor by remember { mutableFloatStateOf(0.3f) }
    val density = LocalDensity.current
    val dismissThreshold = with(density) { 200.dp.toPx() }
    val dismissVelocityThreshold = with(density) { 1100.dp.toPx() }
    val maximumTrackedDismissVelocity = with(density) { 3600.dp.toPx() }
    val layerOffsetPx = with(density) { 22.dp.toPx() }

    val draggableState = rememberDraggableState { delta ->
        dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
    }

    val settledDragOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = if (isDraggingToDismiss) {
            snap()
        } else {
            spring(dampingRatio = 0.82f, stiffness = 520f)
        },
        label = "drag_offset_settle"
    )
    val visibleDragOffsetY = if (isDragDismissalRunning) {
        // 保持手指释放时的位置，再与外层关闭进度一起回到迷你播放器，避免先闪回全屏。
        val travelProgress = parabolicVerticalProgress(
            linearProgress = 1f - clampedPresentationProgress,
            initialVelocityFactor = dismissalPathLinearFactor
        )
        dismissalStartOffsetY * (1f - travelProgress)
    } else if (isDraggingToDismiss) {
        dragOffsetY
    } else {
        settledDragOffsetY
    }
    val dragProgress = (visibleDragOffsetY / dismissThreshold).coerceIn(0f, 1f)
    val animatedScale = 1f - (dragProgress * 0.08f)
    val animatedCorner by animateDpAsState(
        targetValue = if (visibleDragOffsetY > 0.5f) 26.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "corner"
    )
    val presentationCornerPercent = ((1f - clampedPresentationProgress) * 50f)
        .roundToInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, visibleDragOffsetY.roundToInt()) }
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(RoundedCornerShape(animatedCorner))
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                enabled = pageProgress <= 0.001f &&
                    clampedPresentationProgress >= 0.999f &&
                    !isDragDismissalRunning,
                onDragStarted = {
                    isDraggingToDismiss = true
                },
                onDragStopped = { velocity ->
                    isDraggingToDismiss = false
                    val dismissByDistance = dragOffsetY > dismissThreshold
                    val dismissByVelocity = velocity > dismissVelocityThreshold
                    if (dismissByDistance || dismissByVelocity) {
                        dismissalStartOffsetY = dragOffsetY
                        val normalizedVelocity = (
                            velocity.coerceAtLeast(0f) / maximumTrackedDismissVelocity
                            ).coerceIn(0f, 1f)
                        dismissalPathLinearFactor = 0.18f + (0.54f * normalizedVelocity)
                        isDragDismissalRunning = true
                        onClose()
                    } else {
                        // target 归零后由 settledDragOffsetY 负责弹簧回弹。
                        dragOffsetY = 0f
                    }
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val hasSourceBar = sourceBarBounds.width > 0f &&
                        sourceBarBounds.height > 0f && size.width > 0f && size.height > 0f
                    val startScaleX = if (hasSourceBar) {
                        (sourceBarBounds.width / size.width).coerceIn(0.82f, 0.96f)
                    } else {
                        0.9f
                    }
                    val startScaleY = if (hasSourceBar) {
                        (sourceBarBounds.height / size.height).coerceIn(0.04f, 0.12f)
                    } else {
                        0.08f
                    }
                    val startTranslationY = if (hasSourceBar) {
                        sourceBarBounds.top
                    } else {
                        size.height * 0.82f
                    }

                    scaleX = startScaleX + ((1f - startScaleX) * clampedPresentationProgress)
                    scaleY = startScaleY + ((1f - startScaleY) * clampedPresentationProgress)
                    translationY = startTranslationY * (1f - clampedPresentationProgress)
                    alpha = (clampedPresentationProgress / 0.18f).coerceIn(0f, 1f)
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                }
                .clip(RoundedCornerShape(percent = presentationCornerPercent))
                .background(Color.Black)
        ) {
            AppleMusicBackdrop(
                coverUrl = coverUrl,
                topColor = backgroundTopColor,
                bottomColor = backgroundBottomColor,
                lyricsProgress = pageProgress
            )
        }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = clampedPresentationProgress >= 0.999f,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> AlbumModeContent(
                    coverUrl = coverUrl,
                    songName = songName,
                    artistName = artistName,
                    isPlaying = isPlaying,
                    currentPositionMs = playbackState.currentPositionMs,
                    durationMs = playbackState.durationMs,
                    dominantColor = accentColor,
                    shuffleEnabled = playbackState.shuffleEnabled,
                    repeatMode = playbackState.repeatMode,
                    volume = playbackState.volume,
                    swipeProgress = pageProgress,
                    presentationProgress = clampedPresentationProgress,
                    contentRevealProgress = contentRevealProgress,
                    sourceArtworkBounds = sourceArtworkBounds,
                    allowArtworkBoundsUpdate = !isDraggingToDismiss &&
                        !isDragDismissalRunning &&
                        visibleDragOffsetY < 0.5f,
                    artworkPathLinearFactor = dismissalPathLinearFactor,
                    onPlayPause = { viewModel.onPlayPause() },
                    onNext = { viewModel.onNext() },
                    onPrevious = { viewModel.onPrevious() },
                    onSeekTo = { positionMs -> viewModel.seekTo(positionMs) },
                    onShuffleClick = viewModel::onShuffleClick,
                    onRepeatClick = viewModel::onRepeatClick,
                    onVolumeChange = viewModel::setVolume,
                    onDeviceClick = { openAudioOutputPanel(context) },
                    onQueueClick = { showQueueSheet = true },
                    onMoreClick = { showMoreSheet = true },
                    onLyricsClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    },
                    onClose = onClose,
                    modifier = Modifier.graphicsLayer {
                        alpha = 1f - (pageProgress * 0.18f)
                        translationY = pageProgress * layerOffsetPx
                    }
                )

                else -> LyricsModeContent(
                    coverUrl = coverUrl,
                    songName = songName,
                    artistName = artistName,
                    lyrics = flamingoData,
                    currentPositionMs = playbackState.currentPositionMs,
                    durationMs = playbackState.durationMs,
                    isPlaying = isPlaying,
                    onSeek = { positionMs -> viewModel.seekTo(positionMs) },
                    onPlayPause = { viewModel.onPlayPause() },
                    onNext = { viewModel.onNext() },
                    onPrevious = { viewModel.onPrevious() },
                    onDismissLyrics = {
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                    },
                    modifier = Modifier.graphicsLayer {
                        alpha = 0.82f + (pageProgress * 0.18f)
                        translationY = (1f - pageProgress) * layerOffsetPx
                    }
                )
            }
        }
    }

    if (showMoreSheet) {
        TrackActionsSheet(
            track = track,
            isFavorite = isCurrentTrackFavorite,
            onDismiss = { showMoreSheet = false },
            onFavoriteClick = viewModel::toggleCurrentTrackFavorite,
            onShareClick = {
                track?.let { shareTrack(context, it) }
                showMoreSheet = false
            },
            onQueueClick = {
                showMoreSheet = false
                showQueueSheet = true
            }
        )
    }

    if (showQueueSheet) {
        PlaybackQueueSheet(
            tracks = playbackState.queueTracks,
            currentIndex = playbackState.currentQueueIndex,
            onDismiss = { showQueueSheet = false },
            onTrackClick = { index ->
                viewModel.onQueueTrackClick(index)
                showQueueSheet = false
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TrackActionsSheet(
    track: Track?,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit,
    onQueueClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        contentColor = Color.White,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 12.dp)
                    .size(width = 38.dp, height = 5.dp)
                    .background(Color.White.copy(alpha = 0.28f), RoundedCornerShape(50))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track?.coverUrl ?: track?.album?.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = track?.title ?: "未在播放",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track?.artists?.joinToString(", ") { it.name }.orEmpty(),
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

            SheetActionButton(
                icon = {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) Color(0xFFFF375F) else Color.White
                    )
                },
                text = if (isFavorite) "取消收藏" else "收藏",
                enabled = track != null,
                onClick = onFavoriteClick
            )
            SheetActionButton(
                icon = {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = Color.White)
                },
                text = "分享歌曲",
                enabled = track != null,
                onClick = onShareClick
            )
            SheetActionButton(
                icon = {
                    Icon(
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = Color.White
                    )
                },
                text = "查看播放队列",
                onClick = onQueueClick
            )
        }
    }
}

@Composable
private fun SheetActionButton(
    icon: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    AppleMusicIconButton(
        onClick = onClick,
        enabled = enabled,
        pressedScale = 0.975f,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(14.dp))
            Text(text = text, color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlaybackQueueSheet(
    tracks: List<Track>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onTrackClick: (Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 18.dp)
        ) {
            Text(
                text = "接下来播放",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            if (tracks.isEmpty()) {
                Text(
                    text = "播放队列为空",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 28.dp),
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 15.sp
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 520.dp)) {
                    itemsIndexed(
                        items = tracks,
                        key = { index, item -> "${item.id}_$index" }
                    ) { index, item ->
                        AppleMusicIconButton(
                            onClick = { onTrackClick(index) },
                            pressedScale = 0.975f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(66.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = item.coverUrl ?: item.album.coverUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.08f)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = if (index == currentIndex) Color(0xFFFF375F) else Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.artists.joinToString(", ") { it.name },
                                        color = Color.White.copy(alpha = 0.48f),
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (index == currentIndex) {
                                    Icon(
                                        Icons.Filled.GraphicEq,
                                        contentDescription = "正在播放",
                                        tint = Color(0xFFFF375F),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun shareTrack(context: Context, track: Track) {
    val artist = track.artists.joinToString(", ") { it.name }
    val text = buildString {
        append("我正在听《${track.title}》")
        if (artist.isNotBlank()) append(" — $artist")
        append("（专辑：${track.album.title}）")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, track.title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "分享歌曲"))
}

private fun openAudioOutputPanel(context: Context) {
    val systemSwitcherShown = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        runCatching {
            MediaRouter2.getInstance(context).showSystemOutputSwitcher()
        }.getOrDefault(false)
    } else {
        false
    }
    if (!systemSwitcherShown) {
        context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
    }
}

@Composable
private fun AppleMusicBackdrop(
    coverUrl: String?,
    topColor: Color,
    bottomColor: Color,
    lyricsProgress: Float
) {
    val supportBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val artworkAlpha = 0.42f + (lyricsProgress * 0.12f)
    val artworkScale = 1.34f + (lyricsProgress * 0.12f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        topColor,
                        bottomColor.copy(alpha = 0.96f),
                        Color.Black
                    )
                )
            )
    ) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = artworkAlpha
                        scaleX = artworkScale
                        scaleY = artworkScale
                    }
                    .then(
                        if (supportBlur) {
                            Modifier.blur(
                                radius = 50.dp,
                                edgeTreatment = BlurredEdgeTreatment.Rectangle
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.28f - lyricsProgress * 0.08f),
                            Color.Black.copy(alpha = 0.18f - lyricsProgress * 0.08f),
                            Color.Black.copy(alpha = 0.62f - lyricsProgress * 0.16f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.20f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.16f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun AlbumModeContent(
    coverUrl: String?,
    songName: String,
    artistName: String,
    isPlaying: Boolean,
    currentPositionMs: Int,
    durationMs: Int,
    dominantColor: Color,
    shuffleEnabled: Boolean,
    repeatMode: QueueRepeatMode,
    volume: Float,
    swipeProgress: Float,
    presentationProgress: Float,
    contentRevealProgress: Float,
    sourceArtworkBounds: Rect,
    allowArtworkBoundsUpdate: Boolean,
    artworkPathLinearFactor: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onDeviceClick: () -> Unit,
    onQueueClick: () -> Unit,
    onMoreClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val coverSize = minOf(maxWidth - 56.dp, maxHeight * 0.43f).coerceIn(220.dp, 372.dp)
        val topGap = if (maxHeight < 720.dp) 18.dp else 34.dp
        val titleGap = if (maxHeight < 720.dp) 22.dp else 30.dp

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.graphicsLayer { alpha = contentRevealProgress }
            ) {
                NowPlayingTopBar(
                    onClose = onClose,
                    onMoreClick = onMoreClick
                )
            }

            Spacer(modifier = Modifier.height(topGap))

            AlbumArtwork(
                coverUrl = coverUrl,
                isPlaying = isPlaying,
                swipeProgress = swipeProgress,
                presentationProgress = presentationProgress,
                sourceArtworkBounds = sourceArtworkBounds,
                allowBoundsUpdate = allowArtworkBoundsUpdate,
                pathLinearFactor = artworkPathLinearFactor,
                modifier = Modifier.size(coverSize)
            )

            Spacer(modifier = Modifier.height(titleGap))

            TrackTitleBlock(
                songName = songName,
                artistName = artistName,
                onMoreClick = onMoreClick,
                modifier = Modifier
                    .padding(horizontal = 30.dp)
                    .graphicsLayer { alpha = contentRevealProgress }
            )

            Spacer(modifier = Modifier.weight(1f))

            PlayerControls(
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                isPlaying = isPlaying,
                dominantColor = dominantColor,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                volume = volume,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeekTo = onSeekTo,
                onShuffleClick = onShuffleClick,
                onRepeatClick = onRepeatClick,
                onVolumeChange = onVolumeChange,
                onDeviceClick = onDeviceClick,
                onQueueClick = onQueueClick,
                onLyricsClick = onLyricsClick,
                lyricsActive = false,
                modifier = Modifier.graphicsLayer { alpha = contentRevealProgress }
            )
        }
    }
}

@Composable
private fun NowPlayingTopBar(
    onClose: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppleMusicIconButton(
            onClick = onClose,
            modifier = Modifier
                .size(42.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "收起",
                tint = Color.White.copy(alpha = 0.86f),
                modifier = Modifier.size(27.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.34f))
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = "现在播放",
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
        }

        AppleMusicIconButton(
            onClick = onMoreClick,
            modifier = Modifier
                .size(42.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.MoreHoriz,
                contentDescription = "更多",
                tint = Color.White.copy(alpha = 0.86f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun AlbumArtwork(
    coverUrl: String?,
    isPlaying: Boolean,
    swipeProgress: Float,
    presentationProgress: Float,
    sourceArtworkBounds: Rect,
    allowBoundsUpdate: Boolean,
    pathLinearFactor: Float,
    modifier: Modifier = Modifier
) {
    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.982f,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessLow
        ),
        label = "artwork_breathing"
    )
    val swipeScale = 1f - (swipeProgress.coerceIn(0f, 1f) * 0.12f)
    val targetScale = artworkScale * swipeScale
    val transitionProgress = presentationProgress.coerceIn(0f, 1f)
    var targetArtworkBounds by remember { mutableStateOf(Rect.Zero) }
    val hasSharedBounds = sourceArtworkBounds.width > 0f &&
        sourceArtworkBounds.height > 0f &&
        targetArtworkBounds.width > 0f &&
        targetArtworkBounds.height > 0f
    val sourceScale = if (hasSharedBounds) {
        (sourceArtworkBounds.width / targetArtworkBounds.width).coerceIn(0.05f, 1f)
    } else {
        0.12f
    }
    val sourceTranslationX = if (hasSharedBounds) {
        sourceArtworkBounds.center.x - targetArtworkBounds.center.x
    } else {
        0f
    }
    val sourceTranslationY = if (hasSharedBounds) {
        sourceArtworkBounds.center.y - targetArtworkBounds.center.y
    } else {
        280f
    }
    val sharedScale = sourceScale + ((targetScale - sourceScale) * transitionProgress)
    val travelProgress = 1f - transitionProgress
    val sharedTranslationX = sourceTranslationX * travelProgress
    val sharedTranslationY = sourceTranslationY * parabolicVerticalProgress(
        linearProgress = travelProgress,
        initialVelocityFactor = pathLinearFactor
    )
    val artworkCorner = 8.dp + (10.dp * transitionProgress)
    val artworkShape = RoundedCornerShape(artworkCorner)

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                if (allowBoundsUpdate) {
                    targetArtworkBounds = coordinates.boundsInRoot()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = sharedScale
                    scaleY = sharedScale
                    translationX = sharedTranslationX
                    translationY = sharedTranslationY
                    alpha = if (hasSharedBounds || transitionProgress >= 0.999f) 1f else 0f
                }
                .shadow(
                    elevation = 34.dp * transitionProgress,
                    shape = artworkShape,
                    ambientColor = Color.Black.copy(alpha = 0.56f * transitionProgress),
                    spotColor = Color.Black.copy(alpha = 0.42f * transitionProgress)
                )
                .clip(artworkShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(
                    width = 0.8.dp,
                    color = Color.White.copy(alpha = 0.14f * transitionProgress),
                    shape = artworkShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = "专辑封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.34f),
                    modifier = Modifier.size(72.dp)
                )
            }
        }
    }
}

@Composable
private fun TrackTitleBlock(
    songName: String,
    artistName: String,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = songName,
                color = Color.White,
                fontSize = 23.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = artistName,
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(18.dp))

        AppleMusicIconButton(
            onClick = onMoreClick,
            modifier = Modifier
                .size(38.dp)
                .background(Color.White.copy(alpha = 0.10f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.MoreHoriz,
                contentDescription = "更多",
                tint = Color.White.copy(alpha = 0.80f),
                modifier = Modifier.size(23.dp)
            )
        }
    }
}

@Composable
private fun LyricsModeContent(
    coverUrl: String?,
    songName: String,
    artistName: String,
    lyrics: FlamingoLyricData,
    currentPositionMs: Int,
    durationMs: Int,
    isPlaying: Boolean,
    onSeek: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onDismissLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val mainTextSize = if (maxWidth < 380.dp) 27 else 29
        val topPadding = if (maxHeight < 720.dp) 78.dp else 92.dp
        val bottomPadding = if (maxHeight < 720.dp) 116.dp else 132.dp

        FlamingoLyricView(
            lyrics = lyrics.lyrics,
            sideFlags = lyrics.sideFlags,
            currentTimeMs = { currentPositionMs },
            onSeek = onSeek,
            isPlaying = isPlaying,
            translationEnabled = true,
            blurEnabled = true,
            uiConfig = LyricUIConfig(
                noLrcText = "暂无歌词",
                blankHeight = 58,
                mainTextSize = mainTextSize,
                subTextSize = 13,
                mainTextBasicColor = 0xFFFFFFFF,
                subTextBasicColor = 0xFFD6D6D6,
                fontWeight = FontWeight.ExtraBold,
                lineBalance = true,
                letterSpacing = 0f,
                activeTextScale = 1.12f,
                inactiveTextScale = 0.88f,
                inactiveTextAlpha = 0.24f,
                activeGlowColor = 0xA6FFFFFF,
                activeGlowRadius = 22f
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 10.dp,
                    top = topPadding,
                    end = 10.dp,
                    bottom = bottomPadding
                ),
            onEmptyAreaClick = onDismissLyrics
        )

        LyricsHeader(
            coverUrl = coverUrl,
            songName = songName,
            artistName = artistName,
            onDismissLyrics = onDismissLyrics,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        LyricsBottomControls(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            onSeek = onSeek,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun LyricsHeader(
    coverUrl: String?,
    songName: String,
    artistName: String,
    onDismissLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(180)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismissLyrics() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.10f)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.40f),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = songName,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artistName,
                    color = Color.White.copy(alpha = 0.52f),
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AppleMusicIconButton(
                onClick = onDismissLyrics,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "返回专辑页",
                    tint = Color.White.copy(alpha = 0.74f),
                    modifier = Modifier.size(25.dp)
                )
            }
        }
    }
}

@Composable
private fun LyricsBottomControls(
    currentPositionMs: Int,
    durationMs: Int,
    isPlaying: Boolean,
    onSeek: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 14.dp)
    ) {
        LyricsProgressBar(
            progress = if (durationMs > 0) {
                currentPositionMs.toFloat() / durationMs.toFloat()
            } else {
                0f
            },
            durationMs = durationMs,
            currentPositionMs = currentPositionMs,
            onSeek = onSeek
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppleMusicIconButton(
                onClick = onPrevious,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "上一曲",
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.width(28.dp))

            AppleMusicIconButton(
                onClick = onPlayPause,
                pressedScale = 0.86f,
                modifier = Modifier
                    .size(58.dp)
                    .background(Color.White.copy(alpha = 0.16f), CircleShape)
            ) {
                AnimatedPlayPauseIcon(
                    isPlaying = isPlaying,
                    size = 38.dp
                )
            }

            Spacer(modifier = Modifier.width(28.dp))

            AppleMusicIconButton(
                onClick = onNext,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "下一曲",
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}

@Composable
private fun LyricsProgressBar(
    progress: Float,
    durationMs: Int,
    currentPositionMs: Int,
    onSeek: (Int) -> Unit
) {
    val safeProgress = progress.coerceIn(0f, 1f)

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        if (durationMs > 0) {
                            onSeek(((offset.x / size.width).coerceIn(0f, 1f) * durationMs).toInt())
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.20f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(safeProgress)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.84f))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatPlayerTime(currentPositionMs),
                color = Color.White.copy(alpha = 0.46f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Start
            )
            Text(
                text = "-${formatPlayerTime((durationMs - currentPositionMs).coerceAtLeast(0))}",
                color = Color.White.copy(alpha = 0.46f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End
            )
        }
    }
}

private fun formatPlayerTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/**
 * 纵向采用 y(t) = at + (1-a)t²：初速度由松手速度决定且全程单调，随后自然加速。
 * 横向保持线性后，封面在二维空间中的轨迹就是稳定、可复现的抛物线。
 */
private fun parabolicVerticalProgress(
    linearProgress: Float,
    initialVelocityFactor: Float = 0.3f
): Float {
    val t = linearProgress.coerceIn(0f, 1f)
    val linearFactor = initialVelocityFactor.coerceIn(0.05f, 0.9f)
    return (linearFactor * t) + ((1f - linearFactor) * t * t)
}
