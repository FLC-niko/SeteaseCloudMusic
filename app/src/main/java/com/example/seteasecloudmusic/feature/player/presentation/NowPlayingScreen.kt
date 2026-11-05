package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.core.ui.components.LocalGlassBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp

@Composable
fun NowPlayingScreen(
    playerViewModel: PlayerViewModel,
    onClose: () -> Unit,
    artworkAlpha: State<Float>,
    onArtworkBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    enableLightStatusBarSwitch: Boolean = true,
    contentAlpha: State<Float>? = null,
    backgroundAlpha: State<Float>? = null,
    onArtworkCornerRadiusChanged: (Dp) -> Unit = {},
    onArtworkShadowChanged: (Dp) -> Unit = {},
    onArtworkTrackTopChanged: (Boolean) -> Unit = {},
    onGrabberDragStart: (() -> Unit)? = null,
    onGrabberDragDelta: ((Float) -> Unit)? = null,
    onGrabberDragEnd: ((Float) -> Unit)? = null
) {
    if (enableLightStatusBarSwitch) {
        PlayerSystemBars()
    }
    val state by playerViewModel.playbackState.collectAsStateWithLifecycle()
    val background = rememberLayerBackdrop { drawContent() }
    Box(modifier.fillMaxSize()) {
        PlayerBackdrop(state.currentTrack,
            Modifier.graphicsLayer { alpha = backgroundAlpha?.value ?: 1f }.layerBackdrop(background),
            state.status == PlayerStatus.PLAYING)
        CompositionLocalProvider(LocalPlayerBackdrop provides background, LocalGlassBackdrop provides background) {
    NativePlayerScreen(
        playerViewModel = playerViewModel,
        onClose = onClose,
        artworkAlpha = artworkAlpha,
        onArtworkBoundsChanged = onArtworkBoundsChanged,
        modifier = Modifier.fillMaxSize(),
        contentAlpha = contentAlpha,
        backgroundAlpha = backgroundAlpha,
        onArtworkCornerRadiusChanged = onArtworkCornerRadiusChanged,
        onArtworkShadowChanged = onArtworkShadowChanged,
        onArtworkTrackTopChanged = onArtworkTrackTopChanged,
        onGrabberDragStart = onGrabberDragStart,
        onGrabberDragDelta = onGrabberDragDelta,
        onGrabberDragEnd = onGrabberDragEnd
    )
        }
    }
}
