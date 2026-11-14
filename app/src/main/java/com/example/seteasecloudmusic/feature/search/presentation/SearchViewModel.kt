package com.example.seteasecloudmusic.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seteasecloudmusic.core.common.toUserFriendlyMessage
import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.feature.artist.domain.usecase.GetAlbumDetailUseCase
import com.example.seteasecloudmusic.feature.artist.domain.usecase.GetArtistDetailUseCase
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.mine.domain.usecase.GetPlaylistDetailUseCase
import com.example.seteasecloudmusic.feature.search.data.SearchHistoryManager
import com.example.seteasecloudmusic.feature.search.domain.HotSearchItem
import com.example.seteasecloudmusic.feature.search.domain.PlaylistSuggestion
import com.example.seteasecloudmusic.feature.search.domain.SearchRepository
import com.example.seteasecloudmusic.feature.search.domain.SearchSuggestions
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.search.domain.GetSearchSuggestionsUseCase
import com.example.seteasecloudmusic.feature.search.domain.SearchMusicUseCase
import com.example.seteasecloudmusic.feature.search.domain.usecase.GetHotSearchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 搜索页 UI 状态：
 * 1. `query` 负责输入框内容。
 * 2. `suggestions` 负责联想建议列表，与正式搜索结果分开维护。
 * 3. `tracks` 承载用户“提交搜索”后的歌曲结果。
 * 4. `artists` 承载用户“提交搜索”后的歌手结果（含真实头像）。
 * 5. `playlists` 承载用户“提交搜索”后的歌单结果。
 * 6. `searchHistory` 本地持久化搜索历史。
 * 7. `hotSearches` 热门搜索列表。
 * 8. `isSearchSubmitted` 标记用户是否已触发正式搜索（点击回车/点击建议/点击热搜/点击历史），
 *    区分“即时联想状态”与“搜索结果展示状态”。
 */
