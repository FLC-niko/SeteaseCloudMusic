package com.example.seteasecloudmusic.feature.mine.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.seteasecloudmusic.core.ui.adaptive.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.seteasecloudmusic.core.ui.adaptive.AdaptiveAlertDialog
import com.example.seteasecloudmusic.core.ui.components.AppLoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.seteasecloudmusic.R
import androidx.compose.ui.draw.clip
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.core.common.toCoverThumbnailUrl
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.ui.components.AmbientTopPinkBackground
import com.example.seteasecloudmusic.core.ui.components.AppPullToRefresh
import com.example.seteasecloudmusic.core.ui.components.AppleMusicMorphingTopBar
import com.example.seteasecloudmusic.core.ui.components.AppleMusicTopBarDefaults
import com.example.seteasecloudmusic.core.ui.components.GlassSurface
import com.example.seteasecloudmusic.core.ui.components.LiquidGlassCircleButton
import com.example.seteasecloudmusic.core.ui.components.LiquidGlassSegmentedBar
import com.example.seteasecloudmusic.core.ui.components.UserAvatar
import com.example.seteasecloudmusic.core.ui.components.rememberAppleMusicCollapseFraction
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import com.example.seteasecloudmusic.feature.mine.domain.model.UserPlaylist
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

private val MineTextPrimary: Color
    @Composable
    get() = AppTheme.colors.textPrimary
private val MineTextSecondary: Color
    @Composable
    get() = AppTheme.colors.textSecondary
private val MineDividerColor: Color
    @Composable
    get() = AppTheme.colors.divider
private val MineAccentRed: Color
    @Composable
    get() = AppTheme.colors.accent
private val MinePageBase: Color
    @Composable
    get() = AppTheme.colors.backgroundSecondary

