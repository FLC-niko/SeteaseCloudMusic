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
import com.example.seteasecloudmusic.feature.mine.presentation.PlaylistDetailStyle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/** 每日推荐及首页聚合歌单详情页路由入口，绑定 HomeViewModel 播放动作。 */
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

/**
 * 首页聚合推荐歌单详情页，复用 [PlaylistDetailScreen] 并按标题选择动态海报墙或全封面样式。
 */
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
            // 使用负值虚拟 ID 区分首页聚合歌单与真实歌单
            id = when (title) {
                "每日推荐" -> -1001L
                "雷达歌单" -> -1002L
                "心动模式" -> -1003L
                "新歌雷达" -> -1006L
                else -> title.hashCode().toLong()
            },
            name = title,
            coverUrl = coverUrl ?: tracks.firstOrNull()?.coverUrl,
            description = when (title) {
                "每日推荐" -> "今天，听点喜欢的"
                "雷达歌单" -> "反复聆听你爱的歌"
                "心动模式" -> "红心歌曲和相似推荐"
                "新歌雷达" -> "发现最新发行的好歌"
                else -> "为你精选 · 共 ${tracks.size} 首歌曲"
            },
            trackCount = tracks.size,
            playCount = 0L,
            creatorName = "云音乐",
            tracks = tracks
        )
    }

    val defaultStyle = remember(title) {
        if (title == "每日推荐") PlaylistDetailStyle.DYNAMIC_POSTER_WALL
        else PlaylistDetailStyle.FULL_COVER
    }

    PlaylistDetailScreen(
        detail = playlistDetail,
        backdrop = glassBackdrop,
        onClose = onClose,
        onPlayTrack = onTrackClick,
        onPlayAll = onPlayAll,
        onShufflePlay = onShufflePlay,
        modifier = modifier,
        listState = listState,
        defaultStyle = defaultStyle
    )
}
