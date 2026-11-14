package com.example.seteasecloudmusic.feature.radio.presentation

import androidx.compose.runtime.saveable.rememberSaveable
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.example.seteasecloudmusic.core.ui.adaptive.*
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.seteasecloudmusic.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
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
import com.example.seteasecloudmusic.core.ui.components.AppleMusicMorphingTopBar
import com.example.seteasecloudmusic.core.ui.components.AppleMusicTopBarDefaults
import com.example.seteasecloudmusic.core.ui.components.UserAvatarButton
import com.example.seteasecloudmusic.core.ui.components.rememberAppleMusicCollapseFraction
import com.example.seteasecloudmusic.core.ui.components.verticalElasticOverscroll
import com.example.seteasecloudmusic.feature.mine.presentation.PlaylistDetailScreen
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.mine.presentation.trySyncExtractColor
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
private val RadioCoverBorder = Color(0x12000000)
private val RadioHorizontalCardWidth = 160.dp
private val RadioHorizontalCardSpacing = 12.dp
private val RadioCoverCornerRadius = 12.dp

@Composable
fun RadioRoute(
    topContentPadding: Dp,
    bottomContentPadding: Dp = 180.dp,
    avatarUrl: String? = null,
    displayName: String? = null,
    onAvatarClick: (() -> Unit)? = null,
    onArtistClick: ((artistId: Long, artistName: String, artistCoverUrl: String?) -> Unit)? = null,
    viewModel: RadioViewModel = hiltViewModel(),
    lazyListState: LazyListState = rememberLazyListState(),
    isForeground: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    androidx.compose.runtime.LaunchedEffect(isForeground) {
        if (isForeground) {
            viewModel.onEnterForeground()
        }
    }

    RadioScreenContent(
        uiState = uiState,
        topContentPadding = topContentPadding,
        bottomContentPadding = bottomContentPadding,
        avatarUrl = avatarUrl,
        displayName = displayName,
        onAvatarClick = onAvatarClick,
        onArtistClick = onArtistClick,
        onOpenPlaylistDetail = { id, name, coverUrl ->
            trySyncExtractColor(context, coverUrl)
            viewModel.openPlaylistDetail(id, name, coverUrl)
        },
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
        onRetryClick = viewModel::onRetryClick,
        lazyListState = lazyListState,
        isForeground = isForeground
    )
}

