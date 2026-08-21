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
                    // 1. 0ms 瞬间加载本地持久化歌单缓存，绝不让用户在「我的」界面等待白屏转圈
                    val cachedGroup = getUserPlaylistsUseCase.getCached(session.userId)
                    if (cachedGroup != null) {
                        _uiState.update {
                            it.copy(
                                likedPlaylist = cachedGroup.likedPlaylist,
                                createdPlaylists = cachedGroup.createdPlaylists,
                                favoritedPlaylists = cachedGroup.favoritedPlaylists,
                                isLoading = false
                            )
                        }
                    }
                    // 2. 静默拉取最新歌单更新
                    loadUserPlaylists(session.userId, silent = (cachedGroup != null))
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
                loadUserPlaylists(session.userId, silent = false)
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private suspend fun loadUserPlaylists(userId: Long, silent: Boolean = false) {
        if (!silent) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        }
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
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        errorMessage = if (current.createdPlaylists.isEmpty() && current.likedPlaylist == null) {
                            err.message ?: "加载歌单失败"
                        } else null
                    )
                }
            }
        )
    }

    fun selectTab(tab: MinePlaylistTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    /**
     * 极速打开歌单（0ms 秒级弹出，结合内存与首屏持久化缓存）
     */
    fun openPlaylist(playlist: UserPlaylist) {
        // 1. 优先从内存取，其次从轻量磁盘首屏缓存取（前20首歌曲+封面秒显），最后兜底基础元信息
        val cached = playlistDetailCache[playlist.id]
            ?: getPlaylistDetailUseCase.getCachedPreview(playlist.id)

        val instantPreview = cached?.let {
            if (playlist.trackCount > 0 && it.trackCount != playlist.trackCount) {
                it.copy(trackCount = playlist.trackCount)
            } else {
                it
            }
        } ?: PlaylistDetail(
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
                isLoadingDetail = (instantPreview.tracks.isEmpty()),
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
                                errorMessage = if (current.activePlaylistDetail?.tracks.isNullOrEmpty()) {
                                    err.message ?: "加载歌单曲目失败"
                                } else null
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
