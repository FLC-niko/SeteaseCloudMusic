package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun PlayerRoute(
    viewModel: PlayerViewModel,
    onClose: () -> Unit
) {
    val playbackState by viewModel.playbackState.collectAsState()

    NowPlayingScreen(
        playbackState = playbackState,
        onClose = onClose,
        onPlayPause = viewModel::onPlayPause,
        onNext = viewModel::onNext,
        onPrevious = viewModel::onPrevious,
        onSeekTo = viewModel::seekTo
    )
}