@Composable
internal fun RadioScreenContent(
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
    onRetryClick: () -> Unit,
    lazyListState: LazyListState = rememberLazyListState(),
    isForeground: Boolean = true
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val collapseFractionState = rememberAppleMusicCollapseFraction(
        lazyListState = lazyListState,
        collapseThresholdDp = AppleMusicTopBarDefaults.CollapseThreshold
    )
    val detailListState = rememberSaveable(uiState.activePlaylistDetail?.id, saver = LazyListState.Saver) { LazyListState() }
    val radioBackdrop = rememberLayerBackdrop {
        drawRect(RadioBackground)
        drawContent()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
    val window = LocalAdaptiveWindow.current
    val split = window.showDetailAlongside(uiState.activePlaylistDetail != null, maxWidth, maxHeight)
    val feed: @Composable () -> Unit = {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RadioBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(radioBackdrop)
        ) {
        val sectionTopPadding = if (LocalAdaptiveWindow.current.width >= 800.dp) 16.dp else 26.dp
        AdaptiveSectionColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .verticalElasticOverscroll(),
            contentPadding = PaddingValues(
                top = statusBarHeight + 8.dp,
                bottom = bottomContentPadding
            )
        ) {
            // 大标题占位（实体标题与头像在顶部随滑动连续收缩）
            item(key = "radio_large_title", fullSpan = true) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppleMusicTopBarDefaults.HeaderSpacerHeight)
                )
            }

            if (uiState.isLoading) {
                item(key = "radio_loading", fullSpan = true) {
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
                item(key = "radio_error", fullSpan = true) {
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
                        modifier = Modifier.padding(top = if (LocalAdaptiveWindow.current.width >= 800.dp) 16.dp else 12.dp)
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
                        modifier = Modifier.padding(top = sectionTopPadding)
                    )
                }
            }

            // 3. 从你喜欢的歌曲开始漫游 (背景封面取色，上下全为歌名)
            if (uiState.roamItems.isNotEmpty()) {
                item(key = "section_roam") {
                    RoamSection(
                        items = uiState.roamItems,
                        onItemClick = onPlayRoam,
                        modifier = Modifier.padding(top = sectionTopPadding)
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
                        modifier = Modifier.padding(top = sectionTopPadding)
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
                        modifier = Modifier.padding(top = sectionTopPadding)
                    )
                }
            }

            // 6. 艺人热门金曲
            if (uiState.artistHotSection.items.isNotEmpty()) {
                item(key = "section_artist_hot") {
                    ArtistHotSection(
                        sectionData = uiState.artistHotSection,
                        onItemClick = onPlayArtistHot,
                        modifier = Modifier.padding(top = sectionTopPadding)
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
                        modifier = Modifier.padding(top = sectionTopPadding)
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
                        modifier = Modifier.padding(top = sectionTopPadding)
                    )
                }
            }

            // 9. 你关注的艺人新动向 (仅展示真正关注的歌手)
            if (uiState.followedArtistSection.items.isNotEmpty()) {
                item(key = "section_followed_artists") {
                    FollowedArtistSection(
                        sectionData = uiState.followedArtistSection,
                        onItemClick = onPlayFollowedNewSong,
                        modifier = Modifier.padding(top = sectionTopPadding)
                    )
                }
            }

            // 底部间距
            item(key = "radio_bottom_spacer", fullSpan = true) {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
        }

        // 连续一体式折叠顶栏
        AppleMusicMorphingTopBar(
            title = "电台",
            collapseFractionProvider = { collapseFractionState.value },
            statusBarHeight = statusBarHeight,
            backdrop = radioBackdrop,
            modifier = Modifier.align(Alignment.TopCenter),
            trailingContent = {
                UserAvatarButton(
                    avatarUrl = avatarUrl,
                    displayName = displayName,
                    size = 46.dp,
                    onClick = { onAvatarClick?.invoke() }
                )
            }
        )

        val lastPlaylistDetail = remember { mutableStateOf<PlaylistDetail?>(null) }
        val playlistOpenCounter = remember { androidx.compose.runtime.mutableIntStateOf(0) }
        LaunchedEffect(uiState.activePlaylistDetail) {
            if (uiState.activePlaylistDetail != null) {
                lastPlaylistDetail.value = uiState.activePlaylistDetail
                playlistOpenCounter.intValue++
            }
        }
        LaunchedEffect(isForeground) {
            if (!isForeground) {
                lastPlaylistDetail.value = null
            }
        }

        // 歌单详情层（类似我的喜欢歌单页面）
        if (!split) {
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
                androidx.compose.runtime.key(playlistOpenCounter.intValue) {
                    PlaylistDetailScreen(
                        detail = detail,
                        isLoading = uiState.isLoadingDetail,
                        listState = detailListState,
                        backdrop = radioBackdrop,
                        onClose = onClosePlaylistDetail,
                        onPlayTrack = { track -> onPlayTrackInPlaylist(track, detail.tracks) },
                        onPlayAll = { onPlayAllTracks(detail.tracks) },
                        onShufflePlay = { onPlayAllTracks(detail.tracks.shuffled()) }
                    )
                }
            }
        }
    }    }
    }
    RetainedMusicFeed(!split) { AdaptiveContent { feed() } }
    if (split) {
        val detail = uiState.activePlaylistDetail
        if (detail != null) {
            PlaylistDetailScreen(detail, radioBackdrop, onClosePlaylistDetail,
                { onPlayTrackInPlaylist(it, detail.tracks) }, { onPlayAllTracks(detail.tracks) },
                onShufflePlay = { onPlayAllTracks(detail.tracks.shuffled()) },
                isLoading = uiState.isLoadingDetail, listState = detailListState)
        }
    }
    }

}

