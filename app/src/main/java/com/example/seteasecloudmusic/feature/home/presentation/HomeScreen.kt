package com.example.seteasecloudmusic.feature.home.presentation

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.seteasecloudmusic.core.ui.adaptive.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.zIndex
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.mine.presentation.PlaylistDetailScreen
import com.example.seteasecloudmusic.feature.mine.presentation.PlaylistDetailStyle
import com.example.seteasecloudmusic.core.ui.components.LocalGlassBackdrop
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.pager.PageSize
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
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import com.example.seteasecloudmusic.feature.home.domain.repository.RadarPlaylistData
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.seteasecloudmusic.core.ui.components.AppLoadingIndicator
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleClip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.seteasecloudmusic.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.seteasecloudmusic.core.model.Album
import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.common.toCoverThumbnailUrl
import com.example.seteasecloudmusic.core.ui.components.AmbientTopPinkBackground
import com.example.seteasecloudmusic.core.ui.components.AppPullToRefresh
import com.example.seteasecloudmusic.core.ui.components.AppleMusicMorphingTopBar
import com.example.seteasecloudmusic.core.ui.components.AppleMusicTopBarDefaults
import com.example.seteasecloudmusic.core.ui.components.LocalPageVisible
import com.example.seteasecloudmusic.core.ui.components.RoamStaggeredCoverStack
import com.example.seteasecloudmusic.core.ui.components.UserAvatarButton
import com.example.seteasecloudmusic.core.ui.components.deriveDarkHueFromId
import com.example.seteasecloudmusic.core.ui.components.extractCachedBottomEdgeColor
import com.example.seteasecloudmusic.core.ui.components.extractCachedDominantDarkColor
import com.example.seteasecloudmusic.core.ui.components.getCachedBottomEdgeColor
import com.example.seteasecloudmusic.core.ui.components.getCachedDominantDarkColor
import com.example.seteasecloudmusic.core.ui.components.rememberAppleMusicCollapseFraction
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive

private val HomeBackground: Color
    @Composable
    get() = AppTheme.colors.background
private val HomePrimary: Color
    @Composable
    get() = AppTheme.colors.textPrimary
private val HomeSecondary: Color
    @Composable
    get() = AppTheme.colors.textSecondary
private val HomeDivider: Color
    @Composable
    get() = AppTheme.colors.divider
private val HomeAccent: Color
    @Composable
    get() = AppTheme.colors.accent
private val PosterWallSurface = Color(0xFF1C1C22)

@Composable
fun HomeRoute(
    topContentPadding: Dp,
    bottomContentPadding: Dp = 180.dp,
    avatarUrl: String? = null,
    displayName: String? = null,
    onAvatarClick: (() -> Unit)? = null,
    onPosterWallClick: ((tracks: List<Track>, title: String, coverUrl: String?) -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel(),
    lazyListState: LazyListState = rememberLazyListState(),
    isForeground: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                BigCardType.HEART_MODE -> {
                    Toast.makeText(context, "正在开启心动模式", Toast.LENGTH_SHORT).show()
                    viewModel.playHeartMode()
                }
                BigCardType.PRIVATE_DJ -> viewModel.playPrivateDj()
                BigCardType.SIMI_SONGS -> viewModel.playSimiSongs()
                BigCardType.NEW_SONG_RADAR -> viewModel.playAll(
                    if (uiState.newSongRadarTracks.isNotEmpty()) uiState.newSongRadarTracks else uiState.tracks,
                    0
                )
            }
        },
        onRetryClick = viewModel::onRetryClick,
        onRefreshClick = viewModel::onRefreshClick,
        onPosterWallClick = { tracks, title, coverUrl ->
            if (onPosterWallClick != null) {
                onPosterWallClick(tracks, title, coverUrl)
            } else {
                viewModel.openPlaylistDetail(tracks, title, coverUrl)
            }
        },
        onClosePlaylistDetail = viewModel::closePlaylistDetail,
        onPlayAll = { tracks -> viewModel.playAll(tracks, 0) },
        onShufflePlay = { tracks -> viewModel.playAll(tracks.shuffled(), 0) },
        lazyListState = lazyListState,
        isForeground = isForeground
    )
}

