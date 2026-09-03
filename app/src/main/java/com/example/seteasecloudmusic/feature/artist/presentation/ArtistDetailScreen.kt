package com.example.seteasecloudmusic.feature.artist.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistAlbum
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistSummary

private val ArtistPageBg = Color(0xFFF8F8F9)
private val ArtistPrimaryText = Color(0xFF111111)
private val ArtistSecondaryText = Color(0xFF8D8D93)
private val ArtistAccent = Color(0xFFFA233B)

@Composable
fun ArtistDetailRoute(
    artistId: Long,
    artistName: String,
    artistCoverUrl: String?,
    onClose: () -> Unit,
    viewModel: ArtistDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(artistId) {
        viewModel.loadArtist(
            artistId = artistId,
            fallbackName = artistName,
            fallbackCoverUrl = artistCoverUrl
        )
    }

    ArtistDetailScreen(
        uiState = uiState,
        onClose = onClose,
        onPlayFirstSong = viewModel::onPlayFirstSongClick,
        onSongClick = viewModel::onSongClick,
        onMoreSongsClick = viewModel::onLoadAllSongsClick,
        onMoreAlbumsClick = viewModel::onLoadAllAlbumsClick,
        onMoreSimilarArtistsClick = viewModel::onLoadAllSimilarArtistsClick
    )
}

@Composable
fun ArtistDetailScreen(
    uiState: ArtistDetailUiState,
    onClose: () -> Unit,
    onPlayFirstSong: () -> Unit,
    onSongClick: (Track) -> Unit,
    onMoreSongsClick: () -> Unit,
    onMoreAlbumsClick: () -> Unit,
    onMoreSimilarArtistsClick: () -> Unit
) {
    BackHandler(onBack = onClose)

    val previewSongs = uiState.songs.take(ArtistDetailViewModel.PREVIEW_LIMIT)
    val previewAlbums = uiState.albums.take(ArtistDetailViewModel.PREVIEW_LIMIT)
    val previewSimilar = uiState.similarArtists.take(ArtistDetailViewModel.PREVIEW_LIMIT)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArtistPageBg)
    ) {
        if (uiState.isLoading && previewSongs.isEmpty() && previewAlbums.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = ArtistAccent,
                strokeWidth = 2.dp
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                ArtistHeroSection(
                    artistName = uiState.artistName,
                    artistCoverUrl = uiState.artistCoverUrl,
                    onClose = onClose,
                    onPlayFirstSong = onPlayFirstSong
                )
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                item {
                    Text(
                        text = uiState.errorMessage,
                        color = Color(0xFFB52438),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }

            item {
                SectionHeader(
                    title = "歌曲",
                    isLoadingMore = uiState.isSongsLoadingMore,
                    onMoreClick = onMoreSongsClick
                )
            }
            item {
                SongSection(
                    songs = previewSongs,
                    onSongClick = onSongClick
                )
            }

            item {
                SectionHeader(
                    title = "专辑",
                    isLoadingMore = uiState.isAlbumsLoadingMore,
                    onMoreClick = onMoreAlbumsClick
                )
            }
            item {
                AlbumSection(albums = previewAlbums)
            }

            item {
                SectionHeader(
                    title = "音乐视频",
                    isLoadingMore = false,
                    onMoreClick = {}
                )
            }
            item {
                PlaceholderSection(
                    titlePrefix = "MV",
                    fallbackImageUrl = uiState.artistCoverUrl,
                    fallbackSubtitle = "Coming soon"
                )
            }

            item {
                SectionHeader(
                    title = "艺人歌单",
                    isLoadingMore = false,
                    onMoreClick = {}
                )
            }
            item {
                PlaceholderSection(
                    titlePrefix = "Playlist",
                    fallbackImageUrl = previewAlbums.firstOrNull()?.coverUrl,
                    fallbackSubtitle = "Coming soon"
                )
            }

            item {
                SectionHeader(
                    title = "相似歌手",
                    isLoadingMore = uiState.isSimilarLoadingMore,
                    onMoreClick = onMoreSimilarArtistsClick
                )
            }
            item {
                SimilarArtistsSection(artists = previewSimilar)
            }

            if (!uiState.description?.brief.isNullOrBlank() || uiState.description?.sections?.isNotEmpty() == true) {
                item {
                    DescriptionSection(uiState = uiState)
                }
            }
        }
    }
}

