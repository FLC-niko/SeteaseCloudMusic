package com.example.seteasecloudmusic.feature.search.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.seteasecloudmusic.core.ui.adaptive.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.R
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.ui.components.GlassSliderDefaults
import com.example.seteasecloudmusic.core.ui.components.GlassThumb
import com.example.seteasecloudmusic.core.ui.components.LocalGlassBackdrop
import com.example.seteasecloudmusic.core.ui.components.ProgressiveTopBlur
import com.example.seteasecloudmusic.core.ui.components.glassSegmentDrag
import com.example.seteasecloudmusic.core.ui.components.liquidGlass
import com.example.seteasecloudmusic.core.ui.components.rememberGlassThumbGeometry
import com.example.seteasecloudmusic.core.ui.components.verticalElasticOverscroll
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.mine.presentation.PlaylistDetailScreen
import com.example.seteasecloudmusic.feature.search.domain.HotSearchItem
import com.example.seteasecloudmusic.feature.search.domain.SearchSuggestions
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch
import java.util.Locale

private val PageBackground = Color.White
private val PrimaryText = Color(0xFF111111)
private val SecondaryText = Color(0xFF909094)
private val DividerColor = Color(0xFFE2E2E6)
private val TabActiveBg = Color(0xFFFA233B)
private val TabActiveText = Color.White

private enum class SearchTab(val title: String) {
    BEST("最佳结果"),
    SONG("歌曲"),
    ARTIST("艺人"),
    ALBUM("专辑"),
    PLAYLIST("歌单")
}

private sealed interface SearchRowItem {
    val key: String

    data class Song(
        val track: Track
    ) : SearchRowItem {
        override val key: String = "song_${track.id}"
    }

    data class Album(
        val id: Long,
        val title: String,
        val artistName: String,
        val coverUrl: String?
    ) : SearchRowItem {
        override val key: String = "album_$id"
    }

    data class Artist(
        val id: Long,
        val name: String,
        val coverUrl: String?
    ) : SearchRowItem {
        override val key: String = "artist_$id"
    }

    data class Playlist(
        val id: Long,
        val name: String,
        val trackCount: Int,
        val coverUrl: String?
    ) : SearchRowItem {
        override val key: String = "playlist_$id"
    }

    data class Suggestion(
        val keyword: String
    ) : SearchRowItem {
        override val key: String = "suggest_$keyword"
    }
}

@Composable
fun SearchRoute(
    viewModel: SearchViewModel,
    topContentPadding: Dp = 18.dp,
    bottomContentPadding: Dp = 180.dp,
    onArtistClick: (Long, String, String?) -> Unit = { _, _, _ -> },
    isForeground: Boolean = true,
    topHeader: @Composable () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SearchScreenContent(
        uiState = uiState,
        topContentPadding = topContentPadding,
        bottomContentPadding = bottomContentPadding,
        onSearchSubmit = { viewModel.onSearchSubmit() },
        onSuggestionClick = { viewModel.onSuggestionClick(it) },
        onHotSearchClick = { viewModel.onHotSearchClick(it) },
        onHistoryClick = { viewModel.onHistoryClick(it) },
        onClearHistory = { viewModel.onClearHistory() },
        onRemoveHistory = { viewModel.onRemoveHistory(it) },
        onArtistClick = onArtistClick,
        onTrackClick = { viewModel.onTrackClick(it) },
        onRetryClick = { viewModel.onRetryClick() },
        onAlbumClick = { id, title, artistName, coverUrl ->
            viewModel.openAlbumDetail(id, title, artistName, coverUrl)
        },
        onPlaylistClick = { id, name, trackCount, coverUrl ->
            viewModel.openPlaylistDetail(id, name, trackCount, coverUrl)
        },
        onClosePlaylistDetail = { viewModel.closePlaylistDetail() },
        onPlayPlaylistTrack = { track, tracks ->
            val idx = tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            viewModel.playTrackList(tracks, idx)
        },
        onPlayPlaylistAll = { tracks -> viewModel.playTrackList(tracks, 0) },
        onShufflePlaylist = { tracks -> viewModel.playTrackList(tracks.shuffled(), 0) },
        isForeground = isForeground,
        topHeader = topHeader
    )
}