@Composable
internal fun HomeScreenContent(
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
    onPosterWallClick: (tracks: List<Track>, title: String, coverUrl: String?) -> Unit,
    onClosePlaylistDetail: () -> Unit = {},
    onPlayAll: (List<Track>) -> Unit = {},
    onShufflePlay: (List<Track>) -> Unit = {},
    lazyListState: LazyListState = rememberLazyListState(),
    isForeground: Boolean = true
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val collapseFractionState = rememberAppleMusicCollapseFraction(
        lazyListState = lazyListState,
        collapseThresholdDp = AppleMusicTopBarDefaults.CollapseThreshold
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
    val detailListState = rememberSaveable(uiState.activePlaylistDetail?.id, saver = LazyListState.Saver) { LazyListState() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
    ) {
        val window = LocalAdaptiveWindow.current
        val split = window.showDetailAlongside(uiState.activePlaylistDetail != null, maxWidth, maxHeight)

        val isPosterWallInViewport by remember(lazyListState) {
            derivedStateOf {
                lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "daily_recommend_wall" }
            }
        }
        val isPosterWallAnimating = LocalPageVisible.current &&
            isForeground &&
            (split || uiState.activePlaylistDetail == null) &&
            isPosterWallInViewport

        val homeBgColor = HomeBackground
        val homeBgColorState = rememberUpdatedState(homeBgColor)
        val colors = AppTheme.colors

        val feedContent: @Composable () -> Unit = {
            val homeBackdrop = rememberLayerBackdrop {
                drawRect(homeBgColorState.value)
                drawContent()
            }
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().layerBackdrop(homeBackdrop)) {
                    AmbientTopPinkBackground(baseColor = HomeBackground)
                AdaptiveContent {
                AppPullToRefresh(
                    isRefreshing = uiState.isLoading && uiState.tracks.isNotEmpty(),
                    onRefresh = onRefreshClick,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = statusBarHeight + 56.dp)
                ) {
                AdaptiveSectionColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = statusBarHeight + 8.dp,
                bottom = bottomContentPadding
            )
        ) {
            item(key = "large_page_title", fullSpan = true) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppleMusicTopBarDefaults.HeaderSpacerHeight)
                )
            }

            if (uiState.isLoading && uiState.tracks.isEmpty()) {
                item(key = "home_top_loading", fullSpan = true) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppLoadingIndicator(
                            color = HomeAccent,
                            strokeWidth = 2.dp,
                            size = 24.dp
                        )
                    }
                }
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                item(key = "home_error_message", fullSpan = true) {
                    Card(
                        cornerRadius = 16.dp,
                        colors = CardDefaults.defaultColors(color = colors.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_warning),
                                contentDescription = null,
                                tint = colors.errorIcon,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = uiState.errorMessage,
                                    color = colors.errorContent,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                                Text(
                                    text = "提示：可轻触页面上方刷新按钮重新获取",
                                    color = colors.textSecondary,
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
                    onClick = { onPosterWallClick(uiState.tracks, "每日推荐", uiState.tracks.firstOrNull()?.coverUrl) },
                    isAnimating = isPosterWallAnimating,
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
            item(key = "big_card_playlists_section", fullSpan = true) {
                BigCardPlaylistsSection(
                    tracks = uiState.tracks,
                    favoriteTracks = uiState.favoriteTracks,
                    radarTracks = uiState.radarTracks,
                    radarPlaylist = uiState.radarPlaylist,
                    newSongRadarTracks = uiState.newSongRadarTracks,
                    newSongRadarPlaylist = uiState.newSongRadarPlaylist,
                    privateDjTracks = uiState.privateDjTracks,
                    simiSongsTracks = uiState.simiSongsTracks,
                    likedCoverUrl = uiState.likedMusicCoverUrl,
                    privateDjCoverUrl = uiState.privateDjCoverUrl,
                    onCardClick = { card ->
                        when (card.type) {
                            BigCardType.HEART_MODE,
                            BigCardType.PRIVATE_DJ,
                            BigCardType.SIMI_SONGS -> onCardPlayClick(card)
                            else -> {
                                val targetTracks = when (card.type) {
                                    BigCardType.DAILY_RECOMMEND -> uiState.tracks
                                    BigCardType.RADAR_PLAYLIST -> uiState.radarTracks
                                    BigCardType.NEW_SONG_RADAR -> if (uiState.newSongRadarTracks.isNotEmpty()) uiState.newSongRadarTracks else uiState.tracks
                                    BigCardType.HEART_MODE, BigCardType.PRIVATE_DJ, BigCardType.SIMI_SONGS -> emptyList()
                                }
                                if (targetTracks.isNotEmpty()) {
                                    onPosterWallClick(targetTracks, card.title, card.coverUrl)
                                } else {
                                    onCardPlayClick(card)
                                }
                            }
                        }
                    },
                    onCardPlayClick = onCardPlayClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp, bottom = 16.dp)
                )
            }
        }
                }

        }
        }
        // 覆盖在顶部的 Apple Music 风格渐进式高斯模糊导航栏
        HomeCollapsedTopBar(
            collapseFractionProvider = { collapseFractionState.value },
            statusBarHeight = statusBarHeight,
            backdrop = homeBackdrop,
            avatarUrl = avatarUrl,
            displayName = displayName,
            onAvatarClick = onAvatarClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
    }

    RetainedMusicFeed(!split) { feedContent() }
    if (split) {
        uiState.activePlaylistDetail?.let { detail ->
            val fallbackBackdrop = rememberLayerBackdrop { drawContent() }
            val glassBackdrop = LocalGlassBackdrop.current ?: fallbackBackdrop
            PlaylistDetailScreen(
                detail, glassBackdrop, onClosePlaylistDetail, onTrackClick,
                { onPlayAll(detail.tracks) },
                onShufflePlay = { onShufflePlay(detail.tracks) }, listState = detailListState,
                defaultStyle = homeDetailDefaultStyle(detail)
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
                androidx.compose.runtime.key(playlistOpenCounter.intValue) {
                    val fallbackBackdrop = rememberLayerBackdrop { drawContent() }
                    val glassBackdrop = LocalGlassBackdrop.current ?: fallbackBackdrop
                    PlaylistDetailScreen(
                        detail = detail,
                        isLoading = false,
                        listState = detailListState,
                        backdrop = glassBackdrop,
                        onClose = onClosePlaylistDetail,
                        onPlayTrack = onTrackClick,
                        onPlayAll = { onPlayAll(detail.tracks) },
                        onShufflePlay = { onShufflePlay(detail.tracks) },
                        defaultStyle = homeDetailDefaultStyle(detail)
                    )
                }
            }
        }
    }
    }
}

