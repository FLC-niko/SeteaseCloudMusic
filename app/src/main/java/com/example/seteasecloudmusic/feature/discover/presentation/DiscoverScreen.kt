package com.example.seteasecloudmusic.feature.discover.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.discover.domain.model.DiscoverPlaylist
import com.example.seteasecloudmusic.feature.discover.domain.model.DiscoverToplist
import com.example.seteasecloudmusic.feature.discover.domain.model.DiscoverTrackPreview

private val DiscoverBackground = Color(0xFFF4F4F6)
private val DiscoverPrimary = Color(0xFF111111)
private val DiscoverSecondary = Color(0xFF8F8F95)
private val DiscoverDivider = Color(0xFFE2E2E6)
private val DiscoverAccent = Color(0xFFFA233B)
private val DiscoverSurface = Color(0xFFF7F7FA)

@Composable
fun DiscoverScreen(
    uiState: DiscoverUiState,
    topContentPadding: Dp,
    bottomContentPadding: Dp,
    onRetryPersonalized: () -> Unit,
    onRetryHotPlaylists: () -> Unit,
    onRetryNewsongs: () -> Unit,
    onRetryToplists: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscoverBackground)
            .padding(top = topContentPadding, bottom = bottomContentPadding)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                DiscoverChipSection(
                    title = "推荐歌单",
                    playlists = uiState.personalizedPlaylists.ifEmpty { uiState.hotPlaylists },
                    isLoading = uiState.isPersonalizedLoading,
                    errorMessage = uiState.personalizedErrorMessage,
                    onRetry = onRetryPersonalized
                )
            }

            item {
                DiscoverNowPlayingSection(
                    toplists = uiState.toplists,
                    isLoading = uiState.isToplistsLoading,
                    errorMessage = uiState.toplistsErrorMessage,
                    onRetry = onRetryToplists
                )
            }

            item {
                DiscoverSectionHeader(
                    title = "推荐新歌",
                    subtitle = "近期值得听的新鲜声音"
                )
            }

            item {
                DiscoverSongSection(
                    tracks = uiState.newsongs,
                    isLoading = uiState.isNewsongsLoading,
                    errorMessage = uiState.newsongsErrorMessage,
                    onRetry = onRetryNewsongs
                )
            }

            item {
                DiscoverSectionHeader(
                    title = "热门歌单",
                    subtitle = "持续更新的流行内容"
                )
            }

            item {
                DiscoverPlaylistListSection(
                    playlists = uiState.hotPlaylists,
                    isLoading = uiState.isHotPlaylistsLoading,
                    errorMessage = uiState.hotPlaylistsErrorMessage,
                    onRetry = onRetryHotPlaylists
                )
            }
        }
    }
}

@Composable
private fun DiscoverSectionHeader(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = DiscoverPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = DiscoverSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = DiscoverSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DiscoverChipSection(
    title: String,
    playlists: List<DiscoverPlaylist>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DiscoverSectionHeader(
            title = title,
            subtitle = "根据你的口味推荐"
        )

        when {
            isLoading && playlists.isEmpty() -> DiscoverLoadingRow()
            !errorMessage.isNullOrBlank() && playlists.isEmpty() -> DiscoverErrorPanel(errorMessage, onRetry)
            playlists.isEmpty() -> DiscoverEmptyPanel("暂无推荐歌单")
            else -> LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    DiscoverChipCard(playlist = playlist)
                }
            }
        }
    }
}

@Composable
private fun DiscoverChipCard(
    playlist: DiscoverPlaylist
) {
    Column(
        modifier = Modifier.width(136.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, DiscoverDivider),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = playlist.coverUrl,
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.16f))
                )
            }
        }
        Text(
            text = playlist.name,
            color = DiscoverPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        playlist.copywriter?.let { copywriter ->
            Text(
                text = copywriter,
                color = DiscoverSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DiscoverNowPlayingSection(
    toplists: List<DiscoverToplist>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DiscoverSectionHeader(
            title = "榜单",
            subtitle = "榜单热点与播放摘要"
        )

        when {
            isLoading && toplists.isEmpty() -> DiscoverLoadingRow(cardHeight = 420.dp)
            !errorMessage.isNullOrBlank() && toplists.isEmpty() -> DiscoverErrorPanel(errorMessage, onRetry)
            toplists.isEmpty() -> DiscoverEmptyPanel("暂无榜单内容")
            else -> LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(toplists, key = { it.id }) { toplist ->
                    DiscoverToplistCard(toplist = toplist)
                }
            }
        }
    }
}

@Composable
private fun DiscoverToplistCard(
    toplist: DiscoverToplist
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, DiscoverDivider),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.width(320.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(DiscoverSurface)
            ) {
                AsyncImage(
                    model = toplist.coverUrl,
                    contentDescription = toplist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.15f)
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = toplist.name,
                    color = DiscoverPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                toplist.updateFrequency?.let {
                    Text(
                        text = it,
                        color = DiscoverSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                toplist.previews.take(3).forEachIndexed { index, preview ->
                    DiscoverPreviewRow(index = index + 1, preview = preview)
                }
            }
        }
    }
}

