package com.example.seteasecloudmusic.feature.player.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import com.example.seteasecloudmusic.core.player.PlaybackState
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.core.settings.PlayerSettingsManager
import com.example.seteasecloudmusic.feature.player.data.LyricResponse
import com.example.seteasecloudmusic.feature.player.domain.usecase.GetLyricUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 播放器 ViewModel：
 * 1) 对 UI 暴露只读播放状态
 * 2) 转发播放控制命令
 * 3) 统一处理 connect 生命周期入口
 * 4) 获取和管理歌词数据
 * 5) 持有播放器风格设置管理器
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    val controller: MusicPlayerController,
    private val getLyricUseCase: GetLyricUseCase,
    val playerSettingsManager: PlayerSettingsManager
) : ViewModel() {

    // 直接复用 Controller 内部的状态流
    val playbackState: StateFlow<PlaybackState> = controller.playbackState

    private val _lyricState = MutableStateFlow<Result<LyricResponse>?>(null)
    val lyricState: StateFlow<Result<LyricResponse>?> = _lyricState.asStateFlow()

    private var currentLyricTrackId: Long? = null

    init {
        viewModelScope.launch {
            controller.playbackState.collect { state ->
                val trackId = state.currentTrack?.id
                if (trackId != null && trackId != currentLyricTrackId) {
                    currentLyricTrackId = trackId
                    fetchLyric(trackId)
                }
            }
        }
    }

    suspend fun getLyricDataDirectly(songId: Long): String? {
        val res = getLyricUseCase(songId)
        val data = res.getOrNull()
        val yrcLyric = data?.yrc?.lyric
        val lrcLyric = data?.lrc?.lyric
        return yrcLyric.takeIf { !it.isNullOrBlank() } ?: lrcLyric.takeIf { !it.isNullOrBlank() }
    }

    private var lyricJob: Job? = null

    private fun fetchLyric(songId: Long) {
        lyricJob?.cancel()
        _lyricState.value = null
        lyricJob = viewModelScope.launch {
            delay(100L) // 快速连续切歌防抖，避免高频发包与 UI 线程重绘卡顿
            val res = getLyricUseCase(songId)
            _lyricState.value = res
        }
    }

    /** 建立与 MusicService 的连接 */
    fun connect() {
        controller.connect()
    }

    val currentAudioQuality: StateFlow<com.example.seteasecloudmusic.core.settings.OnlineAudioQuality> = playerSettingsManager.audioQuality

    fun selectAudioQuality(quality: com.example.seteasecloudmusic.core.settings.OnlineAudioQuality) {
        playerSettingsManager.setAudioQuality(quality)
        controller.reloadCurrentTrackWithQuality()
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

    fun togglePlaybackMode() {
        controller.togglePlaybackMode()
    }

    fun setPlaybackMode(mode: com.example.seteasecloudmusic.core.player.PlaybackMode) {
        controller.setPlaybackMode(mode)
    }
}