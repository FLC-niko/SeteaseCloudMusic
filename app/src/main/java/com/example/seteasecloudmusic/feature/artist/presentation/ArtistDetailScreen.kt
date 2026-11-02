package com.example.seteasecloudmusic.feature.artist.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.seteasecloudmusic.core.ui.adaptive.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.example.seteasecloudmusic.core.common.toCoverThumbnailUrl
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.ui.components.AppleMusicCollapsedTopBar
import com.example.seteasecloudmusic.core.ui.components.rememberAppleMusicCollapseFraction
import com.example.seteasecloudmusic.core.ui.components.verticalElasticOverscroll
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
    val window = LocalAdaptiveWindow.current
    val bottom = LocalContentBottomPadding.current
    val listState = rememberLazyListState()
    val collapse = rememberAppleMusicCollapseFraction(listState, 200.dp)
    BoxWithConstraints(Modifier.fillMaxSize().background(ArtistPageBg)) {
        val split = (maxWidth >= 720.dp && maxHeight >= 480.dp) || window.isBook || window.isTabletop
        val works: @Composable () -> Unit = {
            LazyColumn(Modifier.fillMaxSize(), state = listState,
                contentPadding = PaddingValues(top = if (split) 20.dp else 0.dp, bottom = bottom),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!split) item(key = "artist_hero") { ArtistHeroSection(uiState.artistName, uiState.artistCoverUrl, onPlayFirstSong) }
                else item(key = "artist_title") { MusicPaneHeader(uiState.artistName, "精选作品与音乐旅程") }
                if (uiState.isLoading) item(key = "loading") { Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ArtistAccent) } }
                if (uiState.errorMessage != null) item(key = "error") { Text(uiState.errorMessage, Modifier.padding(24.dp), color = ArtistAccent) }
                item(key = "songs") {
                    Column {
                        SectionHeader("歌曲", uiState.isSongsLoadingMore, onMoreSongsClick)
                        SongSection(uiState.songs.take(ArtistDetailViewModel.PREVIEW_LIMIT), onSongClick)
                    }
                }
                item(key = "albums") {
                    Column {
                        SectionHeader("专辑", uiState.isAlbumsLoadingMore, onMoreAlbumsClick)
                        AlbumSection(uiState.albums.take(ArtistDetailViewModel.PREVIEW_LIMIT))
                    }
                }
                item(key = "videos") { Column { SectionHeader("音乐视频", false, {}); PlaceholderSection("MV", uiState.artistCoverUrl, "Coming soon") } }
                item(key = "playlists") { Column { SectionHeader("艺人歌单", false, {}); PlaceholderSection("Playlist", uiState.albums.firstOrNull()?.coverUrl, "Coming soon") } }
                item(key = "similar") {
                    Column {
                        SectionHeader("相似歌手", uiState.isSimilarLoadingMore, onMoreSimilarArtistsClick)
                        SimilarArtistsSection(uiState.similarArtists.take(ArtistDetailViewModel.PREVIEW_LIMIT))
                    }
                }
                if (!split && uiState.description != null) item(key = "description") { DescriptionSection(uiState) }
            }
        }
        if (split) {
            AdaptiveTwoPane(Modifier.fillMaxSize().statusBarsPadding(), primaryFraction = 0.38f,
                primary = {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                        MusicPaneHeader("艺人", "走近你喜欢的声音", onClose)
                        Box(Modifier.clip(RoundedCornerShape(28.dp))) { ArtistHeroSection(uiState.artistName, uiState.artistCoverUrl, onPlayFirstSong) }
                        if (uiState.description != null) DescriptionSection(uiState)
                    }
                }, secondary = works)
        } else {
            works()
            AppleMusicCollapsedTopBar(uiState.artistName, collapse.value,
                WindowInsets.statusBars.asPaddingValues().calculateTopPadding(), backdrop = null,
                showBackButton = true, onBackClick = onClose, modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun ArtistHeroSection(
    artistName: String,
    artistCoverUrl: String?,
    onPlayFirstSong: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height((LocalAdaptiveWindow.current.height * 0.52f).coerceIn(220.dp, 430.dp))
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
                fontSize = 40.sp,
                lineHeight = 46.sp,
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
            fontSize = if (LocalAdaptiveWindow.current.width >= 600.dp) 28.sp else 34.sp,
            lineHeight = 36.sp,
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
            model = imageUrl.toCoverThumbnailUrl(240),
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
            model = imageUrl.toCoverThumbnailUrl(160),
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
