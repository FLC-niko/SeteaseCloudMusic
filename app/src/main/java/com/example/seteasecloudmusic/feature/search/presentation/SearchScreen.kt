package com.example.seteasecloudmusic.feature.search.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.feature.search.domain.SearchSuggestions
import com.example.seteasecloudmusic.core.model.Track

private val PageBackground = Color.White
private val PrimaryText = Color(0xFF111111)
private val SecondaryText = Color(0xFF909094)
private val DividerColor = Color(0xFFE2E2E6)
private val TabActiveBg = Color(0xFFFF2442)
private val TabActiveText = Color.White

private enum class SearchTab(val title: String) {
    BEST("最佳结果"),
    ARTIST("艺人"),
    ALBUM("专辑"),
    SONG("歌曲"),
    PLAYLIST("歌单"),
    MV("音乐视频")
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
    onArtistClick: (Long, String, String?) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SearchScreenContent(
        uiState = uiState,
        topContentPadding = topContentPadding,
        bottomContentPadding = bottomContentPadding,
        onSuggestionClick = { viewModel.onSuggestionClick(it) },
        onArtistClick = onArtistClick,
        onTrackClick = { viewModel.onTrackClick(it) },
        onRetryClick = { viewModel.onRetryClick() }
    )
}

@Composable
fun SearchScreenContent(
    uiState: SearchUiState,
    topContentPadding: Dp,
    bottomContentPadding: Dp = 180.dp,
    onSuggestionClick: (String) -> Unit,
    onArtistClick: (Long, String, String?) -> Unit,
    onTrackClick: (Track) -> Unit,
    onRetryClick: () -> Unit
) {
    var selectedTab by rememberSaveable(uiState.query) { mutableStateOf(SearchTab.BEST) }

    val items = remember(uiState, selectedTab) {
        buildSearchItems(uiState = uiState, selectedTab = selectedTab)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(top = topContentPadding, bottom = bottomContentPadding)
    ) {
        SearchTabsRow(
            selectedTab = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        when {
            shouldShowInitialEmptyState(uiState) -> {
                InitialEmptyStateSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            uiState.isLoading || uiState.isSuggestionLoading -> {
                ResultLoadingSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            shouldShowResultError(uiState) -> {
                ResultErrorSection(
                    errorMessage = uiState.errorMessage ?: "搜索失败",
                    onRetryClick = onRetryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            items.isEmpty() -> {
                EmptyResultsSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            else -> {
                SearchResultsList(
                    items = items,
                    onRowClick = onSuggestionClick,
                    onArtistClick = onArtistClick,
                    onTrackClick = onTrackClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SearchTabsRow(
    selectedTab: SearchTab,
    onSelect: (SearchTab) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(SearchTab.entries) { tab ->
            val selected = tab == selectedTab
            Surface(
                color = if (selected) TabActiveBg else Color.Transparent,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.clickable { onSelect(tab) }
            ) {
                Text(
                    text = tab.title,
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) TabActiveText else PrimaryText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    items: List<SearchRowItem>,
    onRowClick: (String) -> Unit,
    onArtistClick: (Long, String, String?) -> Unit,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(items = items, key = { _, item -> item.key }) { index, item ->
            when (item) {
                is SearchRowItem.Song -> SongRow(
                    track = item.track,
                    onClick = { onTrackClick(item.track) }
                )

                is SearchRowItem.Album -> AlbumRow(
                    title = item.title,
                    artistName = item.artistName,
                    coverUrl = item.coverUrl,
                    onClick = { onRowClick(item.title) }
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
                    onClick = { onRowClick(item.name) }
                )

                is SearchRowItem.Suggestion -> SuggestionRow(
                    keyword = item.keyword,
                    onClick = { onRowClick(item.keyword) }
                )
            }

            if (index != items.lastIndex) {
                HorizontalDivider(
                    color = DividerColor,
                    modifier = Modifier.padding(start = 84.dp)
                )
            }
        }
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
            imageVector = Icons.Filled.Search,
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
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
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
        Text(text = "加载失败", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = errorMessage, fontSize = 13.sp, color = SecondaryText)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetryClick) {
            Text("重试")
        }
    }
}

@Composable
fun InitialEmptyStateSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "在底栏输入关键词开始搜索",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryText
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "支持歌曲、艺人、专辑、播放列表",
            fontSize = 13.sp,
            color = SecondaryText
        )
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
    // 歌曲、艺人、专辑统一只使用正式搜索（cloudsearch）结果，
    // 保证图片、艺人名等数据完整正确。
    val tracks = uiState.tracks

    val artists = tracks
        .flatMap { track ->
            track.artists.map { artist ->
                SearchRowItem.Artist(
                    id = artist.id,
                    name = artist.name,
                    coverUrl = artist.coverUrl
                )
            }
        }
        .distinctBy { it.id }

    val albums = tracks
        .map {
            SearchRowItem.Album(
                id = it.album.id,
                title = it.album.title,
                artistName = it.artists.firstOrNull()?.name ?: "未知艺人",
                coverUrl = it.album.coverUrl ?: it.coverUrl
            )
        }
        .distinctBy { it.id }

    val playlists = uiState.suggestions.playlists.map {
        SearchRowItem.Playlist(
            id = it.id,
            name = it.name,
            trackCount = it.trackCount,
            coverUrl = it.coverUrl
        )
    }

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
            if (mixed.isEmpty()) {
                mixed += playlists.take(6)
            }
            mixed
        }

        SearchTab.ARTIST -> artists
        SearchTab.ALBUM -> albums
        SearchTab.SONG -> tracks.map { SearchRowItem.Song(it) }
        SearchTab.PLAYLIST -> playlists
        SearchTab.MV -> emptyList()
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
