package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.seteasecloudmusic.core.settings.PlayerStyle

@Composable
fun NowPlayingScreen(
    playerViewModel: PlayerViewModel,
    onClose: () -> Unit,
) {
    val playerStyle by playerViewModel.playerSettingsManager.playerStyle.collectAsStateWithLifecycle()

    when (playerStyle) {
        PlayerStyle.AMLL_WEB -> {
            Box(modifier = Modifier.fillMaxSize()) {
                WebPlayerScreen(
                    musicPlayerController = playerViewModel.controller,
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
                onClose = onClose
            )
        }
    }
}
