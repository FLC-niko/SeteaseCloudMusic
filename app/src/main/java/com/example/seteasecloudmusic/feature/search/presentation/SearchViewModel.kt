package com.example.seteasecloudmusic.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.feature.search.domain.SearchSuggestions
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.search.domain.GetSearchSuggestionsUseCase
import com.example.seteasecloudmusic.feature.search.domain.SearchMusicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
 * 3. `tracks` 只承载用户“提交搜索”后的结果，避免和建议数据混用。
 */
data class SearchUiState(
    val query: String = "",
    val suggestions: SearchSuggestions = SearchSuggestions(),
    val isSuggestionLoading: Boolean = false,
    val suggestionErrorMessage: String? = null,
    val isLoading: Boolean = false,
    val tracks: List<Track> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMusicUseCase: SearchMusicUseCase,
    private val getSearchSuggestionsUseCase: GetSearchSuggestionsUseCase,
    val musicPlayerController: MusicPlayerController
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()
    val playbackState = musicPlayerController.playbackState

    private var lastSubmittedQuery: String? = null
    // 正式搜索和联想建议各自维护独立 Job，避免互相取消或状态覆盖。
    private var searchJob: Job? = null
    private var suggestionJob: Job? = null
    private var searchRequestId = 0L
    private var suggestionRequestId = 0L

    companion object {
        // 输入联想不需要每次敲字都立刻请求，做一层轻量防抖更节省接口调用。
        private const val SEARCH_SUGGEST_DEBOUNCE_MS = 300L
        // 输入时自动搜索的防抖，比建议稍长一点，减少接口调用。
        private const val AUTO_SEARCH_DEBOUNCE_MS = 500L
    }

    /**
     * 处理搜索输入框内容变化。
     *
     * 当输入变化时会同步更新输入态、重置上一轮建议态；
     * 当输入为空时会清空相关状态并停止建议请求；
     * 当输入非空时会触发新的联想建议请求。
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

        // 输入变化后先更新输入态，并清空上一轮建议的加载/错误，等待新的建议请求返回。
        _uiState.update { state ->
            if (normalizedQuery.isEmpty()) {
                state.copy(
                    query = newQuery,
                    suggestions = SearchSuggestions(),
                    isSuggestionLoading = false,
                    suggestionErrorMessage = null,
                    tracks = emptyList(),
                    errorMessage = null,
                    isLoading = false
                )
            } else {
                state.copy(
                    query = newQuery,
                    suggestions = SearchSuggestions(),
                    isSuggestionLoading = false,
                    suggestionErrorMessage = null,
                    errorMessage = null,
                    isLoading = false
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

        requestSearchSuggestions(normalizedQuery)
        requestAutoSearch(normalizedQuery)
    }

    /**
     * 处理用户提交正式搜索（例如点击搜索按钮或键盘搜索）。
     *
     * 仅在关键词非空时执行：先收起联想建议，再记录本次提交关键词并发起正式搜索。
     */
    fun onSearchSubmit() {
        val query = uiState.value.query.trim()
        if (query.isEmpty()) {
            return
        }

        clearSearchSuggestions()
        lastSubmittedQuery = query
        search(query)
    }

    /**
     * 处理用户点击联想建议词。
     *
     * 将建议词写回输入框后，复用正式搜索流程直接搜索该关键词。
     *
     * @param keyword 用户点击的建议词。
     */
    fun onSuggestionClick(keyword: String) {
        val query = keyword.trim()
        if (query.isEmpty()) {
            return
        }

        // 点击建议后，把建议词写回输入框，并直接复用正式搜索流程。
        _uiState.update { state ->
            state.copy(
                query = query,
                suggestionErrorMessage = null
            )
        }

        clearSearchSuggestions()
        lastSubmittedQuery = query
        search(query)
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

            // 3) 调用 UseCase。它会返回 Result<List<Track>>。
            val result = searchMusicUseCase(query)

            if (!isActive || requestId != searchRequestId || uiState.value.query.trim() != query) {
                return@launch
            }

            // 4) 按成功/失败分别更新 UI 状态。
            result.fold(
                onSuccess = { tracks ->
                    if (requestId == searchRequestId) {
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                tracks = tracks,
                                errorMessage = null
                            )
                        }
                    }
                },
                onFailure = { throwable ->
                    if (requestId == searchRequestId) {
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                tracks = emptyList(),
                                errorMessage = throwable.message ?: "搜索失败，请稍后重试"
                            )
                        }
                    }
                }
            )
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

        // 3) 回到初始 UI 状态：输入为空、非加载、无结果、无错误。
        _uiState.update { state ->
            state.copy(
                query = "",
                suggestions = SearchSuggestions(),
                isSuggestionLoading = false,
                suggestionErrorMessage = null,
                isLoading = false,
                tracks = emptyList(),
                errorMessage = null
            )
        }
    }

    /**
     * 输入变化时自动发起搜索（cloudsearch）。
     *
     * 带防抖处理，避免每次敲字都请求；
     * 返回后会校验输入是否仍匹配，防止慢请求覆盖新结果。
     */
    private fun requestAutoSearch(query: String) {
        searchJob?.cancel()
        val requestId = ++searchRequestId

        searchJob = viewModelScope.launch {
            delay(AUTO_SEARCH_DEBOUNCE_MS)

            if (!isActive || requestId != searchRequestId || uiState.value.query.trim() != query) {
                return@launch
            }

            _uiState.update { state ->
                state.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            val result = searchMusicUseCase(query)

            if (!isActive || requestId != searchRequestId || uiState.value.query.trim() != query) {
                return@launch
            }

            result.fold(
                onSuccess = { tracks ->
                    if (requestId == searchRequestId) {
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                tracks = tracks,
                                errorMessage = null
                            )
                        }
                    }
                },
                onFailure = { throwable ->
                    if (requestId == searchRequestId) {
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                errorMessage = throwable.message ?: "搜索失败，请稍后重试"
                            )
                        }
                    }
                }
            )
        }
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
                                suggestionErrorMessage = throwable.message ?: "搜索建议加载失败，请稍后重试"
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
        super.onCleared()
    }
}
