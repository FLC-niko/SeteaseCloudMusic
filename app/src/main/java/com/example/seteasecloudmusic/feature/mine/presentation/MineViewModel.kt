package com.example.seteasecloudmusic.feature.mine.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import com.example.seteasecloudmusic.feature.auth.domain.model.AuthSession
import com.example.seteasecloudmusic.feature.auth.domain.repository.AuthRepository
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.mine.domain.model.UserPlaylist
import com.example.seteasecloudmusic.feature.mine.domain.usecase.GetPlaylistDetailUseCase
import com.example.seteasecloudmusic.feature.mine.domain.usecase.GetUserPlaylistsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MinePlaylistTab(val title: String) {
    CREATED("创建的歌单"),
    FAVORITED("收藏的歌单")
}

data class MineUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val authSession: AuthSession? = null,
    val likedPlaylist: UserPlaylist? = null,
    val createdPlaylists: List<UserPlaylist> = emptyList(),
    val favoritedPlaylists: List<UserPlaylist> = emptyList(),
    val selectedTab: MinePlaylistTab = MinePlaylistTab.CREATED,
    val activePlaylistDetail: PlaylistDetail? = null,
    val isLoadingDetail: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class MineViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getUserPlaylistsUseCase: GetUserPlaylistsUseCase,
    private val getPlaylistDetailUseCase: GetPlaylistDetailUseCase,
    private val musicPlayerController: MusicPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(MineUiState())
    val uiState: StateFlow<MineUiState> = _uiState.asStateFlow()

    // 内存高速缓存：二次点击秒开（0ms 延迟）
    private val playlistDetailCache = mutableMapOf<Long, PlaylistDetail>()

    init {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { session ->
                _uiState.update { it.copy(authSession = session) }
                if (session?.isLoggedIn == true && session.userId != null) {
                    loadUserPlaylists(session.userId)
                } else {
                    _uiState.update {
                        it.copy(
                            likedPlaylist = null,
                            createdPlaylists = emptyList(),
                            favoritedPlaylists = emptyList(),
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun refresh() {
        val session = _uiState.value.authSession
        if (session?.isLoggedIn == true && session.userId != null) {
            viewModelScope.launch {
                _uiState.update { it.copy(isRefreshing = true) }
                loadUserPlaylists(session.userId)
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private suspend fun loadUserPlaylists(userId: Long) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        val result = getUserPlaylistsUseCase(userId)
        result.fold(
            onSuccess = { group ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        likedPlaylist = group.likedPlaylist,
                        createdPlaylists = group.createdPlaylists,
                        favoritedPlaylists = group.favoritedPlaylists,
                        errorMessage = null
                    )
                }
            },
            onFailure = { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "加载歌单失败"
                    )
                }
            }
        )
    }

    fun selectTab(tab: MinePlaylistTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    /**
     * 极速打开歌单（0ms 秒级弹出，不等待网络返回）
     */
    fun openPlaylist(playlist: UserPlaylist) {
        // 1. 若有缓存，立即以完整缓存数据秒开；若无缓存，先立即以基础元信息弹窗展示封面与骨架
        val cached = playlistDetailCache[playlist.id]
        val instantPreview = cached ?: PlaylistDetail(
            id = playlist.id,
            name = playlist.name,
            coverUrl = playlist.coverUrl,
            description = playlist.description,
            trackCount = playlist.trackCount,
            playCount = playlist.playCount,
            creatorName = playlist.creatorName,
            tracks = emptyList()
        )

        // 立即弹出界面（0ms 响应）
        _uiState.update {
            it.copy(
                activePlaylistDetail = instantPreview,
                isLoadingDetail = (cached == null),
                errorMessage = null
            )
        }

        // 2. 异步请求/刷新完整曲目列表
        viewModelScope.launch {
            val result = getPlaylistDetailUseCase(playlist.id)
            result.fold(
                onSuccess = { detail ->
                    playlistDetailCache[playlist.id] = detail
                    // 仅当用户仍在当前歌单时平滑更新
                    _uiState.update { current ->
                        if (current.activePlaylistDetail?.id == playlist.id) {
                            current.copy(
                                activePlaylistDetail = detail,
                                isLoadingDetail = false
                            )
                        } else {
                            current
                        }
                    }
                },
                onFailure = { err ->
                    _uiState.update { current ->
                        if (current.activePlaylistDetail?.id == playlist.id) {
                            current.copy(
                                isLoadingDetail = false,
                                errorMessage = err.message ?: "加载歌单曲目失败"
                            )
                        } else {
                            current
                        }
                    }
                }
            )
        }
    }

    fun closePlaylistDetail() {
        _uiState.update { it.copy(activePlaylistDetail = null) }
    }

    fun playTrack(track: Track, trackList: List<Track>) {
        val index = trackList.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        musicPlayerController.replaceQueueAndPlay(trackList, index)
    }

    fun playAll(trackList: List<Track>) {
        if (trackList.isNotEmpty()) {
            musicPlayerController.replaceQueueAndPlay(trackList, 0)
        }
    }
}
