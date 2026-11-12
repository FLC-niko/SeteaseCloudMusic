package com.example.seteasecloudmusic.feature.mine.presentation

import android.content.Context
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.core.common.toCoverThumbnailUrl
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.FavoriteTracksManager
import com.example.seteasecloudmusic.core.settings.PlayerSettingsManager
import com.example.seteasecloudmusic.core.ui.adaptive.AdaptiveTwoPane
import com.example.seteasecloudmusic.core.ui.adaptive.LocalAdaptiveWindow
import com.example.seteasecloudmusic.core.ui.adaptive.LocalContentBottomPadding
import com.example.seteasecloudmusic.core.ui.adaptive.MusicPaneHeader
import com.example.seteasecloudmusic.core.ui.adaptive.rememberDeviceCornerRadius
import com.example.seteasecloudmusic.core.ui.components.rememberAppleMusicCollapseFraction
import com.example.seteasecloudmusic.core.ui.components.verticalElasticOverscroll
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.player.presentation.AppleMusicMoreIcon
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val PlaylistPageBg = Color(0xFFF7F7FA)
private val PlaylistTextPrimary = Color(0xFF111111)
private val PlaylistTextSecondary = Color(0xFF8E8E93)
private val PlaylistDividerColor = Color(0xFFEAEAEE)
private val PlaylistAccentColor = Color(0xFFFA233B)

private const val PREFS_NAME = "app_settings_prefs"
private const val KEY_PLAYLIST_STYLE = "playlist_detail_style"
private const val KEY_PLAYLIST_COLOR_BACKGROUND = "playlist_color_background_enabled"

