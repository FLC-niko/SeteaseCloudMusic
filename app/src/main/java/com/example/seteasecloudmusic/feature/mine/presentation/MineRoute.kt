package com.example.seteasecloudmusic.feature.mine.presentation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MineRoute(
    topContentPadding: Dp,
    bottomContentPadding: Dp,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MineViewModel = hiltViewModel(),
    lazyListState: LazyListState = rememberLazyListState(),
    isForeground: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MineScreen(
        uiState = uiState,
        topContentPadding = topContentPadding,
        bottomContentPadding = bottomContentPadding,
        onLoginClick = onLoginClick,
        onRefresh = viewModel::refresh,
        onTabSelected = viewModel::selectTab,
        onPlaylistClick = viewModel::openPlaylist,
        onCloseDetail = viewModel::closePlaylistDetail,
        onPlayTrack = viewModel::playTrack,
        onPlayAll = viewModel::playAll,
        onScanLocal = viewModel::scanLocalMusic,
        onSetLocalDirectory = viewModel::setLocalDirectoryAndScan,
        onTrackLikedChanged = viewModel::onTrackLikedChanged,
        onTrackAddedToPlaylist = viewModel::onTrackAddedToPlaylist,
        onTracksAddedToPlaylist = viewModel::onTracksAddedToPlaylist,
        onTracksReordered = viewModel::updateTrackOrder,
        onDeleteTracks = viewModel::removeTracksFromPlaylist,
        modifier = modifier,
        lazyListState = lazyListState,
        isForeground = isForeground
    )
}
