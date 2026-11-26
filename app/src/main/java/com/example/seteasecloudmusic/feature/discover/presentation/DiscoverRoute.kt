package com.example.seteasecloudmusic.feature.discover.presentation

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/**
 * 发现页路由入口，订阅 [DiscoverViewModel] 状态并向 [DiscoverScreen] 注入刷新回调。
 */
@Composable
fun DiscoverRoute(
    topContentPadding: Dp,
    bottomContentPadding: Dp = 180.dp, // 底部默认留白，用于避让悬浮播放栏与导航栏
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DiscoverScreen(
        uiState = uiState,
        topContentPadding = topContentPadding,
        bottomContentPadding = bottomContentPadding,
        onRetryPersonalized = viewModel::refreshPersonalizedPlaylists,
        onRetryHotPlaylists = viewModel::refreshHotPlaylists,
        onRetryNewsongs = viewModel::refreshNewsongs,
        onRetryToplists = viewModel::refreshToplists,
        onRefreshAll = viewModel::refreshAll
    )
}
