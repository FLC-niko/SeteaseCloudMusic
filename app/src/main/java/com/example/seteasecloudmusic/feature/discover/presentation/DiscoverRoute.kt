package com.example.seteasecloudmusic.feature.discover.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun DiscoverRoute(
    topContentPadding: Dp,
    bottomContentPadding: Dp = 180.dp,
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    DiscoverScreen(
        uiState = uiState,
        topContentPadding = topContentPadding,
        bottomContentPadding = bottomContentPadding,
        onRetryPersonalized = viewModel::refreshPersonalizedPlaylists,
        onRetryHotPlaylists = viewModel::refreshHotPlaylists,
        onRetryNewsongs = viewModel::refreshNewsongs,
        onRetryToplists = viewModel::refreshToplists
    )
}
