package com.example.seteasecloudmusic.feature.mine.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import com.example.seteasecloudmusic.core.local.LocalMusicRepository
import com.example.seteasecloudmusic.core.auth.AuthSession
import com.example.seteasecloudmusic.core.auth.AuthStateProvider
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.mine.domain.model.UserPlaylist
import com.example.seteasecloudmusic.feature.mine.domain.usecase.GetPlaylistDetailUseCase
import com.example.seteasecloudmusic.feature.mine.domain.usecase.GetUserPlaylistsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

const val LOCAL_PLAYLIST_ID = -9999L

enum class MinePlaylistTab(val title: String) {
    CREATED("创建歌单"),
    FAVORITED("收藏歌单"),
    LOCAL("本地音乐")
}

data class MineUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val authSession: AuthSession? = null,
    val likedPlaylist: UserPlaylist? = null,
    val createdPlaylists: List<UserPlaylist> = emptyList(),
    val favoritedPlaylists: List<UserPlaylist> = emptyList(),
    val localSongs: List<Track> = emptyList(),
    val isScanningLocal: Boolean = false,
    val localDirectoryPath: String? = null,
    val selectedTab: MinePlaylistTab = MinePlaylistTab.CREATED,
    val activePlaylistDetail: PlaylistDetail? = null,
    val isLoadingDetail: Boolean = false,
    val errorMessage: String? = null
) {
    val localPlaylist: UserPlaylist
        get() = UserPlaylist(
            id = LOCAL_PLAYLIST_ID,
            name = "本地音乐",
            coverUrl = localSongs.firstOrNull { !it.coverUrl.isNullOrBlank() }?.coverUrl,
            trackCount = localSongs.size,
            playCount = localSongs.size.toLong(),
            isLikedHero = false,
            creatorName = "本地媒体库",
            description = "设备存储音频 · 离线畅享高品质音乐"
        )
}