@Composable
fun PlaylistDetailScreen(
    detail: PlaylistDetail,
    backdrop: Backdrop,
    onClose: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
    onShufflePlay: (() -> Unit)? = null,
    isLoading: Boolean = false,
    embedded: Boolean = false,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    onTrackLikedChanged: ((Track, Boolean) -> Unit)? = null,
    onTrackAddedToPlaylist: ((Long, Track) -> Unit)? = null
) {
    val bottom = LocalContentBottomPadding.current
    val window = LocalAdaptiveWindow.current
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    val isLikedPlaylist = remember(detail.name, detail.id) {
        detail.name.contains("喜欢") || (detail.id != LOCAL_PLAYLIST_ID && detail.description?.contains("红心") == true)
    }

    val likedTrackIds by FavoriteTracksManager.likedTrackIds.collectAsState()

    val effectiveTracks = remember(detail.tracks, isLikedPlaylist, likedTrackIds) {
        if (isLikedPlaylist) {
            detail.tracks.filter { it.id in likedTrackIds }
        } else {
            detail.tracks
        }
    }

    var localTracks by remember(detail.id, effectiveTracks) { mutableStateOf(effectiveTracks) }

    LaunchedEffect(effectiveTracks) {
        localTracks = effectiveTracks
    }

    val currentDetail = remember(detail, localTracks) {
        detail.copy(
            tracks = localTracks,
            trackCount = if (isLikedPlaylist) localTracks.size else detail.trackCount.coerceAtLeast(localTracks.size)
        )
    }

    var currentStyle by remember {
        val saved = prefs.getString(KEY_PLAYLIST_STYLE, PlaylistDetailStyle.APPLE_MUSIC.id)
        mutableStateOf(PlaylistDetailStyle.values().firstOrNull { it.id == saved } ?: PlaylistDetailStyle.APPLE_MUSIC)
    }

    var showMoreSheet by remember { mutableStateOf(false) }
    var selectedTrackForAction by remember { mutableStateOf<Track?>(null) }

    val handleStyleChange: (PlaylistDetailStyle) -> Unit = { newStyle ->
        currentStyle = newStyle
        prefs.edit().putString(KEY_PLAYLIST_STYLE, newStyle.id).apply()
    }

    var useColorBackground by remember {
        val saved = prefs.getBoolean(KEY_PLAYLIST_COLOR_BACKGROUND, true)
        mutableStateOf(saved)
    }

    val handleToggleColorBackground: (Boolean) -> Unit = { enabled ->
        useColorBackground = enabled
        prefs.edit().putBoolean(KEY_PLAYLIST_COLOR_BACKGROUND, enabled).apply()
    }

    val dominantDarkColor by rememberCoverDominantDarkColor(detail.coverUrl)
    val isDark = useColorBackground
    val solidBackgroundColor = if (isDark) dominantDarkColor.copy(alpha = 1f) else Color.White

    val handleShuffle: () -> Unit = {
        if (onShufflePlay != null) {
            onShufflePlay()
        } else {
            val shuffled = detail.tracks.shuffled()
            if (shuffled.isNotEmpty()) {
                onPlayTrack(shuffled.first())
            }
        }
    }

    val settingsManager = remember(context) { PlayerSettingsManager.getInstance(context) }
    val isPredictiveBackEnabled by settingsManager.predictiveBackEnabled.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val predictiveTranslationX = remember(detail.id) { Animatable(0f) }

    LaunchedEffect(detail.id) {
        predictiveTranslationX.snapTo(0f)
    }

    val deviceCorners = rememberDeviceCornerRadius()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val split = !embedded && ((maxWidth >= 720.dp && maxHeight >= 360.dp) || window.isBook || window.isTabletop)
        val expandedDark = solidBackgroundColor.luminance() < 0.4f
        val paneDark = if (split) expandedDark else isDark
        val isCardMode = !embedded && !split
        val fullWidthPx = with(density) { maxWidth.toPx() }
        val isPredictiveGestureActive = predictiveTranslationX.value > 0.5f

        if (isCardMode && isPredictiveBackEnabled) {
            PredictiveBackHandler(enabled = true) { progressFlow ->
                try {
                    progressFlow.collect { backEvent ->
                        val targetTranslationX = com.example.seteasecloudmusic.core.ui.adaptive.PredictiveBackMotion.calculateTranslationX(
                            progress = backEvent.progress,
                            fullWidthPx = fullWidthPx
                        )
                        predictiveTranslationX.snapTo(targetTranslationX)
                    }
                    onClose()
                } catch (e: CancellationException) {
                    coroutineScope.launch {
                        predictiveTranslationX.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = 0.86f,
                                stiffness = 400f
                            )
                        )
                    }
                }
            }
        } else {
            BackHandler(onBack = onClose)
        }

        val cornerShape = remember(deviceCorners, isCardMode, isPredictiveGestureActive) {
            if (isCardMode) {
                if (deviceCorners.hasRoundedCorners) {
                    deviceCorners.toShape()
                } else if (isPredictiveGestureActive) {
                    RoundedCornerShape(24.dp)
                } else {
                    RoundedCornerShape(0.dp)
                }
            } else RoundedCornerShape(0.dp)
        }

        // 100% 绝对不透明的纯色底座，结合真机硬件 r 角裁剪与向右滑入/返回柔和投影
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag(if (split) "expanded_playlist_surface" else "playlist_detail_surface")
                .graphicsLayer {
                    translationX = predictiveTranslationX.value
                }
                .then(
                    if (isCardMode && (deviceCorners.hasRoundedCorners || isPredictiveGestureActive)) {
                        Modifier
                            .shadow(
                                elevation = if (isPredictiveGestureActive) 24.dp else 20.dp,
                                shape = cornerShape,
                                clip = false
                            )
                            .clip(cornerShape)
                    } else Modifier
                ),
            shape = cornerShape,
            color = solidBackgroundColor
        ) {
            if (embedded || split) {
                val songs: @Composable () -> Unit = {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(top = 12.dp, bottom = bottom + 16.dp)
                    ) {
                        item(key = "detail_title") {
                            if (split && window.isTabletop) {
                                PlaylistHeroAppleMusicSection(currentDetail, onPlayAll, isDark = paneDark, showArtwork = false)
                            }
                            if (embedded) {
                                MusicPaneHeader(currentDetail.displayTitle,
                                    "${currentDetail.trackCount.coerceAtLeast(currentDetail.tracks.size)} 首歌曲", onClose,
                                    onMore = { showMoreSheet = true })
                            } else {
                                Text("${currentDetail.trackCount.coerceAtLeast(currentDetail.tracks.size)} 首歌曲",
                                    color = if (expandedDark) Color.White.copy(alpha = 0.65f) else PlaylistTextSecondary,
                                    fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                            }
                        }
                        if (embedded) {
                            item(key = "playlist_hero") {
                                Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                                    RenderPlaylistHero(
                                        style = currentStyle,
                                        detail = currentDetail,
                                        dominantDarkColor = solidBackgroundColor,
                                        onPlayAll = onPlayAll,
                                        isDark = isDark
                                    )
                                }
                            }
                        }
                        if (isLoading && currentDetail.tracks.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = if (paneDark) Color.White else PlaylistAccentColor)
                                }
                            }
                        }
                        itemsIndexed(currentDetail.tracks, key = { _, track -> track.id }) { index, track ->
                            Box(Modifier.padding(horizontal = 12.dp)) {
                                PlaylistDetailTrackRow(
                                    index = index + 1,
                                    track = track,
                                    isDark = paneDark,
                                    onClick = { onPlayTrack(track) },
                                    onMoreClick = { selectedTrackForAction = track }
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 88.dp, end = 20.dp),
                                color = if (paneDark) Color.White.copy(alpha = 0.08f) else PlaylistDividerColor
                            )
                        }
                    }
                }
                Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    if (split) {
                        AdaptiveTwoPane(
                            Modifier.fillMaxSize().testTag("expanded_playlist_panes"),
                            primaryFraction = 0.44f,
                            primary = {
                                Column(Modifier.fillMaxSize()) {
                                    ExpandedPlaylistToolbar(expandedDark, onClose) { showMoreSheet = true }
                                    if (window.isTabletop) {
                                        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            val mediaHeight = maxHeight
                                            val cap = when (currentStyle) {
                                                PlaylistDetailStyle.APPLE_MUSIC -> ((maxHeight - 22.dp) / 0.88f).coerceAtLeast(96.dp)
                                                PlaylistDetailStyle.POSTER_WALL -> (maxHeight - 8.dp).coerceAtLeast(96.dp)
                                                PlaylistDetailStyle.DYNAMIC_POSTER_WALL -> 560.dp
                                            }.coerceAtMost(560.dp)
                                            Box(Modifier.widthIn(max = cap).fillMaxWidth()) {
                                                RenderPlaylistHero(currentStyle, detail, solidBackgroundColor, onPlayAll,
                                                    isDark = expandedDark, expanded = true, mediaOnly = true, artworkHeight = mediaHeight)
                                            }
                                        }
                                    } else Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                                        .padding(start = 20.dp, end = 20.dp, bottom = bottom + 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                                            RenderPlaylistHero(currentStyle, detail, solidBackgroundColor,
                                                onPlayAll, isDark = expandedDark, expanded = true)
                                        }
                                    }
                                }
                            },
                            secondary = songs
                        )
                    } else {
                        songs()
                    }
                }
            } else {
                CompactPlaylistDetailScreen(
                    detail = currentDetail,
                    currentStyle = currentStyle,
                    dominantDarkColor = solidBackgroundColor,
                    isDark = isDark,
                    backdrop = backdrop,
                    onClose = onClose,
                    onPlayTrack = onPlayTrack,
                    onPlayAll = onPlayAll,
                    onShufflePlay = handleShuffle,
                    onOpenMoreMenu = { showMoreSheet = true },
                    onTrackMoreClick = { selectedTrackForAction = it },
                    isLoading = isLoading,
                    lazyListState = listState
                )
            }

            // 歌曲更多操作二级菜单底栏（竖置三点点击后弹出的抽屉）
            selectedTrackForAction?.let { track ->
                TrackActionSheet(
                    track = track,
                    currentPlaylistId = currentDetail.id,
                    onDismiss = { selectedTrackForAction = null },
                    onToggleLiked = { t, willBeLiked ->
                        onTrackLikedChanged?.invoke(t, willBeLiked)
                    },
                    onTrackAddedToPlaylist = { playlistId, t ->
                        onTrackAddedToPlaylist?.invoke(playlistId, t)
                    }
                )
            }

            // 更多操作与歌单壁纸二级菜单底栏（从底部冒出贴底展示）
            if (showMoreSheet) {
                PlaylistMoreActionSheet(
                    currentStyle = currentStyle,
                    useColorBackground = useColorBackground,
                    onToggleColorBackground = handleToggleColorBackground,
                    onSelectStyle = handleStyleChange,
                    onDismiss = { showMoreSheet = false }
                )
            }
        }
    }
}