// =========================================================================
// 标题通用组件与卡片复用组件 (对齐艺人页专辑区排版与尺寸)
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
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 2.dp)
    ) {
        leadingContent?.invoke()
        Text(
            text = title,
            fontSize = 21.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.3).sp,
            fontWeight = FontWeight.Bold,
            color = RadioTextPrimary
        )
    }
}

@Composable
private fun RadioPlayCountBadge(
    playCount: Long?,
    modifier: Modifier = Modifier
) {
    if (playCount == null || playCount <= 0L) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_headphone),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = formatPlayCount(playCount),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RadioQuickPlayButton(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_mini_play),
            contentDescription = "播放",
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun RadioCardTitle(
    text: String,
    maxLines: Int = 2,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = RadioTextPrimary,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

// =========================================================================
// 1. 排行榜 (热歌榜、飙升榜、新歌榜排序，双行固定高度对齐，点卡片开详情)
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
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(RadioHorizontalCardSpacing)
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                Column(
                    modifier = Modifier
                        .width(RadioHorizontalCardWidth)
                        .clickable { onOpenDetail(item) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(RadioHorizontalCardWidth)
                            .clip(RoundedCornerShape(RadioCoverCornerRadius))
                            .border(0.5.dp, RadioCoverBorder, RoundedCornerShape(RadioCoverCornerRadius))
                    ) {
                        ToplistCoverArtwork(
                            item = item,
                            variant = index,
                            modifier = Modifier.fillMaxSize()
                        )

                        RadioPlayCountBadge(
                            playCount = item.playCount,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 10.dp, top = 10.dp)
                        )

                        // 右上角迷你榜单标识（阶梯柱状图 + TOP）
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 9.dp, end = 9.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Canvas(modifier = Modifier.size(width = 10.dp, height = 9.dp)) {
                                val barW = size.width * 0.24f
                                val gap = size.width * 0.14f
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.85f),
                                    topLeft = Offset(0f, size.height * 0.42f),
                                    size = Size(barW, size.height * 0.58f),
                                    cornerRadius = CornerRadius(2f, 2f)
                                )
                                drawRoundRect(
                                    color = Color.White,
                                    topLeft = Offset(barW + gap, 0f),
                                    size = Size(barW, size.height),
                                    cornerRadius = CornerRadius(2f, 2f)
                                )
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.78f),
                                    topLeft = Offset((barW + gap) * 2f, size.height * 0.25f),
                                    size = Size(barW, size.height * 0.75f),
                                    cornerRadius = CornerRadius(2f, 2f)
                                )
                            }
                            Text(
                                text = "TOP",
                                color = Color.White.copy(alpha = 0.94f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp
                            )
                        }

                        // 榜单名称
                        Text(
                            text = item.name,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 10.dp)
                        )

                        RadioQuickPlayButton(
                            onClick = { onPlayClick(item) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 6.dp, bottom = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    // 固定高度 38dp，双行单曲展示，避免跨行折断凌乱
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val firstSong = item.topSongs.firstOrNull()
                        if (!firstSong.isNullOrBlank()) {
                            RadioCardTitle(
                                text = "1. $firstSong",
                                maxLines = 1
                            )
                        }
                        if (item.topSongs.size > 1) {
                            val restSongs = item.topSongs
                                .drop(1)
                                .mapIndexed { index, song -> "${index + 2}. $song" }
                                .joinToString("  ")
                            Text(
                                text = restSongs,
                                color = RadioTextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToplistCoverArtwork(
    item: RadioToplistItem,
    variant: Int,
    modifier: Modifier = Modifier
) {
    val baseStart = item.gradientColors.getOrNull(0) ?: Color(0xFFFF9A44)
    val baseEnd = item.gradientColors.getOrNull(1) ?: Color(0xFFFC6076)
    val mode = ((variant % 4) + 4) % 4
    val accentWarm = remember(item.id) {
        shiftColorHsv(baseStart, hueDelta = 26f, satScale = 1.08f, valScale = 1.12f)
    }
    val accentCool = remember(item.id) {
        shiftColorHsv(baseEnd, hueDelta = -34f, satScale = 1.14f, valScale = 0.84f)
    }
    val accentDeep = remember(item.id) {
        shiftColorHsv(baseEnd, hueDelta = 18f, satScale = 1.16f, valScale = 0.68f)
    }

    Box(modifier = modifier) {
        // 不规则多层次色彩流动底图 + 排行榜动态柱状/光弧几何元素
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val (gradStart, gradEnd) = when (mode) {
                0 -> Offset(0f, 0f) to Offset(w, h)
                1 -> Offset(w * 0.85f, 0f) to Offset(w * 0.1f, h)
                2 -> Offset(0f, h * 0.65f) to Offset(w, h * 0.15f)
                else -> Offset(w * 0.25f, 0f) to Offset(w * 0.9f, h)
            }
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(baseStart, accentCool, baseEnd),
                    start = gradStart,
                    end = gradEnd
                )
            )

            // 三个错位有机色块，打破工整对称渐变
            val blob1Center = when (mode) {
                0 -> Offset(w * 0.86f, h * 0.16f)
                1 -> Offset(w * 0.18f, h * 0.22f)
                2 -> Offset(w * 0.78f, h * 0.78f)
                else -> Offset(w * 0.14f, h * 0.74f)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentWarm.copy(alpha = 0.78f), Color.Transparent),
                    center = blob1Center,
                    radius = w * 0.74f
                ),
                center = blob1Center,
                radius = w * 0.74f
            )

            val blob2Center = when (mode) {
                0 -> Offset(w * 0.16f, h * 0.88f)
                1 -> Offset(w * 0.84f, h * 0.82f)
                2 -> Offset(w * 0.20f, h * 0.14f)
                else -> Offset(w * 0.82f, h * 0.20f)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentDeep.copy(alpha = 0.74f), Color.Transparent),
                    center = blob2Center,
                    radius = w * 0.68f
                ),
                center = blob2Center,
                radius = w * 0.68f
            )

            val blob3Center = when (mode) {
                0 -> Offset(w * 0.44f, h * 0.48f)
                1 -> Offset(w * 0.60f, h * 0.36f)
                2 -> Offset(w * 0.42f, h * 0.60f)
                else -> Offset(w * 0.56f, h * 0.52f)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                    center = blob3Center,
                    radius = w * 0.52f
                ),
                center = blob3Center,
                radius = w * 0.52f
            )

            // 斜向柔光色带（不规则倾角）
            val ribbonAngle = when (mode) {
                0 -> -22f
                1 -> 18f
                2 -> -14f
                else -> 24f
            }
            rotate(degrees = ribbonAngle, pivot = Offset(w * 0.5f, h * 0.5f)) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(-w * 0.2f, h * 0.34f),
                    size = Size(w * 1.4f, h * 0.28f),
                    cornerRadius = CornerRadius(h * 0.14f, h * 0.14f)
                )
            }

            // 排行榜视觉元素 1：斜切角标圆弧（暗喻榜单与唱片）
            val arcCenter = when (mode % 2) {
                0 -> Offset(w * 0.92f, h * 0.88f)
                else -> Offset(w * 0.10f, h * 0.18f)
            }
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = w * 0.40f,
                center = arcCenter,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.015f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = w * 0.54f,
                center = arcCenter,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.010f)
            )

            // 排行榜视觉元素 2：左下角错落律动排名柱状图（带轻微倾角）
            val heights = listOf(0.42f, 0.78f, 0.56f, 0.96f, 0.64f)
            val barWidth = w * 0.044f
            val barGap = w * 0.026f
            val maxBarHeight = h * 0.19f
            val originX = w * 0.075f
            val baseY = h * 0.92f
            rotate(degrees = -5f, pivot = Offset(originX, baseY)) {
                heights.forEachIndexed { i, ratio ->
                    val bh = maxBarHeight * ratio
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.14f + ratio * 0.12f),
                        topLeft = Offset(originX + i * (barWidth + barGap), baseY - bh),
                        size = Size(barWidth, bh),
                        cornerRadius = CornerRadius(barWidth * 0.5f, barWidth * 0.5f)
                    )
                }
            }
        }

        // 排行榜视觉元素 3：不规则倾斜水印数字（#1 / TOP）
        Text(
            text = when (mode) {
                0 -> "TOP"
                1 -> "NO.1"
                2 -> "HOT"
                else -> "#1"
            },
            color = Color.White.copy(alpha = 0.11f),
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp,
            modifier = Modifier
                .align(if (mode % 2 == 0) Alignment.BottomEnd else Alignment.TopStart)
                .offset(
                    x = if (mode % 2 == 0) (-8).dp else 6.dp,
                    y = if (mode % 2 == 0) (-18).dp else 22.dp
                )
                .graphicsLayer {
                    rotationZ = if (mode % 2 == 0) -14f else 12f
                }
        )
    }
}