@Composable
private fun ArtistHeroSection(
    artistName: String,
    artistCoverUrl: String?,
    onClose: () -> Unit,
    onPlayFirstSong: () -> Unit
) {
    val topInset = 48.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(430.dp)
            .background(Color.White)
    ) {
        AsyncImage(
            model = artistCoverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.36f),
                            ArtistPageBg
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topInset + 8.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.78f),
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onClose() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ArtistPrimaryText,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.78f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = "More",
                        tint = ArtistPrimaryText,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = artistName.ifBlank { "Unknown Artist" },
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 46.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Surface(
                shape = CircleShape,
                color = ArtistAccent,
                modifier = Modifier
                    .size(62.dp)
                    .clickable { onPlayFirstSong() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    isLoadingMore: Boolean,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMoreClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = ArtistPrimaryText,
            fontSize = 42.sp,
            lineHeight = 44.sp,
            letterSpacing = (-1).sp,
            fontWeight = FontWeight.Black
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLoadingMore) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = ArtistAccent,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "More",
                tint = ArtistSecondaryText,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun SongSection(
    songs: List<Track>,
    onSongClick: (Track) -> Unit
) {
    if (songs.isEmpty()) {
        EmptySectionHint(text = "No songs yet")
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items = songs, key = { it.id }) { song ->
            Column(
                modifier = Modifier
                    .width(160.dp)
                    .clickable { onSongClick(song) }
            ) {
                ArtworkCard(
                    imageUrl = song.coverUrl ?: song.album.coverUrl,
                    size = 160.dp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = song.title,
                    color = ArtistPrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artists.joinToString(" / ") { it.name }.ifBlank { "Unknown Artist" },
                    color = ArtistSecondaryText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AlbumSection(albums: List<ArtistAlbum>) {
    if (albums.isEmpty()) {
        EmptySectionHint(text = "No albums yet")
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items = albums, key = { it.id }) { album ->
            Column(modifier = Modifier.width(160.dp)) {
                ArtworkCard(
                    imageUrl = album.coverUrl,
                    size = 160.dp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = album.title,
                    color = ArtistPrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.publishYear?.toString() ?: "Unknown year",
                    color = ArtistSecondaryText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SimilarArtistsSection(artists: List<ArtistSummary>) {
    if (artists.isEmpty()) {
        EmptySectionHint(text = "No similar artists yet")
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items = artists, key = { it.id }) { artist ->
            Column(
                modifier = Modifier.width(120.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircleArtworkCard(imageUrl = artist.coverUrl)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = artist.name,
                    color = ArtistPrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PlaceholderSection(
    titlePrefix: String,
    fallbackImageUrl: String?,
    fallbackSubtitle: String
) {
    val placeholders = List(3) { index -> "${titlePrefix} ${index + 1}" }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(placeholders) { title ->
            Column(modifier = Modifier.width(160.dp)) {
                ArtworkCard(
                    imageUrl = fallbackImageUrl,
                    size = 160.dp,
                    alpha = 0.58f
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = title,
                    color = ArtistPrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = fallbackSubtitle,
                    color = ArtistSecondaryText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DescriptionSection(uiState: ArtistDetailUiState) {
    val description = uiState.description ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "简介",
            color = ArtistPrimaryText,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black
        )

        if (description.brief.isNotBlank()) {
            Text(
                text = description.brief,
                color = ArtistSecondaryText,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
        }

        description.sections.take(2).forEach { section ->
            if (section.title.isNotBlank()) {
                Text(
                    text = section.title,
                    color = ArtistPrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = section.content,
                color = ArtistSecondaryText,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun ArtworkCard(
    imageUrl: String?,
    size: androidx.compose.ui.unit.Dp,
    alpha: Float = 1f
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE7E7EC))
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = alpha,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun CircleArtworkCard(imageUrl: String?) {
    Box(
        modifier = Modifier
            .size(104.dp)
            .clip(CircleShape)
            .background(Color(0xFFE7E7EC))
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun EmptySectionHint(text: String) {
    Text(
        text = text,
        color = ArtistSecondaryText,
        fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
    )
}
