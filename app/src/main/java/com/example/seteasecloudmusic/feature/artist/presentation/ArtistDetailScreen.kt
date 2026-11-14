package com.example.seteasecloudmusic.feature.artist.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.core.common.toCoverThumbnailUrl
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.ui.adaptive.AdaptiveModalSheet
import com.example.seteasecloudmusic.core.ui.adaptive.AdaptiveTwoPane
import com.example.seteasecloudmusic.core.ui.adaptive.LocalAdaptiveWindow
import com.example.seteasecloudmusic.core.ui.adaptive.LocalContentBottomPadding
import com.example.seteasecloudmusic.core.ui.components.LiquidGlassCircleButton
import com.example.seteasecloudmusic.core.ui.components.ProgressiveTopBlur
import com.example.seteasecloudmusic.core.ui.components.verticalElasticOverscroll
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistAlbum
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistFavoritePlaylist
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistSummary
import com.example.seteasecloudmusic.feature.main.navigation.LocalNavigationScrollState
import com.example.seteasecloudmusic.feature.main.navigation.toNavigationViewport
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.mine.presentation.PlaylistDetailScreen
import com.example.seteasecloudmusic.feature.mine.presentation.TrackActionSheet
import com.example.seteasecloudmusic.feature.player.presentation.AppleMusicPlayTriangleIcon
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlin.math.roundToInt

private val ArtistPrimaryText = Color.White
private val ArtistSecondaryText = Color(0x9EFFFFFF)
private val ArtistCardSurface = Color(0x14FFFFFF)
private val ArtistCardBorder = Color(0x1AFFFFFF)
private val ArtistDividerColor = Color(0x14FFFFFF)
private val ArtistAccentRed = Color(0xFFFA233B)
private val ArtistHorizontalCardWidth = 156.dp

private data class ArtistNavTarget(
    val id: Long,
    val name: String,
    val coverUrl: String?
)

