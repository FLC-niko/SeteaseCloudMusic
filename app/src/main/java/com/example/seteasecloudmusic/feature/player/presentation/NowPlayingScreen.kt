package com.example.seteasecloudmusic.feature.player.presentation

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
    NativePlayerScreen(
        playerViewModel = playerViewModel,
        onClose = onClose,
        artworkAlpha = artworkAlpha,
        onArtworkBoundsChanged = onArtworkBoundsChanged,
        modifier = modifier,
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