data class SearchUiState(
    val query: String = "",
    val suggestions: SearchSuggestions = SearchSuggestions(),
    val isSuggestionLoading: Boolean = false,
    val suggestionErrorMessage: String? = null,
    val isLoading: Boolean = false,
    val tracks: List<Track> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<PlaylistSuggestion> = emptyList(),
    val errorMessage: String? = null,
    val searchHistory: List<String> = emptyList(),
    val hotSearches: List<HotSearchItem> = emptyList(),
    val isHotSearchLoading: Boolean = false,
    val isSearchSubmitted: Boolean = false,
    val activePlaylistDetail: PlaylistDetail? = null,
    val isLoadingDetail: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMusicUseCase: SearchMusicUseCase,
    private val getSearchSuggestionsUseCase: GetSearchSuggestionsUseCase,
    private val getHotSearchUseCase: GetHotSearchUseCase,
    private val searchHistoryManager: SearchHistoryManager,
    private val searchRepository: SearchRepository,
    private val getArtistDetailUseCase: GetArtistDetailUseCase,
    private val getPlaylistDetailUseCase: GetPlaylistDetailUseCase,
    private val getAlbumDetailUseCase: GetAlbumDetailUseCase,
    val musicPlayerController: MusicPlayerController
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()
    val playbackState = musicPlayerController.playbackState

    private var lastSubmittedQuery: String? = null
    // 正式搜索和联想建议各自维护独立 Job，避免互相取消或状态覆盖。
    private var searchJob: Job? = null
    private var suggestionJob: Job? = null
    private var detailJob: Job? = null
    private var searchRequestId = 0L
    private var suggestionRequestId = 0L
    private var detailRequestId = 0L

    companion object {
        // 输入联想不需要每次敲字都立刻请求，做一层轻量防抖更节省接口调用。
        private const val SEARCH_SUGGEST_DEBOUNCE_MS = 250L
        // 专辑使用独立负向 ID 空间映射到 PlaylistDetail，避免与真实歌单 ID 冲突。
        private const val SYNTHETIC_ALBUM_PLAYLIST_OFFSET = 1_000_000_000L
    }

    init {
        // 观察本地持久化搜索历史变更
        viewModelScope.launch {
            searchHistoryManager.historyFlow.collect { history ->
                _uiState.update { it.copy(searchHistory = history) }
            }
        }
        // 加载热搜列表
        loadHotSearches()
    }

    /**
     * 获取热搜列表。
     */
    fun loadHotSearches() {
        viewModelScope.launch {
            _uiState.update { it.copy(isHotSearchLoading = true) }
            val result = getHotSearchUseCase()
            result.fold(
                onSuccess = { items ->
                    _uiState.update {
                        it.copy(
                            hotSearches = items,
                            isHotSearchLoading = false
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(isHotSearchLoading = false)
                    }
                }
            )
        }
    }

    /**
     * 处理搜索输入框内容变化。
     *
     * 当输入变化时会同步更新输入态、重置上一轮建议态；
     * 当输入为空时会清空相关状态并停止建议请求；
     * 当输入非空时会触发新的联想建议请求（即时搜索建议）。
     *
     * @param newQuery 输入框当前文本（保留原始输入展示）。
     */
    fun onQueryChanged(newQuery: String) {
        val normalizedQuery = newQuery.trim()

        // 输入变化后，如果上一次搜索还在进行且关键词已变，就取消旧请求，避免结果错位。
        if (uiState.value.isLoading && normalizedQuery != lastSubmittedQuery) {
            searchJob?.cancel()
            searchJob = null
        }

        // 输入变化后先更新输入态，并重置提交状态（进入建议模式）
        detailJob?.cancel()
        detailJob = null
        detailRequestId += 1L
        _uiState.update { state ->
            if (normalizedQuery.isEmpty()) {
                state.copy(
                    query = newQuery,
                    suggestions = SearchSuggestions(),
                    isSuggestionLoading = false,
                    suggestionErrorMessage = null,
                    tracks = emptyList(),
                    artists = emptyList(),
                    playlists = emptyList(),
                    errorMessage = null,
                    isLoading = false,
                    isSearchSubmitted = false,
                    activePlaylistDetail = null,
                    isLoadingDetail = false
                )
            } else {
                state.copy(
                    query = newQuery,
                    suggestions = SearchSuggestions(),
                    isSuggestionLoading = false,
                    suggestionErrorMessage = null,
                    errorMessage = null,
                    isLoading = false,
                    isSearchSubmitted = false,
                    activePlaylistDetail = null,
                    isLoadingDetail = false
                )
            }
        }

        // 输入被清空时，不再请求建议，同时重置与当前输入绑定的可重试关键词。
        if (normalizedQuery.isEmpty()) {
            searchRequestId += 1L
            suggestionJob?.cancel()
            suggestionJob = null
            suggestionRequestId += 1L
            lastSubmittedQuery = null
            return
        }

        // 即时获取搜索建议
        requestSearchSuggestions(normalizedQuery)
    }

    /**
     * 处理用户提交正式搜索（例如点击搜索按钮或键盘搜索）。
     *
     * 仅在关键词非空时执行：记录历史、收起联想建议，再记录本次提交关键词并发起正式搜索。
     */
    fun onSearchSubmit() {
        val query = uiState.value.query.trim()
        if (query.isEmpty()) {
            return
        }

        searchHistoryManager.addHistory(query)
        closePlaylistDetail()
        clearSearchSuggestions()
        lastSubmittedQuery = query
        _uiState.update { it.copy(isSearchSubmitted = true) }
        search(query)
    }

    /**
     * 处理用户点击联想建议词。
     *
     * 将建议词写入历史并写回输入框后，复用正式搜索流程直接搜索该关键词。
     *
     * @param keyword 用户点击的建议词。
     */
    fun onSuggestionClick(keyword: String) {
        val query = keyword.trim()
        if (query.isEmpty()) {
            return
        }

        searchHistoryManager.addHistory(query)
        closePlaylistDetail()
        _uiState.update { state ->
            state.copy(
                query = query,
                suggestionErrorMessage = null,
                isSearchSubmitted = true
            )
        }

        clearSearchSuggestions()
        lastSubmittedQuery = query
        search(query)
    }

    /**
     * 处理用户点击热搜榜项。
     */
    fun onHotSearchClick(searchWord: String) {
        onSuggestionClick(searchWord)
    }

    /**
     * 处理用户点击历史搜索词。
     */
    fun onHistoryClick(historyWord: String) {
        onSuggestionClick(historyWord)
    }

    /**
     * 清空全部搜索历史。
     */
    fun onClearHistory() {
        searchHistoryManager.clearHistory()
    }

    /**
     * 删除单条搜索历史。
     */
    fun onRemoveHistory(keyword: String) {
        searchHistoryManager.removeHistory(keyword)
    }

    /**
     * 处理用户点击歌曲：以当前搜索结果快照形成队列，
     * 从点击项开始顺序播放。
     */
    fun onTrackClick(track: Track) {
        val snapshotTracks = uiState.value.tracks
            .distinctBy { it.id }

        if (snapshotTracks.isEmpty()) {
            _uiState.update { state ->
                state.copy(errorMessage = "当前没有可播放的搜索结果")
            }
            return
        }

        val clickedIndex = snapshotTracks.indexOfFirst { it.id == track.id }
        if (clickedIndex !in snapshotTracks.indices) {
            _uiState.update { state ->
                state.copy(errorMessage = "未找到所选歌曲")
            }
            return
        }

        musicPlayerController.replaceQueueAndPlay(snapshotTracks, clickedIndex)
    }

    /**
     * 发起正式搜索请求并更新搜索结果状态。
     *
     * 会先取消上一次未完成的正式搜索，避免旧结果覆盖新结果；
     * 请求期间进入加载态，请求完成后按成功/失败更新结果和错误信息。
     *
     * @param query 本次正式搜索关键词。
     */
    private fun search(query: String) {
        // 1) 先取消上一次搜索，避免“旧请求后返回”把新结果覆盖掉。
        searchJob?.cancel()
        val requestId = ++searchRequestId

        searchJob = viewModelScope.launch {
            // 2) 发请求前先进入加载态，并清空旧错误。
            _uiState.update { state ->
                state.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            // 3) 并发请求单曲、歌手、歌单（歌手结果自带真实头像）
            val tracksDeferred = async { searchMusicUseCase(query) }
            val artistsDeferred = async { searchRepository.searchArtists(query) }
            val playlistsDeferred = async { searchRepository.searchPlaylists(query) }

            val tracksResult = tracksDeferred.await()
            val artistsResult = artistsDeferred.await()
            val playlistsResult = playlistsDeferred.await()

            if (!isActive || requestId != searchRequestId || uiState.value.query.trim() != query) {
                return@launch
            }

            val tracks = tracksResult.getOrElse { emptyList() }
            val artists = artistsResult.getOrElse { emptyList() }
            val playlists = playlistsResult.getOrElse { emptyList() }

            // 4) 按成功/失败分别更新 UI 状态。
            if (tracks.isEmpty() && artists.isEmpty() && playlists.isEmpty() && tracksResult.isFailure) {
                val throwable = tracksResult.exceptionOrNull() ?: Exception("搜索失败")
                if (requestId == searchRequestId) {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            tracks = emptyList(),
                            artists = emptyList(),
                            playlists = emptyList(),
                            errorMessage = throwable.toUserFriendlyMessage("搜索")
                        )
                    }
                }
            } else {
                if (requestId == searchRequestId) {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            tracks = tracks,
                            artists = artists,
                            playlists = playlists,
                            errorMessage = null
                        )
                    }
                }

                // 5) 为歌曲中可能缺少头像的歌手做异步兜底补充
                val missingArtistIds = tracks
                    .flatMap { it.artists }
                    .filter { it.coverUrl.isNullOrBlank() && it.id > 0L }
                    .map { it.id }
                    .distinct()
                    .filter { id -> artists.none { it.id == id && !it.coverUrl.isNullOrBlank() } }
                    .take(8)

                if (missingArtistIds.isNotEmpty()) {
                    launch {
                        val supplemented = artists.toMutableList()
                        var updated = false
                        for (id in missingArtistIds) {
                            if (!isActive || requestId != searchRequestId) break
                            val detail = getArtistDetailUseCase(id).getOrNull()
                            val cover = detail?.coverUrl
                            if (!cover.isNullOrBlank()) {
                                updated = true
                                val idx = supplemented.indexOfFirst { it.id == id }
                                if (idx >= 0) {
                                    supplemented[idx] = supplemented[idx].copy(coverUrl = cover)
                                } else {
                                    val name = tracks.flatMap { it.artists }.firstOrNull { it.id == id }?.name ?: detail.name
                                    supplemented.add(Artist(id = id, name = name, coverUrl = cover))
                                }
                            }
                        }
                        if (updated && isActive && requestId == searchRequestId) {
                            _uiState.update { it.copy(artists = supplemented) }
                        }
                    }
                }
            }
        }
    }

    /**
     * 处理搜索失败后的重试动作。
     *
     * 优先使用最近一次提交关键词重试；若不存在则回退到当前输入框关键词。
     * 当无可用关键词时仅提示错误，不发起请求。
     */
    fun onRetryClick() {
        // 正在搜索时不重复发起请求，避免并发重试。
        if (uiState.value.isLoading) {
            return
        }

        // 优先使用上次提交的关键词；若为空则回退到当前输入框内容。
        val retryQuery = lastSubmittedQuery
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: uiState.value.query.trim()

        // 仍为空时给出提示，不发请求。
        if (retryQuery.isEmpty()) {
            _uiState.update { state ->
                state.copy(errorMessage = "没有可重试的关键词")
            }
            return
        }

        // 记录本次重试关键词，并复用统一搜索流程。
        clearSearchSuggestions()
        lastSubmittedQuery = retryQuery
        search(retryQuery)
    }

    /**
     * 清空搜索输入与页面状态。
     *
     * 会同时取消正在进行的正式搜索和联想请求，重置可重试关键词，
     * 并将页面恢复为初始空态。
     */
    fun onClearQuery() {
        // 1) 清空时先取消正在进行的请求，避免返回结果覆盖清空后的页面状态。
        searchJob?.cancel()
        searchJob = null
        searchRequestId += 1L
        suggestionJob?.cancel()
        suggestionJob = null
        suggestionRequestId += 1L

        // 2) 重置“可重试关键词”，确保后续重试行为与当前输入一致。
        lastSubmittedQuery = null

        // 3) 回到初始 UI 状态：输入为空、非加载、无结果、无错误、未提交搜索。
        detailJob?.cancel()
        detailJob = null
        detailRequestId += 1L
        _uiState.update { state ->
            state.copy(
                query = "",
                suggestions = SearchSuggestions(),
                isSuggestionLoading = false,
                suggestionErrorMessage = null,
                isLoading = false,
                tracks = emptyList(),
                artists = emptyList(),
                playlists = emptyList(),
                errorMessage = null,
                isSearchSubmitted = false,
                activePlaylistDetail = null,
                isLoadingDetail = false
            )
        }
    }

    /**
     * 打开搜索结果中的歌单详情页。
     */
    fun openPlaylistDetail(
        playlistId: Long,
        name: String,
        trackCount: Int,
        coverUrl: String?
    ) {
        if (playlistId <= 0L) return
        detailJob?.cancel()
        val requestId = ++detailRequestId

        val cached = getPlaylistDetailUseCase.getCachedPreview(playlistId)
        val instantPreview = cached ?: PlaylistDetail(
            id = playlistId,
            name = name,
            coverUrl = coverUrl,
            description = null,
            trackCount = trackCount,
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

        detailJob = viewModelScope.launch {
            val result = getPlaylistDetailUseCase(playlistId)
            if (!isActive || requestId != detailRequestId) return@launch
            result.fold(
                onSuccess = { detail ->
                    if (requestId != detailRequestId) return@fold
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
                    if (requestId != detailRequestId) return@fold
                    _uiState.update { current ->
                        if (current.activePlaylistDetail?.id == playlistId) {
                            current.copy(isLoadingDetail = false)
                        } else current
                    }
                }
            )
        }
    }

    /**
     * 打开搜索结果中的专辑详情页（复用 PlaylistDetailScreen 展示）。
     */
    fun openAlbumDetail(
        albumId: Long,
        title: String,
        artistName: String,
        coverUrl: String?
    ) {
        detailJob?.cancel()
        val requestId = ++detailRequestId

        if (albumId <= 0L) {
            val matchingTracks = uiState.value.tracks
                .filter { it.album.title == title }
                .distinctBy { it.id }
            if (matchingTracks.isEmpty()) return
            val syntheticId = -(title.hashCode().toLong() and 0x7FFFFFFFL) - SYNTHETIC_ALBUM_PLAYLIST_OFFSET
            _uiState.update {
                it.copy(
                    activePlaylistDetail = PlaylistDetail(
                        id = syntheticId,
                        name = title,
                        coverUrl = coverUrl ?: matchingTracks.firstOrNull()?.coverUrl,
                        description = null,
                        trackCount = matchingTracks.size,
                        playCount = 0L,
                        creatorName = artistName.takeIf { name -> name.isNotBlank() },
                        tracks = matchingTracks
                    ),
                    isLoadingDetail = false
                )
            }
            return
        }

        val syntheticAlbumId = -albumId - SYNTHETIC_ALBUM_PLAYLIST_OFFSET
        _uiState.update {
            it.copy(
                activePlaylistDetail = PlaylistDetail(
                    id = syntheticAlbumId,
                    name = title,
                    coverUrl = coverUrl,
                    description = null,
                    trackCount = 0,
                    playCount = 0L,
                    creatorName = artistName.takeIf { name -> name.isNotBlank() },
                    tracks = emptyList()
                ),
                isLoadingDetail = true
            )
        }

        detailJob = viewModelScope.launch {
            val result = getAlbumDetailUseCase(albumId)
            if (!isActive || requestId != detailRequestId) return@launch
            result.fold(
                onSuccess = { detail ->
                    if (requestId != detailRequestId) return@fold
                    _uiState.update { current ->
                        if (current.activePlaylistDetail?.id == syntheticAlbumId) {
                            current.copy(
                                activePlaylistDetail = PlaylistDetail(
                                    id = syntheticAlbumId,
                                    name = detail.album.title.ifBlank { title },
                                    coverUrl = detail.album.coverUrl ?: coverUrl,
                                    description = detail.description,
                                    trackCount = detail.tracks.size,
                                    playCount = 0L,
                                    creatorName = detail.artistName.ifBlank { artistName.ifBlank { null } },
                                    tracks = detail.tracks
                                ),
                                isLoadingDetail = false
                            )
                        } else current
                    }
                },
                onFailure = {
                    if (requestId != detailRequestId) return@fold
                    _uiState.update { current ->
                        if (current.activePlaylistDetail?.id == syntheticAlbumId) {
                            current.copy(isLoadingDetail = false)
                        } else current
                    }
                }
            )
        }
    }

    /**
     * 关闭当前打开的歌单/专辑详情页。
     */
    fun closePlaylistDetail() {
        detailJob?.cancel()
        detailJob = null
        detailRequestId += 1L
        _uiState.update { it.copy(activePlaylistDetail = null, isLoadingDetail = false) }
    }

    /**
     * 播放歌单或专辑详情中的曲目列表。
     */
    fun playTrackList(tracks: List<Track>, startIndex: Int = 0) {
        val distinct = tracks.distinctBy { it.id }
        if (distinct.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, distinct.lastIndex)
        musicPlayerController.replaceQueueAndPlay(distinct, safeIndex)
    }

    /**
     * 请求联想建议并更新建议状态。
     *
     * 内部包含防抖逻辑，且在请求返回后会校验输入是否仍匹配，
     * 防止慢请求导致建议列表与当前输入错位。
     *
     * @param query 用于请求联想建议的关键词。
     */
    private fun requestSearchSuggestions(query: String) {
        // 用户继续输入时，旧的联想请求已经失效，直接取消即可。
        suggestionJob?.cancel()
        val requestId = ++suggestionRequestId

        suggestionJob = viewModelScope.launch {
            // 对输入做轻量防抖，减少每次敲字都直连接口。
            delay(SEARCH_SUGGEST_DEBOUNCE_MS)

            _uiState.update { state ->
                state.copy(
                    isSuggestionLoading = true,
                    suggestionErrorMessage = null
                )
            }

            val result = getSearchSuggestionsUseCase(query = query, type = "mobile")

            // 用户已经继续输入时，丢弃旧建议，避免列表与输入框内容不一致。
            if (!isActive || requestId != suggestionRequestId || uiState.value.query.trim() != query) {
                return@launch
            }

            result.fold(
                onSuccess = { suggestions ->
                    if (requestId == suggestionRequestId) {
                        _uiState.update { state ->
                            state.copy(
                                suggestions = suggestions,
                                isSuggestionLoading = false,
                                suggestionErrorMessage = null
                            )
                        }
                    }
                },
                onFailure = { throwable ->
                    if (requestId == suggestionRequestId) {
                        _uiState.update { state ->
                            state.copy(
                                suggestions = SearchSuggestions(),
                                isSuggestionLoading = false,
                                suggestionErrorMessage = throwable.toUserFriendlyMessage("搜索建议加载")
                            )
                        }
                    }
                }
            )
        }
    }

    /**
     * 清理联想建议相关状态。
     *
     * 取消联想请求并清空建议列表、建议加载态及建议错误信息。
     */
    private fun clearSearchSuggestions() {
        suggestionJob?.cancel()
        suggestionJob = null
        suggestionRequestId += 1L
        _uiState.update { state ->
            state.copy(
                suggestions = SearchSuggestions(),
                isSuggestionLoading = false,
                suggestionErrorMessage = null
            )
        }
    }

    /**
     * 迷你播放器的播放/暂停按钮。
     */
    fun onMiniPlayerPlayPause() {
        when (playbackState.value.status) {
            PlayerStatus.PLAYING -> musicPlayerController.pause()
            PlayerStatus.PAUSED -> musicPlayerController.resume()
            PlayerStatus.BUFFERING -> Unit
            PlayerStatus.IDLE,
            PlayerStatus.ENDED,
            PlayerStatus.ERROR -> musicPlayerController.replayCurrent()
        }
    }

    /**
     * 迷你播放器的下一首按钮。
     */
    fun onMiniPlayerNext() {
        musicPlayerController.playNext()
    }

    override fun onCleared() {
        searchJob?.cancel()
        suggestionJob?.cancel()
        detailJob?.cancel()
        super.onCleared()
    }
}
