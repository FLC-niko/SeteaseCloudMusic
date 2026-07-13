package com.example.seteasecloudmusic.feature.player.presentation

import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.feature.player.presentation.lyric.FlamingoLyricData
import com.example.seteasecloudmusic.feature.player.presentation.lyric.FlamingoLyricView
import com.example.seteasecloudmusic.feature.player.presentation.lyric.LyricDataAdapter
import com.example.seteasecloudmusic.feature.player.presentation.lyric.LyricUIConfig
import kotlin.math.roundToInt

@Composable
fun NowPlayingScreen(
    onClose: () -> Unit
) {
    val viewModel: PlayerViewModel = hiltViewModel()
    val playbackState by viewModel.playbackState.collectAsState()
    val lyricsState by viewModel.lyricsState.collectAsState()
    val currentPosition by viewModel.currentPositionMs.collectAsState()

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

    var showLyrics by remember { mutableStateOf(false) }

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

    val animatedTopColor by animateColorAsState(
        targetValue = if (showLyrics) dominantColor else dominantColor.copy(alpha = 0.92f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "background_top"
    )
    val animatedBottomColor by animateColorAsState(
        targetValue = if (showLyrics) mutedColor else Color(0xFF09090B),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "background_bottom"
    )

    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val dismissThreshold = with(density) { 200.dp.toPx() }
    val layerOffsetPx = with(density) { 22.dp.toPx() }

    val draggableState = rememberDraggableState { delta ->
        dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
    }

    val dragProgress = (dragOffsetY / dismissThreshold).coerceIn(0f, 1f)
    val animatedScale = 1f - (dragProgress * 0.08f)
    val animatedCorner by animateDpAsState(
        targetValue = if (dragOffsetY > 0f) 26.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "corner"
    )

    val albumAlpha by animateFloatAsState(
        targetValue = if (showLyrics) 0f else 1f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "album_alpha"
    )
    val lyricsAlpha by animateFloatAsState(
        targetValue = if (showLyrics) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "lyrics_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragOffsetY.roundToInt()) }
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(RoundedCornerShape(animatedCorner))
            .background(Color.Black)
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                enabled = !showLyrics,
                onDragStopped = {
                    if (dragOffsetY > dismissThreshold) {
                        onClose()
                    }
                    dragOffsetY = 0f
                }
            )
    ) {
        AppleMusicBackdrop(
            coverUrl = coverUrl,
            topColor = animatedTopColor,
            bottomColor = animatedBottomColor,
            lyricsMode = showLyrics
        )

        if (albumAlpha > 0.01f) {
            AlbumModeContent(
                coverUrl = coverUrl,
                songName = songName,
                artistName = artistName,
                isPlaying = isPlaying,
                currentPositionMs = currentPosition,
                durationMs = playbackState.durationMs,
                dominantColor = accentColor,
                onPlayPause = { viewModel.onPlayPause() },
                onNext = { viewModel.onNext() },
                onPrevious = { viewModel.onPrevious() },
                onSeekTo = { positionMs -> viewModel.seekTo(positionMs) },
                onLyricsClick = { showLyrics = true },
                onClose = onClose,
                modifier = Modifier.graphicsLayer {
                    alpha = albumAlpha
                    val scale = 0.985f + (albumAlpha * 0.015f)
                    scaleX = scale
                    scaleY = scale
                    translationY = (1f - albumAlpha) * layerOffsetPx
                }
            )
        }

        if (lyricsAlpha > 0.01f) {
            LyricsModeContent(
                coverUrl = coverUrl,
                songName = songName,
                artistName = artistName,
                lyrics = flamingoData,
                currentPositionMs = currentPosition,
                durationMs = playbackState.durationMs,
                isPlaying = isPlaying,
                onSeek = { positionMs -> viewModel.seekTo(positionMs) },
                onPlayPause = { viewModel.onPlayPause() },
                onNext = { viewModel.onNext() },
                onPrevious = { viewModel.onPrevious() },
                onDismissLyrics = { showLyrics = false },
                modifier = Modifier.graphicsLayer {
                    alpha = lyricsAlpha
                    translationY = (1f - lyricsAlpha) * layerOffsetPx
                }
            )
        }
    }
}

@Composable
private fun AppleMusicBackdrop(
    coverUrl: String?,
    topColor: Color,
    bottomColor: Color,
    lyricsMode: Boolean
) {
    val supportBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val artworkAlpha by animateFloatAsState(
        targetValue = if (lyricsMode) 0.54f else 0.42f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "backdrop_artwork_alpha"
    )

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
                        scaleX = if (lyricsMode) 1.46f else 1.34f
                        scaleY = if (lyricsMode) 1.46f else 1.34f
                    }
                    .then(
                        if (supportBlur) {
                            Modifier.blur(
                                radius = if (lyricsMode) 78.dp else 68.dp,
                                edgeTreatment = BlurredEdgeTreatment.Unbounded
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
                            Color.Black.copy(alpha = if (lyricsMode) 0.20f else 0.28f),
                            Color.Black.copy(alpha = if (lyricsMode) 0.10f else 0.18f),
                            Color.Black.copy(alpha = if (lyricsMode) 0.46f else 0.62f)
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
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Int) -> Unit,
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
            NowPlayingTopBar(onClose = onClose)

            Spacer(modifier = Modifier.height(topGap))

            AlbumArtwork(
                coverUrl = coverUrl,
                isPlaying = isPlaying,
                modifier = Modifier.size(coverSize)
            )

            Spacer(modifier = Modifier.height(titleGap))

            TrackTitleBlock(
                songName = songName,
                artistName = artistName,
                modifier = Modifier.padding(horizontal = 30.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            PlayerControls(
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                isPlaying = isPlaying,
                dominantColor = dominantColor,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeekTo = onSeekTo,
                onLyricsClick = onLyricsClick,
                lyricsActive = false
            )
        }
    }
}

@Composable
private fun NowPlayingTopBar(
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
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

        IconButton(
            onClick = {},
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

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = artworkScale
                scaleY = artworkScale
            }
            .shadow(
                elevation = 34.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = 0.56f),
                spotColor = Color.Black.copy(alpha = 0.42f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(
                width = 0.8.dp,
                color = Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(18.dp)
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

@Composable
private fun TrackTitleBlock(
    songName: String,
    artistName: String,
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

        IconButton(
            onClick = {},
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

            IconButton(
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
            IconButton(
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

            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(58.dp)
                    .background(Color.White.copy(alpha = 0.16f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.width(28.dp))

            IconButton(
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
