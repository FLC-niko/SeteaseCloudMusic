package com.example.seteasecloudmusic.feature.radio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.network.NetworkMonitor
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.mine.domain.usecase.GetPlaylistDetailUseCase
import com.example.seteasecloudmusic.feature.radio.domain.model.ArtistHotSectionData
import com.example.seteasecloudmusic.feature.radio.domain.model.FollowedArtistSectionData
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioArtistHotItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioArtistItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioFollowedArtistNewSongItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioMoodItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioPlaylistItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioRadarItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioRoamItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioToplistItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioTrendItem
import com.example.seteasecloudmusic.feature.radio.domain.repository.RadioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RadioUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    // 歌单详情层 (类似我的喜欢歌单页面)
    val activePlaylistDetail: PlaylistDetail? = null,
    val isLoadingDetail: Boolean = false,
    // 1. 排行榜
    val toplists: List<RadioToplistItem> = emptyList(),
    // 2. 雷达歌单
    val radarPlaylists: List<RadioRadarItem> = emptyList(),
    // 3. 从你喜欢的歌曲开始漫游
    val roamItems: List<RadioRoamItem> = emptyList(),
    // 4. 歌手推荐
    val recommendedSingers: List<RadioArtistItem> = emptyList(),
    // 5. 推荐歌单
    val recommendedPlaylists: List<RadioPlaylistItem> = emptyList(),
    // 6. 艺人热门金曲
    val artistHotSection: ArtistHotSectionData = ArtistHotSectionData(),
    // 7. 每周新热趋势
    val trends: List<RadioTrendItem> = emptyList(),
    // 8. 心情氛围
    val moodPlaylists: List<RadioMoodItem> = emptyList(),
    // 9. 你关注的艺人新动向
    val followedArtistSection: FollowedArtistSectionData = FollowedArtistSectionData()
)

