package com.example.seteasecloudmusic.feature.radio.presentation

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.example.seteasecloudmusic.core.common.toCoverThumbnailUrl
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.ui.components.AppleMusicCollapsedTopBar
import com.example.seteasecloudmusic.core.ui.components.AppleMusicLargeTitle
import com.example.seteasecloudmusic.core.ui.components.UserAvatarButton
import com.example.seteasecloudmusic.core.ui.components.rememberAppleMusicCollapseFraction
import com.example.seteasecloudmusic.core.ui.components.verticalElasticOverscroll
import com.example.seteasecloudmusic.feature.mine.presentation.PlaylistDetailScreen
import com.example.seteasecloudmusic.feature.radio.domain.model.ArtistHotSectionData
import com.example.seteasecloudmusic.feature.radio.domain.model.FollowedArtistSectionData
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioArtistHotItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioArtistItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioFollowedArtistNewSongItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioMoodItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioPlaylistItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioRadarItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioRoamItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioToplistItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioTrendItem
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private val RadioBackground = Color.White
private val RadioTextPrimary = Color(0xFF111111)
private val RadioTextSecondary = Color(0xFF8F8F95)
private val RadioAccent = Color(0xFFFA233B)

@Composable
fun RadioRoute(
    topContentPadding: Dp,
    bottomContentPadding: Dp = 180.dp,
    avatarUrl: String? = null,
    displayName: String? = null,
    onAvatarClick: (() -> Unit)? = null,
    onArtistClick: ((artistId: Long, artistName: String, artistCoverUrl: String?) -> Unit)? = null,
    viewModel: RadioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RadioScreenContent(
        uiState = uiState,
        topContentPadding = topContentPadding,
        bottomContentPadding = bottomContentPadding,
        avatarUrl = avatarUrl,
        displayName = displayName,
        onAvatarClick = onAvatarClick,
        onArtistClick = onArtistClick,
        onOpenPlaylistDetail = viewModel::openPlaylistDetail,
        onClosePlaylistDetail = viewModel::closePlaylistDetail,
        onPlayTrackInPlaylist = viewModel::playTrackInPlaylist,
        onPlayAllTracks = viewModel::playAllTracks,
        onPlayToplist = viewModel::playToplist,
        onPlayRadar = viewModel::playRadar,
        onPlayRoam = viewModel::playRoam,
        onPlayPlaylist = viewModel::playPlaylist,
        onPlayArtistHot = viewModel::playArtistHot,
        onPlayTrend = viewModel::playTrend,
        onPlayMood = viewModel::playMood,
        onPlayFollowedNewSong = viewModel::playFollowedNewSong,
        onRetryClick = viewModel::onRetryClick
    )
}

