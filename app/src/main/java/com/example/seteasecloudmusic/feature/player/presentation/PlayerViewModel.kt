package com.example.seteasecloudmusic.feature.player.presentation

import androidx.lifecycle.ViewModel
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import com.example.seteasecloudmusic.core.player.PlaybackState
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.feature.player.domain.usecase.GetLyricUseCase
import com.example.seteasecloudmusic.feature.player.data.LyricResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.util.Log

/**
 * 播放器 ViewModel：
 * 1) 对 UI 暴露只读播放状态
 * 2) 转发播放控制命令
 * 3) 统一处理 connect/release 生命周期入口
 * 4) 获取和管理歌词数据
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val controller: MusicPlayerController,
    private val getLyricUseCase: GetLyricUseCase
) : ViewModel(){

    // 直接复用 Controller 内部的状态流，避免重复维护一份状态
    val playbackState: StateFlow<PlaybackState> = controller.playbackState

    private val _lyricState = MutableStateFlow<Result<LyricResponse>?>(null)
    val lyricState: StateFlow<Result<LyricResponse>?> = _lyricState.asStateFlow()

    init {
        // Observe track changes to fetch new lyrics
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

    private var currentLyricTrackId: Long? = null

    suspend fun getLyricDataDirectly(songId: Long): String? {
        val res = getLyricUseCase(songId)
        Log.d("PlayerViewModel", "getLyricDataDirectly: songId=$songId, result isSuccess=${res.isSuccess}")
        val data = res.getOrNull()
        if (res.isFailure) {
            Log.e("PlayerViewModel", "API Error: ", res.exceptionOrNull())
        }
        val yrcLyric = data?.yrc?.lyric
        val lrcLyric = data?.lrc?.lyric
        Log.d("PlayerViewModel", "yrc length=${yrcLyric?.length}, lrc length=${lrcLyric?.length}")
        return yrcLyric.takeIf { !it.isNullOrBlank() } ?: lrcLyric.takeIf { !it.isNullOrBlank() }
    }

    private fun fetchLyric(songId: Long) {
        viewModelScope.launch {
            _lyricState.value = getLyricUseCase(songId)
        }
    }

    /** 建立与 MusicService 的连接 */
    fun connect() {
        controller.connect()
    }

    fun onPlayPause() {
        when(playbackState.value.status) {
            PlayerStatus.PLAYING -> controller.pause()
            PlayerStatus.PAUSED -> controller.pause()
            PlayerStatus.BUFFERING -> Unit
            PlayerStatus.IDLE,
            PlayerStatus.ENDED,
            PlayerStatus.ERROR ->  controller.replayCurrent()
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

    override  fun onCleared() {
        // ViewModel 销毁时释放控制器，避免监听器和协程泄漏
        controller.release()
        super.onCleared()
    }
}