/** 侧栏与详情共用取色背景；偏好变化只在实际切换时通知导航层。 */
@Composable
internal fun rememberPlaylistColorBackgroundEnabled(): Boolean {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var enabled by remember(prefs) { mutableStateOf(prefs.getBoolean(KEY_PLAYLIST_COLOR_BACKGROUND, true)) }
    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_PLAYLIST_COLOR_BACKGROUND) enabled = prefs.getBoolean(key, true)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return enabled
}

@Composable
private fun ExpandedPlaylistToolbar(isDark: Boolean, onClose: () -> Unit, onMore: () -> Unit) {
    val sideControls = LocalAdaptiveWindow.current.useNavigationRail && !LocalAdaptiveWindow.current.sideControlsAtStart
    val tint = if (isDark) Color.White else PlaylistTextPrimary
    val button: @Composable (Boolean, () -> Unit) -> Unit = { back, action ->
        IconButton(action, Modifier.size(44.dp)) {
            Box(Modifier.size(38.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = if (isDark) 0.16f else 0.70f))
                .border(0.5.dp, Color.White.copy(alpha = if (isDark) 0.18f else 0.85f), CircleShape),
                contentAlignment = Alignment.Center) {
                if (back) Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", Modifier.size(20.dp), tint = tint)
                else Box(Modifier.semantics { contentDescription = "更多操作" }) { AppleMusicMoreIcon(tint = tint, size = 20.dp) }
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!sideControls) button(true, onClose)
        Spacer(Modifier.weight(1f))
        button(false, onMore)
        if (sideControls) { Spacer(Modifier.width(8.dp)); button(true, onClose) }
    }
}

