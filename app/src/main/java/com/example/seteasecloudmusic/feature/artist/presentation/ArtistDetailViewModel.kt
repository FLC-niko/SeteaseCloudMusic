package com.example.seteasecloudmusic.feature.artist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seteasecloudmusic.core.common.toUserFriendlyMessage
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistAlbum
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistDescription
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistDetailInfo
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistFavoritePlaylist
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistSongOrder
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistSummary
import com.example.seteasecloudmusic.feature.artist.domain.usecase.GetAlbumDetailUseCase
import com.example.seteasecloudmusic.feature.artist.domain.usecase.GetArtistAlbumsUseCase
import com.example.seteasecloudmusic.feature.artist.domain.usecase.GetArtistDescriptionUseCase
import com.example.seteasecloudmusic.feature.artist.domain.usecase.GetArtistDetailUseCase
import com.example.seteasecloudmusic.feature.artist.domain.usecase.GetArtistFavoritePlaylistsUseCase
import com.example.seteasecloudmusic.feature.artist.domain.usecase.GetArtistSongsUseCase
import com.example.seteasecloudmusic.feature.artist.domain.usecase.GetArtistTopSongsUseCase
import com.example.seteasecloudmusic.feature.artist.domain.usecase.GetSimilarArtistsUseCase
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.mine.domain.usecase.GetPlaylistDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArtistDetailUiState(
    val artistId: Long = 0L,
    val artistName: String = "",
    val artistCoverUrl: String? = null,
    val detail: ArtistDetailInfo? = null,
    val description: ArtistDescription? = null,
    val songs: List<Track> = emptyList(),
    val songPublishYears: Map<Long, Int> = emptyMap(),
    val albums: List<ArtistAlbum> = emptyList(),
    val favoritePlaylists: List<ArtistFavoritePlaylist> = emptyList(),
    val similarArtists: List<ArtistSummary> = emptyList(),
    val songsOffset: Int = 0,
    val songsHasMore: Boolean = true,
    val songsAllLoaded: Boolean = false,
    val isSongsLoadingMore: Boolean = false,
    val albumsOffset: Int = 0,
    val albumsHasMore: Boolean = true,
    val albumsAllLoaded: Boolean = false,
    val isAlbumsLoadingMore: Boolean = false,
    val similarAllLoaded: Boolean = false,
    val isSimilarLoadingMore: Boolean = false,
    val activePlaylistDetail: PlaylistDetail? = null,
    val isPlaylistDetailLoading: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val getArtistDetailUseCase: GetArtistDetailUseCase,
    private val getArtistDescriptionUseCase: GetArtistDescriptionUseCase,
    private val getArtistTopSongsUseCase: GetArtistTopSongsUseCase,
    private val getArtistSongsUseCase: GetArtistSongsUseCase,
    private val getArtistAlbumsUseCase: GetArtistAlbumsUseCase,
    private val getAlbumDetailUseCase: GetAlbumDetailUseCase,
    private val getArtistFavoritePlaylistsUseCase: GetArtistFavoritePlaylistsUseCase,
    private val getPlaylistDetailUseCase: GetPlaylistDetailUseCase,
    private val getSimilarArtistsUseCase: GetSimilarArtistsUseCase,
    private val musicPlayerController: MusicPlayerController
) : ViewModel() {
    companion object {
        const val PREVIEW_LIMIT = 8
        const val HOT_SONGS_PREVIEW_LIMIT = 5
        const val HOT_PLAY_LIMIT = 50
        private const val SONGS_PAGE_LIMIT = 50
        private const val ALBUMS_INITIAL_LIMIT = 15
        private const val ALBUMS_PAGE_LIMIT = 30
        private const val SYNTHETIC_HOT_PLAYLIST_OFFSET = 1_000_000L
        private const val SYNTHETIC_ALBUM_PLAYLIST_OFFSET = 2_000_000L
    }

    private val _uiState = MutableStateFlow(ArtistDetailUiState())
    val uiState = _uiState.asStateFlow()

    private var currentArtistId: Long? = null
    private var artistRequestId = 0L
    private var initialLoadJob: Job? = null
    private var songsLoadMoreJob: Job? = null
    private var albumsLoadMoreJob: Job? = null
    private var similarLoadMoreJob: Job? = null
    private var albumDetailJob: Job? = null
    private var playlistDetailJob: Job? = null

    fun loadArtist(
        artistId: Long,
        fallbackName: String,
        fallbackCoverUrl: String?
    ) {
        if (artistId <= 0L) {
            return
        }
        val state = _uiState.value
        if (
            currentArtistId == artistId &&
            (state.detail != null || state.songs.isNotEmpty() || state.albums.isNotEmpty())
        ) {
            albumDetailJob?.cancel()
            playlistDetailJob?.cancel()
            if (state.activePlaylistDetail != null || state.isPlaylistDetailLoading) {
                _uiState.update {
                    it.copy(
                        activePlaylistDetail = null,
                        isPlaylistDetailLoading = false
                    )
                }
            }
            return
        }

        val requestId = ++artistRequestId
        currentArtistId = artistId
        initialLoadJob?.cancel()
        songsLoadMoreJob?.cancel()
        albumsLoadMoreJob?.cancel()
        similarLoadMoreJob?.cancel()
        albumDetailJob?.cancel()
        playlistDetailJob?.cancel()

        _uiState.value = ArtistDetailUiState(
            artistId = artistId,
            artistName = fallbackName,
            artistCoverUrl = fallbackCoverUrl,
            isLoading = true
        )

        initialLoadJob = viewModelScope.launch {
            val detailAndFavoritesDeferred = async {
                val detailResult = getArtistDetailUseCase(artistId)
                val userId = detailResult.getOrNull()?.userId
                val favoritesResult = if (userId != null) {
                    getArtistFavoritePlaylistsUseCase(userId)
                } else {
                    Result.success(emptyList())
                }
                detailResult to favoritesResult
            }
            val descriptionDeferred = async { getArtistDescriptionUseCase(artistId) }
            val topSongsDeferred = async { getArtistTopSongsUseCase(artistId) }
            val albumsDeferred = async {
                getArtistAlbumsUseCase(
                    artistId = artistId,
                    limit = ALBUMS_INITIAL_LIMIT,
                    offset = 0
                )
            }
            val similarDeferred = async { getSimilarArtistsUseCase(artistId) }

            val (detailResult, favoritesResult) = detailAndFavoritesDeferred.await()
            val descriptionResult = descriptionDeferred.await()
            val topSongsResult = topSongsDeferred.await()
            val albumsResult = albumsDeferred.await()
            val similarResult = similarDeferred.await()

            if (!isActive || requestId != artistRequestId) {
                return@launch
            }

            val detail = detailResult.getOrNull()
            val description = descriptionResult.getOrNull()
            val topSongs = topSongsResult.getOrNull()
            val songs = topSongs?.tracks.orEmpty()
            val albumsPage = albumsResult.getOrNull()
            val similarArtists = similarResult.getOrNull().orEmpty()
            val favoritePlaylists = favoritesResult.getOrNull().orEmpty()

            val firstError = listOf(
                detailResult.exceptionOrNull(),
                topSongsResult.exceptionOrNull(),
                albumsResult.exceptionOrNull()
            ).firstOrNull { it != null }?.toUserFriendlyMessage("加载歌手信息")

            _uiState.update { prev ->
                if (requestId != artistRequestId || prev.artistId != artistId) {
                    prev
                } else {
                    prev.copy(
                        artistName = detail?.name ?: prev.artistName,
                        artistCoverUrl = detail?.coverUrl ?: prev.artistCoverUrl,
                        detail = detail,
                        description = description,
                        songs = songs,
                        songPublishYears = topSongs?.publishYears.orEmpty(),
                        songsOffset = songs.size,
                        songsHasMore = songs.size >= HOT_PLAY_LIMIT,
                        songsAllLoaded = songs.size < HOT_PLAY_LIMIT,
                        albums = albumsPage?.items ?: emptyList(),
                        albumsOffset = albumsPage?.nextOffset ?: 0,
                        albumsHasMore = albumsPage?.hasMore ?: false,
                        albumsAllLoaded = !(albumsPage?.hasMore ?: false),
                        favoritePlaylists = favoritePlaylists,
                        similarArtists = similarArtists.take(12),
                        similarAllLoaded = true,
                        isLoading = false,
                        errorMessage = if (songs.isEmpty() && albumsPage == null && detail == null) firstError else null
                    )
                }
            }
        }
    }

    fun onLoadAllSongsClick() {
        val state = _uiState.value
        if (
            state.artistId <= 0L ||
            state.isSongsLoadingMore ||
            state.songsAllLoaded ||
            !state.songsHasMore
        ) {
            return
        }

        songsLoadMoreJob?.cancel()
        val requestId = artistRequestId
        songsLoadMoreJob = viewModelScope.launch {
            _uiState.update { it.copy(isSongsLoadingMore = true) }

            val result = getArtistSongsUseCase(
                artistId = state.artistId,
                order = ArtistSongOrder.HOT,
                limit = SONGS_PAGE_LIMIT,
                offset = state.songsOffset
            )

            if (!isActive || requestId != artistRequestId || _uiState.value.artistId != state.artistId) {
                return@launch
            }

            result.onSuccess { page ->
                if (requestId != artistRequestId) return@onSuccess
                _uiState.update { prev ->
                    val merged = (prev.songs + page.items).distinctBy { it.id }
                    prev.copy(
                        songs = merged,
                        songsOffset = page.nextOffset,
                        songsHasMore = page.hasMore,
                        songsAllLoaded = !page.hasMore,
                        isSongsLoadingMore = false,
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                if (requestId != artistRequestId) return@onFailure
                _uiState.update {
                    it.copy(
                        isSongsLoadingMore = false,
                        errorMessage = throwable.toUserFriendlyMessage("加载歌手歌曲")
                    )
                }
            }
        }
    }

    fun onOpenHotSongsPlaylistClick() {
        val state = _uiState.value
        val hotTracks = state.songs.distinctBy { it.id }.take(HOT_PLAY_LIMIT)
        if (hotTracks.isEmpty()) {
            onLoadAllSongsClick()
            return
        }
        albumDetailJob?.cancel()
        playlistDetailJob?.cancel()
        val syntheticId = -state.artistId - SYNTHETIC_HOT_PLAYLIST_OFFSET
        _uiState.update {
            it.copy(
                activePlaylistDetail = PlaylistDetail(
                    id = syntheticId,
                    name = "${state.artistName.ifBlank { "艺人" }} · 热门 ${hotTracks.size} 首",
                    coverUrl = state.artistCoverUrl ?: hotTracks.firstOrNull()?.coverUrl,
                    description = null,
                    trackCount = hotTracks.size,
                    playCount = 0L,
                    creatorName = state.artistName.ifBlank { null },
                    tracks = hotTracks
                ),
                isPlaylistDetailLoading = false
            )
        }
    }

    fun onLoadAllAlbumsClick() {
        val state = _uiState.value
        if (
            state.artistId <= 0L ||
            state.isAlbumsLoadingMore ||
            state.albumsAllLoaded ||
            !state.albumsHasMore
        ) {
            return
        }

        albumsLoadMoreJob?.cancel()
        val requestId = artistRequestId
        albumsLoadMoreJob = viewModelScope.launch {
            _uiState.update { it.copy(isAlbumsLoadingMore = true) }

            val result = getArtistAlbumsUseCase(
                artistId = state.artistId,
                limit = ALBUMS_PAGE_LIMIT,
                offset = state.albumsOffset
            )

            if (!isActive || requestId != artistRequestId || _uiState.value.artistId != state.artistId) {
                return@launch
            }

            result.onSuccess { page ->
                if (requestId != artistRequestId) return@onSuccess
                _uiState.update { prev ->
                    val merged = (prev.albums + page.items).distinctBy { it.id }
                    prev.copy(
                        albums = merged,
                        albumsOffset = page.nextOffset,
                        albumsHasMore = page.hasMore,
                        albumsAllLoaded = !page.hasMore,
                        isAlbumsLoadingMore = false,
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                if (requestId != artistRequestId) return@onFailure
                _uiState.update {
                    it.copy(
                        isAlbumsLoadingMore = false,
                        errorMessage = throwable.toUserFriendlyMessage("加载歌手专辑")
                    )
                }
            }
        }
    }

    fun onLoadAllSimilarArtistsClick() {
        val state = _uiState.value
        if (state.artistId <= 0L || state.isSimilarLoadingMore || state.similarAllLoaded) {
            return
        }

        similarLoadMoreJob?.cancel()
        val requestId = artistRequestId
        similarLoadMoreJob = viewModelScope.launch {
            _uiState.update { it.copy(isSimilarLoadingMore = true) }

            val result = getSimilarArtistsUseCase(state.artistId)
            if (!isActive || requestId != artistRequestId || _uiState.value.artistId != state.artistId) {
                return@launch
            }
            result.onSuccess { artists ->
                if (requestId != artistRequestId) return@onSuccess
                _uiState.update {
                    it.copy(
                        similarArtists = artists,
                        similarAllLoaded = true,
                        isSimilarLoadingMore = false,
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                if (requestId != artistRequestId) return@onFailure
                _uiState.update {
                    it.copy(
                        isSimilarLoadingMore = false,
                        errorMessage = throwable.toUserFriendlyMessage("加载相似歌手")
                    )
                }
            }
        }
    }

    fun onAlbumClick(album: ArtistAlbum) {
        if (album.id <= 0L) return
        albumDetailJob?.cancel()
        playlistDetailJob?.cancel()
        val requestId = artistRequestId
        val fallbackArtist = _uiState.value.artistName
        val syntheticAlbumId = -album.id - SYNTHETIC_ALBUM_PLAYLIST_OFFSET
        _uiState.update {
            it.copy(
                activePlaylistDetail = PlaylistDetail(
                    id = syntheticAlbumId,
                    name = album.title,
                    coverUrl = album.coverUrl,
                    description = null,
                    trackCount = album.trackCount ?: 0,
                    playCount = 0L,
                    creatorName = fallbackArtist.ifBlank { null },
                    tracks = emptyList()
                ),
                isPlaylistDetailLoading = true
            )
        }
        albumDetailJob = viewModelScope.launch {
            val result = getAlbumDetailUseCase(album.id)
            if (!isActive || requestId != artistRequestId) return@launch
            result.onSuccess { detail ->
                _uiState.update { prev ->
                    if (prev.activePlaylistDetail?.id == syntheticAlbumId) {
                        prev.copy(
                            activePlaylistDetail = PlaylistDetail(
                                id = syntheticAlbumId,
                                name = detail.album.title,
                                coverUrl = detail.album.coverUrl ?: album.coverUrl,
                                description = detail.description,
                                trackCount = detail.tracks.size,
                                playCount = 0L,
                                creatorName = detail.artistName.ifBlank { fallbackArtist.ifBlank { null } },
                                tracks = detail.tracks
                            ),
                            isPlaylistDetailLoading = false
                        )
                    } else {
                        prev
                    }
                }
            }.onFailure {
                _uiState.update { prev ->
                    if (prev.activePlaylistDetail?.id == syntheticAlbumId) {
                        prev.copy(isPlaylistDetailLoading = false)
                    } else {
                        prev
                    }
                }
            }
        }
    }

    fun onFavoritePlaylistClick(playlist: ArtistFavoritePlaylist) {
        if (playlist.id <= 0L) return
        albumDetailJob?.cancel()
        playlistDetailJob?.cancel()
        val requestId = artistRequestId
        val fallbackArtist = _uiState.value.artistName.ifBlank { "TA" }
        val normalizedName: (String) -> String = { raw ->
            if (playlist.isLikedPlaylist || raw.endsWith("喜欢的音乐")) {
                "$fallbackArtist 爱听的音乐"
            } else {
                raw
            }
        }
        val cached = getPlaylistDetailUseCase.getCachedPreview(playlist.id)
        val preview = cached?.copy(name = normalizedName(cached.name)) ?: PlaylistDetail(
            id = playlist.id,
            name = normalizedName(playlist.name),
            coverUrl = playlist.coverUrl,
            description = null,
            trackCount = playlist.trackCount,
            playCount = playlist.playCount,
            creatorName = fallbackArtist,
            tracks = emptyList()
        )
        _uiState.update {
            it.copy(
                activePlaylistDetail = preview,
                isPlaylistDetailLoading = preview.tracks.isEmpty()
            )
        }
        playlistDetailJob = viewModelScope.launch {
            val result = getPlaylistDetailUseCase(playlist.id)
            if (!isActive || requestId != artistRequestId) return@launch
            result.onSuccess { detail ->
                _uiState.update { prev ->
                    if (prev.activePlaylistDetail?.id == playlist.id) {
                        prev.copy(
                            activePlaylistDetail = detail.copy(name = normalizedName(detail.name)),
                            isPlaylistDetailLoading = false
                        )
                    } else {
                        prev
                    }
                }
            }.onFailure {
                _uiState.update { prev ->
                    if (prev.activePlaylistDetail?.id == playlist.id) {
                        prev.copy(isPlaylistDetailLoading = false)
                    } else {
                        prev
                    }
                }
            }
        }
    }

    fun onClosePlaylistDetail() {
        albumDetailJob?.cancel()
        playlistDetailJob?.cancel()
        _uiState.update { it.copy(activePlaylistDetail = null, isPlaylistDetailLoading = false) }
    }

    /**
     * 播放艺人热门 50 首（从第一首开始替换播放队列）。
     */
    fun onPlayFirstSongClick() {
        val hot50 = _uiState.value.songs
            .distinctBy { it.id }
            .take(HOT_PLAY_LIMIT)
        if (hot50.isEmpty()) return
        musicPlayerController.replaceQueueAndPlay(hot50, 0)
    }

    fun onSongClick(track: Track) {
        val tracks = _uiState.value.songs.distinctBy { it.id }
        if (tracks.isEmpty()) {
            return
        }

        val index = tracks.indexOfFirst { it.id == track.id }
        if (index !in tracks.indices) {
            return
        }

        musicPlayerController.replaceQueueAndPlay(tracks, index)
    }

    fun onPlayTrackList(tracks: List<Track>, startIndex: Int = 0) {
        val distinct = tracks.distinctBy { it.id }
        if (distinct.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, distinct.lastIndex)
        musicPlayerController.replaceQueueAndPlay(distinct, safeIndex)
    }
}
