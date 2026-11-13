package com.example.seteasecloudmusic.feature.mine.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seteasecloudmusic.core.common.toUserFriendlyMessage
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import com.example.seteasecloudmusic.core.local.LocalMusicRepository
import com.example.seteasecloudmusic.core.auth.AuthSession
import com.example.seteasecloudmusic.core.auth.AuthStateProvider
import com.example.seteasecloudmusic.core.player.FavoriteTracksManager
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.mine.domain.model.UserPlaylist
import com.example.seteasecloudmusic.feature.mine.domain.repository.MineRepository
import com.example.seteasecloudmusic.feature.mine.domain.usecase.GetPlaylistDetailUseCase
import com.example.seteasecloudmusic.feature.mine.domain.usecase.GetUserPlaylistsUseCase
import com.example.seteasecloudmusic.core.network.NetworkMonitor
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
    private val mineRepository: MineRepository,
    private val getUserPlaylistsUseCase: GetUserPlaylistsUseCase,
    private val getPlaylistDetailUseCase: GetPlaylistDetailUseCase,
    private val localMusicRepository: LocalMusicRepository,
    private val musicPlayerController: MusicPlayerController,
    private val networkMonitor: NetworkMonitor
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
                    // 同步网易云云端已喜欢的音乐 ID 列表
                    viewModelScope.launch {
                        mineRepository.getLikelist(session.userId)
                    }
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

        // 4. 监听全局断网重试事件
        viewModelScope.launch {
            networkMonitor.retryTrigger.collect {
                val session = _uiState.value.authSession
                if (session?.isLoggedIn == true && session.userId != null) {
                    val reqId = ++playlistRequestId
                    loadUserPlaylists(session.userId, silent = false, requestId = reqId)
                }
            }
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
                    // 刷新时同步网易云云端喜欢的音乐 ID 列表
                    mineRepository.getLikelist(session.userId)
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
                    _uiState.update { it.copy(errorMessage = e.toUserFriendlyMessage("扫描本地音乐")) }
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
                            err.toUserFriendlyMessage("加载歌单")
                        } else null
                    )
                }
                // 仅当设备确实离线时才提示“网络连接已断开”。
                // 在线但歌单为空（新账号、服务端异常）时会走上面的 errorMessage 提示，不应误报为断网。
                if (!networkMonitor.checkOnlineStatus()) {
                    networkMonitor.requestOfflineDialog(
                        title = "网络连接已断开",
                        description = "歌单加载失败，请检查网络设置后重试。"
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
        val isLikedPlaylist = playlist.isLikedHero || playlist.name.contains("喜欢") || playlist.id == _uiState.value.likedPlaylist?.id

        val cached = playlistDetailCache[playlist.id]
            ?: getPlaylistDetailUseCase.getCachedPreview(playlist.id)

        val instantPreview = cached?.let {
            val validTracks = if (isLikedPlaylist) {
                it.tracks.filter { t -> FavoriteTracksManager.isLiked(t.id) }
            } else {
                it.tracks
            }
            val count = if (isLikedPlaylist) validTracks.size else (if (playlist.trackCount > 0) playlist.trackCount else it.trackCount)
            it.copy(tracks = validTracks, trackCount = count)
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
                    // 若当前打开的是“我喜欢的音乐”，以本地 FavoriteTracksManager 的喜欢状态为准强校验过滤，
                    // 彻底阻断任何服务端内存旧缓存中已取消喜欢的曲目被带回复活。
                    val finalTracks = if (isLikedPlaylist) {
                        detail.tracks.filter { t -> FavoriteTracksManager.isLiked(t.id) }
                    } else {
                        detail.tracks
                    }
                    val finalDetail = detail.copy(
                        tracks = finalTracks,
                        trackCount = if (isLikedPlaylist) finalTracks.size else detail.trackCount
                    )
                    playlistDetailCache[playlist.id] = finalDetail
                    mineRepository.savePlaylistDetailPreview(finalDetail)

                    _uiState.update { current ->
                        if (requestId == playlistDetailRequestId && current.activePlaylistDetail?.id == playlist.id) {
                            current.copy(
                                activePlaylistDetail = finalDetail,
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
                                errorMessage = if (current.activePlaylistDetail.tracks.isEmpty()) {
                                    err.toUserFriendlyMessage("加载歌单曲目")
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

    /**
     * 当某首歌曲在列表/菜单中触发喜欢/取消喜欢时，动态同步当前打开歌单与我喜欢的音乐数据
     */
    fun onTrackLikedChanged(track: Track, willBeLiked: Boolean) {
        val currentLikedId = _uiState.value.likedPlaylist?.id
        val activeDetail = _uiState.value.activePlaylistDetail

        // 1. 若当前详情页恰好是“我喜欢的音乐”，动态增删歌曲列表
        if (activeDetail?.id == currentLikedId || activeDetail?.name?.contains("喜欢") == true) {
            _uiState.update { current ->
                val detail = current.activePlaylistDetail ?: return@update current
                val updatedTracks = if (willBeLiked) {
                    listOf(track) + detail.tracks.filter { it.id != track.id }
                } else {
                    detail.tracks.filter { it.id != track.id }
                }
                val updatedDetail = detail.copy(
                    tracks = updatedTracks,
                    trackCount = updatedTracks.size
                )
                playlistDetailCache[detail.id] = updatedDetail
                mineRepository.savePlaylistDetailPreview(updatedDetail)
                current.copy(
                    activePlaylistDetail = updatedDetail,
                    likedPlaylist = current.likedPlaylist?.copy(trackCount = updatedTracks.size)
                )
            }
        } else {
            // 2. 若当前在其它歌单，仅更新我喜欢的音乐的曲目计数
            _uiState.update { current ->
                val count = current.likedPlaylist?.trackCount ?: 0
                val newCount = if (willBeLiked) count + 1 else (count - 1).coerceAtLeast(0)
                current.copy(likedPlaylist = current.likedPlaylist?.copy(trackCount = newCount))
            }
            // 同步更新缓存中“我喜欢的音乐”
            currentLikedId?.let { likedId ->
                playlistDetailCache[likedId]?.let { cachedLiked ->
                    val updatedTracks = if (willBeLiked) {
                        listOf(track) + cachedLiked.tracks.filter { it.id != track.id }
                    } else {
                        cachedLiked.tracks.filter { it.id != track.id }
                    }
                    val updated = cachedLiked.copy(
                        tracks = updatedTracks,
                        trackCount = updatedTracks.size
                    )
                    playlistDetailCache[likedId] = updated
                    mineRepository.savePlaylistDetailPreview(updated)
                } ?: run {
                    mineRepository.getCachedPlaylistDetailPreview(likedId)?.let { cachedPreview ->
                        val updatedTracks = if (willBeLiked) {
                            listOf(track) + cachedPreview.tracks.filter { it.id != track.id }
                        } else {
                            cachedPreview.tracks.filter { it.id != track.id }
                        }
                        val updated = cachedPreview.copy(
                            tracks = updatedTracks,
                            trackCount = updatedTracks.size
                        )
                        mineRepository.savePlaylistDetailPreview(updated)
                    }
                }
            }
        }
    }

    /**
     * 当单曲成功收藏到目标自建歌单时，更新歌单曲目数与详情缓存
     */
    fun onTrackAddedToPlaylist(playlistId: Long, track: Track) {
        _uiState.update { current ->
            val updatedCreated = current.createdPlaylists.map { pl ->
                if (pl.id == playlistId) pl.copy(trackCount = pl.trackCount + 1) else pl
            }
            val active = current.activePlaylistDetail
            val updatedActive = if (active?.id == playlistId) {
                val newTracks = active.tracks + track
                active.copy(tracks = newTracks, trackCount = newTracks.size)
            } else active

            if (updatedActive != null && active?.id == playlistId) {
                playlistDetailCache[playlistId] = updatedActive
            }

            current.copy(
                createdPlaylists = updatedCreated,
                activePlaylistDetail = updatedActive
            )
        }
    }

    /**
     * 更新歌单内部曲目排序（仅支持我喜欢的音乐与我创建的歌单）
     */
    fun updateTrackOrder(playlistId: Long, reorderedTracks: List<Track>) {
        val state = _uiState.value
        val canReorder = playlistId != LOCAL_PLAYLIST_ID && (
            playlistId == state.likedPlaylist?.id ||
                state.createdPlaylists.any { it.id == playlistId }
        )
        if (!canReorder) return

        val active = state.activePlaylistDetail
        val updatedActive = if (active?.id == playlistId) {
            active.copy(tracks = reorderedTracks)
        } else active

        if (playlistId == LOCAL_PLAYLIST_ID) {
            localMusicRepository.updateTrackOrder(reorderedTracks)
            _uiState.update { current ->
                current.copy(
                    localSongs = reorderedTracks,
                    activePlaylistDetail = updatedActive
                )
            }
        } else {
            _uiState.update { current ->
                current.copy(activePlaylistDetail = updatedActive)
            }
            updatedActive?.let { detail ->
                playlistDetailCache[playlistId] = detail
                mineRepository.savePlaylistDetailPreview(detail)
            }
            viewModelScope.launch {
                val result = mineRepository.updatePlaylistTrackOrder(
                    playlistId = playlistId,
                    trackIds = reorderedTracks.map { it.id }
                )
                if (result.isSuccess && updatedActive != null) {
                    mineRepository.savePlaylistDetailPreview(updatedActive)
                }
            }
        }
    }

    /**
     * 批量从歌单中移除歌曲（仅支持我喜欢的音乐与我创建的歌单）
     */
    fun removeTracksFromPlaylist(playlistId: Long, trackIds: Set<Long>) {
        if (trackIds.isEmpty()) return
        val state = _uiState.value
        val canDelete = playlistId != LOCAL_PLAYLIST_ID && (
            playlistId == state.likedPlaylist?.id ||
                state.createdPlaylists.any { it.id == playlistId }
        )
        if (!canDelete) return
        val active = state.activePlaylistDetail ?: return
        val remainingTracks = active.tracks.filterNot { it.id in trackIds }
        val updatedDetail = active.copy(
            tracks = remainingTracks,
            trackCount = remainingTracks.size
        )

        if (playlistId == LOCAL_PLAYLIST_ID) {
            localMusicRepository.removeTracks(trackIds)
            _uiState.update { current ->
                current.copy(
                    localSongs = remainingTracks,
                    activePlaylistDetail = updatedDetail
                )
            }
        } else if (active.id == _uiState.value.likedPlaylist?.id || active.name.contains("喜欢")) {
            trackIds.forEach { id ->
                FavoriteTracksManager.setLiked(id, false)
                viewModelScope.launch {
                    mineRepository.likeTrack(id, false)
                }
            }
            playlistDetailCache[playlistId] = updatedDetail
            mineRepository.savePlaylistDetailPreview(updatedDetail)
            _uiState.update { current ->
                current.copy(
                    activePlaylistDetail = updatedDetail,
                    likedPlaylist = current.likedPlaylist?.copy(trackCount = remainingTracks.size)
                )
            }
        } else {
            viewModelScope.launch {
                mineRepository.removeTracksFromPlaylist(playlistId, trackIds.toList())
            }
            playlistDetailCache[playlistId] = updatedDetail
            mineRepository.savePlaylistDetailPreview(updatedDetail)
            _uiState.update { current ->
                val updatedCreated = current.createdPlaylists.map { pl ->
                    if (pl.id == playlistId) pl.copy(trackCount = remainingTracks.size) else pl
                }
                current.copy(
                    activePlaylistDetail = updatedDetail,
                    createdPlaylists = updatedCreated
                )
            }
        }
    }

    /**
     * 当多首歌曲成功收藏到目标自建歌单时，批量更新歌单曲目数与详情缓存
     */
    fun onTracksAddedToPlaylist(playlistId: Long, newAddedTracks: List<Track>) {
        if (newAddedTracks.isEmpty()) return
        _uiState.update { current ->
            val updatedCreated = current.createdPlaylists.map { pl ->
                if (pl.id == playlistId) pl.copy(trackCount = pl.trackCount + newAddedTracks.size) else pl
            }
            val active = current.activePlaylistDetail
            val updatedActive = if (active?.id == playlistId) {
                val newTracks = active.tracks + newAddedTracks.filterNot { nt -> active.tracks.any { it.id == nt.id } }
                active.copy(tracks = newTracks, trackCount = newTracks.size)
            } else active

            if (updatedActive != null && active?.id == playlistId) {
                playlistDetailCache[playlistId] = updatedActive
            }

            current.copy(
                createdPlaylists = updatedCreated,
                activePlaylistDetail = updatedActive
            )
        }
    }
}
