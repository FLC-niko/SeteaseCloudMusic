package com.example.seteasecloudmusic.feature.player.presentation

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
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

    // ── Palette 主色提取 ──
    val context = LocalContext.current
    var dominantColor by remember { mutableStateOf(Color(0xFF1A1A1A)) }
    var mutedColor by remember { mutableStateOf(Color(0xFF2A2A2A)) }

    LaunchedEffect(coverUrl) {
        if (coverUrl == null) return@LaunchedEffect
        try {
            val request = ImageRequest.Builder(context)
                .data(coverUrl)
                .size(200, 200)
                .allowHardware(false)
                .build()
            val loader = coil.ImageLoader(context)
            val result = loader.execute(request)
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
            if (bitmap != null) {
                val palette = Palette.from(bitmap).generate()
                palette.getDarkMutedColor(0xFF1A1A1A.toInt()).let { c ->
                    dominantColor = Color(c)
                }
                palette.getMutedColor(0xFF2A2A2A.toInt()).let { c ->
                    mutedColor = Color(c)
                }
            }
        } catch (_: Exception) { }
    }

    // ── 下滑关闭手势 ──
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val dismissThreshold = with(density) { 200.dp.toPx() }

    val draggableState = rememberDraggableState { delta ->
        dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
    }

    val dragProgress = (dragOffsetY / dismissThreshold).coerceIn(0f, 1f)
    val animatedScale = 1f - (dragProgress * 0.1f)
    val animatedCorner = animateDpAsState(
        targetValue = if (dragOffsetY > 0f) 24.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "corner"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragOffsetY.roundToInt()) }
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(RoundedCornerShape(animatedCorner.value))
            .background(Color.Black)
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                onDragStopped = {
                    if (dragOffsetY > dismissThreshold) {
                        onClose()
                    }
                    dragOffsetY = 0f
                }
            )
    ) {
        // ── 模糊背景层 ──
        BlurredCoverBackground(coverUrl = coverUrl, dominantColor = dominantColor, mutedColor = mutedColor)

        // ── 内容层 ──
        Box(modifier = Modifier.fillMaxSize()) {
            // 歌词视图层（始终存在，通过 alpha 控制可见性）
            val lyricsAlpha by animateFloatAsState(
                targetValue = if (showLyrics) 1f else 0f,
                animationSpec = tween(400),
                label = "lyrics_alpha"
            )

            if (lyricsAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = lyricsAlpha }
                ) {
                    FlamingoLyricView(
                        lyrics = flamingoData.lyrics,
                        sideFlags = flamingoData.sideFlags,
                        currentTimeMs = { currentPosition },
                        onSeek = { positionMs -> viewModel.seekTo(positionMs) },
                        translationEnabled = true,
                        blurEnabled = true,
                        uiConfig = LyricUIConfig(
                            mainTextSize = 32,
                            subTextSize = 16,
                            mainTextBasicColor = 0xFFF2F2F2,
                            subTextBasicColor = 0xFF919191,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = -0.02f
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 80.dp),
                        onEmptyAreaClick = { showLyrics = false }
                    )
                }
            }

            // 专辑视图层
            val albumAlpha by animateFloatAsState(
                targetValue = if (showLyrics) 0f else 1f,
                animationSpec = tween(400),
                label = "album_alpha"
            )

            if (albumAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = albumAlpha }
                ) {
                    AlbumModeContent(
                        coverUrl = coverUrl,
                        songName = songName,
                        artistName = artistName,
                        isPlaying = isPlaying,
                        currentPositionMs = currentPosition,
                        durationMs = playbackState.durationMs,
                        dominantColor = dominantColor,
                        onPlayPause = { viewModel.onPlayPause() },
                        onNext = { viewModel.onNext() },
                        onPrevious = { viewModel.onPrevious() },
                        onSeekTo = { positionMs -> viewModel.seekTo(positionMs) },
                        onLyricsClick = { showLyrics = true },
                        onClose = onClose
                    )
                }
            }

            // ── Lyric 模式：迷你播放栏（叠在顶部）──
            AnimatedVisibility(
                visible = showLyrics,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                MiniPlayingBar(
                    coverUrl = coverUrl,
                    songName = songName,
                    artistName = artistName,
                    showLyrics = showLyrics,
                    coverScale = animateFloatAsState(
                        targetValue = if (showLyrics) 1f else 0f,
                        animationSpec = tween(500),
                        label = "cover_scale"
                    ).value,
                    onBarClick = { showLyrics = false },
                    modifier = Modifier
                )
            }
        }
    }
}

// ── 模糊封面背景 ──
@Composable
private fun BlurredCoverBackground(
    coverUrl: String?,
    dominantColor: Color,
    mutedColor: Color
) {
    val supportBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(modifier = Modifier.fillMaxSize()) {
        if (coverUrl != null && supportBlur) {
            // 模糊封面图
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = 1.6f; scaleY = 1.6f }
                    .blur(radius = 80.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded),
                contentScale = ContentScale.Crop
            )
            // 暗色叠加层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )
        } else {
            // API < 31 回退：渐变背景
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                dominantColor,
                                mutedColor.copy(alpha = 0.8f),
                                Color(0xFF0A0A0A)
                            )
                        )
                    )
            )
        }
    }
}

// ── Album 模式内容 ──
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
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── 顶部 Pill Handle + 下拉箭头 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pill handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(Color.White.copy(alpha = 0.35f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onClose() }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── 专辑封面 ──
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = "专辑封面",
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .aspectRatio(1f)
                    .shadow(
                        elevation = 32.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color.Black.copy(alpha = 0.6f),
                        spotColor = Color.Black.copy(alpha = 0.4f)
                    )
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── 歌曲信息 ──
        Text(
            text = songName,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = (-0.3).sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = artistName,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = (-0.2).sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // ── 播放控制 ──
        PlayerControls(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            dominantColor = dominantColor,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            onSeekTo = onSeekTo,
            onLyricsClick = onLyricsClick
        )
    }
}

// ── 迷你播放栏（歌词模式顶部）──
@Composable
private fun MiniPlayingBar(
    coverUrl: String?,
    songName: String,
    artistName: String,
    showLyrics: Boolean,
    coverScale: Float,
    onBarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onBarClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 迷你封面（带缩放动画，模拟封面缩到角落的效果）
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer {
                        scaleX = 0.5f + (coverScale * 0.5f)
                        scaleY = 0.5f + (coverScale * 0.5f)
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2A2A2A)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 歌名 + 歌手
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = songName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = (-0.2).sp
            )
            Text(
                text = artistName,
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 收起箭头（向下箭头代表收起歌词回到专辑页）
        IconButton(
            onClick = onBarClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "收起歌词",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
