package com.example.seteasecloudmusic.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.ui.components.AppleMusicCollapsedTopBar
import com.example.seteasecloudmusic.core.ui.components.AppleMusicLargeTitle
import com.example.seteasecloudmusic.core.ui.components.UserAvatarButton
import com.example.seteasecloudmusic.core.ui.components.rememberAppleMusicCollapseFraction

private val HomeBackground = Color.White
private val HomePrimary = Color(0xFF111111)
private val HomeSecondary = Color(0xFF8F8F95)
private val HomeDivider = Color(0xFFE2E2E6)
private val HomeAccent = Color(0xFFFA233B)
private val PosterWallSurface = Color(0xFFF6F6F8)

@Composable
fun HomeRoute(
    topContentPadding: Dp,
    bottomContentPadding: Dp = 180.dp,
    avatarUrl: String? = null,
    displayName: String? = null,
    onAvatarClick: (() -> Unit)? = null,
    onPosterWallClick: (tracks: List<Track>, posterBounds: Rect) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreenContent(
        uiState = uiState,
        topContentPadding = topContentPadding,
        bottomContentPadding = bottomContentPadding,
        avatarUrl = avatarUrl,
        displayName = displayName,
        onAvatarClick = onAvatarClick,
        onTrackClick = viewModel::onTrackClick,
        onRetryClick = viewModel::onRetryClick,
        onRefreshClick = viewModel::onRefreshClick,
        onPosterWallClick = onPosterWallClick
    )
}

@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    topContentPadding: Dp,
    bottomContentPadding: Dp,
    avatarUrl: String? = null,
    displayName: String? = null,
    onAvatarClick: (() -> Unit)? = null,
    onTrackClick: (Track) -> Unit,
    onRetryClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onPosterWallClick: (tracks: List<Track>, posterBounds: Rect) -> Unit
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val lazyListState = rememberLazyListState()
    val collapseFraction by rememberAppleMusicCollapseFraction(
        lazyListState = lazyListState,
        collapseThresholdDp = 76.dp
    )
    var posterWallBounds by remember { mutableStateOf(Rect.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
    ) {
        when {
            uiState.isLoading && uiState.tracks.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = statusBarHeight + 8.dp)
                ) {
                    AppleMusicLargeTitle(
                        title = "首页",
                        collapseFraction = 0f,
                        trailingContent = {
                            UserAvatarButton(
                                avatarUrl = avatarUrl,
                                displayName = displayName,
                                onClick = { onAvatarClick?.invoke() }
                            )
                        }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = HomeAccent, strokeWidth = 2.dp)
                    }
                }
            }

            uiState.errorMessage != null && uiState.tracks.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = statusBarHeight + 8.dp)
                ) {
                    AppleMusicLargeTitle(
                        title = "首页",
                        collapseFraction = 0f,
                        trailingContent = {
                            UserAvatarButton(
                                avatarUrl = avatarUrl,
                                displayName = displayName,
                                onClick = { onAvatarClick?.invoke() }
                            )
                        }
                    )
                    HomeErrorState(
                        message = uiState.errorMessage,
                        onRetryClick = onRetryClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }

            uiState.tracks.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = statusBarHeight + 8.dp)
                ) {
                    AppleMusicLargeTitle(
                        title = "首页",
                        collapseFraction = 0f,
                        trailingContent = {
                            UserAvatarButton(
                                avatarUrl = avatarUrl,
                                displayName = displayName,
                                onClick = { onAvatarClick?.invoke() }
                            )
                        }
                    )
                    HomeEmptyState(
                        onRefreshClick = onRefreshClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }

            else -> {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = statusBarHeight + 8.dp,
                        bottom = bottomContentPadding
                    )
                ) {
                    item(key = "large_page_title") {
                        AppleMusicLargeTitle(
                            title = "首页",
                            collapseFraction = collapseFraction,
                            trailingContent = {
                                UserAvatarButton(
                                    avatarUrl = avatarUrl,
                                    displayName = displayName,
                                    onClick = { onAvatarClick?.invoke() }
                                )
                            }
                        )
                    }

                    if (!uiState.errorMessage.isNullOrBlank()) {
                        item {
                            Text(
                                text = uiState.errorMessage,
                                color = Color(0xFFB52438),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    }

                    item(key = "daily_recommend_wall") {
                        DailyRecommendPosterWall(
                            tracks = uiState.tracks,
                            onClick = { onPosterWallClick(uiState.tracks, posterWallBounds) },
                            onBoundsChanged = { posterWallBounds = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        // 覆盖在顶部的 Apple Music 风格折叠顶栏（含毛玻璃材质、底部分割线与居中小标题）
        AppleMusicCollapsedTopBar(
            title = "首页",
            collapseFraction = collapseFraction,
            statusBarHeight = statusBarHeight,
            modifier = Modifier.align(Alignment.TopCenter),
            surfaceColor = Color.White,
            surfaceAlpha = 0.92f,
            trailingContent = {
                UserAvatarButton(
                    avatarUrl = avatarUrl,
                    displayName = displayName,
                    size = 34.dp,
                    onClick = { onAvatarClick?.invoke() }
                )
            }
        )
    }
}

@Composable
private fun DailyRecommendPosterWall(
    tracks: List<Track>,
    onClick: () -> Unit,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val wallCovers = tracks.mapNotNull { it.coverUrl?.takeIf(String::isNotBlank) }

    Column(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val pos = coordinates.positionInWindow()
                val sz = coordinates.size
                onBoundsChanged(
                    Rect(
                        left = pos.x,
                        top = pos.y,
                        right = pos.x + sz.width,
                        bottom = pos.y + sz.height
                    )
                )
            }
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.78f)
                .clip(RoundedCornerShape(26.dp))
                .background(PosterWallSurface)
        ) {
            PosterCoverGrid(
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
                    .padding(start = 18.dp, top = 18.dp)
            )
        }
    }
}

@Composable
private fun PosterCoverGrid(
    covers: List<String>,
    modifier: Modifier = Modifier
) {
    val slotCount = 20
    val displayItems = List(slotCount) { index -> covers.getOrNull(index) }

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
                        PosterGridCell(
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
private fun PosterGridCell(
    imageUrl: String?,
    size: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(Color(0xFFDCDDE2))
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "推荐封面",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun DailyTrackRow(
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
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE4E4E8)),
            contentAlignment = Alignment.Center
        ) {
            if (!track.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = "歌曲封面",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
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
                color = HomePrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artists.joinToString(" / ") { it.name }.ifBlank { "未知歌手" },
                color = HomeSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.album.title,
                color = HomeSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeErrorState(
    message: String?,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = Color(0xFFB52438),
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = message ?: "获取每日推荐失败",
                color = Color(0xFFB52438),
                fontSize = 13.sp
            )
            Button(onClick = onRetryClick) {
                Text(text = "重试")
            }
        }
    }
}

@Composable
private fun HomeEmptyState(
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "暂无每日推荐",
                color = HomePrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "请先确认登录状态，然后刷新",
                color = HomeSecondary,
                fontSize = 12.sp
            )
            Button(onClick = onRefreshClick) {
                Text(text = "刷新")
            }
        }
    }
}
