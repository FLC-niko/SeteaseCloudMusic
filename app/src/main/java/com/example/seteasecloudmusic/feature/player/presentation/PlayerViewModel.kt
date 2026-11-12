package com.example.seteasecloudmusic.feature.player.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import com.example.seteasecloudmusic.core.player.PlaybackMode
import com.example.seteasecloudmusic.core.player.PlaybackState
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.core.settings.OnlineAudioQuality
import com.example.seteasecloudmusic.core.settings.PlayerSettingsManager
import com.example.seteasecloudmusic.feature.player.domain.GetLyricsUseCase
import com.example.seteasecloudmusic.feature.player.domain.model.LyricSource
import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 播放器 ViewModel：
 * 1) 对 UI 暴露只读播放状态
 * 2) 转发播放控制命令
 * 3) 统一处理 connect 生命周期入口
 * 4) 优先通过 AMLL TTML 词库获取逐字歌词，自动降级至网易云 YRC / LRC + 翻译
 * 5) 管理在线播放音质切换与曲目喜欢状态
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    val controller: MusicPlayerController,
    private val getLyricsUseCase: GetLyricsUseCase,
    val playerSettingsManager: PlayerSettingsManager
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = controller.playbackState

    private val _parsedLyrics = MutableStateFlow(EmptyLyrics)
    val parsedLyrics: StateFlow<ParsedLyrics> = _parsedLyrics.asStateFlow()

    private val _isLyricsLoading = MutableStateFlow(false)
    val isLyricsLoading: StateFlow<Boolean> = _isLyricsLoading.asStateFlow()

    val likedTrackIds: StateFlow<Set<Long>> = com.example.seteasecloudmusic.core.player.FavoriteTracksManager.likedTrackIds

    private var currentLyricTrackId: Long? = null
    private var lyricRequestId = 0L
    private var lyricJob: Job? = null

    init {
        controller.connect()
        viewModelScope.launch {
            controller.playbackState.collect { state ->
                val trackId = state.currentTrack?.id
                if (trackId != currentLyricTrackId) {
                    currentLyricTrackId = trackId
                    if (trackId == null || trackId <= 0L) {
                        lyricRequestId += 1L
                        lyricJob?.cancel()
                        lyricJob = null
                        _isLyricsLoading.value = false
                        _parsedLyrics.value = EmptyLyrics
                    } else {
                        fetchLyrics(trackId)
                    }
                }
            }
        }
    }

    private fun fetchLyrics(songId: Long) {
        lyricJob?.cancel()
        val requestId = ++lyricRequestId
        _parsedLyrics.value = EmptyLyrics
        _isLyricsLoading.value = true
        lyricJob = viewModelScope.launch {
            delay(60L)
            val result = getLyricsUseCase(songId)
            if (isActive && requestId == lyricRequestId && controller.playbackState.value.currentTrack?.id == songId) {
                _parsedLyrics.value = result.getOrElse { EmptyLyrics }
                _isLyricsLoading.value = false
            }
        }
    }

    fun connect() {
        controller.connect()
    }

    val currentAudioQuality: StateFlow<OnlineAudioQuality> = playerSettingsManager.audioQuality

    fun selectAudioQuality(quality: OnlineAudioQuality) {
        playerSettingsManager.setAudioQuality(quality)
        controller.reloadCurrentTrackWithQuality()
    }

    fun toggleTrackLiked(trackId: Long) {
        com.example.seteasecloudmusic.core.player.FavoriteTracksManager.toggleLiked(trackId)
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

    fun onPrevious(forcePreviousSong: Boolean = false) {
        controller.playPrevious(forcePreviousSong)
    }

    fun seekTo(positionMs: Int) {
        controller.seekTo(positionMs)
    }

    fun togglePlaybackMode() {
        controller.togglePlaybackMode()
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        controller.setPlaybackMode(mode)
    }

    fun playQueueTrackAt(index: Int) {
        controller.playQueueIndexAt(index)
    }

    fun removeQueueTrackAt(index: Int) {
        controller.removeQueueTrackAt(index)
    }

    fun clearQueue() {
        controller.clearQueue()
    }

    companion object {
        val EmptyLyrics = ParsedLyrics(emptyList(), hasWordTiming = false, source = LyricSource.NONE)
    }
}
