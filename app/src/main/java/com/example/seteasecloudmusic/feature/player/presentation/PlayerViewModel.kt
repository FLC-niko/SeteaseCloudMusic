package com.example.seteasecloudmusic.feature.player.presentation

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import com.example.seteasecloudmusic.core.player.PlaybackState
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.feature.player.domain.GetLyricsUseCase
import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val getLyricsUseCase: GetLyricsUseCase,
    @ApplicationContext context: Context
) : ViewModel() {

    private val favoritesPreferences = context.getSharedPreferences(
        FAVORITES_PREFERENCES,
        Context.MODE_PRIVATE
    )
    private val _favoriteTrackIds = MutableStateFlow(
        favoritesPreferences.getStringSet(FAVORITE_TRACK_IDS, emptySet()).orEmpty().toSet()
    )

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

    val isCurrentTrackFavorite: StateFlow<Boolean> = combine(
        playbackState,
        _favoriteTrackIds
    ) { state, favoriteIds ->
        state.currentTrack?.id?.toString() in favoriteIds
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

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

    fun onQueueTrackClick(index: Int) {
        controller.playQueueItem(index)
    }

    fun onShuffleClick() {
        controller.toggleShuffle()
    }

    fun onRepeatClick() {
        controller.cycleRepeatMode()
    }

    fun setVolume(volume: Float) {
        controller.setVolume(volume)
    }

    fun toggleCurrentTrackFavorite() {
        val trackId = playbackState.value.currentTrack?.id?.toString() ?: return
        val updated = _favoriteTrackIds.value.toMutableSet().apply {
            if (!add(trackId)) remove(trackId)
        }.toSet()
        _favoriteTrackIds.value = updated
        favoritesPreferences.edit { putStringSet(FAVORITE_TRACK_IDS, updated) }
    }

    fun seekTo(positionMs: Int) {
        controller.seekTo(positionMs)
    }

    private companion object {
        const val FAVORITES_PREFERENCES = "player_favorites"
        const val FAVORITE_TRACK_IDS = "favorite_track_ids"
    }
}

sealed class LyricsUiState {
    object Idle : LyricsUiState()
    object Loading : LyricsUiState()
    data class Success(val lyrics: ParsedLyrics) : LyricsUiState()
    data class Error(val message: String?) : LyricsUiState()
}
