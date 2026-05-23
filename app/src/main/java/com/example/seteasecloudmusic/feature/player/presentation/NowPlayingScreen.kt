package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.feature.player.presentation.lyric.FlamingoLyricData
import com.example.seteasecloudmusic.feature.player.presentation.lyric.FlamingoLyricView
import com.example.seteasecloudmusic.feature.player.presentation.lyric.LyricDataAdapter
import com.example.seteasecloudmusic.feature.player.presentation.lyric.LyricUIConfig

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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
        AnimatedContent(
            targetState = showLyrics,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            label = "page_switch"
        ) { isLyricsMode ->
            if (isLyricsMode) {
                // ── Lyric 模式 ──
                Box(modifier = Modifier.fillMaxSize()) {
                    // 歌词（全屏）
                    FlamingoLyricView(
                        lyrics = flamingoData.lyrics,
                        sideFlags = flamingoData.sideFlags,
                        currentTimeMs = { currentPosition },
                        onSeek = { positionMs -> viewModel.seekTo(positionMs) },
                        translationEnabled = true,
                        blurEnabled = true,
                        uiConfig = LyricUIConfig(
                            mainTextSize = 34,
                            subTextSize = 16,
                            mainTextBasicColor = 0xFFF2F2F2,
                            subTextBasicColor = 0xFF919191,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        modifier = Modifier.fillMaxSize()
                    )
                    // 迷你播放栏（叠在顶部）
                    MiniPlayingBar(
                        coverUrl = coverUrl,
                        songName = songName,
                        artistName = artistName,
                        onClose = onClose,
                        onBarClick = { showLyrics = false },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            } else {
                // ── Album 模式 ──
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 顶部关闭按钮
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 12.dp, end = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "关闭",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // 专辑封面
                    if (coverUrl != null) {
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = "专辑封面",
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
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

                    Spacer(modifier = Modifier.height(24.dp))

                    // 歌曲信息
                    Text(
                        text = songName,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = artistName,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // 播放控制
                    PlayerControls(
                        currentPositionMs = currentPosition,
                        durationMs = playbackState.durationMs,
                        isPlaying = isPlaying,
                        dominantColor = Color(0xFF1A1A1A),
                        onPlayPause = { viewModel.onPlayPause() },
                        onNext = { viewModel.onNext() },
                        onPrevious = { viewModel.onPrevious() },
                        onSeekTo = { positionMs -> viewModel.seekTo(positionMs) },
                        onLyricsClick = { showLyrics = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayingBar(
    coverUrl: String?,
    songName: String,
    artistName: String,
    onClose: () -> Unit,
    onBarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 迷你封面
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2A2A2A)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
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
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artistName,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 关闭按钮
        IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "关闭",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
