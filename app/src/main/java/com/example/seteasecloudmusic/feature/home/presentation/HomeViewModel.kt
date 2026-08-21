package com.example.seteasecloudmusic.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import com.example.seteasecloudmusic.feature.home.domain.usecase.GetDailyRecommendSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val tracks: List<Track> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDailyRecommendSongsUseCase: GetDailyRecommendSongsUseCase,
    private val musicPlayerController: MusicPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var playbackJob: Job? = null

    init {
        // 1. 0ms 瞬间加载本地持久化推荐缓存，冷启动直接秒出封面与曲目，绝不转圈
        val cached = getDailyRecommendSongsUseCase.getCached()
        if (!cached.isNullOrEmpty()) {
            _uiState.update { it.copy(tracks = cached, isLoading = false) }
        }
        // 2. 后台静默拉取今日最新推荐
        refreshDailyRecommend(afresh = false, silent = !cached.isNullOrEmpty())
    }

    fun refreshDailyRecommend(afresh: Boolean = false, silent: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            val result = getDailyRecommendSongsUseCase(afresh)
            result.fold(
                onSuccess = { tracks ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            tracks = tracks,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update { state ->
                        // 若本地已有缓存，失败时不遮挡已有缓存内容
                        state.copy(
                            isLoading = false,
                            errorMessage = if (state.tracks.isEmpty()) (throwable.message ?: "获取每日推荐失败") else null
                        )
                    }
                }
            )
        }
    }

    fun onRetryClick() {
        refreshDailyRecommend(afresh = false, silent = false)
    }

    fun onRefreshClick() {
        refreshDailyRecommend(afresh = true, silent = false)
    }

    fun onTrackClick(track: Track) {
        val snapshotTracks = uiState.value.tracks.distinctBy { it.id }
        if (snapshotTracks.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "暂无可播放的每日推荐") }
            return
        }

        val clickedIndex = snapshotTracks.indexOfFirst { it.id == track.id }
        if (clickedIndex !in snapshotTracks.indices) {
            _uiState.update { it.copy(errorMessage = "未找到所选歌曲") }
            return
        }

        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            musicPlayerController.replaceQueueAndPlay(snapshotTracks, clickedIndex)
        }
    }
}