@Composable
private fun CompactPlaylistDetailScreen(
    detail: PlaylistDetail,
    currentStyle: PlaylistDetailStyle,
    dominantDarkColor: Color,
    isDark: Boolean,
    backdrop: Backdrop,
    onClose: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    onOpenMoreMenu: () -> Unit,
    onTrackMoreClick: (Track) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    lazyListState: androidx.compose.foundation.lazy.LazyListState
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val collapseThreshold = if (currentStyle == PlaylistDetailStyle.APPLE_MUSIC) 260.dp else 160.dp

    val collapseFractionState = rememberAppleMusicCollapseFraction(
        lazyListState = lazyListState,
        collapseThresholdDp = collapseThreshold
    )

    val isPosterWall = currentStyle == PlaylistDetailStyle.POSTER_WALL || currentStyle == PlaylistDetailStyle.DYNAMIC_POSTER_WALL

    // 整页统一纯色沉浸背景，没有任何黑色断层和过渡色阶，彻底消除透光
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) dominantDarkColor else Color.White)
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .verticalElasticOverscroll(),
            contentPadding = PaddingValues(
                top = if (isPosterWall) 0.dp else statusBarHeight + 12.dp,
                bottom = LocalContentBottomPadding.current + 16.dp,
                start = if (isPosterWall) 0.dp else 16.dp,
                end = if (isPosterWall) 0.dp else 16.dp
            )
        ) {
            // 1. 根据当前选择的风格渲染 Hero 头部（大封面墙 / 静态海报墙）
            item(key = "playlist_hero") {
                RenderPlaylistHero(
                    style = currentStyle,
                    detail = detail,
                    dominantDarkColor = dominantDarkColor,
                    onPlayAll = onPlayAll,
                    topInset = if (isPosterWall) statusBarHeight else 0.dp,
                    isDark = isDark
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 3. 曲目列表
            if (detail.tracks.isEmpty() && isLoading) {
                item(key = "loading_tracks") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = if (isDark) Color.White else PlaylistAccentColor,
                            strokeWidth = 2.5.dp
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = detail.tracks,
                    key = { _, track -> track.id }
                ) { index, track ->
                    Box(
                        modifier = if (isPosterWall) Modifier.padding(horizontal = 16.dp) else Modifier
                    ) {
                        PlaylistDetailTrackRow(
                            index = index + 1,
                            track = track,
                            isDark = isDark,
                            onClick = { onPlayTrack(track) },
                            onMoreClick = { onTrackMoreClick(track) }
                        )
                    }
                    if (index < detail.tracks.lastIndex) {
                        HorizontalDivider(
                            color = if (isDark) Color.White.copy(alpha = 0.08f) else PlaylistDividerColor,
                            modifier = Modifier.padding(
                                start = if (isPosterWall) 92.dp else 76.dp,
                                end = if (isPosterWall) 16.dp else 0.dp
                            )
                        )
                    }
                }
            }
        }

        val collapseFraction = collapseFractionState.value

        // 1. 折叠背景层（随下滑平滑淡入顶栏底色）
        if (collapseFraction > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBarHeight + 54.dp)
                    .align(Alignment.TopCenter)
                    .graphicsLayer { alpha = collapseFraction }
                    .background(
                        if (isDark) dominantDarkColor else Color.White.copy(alpha = 0.94f)
                    )
            )
        }

        // 2. 居中标题（随下滑平滑淡入，严格在左右按钮内侧居中）
        if (collapseFraction > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBarHeight + 54.dp)
                    .align(Alignment.TopCenter)
                    .padding(top = statusBarHeight, start = 62.dp, end = 62.dp)
                    .graphicsLayer { alpha = collapseFraction },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = detail.displayTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else PlaylistTextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 3. 左右两个常驻操作按钮（按未下滑的位置、大小与边框严格统一，下滑过程中纹丝不动）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarHeight + 8.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter)
        ) {
            // 左上角返回圆钮（位置与尺寸固定为 38.dp，图标固定为 20.dp）
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.70f)
                    )
                    .border(
                        0.5.dp,
                        if (isDark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.85f),
                        CircleShape
                    )
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = if (isDark) Color.White else Color(0xFF111111),
                    modifier = Modifier.size(20.dp)
                )
            }

            // 右上角三个点更多圆钮（位置与尺寸固定为 38.dp，图标固定为 20.dp）
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.70f)
                    )
                    .border(
                        0.5.dp,
                        if (isDark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.85f),
                        CircleShape
                    )
                    .semantics { contentDescription = "更多操作" }
                    .clickable(onClick = onOpenMoreMenu),
                contentAlignment = Alignment.Center
            ) {
                AppleMusicMoreIcon(
                    tint = if (isDark) Color.White else Color(0xFF111111),
                    size = 20.dp
                )
            }
        }
    }
}

