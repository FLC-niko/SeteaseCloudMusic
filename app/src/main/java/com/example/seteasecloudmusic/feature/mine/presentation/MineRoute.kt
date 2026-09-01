package com.example.seteasecloudmusic.feature.mine.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.Backdrop

@Composable
fun MineRoute(
    backdrop: Backdrop,
    topContentPadding: Dp,
    bottomContentPadding: Dp,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MineScreen(
        uiState = uiState,
        backdrop = backdrop,
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
        modifier = modifier
    )
}