@Composable
fun ArtistDetailRoute(
    artistId: Long,
    artistName: String,
    artistCoverUrl: String?,
    onClose: () -> Unit,
    backHandlerEnabled: Boolean = true,
    viewModel: ArtistDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var artistStack by remember(artistId) {
        mutableStateOf(listOf(ArtistNavTarget(artistId, artistName, artistCoverUrl)))
    }
    val currentTarget = artistStack.last()
    val listState = rememberLazyListState()

    LaunchedEffect(currentTarget.id) {
        listState.scrollToItem(0)
        viewModel.loadArtist(
            artistId = currentTarget.id,
            fallbackName = currentTarget.name,
            fallbackCoverUrl = currentTarget.coverUrl
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onClosePlaylistDetail()
        }
    }

    val handleBack: () -> Unit = {
        if (artistStack.size > 1) {
            artistStack = artistStack.dropLast(1)
        } else {
            onClose()
        }
    }

    val effectiveUiState = if (uiState.artistId == currentTarget.id) {
        uiState
    } else {
        ArtistDetailUiState(
            artistId = currentTarget.id,
            artistName = currentTarget.name,
            artistCoverUrl = currentTarget.coverUrl,
            isLoading = true
        )
    }

    ArtistDetailScreen(
        uiState = effectiveUiState,
        onClose = handleBack,
        backHandlerEnabled = backHandlerEnabled,
        onPlayFirstSong = viewModel::onPlayFirstSongClick,
        onSongClick = viewModel::onSongClick,
        onMoreSongsClick = viewModel::onOpenHotSongsPlaylistClick,
        onMoreAlbumsClick = viewModel::onLoadAllAlbumsClick,
        onMoreSimilarArtistsClick = viewModel::onLoadAllSimilarArtistsClick,
        onAlbumClick = viewModel::onAlbumClick,
        onFavoritePlaylistClick = viewModel::onFavoritePlaylistClick,
        onClosePlaylistDetail = viewModel::onClosePlaylistDetail,
        onArtistClick = { artist ->
            if (artist.id > 0L && artist.id != currentTarget.id) {
                artistStack = artistStack + ArtistNavTarget(
                    id = artist.id,
                    name = artist.name,
                    coverUrl = artist.coverUrl
                )
            }
        },
        onPlayTrackList = viewModel::onPlayTrackList,
        listState = listState
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
    onMoreSimilarArtistsClick: () -> Unit,
    backHandlerEnabled: Boolean = true,
    onAlbumClick: (ArtistAlbum) -> Unit = {},
    onFavoritePlaylistClick: (ArtistFavoritePlaylist) -> Unit = {},
    onClosePlaylistDetail: () -> Unit = {},
    onArtistClick: (ArtistSummary) -> Unit = {},
    onPlayTrackList: (List<Track>, Int) -> Unit = { _, _ -> },
    listState: LazyListState = rememberLazyListState()
) {
    val hasActivePlaylist = uiState.activePlaylistDetail != null
    BackHandler(enabled = backHandlerEnabled && !hasActivePlaylist, onBack = onClose)
    ArtistLightStatusBarEffect()

    val window = LocalAdaptiveWindow.current
    val bottom = LocalContentBottomPadding.current
    val density = LocalDensity.current
    val navigationScroll = LocalNavigationScrollState.current
    DisposableEffect(navigationScroll, listState, hasActivePlaylist) {
        if (!hasActivePlaylist) {
            val viewport = listState.toNavigationViewport()
            navigationScroll?.attachDetailViewport(viewport)
            onDispose {
                navigationScroll?.detachDetailViewport(viewport)
            }
        } else {
            onDispose {}
        }
    }

    val ambientDarkColor by rememberArtistAmbientDarkColor(uiState.artistCoverUrl)
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val artistBackdrop = rememberLayerBackdrop {
        drawRect(ambientDarkColor)
        drawContent()
    }

    // 连续一体式艺人标题动效指标（1:1 复刻歌单详情页动效）
    var heroTitleOffsetFromTopPx by remember(uiState.artistId) { mutableFloatStateOf(0f) }
    var heroTitleLineCount by remember(uiState.artistId) { mutableIntStateOf(1) }
    val dockCenterY = remember(density, statusBarHeight) {
        with(density) { (statusBarHeight + 27.dp).toPx() }
    }
    val morphBandPx = remember(density) {
        with(density) { 54.dp.toPx() }
    }
    val heroTitleCenterY by remember(listState) {
        derivedStateOf {
            val offsetInHero = heroTitleOffsetFromTopPx
            if (offsetInHero <= 0f) {
                Float.POSITIVE_INFINITY
            } else if (listState.firstVisibleItemIndex == 0) {
                offsetInHero - listState.firstVisibleItemScrollOffset
            } else {
                Float.NEGATIVE_INFINITY
            }
        }
    }
    val topBlurAlpha by remember(listState, density) {
        val fadeDistancePx = with(density) { 44.dp.toPx().coerceAtLeast(1f) }
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / fadeDistancePx).coerceIn(0f, 1f)
        }
    }

    var showAllAlbumsSheet by remember(uiState.artistId) { mutableStateOf(false) }
    var selectedTrackForAction by remember { mutableStateOf<Track?>(null) }

    val lastPlaylistDetail = remember { mutableStateOf<PlaylistDetail?>(null) }
    val playlistOpenCounter = remember { mutableIntStateOf(0) }
    LaunchedEffect(uiState.activePlaylistDetail) {
        if (uiState.activePlaylistDetail != null) {
            lastPlaylistDetail.value = uiState.activePlaylistDetail
            playlistOpenCounter.intValue++
        }
    }
    val playlistDetailListState = rememberSaveable(
        uiState.activePlaylistDetail?.id,
        saver = LazyListState.Saver
    ) { LazyListState() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ambientDarkColor)
    ) {
        val split = (maxWidth >= 720.dp && maxHeight >= 360.dp) || window.isBook || window.isTabletop
        val works: @Composable () -> Unit = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalElasticOverscroll(),
                state = listState,
                contentPadding = PaddingValues(
                    top = if (split) statusBarHeight + 16.dp else 0.dp,
                    bottom = bottom + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (!split) {
                    item(key = "artist_hero") {
                        ArtistHeroSection(
                            artistName = uiState.artistName,
                            artistCoverUrl = uiState.artistCoverUrl,
                            ambientDarkColor = ambientDarkColor,
                            hasSongs = uiState.songs.isNotEmpty(),
                            onPlayHot50 = onPlayFirstSong,
                            titleAlphaProvider = {
                                val currentY = heroTitleCenterY
                                val startY = dockCenterY + morphBandPx
                                if (currentY > startY) {
                                    1f
                                } else if (heroTitleLineCount <= 1) {
                                    0f
                                } else {
                                    val p = ((startY - currentY) / morphBandPx).coerceIn(0f, 1f)
                                    (1f - p / 0.25f).coerceIn(0f, 1f)
                                }
                            },
                            onHeroTitleMetricsChanged = { centerOffsetPx ->
                                heroTitleOffsetFromTopPx = centerOffsetPx
                            },
                            onTitleLineCount = { count ->
                                heroTitleLineCount = count
                            }
                        )
                    }
                } else {
                    item(key = "artist_title") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = uiState.artistName.ifBlank { "未知艺人" },
                                color = ArtistPrimaryText,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "热门作品与音乐档案",
                                color = ArtistSecondaryText,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                if (uiState.isLoading && uiState.songs.isEmpty() && uiState.albums.isEmpty()) {
                    item(key = "loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                if (uiState.errorMessage != null && uiState.songs.isEmpty()) {
                    item(key = "error") {
                        Text(
                            text = uiState.errorMessage,
                            color = Color(0xFFFF8A80),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                // 1. 热门热曲
                item(key = "hot_songs") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ArtistSectionHeader(
                            title = "热门热曲",
                            showChevron = uiState.songs.isNotEmpty(),
                            isLoadingMore = uiState.isSongsLoadingMore,
                            onClick = onMoreSongsClick
                        )
                        HotSongsSection(
                            songs = uiState.songs.take(ArtistDetailViewModel.HOT_SONGS_PREVIEW_LIMIT),
                            publishYears = uiState.songPublishYears,
                            onSongClick = onSongClick,
                            onMoreClick = { selectedTrackForAction = it }
                        )
                    }
                }

                // 2. 专辑
                item(key = "albums") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ArtistSectionHeader(
                            title = "专辑",
                            showChevron = uiState.albums.isNotEmpty(),
                            isLoadingMore = uiState.isAlbumsLoadingMore,
                            onClick = {
                                onMoreAlbumsClick()
                                if (uiState.albums.isNotEmpty()) {
                                    showAllAlbumsSheet = true
                                }
                            }
                        )
                        AlbumSection(
                            albums = uiState.albums,
                            onAlbumClick = onAlbumClick
                        )
                    }
                }

                // 3. 艺人爱听
                item(key = "artist_favorites") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ArtistSectionHeader(
                            title = "艺人爱听",
                            showChevron = false,
                            isLoadingMore = false,
                            onClick = null
                        )
                        ArtistFavoritesSection(
                            playlists = uiState.favoritePlaylists,
                            isLoading = uiState.isLoading,
                            onPlaylistClick = onFavoritePlaylistClick
                        )
                    }
                }

                // 4. 艺人（参考图二）
                item(key = "similar_artists") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ArtistSectionHeader(
                            title = "艺人",
                            showChevron = !uiState.similarAllLoaded,
                            isLoadingMore = uiState.isSimilarLoadingMore,
                            onClick = if (!uiState.similarAllLoaded) onMoreSimilarArtistsClick else null
                        )
                        SimilarArtistsCardSection(
                            artists = uiState.similarArtists,
                            onArtistClick = onArtistClick
                        )
                    }
                }

                // 5. 百科（前10行展示，超出折叠）
                if (!split && uiState.description != null) {
                    item(key = "description") {
                        DescriptionSection(uiState)
                    }
                }
            }
        }

        if (split) {
            val primaryScrollState = rememberScrollState()
            val splitBlurThresholdPx = remember(density) { with(density) { 28.dp.toPx() }.coerceAtLeast(1f) }
            val splitBlurAlpha by remember(listState, primaryScrollState, splitBlurThresholdPx) {
                derivedStateOf {
                    val primaryProgress = (primaryScrollState.value.toFloat() / splitBlurThresholdPx).coerceIn(0f, 1f)
                    val secondaryProgress = if (listState.firstVisibleItemIndex > 0) 1f
                    else (listState.firstVisibleItemScrollOffset.toFloat() / splitBlurThresholdPx).coerceIn(0f, 1f)
                    maxOf(primaryProgress, secondaryProgress)
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .layerBackdrop(artistBackdrop)
            ) {
                AdaptiveTwoPane(
                    modifier = Modifier.fillMaxSize(),
                    primaryFraction = 0.38f,
                    primary = {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(primaryScrollState)
                                .padding(
                                    start = 20.dp,
                                    end = 20.dp,
                                    top = statusBarHeight + 16.dp,
                                    bottom = bottom + 20.dp
                                ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LiquidGlassCircleButton(
                                    onClick = onClose,
                                    backdrop = artistBackdrop,
                                    size = 38.dp,
                                    isDarkSurface = true,
                                    contentDescription = "返回"
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "艺人",
                                    color = ArtistPrimaryText,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(Modifier.clip(RoundedCornerShape(24.dp))) {
                                ArtistHeroSection(
                                    artistName = uiState.artistName,
                                    artistCoverUrl = uiState.artistCoverUrl,
                                    ambientDarkColor = ambientDarkColor,
                                    hasSongs = uiState.songs.isNotEmpty(),
                                    onPlayHot50 = onPlayFirstSong,
                                    compactHeight = true
                                )
                            }
                            if (uiState.description != null) {
                                DescriptionSection(uiState, horizontalPadding = 4.dp)
                            }
                        }
                    },
                    secondary = works
                )
            }
            if (splitBlurAlpha > 0.005f) {
                ProgressiveTopBlur(
                    backdrop = artistBackdrop,
                    totalHeight = statusBarHeight + 28.dp,
                    statusBarHeight = statusBarHeight,
                    alphaProvider = { splitBlurAlpha },
                    deepBlurRadius = 20.dp,
                    mediumBlurRadius = 10.dp,
                    shallowBlurRadius = 4.dp,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .layerBackdrop(artistBackdrop)
            ) {
                works()
            }

            // 顶部渐进高斯模糊 + 连续一体式艺人标题动效 + 左侧液态玻璃返回按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                ProgressiveTopBlur(
                    backdrop = artistBackdrop,
                    totalHeight = statusBarHeight + 24.dp,
                    statusBarHeight = statusBarHeight,
                    modifier = Modifier.align(Alignment.TopCenter),
                    alphaProvider = { 1f - topBlurAlpha },
                    deepBlurRadius = 16.dp,
                    mediumBlurRadius = 8.dp,
                    shallowBlurRadius = 3.dp
                )
                ProgressiveTopBlur(
                    backdrop = artistBackdrop,
                    totalHeight = statusBarHeight + 82.dp,
                    statusBarHeight = statusBarHeight,
                    modifier = Modifier.align(Alignment.TopCenter),
                    alphaProvider = { topBlurAlpha }
                )

                // 连续一体式艺人名：随手指上滑从 Hero 位置缩放并平稳停靠至顶栏中央
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.TopCenter)
                        .offset {
                            val currentY = heroTitleCenterY
                            val startY = dockCenterY + morphBandPx
                            val targetY = if (currentY <= startY) {
                                currentY.coerceAtLeast(dockCenterY)
                            } else {
                                startY
                            }
                            IntOffset(0, (targetY - 24.dp.toPx()).roundToInt())
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val scaleFactor = 18f / 32f
                    val dockedMaxTextWidth = ((maxWidth - 124.dp) / scaleFactor).coerceAtLeast(120.dp)
                    val layoutMaxWidth = minOf(maxWidth - 48.dp, dockedMaxTextWidth)
                    Text(
                        text = uiState.artistName.ifBlank { "未知艺人" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                        letterSpacing = (-0.5).sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .widthIn(max = layoutMaxWidth)
                            .graphicsLayer {
                                val currentY = heroTitleCenterY
                                val startY = dockCenterY + morphBandPx
                                val inMorph = currentY <= startY
                                val progress = if (inMorph) {
                                    ((startY - currentY) / morphBandPx).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                                val scale = lerp(1f, scaleFactor, progress)
                                scaleX = scale
                                scaleY = scale
                                transformOrigin = TransformOrigin.Center
                                alpha = if (!inMorph) {
                                    0f
                                } else if (heroTitleLineCount <= 1) {
                                    1f
                                } else {
                                    (progress / 0.25f).coerceIn(0f, 1f)
                                }
                            }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = statusBarHeight + 8.dp, start = 16.dp, end = 16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    LiquidGlassCircleButton(
                        onClick = onClose,
                        backdrop = artistBackdrop,
                        size = 38.dp,
                        isDarkSurface = true,
                        contentDescription = "返回",
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 热门歌曲 / 专辑 / 艺人爱听 统一以歌单详情页形式滑入打开
        AnimatedVisibility(
            visible = uiState.activePlaylistDetail != null,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = spring(dampingRatio = 0.92f, stiffness = 260f)
            ),
            exit = if (backHandlerEnabled) {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = spring(dampingRatio = 0.92f, stiffness = 300f)
                )
            } else {
                ExitTransition.None
            },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(30f)
        ) {
            val displayDetail = uiState.activePlaylistDetail ?: lastPlaylistDetail.value
            displayDetail?.let { detail ->
                key(playlistOpenCounter.intValue) {
                    PlaylistDetailScreen(
                        detail = detail,
                        backdrop = artistBackdrop,
                        onClose = onClosePlaylistDetail,
                        onPlayTrack = { track ->
                            val idx = detail.tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                            onPlayTrackList(detail.tracks, idx)
                        },
                        onPlayAll = {
                            onPlayTrackList(detail.tracks, 0)
                        },
                        onShufflePlay = {
                            onPlayTrackList(detail.tracks.shuffled(), 0)
                        },
                        isLoading = uiState.isPlaylistDetailLoading,
                        listState = playlistDetailListState,
                        filterByCurrentUserLikes = false
                    )
                }
            }
        }
    }

    if (showAllAlbumsSheet) {
        ArtistAllAlbumsSheet(
            artistName = uiState.artistName,
            albums = uiState.albums,
            hasMore = uiState.albumsHasMore,
            isLoadingMore = uiState.isAlbumsLoadingMore,
            containerColor = ambientDarkColor,
            onDismiss = { showAllAlbumsSheet = false },
            onLoadMore = onMoreAlbumsClick,
            onAlbumClick = { album ->
                showAllAlbumsSheet = false
                onAlbumClick(album)
            }
        )
    }

    val actionTrack = selectedTrackForAction
    if (actionTrack != null) {
        TrackActionSheet(
            track = actionTrack,
            onDismiss = { selectedTrackForAction = null }
        )
    }
}

@Composable
private fun ArtistLightStatusBarEffect() {
    val view = LocalView.current
    DisposableEffect(view) {
        fun findActivity(context: Context): Activity? = when (context) {
            is Activity -> context
            is ContextWrapper -> findActivity(context.baseContext)
            else -> null
        }
        val controller = findActivity(view.context)?.let { WindowCompat.getInsetsController(it.window, view) }
        val prevLightStatus = controller?.isAppearanceLightStatusBars ?: true
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = prevLightStatus
        }
    }
}

@Composable
private fun ArtistHeroSection(
    artistName: String,
    artistCoverUrl: String?,
    ambientDarkColor: Color,
    hasSongs: Boolean,
    onPlayHot50: () -> Unit,
    compactHeight: Boolean = false,
    titleAlphaProvider: () -> Float = { 1f },
    onHeroTitleMetricsChanged: ((centerOffsetFromTopPx: Float) -> Unit)? = null,
    onTitleLineCount: ((Int) -> Unit)? = null
) {
    val heroHeight = if (compactHeight) {
        320.dp
    } else {
        (LocalAdaptiveWindow.current.height * 0.50f).coerceIn(320.dp, 440.dp)
    }

    val coordsHolder = remember { arrayOfNulls<LayoutCoordinates>(2) }
    val reportTitleMetrics = {
        val root = coordsHolder[0]
        val title = coordsHolder[1]
        if (root != null && title != null && root.isAttached && title.isAttached) {
            val topY = root.localPositionOf(title, Offset.Zero).y
            onHeroTitleMetricsChanged?.invoke(topY + title.size.height / 2f)
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val playScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 650f),
        label = "artistPlayButtonScale"
    )
    val smoothBottomScrimBrush = remember(ambientDarkColor) {
        buildSmoothHeroScrimBrush(ambientDarkColor)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .clipToBounds()
            .background(ambientDarkColor)
            .let { base ->
                if (onHeroTitleMetricsChanged != null) {
                    base.onGloballyPositioned { coords ->
                        coordsHolder[0] = coords
                        reportTitleMetrics()
                    }
                } else {
                    base
                }
            }
    ) {
        AsyncImage(
            model = artistCoverUrl,
            contentDescription = artistName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 3.dp)
                .clipToBounds()
        )

        // 顶部轻暗角保护返回按钮与状态栏可读性
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.34f),
                        1.0f to Color.Transparent
                    )
                )
        )

        // 底部 S 曲线平滑垂直渐变，避免在文字和按钮处产生色阶断层，并在底边完全融入氛围底色
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = smoothBottomScrimBrush)
        )

        // 歌手名居中 + 下方仅保留一个白色圆形播放键（播放热门 50 首）
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = artistName.ifBlank { "未知艺人" },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = if (onTitleLineCount != null) {
                    { result -> onTitleLineCount(result.lineCount) }
                } else {
                    null
                },
                modifier = Modifier
                    .graphicsLayer { alpha = titleAlphaProvider() }
                    .let { base ->
                        if (onHeroTitleMetricsChanged != null) {
                            base.onGloballyPositioned { coords ->
                                coordsHolder[1] = coords
                                reportTitleMetrics()
                            }
                        } else {
                            base
                        }
                    }
            )

            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(62.dp)
                    .graphicsLayer {
                        scaleX = playScale
                        scaleY = playScale
                    }
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = hasSongs,
                        onClick = onPlayHot50
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AppleMusicPlayTriangleIcon(
                        tint = Color(0xFF111111),
                        size = 22.dp
                    )
                }
            }
        }
    }
}