/** “我的”页面主视图：展示账号信息、“我喜欢的音乐”入口、分类歌单列表及本地音乐管理，并在大屏下支持双栏详情联动。 */
@Composable
fun MineScreen(
    uiState: MineUiState,
    topContentPadding: Dp,
    bottomContentPadding: Dp,
    onLoginClick: () -> Unit,
    onRefresh: () -> Unit,
    onTabSelected: (MinePlaylistTab) -> Unit,
    onPlaylistClick: (UserPlaylist) -> Unit,
    onCloseDetail: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    onScanLocal: (String?) -> Unit,
    onSetLocalDirectory: (String) -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    isForeground: Boolean = true,
    onTrackLikedChanged: ((Track, Boolean) -> Unit)? = null,
    onTrackAddedToPlaylist: ((Long, Track) -> Unit)? = null,
    onTracksAddedToPlaylist: ((Long, List<Track>) -> Unit)? = null,
    onTracksReordered: ((Long, List<Track>) -> Unit)? = null,
    onDeleteTracks: ((Long, Set<Long>) -> Unit)? = null
) {
    val context = LocalContext.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var showDirectoryDialog by rememberSaveable { mutableStateOf(false) }

    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permissionToRequest) == PackageManager.PERMISSION_GRANTED
        )
    }

    val appIsDark = AppTheme.isDark
    val handlePlaylistClick: (UserPlaylist) -> Unit = { playlist ->
        trySyncExtractColor(context, playlist.coverUrl, isDark = appIsDark)
        onPlaylistClick(playlist)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) onScanLocal(uiState.localDirectoryPath)
    }

    LaunchedEffect(uiState.selectedTab) {
        if (uiState.selectedTab == MinePlaylistTab.LOCAL) {
            val granted = ContextCompat.checkSelfPermission(context, permissionToRequest) == PackageManager.PERMISSION_GRANTED
            hasPermission = granted
            if (!granted) {
                permissionLauncher.launch(permissionToRequest)
            } else if (uiState.localSongs.isEmpty()) {
                onScanLocal(uiState.localDirectoryPath)
            }
        }
    }

    val collapseFractionState = rememberAppleMusicCollapseFraction(
        lazyListState = lazyListState,
        collapseThresholdDp = AppleMusicTopBarDefaults.CollapseThreshold
    )

    // 不要在 AppNavigation 的全局 layerBackdrop 中再次采样同一个 Backdrop。
    // “我的”页面只采集自己的静态氛围背景，玻璃卡片从这张局部纹理取景，
    // 这样既保留液态玻璃效果，也避免渲染反馈导致点击页面后主线程/渲染线程卡死。
    val detailListState = rememberSaveable(uiState.activePlaylistDetail?.id, saver = LazyListState.Saver) { LazyListState() }
    val minePageBaseState = rememberUpdatedState(MinePageBase)
    val mineBackdrop = rememberLayerBackdrop {
        drawRect(minePageBaseState.value)
        drawContent()
    }
    val topBarBackdrop = rememberLayerBackdrop {
        drawRect(minePageBaseState.value)
        drawContent()
    }

    // 注意：不要用 `CompositionLocalProvider(LocalOverscrollFactory provides null)`
    // 关掉系统边缘拉伸。实测为负优化（50 分位帧耗时 13ms → 17ms）：关掉后本页的
    // verticalElasticOverscroll 才开始生效，而它用 graphicsLayer 平移整个 LazyColumn，
    // 代价高于系统拉伸。
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val window = LocalAdaptiveWindow.current
    val split = window.showDetailAlongside(uiState.activePlaylistDetail != null, maxWidth, maxHeight)
    val libraryContent: @Composable () -> Unit = {
    val session = uiState.authSession
    val isLoggedIn = session?.isLoggedIn == true && session.userId != null

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(topBarBackdrop)
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(mineBackdrop)
        ) {
            MineAmbientBackground()
        }

        if (!isLoggedIn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = statusBarHeight + 8.dp + AppleMusicTopBarDefaults.HeaderSpacerHeight,
                        bottom = bottomContentPadding
                    ),
                contentAlignment = Alignment.Center
            ) {
                MineLoggedOutView(
                    backdrop = mineBackdrop,
                    onLoginClick = onLoginClick
                )
            }
        } else {
            // 吸顶触发索引：0=顶栏占位, 1=用户卡片, 2=红心歌单卡片, 3=分类标签栏（到达索引 3 及以上时切换实色背景避免内容透底）
            val isTabsPinned by remember {
                derivedStateOf { lazyListState.firstVisibleItemIndex >= 3 }
            }
            val tabsBackgroundColor by animateColorAsState(
                targetValue = if (isTabsPinned) MinePageBase else Color.Transparent,
                animationSpec = tween(durationMillis = 150), // 标签栏吸顶背景色过渡时长（ms）
                label = "tabsBg"
            )

            AppPullToRefresh(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = statusBarHeight + 56.dp)
            ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = statusBarHeight + 8.dp,
                    bottom = bottomContentPadding
                )
            ) {
                item(key = "large_page_title") {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AppleMusicTopBarDefaults.HeaderSpacerHeight)
                    )
                }

            item(key = "user_header") {
                Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
                    UserProfileCenterpiece(
                        backdrop = mineBackdrop,
                        nickname = session.nickname ?: "云音乐用户",
                        userId = session.userId,
                        avatarUrl = session.avatarUrl,
                        createdCount = uiState.createdPlaylists.size,
                        favoritedCount = uiState.favoritedPlaylists.size,
                        likedCount = uiState.likedPlaylist?.trackCount ?: 0,
                        localCount = uiState.localSongs.size,
                        onRefresh = onRefresh,
                        onAccountClick = onLoginClick,
                        isLoading = uiState.isLoading
                    )
                }
            }

            item(key = "liked_hero") {
                Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp)) {
                    LikedSongsHeroCard(
                        backdrop = mineBackdrop,
                        playlist = uiState.likedPlaylist,
                        onClick = { uiState.likedPlaylist?.let(handlePlaylistClick) }
                    )
                }
            }

            // 使用 LazyColumn 原生 stickyHeader，与列表同帧布局以避免吸顶抖动
            stickyHeader(key = "playlist_tabs") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tabsBackgroundColor)
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    MinePlaylistTabs(
                        backdrop = mineBackdrop,
                        selectedTab = uiState.selectedTab,
                        createdCount = uiState.createdPlaylists.size,
                        favoritedCount = uiState.favoritedPlaylists.size,
                        localCount = uiState.localSongs.size,
                        onTabSelected = onTabSelected
                    )
                }
            }

            when (uiState.selectedTab) {
                MinePlaylistTab.CREATED -> {
                    if (uiState.createdPlaylists.isEmpty() && !uiState.isLoading) {
                        if (!uiState.errorMessage.isNullOrBlank()) {
                            item(key = "error_created") {
                                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                                    MineErrorNotice(
                                        message = uiState.errorMessage,
                                        onRetry = onRefresh
                                    )
                                }
                            }
                        } else {
                            item(key = "empty_created") {
                                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                                    EmptyPlaylistNotice("暂无自建歌单")
                                }
                            }
                        }
                    } else {
                        item(key = "created_header") {
                            SmallTitle(
                                text = "自建歌单 (${uiState.createdPlaylists.size})",
                                insideMargin = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }
                        item(key = "created_playlists_card") {
                            Card(
                                colors = CardDefaults.defaultColors(color = AppTheme.colors.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                uiState.createdPlaylists.forEachIndexed { index, playlist ->
                                    PlaylistRowItem(playlist = playlist, onClick = { handlePlaylistClick(playlist) })
                                    if (index < uiState.createdPlaylists.lastIndex) {
                                        HorizontalDivider(
                                            color = MineDividerColor,
                                            thickness = 0.5.dp,
                                            modifier = Modifier.padding(start = 86.dp, end = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                MinePlaylistTab.FAVORITED -> {
                    if (uiState.favoritedPlaylists.isEmpty() && !uiState.isLoading) {
                        if (!uiState.errorMessage.isNullOrBlank()) {
                            item(key = "error_favorited") {
                                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                                    MineErrorNotice(
                                        message = uiState.errorMessage,
                                        onRetry = onRefresh
                                    )
                                }
                            }
                        } else {
                            item(key = "empty_favorited") {
                                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                                    EmptyPlaylistNotice("暂无收藏歌单")
                                }
                            }
                        }
                    } else {
                        item(key = "favorited_header") {
                            SmallTitle(
                                text = "收藏歌单 (${uiState.favoritedPlaylists.size})",
                                insideMargin = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }
                        item(key = "favorited_playlists_card") {
                            Card(
                                colors = CardDefaults.defaultColors(color = AppTheme.colors.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                uiState.favoritedPlaylists.forEachIndexed { index, playlist ->
                                    PlaylistRowItem(playlist = playlist, onClick = { handlePlaylistClick(playlist) })
                                    if (index < uiState.favoritedPlaylists.lastIndex) {
                                        HorizontalDivider(
                                            color = MineDividerColor,
                                            thickness = 0.5.dp,
                                            modifier = Modifier.padding(start = 86.dp, end = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                MinePlaylistTab.LOCAL -> {
                    item(key = "local_header") {
                        SmallTitle(
                            text = "本地音乐",
                            insideMargin = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                    item(key = "local_playlist_entry") {
                        Card(
                            colors = CardDefaults.defaultColors(color = AppTheme.colors.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            PlaylistRowItem(
                                playlist = uiState.localPlaylist,
                                onClick = { handlePlaylistClick(uiState.localPlaylist) }
                            )
                        }
                    }
                    item(key = "local_dir_header") {
                        SmallTitle(
                            text = "扫描目录",
                            insideMargin = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                    item(key = "local_dir_control_bar") {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            LocalMusicDirectoryBar(
                                backdrop = mineBackdrop,
                                directoryPath = uiState.localDirectoryPath,
                                hasPermission = hasPermission,
                                isScanning = uiState.isScanningLocal,
                                onChangeDirectory = { showDirectoryDialog = true },
                                onRequestPermission = { permissionLauncher.launch(permissionToRequest) },
                                onRescan = {
                                    if (!hasPermission) permissionLauncher.launch(permissionToRequest)
                                    else onScanLocal(uiState.localDirectoryPath)
                                }
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                item(key = "loading_indicator") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppLoadingIndicator(
                            color = MineAccentRed,
                            strokeWidth = 2.5.dp,
                            size = 26.dp
                        )
                    }
                }
            }
        }
            }
        }
        }

        MineCollapsedTopBar(
            collapseFractionProvider = { if (isLoggedIn) collapseFractionState.value else 0f },
            statusBarHeight = statusBarHeight,
            backdrop = topBarBackdrop,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(4f)
        )

    if (showDirectoryDialog) {
        ChangeLocalDirectoryDialog(
            currentPath = uiState.localDirectoryPath ?: "",
            onDismiss = { showDirectoryDialog = false },
            onConfirm = { newPath ->
                showDirectoryDialog = false
                if (!hasPermission) permissionLauncher.launch(permissionToRequest)
                onSetLocalDirectory(newPath)
            }
        )
    }

    val lastPlaylistDetail = remember { mutableStateOf<PlaylistDetail?>(null) }
    val lastOpenedPlaylistId = remember { mutableStateOf<Long?>(null) }
    val playlistOpenCounter = remember { androidx.compose.runtime.mutableIntStateOf(0) }
    LaunchedEffect(uiState.activePlaylistDetail) {
        val current = uiState.activePlaylistDetail
        if (current != null) {
            if (lastOpenedPlaylistId.value != current.id) {
                playlistOpenCounter.intValue++
                lastOpenedPlaylistId.value = current.id
            }
            lastPlaylistDetail.value = current
        } else {
            lastOpenedPlaylistId.value = null
        }
    }
    LaunchedEffect(isForeground) {
        if (!isForeground) {
            lastPlaylistDetail.value = null
        }
    }

    if (!split) {
    AnimatedVisibility(
        visible = uiState.activePlaylistDetail != null,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = spring(dampingRatio = 0.92f, stiffness = 260f) // 详情页滑入动画：阻尼比 0.92f，刚度 260f
        ),
        exit = if (isForeground) {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = spring(dampingRatio = 0.92f, stiffness = 300f) // 详情页滑出动画：阻尼比 0.92f，刚度 300f（退场略快）
            )
        } else {
            ExitTransition.None
        },
        modifier = Modifier.fillMaxSize().zIndex(20f)
    ) {
        val displayDetail = uiState.activePlaylistDetail ?: lastPlaylistDetail.value
        displayDetail?.let { detail ->
            val canReorderTracks = detail.id != LOCAL_PLAYLIST_ID && (
                detail.id == uiState.likedPlaylist?.id ||
                    uiState.createdPlaylists.any { it.id == detail.id }
            )
            val defaultStyle = mineDetailDefaultStyle(detail, uiState)
            androidx.compose.runtime.key(playlistOpenCounter.intValue) {
                PlaylistDetailScreen(
                    detail = detail,
                    isLoading = uiState.isLoadingDetail,
                    listState = detailListState,
                    backdrop = mineBackdrop,
                    onClose = onCloseDetail,
                    onPlayTrack = { track -> onPlayTrack(track, detail.tracks) },
                    onPlayAll = { onPlayAll(detail.tracks) },
                    onShufflePlay = { onPlayAll(detail.tracks.shuffled()) },
                    onTrackLikedChanged = onTrackLikedChanged,
                    onTrackAddedToPlaylist = onTrackAddedToPlaylist,
                    onTracksAddedToPlaylist = onTracksAddedToPlaylist,
                    onTracksReordered = if (canReorderTracks && onTracksReordered != null) {
                        { reordered -> onTracksReordered(detail.id, reordered) }
                    } else {
                        null
                    },
                    onDeleteTracks = if (canReorderTracks && onDeleteTracks != null) {
                        { ids -> onDeleteTracks(detail.id, ids) }
                    } else {
                        null
                    },
                    defaultStyle = defaultStyle
                )
            }
        }
    }
    }    }
    }
    RetainedMusicFeed(!split) { libraryContent() }
    if (split) {
        uiState.activePlaylistDetail?.let { detail ->
            val canReorderTracks = detail.id != LOCAL_PLAYLIST_ID && (
                detail.id == uiState.likedPlaylist?.id ||
                    uiState.createdPlaylists.any { it.id == detail.id }
            )
            val defaultStyle = mineDetailDefaultStyle(detail, uiState)
            PlaylistDetailScreen(
                detail = detail,
                backdrop = mineBackdrop,
                onClose = onCloseDetail,
                onPlayTrack = { onPlayTrack(it, detail.tracks) },
                onPlayAll = { onPlayAll(detail.tracks) },
                onShufflePlay = { onPlayAll(detail.tracks.shuffled()) },
                isLoading = uiState.isLoadingDetail,
                listState = detailListState,
                onTrackLikedChanged = onTrackLikedChanged,
                onTrackAddedToPlaylist = onTrackAddedToPlaylist,
                onTracksAddedToPlaylist = onTracksAddedToPlaylist,
                onTracksReordered = if (canReorderTracks && onTracksReordered != null) {
                    { reordered -> onTracksReordered(detail.id, reordered) }
                } else {
                    null
                },
                onDeleteTracks = if (canReorderTracks && onDeleteTracks != null) {
                    { ids -> onDeleteTracks(detail.id, ids) }
                } else {
                    null
                },
                defaultStyle = defaultStyle
            )
        }
    }
    }

}

internal fun mineDetailDefaultStyle(
    detail: PlaylistDetail,
    uiState: MineUiState
): PlaylistDetailStyle = when {
    detail.id == LOCAL_PLAYLIST_ID ||
        detail.id == uiState.likedPlaylist?.id ||
        uiState.createdPlaylists.any { it.id == detail.id } -> PlaylistDetailStyle.POSTER_WALL
    uiState.favoritedPlaylists.any { it.id == detail.id } -> PlaylistDetailStyle.FULL_COVER
    else -> PlaylistDetailStyle.APPLE_MUSIC
}

/**
 * 「我的」页顶部折叠栏。
 *
 * 单独抽成一层的原因：`collapseFraction` 在滚动最开始的 76dp 内每帧都会变化，
 * 如果直接在 [MineScreen] 作用域里读取它的 `.value`，就会让整页（含 LazyColumn
 * 的整个内容 DSL）每帧重新组合一次。把读取动作放进这个子组件后，滚动时只有
 * 这个顶栏自己重组。
 */
@Composable
private fun MineCollapsedTopBar(
    collapseFractionProvider: () -> Float,
    statusBarHeight: Dp,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    // 顶部栏不渲染右上角头像，避免与下方用户信息卡片中的头像重复出现；点击下方资料卡片即可打开账户与设置页。
    AppleMusicMorphingTopBar(
        title = "我的",
        collapseFractionProvider = collapseFractionProvider,
        statusBarHeight = statusBarHeight,
        backdrop = backdrop,
        modifier = modifier
    )
}

@Composable
private fun MineAmbientBackground() {
    AmbientTopPinkBackground(
        baseColor = MinePageBase,
        includeSideGlows = true
    )
}

@Composable
private fun LocalMusicDirectoryBar(
    @Suppress("UNUSED_PARAMETER") backdrop: Backdrop,
    directoryPath: String?,
    hasPermission: Boolean,
    isScanning: Boolean,
    onChangeDirectory: () -> Unit,
    onRequestPermission: () -> Unit,
    onRescan: () -> Unit
) {
    Card(
        colors = CardDefaults.defaultColors(color = AppTheme.colors.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = MiuixIcons.Folder,
                    contentDescription = null,
                    tint = MineAccentRed,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = directoryPath?.ifBlank { "全部系统音频 / 默认目录" } ?: "全部系统音频 / 默认目录",
                    style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp, color = MineTextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!hasPermission) {
                    TextButton(
                        text = "授权权限",
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.textButtonColors(
                            color = Color.Transparent,
                            textColor = MineAccentRed
                        ),
                        insideMargin = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.height(28.dp)
                    )
                } else {
                    TextButton(
                        text = "指定目录",
                        onClick = onChangeDirectory,
                        colors = ButtonDefaults.textButtonColors(
                            color = Color.Transparent,
                            textColor = MineTextPrimary
                        ),
                        insideMargin = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        textStyle = TextStyle(fontSize = 12.sp),
                        modifier = Modifier.height(28.dp)
                    )
                    IconButton(
                        onClick = onRescan,
                        enabled = !isScanning,
                        modifier = Modifier.size(28.dp)
                    ) {
                        if (isScanning) {
                            AppLoadingIndicator(
                                color = MineAccentRed,
                                strokeWidth = 2.dp,
                                size = 14.dp
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_refresh),
                                contentDescription = "重新扫描",
                                tint = MineTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserProfileCenterpiece(
    backdrop: Backdrop,
    nickname: String,
    userId: Long,
    avatarUrl: String?,
    createdCount: Int,
    favoritedCount: Int,
    likedCount: Int,
    localCount: Int,
    onRefresh: () -> Unit,
    onAccountClick: () -> Unit,
    isLoading: Boolean
) {
    GlassSurface(
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onAccountClick),
        cornerRadius = 28.dp, // 用户信息卡片圆角
        surfaceAlpha = 0.30f, // 玻璃卡片底色透明度
        interactiveHighlight = true
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    avatarUrl = avatarUrl,
                    displayName = nickname,
                    size = 64.dp, // 用户头像尺寸
                    showBorder = false
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nickname,
                        style = MiuixTheme.textStyles.title2.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MineTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "UID $userId",
                        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp, color = MineTextSecondary)
                    )
                }
                GlassIconButton(
                    backdrop = backdrop,
                    painter = painterResource(id = R.drawable.ic_refresh),
                    contentDescription = "刷新歌单",
                    tint = if (isLoading) MineAccentRed else MineTextPrimary,
                    onClick = onRefresh,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        AppLoadingIndicator(
                            color = MineAccentRed,
                            strokeWidth = 2.dp,
                            size = 17.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            val statBgColor = Color.White.copy(alpha = if (AppTheme.isDark) 0.08f else 0.24f) // 统计栏胶囊背景透明度（深色/浅色区分）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp)) // 统计栏圆角
                    .background(statBgColor)
                    .padding(vertical = 12.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileStatItem("喜欢", "$likedCount")
                ProfileStatDivider()
                ProfileStatItem("创建", "$createdCount")
                ProfileStatDivider()
                ProfileStatItem("收藏", "$favoritedCount")
                ProfileStatDivider()
                ProfileStatItem("本地", "$localCount")
            }
        }
    }
}

@Composable
private fun GlassIconButton(
    backdrop: Backdrop,
    painter: Painter,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    LiquidGlassCircleButton(
        onClick = onClick,
        backdrop = backdrop,
        size = 38.dp, // 玻璃图标按钮直径
        enabled = enabled,
        contentDescription = contentDescription
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        overlay()
    }
}

@Composable
private fun ProfileStatDivider() {
    Box(
        modifier = Modifier
            .height(26.dp)
            .width(1.dp)
            .background(Color.White.copy(alpha = if (AppTheme.isDark) 0.14f else 0.42f))
    )
}

@Composable
private fun ProfileStatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MiuixTheme.textStyles.headline1.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MineTextPrimary
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 11.sp, color = MineTextSecondary)
        )
    }
}

@Composable
private fun MineLoggedOutView(
    backdrop: Backdrop,
    onLoginClick: () -> Unit,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlassSurface(
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 30.dp, // 未登录引导卡片圆角
            surfaceAlpha = 0.32f // 未登录卡片玻璃底色透明度
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp) // 未登录页耳机图标圆形底框尺寸
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (AppTheme.isDark) 0.10f else 0.32f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_headphone),
                        contentDescription = null,
                        tint = MineAccentRed,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "登录网易云音乐",
                    style = MiuixTheme.textStyles.title2.copy(
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = MineTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "同步红心歌单、自建歌单与收藏资产",
                    style = MiuixTheme.textStyles.body2.copy(color = MineTextSecondary)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth().height(48.dp), // 登录按钮高度
                    cornerRadius = 24.dp, // 登录按钮圆角（全胶囊）
                    colors = ButtonDefaults.buttonColorsPrimary(
                        color = MineAccentRed,
                        contentColor = Color.White
                    )
                ) {
                    Text("立即登录 / 扫码登录", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun LikedSongsHeroCard(
    backdrop: Backdrop,
    playlist: UserPlaylist?,
    onClick: () -> Unit
) {
    GlassSurface(
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        cornerRadius = 26.dp, // 红心歌单卡片圆角
        surfaceAlpha = 0.26f, // 红心歌单卡片玻璃底色透明度
        interactiveHighlight = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp) // 红心歌单封面尺寸
                    .squircleClip(18.dp) // 封面超椭圆圆角
                    .background(Color.White.copy(alpha = if (AppTheme.isDark) 0.10f else 0.32f)),
                contentAlignment = Alignment.Center
            ) {
                if (!playlist?.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = playlist.coverUrl.toCoverThumbnailUrl(180),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_heart_filled),
                        contentDescription = null,
                        tint = MineAccentRed,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "我喜欢的音乐",
                    style = MiuixTheme.textStyles.headline1.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MineTextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp) // 卡片右侧播放按钮直径
                    .clip(CircleShape)
                    .background(MineAccentRed.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mini_play),
                    contentDescription = "播放我喜欢的音乐",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun MinePlaylistTabs(
    backdrop: Backdrop,
    selectedTab: MinePlaylistTab,
    createdCount: Int = 0,
    favoritedCount: Int = 0,
    localCount: Int = 0,
    onTabSelected: (MinePlaylistTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = remember {
        listOf(
            MinePlaylistTab.CREATED to "创建",
            MinePlaylistTab.FAVORITED to "收藏",
            MinePlaylistTab.LOCAL to "本地"
        )
    }

    val currentOnTabSelected by rememberUpdatedState(onTabSelected)
    val currentSelectedTab by rememberUpdatedState(selectedTab)
    val selectedIndex = tabs.indexOfFirst { it.first == currentSelectedTab }.coerceAtLeast(0)

    LiquidGlassSegmentedBar(
        backdrop = backdrop,
        itemCount = tabs.size,
        selectedIndex = selectedIndex,
        onSelect = { settledIndex -> currentOnTabSelected(tabs[settledIndex].first) },
        modifier = modifier.fillMaxWidth(),
        barHeight = 50.dp, // 分类分段栏高度
        cornerRadius = 25.dp // 分类分段栏圆角（半高形成胶囊外框）
    ) { isInsideSlider ->
        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEach { (tab, title) ->
                val itemModifier = if (!isInsideSlider) {
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            currentOnTabSelected(tab)
                        }
                } else {
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                }
                Box(
                    modifier = itemModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MiuixTheme.textStyles.footnote1.copy(
                            fontSize = 13.sp,
                            fontWeight = if (isInsideSlider) FontWeight.Bold else FontWeight.Medium,
                            color = if (isInsideSlider) MineAccentRed else MineTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangeLocalDirectoryDialog(
    currentPath: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var inputPath by remember { mutableStateOf(currentPath) }
    val quickPaths = listOf(
        "/storage/emulated/0/Music",
        "/storage/emulated/0/Download",
        "/storage/emulated/0/netease/cloudmusic/Music",
        "/sdcard/Music"
    )

    AdaptiveAlertDialog(
        onDismissRequest = onDismiss,
        title = "设置音乐扫描目录",
        confirmText = "确定并扫描",
        confirmColor = MineAccentRed,
        onConfirm = { onConfirm(inputPath.trim()) },
        dismissText = "取消",
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "请输入存放音频文件的完整目录路径（支持递归扫描子目录）：",
                style = MiuixTheme.textStyles.footnote1.copy(color = MineTextSecondary)
            )
            @Suppress("DEPRECATION")
            TextField(
                value = inputPath,
                onValueChange = { inputPath = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = "/storage/emulated/0/Music",
                useLabelAsPlaceholder = true,
                textStyle = MiuixTheme.textStyles.body1.copy(color = MineTextPrimary),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = AppTheme.colors.surfaceVariant,
                    labelColor = MineTextSecondary,
                    borderColor = MineAccentRed
                ),
                cursorBrush = SolidColor(MineAccentRed)
            )
            Text(
                text = "常用快捷目录：",
                style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.SemiBold)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickPaths.take(3).forEach { path ->
                    Surface(
                        onClick = { inputPath = path },
                        shape = RoundedCornerShape(8.dp),
                        color = AppTheme.colors.surfaceVariant,
                        border = BorderStroke(0.5.dp, AppTheme.colors.divider)
                    ) {
                        Text(
                            text = path.substringAfterLast("/"),
                            fontSize = 11.sp,
                            color = MineTextPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistRowItem(
    playlist: UserPlaylist,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp) // 歌单列表项封面尺寸
                .squircleClip(10.dp) // 封面超椭圆圆角
                .background(Color.White.copy(alpha = if (AppTheme.isDark) 0.12f else 0.42f)),
            contentAlignment = Alignment.Center
        ) {
            if (!playlist.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = playlist.coverUrl.toCoverThumbnailUrl(140),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = MiuixIcons.Music,
                    contentDescription = null,
                    tint = MineAccentRed,
                    modifier = Modifier.size(27.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MiuixTheme.textStyles.body1.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MineTextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${playlist.trackCount} 首" + if (!playlist.creatorName.isNullOrBlank()) " · ${playlist.creatorName}" else "",
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp, color = MineTextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = MiuixIcons.ChevronForward,
            contentDescription = null,
            tint = AppTheme.colors.textTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EmptyPlaylistNotice(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MiuixTheme.textStyles.body2.copy(color = MineTextSecondary)
        )
    }
}

@Composable
private fun MineErrorNotice(
    message: String,
    onRetry: () -> Unit
) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_cloud_off),
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(44.dp)
        )
        Text(
            text = "加载歌单受阻",
            style = MiuixTheme.textStyles.headline2.copy(color = MineTextPrimary, fontWeight = FontWeight.SemiBold)
        )
        Text(
            text = message,
            style = MiuixTheme.textStyles.footnote1.copy(color = MineTextSecondary),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Text(
            text = "提示：请检查网络设置后重试",
            style = MiuixTheme.textStyles.footnote1.copy(color = colors.textSecondary, fontSize = 11.sp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColorsPrimary(color = colors.accent, contentColor = Color.White)
        ) {
            Icon(painter = painterResource(R.drawable.ic_refresh), contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("重新加载")
        }
    }
}
