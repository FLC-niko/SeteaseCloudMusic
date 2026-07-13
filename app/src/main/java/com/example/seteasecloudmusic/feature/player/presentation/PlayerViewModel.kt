package com.example.seteasecloudmusic.feature.player.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import com.example.seteasecloudmusic.core.player.PlaybackState
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.feature.player.domain.GetLyricsUseCase
import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val controller: MusicPlayerController,
    private val getLyricsUseCase: GetLyricsUseCase
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = controller.playbackState

    private val _lyricsState = MutableStateFlow<LyricsUiState>(LyricsUiState.Idle)
    val lyricsState: StateFlow<LyricsUiState> = _lyricsState.asStateFlow()

    val currentPositionMs: StateFlow<Int> = controller.playbackState
        .map { it.currentPositionMs }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    val activeLineIndex: StateFlow<Int> = combine(_lyricsState, currentPositionMs) { state, pos ->
        if (state !is LyricsUiState.Success) return@combine -1
        state.lyrics.lines.indexOfLast { it.startTime <= pos }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    private var loadLyricsJob: Job? = null

    fun loadLyrics(songId: Long) {
        loadLyricsJob?.cancel()
        loadLyricsJob = viewModelScope.launch {
            _lyricsState.value = LyricsUiState.Loading
            getLyricsUseCase(songId)
                .onSuccess { _lyricsState.value = LyricsUiState.Success(it) }
                .onFailure { _lyricsState.value = LyricsUiState.Error(it.message) }
        }
    }

    fun clearLyrics() {
        _lyricsState.value = LyricsUiState.Idle
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

    fun onPrevious() {
        controller.playPrevious()
    }

    fun seekTo(positionMs: Int) {
        controller.seekTo(positionMs)
    }
}

sealed class LyricsUiState {
    object Idle : LyricsUiState()
    object Loading : LyricsUiState()
    data class Success(val lyrics: ParsedLyrics) : LyricsUiState()
    data class Error(val message: String?) : LyricsUiState()
}
