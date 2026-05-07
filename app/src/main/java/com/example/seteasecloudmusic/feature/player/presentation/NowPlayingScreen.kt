package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.seteasecloudmusic.core.player.PlaybackState
import com.example.seteasecloudmusic.feature.search.presentation.SearchViewModel
import com.example.seteasecloudmusic.feature.player.presentation.PlayerViewModel
import kotlinx.coroutines.flow.first

@Composable
fun NowPlayingScreen(
    playbackState: PlaybackState,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Int) -> Unit
) {
    val searchViewModel: SearchViewModel = hiltViewModel()
    val playerViewModel: PlayerViewModel = hiltViewModel()

    Box(modifier = Modifier.fillMaxSize()) {
        WebPlayerScreen(
            musicPlayerController = searchViewModel.musicPlayerController,
            ttmlProvider = { songId ->
                try {
                    playerViewModel.getLyricDataDirectly(songId.toLong())
                } catch (e: Exception) {
                    null
                }
            }
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "关闭"
            )
        }
    }
}