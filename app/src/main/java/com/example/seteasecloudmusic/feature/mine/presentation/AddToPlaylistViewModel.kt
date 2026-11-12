package com.example.seteasecloudmusic.feature.mine.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seteasecloudmusic.core.auth.AuthStateProvider
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.FavoriteTracksManager
import com.example.seteasecloudmusic.feature.mine.domain.model.UserPlaylist
import com.example.seteasecloudmusic.feature.mine.domain.repository.MineRepository
import com.example.seteasecloudmusic.feature.mine.domain.repository.PlaylistTrackAlreadyExistsException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddToPlaylistUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val isOperating: Boolean = false,
    val likedPlaylist: UserPlaylist? = null,
    val createdPlaylists: List<UserPlaylist> = emptyList(),
    val existingPlaylistIds: Set<Long> = emptySet(),
    val errorMessage: String? = null
)

@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    private val authStateProvider: AuthStateProvider,
    private val mineRepository: MineRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddToPlaylistUiState())
    val uiState: StateFlow<AddToPlaylistUiState> = _uiState.asStateFlow()

    private var currentTrackId: Long = 0L
    private var checkJob: Job? = null

    init {
        loadPlaylists()
    }

    fun loadPlaylists() {
        viewModelScope.launch {
            authStateProvider.observeAuthState().collectLatest { session ->
                if (session?.isLoggedIn == true && session.userId != null) {
                    _uiState.update { it.copy(isLoggedIn = true, isLoading = true, errorMessage = null) }
                    val cached = mineRepository.getCachedUserPlaylists(session.userId)
                    if (cached != null) {
                        _uiState.update {
                            it.copy(
                                likedPlaylist = cached.likedPlaylist,
                                createdPlaylists = cached.createdPlaylists,
                                isLoading = false
                            )
                        }
                    }
                    val result = mineRepository.getUserPlaylists(session.userId)
                    result.fold(
                        onSuccess = { group ->
                            _uiState.update {
                                it.copy(
                                    likedPlaylist = group.likedPlaylist,
                                    createdPlaylists = group.createdPlaylists,
                                    isLoading = false
                                )
                            }
                            if (currentTrackId > 0L) {
                                checkTrackExistence(currentTrackId)
                            }
                        },
                        onFailure = { err ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = if (it.createdPlaylists.isEmpty() && it.likedPlaylist == null) err.message else null
                                )
                            }
                        }
                    )
                } else {
                    _uiState.update {
                        it.copy(
                            isLoggedIn = false,
                            isLoading = false,
                            likedPlaylist = null,
                            createdPlaylists = emptyList(),
                            existingPlaylistIds = emptySet()
                        )
                    }
                }
            }
        }
    }

    /**
     * 为目标单曲初始化“已添加”状态：包括当前打开的歌单、我喜欢的音乐以及已检测出的自建歌单
     */
    fun initForTrack(trackId: Long, currentPlaylistId: Long?) {
        checkJob?.cancel()
        currentTrackId = trackId
        val existing = mutableSetOf<Long>()
        if (currentPlaylistId != null && currentPlaylistId > 0L) {
            existing.add(currentPlaylistId)
        }
        if (FavoriteTracksManager.isLiked(trackId)) {
            _uiState.value.likedPlaylist?.id?.let { existing.add(it) }
        }
        // 快速检查本地预览缓存
        _uiState.value.createdPlaylists.forEach { pl ->
            val cached = mineRepository.getCachedPlaylistDetailPreview(pl.id)
            if (cached?.tracks?.any { it.id == trackId } == true) {
                existing.add(pl.id)
            }
        }
        // 彻底重置为当前歌曲的已添加集合，绝不能在上一首歌的基础上累加！
        _uiState.update { it.copy(existingPlaylistIds = existing) }

        // 后台检查其余歌单中的完整曲目
        checkTrackExistence(trackId)
    }

    private fun checkTrackExistence(trackId: Long) {
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            _uiState.value.createdPlaylists.forEach { pl ->
                if (currentTrackId != trackId) return@launch
                if (pl.id !in _uiState.value.existingPlaylistIds) {
                    val result = mineRepository.getPlaylistDetail(pl.id)
                    if (currentTrackId != trackId) return@launch
                    result.getOrNull()?.let { detail ->
                        if (detail.tracks.any { it.id == trackId }) {
                            _uiState.update { current ->
                                if (currentTrackId == trackId) {
                                    current.copy(existingPlaylistIds = current.existingPlaylistIds + pl.id)
                                } else current
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long): Result<Unit> {
        _uiState.update { it.copy(isOperating = true) }
        try {
            val result = mineRepository.addTrackToPlaylist(playlistId, trackId)
            if (result.isSuccess) {
                _uiState.update { it.copy(existingPlaylistIds = it.existingPlaylistIds + playlistId) }
            } else if (result.exceptionOrNull() is PlaylistTrackAlreadyExistsException) {
                _uiState.update { it.copy(existingPlaylistIds = it.existingPlaylistIds + playlistId) }
            }
            return result
        } finally {
            _uiState.update { it.copy(isOperating = false) }
        }
    }

    suspend fun likeTrack(track: Track): Result<Boolean> {
        _uiState.update { it.copy(isOperating = true) }
        try {
            FavoriteTracksManager.setLiked(track.id, true)
            val result = mineRepository.likeTrack(track.id, true)
            _uiState.value.likedPlaylist?.id?.let { likedId ->
                _uiState.update { it.copy(existingPlaylistIds = it.existingPlaylistIds + likedId) }
            }
            return result
        } finally {
            _uiState.update { it.copy(isOperating = false) }
        }
    }

    suspend fun createPlaylistAndAddTrack(name: String, trackId: Long): Result<UserPlaylist> {
        _uiState.update { it.copy(isOperating = true) }
        try {
            val createResult = mineRepository.createPlaylist(name)
            return createResult.mapCatching { newPlaylist ->
                mineRepository.addTrackToPlaylist(newPlaylist.id, trackId).getOrThrow()
                val currentList = _uiState.value.createdPlaylists
                _uiState.update {
                    it.copy(
                        createdPlaylists = listOf(newPlaylist.copy(trackCount = 1)) + currentList,
                        existingPlaylistIds = it.existingPlaylistIds + newPlaylist.id
                    )
                }
                newPlaylist
            }
        } finally {
            _uiState.update { it.copy(isOperating = false) }
        }
    }
}
