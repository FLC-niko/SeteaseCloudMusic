package com.example.seteasecloudmusic.feature.home.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.seteasecloudmusic.core.model.Track

private val DetailPageBg = Color.White
private val DetailPrimary = Color(0xFF111111)
private val DetailSecondary = Color(0xFF8F8F95)
private val DetailDivider = Color(0xFFE2E2E6)
private val DetailAccent = Color(0xFFFA233B)
private val PosterWallSurface = Color(0xFFF6F6F8)

@Composable
fun DailyRecommendDetailRoute(
    tracks: List<Track>,
    onClose: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    DailyRecommendDetailScreen(
        tracks = tracks,
        onTrackClick = viewModel::onTrackClick,
        onClose = onClose
    )
}

@Composable
fun DailyRecommendDetailScreen(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DetailPageBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                DetailHeroSection(tracks = tracks)
            }

            item {
                Text(
                    text = "共 ${tracks.size} 首",
                    color = DetailSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            items(
                items = tracks,
                key = { it.id },
                contentType = { "daily-recommend-track" }
            ) { track ->
                DetailTrackRow(
                    track = track,
                    onClick = { onTrackClick(track) }
                )
                HorizontalDivider(
                    color = DetailDivider,
                    modifier = Modifier.padding(start = 84.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 20.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.82f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "关闭",
                tint = Color.Black,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun DetailHeroSection(
    tracks: List<Track>
) {
    val wallCovers = remember(tracks) {
        tracks.mapNotNull { it.coverUrl?.takeIf(String::isNotBlank) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .background(PosterWallSurface)
    ) {
        DetailPosterCoverGrid(
            covers = wallCovers,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.46f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.68f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.40f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.72f)
                        )
                    )
                )
        )

        Text(
            text = "每日推荐",
            color = Color.White,
            fontSize = 42.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = 48.dp)
        )
    }
}

@Composable
private fun DetailPosterCoverGrid(
    covers: List<String>,
    modifier: Modifier = Modifier
) {
    val slotCount = 20
    val displayItems = remember(covers) {
        List(slotCount) { index -> covers.getOrNull(index) }
    }

    BoxWithConstraints(modifier = modifier) {
        val gap = 1.dp
        val columns = 4
        val rows = 5
        val cellSize = (maxWidth - gap * (columns - 1)) / columns

        Column(
            verticalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier.fillMaxSize()
        ) {
            repeat(rows) { rowIndex ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(columns) { colIndex ->
                        val itemIndex = rowIndex * columns + colIndex
                        DetailPosterGridCell(
                            imageUrl = displayItems[itemIndex],
                            size = cellSize
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailPosterGridCell(
    imageUrl: String?,
    size: androidx.compose.ui.unit.Dp
) {
    val imageRequest = rememberCoverRequest(imageUrl = imageUrl, targetSize = size)
    Box(
        modifier = Modifier
            .size(size)
            .background(Color(0xFFDCDDE2))
    ) {
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = "推荐封面",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun DetailTrackRow(
    track: Track,
    onClick: () -> Unit
) {
    val imageRequest = rememberCoverRequest(
        imageUrl = track.coverUrl,
        targetSize = 56.dp
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE4E4E8)),
            contentAlignment = Alignment.Center
        ) {
            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = "歌曲封面",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = track.title,
                color = DetailPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artists.joinToString(" / ") { it.name }.ifBlank { "未知歌手" },
                color = DetailSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.album.title,
                color = DetailSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun rememberCoverRequest(
    imageUrl: String?,
    targetSize: androidx.compose.ui.unit.Dp
): ImageRequest? {
    if (imageUrl.isNullOrBlank()) return null

    val context = LocalContext.current
    val density = LocalDensity.current
    val targetSizePx = with(density) { targetSize.roundToPx() }.coerceAtLeast(1)
    return remember(imageUrl, targetSizePx) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .size(targetSizePx, targetSizePx)
            .crossfade(false)
            .build()
    }
}