@Composable
fun SearchScreenContent(
    uiState: SearchUiState,
    topContentPadding: Dp,
    bottomContentPadding: Dp = 180.dp,
    onSearchSubmit: () -> Unit = {},
    onSuggestionClick: (String) -> Unit = {},
    onHotSearchClick: (String) -> Unit = onSuggestionClick,
    onHistoryClick: (String) -> Unit = onSuggestionClick,
    onClearHistory: () -> Unit = {},
    onRemoveHistory: (String) -> Unit = {},
    onArtistClick: (Long, String, String?) -> Unit = { _, _, _ -> },
    onTrackClick: (Track) -> Unit = {},
    onRetryClick: () -> Unit = {},
    onAlbumClick: (Long, String, String, String?) -> Unit = { _, _, _, _ -> },
    onPlaylistClick: (Long, String, Int, String?) -> Unit = { _, _, _, _ -> },
    onClosePlaylistDetail: () -> Unit = {},
    onPlayPlaylistTrack: (Track, List<Track>) -> Unit = { _, _ -> },
    onPlayPlaylistAll: (List<Track>) -> Unit = {},
    onShufflePlaylist: (List<Track>) -> Unit = {},
    isForeground: Boolean = true,
    topHeader: @Composable () -> Unit = {}
) {
    val searchBackdrop = rememberLayerBackdrop {
        drawRect(PageBackground)
        drawContent()
    }

    var selectedTab by rememberSaveable(uiState.query) { mutableStateOf(SearchTab.BEST) }

    val items = remember(uiState, selectedTab) {
        buildSearchItems(uiState = uiState, selectedTab = selectedTab)
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val handleSelect = { action: () -> Unit ->
        keyboardController?.hide()
        focusManager.clearFocus()
        action()
    }
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val resultsTopPadding = topContentPadding + 58.dp

    val lastPlaylistDetail = remember { mutableStateOf<PlaylistDetail?>(null) }
    val lastOpenedPlaylistId = remember { mutableStateOf<Long?>(null) }
    val playlistOpenCounter = remember { mutableIntStateOf(0) }
    LaunchedEffect(uiState.activePlaylistDetail) {
        val current = uiState.activePlaylistDetail
        if (current != null) {
            if (lastOpenedPlaylistId.value != current.id) {
                playlistOpenCounter.intValue++
                lastOpenedPlaylistId.value = current.id
            }
            lastPlaylistDetail.value = current
        } else {
            lastOpenedPlaylistId.value = null
        }
    }
    LaunchedEffect(isForeground) {
        if (!isForeground) {
            lastPlaylistDetail.value = null
        }
    }
    val detailListState = rememberSaveable(uiState.activePlaylistDetail?.id, saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
        androidx.compose.foundation.lazy.LazyListState()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        val window = LocalAdaptiveWindow.current
        val split = window.showDetailAlongside(uiState.activePlaylistDetail != null, maxWidth, maxHeight)

        val feedContent: @Composable () -> Unit = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PageBackground)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(searchBackdrop)
                ) {
                    when {
                        // 1. 用户尚未输入：展示空白状态（顶部搜索历史 + 底部热搜列表）
                        uiState.query.isEmpty() -> {
                            InitialSearchPage(
                                history = uiState.searchHistory,
                                hotSearches = uiState.hotSearches,
                                isHotSearchLoading = uiState.isHotSearchLoading,
                                topContentPadding = topContentPadding,
                                bottomContentPadding = bottomContentPadding,
                                onHistoryClick = { handleSelect { onHistoryClick(it) } },
                                onClearHistory = onClearHistory,
                                onRemoveHistory = onRemoveHistory,
                                onHotSearchClick = { handleSelect { onHotSearchClick(it) } },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // 2. 用户开始输入但尚未提交正式搜索：即时展示联想搜索建议
                        !uiState.isSearchSubmitted -> {
                            SearchSuggestionsSection(
                                query = uiState.query,
                                suggestions = uiState.suggestions,
                                isLoading = uiState.isSuggestionLoading,
                                topContentPadding = topContentPadding,
                                bottomContentPadding = bottomContentPadding,
                                onSearchSubmit = { handleSelect { onSearchSubmit() } },
                                onSuggestionClick = { handleSelect { onSuggestionClick(it) } },
                                onTrackClick = onTrackClick,
                                onArtistClick = { id, name, cover -> handleSelect { onArtistClick(id, name, cover) } },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // 3. 用户提交搜索后：展示搜索结果（顶部分类标签浮于模糊区之上）
                        else -> {
                            when {
                                uiState.isLoading -> {
                                    ResultLoadingSection(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = resultsTopPadding, bottom = bottomContentPadding)
                                    )
                                }

                                shouldShowResultError(uiState) -> {
                                    ResultErrorSection(
                                        errorMessage = uiState.errorMessage ?: "搜索失败",
                                        onRetryClick = onRetryClick,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = resultsTopPadding, bottom = bottomContentPadding)
                                    )
                                }

                                items.isEmpty() -> {
                                    EmptyResultsSection(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = resultsTopPadding, bottom = bottomContentPadding)
                                    )
                                }

                                else -> {
                                    SearchResultsList(
                                        items = items,
                                        artworkGrid = selectedTab == SearchTab.ARTIST || selectedTab == SearchTab.ALBUM || selectedTab == SearchTab.PLAYLIST,
                                        topContentPadding = resultsTopPadding,
                                        bottomContentPadding = bottomContentPadding,
                                        onRowClick = onSuggestionClick,
                                        onArtistClick = { id, name, cover -> handleSelect { onArtistClick(id, name, cover) } },
                                        onTrackClick = onTrackClick,
                                        onAlbumClick = { album ->
                                            handleSelect {
                                                onAlbumClick(album.id, album.title, album.artistName, album.coverUrl)
                                            }
                                        },
                                        onPlaylistClick = { playlist ->
                                            handleSelect {
                                                onPlaylistClick(playlist.id, playlist.name, playlist.trackCount, playlist.coverUrl)
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }

                // 顶部渐进高斯模糊（顶部深、底部浅自然过渡，无状态栏白色遮罩）
                ProgressiveTopBlur(
                    backdrop = searchBackdrop,
                    totalHeight = if (uiState.isSearchSubmitted) topContentPadding + 64.dp else topContentPadding + 18.dp,
                    statusBarHeight = statusBarHeight,
                    modifier = Modifier.align(Alignment.TopCenter),
                    deepBlurRadius = 26.dp,
                    mediumBlurRadius = 13.dp,
                    shallowBlurRadius = 4.5.dp
                )

                if (uiState.isSearchSubmitted) {
                    SearchTabsRow(
                        backdrop = searchBackdrop,
                        selectedTab = selectedTab,
                        onSelect = { selectedTab = it },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(top = topContentPadding + 4.dp, start = 16.dp, end = 16.dp)
                    )
                }

                topHeader()
            }
        }

        RetainedMusicFeed(!split) { feedContent() }

        if (split) {
            uiState.activePlaylistDetail?.let { detail ->
                val glassBackdrop = LocalGlassBackdrop.current ?: searchBackdrop
                PlaylistDetailScreen(
                    detail = detail,
                    backdrop = glassBackdrop,
                    onClose = onClosePlaylistDetail,
                    onPlayTrack = { track -> onPlayPlaylistTrack(track, detail.tracks) },
                    onPlayAll = { onPlayPlaylistAll(detail.tracks) },
                    onShufflePlay = { onShufflePlaylist(detail.tracks) },
                    isLoading = uiState.isLoadingDetail,
                    listState = detailListState,
                    filterByCurrentUserLikes = false
                )
            }
        } else {
            AnimatedVisibility(
                visible = uiState.activePlaylistDetail != null,
                enter = slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = spring(dampingRatio = 0.92f, stiffness = 260f)
                ),
                exit = if (isForeground) {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = spring(dampingRatio = 0.92f, stiffness = 300f)
                    )
                } else {
                    ExitTransition.None
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f)
            ) {
                val displayDetail = uiState.activePlaylistDetail ?: lastPlaylistDetail.value
                displayDetail?.let { detail ->
                    key(playlistOpenCounter.intValue) {
                        val glassBackdrop = LocalGlassBackdrop.current ?: searchBackdrop
                        PlaylistDetailScreen(
                            detail = detail,
                            backdrop = glassBackdrop,
                            onClose = onClosePlaylistDetail,
                            onPlayTrack = { track -> onPlayPlaylistTrack(track, detail.tracks) },
                            onPlayAll = { onPlayPlaylistAll(detail.tracks) },
                            onShufflePlay = { onShufflePlaylist(detail.tracks) },
                            isLoading = uiState.isLoadingDetail,
                            listState = detailListState,
                            filterByCurrentUserLikes = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTabsRow(
    backdrop: Backdrop,
    selectedTab: SearchTab,
    onSelect: (SearchTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = remember { SearchTab.entries }
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentSelectedTab by rememberUpdatedState(selectedTab)
    val selectedIndex = tabs.indexOf(currentSelectedTab).coerceAtLeast(0)

    val navBarHeight = 44.dp
    val cornerRadius = navBarHeight / 2
    val thumbPadding = GlassSliderDefaults.ThumbPadding

    val animationScope = rememberCoroutineScope()
    val pressAnimation = remember { Animatable(0f) }
    val animationSpec = remember { spring<Float>(0.8f, 500f, 0.001f) }

    var dragOffsetX by remember { mutableStateOf<Float?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(navBarHeight)
            .graphicsLayer {
                val progress = pressAnimation.value
                val maxScale = (size.width + 6f.dp.toPx()) / size.width
                val scale = lerp(1f, maxScale, progress)
                scaleX = scale
                scaleY = scale
            }
            .liquidGlass(
                backdrop = backdrop,
                cornerRadius = cornerRadius
            )
            .glassSegmentDrag(
                itemCount = tabs.size,
                onDragOffsetChange = { dragOffsetX = it },
                onSettle = { settledIndex -> currentOnSelect(tabs[settledIndex]) },
                onPressChanged = { pressed ->
                    animationScope.launch {
                        pressAnimation.animateTo(if (pressed) 1f else 0f, animationSpec)
                    }
                }
            )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = thumbPadding),
            contentAlignment = Alignment.CenterStart
        ) {
            val geometry = rememberGlassThumbGeometry(
                containerWidth = maxWidth,
                itemCount = tabs.size,
                selectedIndex = selectedIndex,
                dragOffsetX = dragOffsetX,
                barHeight = navBarHeight,
                cornerRadius = cornerRadius
            )

            GlassThumb(
                backdrop = backdrop,
                geometry = geometry,
                surfaceColor = TabActiveBg,
                surfaceAlpha = 0.95f
            )

            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = index == selectedIndex
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) TabActiveText else SecondaryText,
                        animationSpec = tween(durationMillis = 200),
                        label = "searchTabTextColor"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                currentOnSelect(tab)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    items: List<SearchRowItem>,
    artworkGrid: Boolean,
    topContentPadding: Dp,
    bottomContentPadding: Dp,
    onRowClick: (String) -> Unit,
    onArtistClick: (Long, String, String?) -> Unit,
    onTrackClick: (Track) -> Unit,
    onAlbumClick: (SearchRowItem.Album) -> Unit,
    onPlaylistClick: (SearchRowItem.Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val window = LocalAdaptiveWindow.current
    val renderRow: @Composable (SearchRowItem) -> Unit = { item ->
            when (item) {
                is SearchRowItem.Song -> SongRow(
                    track = item.track,
                    onClick = { onTrackClick(item.track) }
                )

                is SearchRowItem.Album -> AlbumRow(
                    title = item.title,
                    artistName = item.artistName,
                    coverUrl = item.coverUrl,
                    onClick = { onAlbumClick(item) }
                )

                is SearchRowItem.Artist -> ArtistRow(
                    name = item.name,
                    coverUrl = item.coverUrl,
                    onClick = {
                        onArtistClick(item.id, item.name, item.coverUrl)
                    }
                )

                is SearchRowItem.Playlist -> PlaylistRow(
                    name = item.name,
                    trackCount = item.trackCount,
                    coverUrl = item.coverUrl,
                    onClick = { onPlaylistClick(item) }
                )

                is SearchRowItem.Suggestion -> SuggestionRow(
                    keyword = item.keyword,
                    onClick = { onRowClick(item.keyword) }
                )
            }
    }
    BoxWithConstraints(modifier) {
        val useGrid = artworkGrid && maxWidth >= 500.dp
        var previousGrid by rememberSaveable { mutableStateOf(useGrid) }
        LaunchedEffect(useGrid) {
            if (previousGrid != useGrid) {
                if (useGrid) gridState.scrollToItem(listState.firstVisibleItemIndex.coerceAtMost((items.size - 1).coerceAtLeast(0)), listState.firstVisibleItemScrollOffset)
                else listState.scrollToItem(gridState.firstVisibleItemIndex.coerceAtMost((items.size - 1).coerceAtLeast(0)), gridState.firstVisibleItemScrollOffset)
            }
            previousGrid = useGrid
        }
        val mixedResults = items.any { it is SearchRowItem.Song } && items.any { it is SearchRowItem.Artist || it is SearchRowItem.Album || it is SearchRowItem.Playlist }
        val resultsList: @Composable (List<SearchRowItem>) -> Unit = { results ->
            LazyColumn(
                Modifier.fillMaxSize().verticalElasticOverscroll(),
                state = listState,
                contentPadding = PaddingValues(top = topContentPadding, bottom = bottomContentPadding)
            ) {
                items(results, key = { it.key }) { item ->
                    renderRow(item)
                    HorizontalDivider(Modifier.padding(start = 84.dp, end = 20.dp), color = DividerColor)
                }
            }
        }
        if (window.isBook || window.isTabletop || (maxWidth >= 720.dp && mixedResults && !artworkGrid)) {
            val leading = if (mixedResults) items.filterNot { it is SearchRowItem.Song } else items.take((items.size + 1) / 2)
            val trailing = if (mixedResults) items.filterIsInstance<SearchRowItem.Song>() else items.drop((items.size + 1) / 2)
            AdaptiveTwoPane(Modifier.fillMaxSize(), primaryFraction = 0.42f,
                primary = {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = topContentPadding + 16.dp, end = 16.dp, bottom = bottomContentPadding),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(leading, key = { it.key }) { item ->
                            SearchArtworkCard(item, onRowClick, onArtistClick, onTrackClick, onAlbumClick, onPlaylistClick)
                        }
                    }
                },
                secondary = { resultsList(trailing) })
        } else if (useGrid) {
            LazyVerticalGrid(
                GridCells.Adaptive(168.dp),
                Modifier.fillMaxSize(),
                state = gridState,
                contentPadding = PaddingValues(start = 20.dp, top = topContentPadding + 20.dp, end = 20.dp, bottom = bottomContentPadding),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(items, key = { it.key }) { item ->
                    SearchArtworkCard(item, onRowClick, onArtistClick, onTrackClick, onAlbumClick, onPlaylistClick)
                }
            }
        } else {
            AdaptiveContent(maxWidth = 900.dp) { resultsList(items) }
        }
    }

}

@Composable
private fun SearchArtworkCard(
    item: SearchRowItem,
    onQuery: (String) -> Unit,
    onArtist: (Long, String, String?) -> Unit,
    onTrack: (Track) -> Unit,
    onAlbum: (SearchRowItem.Album) -> Unit,
    onPlaylist: (SearchRowItem.Playlist) -> Unit
) {
    val image: String?
    val title: String
    val subtitle: String
    val action: () -> Unit
    when (item) {
        is SearchRowItem.Artist -> { image = item.coverUrl; title = item.name; subtitle = "艺人"; action = { onArtist(item.id, item.name, image) } }
        is SearchRowItem.Album -> { image = item.coverUrl; title = item.title; subtitle = item.artistName; action = { onAlbum(item) } }
        is SearchRowItem.Playlist -> { image = item.coverUrl; title = item.name; subtitle = "${item.trackCount} 首歌曲"; action = { onPlaylist(item) } }
        is SearchRowItem.Song -> { image = item.track.coverUrl; title = item.track.title; subtitle = item.track.artists.joinToString(" / ") { it.name }; action = { onTrack(item.track) } }
        is SearchRowItem.Suggestion -> { image = null; title = item.keyword; subtitle = "搜索建议"; action = { onQuery(title) } }
    }
    Column(Modifier.fillMaxWidth().clickable(onClick = action).padding(4.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(if (item is SearchRowItem.Artist) CircleShape else RoundedCornerShape(20.dp)).background(Color(0xFFF2F2F6)), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = R.drawable.ic_nav_search),
                contentDescription = null,
                tint = SecondaryText,
                modifier = Modifier.size(32.dp)
            )
            if (!image.isNullOrBlank()) {
                AsyncImage(model = image, contentDescription = title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = PrimaryText)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, fontSize = 13.sp, color = SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SongRow(
    track: Track,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(
            imageUrl = track.coverUrl ?: track.album.coverUrl,
            size = 56.dp,
            isCircle = false
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = track.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "歌曲 · ${track.artists.joinToString("/") { it.name }.ifBlank { "未知艺人" }}",
                fontSize = 11.sp,
                color = SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "歌词：\"${track.title}\"",
                fontSize = 12.sp,
                color = SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.Filled.MoreHoriz,
            contentDescription = null,
            tint = PrimaryText,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun AlbumRow(
    title: String,
    artistName: String,
    coverUrl: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(
            imageUrl = coverUrl,
            size = 56.dp,
            isCircle = false
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "专辑 · $artistName",
                fontSize = 11.sp,
                color = SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = SecondaryText,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ArtistRow(
    name: String,
    coverUrl: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(
            imageUrl = coverUrl,
            size = 56.dp,
            isCircle = true
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "艺人",
                fontSize = 11.sp,
                color = SecondaryText
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = SecondaryText,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun PlaylistRow(
    name: String,
    trackCount: Int,
    coverUrl: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(
            imageUrl = coverUrl,
            size = 56.dp,
            isCircle = false
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "播放列表 · $trackCount 首",
                fontSize = 11.sp,
                color = SecondaryText
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = SecondaryText,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SuggestionRow(
    keyword: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_nav_search),
            contentDescription = null,
            tint = SecondaryText,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = keyword,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun Artwork(
    imageUrl: String?,
    size: Dp,
    isCircle: Boolean
) {
    val shape = if (isCircle) CircleShape else RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(Color(0xFFE3E3E6)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_nav_search),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.45f)
        )
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun ResultLoadingSection(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color(0xFF606060), strokeWidth = 2.dp)
    }
}

@Composable
fun ResultErrorSection(
    errorMessage: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = Color(0xFF9E9EA7),
            modifier = Modifier.size(52.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(text = "搜索遇到问题", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            fontSize = 13.sp,
            color = SecondaryText,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "提示：请检查设备网络状态后重试",
            fontSize = 12.sp,
            color = Color(0xFFAAAAAF)
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(onClick = onRetryClick) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("重新搜索")
        }
    }
}

@Composable
private fun InitialSearchPage(
    history: List<String>,
    hotSearches: List<HotSearchItem>,
    isHotSearchLoading: Boolean,
    topContentPadding: Dp,
    bottomContentPadding: Dp,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    onRemoveHistory: (String) -> Unit,
    onHotSearchClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .verticalElasticOverscroll(),
        contentPadding = PaddingValues(top = topContentPadding, bottom = bottomContentPadding)
    ) {
        // 顶部搜索历史
        if (history.isNotEmpty()) {
            item(key = "search_history") {
                SearchHistorySection(
                    history = history,
                    onHistoryClick = onHistoryClick,
                    onClearHistory = onClearHistory,
                    onRemoveHistory = onRemoveHistory
                )
            }
        }

        // 下方热搜列表：直接展示在搜索历史下方，弹出键盘时底部抬起覆盖
        item(key = "hot_search") {
            HotSearchSection(
                hotSearches = hotSearches,
                isLoading = isHotSearchLoading,
                onHotSearchClick = onHotSearchClick
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHistorySection(
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    onRemoveHistory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "清空搜索历史",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = PrimaryText
                )
            },
            text = {
                Text(
                    text = "确定要清空全部搜索历史吗？",
                    fontSize = 14.sp,
                    color = SecondaryText
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearDialog = false
                    }
                ) {
                    Text("清空", color = Color(0xFFFF2442), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消", color = PrimaryText)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "搜索历史",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_delete_history),
                contentDescription = "清空搜索历史",
                tint = SecondaryText,
                modifier = Modifier
                    .size(19.dp)
                    .clickable { showClearDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            history.forEach { keyword ->
                Surface(
                    color = Color(0xFFF4F4F6),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onHistoryClick(keyword) }
                ) {
                    Text(
                        text = keyword,
                        fontSize = 13.sp,
                        color = Color(0xFF333333),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HotSearchSection(
    hotSearches: List<HotSearchItem>,
    isLoading: Boolean,
    onHotSearchClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "热搜榜",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading && hotSearches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF888888),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            hotSearches.forEachIndexed { index, item ->
                val rank = index + 1
                val isTopThree = rank <= 3
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onHotSearchClick(item.searchWord) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rank.toString(),
                        fontSize = 16.sp,
                        fontWeight = if (isTopThree) FontWeight.Bold else FontWeight.Medium,
                        color = if (isTopThree) Color(0xFFFF2442) else Color(0xFF999999),
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.Start
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.searchWord,
                                fontSize = 15.sp,
                                fontWeight = if (isTopThree) FontWeight.SemiBold else FontWeight.Normal,
                                color = PrimaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (!item.iconUrl.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                AsyncImage(
                                    model = item.iconUrl,
                                    contentDescription = null,
                                    modifier = Modifier.height(14.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        if (item.content.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.content,
                                fontSize = 12.sp,
                                color = SecondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (item.score > 0) {
                        Text(
                            text = formatHotScore(item.score),
                            fontSize = 12.sp,
                            color = Color(0xFFCCCCCC)
                        )
                    }
                }
            }
        }
    }
}

private fun formatHotScore(score: Long): String {
    return when {
        score >= 10_000 -> String.format(Locale.CHINA, "%.1f万", score / 10000.0)
        score > 0 -> score.toString()
        else -> ""
    }
}

@Composable
private fun SearchSuggestionsSection(
    query: String,
    suggestions: SearchSuggestions,
    isLoading: Boolean,
    topContentPadding: Dp,
    bottomContentPadding: Dp,
    onSearchSubmit: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onTrackClick: (Track) -> Unit,
    onArtistClick: (Long, String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .verticalElasticOverscroll(),
        contentPadding = PaddingValues(top = topContentPadding, bottom = bottomContentPadding)
    ) {
        // 第一项：直接搜索输入的当前关键词
        item(key = "suggest_current_query") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSearchSubmit() }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_nav_search),
                    contentDescription = null,
                    tint = Color(0xFFFF2442),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = buildAnnotatedString {
                        append("搜索 “")
                        withStyle(style = SpanStyle(color = Color(0xFFFF2442), fontWeight = FontWeight.SemiBold)) {
                            append(query)
                        }
                        append("”")
                    },
                    fontSize = 16.sp,
                    color = PrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp, end = 20.dp),
                color = DividerColor
            )
        }

        // 匹配建议关键词列表
        items(suggestions.allMatch, key = { "suggest_match_$it" }) { keyword ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSuggestionClick(keyword) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_nav_search),
                    contentDescription = null,
                    tint = SecondaryText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = keyword,
                    fontSize = 16.sp,
                    color = PrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp, end = 20.dp),
                color = DividerColor
            )
        }

        // 单曲联想
        if (suggestions.songs.isNotEmpty()) {
            items(suggestions.songs, key = { "suggest_song_${it.id}" }) { track ->
                SongRow(
                    track = track,
                    onClick = { onTrackClick(track) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 84.dp, end = 20.dp),
                    color = DividerColor
                )
            }
        }

        // 艺人联想
        if (suggestions.artists.isNotEmpty()) {
            items(suggestions.artists, key = { "suggest_artist_${it.id}" }) { artist ->
                ArtistRow(
                    name = artist.name,
                    coverUrl = artist.coverUrl,
                    onClick = { onArtistClick(artist.id, artist.name, artist.coverUrl) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 84.dp, end = 20.dp),
                    color = DividerColor
                )
            }
        }
    }
}

@Composable
fun EmptyResultsSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "没有找到匹配结果",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryText
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "可以试试更短的关键词",
            fontSize = 13.sp,
            color = SecondaryText
        )
    }
}

private fun buildSearchItems(
    uiState: SearchUiState,
    selectedTab: SearchTab
): List<SearchRowItem> {
    val tracks = uiState.tracks

    // 建立歌手头像高速索引表，优先从搜索歌手列表与建议歌手列表中提取真实头像
    val artistAvatarMap = mutableMapOf<Long, String>()
    uiState.artists.forEach { artist ->
        artist.coverUrl?.takeIf { it.isNotBlank() }?.let { artistAvatarMap[artist.id] = it }
    }
    uiState.suggestions.artists.forEach { suggestion ->
        suggestion.coverUrl?.takeIf { it.isNotBlank() }?.let { artistAvatarMap.putIfAbsent(suggestion.id, it) }
    }

    val artists: List<SearchRowItem.Artist> = if (uiState.artists.isNotEmpty()) {
        uiState.artists.map {
            SearchRowItem.Artist(
                id = it.id,
                name = it.name,
                coverUrl = it.coverUrl ?: artistAvatarMap[it.id]
            )
        }
    } else {
        tracks
            .flatMap { track ->
                track.artists.map { artist ->
                    SearchRowItem.Artist(
                        id = artist.id,
                        name = artist.name,
                        coverUrl = artistAvatarMap[artist.id] ?: artist.coverUrl
                    )
                }
            }
            .distinctBy { it.id }
    }

    val albums = tracks
        .map {
            SearchRowItem.Album(
                id = it.album.id,
                title = it.album.title,
                artistName = it.artists.firstOrNull()?.name ?: "未知艺人",
                coverUrl = it.album.coverUrl ?: it.coverUrl
            )
        }
        .distinctBy { if (it.id > 0L) it.id else it.title }

    val playlists = (if (uiState.playlists.isNotEmpty()) {
        uiState.playlists.map {
            SearchRowItem.Playlist(
                id = it.id,
                name = it.name,
                trackCount = it.trackCount,
                coverUrl = it.coverUrl
            )
        }
    } else {
        uiState.suggestions.playlists.map {
            SearchRowItem.Playlist(
                id = it.id,
                name = it.name,
                trackCount = it.trackCount,
                coverUrl = it.coverUrl
            )
        }
    }).distinctBy { it.id }

    return when (selectedTab) {
        SearchTab.BEST -> {
            val mixed = mutableListOf<SearchRowItem>()
            // 1) 搜索联想建议（最多 3 个）
            mixed += uiState.suggestions.allMatch.take(3).map { SearchRowItem.Suggestion(it) }
            // 2) 歌曲
            mixed += tracks.take(8).map { SearchRowItem.Song(it) }
            // 3) 艺人
            mixed += artists.take(4)
            // 4) 专辑
            mixed += albums.take(4)
            // 5) 歌单
            mixed += playlists.take(4)
            mixed
        }

        SearchTab.SONG -> tracks.map { SearchRowItem.Song(it) }
        SearchTab.ARTIST -> artists
        SearchTab.ALBUM -> albums
        SearchTab.PLAYLIST -> playlists
    }
}

fun shouldShowInitialEmptyState(uiState: SearchUiState): Boolean =
    uiState.query.isEmpty()

fun shouldShowResultError(uiState: SearchUiState): Boolean =
    uiState.query.isNotEmpty() &&
        !uiState.isLoading &&
        uiState.errorMessage != null &&
        uiState.tracks.isEmpty()

fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / 1000 / 60) % 60
    val hours = durationMs / 1000 / 60 / 60
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%d:%02d", minutes, seconds)
    }
}

fun SearchSuggestions.isNotEmpty(): Boolean =
    songs.isNotEmpty() || artists.isNotEmpty() || playlists.isNotEmpty() || allMatch.isNotEmpty()
