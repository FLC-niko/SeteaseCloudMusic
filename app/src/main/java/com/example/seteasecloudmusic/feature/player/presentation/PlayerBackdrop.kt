package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.seteasecloudmusic.core.model.Track
import com.kyant.backdrop.backdrops.LayerBackdrop

internal val LocalPlayerBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/** 窄屏、大屏与播放菜单共用云端原生播放器的封面流光采样。 */
@Composable
internal fun PlayerBackdrop(track: Track?, modifier: Modifier = Modifier, isPlaying: Boolean = false) {
    AppleMusicBackground(coverUrl = track?.coverUrl, isPlaying = isPlaying, modifier = modifier)
}