@Composable
private fun DiscoverPreviewRow(
    index: Int,
    preview: DiscoverTrackPreview
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = index.toString(),
            color = DiscoverAccent,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.width(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preview.title,
                color = DiscoverPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = preview.artistName,
                color = DiscoverSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DiscoverSongSection(
    tracks: List<Track>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    when {
        isLoading && tracks.isEmpty() -> DiscoverLoadingColumn(rowCount = 4)
        !errorMessage.isNullOrBlank() && tracks.isEmpty() -> DiscoverErrorPanel(errorMessage, onRetry)
        tracks.isEmpty() -> DiscoverEmptyPanel("暂无新歌")
        else -> Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
        ) {
            tracks.take(10).forEachIndexed { index, track ->
                DiscoverSongRow(track = track)
                if (index != tracks.take(10).lastIndex) {
                    HorizontalDivider(color = DiscoverDivider, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun DiscoverSongRow(
    track: Track
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DiscoverSurface),
            modifier = Modifier.size(56.dp)
        ) {
            AsyncImage(
                model = track.coverUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = DiscoverPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artists.joinToString(" / ") { it.name },
                color = DiscoverSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Filled.MoreHoriz,
                contentDescription = "更多",
                tint = DiscoverSecondary
            )
        }
    }
}

@Composable
private fun DiscoverPlaylistListSection(
    playlists: List<DiscoverPlaylist>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    when {
        isLoading && playlists.isEmpty() -> DiscoverLoadingColumn(rowCount = 3)
        !errorMessage.isNullOrBlank() && playlists.isEmpty() -> DiscoverErrorPanel(errorMessage, onRetry)
        playlists.isEmpty() -> DiscoverEmptyPanel("暂无热门歌单")
        else -> Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
        ) {
            playlists.take(6).forEachIndexed { index, playlist ->
                DiscoverPlaylistRow(playlist = playlist)
                if (index != playlists.take(6).lastIndex) {
                    HorizontalDivider(color = DiscoverDivider, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun DiscoverPlaylistRow(
    playlist: DiscoverPlaylist
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DiscoverSurface),
            modifier = Modifier.size(72.dp)
        ) {
            AsyncImage(
                model = playlist.coverUrl,
                contentDescription = playlist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                color = DiscoverPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    playlist.playCount?.let { append("${formatPlayCount(it)} 次播放") }
                    playlist.trackCount?.let {
                        if (isNotEmpty()) append(" · ")
                        append("$it 首")
                    }
                    playlist.copywriter?.let {
                        if (isNotEmpty()) append(" · ")
                        append(it)
                    }
                },
                color = DiscoverSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = DiscoverSecondary
        )
    }
}

@Composable
private fun DiscoverLoadingRow(
    cardHeight: Dp = 120.dp
) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .width(132.dp)
                    .height(cardHeight)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = DiscoverAccent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DiscoverLoadingColumn(
    rowCount: Int
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
    ) {
        repeat(rowCount) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DiscoverSurface),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DiscoverAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.height(14.dp).fillMaxWidth(0.68f).background(DiscoverSurface))
                    Box(modifier = Modifier.height(10.dp).fillMaxWidth(0.40f).background(DiscoverSurface))
                }
            }
            if (index != rowCount - 1) {
                HorizontalDivider(color = DiscoverDivider, thickness = 1.dp)
            }
        }
    }
}

@Composable
private fun DiscoverErrorPanel(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(20.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = message,
            color = DiscoverPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Button(onClick = onRetry) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "重试")
        }
    }
}

@Composable
private fun DiscoverEmptyPanel(
    message: String
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = DiscoverSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatPlayCount(playCount: Long): String {
    return when {
        playCount >= 100_000_000L -> "${(playCount / 100_000_000L)}亿"
        playCount >= 10_000L -> "${(playCount / 10_000L)}万"
        else -> playCount.toString()
    }
}