/**
 * 根据风格渲染对应的 Hero 区域
 */
@Composable
private fun RenderPlaylistHero(
    style: PlaylistDetailStyle,
    detail: PlaylistDetail,
    dominantDarkColor: Color,
    onPlayAll: () -> Unit,
    topInset: androidx.compose.ui.unit.Dp = 0.dp,
    isDark: Boolean = true,
    expanded: Boolean = false,
    mediaOnly: Boolean = false,
    artworkHeight: androidx.compose.ui.unit.Dp? = null
) {
    Box(Modifier.fillMaxWidth().testTag("playlist_hero").semantics { stateDescription = style.title }) {
        when (style) {
            PlaylistDetailStyle.APPLE_MUSIC -> {
                PlaylistHeroAppleMusicSection(
                    detail = detail,
                    isDark = isDark,
                    onPlayAll = onPlayAll,
                    coverWidthFraction = if (expanded) 0.88f else 0.68f,
                    showControls = !mediaOnly
                )
            }
            PlaylistDetailStyle.POSTER_WALL -> {
                PlaylistHeroPosterWallSection(
                    detail = detail,
                    dominantDarkColor = dominantDarkColor,
                    isDark = isDark,
                    onPlayAll = onPlayAll,
                    topInset = topInset,
                    showControls = !mediaOnly
                )
            }
            PlaylistDetailStyle.DYNAMIC_POSTER_WALL -> {
                PlaylistHeroDynamicPosterWallSection(
                    detail = detail,
                    dominantDarkColor = dominantDarkColor,
                    isDark = isDark,
                    onPlayAll = onPlayAll,
                    topInset = topInset,
                    showControls = !mediaOnly,
                    artworkHeight = artworkHeight
                )
            }
        }
    }
}

/**
 * 歌单曲目单行（支持深浅双色，保留原有结构）
 */
@Composable
private fun PlaylistDetailTrackRow(
    index: Int,
    track: Track,
    isDark: Boolean = false,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 序号
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isDark) Color.White.copy(alpha = 0.45f) else PlaylistTextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            ),
            modifier = Modifier.width(28.dp)
        )

        // 2. 封面缩略方块
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFF0F0F3)),
            contentAlignment = Alignment.Center
        ) {
            if (!track.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = track.coverUrl.toCoverThumbnailUrl(140),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = if (isDark) Color.White.copy(alpha = 0.8f) else PlaylistAccentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // 3. 歌曲标题与歌手专辑
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = if (isDark) Color.White else PlaylistTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            val artistText = track.artists.joinToString(" / ") { it.name }.ifBlank { "未知歌手" }
            val albumText = track.album.title
            val subtitle = if (albumText.isNotBlank() && albumText != "本地音乐") {
                "$artistText · $albumText"
            } else {
                artistText
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isDark) Color.White.copy(alpha = 0.60f) else PlaylistTextSecondary,
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 4. 右侧竖置三个点（更多操作）
        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "更多操作",
                tint = if (isDark) Color.White.copy(alpha = 0.60f) else PlaylistTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
