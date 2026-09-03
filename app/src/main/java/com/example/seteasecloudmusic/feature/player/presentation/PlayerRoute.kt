package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.runtime.Composable
@Composable
fun PlayerRoute(
    viewModel: PlayerViewModel,
    onClose: () -> Unit
) {
    NowPlayingScreen(
        playerViewModel = viewModel,
        onClose = onClose
    )
}
