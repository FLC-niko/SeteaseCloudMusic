package com.example.seteasecloudmusic.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.network.NetworkMonitor
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import com.example.seteasecloudmusic.feature.artist.domain.usecase.GetAlbumDetailUseCase
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 全局专辑详情浮层状态（按触发所在的一级页面索引隔离展示）。 */
data class GlobalAlbumOverlayState(
    val pageIndex: Int,
    val detail: PlaylistDetail,
    val isLoading: Boolean
)

/**
 * 宿主导航层 ViewModel：向界面透传全局网络状态与断网提示弹窗状态，并转发重试与关闭操作，
 * 同时承载从歌曲操作菜单打开的全局专辑详情浮层。
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    val networkMonitor: NetworkMonitor,
    private val getAlbumDetailUseCase: GetAlbumDetailUseCase,
    private val musicPlayerController: MusicPlayerController
) : ViewModel() {

    val showOfflineDialog: StateFlow<Boolean> = networkMonitor.showOfflineDialog
    val isRetrying: StateFlow<Boolean> = networkMonitor.isRetrying
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
    val dialogTitle: StateFlow<String> = networkMonitor.dialogTitle
    val dialogDescription: StateFlow<String> = networkMonitor.dialogDescription

    private val _albumOverlayState = MutableStateFlow<GlobalAlbumOverlayState?>(null)
    val albumOverlayState: StateFlow<GlobalAlbumOverlayState?> = _albumOverlayState.asStateFlow()

    private var albumDetailJob: Job? = null
    private var albumRequestId = 0L

    companion object {
        // 专辑详情复用歌单视图时使用的负向 ID 偏移量，避免与真实歌单 ID 冲突
        private const val SYNTHETIC_ALBUM_PLAYLIST_OFFSET = 1_000_000_000L
    }

    fun retry() {
        networkMonitor.triggerRetry()
    }

    fun dismissOfflineDialog() {
        networkMonitor.dismissOfflineDialog()
    }

    /** 打开指定专辑详情浮层并加载专辑曲目。 */
    fun openAlbumDetail(
        pageIndex: Int,
        albumId: Long,
        title: String,
        artistName: String,
        coverUrl: String?
    ) {
        if (albumId <= 0L) return
        albumDetailJob?.cancel()
        val requestId = ++albumRequestId
        val syntheticAlbumId = -albumId - SYNTHETIC_ALBUM_PLAYLIST_OFFSET
        _albumOverlayState.value = GlobalAlbumOverlayState(
            pageIndex = pageIndex,
            detail = PlaylistDetail(
                id = syntheticAlbumId,
                name = title,
                coverUrl = coverUrl,
                description = null,
                trackCount = 0,
                playCount = 0L,
                creatorName = artistName.takeIf { it.isNotBlank() },
                tracks = emptyList()
            ),
            isLoading = true
        )

        albumDetailJob = viewModelScope.launch {
            val result = getAlbumDetailUseCase(albumId)
            if (!isActive || requestId != albumRequestId) return@launch
            result.fold(
                onSuccess = { detail ->
                    _albumOverlayState.update { current ->
                        if (current?.detail?.id == syntheticAlbumId) {
                            current.copy(
                                detail = PlaylistDetail(
                                    id = syntheticAlbumId,
                                    name = detail.album.title.ifBlank { title },
                                    coverUrl = detail.album.coverUrl ?: coverUrl,
                                    description = detail.description,
                                    trackCount = detail.tracks.size,
                                    playCount = 0L,
                                    creatorName = detail.artistName.ifBlank { artistName.ifBlank { null } },
                                    tracks = detail.tracks
                                ),
                                isLoading = false
                            )
                        } else {
                            current
                        }
                    }
                },
                onFailure = {
                    _albumOverlayState.update { current ->
                        if (current?.detail?.id == syntheticAlbumId) {
                            current.copy(isLoading = false)
                        } else {
                            current
                        }
                    }
                }
            )
        }
    }

    /** 关闭全局专辑详情浮层（传入 [pageIndex] 时仅关闭对应页面的浮层）。 */
    fun closeAlbumDetail(pageIndex: Int? = null) {
        val current = _albumOverlayState.value ?: return
        if (pageIndex == null || current.pageIndex == pageIndex) {
            albumDetailJob?.cancel()
            albumDetailJob = null
            albumRequestId += 1L
            _albumOverlayState.value = null
        }
    }

    /** 播放全局专辑详情中的曲目列表。 */
    fun playAlbumTracks(tracks: List<Track>, startIndex: Int = 0) {
        val distinct = tracks.distinctBy { it.id }
        if (distinct.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, distinct.lastIndex)
        musicPlayerController.replaceQueueAndPlay(distinct, safeIndex)
    }

    override fun onCleared() {
        albumDetailJob?.cancel()
        super.onCleared()
    }
}

