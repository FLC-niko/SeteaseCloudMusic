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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val tracks: List<Track> = emptyList(),
    val favoriteSectionTitle: String = "根据你喜爱的歌曲推荐",
    val favoriteTracks: List<Track> = emptyList(),
    val radarTracks: List<Track> = emptyList(),
    val radarPlaylist: RadarPlaylistData? = null,
    val likedMusicCoverUrl: String? = null,
    val privateDjCoverUrl: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDailyRecommendSongsUseCase: GetDailyRecommendSongsUseCase,
    private val getFavoriteRecommendSongsUseCase: GetFavoriteRecommendSongsUseCase,
    private val getRadarPlaylistUseCase: GetRadarPlaylistUseCase,
    private val homeRecommendRepository: HomeRecommendRepository,
    private val musicPlayerController: MusicPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var favoriteJob: Job? = null
    private var radarJob: Job? = null
    private var loadRequestId = 0L

    init {
        // 1. 异步在 IO 调度器加载本地持久化推荐、喜好推荐与雷达缓存，避免在主线程执行反射与 SharedPreferences 阻塞首帧绘制
        viewModelScope.launch(Dispatchers.IO) {
            val cached = getDailyRecommendSongsUseCase.getCached()
            val cachedFav = getFavoriteRecommendSongsUseCase.getCached()
            val cachedRadar = getRadarPlaylistUseCase.getCached()
            if (!cached.isNullOrEmpty() || cachedFav != null || cachedRadar != null) {
                _uiState.update {
                    it.copy(
                        tracks = cached ?: it.tracks,
                        favoriteSectionTitle = cachedFav?.title ?: it.favoriteSectionTitle,
                        favoriteTracks = cachedFav?.tracks ?: it.favoriteTracks,
                        radarTracks = cachedRadar?.tracks ?: it.radarTracks,
                        radarPlaylist = cachedRadar ?: it.radarPlaylist,
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
        // 3. 后台静默拉取今日最新推荐、喜好推荐、雷达歌单与用户红心封面
        refreshDailyRecommend(afresh = false, silent = true)
        refreshFavoriteRecommend(afresh = false, silent = true)
        refreshRadarPlaylist(silent = true)
        fetchLikedMusicCover()
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

    fun onRetryClick() {
        refreshDailyRecommend(afresh = false, silent = false)
        refreshFavoriteRecommend(afresh = false, silent = false)
        refreshRadarPlaylist(silent = false)
        fetchLikedMusicCover()
    }

    fun onRefreshClick() {
        refreshDailyRecommend(afresh = true, silent = false)
        refreshFavoriteRecommend(afresh = true, silent = false)
        refreshRadarPlaylist(silent = false)
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
     * 点击私人 DJ 播放：设置播放来源为 PRIVATE_DJ，并立即更新私人 DJ 封面为当前播放曲目
     */
    fun playPrivateDj() {
        val list = if (uiState.value.favoriteTracks.isNotEmpty()) uiState.value.favoriteTracks else uiState.value.tracks
        val snapshot = list.distinctBy { it.id }
        if (snapshot.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "暂无可播放的歌曲") }
            return
        }
        val firstCover = snapshot.firstOrNull()?.coverUrl
        if (!firstCover.isNullOrBlank()) {
            _uiState.update { it.copy(privateDjCoverUrl = firstCover) }
        }
        musicPlayerController.replaceQueueAndPlay(
            tracks = snapshot,
            startIndex = 0,
            queueSource = MusicPlayerController.QUEUE_SOURCE_PRIVATE_DJ
        )
    }
}
