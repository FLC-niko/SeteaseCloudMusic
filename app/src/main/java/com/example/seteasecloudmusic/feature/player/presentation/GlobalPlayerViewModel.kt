package com.example.seteasecloudmusic.feature.player.presentation

import androidx.lifecycle.ViewModel
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.core.player.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class GlobalPlayerViewModel @Inject constructor(
    private val controller: MusicPlayerController
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = controller.playbackState

    init {
        controller.connect()
    }

    fun onPlayPause() {
        when (playbackState.value.status) {
            PlayerStatus.PLAYING -> controller.pause()
            PlayerStatus.PAUSED -> controller.resume()
            PlayerStatus.BUFFERING -> Unit
            PlayerStatus.IDLE,
            PlayerStatus.ENDED,
            PlayerStatus.ERROR -> controller.replayCurrent()
        }
    }

    fun onNext() {
        controller.playNext()
    }
}