@HiltViewModel
class MineViewModel @Inject constructor(
    private val authStateProvider: AuthStateProvider,
    private val getUserPlaylistsUseCase: GetUserPlaylistsUseCase,
    private val getPlaylistDetailUseCase: GetPlaylistDetailUseCase,
    private val localMusicRepository: LocalMusicRepository,
    private val musicPlayerController: MusicPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(MineUiState())
    val uiState: StateFlow<MineUiState> = _uiState.asStateFlow()

    // 内存高速缓存：二次点击秒开（0ms 延迟）
    private val playlistDetailCache = mutableMapOf<Long, PlaylistDetail>()
    private var localScanJob: Job? = null
    private var localScanRequestId = 0L
    private var playlistRefreshJob: Job? = null
    private var playlistRequestId = 0L
    private var playlistDetailJob: Job? = null
    private var playlistDetailRequestId = 0L

    init {
        // 1. 监听账号登录状态与云端歌单
        viewModelScope.launch {
            authStateProvider.observeAuthState().collectLatest { session ->
                val previousSession = _uiState.value.authSession
                val userChanged = previousSession?.userId != session?.userId ||
                    previousSession?.isLoggedIn != session?.isLoggedIn
                val loadRequestId = ++playlistRequestId
                playlistRefreshJob?.cancel()

                if (userChanged) {
                    playlistDetailJob?.cancel()
                    playlistDetailJob = null
                    playlistDetailRequestId += 1L
                    playlistDetailCache.clear()
                }

                _uiState.update {
                    it.copy(
                        authSession = session,
                        likedPlaylist = if (userChanged) null else it.likedPlaylist,
                        createdPlaylists = if (userChanged) emptyList() else it.createdPlaylists,
                        favoritedPlaylists = if (userChanged) emptyList() else it.favoritedPlaylists,
                        activePlaylistDetail = if (userChanged) null else it.activePlaylistDetail,
                        isLoading = session?.isLoggedIn == true && session.userId != null,
                        isRefreshing = false,
                        isLoadingDetail = if (userChanged) false else it.isLoadingDetail,
                        errorMessage = if (userChanged) null else it.errorMessage
                    )
                }

                if (session?.isLoggedIn == true && session.userId != null) {
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
                    loadUserPlaylists(
                        userId = session.userId,
                        silent = cachedGroup != null,
                        requestId = loadRequestId
                    )
                } else {
                    _uiState.update {
                        it.copy(
                            likedPlaylist = null,
                            createdPlaylists = emptyList(),
                            favoritedPlaylists = emptyList(),
                            activePlaylistDetail = null,
                            isLoading = false,
                            isLoadingDetail = false
                        )
                    }
                }
            }
        }

        // 2. 监听本地音乐列表更新
        viewModelScope.launch {
            localMusicRepository.localTracksFlow.collect { tracks ->
                _uiState.update { it.copy(localSongs = tracks) }
                // 若当前正打开本地歌单详情，同步刷新曲目
                if (_uiState.value.activePlaylistDetail?.id == LOCAL_PLAYLIST_ID) {
                    _uiState.update { current ->
                        current.copy(
                            activePlaylistDetail = current.activePlaylistDetail?.copy(
                                trackCount = tracks.size,
                                tracks = tracks
                            )
                        )
                    }
                }
            }
        }

        // 3. 初始后台加载本地音乐目录及索引（供在线匹配与本地展示）
        localScanJob = viewModelScope.launch {
            val dirPath = localMusicRepository.getCustomDirectoryPath()
            _uiState.update { it.copy(localDirectoryPath = dirPath) }
            localMusicRepository.getLocalTracks(forceRefresh = false)
        }
    }

    fun refresh() {
        val session = _uiState.value.authSession
        if (session?.isLoggedIn == true && session.userId != null) {
            val requestId = ++playlistRequestId
            playlistRefreshJob?.cancel()
            playlistRefreshJob = viewModelScope.launch {
                _uiState.update { it.copy(isRefreshing = true) }
                try {
                    loadUserPlaylists(
                        userId = session.userId,
                        silent = false,
                        requestId = requestId
                    )
                } finally {
                    if (isActive && requestId == playlistRequestId) {
                        _uiState.update { it.copy(isRefreshing = false) }
                    }
                }
            }
        }
        if (_uiState.value.selectedTab == MinePlaylistTab.LOCAL) {
            scanLocalMusic()
        }
    }

    /**
     * 扫描本地音乐（指定目录或系统媒体库）
     */
    fun scanLocalMusic(customDirectory: String? = null) {
        localScanJob?.cancel()
        val requestId = ++localScanRequestId
        val dir = customDirectory ?: _uiState.value.localDirectoryPath
        localScanJob = viewModelScope.launch {
            _uiState.update { it.copy(isScanningLocal = true) }
            try {
                val songs = if (!dir.isNullOrBlank()) {
                    localMusicRepository.scanDirectory(dir)
                } else {
                    localMusicRepository.getLocalTracks(forceRefresh = true)
                }
                currentCoroutineContext().ensureActive()
                if (requestId != localScanRequestId) return@launch

                _uiState.update {
                    it.copy(
                        localSongs = songs,
                        localDirectoryPath = localMusicRepository.getCustomDirectoryPath(),
                        errorMessage = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (requestId == localScanRequestId) {
                    _uiState.update { it.copy(errorMessage = e.message ?: "扫描本地音乐失败") }
                }
            } finally {
                if (requestId == localScanRequestId && currentCoroutineContext().isActive) {
                    _uiState.update { it.copy(isScanningLocal = false) }
                }
            }
        }
    }

    /**
     * 更改自定义扫描目录并触发扫描
     */
    fun setLocalDirectoryAndScan(path: String) {
        localMusicRepository.setCustomDirectoryPath(path)
        _uiState.update { it.copy(localDirectoryPath = path) }
        scanLocalMusic(path)
    }

    private suspend fun loadUserPlaylists(
        userId: Long,
        silent: Boolean = false,
        requestId: Long
    ) {
        if (!currentCoroutineContext().isActive || requestId != playlistRequestId || !isCurrentLoggedInUser(userId)) {
            return
        }
        if (!silent) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        }
        val result = getUserPlaylistsUseCase(userId)
        if (!currentCoroutineContext().isActive || requestId != playlistRequestId || !isCurrentLoggedInUser(userId)) {
            return
        }
        result.fold(
            onSuccess = { group ->
                if (requestId != playlistRequestId || !isCurrentLoggedInUser(userId)) return@fold
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
                if (requestId != playlistRequestId || !isCurrentLoggedInUser(userId)) return@fold
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

    private fun isCurrentLoggedInUser(userId: Long): Boolean {
        val session = _uiState.value.authSession
        return session?.isLoggedIn == true && session.userId == userId
    }

    fun selectTab(tab: MinePlaylistTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (tab == MinePlaylistTab.LOCAL && _uiState.value.localSongs.isEmpty() && !_uiState.value.isScanningLocal) {
            scanLocalMusic()
        }
    }

    /**
     * 极速打开歌单（0ms 秒级弹出，统一在线歌单与本地歌单）
     */
    fun openPlaylist(playlist: UserPlaylist) {
        playlistDetailJob?.cancel()
        playlistDetailJob = null
        val requestId = ++playlistDetailRequestId

        // 1. 本地音乐歌单统一秒开逻辑
        if (playlist.id == LOCAL_PLAYLIST_ID) {
            val songs = _uiState.value.localSongs
            val localDetail = PlaylistDetail(
                id = LOCAL_PLAYLIST_ID,
                name = "本地音乐",
                coverUrl = songs.firstOrNull { !it.coverUrl.isNullOrBlank() }?.coverUrl,
                description = "设备中的本地音频文件 · 离线畅听",
                trackCount = songs.size,
                playCount = songs.size.toLong(),
                creatorName = "本地媒体库",
                tracks = songs
            )
            _uiState.update {
                it.copy(
                    activePlaylistDetail = localDetail,
                    isLoadingDetail = false,
                    errorMessage = null
                )
            }
            if (songs.isEmpty() && !_uiState.value.isScanningLocal) {
                scanLocalMusic()
            }
            return
        }

        // 2. 在线歌单逻辑
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

        // 异步请求/刷新完整曲目列表
        playlistDetailJob = viewModelScope.launch {
            val result = getPlaylistDetailUseCase(playlist.id)
            if (!isActive || requestId != playlistDetailRequestId) return@launch
            result.fold(
                onSuccess = { detail ->
                    if (requestId != playlistDetailRequestId) return@fold
                    playlistDetailCache[playlist.id] = detail
                    _uiState.update { current ->
                        if (requestId == playlistDetailRequestId && current.activePlaylistDetail?.id == playlist.id) {
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
                    if (requestId != playlistDetailRequestId) return@fold
                    _uiState.update { current ->
                        if (requestId == playlistDetailRequestId && current.activePlaylistDetail?.id == playlist.id) {
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
        playlistDetailJob?.cancel()
        playlistDetailJob = null
        playlistDetailRequestId += 1L
        _uiState.update {
            it.copy(
                activePlaylistDetail = null,
                isLoadingDetail = false
            )
        }
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