private fun shiftColorHsv(
    color: Color,
    hueDelta: Float,
    satScale: Float,
    valScale: Float
): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[0] = ((hsv[0] + hueDelta) % 360f + 360f) % 360f
    hsv[1] = (hsv[1] * satScale).coerceIn(0.28f, 0.95f)
    hsv[2] = (hsv[2] * valScale).coerceIn(0.22f, 0.96f)
    return Color(android.graphics.Color.HSVToColor(hsv))
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
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(RadioHorizontalCardSpacing)
        ) {
            items(items, key = { it.id }) { item ->
                Column(
                    modifier = Modifier
                        .width(RadioHorizontalCardWidth)
                        .clickable { onOpenDetail(item) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(RadioHorizontalCardWidth)
                            .clip(RoundedCornerShape(RadioCoverCornerRadius))
                            .background(Color(0xFF222222))
                            .border(0.5.dp, RadioCoverBorder, RoundedCornerShape(RadioCoverCornerRadius))
                    ) {
                        AsyncImage(
                            model = item.coverUrl?.toCoverThumbnailUrl(320),
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.0f to Color.Black.copy(alpha = 0.22f),
                                        0.24f to Color.Transparent,
                                        1.0f to Color.Transparent
                                    )
                                )
                        )

                        // 播放量 (避开封面左上角网易云 Logo 并与其对齐)
                        if (item.playCount != null && item.playCount > 0L) {
                            Text(
                                text = formatPlayCount(item.playCount),
                                color = Color.White.copy(alpha = 0.96f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = 33.dp, top = 10.dp)
                            )
                        }

                        RadioQuickPlayButton(
                            onClick = { onPlayClick(item) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 6.dp, bottom = 5.dp)
                        )
                    }

                    if (item.subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        RadioCardTitle(
                            text = item.subtitle,
                            maxLines = 1
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
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(RadioHorizontalCardSpacing)
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
            .size(width = RadioHorizontalCardWidth, height = 204.dp)
            .clip(RoundedCornerShape(RadioCoverCornerRadius))
            .background(animatedBg)
            .border(0.5.dp, RadioCoverBorder, RoundedCornerShape(RadioCoverCornerRadius))
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
                    .padding(top = 10.dp, bottom = 6.dp)
            ) {
                // 上方歌名
                Text(
                    text = item.songTitle,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 中间三张错落叠放专辑封面（两边为第2/第3首带小倾角，中间为主曲目）
                val firstCover = item.coverUrl ?: item.tracks.firstOrNull()?.coverUrl
                val secondCover = item.tracks.getOrNull(1)?.coverUrl ?: firstCover
                val thirdCover = item.tracks.getOrNull(2)?.coverUrl ?: secondCover ?: firstCover

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(94.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 左侧第 2 首封面（向左偏转小倾角）
                    AsyncImage(
                        model = secondCover?.toCoverThumbnailUrl(180),
                        contentDescription = null,
                        modifier = Modifier
                            .offset(x = (-28).dp, y = 3.dp)
                            .size(74.dp)
                            .graphicsLayer {
                                rotationZ = -8f
                                shape = RoundedCornerShape(8.dp)
                                clip = true
                            }
                            .background(Color.White.copy(alpha = 0.14f))
                            .border(0.6.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // 右侧第 3 首封面（向右偏转小倾角）
                    AsyncImage(
                        model = thirdCover?.toCoverThumbnailUrl(180),
                        contentDescription = null,
                        modifier = Modifier
                            .offset(x = 28.dp, y = 3.dp)
                            .size(74.dp)
                            .graphicsLayer {
                                rotationZ = 8f
                                shape = RoundedCornerShape(8.dp)
                                clip = true
                            }
                            .background(Color.White.copy(alpha = 0.14f))
                            .border(0.6.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // 中间第 1 首主封面
                    AsyncImage(
                        model = firstCover?.toCoverThumbnailUrl(240),
                        contentDescription = item.songTitle,
                        modifier = Modifier
                            .zIndex(1f)
                            .size(90.dp)
                            .shadow(elevation = 6.dp, shape = RoundedCornerShape(9.dp), clip = false)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(0.6.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(9.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // 下半部分：半透明较深色底栏（上下全为真实歌名，连接显示）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = RadioCoverCornerRadius, bottomEnd = RadioCoverCornerRadius))
                    .background(Color.Black.copy(alpha = 0.28f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = item.subtitle,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
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
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
        ) {
            Text(
                text = "歌手推荐",
                fontSize = 21.sp,
                lineHeight = 26.sp,
                letterSpacing = (-0.3).sp,
                fontWeight = FontWeight.Bold,
                color = RadioTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "发现你可能喜欢的音乐人",
                fontSize = 13.sp,
                color = RadioTextSecondary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(singers, key = { it.id }) { artist ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(80.dp)
                        .clickable { onSingerClick(artist) }
                ) {
                    AsyncImage(
                        model = artist.avatarUrl?.toCoverThumbnailUrl(200),
                        contentDescription = artist.name,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFEFEF))
                            .border(0.5.dp, RadioCoverBorder, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = artist.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
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
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(RadioHorizontalCardSpacing)
        ) {
            items(items, key = { it.id }) { item ->
                Column(
                    modifier = Modifier
                        .width(RadioHorizontalCardWidth)
                        .clickable { onOpenDetail(item) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(RadioHorizontalCardWidth)
                            .clip(RoundedCornerShape(RadioCoverCornerRadius))
                            .background(Color(0xFFEAEAEA))
                            .border(0.5.dp, RadioCoverBorder, RoundedCornerShape(RadioCoverCornerRadius))
                    ) {
                        AsyncImage(
                            model = item.coverUrl?.toCoverThumbnailUrl(320),
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
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        RadioQuickPlayButton(
                            onClick = { onPlayClick(item) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 6.dp, bottom = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    RadioCardTitle(
                        text = item.title,
                        maxLines = 2
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
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(RadioHorizontalCardSpacing)
        ) {
            items(sectionData.items, key = { it.artistId }) { item ->
                Column(
                    modifier = Modifier
                        .width(RadioHorizontalCardWidth)
                        .clickable { onItemClick(item) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(RadioHorizontalCardWidth)
                            .clip(RoundedCornerShape(RadioCoverCornerRadius))
                            .background(Color(0xFF333333))
                            .border(0.5.dp, RadioCoverBorder, RoundedCornerShape(RadioCoverCornerRadius))
                    ) {
                        AsyncImage(
                            model = item.coverUrl?.toCoverThumbnailUrl(320),
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.0f to Color.Transparent,
                                        0.68f to Color.Transparent,
                                        1.0f to Color.Black.copy(alpha = 0.45f)
                                    )
                                )
                        )

                        RadioPlayCountBadge(
                            playCount = item.playCount,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        )

                        // 艺人名称水印
                        Text(
                            text = item.artistName,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 10.dp, end = 44.dp, bottom = 10.dp)
                        )

                        RadioQuickPlayButton(
                            onClick = null,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 6.dp, bottom = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    RadioCardTitle(
                        text = item.title,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// =========================================================================
// 7 & 8 共用封面卡（每周新热趋势 / 心情氛围）
// =========================================================================
@Composable
private fun OverlayLabeledCoverCard(
    title: String,
    overlayLabel: String,
    overlayLabelWeight: FontWeight,
    coverUrl: String?,
    playCount: Long?,
    onOpenDetail: () -> Unit,
    onPlayClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(RadioHorizontalCardWidth)
            .clickable(onClick = onOpenDetail)
    ) {
        Box(
            modifier = Modifier
                .size(RadioHorizontalCardWidth)
                .clip(RoundedCornerShape(RadioCoverCornerRadius))
                .background(Color(0xFF333333))
                .border(0.5.dp, RadioCoverBorder, RoundedCornerShape(RadioCoverCornerRadius))
        ) {
            AsyncImage(
                model = coverUrl?.toCoverThumbnailUrl(320),
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.28f),
                            0.22f to Color.Transparent,
                            0.78f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.48f)
                        )
                    )
            )

            RadioPlayCountBadge(
                playCount = playCount,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )

            Text(
                text = overlayLabel,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = overlayLabelWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, end = 44.dp, bottom = 10.dp)
            )

            RadioQuickPlayButton(
                onClick = onPlayClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 6.dp, bottom = 5.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        RadioCardTitle(
            text = title,
            maxLines = 2
        )
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
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(RadioHorizontalCardSpacing)
        ) {
            items(items, key = { it.id }) { item ->
                OverlayLabeledCoverCard(
                    title = item.title,
                    overlayLabel = item.badge,
                    overlayLabelWeight = FontWeight.ExtraBold,
                    coverUrl = item.coverUrl,
                    playCount = item.playCount,
                    onOpenDetail = { onOpenDetail(item) },
                    onPlayClick = { onPlayClick(item) }
                )
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
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(RadioHorizontalCardSpacing)
        ) {
            items(items, key = { it.id }) { item ->
                OverlayLabeledCoverCard(
                    title = item.title,
                    overlayLabel = item.moodTag,
                    overlayLabelWeight = FontWeight.ExtraBold,
                    coverUrl = item.coverUrl,
                    playCount = item.playCount,
                    onOpenDetail = { onOpenDetail(item) },
                    onPlayClick = { onPlayClick(item) }
                )
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
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(RadioHorizontalCardSpacing)
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
            .size(width = 296.dp, height = 112.dp)
            .clip(RoundedCornerShape(RadioCoverCornerRadius))
            .background(animatedBg)
            .border(0.5.dp, RadioCoverBorder, RoundedCornerShape(RadioCoverCornerRadius))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧专辑封面 + 新单曲标签
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = item.coverUrl?.toCoverThumbnailUrl(240),
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
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.tag,
                        fontSize = 10.sp,
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
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.artistName,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "收录于《${item.albumName}》",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(40.dp))
        }

        RadioQuickPlayButton(
            onClick = null,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
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
