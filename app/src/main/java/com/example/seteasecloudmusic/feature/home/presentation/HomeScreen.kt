package com.example.seteasecloudmusic.feature.home.presentation

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import com.example.seteasecloudmusic.feature.home.domain.repository.RadarPlaylistData
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.core.model.Album
import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.common.toCoverThumbnailUrl
import com.example.seteasecloudmusic.core.ui.components.AppleMusicCollapsedTopBar
import com.example.seteasecloudmusic.core.ui.components.AppleMusicLargeTitle
import com.example.seteasecloudmusic.core.ui.components.UserAvatarButton
import com.example.seteasecloudmusic.core.ui.components.rememberAppleMusicCollapseFraction
import com.example.seteasecloudmusic.core.ui.components.verticalElasticOverscroll

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
    onPosterWallClick: (tracks: List<Track>, posterBounds: Rect, title: String) -> Unit,
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
        onTrackClick = { track -> viewModel.onTrackClick(track, uiState.tracks) },
        onFavoriteTrackClick = { track -> viewModel.onTrackClick(track, uiState.favoriteTracks) },
        onCardPlayClick = { card ->
            when (card.type) {
                BigCardType.DAILY_RECOMMEND -> viewModel.playAll(uiState.tracks, 0)
                BigCardType.RADAR_PLAYLIST -> viewModel.playAll(uiState.radarTracks, 0)
                BigCardType.HEART_MODE -> viewModel.playAll(if (uiState.favoriteTracks.isNotEmpty()) uiState.favoriteTracks else uiState.tracks)
                BigCardType.PRIVATE_DJ -> viewModel.playPrivateDj()
                BigCardType.SIMI_SONGS -> viewModel.playAll(if (uiState.favoriteTracks.isNotEmpty()) uiState.favoriteTracks else uiState.tracks)
                BigCardType.SIMI_ARTISTS -> viewModel.playAll(uiState.tracks)
            }
        },
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
    onFavoriteTrackClick: (Track) -> Unit,
    onCardPlayClick: (BigCardItem) -> Unit,
    onRetryClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onPosterWallClick: (tracks: List<Track>, posterBounds: Rect, title: String) -> Unit
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val lazyListState = rememberLazyListState()
    val collapseFractionState = rememberAppleMusicCollapseFraction(
        lazyListState = lazyListState,
        collapseThresholdDp = 76.dp
    )
    var posterWallBounds by remember { mutableStateOf(Rect.Zero) }
    var radarCardBounds by remember { mutableStateOf(Rect.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .verticalElasticOverscroll(),
            contentPadding = PaddingValues(
                top = statusBarHeight + 8.dp,
                bottom = bottomContentPadding
            )
        ) {
            item(key = "large_page_title") {
                AppleMusicLargeTitle(
                    title = "首页",
                    collapseFraction = collapseFractionState.value,
                    trailingContent = {
                        UserAvatarButton(
                            avatarUrl = avatarUrl,
                            displayName = displayName,
                            onClick = { onAvatarClick?.invoke() }
                        )
                    }
                )
            }

            if (uiState.isLoading && uiState.tracks.isEmpty()) {
                item(key = "home_top_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = HomeAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                item(key = "home_error_message") {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = uiState.errorMessage,
                                    color = Color(0xFFD32F2F),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                                Text(
                                    text = "提示：可轻触页面上方刷新按钮重新获取",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            item(key = "daily_recommend_wall") {
                DailyRecommendPosterWall(
                    tracks = uiState.tracks,
                    onClick = { onPosterWallClick(uiState.tracks, posterWallBounds, "每日推荐") },
                    onBoundsChanged = { posterWallBounds = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }

            // 1. 中间区域：「根据你喜爱的歌曲推荐」—— 始终展示
            item(key = "favorite_recommended_songs_section") {
                FavoriteRecommendedSongsSection(
                    title = uiState.favoriteSectionTitle,
                    tracks = uiState.favoriteTracks,
                    onTrackClick = onFavoriteTrackClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                )
            }

            // 2. 最下面区域：「推荐歌单」—— 始终展示
            item(key = "big_card_playlists_section") {
                BigCardPlaylistsSection(
                    tracks = uiState.tracks,
                    favoriteTracks = uiState.favoriteTracks,
                    radarTracks = uiState.radarTracks,
                    radarPlaylist = uiState.radarPlaylist,
                    likedCoverUrl = uiState.likedMusicCoverUrl,
                    privateDjCoverUrl = uiState.privateDjCoverUrl,
                    onCardClick = { card ->
                        when (card.type) {
                            BigCardType.RADAR_PLAYLIST -> onPosterWallClick(uiState.radarTracks, radarCardBounds, "雷达歌单")
                            else -> onCardPlayClick(card)
                        }
                    },
                    onCardPlayClick = onCardPlayClick,
                    onRadarCardPositioned = { radarCardBounds = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp, bottom = 16.dp)
                )
            }
        }

        // 覆盖在顶部的 Apple Music 风格渐变模糊导航栏（轻量级磨砂玻璃，滑动零开销）
        HomeCollapsedTopBar(
            collapseFractionProvider = { collapseFractionState.value },
            statusBarHeight = statusBarHeight,
            avatarUrl = avatarUrl,
            displayName = displayName,
            onAvatarClick = onAvatarClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun HomeCollapsedTopBar(
    collapseFractionProvider: () -> Float,
    statusBarHeight: Dp,
    avatarUrl: String?,
    displayName: String?,
    onAvatarClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    AppleMusicCollapsedTopBar(
        title = "首页",
        collapseFraction = collapseFractionProvider(),
        statusBarHeight = statusBarHeight,
        backdrop = null,
        modifier = modifier,
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

@Composable
private fun DailyRecommendPosterWall(
    tracks: List<Track>,
    onClick: () -> Unit,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val wallCovers = remember(tracks) {
        tracks.mapNotNull { it.coverUrl?.takeIf(String::isNotBlank) }
    }

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
    val displayItems = remember(covers) {
        List(slotCount) { index -> covers.getOrNull(index).toCoverThumbnailUrl(180) }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = modifier.fillMaxSize()
    ) {
        repeat(5) { rowIndex ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                repeat(4) { colIndex ->
                    val itemIndex = rowIndex * 4 + colIndex
                    PosterGridCell(
                        imageUrl = displayItems[itemIndex],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun PosterGridCell(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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

enum class BigCardType {
    DAILY_RECOMMEND,
    RADAR_PLAYLIST,
    HEART_MODE,
    PRIVATE_DJ,
    SIMI_SONGS,
    SIMI_ARTISTS
}

data class BigCardItem(
    val type: BigCardType,
    val title: String,
    val subtitle: String,
    val coverUrl: String? = null,
    val defaultGradient: List<Color>
)

@Composable
private fun FavoriteRecommendedSongsSection(
    title: String,
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayTracks = if (tracks.isNotEmpty()) tracks else remember {
        listOf(
            Track(
                id = 2650821035L,
                title = "如果",
                artists = listOf(Artist(id = 1L, name = "菠萝赛东 / Bo Peep")),
                album = Album(id = 1L, title = "如果", coverUrl = "http://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg"),
                coverUrl = "http://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg",
                recommendReason = "❤️ 悲伤联盟"
            ),
            Track(
                id = 1827600686L,
                title = "都市传说",
                artists = listOf(Artist(id = 2L, name = "Bo Peep")),
                album = Album(id = 2L, title = "都市传说", coverUrl = "http://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg"),
                coverUrl = "http://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg",
                recommendReason = "超76%人播放"
            ),
            Track(
                id = 2101344445L,
                title = "梦影",
                artists = listOf(Artist(id = 3L, name = "黄霄雲")),
                album = Album(id = 3L, title = "梦影", coverUrl = "http://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg"),
                coverUrl = "http://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg",
                recommendReason = "Hi-Res"
            )
        )
    }

    val songColumns = remember(displayTracks) {
        displayTracks.take(18).chunked(3)
    }

    val pagerState = rememberPagerState(pageCount = { songColumns.size })

    Column(modifier = modifier) {
        Text(
            text = title,
            color = HomePrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(start = 20.dp, end = 44.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val columnTracks = songColumns[page]
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                columnTracks.forEach { track ->
                    SongRecommendRowItem(
                        track = track,
                        onClick = { onTrackClick(track) },
                        onPlayClick = { onTrackClick(track) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SongRecommendRowItem(
    track: Track,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE8E8EC)),
            contentAlignment = Alignment.Center
        ) {
            if (!track.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = track.coverUrl.toCoverThumbnailUrl(140),
                    contentDescription = "歌曲封面",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = track.title,
                color = HomePrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!track.recommendReason.isNullOrBlank()) {
                    SongTagBadge(tag = track.recommendReason)
                }
                Text(
                    text = track.artists.joinToString(" / ") { it.name }.ifBlank { "未知歌手" },
                    color = HomeSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onPlayClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "播放",
                tint = Color(0xFF555555),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SongTagBadge(tag: String) {
    val cleanTag = tag.removePrefix("❤️").trim()
    val isVip = cleanTag.equals("VIP", ignoreCase = true)
    val hasHeart = tag.contains("❤️") || cleanTag.contains("红心") || cleanTag.contains("悲伤联盟")
    val isHiRes = cleanTag.equals("Hi-Res", ignoreCase = true) || cleanTag.equals("HiRes", ignoreCase = true)

    val badgeBg = when {
        isVip -> Color(0xFFFFF0F2)
        isHiRes -> Color(0xFFFFFAFA)
        else -> Color(0xFFFFECEF)
    }

    val textColor = when {
        isVip -> Color(0xFFE53935)
        isHiRes -> Color(0xFFE53935)
        else -> Color(0xFFE53935)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(badgeBg)
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (hasHeart) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(9.dp)
                )
            }
            Text(
                text = cleanTag,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BigCardPlaylistsSection(
    tracks: List<Track>,
    favoriteTracks: List<Track>,
    radarTracks: List<Track>,
    radarPlaylist: RadarPlaylistData?,
    likedCoverUrl: String?,
    privateDjCoverUrl: String?,
    onCardClick: (BigCardItem) -> Unit,
    onCardPlayClick: (BigCardItem) -> Unit,
    onRadarCardPositioned: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val simiTrackCover = favoriteTracks.firstOrNull()?.coverUrl
        ?: tracks.firstOrNull()?.coverUrl

    val defaultRecommendCover = favoriteTracks.firstOrNull()?.coverUrl
        ?: tracks.firstOrNull()?.coverUrl

    val djCover = privateDjCoverUrl?.takeIf { it.isNotBlank() }
        ?: defaultRecommendCover

    val cards = remember(tracks, favoriteTracks, radarTracks, radarPlaylist, likedCoverUrl, djCover) {
        listOf(
            BigCardItem(
                type = BigCardType.RADAR_PLAYLIST,
                title = "雷达歌单",
                subtitle = radarPlaylist?.description?.ifBlank { "反复聆听你爱的歌" } ?: "反复聆听你爱的歌",
                coverUrl = radarPlaylist?.coverUrl ?: radarTracks.firstOrNull()?.coverUrl ?: tracks.getOrNull(1)?.coverUrl,
                defaultGradient = listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
            ),
            BigCardItem(
                type = BigCardType.HEART_MODE,
                title = "心动模式",
                subtitle = "红心歌曲和相似推荐",
                coverUrl = likedCoverUrl ?: tracks.getOrNull(0)?.coverUrl,
                defaultGradient = listOf(Color(0xFF2C3E50), Color(0xFFFD746C))
            ),
            BigCardItem(
                type = BigCardType.PRIVATE_DJ,
                title = "私人 DJ",
                subtitle = "你的全天候音乐助理",
                coverUrl = djCover,
                defaultGradient = listOf(Color(0xFF141E30), Color(0xFF243B55))
            ),
            BigCardItem(
                type = BigCardType.SIMI_SONGS,
                title = "相似歌曲",
                subtitle = "从你喜欢的歌听起",
                coverUrl = simiTrackCover,
                defaultGradient = listOf(Color(0xFF9E8B83), Color(0xFF7A6861))
            ),
            BigCardItem(
                type = BigCardType.SIMI_ARTISTS,
                title = "相似艺人",
                subtitle = "常听歌手风格深度探索",
                coverUrl = tracks.getOrNull(4)?.coverUrl ?: tracks.getOrNull(2)?.coverUrl,
                defaultGradient = listOf(Color(0xFF1F1C18), Color(0xFF8E0E00))
            )
        )
    }

    Column(modifier = modifier) {
        Text(
            text = "推荐歌单",
            color = HomePrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(cards, key = { it.type.name }) { card ->
                BigCardPlaylistItem(
                    card = card,
                    onClick = { onCardClick(card) },
                    onPlayClick = { onCardPlayClick(card) },
                    modifier = if (card.type == BigCardType.RADAR_PLAYLIST) {
                        Modifier.onGloballyPositioned { coordinates ->
                            val pos = coordinates.positionInWindow()
                            val sz = coordinates.size
                            onRadarCardPositioned(
                                Rect(
                                    left = pos.x,
                                    top = pos.y,
                                    right = pos.x + sz.width,
                                    bottom = pos.y + sz.height
                                )
                            )
                        }
                    } else Modifier
                )
            }
        }
    }
}

@Composable
private fun BigCardPlaylistItem(
    card: BigCardItem,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(160.dp)
            .height(215.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        if (card.type == BigCardType.SIMI_SONGS && !card.coverUrl.isNullOrBlank()) {
            // 官方同款：深灰暖咖色底，顶部内嵌精致居中单曲方块
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF94837B)),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 22.dp)
                        .size(86.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = card.coverUrl.toCoverThumbnailUrl(200),
                        contentDescription = card.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(78.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
            }
        } else if (!card.coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = card.coverUrl.toCoverThumbnailUrl(360),
                contentDescription = card.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (card.type == BigCardType.PRIVATE_DJ) {
            // 官方同款手绘素描质感插画兜底
            PrivateDjCardArtwork()
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(card.defaultGradient))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = card.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = card.subtitle,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onPlayClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "播放",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun PrivateDjCardArtwork() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFE2E3E5), Color(0xFFCDCECF), Color(0xFFBCBDC0))
                )
            )
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeColor = Color(0xFF4A4B4D).copy(alpha = 0.65f)
            val lightLineColor = Color(0xFF6B6C6F).copy(alpha = 0.35f)

            // 顶部横向手绘草图线条
            drawRect(
                color = Color(0xFF2C2D2F),
                topLeft = androidx.compose.ui.geometry.Offset(16.dp.toPx(), 22.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(size.width * 0.7f, 3.5.dp.toPx())
            )
            // 装饰草图刻度线
            drawLine(
                color = strokeColor,
                start = androidx.compose.ui.geometry.Offset(16.dp.toPx(), 18.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(26.dp.toPx(), 18.dp.toPx()),
                strokeWidth = 1.5.dp.toPx()
            )

            // 中心唱片/DJ 艺术草图同心圆弧
            val centerX = size.width * 0.5f
            val centerY = size.height * 0.38f
            drawCircle(
                color = lightLineColor,
                radius = 36.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
            )
            drawCircle(
                color = lightLineColor,
                radius = 24.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
            )
            drawCircle(
                color = strokeColor,
                radius = 12.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )

            // 耳机外拱手绘曲线
            drawArc(
                color = Color(0xFF333333),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(centerX - 46.dp.toPx(), centerY - 40.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(92.dp.toPx(), 80.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )
        }

        // 顶部编号 "01" 手绘标注
        Text(
            text = "01",
            color = Color(0xFF333333).copy(alpha = 0.75f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
        )
    }
}
