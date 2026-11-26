package com.example.seteasecloudmusic.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seteasecloudmusic.core.common.toUserFriendlyMessage
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import com.example.seteasecloudmusic.feature.home.domain.repository.HomeRecommendRepository
import com.example.seteasecloudmusic.feature.home.domain.repository.RadarPlaylistData
import com.example.seteasecloudmusic.feature.home.domain.usecase.GetDailyRecommendSongsUseCase
import com.example.seteasecloudmusic.feature.home.domain.usecase.GetFavoriteRecommendSongsUseCase
import com.example.seteasecloudmusic.feature.home.domain.usecase.GetRadarPlaylistUseCase
import com.example.seteasecloudmusic.core.network.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import javax.inject.Inject

/** 首页界面状态，聚合各类推荐歌曲列表、封面及内嵌详情页状态。 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val tracks: List<Track> = emptyList(),
    val favoriteSectionTitle: String = "根据你喜爱的歌曲推荐",
    val favoriteTracks: List<Track> = emptyList(),
    val radarTracks: List<Track> = emptyList(),
    val radarPlaylist: RadarPlaylistData? = null,
    val newSongRadarTracks: List<Track> = emptyList(),
    val newSongRadarPlaylist: RadarPlaylistData? = null,
    val privateDjTracks: List<Track> = emptyList(),
    val simiSongsTracks: List<Track> = emptyList(),
    val heartModeTracks: List<Track> = emptyList(),
    val likedMusicCoverUrl: String? = null,
    val privateDjCoverUrl: String? = null,
    val errorMessage: String? = null,
    val activePlaylistDetail: PlaylistDetail? = null
)

/**
 * 首页 ViewModel：负责本地缓存预加载、多路推荐数据并发刷新、断网重试以及大卡片快捷播放。
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDailyRecommendSongsUseCase: GetDailyRecommendSongsUseCase,
    private val getFavoriteRecommendSongsUseCase: GetFavoriteRecommendSongsUseCase,
    private val getRadarPlaylistUseCase: GetRadarPlaylistUseCase,
    private val homeRecommendRepository: HomeRecommendRepository,
    private val musicPlayerController: MusicPlayerController,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var favoriteJob: Job? = null
    private var radarJob: Job? = null
    private var newSongRadarJob: Job? = null
    private var privateDjJob: Job? = null
    private var simiSongsJob: Job? = null
    private var heartModeJob: Job? = null
    private var loadRequestId = 0L

    init {
        // 1. 异步在 IO 调度器加载本地持久化推荐、喜好推荐、雷达与漫游缓存，避免在主线程执行反射与 SharedPreferences 阻塞首帧绘制
        viewModelScope.launch(Dispatchers.IO) {
            val cached = getDailyRecommendSongsUseCase.getCached()
            val cachedFav = getFavoriteRecommendSongsUseCase.getCached()
            val cachedRadar = getRadarPlaylistUseCase.getCached()
            val cachedNewSongRadar = homeRecommendRepository.getCachedNewSongRadarPlaylist()
            val cachedPrivateDj = homeRecommendRepository.getCachedPrivateDjSongs()
            val cachedSimiSongs = homeRecommendRepository.getCachedSimiRecommendSongs()
            val cachedHeartMode = homeRecommendRepository.getCachedHeartModeSongs()
            if (!cached.isNullOrEmpty() ||
                cachedFav != null ||
                cachedRadar != null ||
                cachedNewSongRadar != null ||
                !cachedPrivateDj.isNullOrEmpty() ||
                !cachedSimiSongs.isNullOrEmpty() ||
                !cachedHeartMode.isNullOrEmpty()
            ) {
                _uiState.update {
                    it.copy(
                        tracks = cached ?: it.tracks,
                        favoriteSectionTitle = cachedFav?.title ?: it.favoriteSectionTitle,
                        favoriteTracks = cachedFav?.tracks ?: it.favoriteTracks,
                        radarTracks = cachedRadar?.tracks ?: it.radarTracks,
                        radarPlaylist = cachedRadar ?: it.radarPlaylist,
                        newSongRadarTracks = cachedNewSongRadar?.tracks ?: it.newSongRadarTracks,
                        newSongRadarPlaylist = cachedNewSongRadar ?: it.newSongRadarPlaylist,
                        privateDjTracks = cachedPrivateDj ?: it.privateDjTracks,
                        simiSongsTracks = cachedSimiSongs ?: it.simiSongsTracks,
                        heartModeTracks = cachedHeartMode ?: it.heartModeTracks,
                        privateDjCoverUrl = it.privateDjCoverUrl ?: cachedPrivateDj?.firstOrNull()?.coverUrl,
                        isLoading = false
                    )
                }
            }
        }
        // 2. 监听播放器状态：仅当当前播放为私人 DJ 时更新其封面，播放其他歌单时私人 DJ 封面保持不变
        viewModelScope.launch {
            musicPlayerController.playbackState.collect { playbackState ->
                if (playbackState.queueSource == MusicPlayerController.QUEUE_SOURCE_PRIVATE_DJ) {
                    val cover = playbackState.currentTrack?.coverUrl
                    if (!cover.isNullOrBlank()) {
                        _uiState.update { it.copy(privateDjCoverUrl = cover) }
                    }
                }
            }
        }
        // 3. 后台静默拉取今日最新推荐、喜好推荐、雷达歌单、新歌雷达、私人 DJ、相似歌曲、心动模式与用户红心封面
        refreshDailyRecommend(afresh = false, silent = true)
        refreshFavoriteRecommend(afresh = false, silent = true)
        refreshRadarPlaylist(silent = true)
        refreshNewSongRadarPlaylist(silent = true)
        refreshPrivateDjSongs()
        refreshSimiSongs()
        refreshHeartModeSongs()
        fetchLikedMusicCover()

        // 4. 监听全局断网重试事件
        viewModelScope.launch {
            networkMonitor.retryTrigger.collect {
                refreshDailyRecommend(afresh = true, silent = false)
                refreshFavoriteRecommend(afresh = true, silent = false)
                refreshRadarPlaylist(silent = false)
                refreshNewSongRadarPlaylist(silent = false)
                refreshPrivateDjSongs()
                refreshSimiSongs()
                refreshHeartModeSongs()
                fetchLikedMusicCover()
            }
        }
    }

    fun refreshDailyRecommend(afresh: Boolean = false, silent: Boolean = false) {
        loadJob?.cancel()
        val requestId = ++loadRequestId
        loadJob = viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            val result = getDailyRecommendSongsUseCase(afresh)
            if (!isActive || requestId != loadRequestId) {
                return@launch
            }
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
                            errorMessage = if (state.tracks.isEmpty()) throwable.toUserFriendlyMessage("获取每日推荐") else null
                        )
                    }
                    // 仅当设备确实离线时才提示“网络连接已断开”。
                    // 在线但接口返回空数据或服务端异常时会走上面的 errorMessage 提示，不应误报为断网。
                    if (!networkMonitor.checkOnlineStatus()) {
                        networkMonitor.requestOfflineDialog(
                            title = "网络连接已断开",
                            description = "获取每日推荐失败，请检查网络设置后重试。"
                        )
                    }
                }
            )
        }
    }

    fun refreshFavoriteRecommend(afresh: Boolean = false, silent: Boolean = false) {
        favoriteJob?.cancel()
        favoriteJob = viewModelScope.launch {
            val result = getFavoriteRecommendSongsUseCase(afresh)
            result.fold(
                onSuccess = { sectionData ->
                    _uiState.update {
                        it.copy(
                            favoriteSectionTitle = sectionData.title,
                            favoriteTracks = sectionData.tracks
                        )
                    }
                },
                onFailure = {
                    // 静默失败，保持已有缓存/预设数据
                }
            )
        }
    }

    fun fetchLikedMusicCover() {
        viewModelScope.launch {
            val coverUrl = homeRecommendRepository.getUserLikedMusicCover()
            if (!coverUrl.isNullOrBlank()) {
                _uiState.update { it.copy(likedMusicCoverUrl = coverUrl) }
            }
        }
    }

    fun refreshRadarPlaylist(silent: Boolean = false) {
        radarJob?.cancel()
        radarJob = viewModelScope.launch {
            val result = getRadarPlaylistUseCase()
            result.fold(
                onSuccess = { radarData ->
                    _uiState.update {
                        it.copy(
                            radarPlaylist = radarData,
                            radarTracks = radarData.tracks
                        )
                    }
                },
                onFailure = {
                    // 静默失败，保持原有/预设数据
                }
            )
        }
    }

    fun refreshNewSongRadarPlaylist(silent: Boolean = false) {
        newSongRadarJob?.cancel()
        newSongRadarJob = viewModelScope.launch {
            val result = homeRecommendRepository.getNewSongRadarPlaylist()
            result.fold(
                onSuccess = { radarData ->
                    _uiState.update {
                        it.copy(
                            newSongRadarPlaylist = radarData,
                            newSongRadarTracks = radarData.tracks
                        )
                    }
                },
                onFailure = {
                    // 静默失败，保持原有/预设数据
                }
            )
        }
    }

    fun refreshPrivateDjSongs() {
        privateDjJob?.cancel()
        privateDjJob = viewModelScope.launch {
            val result = homeRecommendRepository.getPrivateDjSongs()
            result.onSuccess { djTracks ->
                if (djTracks.isNotEmpty()) {
                    _uiState.update { state ->
                        state.copy(
                            privateDjTracks = djTracks,
                            privateDjCoverUrl = state.privateDjCoverUrl ?: djTracks.firstOrNull()?.coverUrl
                        )
                    }
                }
            }
        }
    }

    fun refreshSimiSongs() {
        simiSongsJob?.cancel()
        simiSongsJob = viewModelScope.launch {
            val result = homeRecommendRepository.getSimiRecommendSongs()
            result.onSuccess { simiTracks ->
                if (simiTracks.isNotEmpty()) {
                    _uiState.update { it.copy(simiSongsTracks = simiTracks) }
                }
            }
        }
    }

    fun refreshHeartModeSongs() {
        heartModeJob?.cancel()
        heartModeJob = viewModelScope.launch {
            val result = homeRecommendRepository.getHeartModeSongs()
            result.onSuccess { heartTracks ->
                if (heartTracks.isNotEmpty()) {
                    _uiState.update { it.copy(heartModeTracks = heartTracks) }
                }
            }
        }
    }

    fun onRetryClick() {
        refreshDailyRecommend(afresh = false, silent = false)
        refreshFavoriteRecommend(afresh = false, silent = false)
        refreshRadarPlaylist(silent = false)
        refreshNewSongRadarPlaylist(silent = false)
        refreshPrivateDjSongs()
        refreshSimiSongs()
        refreshHeartModeSongs()
        fetchLikedMusicCover()
    }

    fun onRefreshClick() {
        refreshDailyRecommend(afresh = true, silent = false)
        refreshFavoriteRecommend(afresh = true, silent = false)
        refreshRadarPlaylist(silent = false)
        refreshNewSongRadarPlaylist(silent = false)
        refreshPrivateDjSongs()
        refreshSimiSongs()
        refreshHeartModeSongs()
        fetchLikedMusicCover()
    }

    fun onTrackClick(track: Track, tracksOverride: List<Track>? = null) {
        val tracksToUse = if (!tracksOverride.isNullOrEmpty()) tracksOverride else uiState.value.tracks
        val snapshotTracks = tracksToUse.distinctBy { it.id }
        if (snapshotTracks.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "暂无可播放的歌曲") }
            return
        }

        val clickedIndex = snapshotTracks.indexOfFirst { it.id == track.id }
        val finalIndex = if (clickedIndex in snapshotTracks.indices) clickedIndex else 0

        musicPlayerController.replaceQueueAndPlay(snapshotTracks, finalIndex)
    }

    fun playAll(tracks: List<Track>? = null, startIndex: Int = 0) {
        val list = (tracks ?: uiState.value.tracks).distinctBy { it.id }
        if (list.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "暂无可播放的歌曲") }
            return
        }
        val safeIndex = if (startIndex in list.indices) startIndex else 0
        musicPlayerController.replaceQueueAndPlay(list, safeIndex)
    }

    /**
     * 点击私人 DJ 直接播放：优先调用 /personal/fm/mode?mode=AIDJ 与 /personal_fm，
     * 设置播放来源为 PRIVATE_DJ，并立即更新私人 DJ 封面为当前播放曲目
     */
    fun playPrivateDj() {
        val currentDjTracks = uiState.value.privateDjTracks.distinctBy { it.id }
        if (currentDjTracks.isNotEmpty()) {
            val firstCover = currentDjTracks.firstOrNull()?.coverUrl
            if (!firstCover.isNullOrBlank()) {
                _uiState.update { it.copy(privateDjCoverUrl = firstCover) }
            }
            musicPlayerController.replaceQueueAndPlay(
                tracks = currentDjTracks,
                startIndex = 0,
                queueSource = MusicPlayerController.QUEUE_SOURCE_PRIVATE_DJ
            )
            // 后台拉取下一批私人 DJ 歌曲，保证下次点击持续新鲜
            refreshPrivateDjSongs()
            return
        }

        privateDjJob?.cancel()
        privateDjJob = viewModelScope.launch {
            val fetched = homeRecommendRepository.getPrivateDjSongs().getOrNull().orEmpty()
            val list = when {
                fetched.isNotEmpty() -> fetched
                uiState.value.favoriteTracks.isNotEmpty() -> uiState.value.favoriteTracks
                else -> uiState.value.tracks
            }.distinctBy { it.id }

            if (list.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "暂无可播放的歌曲") }
                return@launch
            }
            val firstCover = list.firstOrNull()?.coverUrl
            _uiState.update {
                it.copy(
                    privateDjTracks = if (fetched.isNotEmpty()) fetched else it.privateDjTracks,
                    privateDjCoverUrl = firstCover ?: it.privateDjCoverUrl
                )
            }
            musicPlayerController.replaceQueueAndPlay(
                tracks = list,
                startIndex = 0,
                queueSource = MusicPlayerController.QUEUE_SOURCE_PRIVATE_DJ
            )
        }
    }

    /**
     * 点击相似歌曲直接播放：不展开歌单详情页，直接播放基于喜欢歌曲的相似漫游序列
     */
    fun playSimiSongs() {
        val currentSimiTracks = uiState.value.simiSongsTracks.distinctBy { it.id }
        if (currentSimiTracks.isNotEmpty()) {
            musicPlayerController.replaceQueueAndPlay(currentSimiTracks, 0)
            return
        }

        simiSongsJob?.cancel()
        simiSongsJob = viewModelScope.launch {
            val fetched = homeRecommendRepository.getSimiRecommendSongs().getOrNull().orEmpty()
            val list = when {
                fetched.isNotEmpty() -> fetched
                uiState.value.favoriteTracks.isNotEmpty() -> uiState.value.favoriteTracks
                else -> uiState.value.tracks
            }.distinctBy { it.id }

            if (list.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "暂无可播放的歌曲") }
                return@launch
            }
            if (fetched.isNotEmpty()) {
                _uiState.update { it.copy(simiSongsTracks = fetched) }
            }
            musicPlayerController.replaceQueueAndPlay(list, 0)
        }
    }

    /**
     * 点击心动模式直接播放：不展开歌单详情页，直接调用 /playmode/intelligence/list 播放红心与智能相似混排
     */
    fun playHeartMode() {
        val currentHeartTracks = uiState.value.heartModeTracks.distinctBy { it.id }
        if (currentHeartTracks.isNotEmpty()) {
            musicPlayerController.replaceQueueAndPlay(currentHeartTracks, 0)
            refreshHeartModeSongs()
            return
        }

        heartModeJob?.cancel()
        heartModeJob = viewModelScope.launch {
            val fetched = homeRecommendRepository.getHeartModeSongs().getOrNull().orEmpty()
            val list = when {
                fetched.isNotEmpty() -> fetched
                uiState.value.favoriteTracks.isNotEmpty() -> uiState.value.favoriteTracks
                else -> uiState.value.tracks
            }.distinctBy { it.id }

            if (list.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "暂无可播放的歌曲") }
                return@launch
            }
            if (fetched.isNotEmpty()) {
                _uiState.update { it.copy(heartModeTracks = fetched) }
            }
            musicPlayerController.replaceQueueAndPlay(list, 0)
        }
    }

    fun openPlaylistDetail(tracks: List<Track>, title: String, coverUrl: String?) {
        val playlistDetail = PlaylistDetail(
            // 使用负值虚拟 ID 区分首页聚合歌单与真实歌单
            id = when (title) {
                "每日推荐" -> -1001L
                "雷达歌单" -> -1002L
                "新歌雷达" -> -1006L
                else -> title.hashCode().toLong()
            },
            name = title,
            coverUrl = coverUrl ?: tracks.firstOrNull()?.coverUrl,
            description = when (title) {
                "每日推荐" -> "今天，听点喜欢的"
                "雷达歌单" -> "反复聆听你爱的歌"
                "新歌雷达" -> uiState.value.newSongRadarPlaylist?.description?.ifBlank { "发现最新发行的好歌" } ?: "发现最新发行的好歌"
                else -> "为你精选 · 共 ${tracks.size} 首歌曲"
            },
            trackCount = tracks.size,
            playCount = 0L,
            creatorName = "网易云音乐",
            tracks = tracks
        )
        _uiState.update { it.copy(activePlaylistDetail = playlistDetail) }
    }

    fun closePlaylistDetail() {
        _uiState.update { it.copy(activePlaylistDetail = null) }
    }
}
