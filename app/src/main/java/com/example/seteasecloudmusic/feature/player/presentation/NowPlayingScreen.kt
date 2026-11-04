package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.example.seteasecloudmusic.core.settings.PlayerStyle
import com.example.seteasecloudmusic.core.ui.components.LocalGlassBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun NowPlayingScreen(
    playerViewModel: PlayerViewModel,
    onClose: () -> Unit,
    artworkAlpha: State<Float>,
    onArtworkBoundsChanged: (Rect) -> Unit,
) {
    PlayerSystemBars()
    val playerStyle by playerViewModel.playerSettingsManager.playerStyle.collectAsStateWithLifecycle()

    var showQueue by rememberSaveable { mutableStateOf(false) }
    val trackFlow = remember(playerViewModel) { playerViewModel.playbackState.map { it.currentTrack }.distinctUntilChanged() }
    val track by trackFlow.collectAsStateWithLifecycle(initialValue = playerViewModel.playbackState.value.currentTrack)
    val background = rememberLayerBackdrop { drawContent() }
    Box(Modifier.fillMaxSize()) {
    PlayerBackdrop(track, Modifier.layerBackdrop(background))
    // Menus opened above playback sample the album light, not the home page hidden underneath it.
    CompositionLocalProvider(LocalPlayerBackdrop provides background, LocalGlassBackdrop provides background) {
    when (playerStyle) {
        PlayerStyle.AMLL_WEB -> {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF141419)).systemBarsPadding()) {
                WebPlayerScreen(
                    musicPlayerController = playerViewModel.controller,
                    onQueueClick = { showQueue = true },
                    ttmlProvider = { songId ->
                        try {
                            playerViewModel.getLyricDataDirectly(songId.toLong())
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
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
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }
            }
        }
        PlayerStyle.NATIVE_COMPOSE -> {
            NativePlayerScreen(
                playerViewModel = playerViewModel,
                onClose = onClose,
                artworkAlpha = artworkAlpha,
                onArtworkBoundsChanged = onArtworkBoundsChanged
            )
        }
    }
    if (showQueue) {
        val state by playerViewModel.playbackState.collectAsStateWithLifecycle()
        PlaylistQueueSheet(state, playerViewModel::playQueueTrackAt, playerViewModel::removeQueueTrackAt,
            playerViewModel::clearQueue, playerViewModel::togglePlaybackMode, { showQueue = false })
    }
    }
    }
}
