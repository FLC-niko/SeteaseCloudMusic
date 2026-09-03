package com.example.seteasecloudmusic.feature.artist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistAlbum
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistDescription
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistDetailInfo
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistSongOrder
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistSummary
import com.example.seteasecloudmusic.feature.artist.domain.usecase.GetArtistAlbumsUseCase
import com.example.seteasecloudmusic.feature.artist.domain.usecase.GetArtistDescriptionUseCase
import com.example.seteasecloudmusic.feature.artist.domain.usecase.GetArtistDetailUseCase
import com.example.seteasecloudmusic.feature.artist.domain.usecase.GetArtistSongsUseCase
import com.example.seteasecloudmusic.feature.artist.domain.usecase.GetSimilarArtistsUseCase
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
    val albums: List<ArtistAlbum> = emptyList(),
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
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val getArtistDetailUseCase: GetArtistDetailUseCase,
    private val getArtistDescriptionUseCase: GetArtistDescriptionUseCase,
    private val getArtistSongsUseCase: GetArtistSongsUseCase,
    private val getArtistAlbumsUseCase: GetArtistAlbumsUseCase,
    private val getSimilarArtistsUseCase: GetSimilarArtistsUseCase,
    private val musicPlayerController: MusicPlayerController
) : ViewModel() {
    companion object {
        const val PREVIEW_LIMIT = 8
        private const val SONGS_PAGE_LIMIT = 50
        private const val ALBUMS_PAGE_LIMIT = 30
    }

    private val _uiState = MutableStateFlow(ArtistDetailUiState())
    val uiState = _uiState.asStateFlow()

    private var currentArtistId: Long? = null
    private var artistRequestId = 0L
    private var initialLoadJob: Job? = null
    private var songsLoadMoreJob: Job? = null
    private var albumsLoadMoreJob: Job? = null
    private var similarLoadMoreJob: Job? = null

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
            return
        }

        val requestId = ++artistRequestId
        currentArtistId = artistId
        initialLoadJob?.cancel()
        songsLoadMoreJob?.cancel()
        albumsLoadMoreJob?.cancel()
        similarLoadMoreJob?.cancel()

        _uiState.value = ArtistDetailUiState(
            artistId = artistId,
            artistName = fallbackName,
            artistCoverUrl = fallbackCoverUrl,
            isLoading = true
        )

        initialLoadJob = viewModelScope.launch {
            val detailDeferred = async { getArtistDetailUseCase(artistId) }
            val descriptionDeferred = async { getArtistDescriptionUseCase(artistId) }
            val songsDeferred = async {
                getArtistSongsUseCase(
                    artistId = artistId,
                    order = ArtistSongOrder.HOT,
                    limit = PREVIEW_LIMIT,
                    offset = 0
                )
            }
            val albumsDeferred = async {
                getArtistAlbumsUseCase(
                    artistId = artistId,
                    limit = PREVIEW_LIMIT,
                    offset = 0
                )
            }
            val similarDeferred = async { getSimilarArtistsUseCase(artistId) }

            val detailResult = detailDeferred.await()
            val descriptionResult = descriptionDeferred.await()
            val songsResult = songsDeferred.await()
            val albumsResult = albumsDeferred.await()
            val similarResult = similarDeferred.await()

            if (!isActive || requestId != artistRequestId) {
                return@launch
            }

            val detail = detailResult.getOrNull()
            val description = descriptionResult.getOrNull()
            val songsPage = songsResult.getOrNull()
            val albumsPage = albumsResult.getOrNull()
            val similarArtists = similarResult.getOrNull().orEmpty()

            val firstError = listOf(
                detailResult.exceptionOrNull()?.message,
                descriptionResult.exceptionOrNull()?.message,
                songsResult.exceptionOrNull()?.message,
                albumsResult.exceptionOrNull()?.message,
                similarResult.exceptionOrNull()?.message
            ).firstOrNull { !it.isNullOrBlank() }

            _uiState.update { prev ->
                if (requestId != artistRequestId || prev.artistId != artistId) {
                    prev
                } else {
                    prev.copy(
                        artistName = detail?.name ?: prev.artistName,
                        artistCoverUrl = detail?.coverUrl ?: prev.artistCoverUrl,
                        detail = detail,
                        description = description,
                        songs = songsPage?.items ?: emptyList(),
                        songsOffset = songsPage?.nextOffset ?: 0,
                        songsHasMore = songsPage?.hasMore ?: false,
                        songsAllLoaded = !(songsPage?.hasMore ?: false),
                        albums = albumsPage?.items ?: emptyList(),
                        albumsOffset = albumsPage?.nextOffset ?: 0,
                        albumsHasMore = albumsPage?.hasMore ?: false,
                        albumsAllLoaded = !(albumsPage?.hasMore ?: false),
                        // Similar artists API does not offer paging args.
                        // Keep only preview items first; fetch full list on arrow click.
                        similarArtists = similarArtists.take(PREVIEW_LIMIT),
                        similarAllLoaded = similarArtists.size <= PREVIEW_LIMIT,
                        isLoading = false,
                        errorMessage = firstError
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
                        errorMessage = throwable.message ?: "Failed to load songs"
                    )
                }
            }
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
                        errorMessage = throwable.message ?: "Failed to load albums"
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
                        errorMessage = throwable.message ?: "Failed to load similar artists"
                    )
                }
            }
        }
    }

    fun onPlayFirstSongClick() {
        val firstSong = _uiState.value.songs.firstOrNull() ?: return
        onSongClick(firstSong)
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
}