@HiltViewModel
class RadioViewModel @Inject constructor(
    private val radioRepository: RadioRepository,
    private val getPlaylistDetailUseCase: GetPlaylistDetailUseCase,
    private val musicPlayerController: MusicPlayerController,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(RadioUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private var playlistDetailJob: Job? = null
    private var playlistDetailRequestId: Long = 0L

    init {
        loadAllSections()

        // 监听断网重连自动刷新
        viewModelScope.launch {
            networkMonitor.retryTrigger.collect {
                loadAllSections(silent = false)
            }
        }
    }

    fun onRefreshClick() {
        loadAllSections(silent = false)
    }

    fun onRetryClick() {
        loadAllSections(silent = false)
    }

    private fun loadAllSections(silent: Boolean = true) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            }

            val toplistDeferred = async { radioRepository.getToplists() }
            val radarDeferred = async { radioRepository.getRadarPlaylists() }
            val roamDeferred = async { radioRepository.getRoamItems() }
            val singersDeferred = async { radioRepository.getRecommendedSingers() }
            val playlistsDeferred = async { radioRepository.getRecommendedPlaylists() }
            val artistHotDeferred = async { radioRepository.getArtistHotSection() }
            val trendsDeferred = async { radioRepository.getTrends() }
            val moodDeferred = async { radioRepository.getMoodPlaylists() }
            val followedDeferred = async { radioRepository.getFollowedArtistSection() }

            val toplists = toplistDeferred.await().getOrDefault(emptyList())
            val radar = radarDeferred.await().getOrDefault(emptyList())
            val roam = roamDeferred.await().getOrDefault(emptyList())
            val singers = singersDeferred.await().getOrDefault(emptyList())
            val playlists = playlistsDeferred.await().getOrDefault(emptyList())
            val artistHot = artistHotDeferred.await().getOrDefault(ArtistHotSectionData())
            val trends = trendsDeferred.await().getOrDefault(emptyList())
            val mood = moodDeferred.await().getOrDefault(emptyList())
            val followed = followedDeferred.await().getOrDefault(FollowedArtistSectionData())

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    toplists = toplists,
                    radarPlaylists = radar,
                    roamItems = roam,
                    recommendedSingers = singers,
                    recommendedPlaylists = playlists,
                    artistHotSection = artistHot,
                    trends = trends,
                    moodPlaylists = mood,
                    followedArtistSection = followed,
                    errorMessage = null
                )
            }
        }
    }

    // ==========================================
    // 歌单详情层操作（类似我的喜欢歌单页面）
    // ==========================================
    fun openPlaylistDetail(playlistId: Long, name: String, coverUrl: String?) {
        playlistDetailJob?.cancel()
        val requestId = ++playlistDetailRequestId

        val cached = getPlaylistDetailUseCase.getCachedPreview(playlistId)
        val instantPreview = cached ?: PlaylistDetail(
            id = playlistId,
            name = name,
            coverUrl = coverUrl,
            description = null,
            trackCount = 0,
            playCount = 0L,
            creatorName = null,
            tracks = emptyList()
        )

        _uiState.update {
            it.copy(
                activePlaylistDetail = instantPreview,
                isLoadingDetail = instantPreview.tracks.isEmpty()
            )
        }

        playlistDetailJob = viewModelScope.launch {
            val result = getPlaylistDetailUseCase(playlistId)
            if (!isActive || requestId != playlistDetailRequestId) return@launch
            result.fold(
                onSuccess = { detail ->
                    if (requestId != playlistDetailRequestId) return@fold
                    _uiState.update { current ->
                        if (current.activePlaylistDetail?.id == playlistId) {
                            current.copy(
                                activePlaylistDetail = detail,
                                isLoadingDetail = false
                            )
                        } else current
                    }
                },
                onFailure = {
                    if (requestId != playlistDetailRequestId) return@fold
                    _uiState.update { current ->
                        if (current.activePlaylistDetail?.id == playlistId) {
                            current.copy(isLoadingDetail = false)
                        } else current
                    }
                }
            )
        }
    }

    fun closePlaylistDetail() {
        playlistDetailJob?.cancel()
        playlistDetailJob = null
        playlistDetailRequestId++
        _uiState.update { it.copy(activePlaylistDetail = null, isLoadingDetail = false) }
    }

    fun playTrackInPlaylist(track: Track, trackList: List<Track>) {
        val index = trackList.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        musicPlayerController.replaceQueueAndPlay(trackList, index)
    }

    fun playAllTracks(trackList: List<Track>) {
        if (trackList.isNotEmpty()) {
            musicPlayerController.replaceQueueAndPlay(trackList, 0)
        }
    }

    // ==========================================
    // 播放逻辑
    // ==========================================
    fun playToplist(item: RadioToplistItem) {
        viewModelScope.launch {
            val tracks = radioRepository.getPlaylistTracks(item.id).getOrDefault(emptyList())
            if (tracks.isNotEmpty()) {
                musicPlayerController.replaceQueueAndPlay(tracks, 0)
            }
        }
    }

    fun playRadar(item: RadioRadarItem) {
        viewModelScope.launch {
            val tracks = radioRepository.getPlaylistTracks(item.id).getOrDefault(emptyList())
            if (tracks.isNotEmpty()) {
                musicPlayerController.replaceQueueAndPlay(tracks, 0)
            }
        }
    }

    fun playRoam(item: RadioRoamItem) {
        if (item.tracks.isNotEmpty()) {
            musicPlayerController.replaceQueueAndPlay(item.tracks, 0)
        }
    }

    fun playPlaylist(item: RadioPlaylistItem) {
        viewModelScope.launch {
            val tracks = radioRepository.getPlaylistTracks(item.id).getOrDefault(emptyList())
            if (tracks.isNotEmpty()) {
                musicPlayerController.replaceQueueAndPlay(tracks, 0)
            }
        }
    }

    fun playArtistHot(item: RadioArtistHotItem) {
        viewModelScope.launch {
            val tracks = if (item.songs.isNotEmpty()) item.songs else radioRepository.getArtistTopTracks(item.artistId).getOrDefault(emptyList())
            if (tracks.isNotEmpty()) {
                musicPlayerController.replaceQueueAndPlay(tracks, 0)
            }
        }
    }

    fun playTrend(item: RadioTrendItem) {
        viewModelScope.launch {
            val tracks = radioRepository.getPlaylistTracks(item.id).getOrDefault(emptyList())
            if (tracks.isNotEmpty()) {
                musicPlayerController.replaceQueueAndPlay(tracks, 0)
            }
        }
    }

    fun playMood(item: RadioMoodItem) {
        viewModelScope.launch {
            val tracks = radioRepository.getPlaylistTracks(item.id).getOrDefault(emptyList())
            if (tracks.isNotEmpty()) {
                musicPlayerController.replaceQueueAndPlay(tracks, 0)
            }
        }
    }

    fun playFollowedNewSong(item: RadioFollowedArtistNewSongItem) {
        item.track?.let { track ->
            musicPlayerController.replaceQueueAndPlay(listOf(track), 0)
        }
    }
}

