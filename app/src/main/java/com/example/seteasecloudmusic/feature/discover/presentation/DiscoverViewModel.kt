package com.example.seteasecloudmusic.feature.discover.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seteasecloudmusic.core.common.toUserFriendlyMessage
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.discover.domain.model.DiscoverPlaylist
import com.example.seteasecloudmusic.feature.discover.domain.model.DiscoverToplist
import com.example.seteasecloudmusic.feature.discover.domain.usecase.GetDiscoverHotPlaylistsUseCase
import com.example.seteasecloudmusic.feature.discover.domain.usecase.GetDiscoverNewsongsUseCase
import com.example.seteasecloudmusic.feature.discover.domain.usecase.GetDiscoverPlaylistsUseCase
import com.example.seteasecloudmusic.feature.discover.domain.usecase.GetDiscoverToplistsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 发现页 UI 状态模型，分别维护四个内容分区的数据、加载态与错误信息。
 */
data class DiscoverUiState(
    val personalizedPlaylists: List<DiscoverPlaylist> = emptyList(),
    val hotPlaylists: List<DiscoverPlaylist> = emptyList(),
    val newsongs: List<Track> = emptyList(),
    val toplists: List<DiscoverToplist> = emptyList(),
    val isPersonalizedLoading: Boolean = false,
    val personalizedErrorMessage: String? = null,
    val isHotPlaylistsLoading: Boolean = false,
    val hotPlaylistsErrorMessage: String? = null,
    val isNewsongsLoading: Boolean = false,
    val newsongsErrorMessage: String? = null,
    val isToplistsLoading: Boolean = false,
    val toplistsErrorMessage: String? = null
)

/**
 * 发现页 ViewModel，并发拉取并独立管理推荐歌单、热门歌单、推荐新歌与榜单的刷新生命周期。
 */
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val getDiscoverPlaylistsUseCase: GetDiscoverPlaylistsUseCase,
    private val getDiscoverNewsongsUseCase: GetDiscoverNewsongsUseCase,
    private val getDiscoverToplistsUseCase: GetDiscoverToplistsUseCase,
    private val getDiscoverHotPlaylistsUseCase: GetDiscoverHotPlaylistsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState = _uiState.asStateFlow()

    private var personalizedJob: Job? = null
    private var hotPlaylistsJob: Job? = null
    private var newsongsJob: Job? = null
    private var toplistsJob: Job? = null

    init {
        refreshAll()
    }

    fun refreshAll() {
        refreshPersonalizedPlaylists()
        refreshHotPlaylists()
        refreshNewsongs()
        refreshToplists()
    }

    fun refreshPersonalizedPlaylists() {
        personalizedJob?.cancel()
        personalizedJob = viewModelScope.launch {
            _uiState.update { it.copy(isPersonalizedLoading = true, personalizedErrorMessage = null) }
            getDiscoverPlaylistsUseCase(limit = 8) // 推荐歌单默认拉取条数
                .onSuccess { playlists ->
                    _uiState.update {
                        it.copy(
                            personalizedPlaylists = playlists,
                            isPersonalizedLoading = false,
                            personalizedErrorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isPersonalizedLoading = false,
                            personalizedErrorMessage = throwable.toUserFriendlyMessage("获取推荐歌单")
                        )
                    }
                }
        }
    }

    fun refreshHotPlaylists() {
        hotPlaylistsJob?.cancel()
        hotPlaylistsJob = viewModelScope.launch {
            _uiState.update { it.copy(isHotPlaylistsLoading = true, hotPlaylistsErrorMessage = null) }
            getDiscoverHotPlaylistsUseCase(limit = 10, offset = 0) // 热门歌单首屏拉取条数与偏移量
                .onSuccess { playlists ->
                    _uiState.update {
                        it.copy(
                            hotPlaylists = playlists,
                            isHotPlaylistsLoading = false,
                            hotPlaylistsErrorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isHotPlaylistsLoading = false,
                            hotPlaylistsErrorMessage = throwable.toUserFriendlyMessage("获取热门歌单")
                        )
                    }
                }
        }
    }

    fun refreshNewsongs() {
        newsongsJob?.cancel()
        newsongsJob = viewModelScope.launch {
            _uiState.update { it.copy(isNewsongsLoading = true, newsongsErrorMessage = null) }
            getDiscoverNewsongsUseCase(limit = 10) // 推荐新歌默认拉取条数
                .onSuccess { tracks ->
                    _uiState.update {
                        it.copy(
                            newsongs = tracks,
                            isNewsongsLoading = false,
                            newsongsErrorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isNewsongsLoading = false,
                            newsongsErrorMessage = throwable.toUserFriendlyMessage("获取新歌")
                        )
                    }
                }
        }
    }

    fun refreshToplists() {
        toplistsJob?.cancel()
        toplistsJob = viewModelScope.launch {
            _uiState.update { it.copy(isToplistsLoading = true, toplistsErrorMessage = null) }
            getDiscoverToplistsUseCase()
                .onSuccess { toplists ->
                    _uiState.update {
                        it.copy(
                            toplists = toplists,
                            isToplistsLoading = false,
                            toplistsErrorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isToplistsLoading = false,
                            toplistsErrorMessage = throwable.toUserFriendlyMessage("获取榜单")
                        )
                    }
                }
        }
    }
}