private fun buildSmoothHeroScrimBrush(ambientColor: Color): Brush {
    val startPos = 0.34f
    val endPos = 0.975f
    val steps = 12
    val stops = Array(steps + 2) { index ->
        when (index) {
            0 -> 0.00f to Color.Transparent
            steps + 1 -> 1.00f to ambientColor
            else -> {
                val t = (index - 1).toFloat() / (steps - 1).toFloat()
                val eased = t * t * (3f - 2f * t)
                val pos = startPos + (endPos - startPos) * t
                pos to ambientColor.copy(alpha = eased)
            }
        }
    }
    return Brush.verticalGradient(colorStops = stops)
}

@Composable
private fun ArtistSectionHeader(
    title: String,
    showChevron: Boolean,
    isLoadingMore: Boolean,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = ArtistPrimaryText,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.3).sp,
            fontWeight = FontWeight.Bold
        )
        if (showChevron) {
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "查看更多",
                tint = ArtistSecondaryText,
                modifier = Modifier.size(22.dp)
            )
        }
        if (isLoadingMore) {
            Spacer(modifier = Modifier.width(8.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = Color.White,
                strokeWidth = 1.8.dp
            )
        }
    }
}

@Composable
private fun HotSongsSection(
    songs: List<Track>,
    publishYears: Map<Long, Int>,
    onSongClick: (Track) -> Unit,
    onMoreClick: (Track) -> Unit
) {
    if (songs.isEmpty()) {
        EmptySectionHint(text = "暂无热门歌曲")
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        songs.forEachIndexed { index, song ->
            ArtistTrackRow(
                track = song,
                publishYear = publishYears[song.id],
                onClick = { onSongClick(song) },
                onMoreClick = { onMoreClick(song) }
            )
            if (index < songs.lastIndex) {
                HorizontalDivider(
                    color = ArtistDividerColor,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 80.dp, end = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun ArtistTrackRow(
    track: Track,
    publishYear: Int?,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val subtitle = remember(track, publishYear) {
        val albumTitle = track.album.title.takeIf { it.isNotBlank() && it != "Unknown Album" }
        val artistText = track.artists.joinToString(" / ") { it.name }
        when {
            albumTitle != null && publishYear != null -> "$albumTitle · ${publishYear}年"
            albumTitle != null -> albumTitle
            artistText.isNotBlank() -> artistText
            else -> "未知专辑"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ArtistCardSurface)
        ) {
            AsyncImage(
                model = (track.coverUrl ?: track.album.coverUrl).toCoverThumbnailUrl(180),
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = track.title,
                color = ArtistPrimaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = ArtistSecondaryText,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "更多操作",
                tint = ArtistSecondaryText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AlbumSection(
    albums: List<ArtistAlbum>,
    onAlbumClick: (ArtistAlbum) -> Unit
) {
    if (albums.isEmpty()) {
        EmptySectionHint(text = "暂无专辑")
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(items = albums, key = { it.id }) { album ->
            AlbumCard(
                album = album,
                modifier = Modifier.width(ArtistHorizontalCardWidth),
                onClick = { onAlbumClick(album) }
            )
        }
    }
}

@Composable
private fun AlbumCard(
    album: ArtistAlbum,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(ArtistCardSurface)
                .border(0.5.dp, ArtistCardBorder, RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = album.coverUrl.toCoverThumbnailUrl(320),
                contentDescription = album.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.title,
            color = ArtistPrimaryText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.publishYear?.toString() ?: "未知年份",
            color = ArtistSecondaryText,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ArtistFavoritesSection(
    playlists: List<ArtistFavoritePlaylist>,
    isLoading: Boolean,
    onPlaylistClick: (ArtistFavoritePlaylist) -> Unit
) {
    if (playlists.isEmpty()) {
        if (!isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ArtistCardSurface)
                    .border(0.5.dp, ArtistCardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = ArtistSecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "该艺人暂未公开爱听歌单",
                    color = ArtistSecondaryText,
                    fontSize = 14.sp
                )
            }
        }
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(items = playlists, key = { it.id }) { playlist ->
            val displayTitle = if (playlist.isLikedPlaylist) "TA 喜欢的音乐" else playlist.name
            val subtitle = "${playlist.trackCount}首 · ${formatPlayCount(playlist.playCount)}播放"
            Column(
                modifier = Modifier
                    .width(ArtistHorizontalCardWidth)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onPlaylistClick(playlist) }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(ArtistHorizontalCardWidth)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ArtistCardSurface)
                        .border(0.5.dp, ArtistCardBorder, RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = playlist.coverUrl.toCoverThumbnailUrl(320),
                        contentDescription = displayTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (playlist.isLikedPlaylist) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.56f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = ArtistAccentRed,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "红心精选",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = displayTitle,
                    color = ArtistPrimaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = ArtistSecondaryText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 艺人推荐横向卡片列表（参考图二“更多艺人”卡片式结构：圆形头像 + 名字 + 副标 + 底部胶囊按钮，融入图一暗色玻璃质感）。
 */
@Composable
private fun SimilarArtistsCardSection(
    artists: List<ArtistSummary>,
    onArtistClick: (ArtistSummary) -> Unit
) {
    if (artists.isEmpty()) {
        EmptySectionHint(text = "暂无更多艺人")
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = artists, key = { it.id }) { artist ->
            Column(
                modifier = Modifier
                    .width(142.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ArtistCardSurface)
                    .border(0.5.dp, ArtistCardBorder, RoundedCornerShape(18.dp))
                    .clickable { onArtistClick(artist) }
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.75.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                ) {
                    AsyncImage(
                        model = artist.coverUrl.toCoverThumbnailUrl(200),
                        contentDescription = artist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = artist.name,
                    color = ArtistPrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = artist.subtitle?.takeIf { it.isNotBlank() } ?: "歌手",
                    color = ArtistSecondaryText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "进入主页",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 百科区块：默认仅展示前 10 行，超出部分折叠并支持点击展开/收起。
 */
@Composable
private fun DescriptionSection(
    uiState: ArtistDetailUiState,
    horizontalPadding: Dp = 20.dp
) {
    val description = uiState.description ?: return
    val wikiText = remember(description) {
        buildAnnotatedString {
            var hasPrevious = false
            if (description.brief.isNotBlank()) {
                withStyle(SpanStyle(color = Color.White.copy(alpha = 0.74f))) {
                    append(description.brief)
                }
                hasPrevious = true
            }
            description.sections.forEach { section ->
                if (section.content.isBlank() && section.title.isBlank()) return@forEach
                if (hasPrevious) {
                    append("\n\n")
                }
                if (section.title.isNotBlank()) {
                    withStyle(
                        SpanStyle(
                            color = ArtistPrimaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(section.title)
                    }
                    if (section.content.isNotBlank()) {
                        append("\n")
                    }
                }
                if (section.content.isNotBlank()) {
                    withStyle(SpanStyle(color = Color.White.copy(alpha = 0.72f))) {
                        append(section.content)
                    }
                }
                hasPrevious = true
            }
        }
    }

    if (wikiText.isEmpty()) return

    var expanded by remember(uiState.artistId) { mutableStateOf(false) }
    var canExpand by remember(uiState.artistId, wikiText) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(horizontal = horizontalPadding, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "百科",
            color = ArtistPrimaryText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
        )

        Text(
            text = wikiText,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 10,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (!expanded) {
                    canExpand = result.hasVisualOverflow
                }
            }
        )

        if (canExpand || expanded) {
            Text(
                text = if (expanded) "收起" else "展开全部",
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ArtistAllAlbumsSheet(
    artistName: String,
    albums: List<ArtistAlbum>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    containerColor: Color,
    onDismiss: () -> Unit,
    onLoadMore: () -> Unit,
    onAlbumClick: (ArtistAlbum) -> Unit
) {
    AdaptiveModalSheet(
        onDismissRequest = onDismiss,
        containerColor = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "全部专辑",
                        color = ArtistPrimaryText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = artistName,
                        color = ArtistSecondaryText,
                        fontSize = 13.sp
                    )
                }
                if (hasMore) {
                    Text(
                        text = if (isLoadingMore) "加载中..." else "加载更多",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ArtistCardSurface)
                            .clickable(enabled = !isLoadingMore, onClick = onLoadMore)
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }

            HorizontalDivider(color = ArtistDividerColor, thickness = 0.5.dp)

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(items = albums, key = { it.id }) { album ->
                    AlbumCard(
                        album = album,
                        onClick = { onAlbumClick(album) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySectionHint(text: String) {
    Text(
        text = text,
        color = ArtistSecondaryText,
        fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
    )
}

private fun formatPlayCount(count: Long): String {
    return when {
        count >= 100_000_000L -> String.format("%.1f亿", count / 100_000_000.0)
        count >= 10_000L -> String.format("%.1f万", count / 10_000.0)
        else -> count.toString()
    }
}