@Composable
private fun RadioScreenContent(
    uiState: RadioUiState,
    topContentPadding: Dp,
    bottomContentPadding: Dp,
    avatarUrl: String? = null,
    displayName: String? = null,
    onAvatarClick: (() -> Unit)? = null,
    onArtistClick: ((artistId: Long, artistName: String, artistCoverUrl: String?) -> Unit)? = null,
    onOpenPlaylistDetail: (playlistId: Long, name: String, coverUrl: String?) -> Unit,
    onClosePlaylistDetail: () -> Unit,
    onPlayTrackInPlaylist: (Track, List<Track>) -> Unit,
    onPlayAllTracks: (List<Track>) -> Unit,
    onPlayToplist: (RadioToplistItem) -> Unit,
    onPlayRadar: (RadioRadarItem) -> Unit,
    onPlayRoam: (RadioRoamItem) -> Unit,
    onPlayPlaylist: (RadioPlaylistItem) -> Unit,
    onPlayArtistHot: (RadioArtistHotItem) -> Unit,
    onPlayTrend: (RadioTrendItem) -> Unit,
    onPlayMood: (RadioMoodItem) -> Unit,
    onPlayFollowedNewSong: (RadioFollowedArtistNewSongItem) -> Unit,
    onRetryClick: () -> Unit
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val lazyListState = rememberLazyListState()
    val collapseFractionState = rememberAppleMusicCollapseFraction(
        lazyListState = lazyListState,
        collapseThresholdDp = 76.dp
    )
    val radioBackdrop = rememberLayerBackdrop {
        drawContent()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RadioBackground)
            .layerBackdrop(radioBackdrop)
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
            // 大标题
            item(key = "radio_large_title") {
                AppleMusicLargeTitle(
                    title = "电台",
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

            if (uiState.isLoading) {
                item(key = "radio_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = RadioAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                item(key = "radio_error") {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clickable { onRetryClick() }
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
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "轻触可重新加载",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // 1. 排行榜 (点击卡片打开歌单详情，点击右下角按钮直接播放)
            if (uiState.toplists.isNotEmpty()) {
                item(key = "section_toplist") {
                    ToplistSection(
                        items = uiState.toplists,
                        onOpenDetail = { item -> onOpenPlaylistDetail(item.id, item.name, item.coverUrl) },
                        onPlayClick = onPlayToplist,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            // 2. 雷达歌单 (点击卡片打开歌单详情，点击右下角按钮直接播放)
            if (uiState.radarPlaylists.isNotEmpty()) {
                item(key = "section_radar") {
                    val radarTitle = if (!displayName.isNullOrBlank()) "${displayName} 的雷达歌单" else "雷达歌单"
                    RadarSection(
                        title = radarTitle,
                        items = uiState.radarPlaylists,
                        onOpenDetail = { item -> onOpenPlaylistDetail(item.id, item.title, item.coverUrl) },
                        onPlayClick = onPlayRadar,
                        modifier = Modifier.padding(top = 26.dp)
                    )
                }
            }

            // 3. 从你喜欢的歌曲开始漫游 (背景封面取色，上下全为歌名)
            if (uiState.roamItems.isNotEmpty()) {
                item(key = "section_roam") {
                    RoamSection(
                        items = uiState.roamItems,
                        onItemClick = onPlayRoam,
                        modifier = Modifier.padding(top = 26.dp)
                    )
                }
            }

            // 4. 歌手推荐（排在推荐歌单上面一行）
            if (uiState.recommendedSingers.isNotEmpty()) {
                item(key = "section_singers") {
                    RecommendedSingersSection(
                        singers = uiState.recommendedSingers,
                        onSingerClick = { artist ->
                            onArtistClick?.invoke(artist.id, artist.name, artist.avatarUrl)
                        },
                        modifier = Modifier.padding(top = 26.dp)
                    )
                }
            }

            // 5. 推荐歌单 (点击卡片打开歌单详情，点击右下角按钮直接播放)
            if (uiState.recommendedPlaylists.isNotEmpty()) {
                item(key = "section_recommended_playlists") {
                    RecommendedPlaylistsSection(
                        items = uiState.recommendedPlaylists,
                        onOpenDetail = { item -> onOpenPlaylistDetail(item.id, item.title, item.coverUrl) },
                        onPlayClick = onPlayPlaylist,
                        modifier = Modifier.padding(top = 26.dp)
                    )
                }
            }

            // 6. 艺人热门金曲
            if (uiState.artistHotSection.items.isNotEmpty()) {
                item(key = "section_artist_hot") {
                    ArtistHotSection(
                        sectionData = uiState.artistHotSection,
                        onItemClick = onPlayArtistHot,
                        modifier = Modifier.padding(top = 26.dp)
                    )
                }
            }

            // 7. 每周新热趋势 (点击卡片打开歌单详情，点击右下角按钮直接播放)
            if (uiState.trends.isNotEmpty()) {
                item(key = "section_trends") {
                    WeeklyTrendsSection(
                        items = uiState.trends,
                        onOpenDetail = { item -> onOpenPlaylistDetail(item.id, item.title, item.coverUrl) },
                        onPlayClick = onPlayTrend,
                        modifier = Modifier.padding(top = 26.dp)
                    )
                }
            }

            // 8. 心情氛围 (点击卡片打开歌单详情，点击右下角按钮直接播放)
            if (uiState.moodPlaylists.isNotEmpty()) {
                item(key = "section_mood") {
                    MoodSection(
                        items = uiState.moodPlaylists,
                        onOpenDetail = { item -> onOpenPlaylistDetail(item.id, item.title, item.coverUrl) },
                        onPlayClick = onPlayMood,
                        modifier = Modifier.padding(top = 26.dp)
                    )
                }
            }

            // 9. 你关注的艺人新动向 (仅展示真正关注的歌手)
            if (uiState.followedArtistSection.items.isNotEmpty()) {
                item(key = "section_followed_artists") {
                    FollowedArtistSection(
                        sectionData = uiState.followedArtistSection,
                        onItemClick = onPlayFollowedNewSong,
                        modifier = Modifier.padding(top = 26.dp)
                    )
                }
            }

            // 底部间距
            item(key = "radio_bottom_spacer") {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // 折叠顶栏
        AppleMusicCollapsedTopBar(
            title = "电台",
            collapseFraction = collapseFractionState.value,
            statusBarHeight = statusBarHeight,
            backdrop = null,
            modifier = Modifier.align(Alignment.TopCenter),
            trailingContent = {
                UserAvatarButton(
                    avatarUrl = avatarUrl,
                    displayName = displayName,
                    size = 34.dp,
                    onClick = { onAvatarClick?.invoke() }
                )
            }
        )

        // 歌单详情层（类似我的喜欢歌单页面）
        AnimatedVisibility(
            visible = uiState.activePlaylistDetail != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(20f)
        ) {
            uiState.activePlaylistDetail?.let { detail ->
                PlaylistDetailScreen(
                    detail = detail,
                    isLoading = uiState.isLoadingDetail,
                    backdrop = radioBackdrop,
                    onClose = onClosePlaylistDetail,
                    onPlayTrack = { track -> onPlayTrackInPlaylist(track, detail.tracks) },
                    onPlayAll = { onPlayAllTracks(detail.tracks) }
                )
            }
        }
    }
}

// =========================================================================
// 标题通用组件 (全部移除末尾的 >)
// =========================================================================
@Composable
private fun SectionHeader(
    title: String,
    onClick: (() -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        leadingContent?.invoke()
        Text(
            text = title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = RadioTextPrimary
        )
    }
}

// =========================================================================
// 1. 排行榜 (热歌榜、飙升榜、新歌榜排序，固定副标题高度防跳动，点卡片开详情)
// =========================================================================
@Composable
private fun ToplistSection(
    items: List<RadioToplistItem>,
    onOpenDetail: (RadioToplistItem) -> Unit,
    onPlayClick: (RadioToplistItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(title = "排行榜")
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            items(items, key = { it.id }) { item ->
                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .clickable { onOpenDetail(item) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    if (item.gradientColors.isNotEmpty()) item.gradientColors
                                    else listOf(Color(0xFFFF9A44), Color(0xFFFC6076))
                                )
                            )
                            .padding(10.dp)
                    ) {
                        // 播放量
                        if (item.playCount != null && item.playCount > 0L) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Headphones,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = formatPlayCount(item.playCount),
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // 榜单名称
                        Text(
                            text = item.name,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        // 独立播放图标快速播放
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                                .clickable { onPlayClick(item) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "播放",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    // 固定高度 36dp，确保划动到歌曲较少的项目时整体高度一致，下方内容绝不往上抬
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                    ) {
                        if (item.topSongs.isNotEmpty()) {
                            Text(
                                text = item.topSongs.joinToString("、"),
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                                color = Color(0xFF333333),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 2. 雷达歌单
// =========================================================================
@Composable
private fun RadarSection(
    title: String,
    items: List<RadioRadarItem>,
    onOpenDetail: (RadioRadarItem) -> Unit,
    onPlayClick: (RadioRadarItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(title = title)
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            items(items, key = { it.id }) { item ->
                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .clickable { onOpenDetail(item) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF222222))
                    ) {
                        AsyncImage(
                            model = item.coverUrl?.toCoverThumbnailUrl(300),
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.45f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.75f)
                                        )
                                    )
                                )
                        )

                        if (item.playCount != null && item.playCount > 0L) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Headphones,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = formatPlayCount(item.playCount),
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 10.dp, bottom = 10.dp)
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                                .clickable { onPlayClick(item) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "播放",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (item.subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.subtitle,
                            fontSize = 13.sp,
                            color = Color(0xFF333333),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 3. 从你喜欢的歌曲开始漫游 (背景封面取色，上下全为歌名)
// =========================================================================
@Composable
private fun RoamSection(
    items: List<RadioRoamItem>,
    onItemClick: (RadioRoamItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(title = "从你喜欢的歌曲开始漫游")
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            items(items, key = { it.id }) { item ->
                RoamCardItem(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
private fun RoamCardItem(
    item: RadioRoamItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var dominantColor by remember(item.id) {
        mutableStateOf(deriveDarkHueFromId(item.id))
    }

    LaunchedEffect(item.coverUrl) {
        val url = item.coverUrl
        if (!url.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = context.imageLoader
                    val req = ImageRequest.Builder(context)
                        .data(url.toCoverThumbnailUrl(100))
                        .allowHardware(false)
                        .build()
                    val res = loader.execute(req)
                    val drawable = res.drawable
                    if (drawable is BitmapDrawable) {
                        dominantColor = extractDominantDarkColor(drawable.bitmap)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    val animatedBg by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 350),
        label = "roamCardBg"
    )

    Box(
        modifier = Modifier
            .size(width = 152.dp, height = 196.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(animatedBg)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 上半部分：歌名与专辑封面
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // 上方歌名
                Text(
                    text = item.songTitle,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 中间专辑封面（带背板微卡片层级，与网易云视觉保持一致）
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 86.dp, height = 70.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 86.dp, height = 62.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                    )
                    AsyncImage(
                        model = item.coverUrl?.toCoverThumbnailUrl(200),
                        contentDescription = item.songTitle,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // 下半部分：半透明较深色底栏（上下全为真实歌名，连接显示）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                    .background(Color.Black.copy(alpha = 0.28f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = item.subtitle,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =========================================================================
// 4. 歌手推荐（排在推荐歌单上面一行）
// =========================================================================
@Composable
private fun RecommendedSingersSection(
    singers: List<RadioArtistItem>,
    onSingerClick: (RadioArtistItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            Text(
                text = "歌手推荐",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = RadioTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "发现你可能喜欢的音乐人",
                fontSize = 12.sp,
                color = RadioTextSecondary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(singers, key = { it.id }) { artist ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(76.dp)
                        .clickable { onSingerClick(artist) }
                ) {
                    AsyncImage(
                        model = artist.avatarUrl?.toCoverThumbnailUrl(160),
                        contentDescription = artist.name,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFEFEF)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = artist.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = RadioTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// =========================================================================
// 5. 推荐歌单 (点击卡片打开歌单详情，点击右下角按钮直接播放)
// =========================================================================
@Composable
private fun RecommendedPlaylistsSection(
    items: List<RadioPlaylistItem>,
    onOpenDetail: (RadioPlaylistItem) -> Unit,
    onPlayClick: (RadioPlaylistItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(title = "推荐歌单")
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            items(items, key = { it.id }) { item ->
                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .clickable { onOpenDetail(item) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEAEAEA))
                    ) {
                        AsyncImage(
                            model = item.coverUrl?.toCoverThumbnailUrl(300),
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // 标签 (如 "最近喜欢听" / "十万收藏")
                        if (!item.tag.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.tag,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // 播放图标快速播放
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.28f))
                                .clickable { onPlayClick(item) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "播放",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        color = Color(0xFF222222),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// =========================================================================
// 6. 艺人热门金曲
// =========================================================================
@Composable
private fun ArtistHotSection(
    sectionData: ArtistHotSectionData,
    onItemClick: (RadioArtistHotItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(
            title = "${sectionData.leadArtistName} 等艺人热门金曲",
            leadingContent = {
                if (!sectionData.leadArtistAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = sectionData.leadArtistAvatarUrl.toCoverThumbnailUrl(80),
                        contentDescription = sectionData.leadArtistName,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            items(sectionData.items, key = { it.artistId }) { item ->
                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .clickable { onItemClick(item) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 140.dp, height = 160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF333333))
                    ) {
                        AsyncImage(
                            model = item.coverUrl?.toCoverThumbnailUrl(300),
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.4f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.65f)
                                        )
                                    )
                                )
                        )

                        // 顶部播放量
                        if (item.playCount != null && item.playCount > 0L) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Headphones,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = formatPlayCount(item.playCount),
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                        // 艺人名称水印
                        Text(
                            text = item.artistName,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 10.dp, bottom = 10.dp)
                        )

                        // 播放图标
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.28f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "播放",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        color = Color(0xFF222222),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// =========================================================================
// 7. 每周新热趋势 (点击卡片打开歌单详情，点击右下角按钮直接播放)
// =========================================================================
@Composable
private fun WeeklyTrendsSection(
    items: List<RadioTrendItem>,
    onOpenDetail: (RadioTrendItem) -> Unit,
    onPlayClick: (RadioTrendItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(title = "每周新热趋势")
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            items(items, key = { it.id }) { item ->
                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .clickable { onOpenDetail(item) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF333333))
                    ) {
                        AsyncImage(
                            model = item.coverUrl?.toCoverThumbnailUrl(300),
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.45f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.7f)
                                        )
                                    )
                                )
                        )

                        // 顶部播放量
                        if (item.playCount != null && item.playCount > 0L) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Headphones,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = formatPlayCount(item.playCount),
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // TREND 标签
                        Text(
                            text = item.badge,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 10.dp, bottom = 10.dp)
                        )

                        // 播放图标快速播放
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.28f))
                                .clickable { onPlayClick(item) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "播放",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        color = Color(0xFF222222),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// =========================================================================
// 8. 心情氛围 (点击卡片打开歌单详情，点击右下角按钮直接播放)
// =========================================================================
@Composable
private fun MoodSection(
    items: List<RadioMoodItem>,
    onOpenDetail: (RadioMoodItem) -> Unit,
    onPlayClick: (RadioMoodItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(title = "心情氛围")
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            items(items, key = { it.id }) { item ->
                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .clickable { onOpenDetail(item) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF333333))
                    ) {
                        AsyncImage(
                            model = item.coverUrl?.toCoverThumbnailUrl(300),
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.45f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.7f)
                                        )
                                    )
                                )
                        )

                        // 顶部播放量
                        if (item.playCount != null && item.playCount > 0L) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Headphones,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = formatPlayCount(item.playCount),
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // 氛围标签
                        Text(
                            text = item.moodTag,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 10.dp, bottom = 10.dp)
                        )

                        // 播放图标快速播放
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.28f))
                                .clickable { onPlayClick(item) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "播放",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        color = Color(0xFF222222),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// =========================================================================
// 9. 你关注的艺人新动向 (严格只展示关注歌手)
// =========================================================================
@Composable
private fun FollowedArtistSection(
    sectionData: FollowedArtistSectionData,
    onItemClick: (RadioFollowedArtistNewSongItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(
            title = "你关注的艺人新动向",
            leadingContent = {
                if (sectionData.artistAvatars.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .width((24 + 14 * (sectionData.artistAvatars.size - 1)).dp)
                            .height(24.dp)
                    ) {
                        sectionData.artistAvatars.forEachIndexed { i, url ->
                            AsyncImage(
                                model = url.toCoverThumbnailUrl(80),
                                contentDescription = null,
                                modifier = Modifier
                                    .offset(x = (i * 14).dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            items(sectionData.items, key = { it.songId }) { item ->
                FollowedArtistCardItem(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
private fun FollowedArtistCardItem(
    item: RadioFollowedArtistNewSongItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var dominantColor by remember(item.songId) {
        mutableStateOf(Color(0xFF8B5E3C))
    }

    LaunchedEffect(item.coverUrl) {
        val url = item.coverUrl
        if (!url.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = context.imageLoader
                    val req = ImageRequest.Builder(context)
                        .data(url.toCoverThumbnailUrl(100))
                        .allowHardware(false)
                        .build()
                    val res = loader.execute(req)
                    val drawable = res.drawable
                    if (drawable is BitmapDrawable) {
                        dominantColor = extractDominantDarkColor(drawable.bitmap)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    val animatedBg by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 350),
        label = "followedCardBg"
    )

    Box(
        modifier = Modifier
            .size(width = 280.dp, height = 104.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(animatedBg)
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧专辑封面 + 新单曲标签
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = item.coverUrl?.toCoverThumbnailUrl(200),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // 新单曲标签
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(topStart = 8.dp, bottomEnd = 6.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.tag,
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 右侧文字信息
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.artistName,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "收录于《${item.albumName}》",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(36.dp))
        }

        // 播放按钮（位于卡片右下角）
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.28f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "播放",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// =========================================================================
// 格式化与颜色抽取辅助函数
// =========================================================================
private fun formatPlayCount(playCount: Long?): String {
    if (playCount == null || playCount <= 0L) return "0"
    return when {
        playCount >= 100_000_000L -> {
            val v = playCount / 100_000_000.0
            if (v >= 100) "${v.toLong()}亿"
            else String.format(Locale.getDefault(), "%.1f亿", v).replace(".0亿", "亿")
        }
        playCount >= 10_000L -> {
            val v = playCount / 10_000.0
            if (v >= 100) "${v.toLong()}万"
            else String.format(Locale.getDefault(), "%.1f万", v).replace(".0万", "万")
        }
        else -> playCount.toString()
    }
}

private fun deriveDarkHueFromId(id: Long): Color {
    val hues = listOf(
        Color(0xFF1E2F38), // deep teal
        Color(0xFF27382B), // deep forest
        Color(0xFF38262F), // deep plum
        Color(0xFF363220), // deep olive
        Color(0xFF202C38), // deep navy
        Color(0xFF382A20)  // deep sienna
    )
    val index = (Math.abs(id) % hues.size).toInt()
    return hues[index]
}

private fun extractDominantDarkColor(bitmap: Bitmap): Color {
    val scaled = Bitmap.createScaledBitmap(bitmap, 24, 24, false)
    val count = 24 * 24
    val pixels = IntArray(count)
    scaled.getPixels(pixels, 0, 24, 0, 0, 24, 24)

    val hsv = FloatArray(3)
    val hueBuckets = IntArray(12)
    val bucketSat = FloatArray(12)
    val bucketVal = FloatArray(12)

    var validPixels = 0
    for (p in pixels) {
        android.graphics.Color.colorToHSV(p, hsv)
        val s = hsv[1]
        val v = hsv[2]
        // 排除黑白灰无彩色以及极暗像素
        if (s >= 0.16f && v >= 0.15f) {
            val bucket = ((hsv[0] % 360f) / 30f).toInt().coerceIn(0, 11)
            hueBuckets[bucket]++
            bucketSat[bucket] += s
            bucketVal[bucket] += v
            validPixels++
        }
    }

    if (validPixels > 0) {
        var bestBucket = 0
        var maxCount = -1
        for (i in 0 until 12) {
            if (hueBuckets[i] > maxCount) {
                maxCount = hueBuckets[i]
                bestBucket = i
            }
        }
        val countInBucket = hueBuckets[bestBucket].coerceAtLeast(1)
        val avgSat = (bucketSat[bestBucket] / countInBucket).coerceIn(0.40f, 0.75f)
        val targetHue = bestBucket * 30f + 15f
        val avgVal = bucketVal[bestBucket] / countInBucket
        val targetVal = (avgVal * 0.55f).coerceIn(0.24f, 0.40f)
        val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(targetHue, avgSat, targetVal))
        return Color(colorInt)
    }

    return Color(0xFF262C30)
}