internal fun homeDetailDefaultStyle(detail: PlaylistDetail): PlaylistDetailStyle =
    if (detail.id == -1001L || detail.name == "每日推荐") {
        PlaylistDetailStyle.DYNAMIC_POSTER_WALL
    } else {
        PlaylistDetailStyle.FULL_COVER
    }

@Composable
private fun HomeCollapsedTopBar(
    collapseFractionProvider: () -> Float,
    statusBarHeight: Dp,
    backdrop: Backdrop,
    avatarUrl: String?,
    displayName: String?,
    onAvatarClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    AppleMusicMorphingTopBar(
        title = "首页",
        collapseFractionProvider = collapseFractionProvider,
        statusBarHeight = statusBarHeight,
        backdrop = backdrop,
        modifier = modifier,
        trailingContent = {
            UserAvatarButton(
                avatarUrl = avatarUrl,
                displayName = displayName,
                size = 46.dp,
                onClick = { onAvatarClick?.invoke() }
            )
        }
    )
}

@Composable
private fun DailyRecommendPosterWall(
    tracks: List<Track>,
    onClick: () -> Unit,
    isAnimating: Boolean = true,
    modifier: Modifier = Modifier
) {
    val wallCovers = remember(tracks) {
        tracks.mapNotNull { it.coverUrl?.takeIf(String::isNotBlank) }
    }

    Box(
        modifier = modifier
            .heightIn(max = if (LocalAdaptiveWindow.current.width >= 600.dp) 380.dp else Dp.Unspecified)
            .aspectRatio(1.08f)
            .squircleClip(24.dp)
            .background(PosterWallSurface)
            .clickable(onClick = onClick)
    ) {
        ScrollingPosterWall(
            covers = wallCovers,
            isAnimating = isAnimating,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.50f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.72f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "每日推荐",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5f).sp
            )
            Text(
                text = "今天，听点喜欢的",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
internal fun ScrollingPosterWall(
    covers: List<String>,
    modifier: Modifier = Modifier,
    isAnimating: Boolean = true
) {
    if (covers.isEmpty()) return

    // 可挂起、断点续播的进度状态：当页面切走、滑出视口、被全屏播放器/详情页遮挡或退到后台时
    // 彻底停止 Choreographer 帧回调，恢复可见时从原位置无缝继续，既保零跳变又彻底消除空载重绘。
    val scrollProgressState = remember { mutableFloatStateOf(0f) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val isLifecycleResumed by remember(lifecycleOwner) {
        lifecycleOwner.lifecycle.currentStateFlow.map { it.isAtLeast(Lifecycle.State.RESUMED) }
    }.collectAsState(initial = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))

    val shouldAnimate = isAnimating && LocalPageVisible.current && isLifecycleResumed
    LaunchedEffect(shouldAnimate) {
        if (!shouldAnimate) return@LaunchedEffect
        var lastFrameNanos = 0L
        val cycleDurationNanos = 64_000_000_000.0
        while (isActive) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos != 0L) {
                    val dtNanos = (frameNanos - lastFrameNanos).coerceIn(0L, 100_000_000L)
                    scrollProgressState.floatValue =
                        (scrollProgressState.floatValue + (dtNanos / cycleDurationNanos).toFloat()) % 1f
                }
                lastFrameNanos = frameNanos
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.clipToBounds()
    ) {
        val density = LocalDensity.current
        val distinctCovers = remember(covers) { covers.distinct() }

        // 5 列，列宽为卡片宽度的 38%
        val colWidth = maxWidth * 0.38f
        val colCount = 5

        // 中间 3 列均分所有封面池
        // 启动性能上限：单列最多 5 张。之前是 8×3=120 个并发图片请求 + 120 个 crossfade 动画。
        // 必须保留 3 组循环（totalRepetitions = 3）：因为奇数列初始向上偏移 1 周期（-1f * cycleHeightPx），
        // 若只有 2 组循环，奇数列底部半区会完全镂空，在 30° 旋转后暴露 2 个空白格子。
        val cycleCount = remember(distinctCovers) {
            maxOf(distinctCovers.size / 3, 4).coerceAtMost(5)
        }
        val cycleHeight = colWidth * cycleCount
        val cycleHeightPx = with(density) { cycleHeight.toPx() }

        // 重复 3 组循环，总高度充足覆盖倾斜视口与滑动偏移
        val totalRepetitions = 3
        val totalRows = cycleCount * totalRepetitions
        val canvasWidth = colWidth * colCount
        val canvasHeight = colWidth * totalRows

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize(unbounded = true, align = Alignment.Center)
                    .requiredSize(width = canvasWidth, height = canvasHeight)
                    .graphicsLayer {
                        rotationZ = 30f
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    repeat(colCount) { colIndex ->
                        val columnCovers = remember(distinctCovers, colIndex, cycleCount) {
                            if (distinctCovers.isEmpty()) emptyList()
                            else {
                                val count = distinctCovers.size
                                List(cycleCount) { i ->
                                    val index = if (colIndex in 1..3) {
                                        // 中间三列（colIndex 1、2、3）：均分所有去重封面，互不重复
                                        val midIndex = colIndex - 1
                                        (midIndex * cycleCount + i) % count
                                    } else {
                                        // 边上两列（colIndex 0、4）：重新均分到这两列
                                        val outerIndex = if (colIndex == 0) 0 else 1
                                        (outerIndex * (count / 2) + i) % count
                                    }
                                    distinctCovers[index].toCoverThumbnailUrl(280)
                                }
                            }
                        }

                        // 5 列交替滚动（倾斜方向为右上到左下）：
                        // colIndex 1（左侧列）和 colIndex 3（右侧列）沿列往左下滚动
                        // colIndex 2（中间列）沿列往右上滚动
                        // 外侧两列（colIndex 0、4）配合交替
                        val isScrollingDown = (colIndex % 2 == 1)

                        Column(
                            modifier = Modifier
                                .requiredWidth(colWidth)
                                .requiredHeight(canvasHeight)
                                .graphicsLayer {
                                    val scrollProgress = scrollProgressState.floatValue
                                    translationY = if (isScrollingDown) {
                                        (scrollProgress - 1f) * cycleHeightPx
                                    } else {
                                        -scrollProgress * cycleHeightPx
                                    }
                                },
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            repeat(totalRepetitions) {
                                columnCovers.forEach { url ->
                                    PosterCoverCell(
                                        imageUrl = url,
                                        modifier = Modifier.requiredSize(colWidth)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PosterCoverCell(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(imageUrl) {
        if (imageUrl.isNullOrBlank()) null
        else {
            ImageRequest.Builder(context)
                .data(imageUrl)
                // 海报墙是 100dp 级小格：关 crossfade，省掉几十个并发的 alpha 动画，
                // 否则加载期每一帧都在做几十层混合，miniPlayer 手势会被掉帧吞掉。
                .crossfade(false)
                .build()
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF26262E))
    ) {
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
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
                .squircleClip(10.dp)
                .background(AppTheme.colors.placeholder),
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
    val colors = AppTheme.colors
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = null,
                tint = colors.errorIcon,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = message ?: "获取每日推荐失败",
                color = colors.errorContent,
                fontSize = 13.sp
            )
            Button(
                onClick = onRetryClick,
                colors = ButtonDefaults.buttonColorsPrimary()
            ) {
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
            Button(
                onClick = onRefreshClick,
                colors = ButtonDefaults.buttonColorsPrimary()
            ) {
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
    NEW_SONG_RADAR
}

data class BigCardItem(
    val type: BigCardType,
    val title: String,
    val subtitle: String,
    val coverUrl: String? = null,
    val previewTracks: List<Track> = emptyList(),
    val defaultGradient: List<Color>
)

/**
 * 单行自适应字号标题：当标题过长一行放不下时，动态计算并缩小字号，确保刚好放满一行或略短，绝不换行
 */
@Composable
private fun AutoFitSingleLineTitle(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = 20.sp,
    minFontSize: TextUnit = 11.sp,
    color: Color = HomePrimary,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val availableWidthPx = constraints.maxWidth
        val calculatedFontSize = remember(text, availableWidthPx, density) {
            if (availableWidthPx <= 0) {
                maxFontSize
            } else {
                val maxStyle = TextStyle(
                    fontSize = maxFontSize,
                    fontWeight = fontWeight
                )
                val fullResult = textMeasurer.measure(
                    text = text,
                    style = maxStyle,
                    maxLines = 1,
                    softWrap = false
                )
                val fullWidth = fullResult.size.width

                if (fullWidth <= availableWidthPx) {
                    maxFontSize
                } else {
                    // 按比例缩放，并乘 0.98 预留 2% 安全微小余量，确保刚好放满一行或略短
                    val ratio = availableWidthPx.toFloat() / fullWidth.toFloat()
                    var currentSp = (maxFontSize.value * ratio * 0.98f)
                        .coerceIn(minFontSize.value, maxFontSize.value)

                    // 步进微调：若仍超出可用宽度则逐步递减 0.5sp，直至严丝合缝放下一行
                    while (currentSp > minFontSize.value) {
                        val testWidth = textMeasurer.measure(
                            text = text,
                            style = TextStyle(fontSize = currentSp.sp, fontWeight = fontWeight),
                            maxLines = 1,
                            softWrap = false
                        ).size.width
                        if (testWidth <= availableWidthPx) {
                            break
                        }
                        currentSp -= 0.5f
                    }
                    currentSp.sp
                }
            }
        }

        Text(
            text = text,
            color = color,
            fontSize = calculatedFontSize,
            fontWeight = fontWeight,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

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
        displayTracks.take(18).chunked(4)
    }

    val pagerState = rememberPagerState(pageCount = { songColumns.size })

    Column(modifier = modifier) {
        AutoFitSingleLineTitle(
            text = title,
            color = HomePrimary,
            maxFontSize = 20.sp,
            minFontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        HorizontalPager(
            state = pagerState,
            pageSize = object : PageSize {
                override fun androidx.compose.ui.unit.Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int {
                    val columns = (availableSpace / 300.dp.roundToPx()).coerceAtLeast(1)
                    return (availableSpace - pageSpacing * (columns - 1)) / columns
                }
            },
            contentPadding = PaddingValues(start = 20.dp, end = 44.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val columnTracks = songColumns[page]
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                columnTracks.forEachIndexed { index, track ->
                    SongRecommendRowItem(
                        track = track,
                        onClick = { onTrackClick(track) },
                        onPlayClick = { onTrackClick(track) }
                    )
                    if (index < columnTracks.lastIndex) {
                        HorizontalDivider(
                            color = HomeDivider,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 66.dp, end = 4.dp)
                        )
                    }
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
    val coverUrl = (track.coverUrl ?: track.album.coverUrl)?.takeIf { it.isNotBlank() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .squircleClip(8.dp)
                .background(AppTheme.colors.placeholder),
            contentAlignment = Alignment.Center
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl.toCoverThumbnailUrl(180),
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
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
                    fontSize = 13.sp,
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
                painter = painterResource(id = R.drawable.ic_mini_play),
                contentDescription = "播放",
                tint = HomeSecondary,
                modifier = Modifier.size(18.dp)
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

    val colors = AppTheme.colors
    val badgeBg = colors.tagContainer
    val textColor = colors.tagContent

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
                    painter = painterResource(R.drawable.ic_heart_filled),
                    contentDescription = null,
                    tint = textColor,
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
    newSongRadarTracks: List<Track>,
    newSongRadarPlaylist: RadarPlaylistData?,
    privateDjTracks: List<Track>,
    simiSongsTracks: List<Track>,
    likedCoverUrl: String?,
    privateDjCoverUrl: String?,
    onCardClick: (BigCardItem) -> Unit,
    onCardPlayClick: (BigCardItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultRecommendCover = favoriteTracks.firstOrNull()?.coverUrl
        ?: tracks.firstOrNull()?.coverUrl

    val djCover = privateDjCoverUrl?.takeIf { it.isNotBlank() }
        ?: privateDjTracks.firstOrNull()?.coverUrl
        ?: defaultRecommendCover

    val simiDisplayTracks = remember(simiSongsTracks, favoriteTracks, tracks) {
        when {
            simiSongsTracks.isNotEmpty() -> simiSongsTracks
            favoriteTracks.isNotEmpty() -> favoriteTracks
            else -> tracks
        }
    }
    val simiLeadTrack = simiDisplayTracks.firstOrNull()
    val simiCompanionText = remember(simiDisplayTracks) {
        val titles = simiDisplayTracks.map { it.title }.filter { it.isNotBlank() }.take(3)
        if (titles.isNotEmpty()) titles.joinToString("、") else "从你喜欢的歌听起"
    }

    val cards = remember(
        tracks,
        favoriteTracks,
        radarTracks,
        radarPlaylist,
        newSongRadarTracks,
        newSongRadarPlaylist,
        likedCoverUrl,
        djCover,
        simiDisplayTracks,
        simiLeadTrack,
        simiCompanionText
    ) {
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
                type = BigCardType.NEW_SONG_RADAR,
                title = "新歌雷达",
                subtitle = newSongRadarPlaylist?.description?.ifBlank { "发现最新发行的好歌" } ?: "发现最新发行的好歌",
                coverUrl = newSongRadarPlaylist?.coverUrl
                    ?: newSongRadarTracks.firstOrNull()?.coverUrl
                    ?: tracks.getOrNull(4)?.coverUrl
                    ?: tracks.getOrNull(2)?.coverUrl,
                defaultGradient = listOf(Color(0xFF1A2A6C), Color(0xFFB21F1F), Color(0xFFFDBB2D))
            ),
            BigCardItem(
                type = BigCardType.SIMI_SONGS,
                title = "相似歌曲",
                subtitle = simiCompanionText,
                coverUrl = simiLeadTrack?.coverUrl,
                previewTracks = simiDisplayTracks.take(3),
                defaultGradient = listOf(Color(0xFF9E8B83), Color(0xFF7A6861))
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
                    modifier = Modifier
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
    if (card.type == BigCardType.SIMI_SONGS) {
        SimiSongsRoamStyleCard(
            card = card,
            onClick = onClick,
            onPlayClick = onPlayClick,
            modifier = modifier
        )
        return
    }

    val context = LocalContext.current
    val coverUrl = card.coverUrl
    val fallbackExtendColor = remember(card.type, card.defaultGradient) {
        if (card.type == BigCardType.PRIVATE_DJ && coverUrl.isNullOrBlank()) {
            Color(0xFF7A7C80)
        } else {
            card.defaultGradient.lastOrNull() ?: Color(0xFF262C30)
        }
    }
    val cachedExtendColor = remember(coverUrl) { getCachedBottomEdgeColor(coverUrl) }
    var extendColor by remember(card.type, coverUrl, cachedExtendColor) {
        mutableStateOf(cachedExtendColor ?: fallbackExtendColor)
    }

    LaunchedEffect(coverUrl, cachedExtendColor) {
        if (cachedExtendColor != null || coverUrl.isNullOrBlank()) return@LaunchedEffect
        extractCachedBottomEdgeColor(context, coverUrl)?.let { extracted ->
            extendColor = extracted
        }
    }

    val animatedExtendColor by animateColorAsState(
        targetValue = extendColor,
        animationSpec = tween(durationMillis = 350),
        label = "bigCardExtendBg"
    )

    Box(
        modifier = modifier
            .width(160.dp)
            .height(215.dp)
            .squircleClip(18.dp)
            .drawBehind { drawRect(animatedExtendColor) }
            .clickable(onClick = onClick)
    ) {
        // 顶部 1:1 完整正方形封面，避免左右裁剪
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .align(Alignment.TopCenter)
        ) {
            if (!coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = coverUrl.toCoverThumbnailUrl(360),
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
        }

        // 从正方形封面下沿柔和过渡到底部取色延伸区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.54f to Color.Transparent,
                        0.66f to animatedExtendColor.copy(alpha = 0.70f),
                        0.74f to animatedExtendColor,
                        1.0f to animatedExtendColor
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
                    painter = painterResource(id = R.drawable.ic_mini_play),
                    contentDescription = "播放",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * 首页「相似歌曲」卡片：复用电台页「从你喜欢的歌曲开始漫游」的视觉样式
 * （封面主色提取背景 + 顶部歌名 + 中部三张错落叠放封面 + 底部深色半透明底栏与直接播放）
 */
@Composable
private fun SimiSongsRoamStyleCard(
    card: BigCardItem,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val leadTrack = card.previewTracks.firstOrNull()
    val firstCover = leadTrack?.coverUrl ?: card.coverUrl
    val secondCover = card.previewTracks.getOrNull(1)?.coverUrl ?: firstCover
    val thirdCover = card.previewTracks.getOrNull(2)?.coverUrl ?: secondCover ?: firstCover
    val seedId = leadTrack?.id ?: 0L
    val cachedColor = remember(firstCover) { getCachedDominantDarkColor(firstCover) }
    var dominantColor by remember(seedId, cachedColor) {
        mutableStateOf(cachedColor ?: deriveDarkHueFromId(seedId))
    }

    LaunchedEffect(firstCover, cachedColor) {
        if (cachedColor != null || firstCover.isNullOrBlank()) return@LaunchedEffect
        extractCachedDominantDarkColor(context, firstCover)?.let { extracted ->
            dominantColor = extracted
        }
    }

    val animatedBgState = animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 350),
        label = "simiSongsRoamBg"
    )

    val topSongTitle = leadTrack?.title?.takeIf { it.isNotBlank() } ?: "从你喜欢的歌听起"

    Box(
        modifier = modifier
            .width(160.dp)
            .height(215.dp)
            .squircleClip(18.dp)
            .drawBehind { drawRect(animatedBgState.value) }
            .squircleBorder(0.5.dp, AppTheme.colors.coverBorder, 18.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 上半部分：起点歌名 + 三张错落叠放专辑封面
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 6.dp)
            ) {
                Text(
                    text = topSongTitle,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                RoamStaggeredCoverStack(
                    firstCoverUrl = firstCover,
                    secondCoverUrl = secondCover,
                    thirdCoverUrl = thirdCover,
                    contentDescription = topSongTitle
                )
            }

            // 下半部分：半透明深色底栏（标题、关联歌名与播放按钮）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.28f))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            painter = painterResource(id = R.drawable.ic_mini_play),
                            contentDescription = "播放",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
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
