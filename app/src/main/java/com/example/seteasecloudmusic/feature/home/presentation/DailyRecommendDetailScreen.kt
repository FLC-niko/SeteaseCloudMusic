package com.example.seteasecloudmusic.feature.home.presentation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.ui.components.LocalGlassBackdrop
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.mine.presentation.PlaylistDetailScreen
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun DailyRecommendDetailRoute(
    tracks: List<Track>,
    title: String = "每日推荐",
    coverUrl: String? = null,
    onClose: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val fallbackBackdrop = rememberLayerBackdrop { drawContent() }
    val backdrop = LocalGlassBackdrop.current ?: fallbackBackdrop
    DailyRecommendDetailScreen(
        tracks = tracks,
        title = title,
        coverUrl = coverUrl,
        onClose = onClose,
        onTrackClick = { track -> viewModel.onTrackClick(track, tracks) },
        onPlayAll = { viewModel.playAll(tracks, 0) },
        onShufflePlay = { viewModel.playAll(tracks.shuffled(), 0) },
        backdrop = backdrop
    )
}

@Composable
fun DailyRecommendDetailScreen(
    tracks: List<Track>,
    title: String = "每日推荐",
    onClose: () -> Unit,
    onTrackClick: (Track) -> Unit,
    onPlayAll: () -> Unit = { if (tracks.isNotEmpty()) onTrackClick(tracks.first()) },
    onShufflePlay: () -> Unit = { if (tracks.isNotEmpty()) onTrackClick(tracks.shuffled().first()) },
    coverUrl: String? = null,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    val fallbackBackdrop = rememberLayerBackdrop { drawContent() }
    val glassBackdrop = backdrop ?: LocalGlassBackdrop.current ?: fallbackBackdrop
    val playlistDetail = remember(tracks, title, coverUrl) {
        PlaylistDetail(
            id = when (title) {
                "每日推荐" -> -1001L
                "雷达歌单" -> -1002L
                "心动模式" -> -1003L
                "私人 DJ" -> -1004L
                "相似歌曲" -> -1005L
                "相似艺人" -> -1006L
                else -> title.hashCode().toLong()
            },
            name = title,
            coverUrl = coverUrl ?: tracks.firstOrNull()?.coverUrl,
            description = when (title) {
                "每日推荐" -> "今天，听点喜欢的"
                "雷达歌单" -> "反复聆听你爱的歌"
                "心动模式" -> "红心歌曲和相似推荐"
                "私人 DJ" -> "你的全天候音乐助理"
                "相似歌曲" -> "从你喜欢的歌听起"
                "相似艺人" -> "常听歌手风格深度探索"
                else -> "为你精选 · 共 ${tracks.size} 首歌曲"
            },
            trackCount = tracks.size,
            playCount = 0L,
            creatorName = "云音乐",
            tracks = tracks
        )
    }

    PlaylistDetailScreen(
        detail = playlistDetail,
        backdrop = glassBackdrop,
        onClose = onClose,
        onPlayTrack = onTrackClick,
        onPlayAll = onPlayAll,
        onShufflePlay = onShufflePlay,
        modifier = modifier,
        listState = listState
    )
}
