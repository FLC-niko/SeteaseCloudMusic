package com.example.seteasecloudmusic.feature.mine.presentation

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.R
import com.example.seteasecloudmusic.core.common.toCoverThumbnailUrl
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.FavoriteTracksManager
import com.example.seteasecloudmusic.core.settings.PlayerSettingsManager
import com.example.seteasecloudmusic.core.ui.adaptive.AdaptiveTwoPane
import com.example.seteasecloudmusic.core.ui.adaptive.LocalAdaptiveWindow
import com.example.seteasecloudmusic.core.ui.adaptive.LocalContentBottomPadding
import com.example.seteasecloudmusic.core.ui.adaptive.MusicPaneHeader
import com.example.seteasecloudmusic.core.ui.adaptive.rememberDeviceCornerRadius
import com.example.seteasecloudmusic.core.ui.components.LiquidGlassCircleButton
import com.example.seteasecloudmusic.core.ui.components.ProgressiveTopBlur
import com.example.seteasecloudmusic.core.ui.components.verticalElasticOverscroll
import com.example.seteasecloudmusic.feature.main.PlayerEntryPoints
import com.example.seteasecloudmusic.feature.main.navigation.LocalNavigationScrollState
import com.example.seteasecloudmusic.feature.main.navigation.toNavigationViewport
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.player.presentation.AppleMusicMoreIcon
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
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
    onTrackAddedToPlaylist: ((Long, Track) -> Unit)? = null,
    onTracksAddedToPlaylist: ((Long, List<Track>) -> Unit)? = null,
    onTracksReordered: ((List<Track>) -> Unit)? = null,
    onDeleteTracks: ((Set<Long>) -> Unit)? = null
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
        if (isLikedPlaylist && likedTrackIds.isNotEmpty()) {
            detail.tracks.filter { it.id in likedTrackIds }
        } else {
            detail.tracks
        }
    }

    var localTracks by remember(detail.id) { mutableStateOf(effectiveTracks) }

    var isEditMode by remember { mutableStateOf(false) }
    val canReorder = onTracksReordered != null && detail.id != LOCAL_PLAYLIST_ID
    val canDelete = canReorder && onDeleteTracks != null
    var selectedTrackIds by remember { mutableStateOf(emptySet<Long>()) }
    var showBatchAddToPlaylistSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(effectiveTracks) {
        if (!isEditMode) {
            localTracks = effectiveTracks
        }
    }

    DisposableEffect(isEditMode) {
        PlayerEntryPoints.setBottomChromeHidden(isEditMode)
        onDispose {
            PlayerEntryPoints.setBottomChromeHidden(false)
        }
    }

    val navigationScroll = LocalNavigationScrollState.current
    DisposableEffect(navigationScroll, listState) {
        val viewport = listState.toNavigationViewport()
        navigationScroll?.attachDetailViewport(viewport)
        onDispose {
            navigationScroll?.detachDetailViewport(viewport)
        }
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

    val commitReorderIfNeeded: () -> Unit = {
        if (localTracks.map { it.id } != effectiveTracks.map { it.id }) {
            onTracksReordered?.invoke(localTracks)
        }
    }

    val handlePlayNextSelected: () -> Unit = {
        if (selectedTrackIds.isEmpty()) {
            Toast.makeText(context, "请先选择歌曲", Toast.LENGTH_SHORT).show()
        } else {
            val selected = localTracks.filter { it.id in selectedTrackIds }
            PlayerEntryPoints.playerController().insertNextBatch(selected)
            Toast.makeText(context, "已添加 ${selected.size} 首歌曲到下一首播放", Toast.LENGTH_SHORT).show()
            commitReorderIfNeeded()
            isEditMode = false
            selectedTrackIds = emptySet()
        }
    }

    val handleAddToPlaylistSelected: () -> Unit = {
        if (selectedTrackIds.isEmpty()) {
            Toast.makeText(context, "请先选择歌曲", Toast.LENGTH_SHORT).show()
        } else {
            showBatchAddToPlaylistSheet = true
        }
    }

    val handleDownloadSelected: () -> Unit = {
        if (selectedTrackIds.isEmpty()) {
            Toast.makeText(context, "请先选择歌曲", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "已将 ${selectedTrackIds.size} 首歌曲加入下载队列", Toast.LENGTH_SHORT).show()
            commitReorderIfNeeded()
            isEditMode = false
            selectedTrackIds = emptySet()
        }
    }

    val handleDeleteSelected: () -> Unit = {
        if (!canDelete) {
            Toast.makeText(context, "当前歌单不支持删除歌曲", Toast.LENGTH_SHORT).show()
        } else if (selectedTrackIds.isEmpty()) {
            Toast.makeText(context, "请先选择歌曲", Toast.LENGTH_SHORT).show()
        } else {
            showDeleteConfirmDialog = true
        }
    }

    val handleToggleSelect: (Long) -> Unit = { trackId ->
        selectedTrackIds = if (trackId in selectedTrackIds) {
            selectedTrackIds - trackId
        } else {
            selectedTrackIds + trackId
        }
    }

    val handleSelectAll: () -> Unit = {
        val allSelected = localTracks.isNotEmpty() && selectedTrackIds.size == localTracks.size
        selectedTrackIds = if (allSelected) emptySet() else localTracks.map { it.id }.toSet()
    }

    val handleEnterEditMode: (Track) -> Unit = { track ->
        isEditMode = true
        selectedTrackIds = setOf(track.id)
    }

    val handleExitEditMode: () -> Unit = {
        commitReorderIfNeeded()
        isEditMode = false
        selectedTrackIds = emptySet()
    }

    val handleTracksReordered: (List<Track>) -> Unit = { reordered ->
        localTracks = reordered
    }

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

        if (isEditMode) {
            BackHandler(enabled = true) {
                handleExitEditMode()
            }
        } else if (isCardMode && isPredictiveBackEnabled) {
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
                val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val songsTopInset = if (split) statusBarHeight else 0.dp
                val songsTopBackdrop = rememberLayerBackdrop {
                    drawRect(solidBackgroundColor)
                    drawContent()
                }
                val songsTopBlurAlpha by remember(listState, density) {
                    val fadeDistancePx = with(density) { 40.dp.toPx().coerceAtLeast(1f) }
                    derivedStateOf {
                        if (listState.firstVisibleItemIndex > 0) 1f
                        else (listState.firstVisibleItemScrollOffset / fadeDistancePx).coerceIn(0f, 1f)
                    }
                }
                val songs: @Composable () -> Unit = {
                    val splitBottomInsetPx = with(density) { (64.dp + bottom).toPx() }
                    val dragReorderState = rememberPlaylistDragReorderState(
                        lazyListState = listState,
                        tracks = currentDetail.tracks,
                        topViewportInsetPx = 0f,
                        bottomViewportInsetPx = splitBottomInsetPx,
                        onTracksReordered = handleTracksReordered
                    )

                    Box(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize()) {
                            AnimatedVisibility(
                                visible = isEditMode,
                                enter = expandVertically(tween(240, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
                                exit = shrinkVertically(tween(220, easing = FastOutSlowInEasing)) + fadeOut(tween(160))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = songsTopInset)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val allSelected = currentDetail.tracks.isNotEmpty() && selectedTrackIds.size == currentDetail.tracks.size
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable(onClick = handleSelectAll)
                                                .padding(vertical = 6.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            SelectionCheckbox(isSelected = allSelected, isDark = paneDark)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "全选",
                                                fontSize = 15.sp,
                                                color = if (paneDark) Color.White else PlaylistTextPrimary
                                            )
                                        }

                                        Text(
                                            text = "完成",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PlaylistAccentColor,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable(onClick = handleExitEditMode)
                                                .padding(vertical = 6.dp, horizontal = 8.dp)
                                        )
                                    }
                                    HorizontalDivider(
                                        color = if (paneDark) Color.White.copy(alpha = 0.08f) else PlaylistDividerColor,
                                        thickness = 0.5.dp
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                LazyColumn(
                                    Modifier
                                        .fillMaxSize()
                                        .layerBackdrop(songsTopBackdrop)
                                        .playlistReorderGesture(state = dragReorderState, enabled = isEditMode && canReorder),
                                    state = listState,
                                    contentPadding = PaddingValues(
                                        top = if (isEditMode) 12.dp else songsTopInset + 12.dp,
                                        bottom = 64.dp + bottom + 16.dp
                                    )
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
                                    val isSelected = track.id in selectedTrackIds
                                    val isActivelyDragging = dragReorderState.isDragging(track.id)
                                    val isElevated = dragReorderState.isElevated(track.id)
                                    val highlightAlpha by animateFloatAsState(
                                        targetValue = if (isActivelyDragging) 0.10f else 0f,
                                        animationSpec = tween(160),
                                        label = "splitDragBg"
                                    )

                                    Column(
                                        modifier = Modifier
                                            .zIndex(if (isElevated) 10f else 0f)
                                            .graphicsLayer {
                                                translationY = dragReorderState.translationYFor(track.id)
                                            }
                                            .animateItem(
                                                placementSpec = if (isActivelyDragging) null else spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp)
                                                .background(
                                                    color = if (paneDark) Color.White.copy(alpha = highlightAlpha) else Color.Black.copy(alpha = highlightAlpha * 0.5f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                        ) {
                                            PlaylistDetailTrackRow(
                                                index = index + 1,
                                                track = track,
                                                isDark = paneDark,
                                                isEditMode = isEditMode,
                                                isSelected = isSelected,
                                                canReorder = canReorder,
                                                onClick = { onPlayTrack(track) },
                                                onLongClick = { handleEnterEditMode(track) },
                                                onToggleSelect = { handleToggleSelect(track.id) },
                                                onMoreClick = { selectedTrackForAction = track }
                                            )
                                        }
                                        if (!isElevated) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(start = 88.dp, end = 20.dp),
                                                color = if (paneDark) Color.White.copy(alpha = 0.08f) else PlaylistDividerColor
                                            )
                                        }
                                    }
                                }
                            }
                            if (split && !isEditMode) {
                                ProgressiveTopBlur(
                                    backdrop = songsTopBackdrop,
                                    totalHeight = songsTopInset + 32.dp,
                                    statusBarHeight = songsTopInset,
                                    modifier = Modifier.align(Alignment.TopCenter),
                                    alphaProvider = { songsTopBlurAlpha },
                                    deepBlurRadius = 20.dp,
                                    mediumBlurRadius = 10.dp,
                                    shallowBlurRadius = 4.dp
                                )
                            }
                            }
                        }

                        AnimatedVisibility(
                            visible = isEditMode,
                            enter = slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),
                            exit = slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(160)),
                            modifier = Modifier.align(Alignment.BottomCenter).zIndex(5f)
                        ) {
                            PlaylistEditBottomBar(
                                selectedCount = selectedTrackIds.size,
                                isDark = paneDark,
                                backgroundColor = if (paneDark) solidBackgroundColor else Color.White,
                                onPlayNext = handlePlayNextSelected,
                                onAddToPlaylist = handleAddToPlaylistSelected,
                                onDownload = handleDownloadSelected,
                                onDelete = handleDeleteSelected,
                                canDelete = canDelete
                            )
                        }
                    }
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(if (embedded) Modifier.statusBarsPadding() else Modifier)
                ) {
                    if (split) {
                        AdaptiveTwoPane(
                            Modifier.fillMaxSize().testTag("expanded_playlist_panes"),
                            primaryFraction = 0.44f,
                            primary = {
                                if (window.isTabletop) {
                                    Column(Modifier.fillMaxSize().padding(top = statusBarHeight)) {
                                        ExpandedPlaylistToolbar(expandedDark, onClose = onClose) { showMoreSheet = true }
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
                                    }
                                } else {
                                    val primaryScrollState = rememberScrollState()
                                    val primaryTopBackdrop = rememberLayerBackdrop {
                                        drawRect(solidBackgroundColor)
                                        drawContent()
                                    }
                                    val primaryBlurAlpha by remember(primaryScrollState, density) {
                                        val fadeDistancePx = with(density) { 40.dp.toPx().coerceAtLeast(1f) }
                                        derivedStateOf {
                                            (primaryScrollState.value / fadeDistancePx).coerceIn(0f, 1f)
                                        }
                                    }
                                    Box(Modifier.fillMaxSize()) {
                                        Column(
                                            Modifier
                                                .fillMaxSize()
                                                .layerBackdrop(primaryTopBackdrop)
                                                .verticalScroll(primaryScrollState)
                                                .padding(
                                                    start = 20.dp,
                                                    end = 20.dp,
                                                    top = statusBarHeight + 68.dp,
                                                    bottom = bottom + 16.dp
                                                ),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                                                RenderPlaylistHero(
                                                    currentStyle, detail, solidBackgroundColor,
                                                    onPlayAll, isDark = expandedDark, expanded = true
                                                )
                                            }
                                        }
                                        ProgressiveTopBlur(
                                            backdrop = primaryTopBackdrop,
                                            totalHeight = statusBarHeight + 82.dp,
                                            statusBarHeight = statusBarHeight,
                                            modifier = Modifier.align(Alignment.TopCenter),
                                            alphaProvider = { primaryBlurAlpha },
                                            deepBlurRadius = 22.dp,
                                            mediumBlurRadius = 11.dp,
                                            shallowBlurRadius = 4.dp
                                        )
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(top = statusBarHeight)
                                                .align(Alignment.TopCenter)
                                        ) {
                                            ExpandedPlaylistToolbar(expandedDark, primaryTopBackdrop, onClose) { showMoreSheet = true }
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
                    isEditMode = isEditMode,
                    canReorder = canReorder,
                    selectedTrackIds = selectedTrackIds,
                    onToggleSelect = handleToggleSelect,
                    onSelectAll = handleSelectAll,
                    onExitEditMode = handleExitEditMode,
                    onEnterEditMode = handleEnterEditMode,
                    onTracksReordered = handleTracksReordered,
                    onPlayNextSelected = handlePlayNextSelected,
                    onAddToPlaylistSelected = handleAddToPlaylistSelected,
                    onDownloadSelected = handleDownloadSelected,
                    onDeleteSelected = handleDeleteSelected,
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

            // 批量收藏到歌单底栏弹窗
            if (showBatchAddToPlaylistSheet) {
                val selectedTracks = remember(localTracks, selectedTrackIds) {
                    localTracks.filter { it.id in selectedTrackIds }
                }
                AddToPlaylistBatchSheet(
                    tracks = selectedTracks,
                    currentPlaylistId = currentDetail.id,
                    onDismiss = { showBatchAddToPlaylistSheet = false },
                    onTracksAdded = { targetPlaylistId, addedTracks ->
                        onTracksAddedToPlaylist?.invoke(targetPlaylistId, addedTracks)
                        showBatchAddToPlaylistSheet = false
                        isEditMode = false
                        selectedTrackIds = emptySet()
                    }
                )
            }

            // 删除确认对话框
            if (showDeleteConfirmDialog) {
                val deleteMessage = if (currentDetail.id == LOCAL_PLAYLIST_ID) {
                    "确定要从本地音乐列表中移除选中的 ${selectedTrackIds.size} 首歌曲吗？"
                } else if (isLikedPlaylist) {
                    "确定要从我喜欢的音乐中移除选中的 ${selectedTrackIds.size} 首歌曲吗？"
                } else {
                    "确定要从歌单「${currentDetail.name}」中删除选中的 ${selectedTrackIds.size} 首歌曲吗？"
                }

                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = {
                        Text(
                            text = "删除歌曲",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    text = {
                        Text(
                            text = deleteMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val count = selectedTrackIds.size
                                onDeleteTracks?.invoke(selectedTrackIds)
                                localTracks = localTracks.filterNot { it.id in selectedTrackIds }
                                selectedTrackIds = emptySet()
                                isEditMode = false
                                showDeleteConfirmDialog = false
                                Toast.makeText(context, "已删除 $count 首歌曲", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = "删除",
                                color = PlaylistAccentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text(
                                text = "取消",
                                color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF666666)
                            )
                        }
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
                    onDismiss = { showMoreSheet = false },
                    onEnterEditMode = {
                        isEditMode = true
                        if (selectedTrackIds.isEmpty() && localTracks.isNotEmpty()) {
                            selectedTrackIds = setOf(localTracks.first().id)
                        }
                    }
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
private fun ExpandedPlaylistToolbar(
    isDark: Boolean,
    backdrop: Backdrop? = null,
    onClose: () -> Unit,
    onMore: () -> Unit
) {
    val sideControls = LocalAdaptiveWindow.current.useNavigationRail && !LocalAdaptiveWindow.current.sideControlsAtStart
    val tint = if (isDark) Color.White else PlaylistTextPrimary
    val button: @Composable (Boolean, () -> Unit) -> Unit = { back, action ->
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            LiquidGlassCircleButton(
                onClick = action,
                backdrop = backdrop,
                size = 38.dp,
                isDarkSurface = isDark,
                contentDescription = if (back) "返回" else "更多操作"
            ) {
                if (back) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(20.dp), tint = tint)
                } else {
                    AppleMusicMoreIcon(tint = tint, size = 20.dp)
                }
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

private class PlaylistDragReorderState(
    val lazyListState: LazyListState,
    val topViewportInsetPx: Float,
    val bottomViewportInsetPx: Float,
    val touchSlopPx: Float,
    private val tracksProvider: () -> List<Track>,
    private val onTracksReordered: (List<Track>) -> Unit,
    private val coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    var draggedTrackId by mutableStateOf<Long?>(null)
    var settlingTrackId by mutableStateOf<Long?>(null)
    var desiredItemTopY by mutableFloatStateOf(0f)
    var rawDesiredItemTopY by mutableFloatStateOf(0f)
    var hasMoved by mutableStateOf(false)
    var totalDragDy by mutableFloatStateOf(0f)
    var dragDirection by mutableIntStateOf(0)
    val settleOffsetY = Animatable(0f)

    fun isDragging(trackId: Long): Boolean = draggedTrackId == trackId

    fun isElevated(trackId: Long): Boolean = draggedTrackId == trackId || settlingTrackId == trackId

    fun translationYFor(trackId: Long): Float {
        return when {
            draggedTrackId == trackId -> {
                if (!hasMoved) return 0f
                val info = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == trackId }
                if (info != null) desiredItemTopY - info.offset else 0f
            }
            settlingTrackId == trackId -> settleOffsetY.value
            else -> 0f
        }
    }

    fun startDrag(trackId: Long): Boolean {
        val info = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == trackId } ?: return false
        settlingTrackId = null
        draggedTrackId = trackId
        rawDesiredItemTopY = info.offset.toFloat()
        desiredItemTopY = info.offset.toFloat()
        hasMoved = false
        totalDragDy = 0f
        dragDirection = 0
        return true
    }

    fun clampDesiredTopY(trackId: Long, candidateTopY: Float): Float {
        val currentTracks = tracksProvider()
        val currentIdx = currentTracks.indexOfFirst { it.id == trackId }
        val layoutInfo = lazyListState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        val draggedInfo = visibleItems.firstOrNull { it.key == trackId } ?: return candidateTopY
        if (currentIdx < 0 || currentTracks.isEmpty()) return draggedInfo.offset.toFloat()

        val viewportTopLimit = layoutInfo.viewportStartOffset.toFloat() + topViewportInsetPx
        val viewportBottomLimit = (layoutInfo.viewportEndOffset - bottomViewportInsetPx - draggedInfo.size).toFloat()

        val firstTrackInfo = visibleItems.firstOrNull { it.key == currentTracks.first().id }
        val lastTrackInfo = visibleItems.firstOrNull { it.key == currentTracks.last().id }

        val minTop = when {
            currentIdx == 0 -> draggedInfo.offset.toFloat()
            firstTrackInfo != null -> maxOf(viewportTopLimit, firstTrackInfo.offset.toFloat())
            else -> viewportTopLimit
        }

        val maxTop = when {
            currentIdx == currentTracks.lastIndex -> draggedInfo.offset.toFloat()
            lastTrackInfo != null -> minOf(viewportBottomLimit, (lastTrackInfo.offset + lastTrackInfo.size - draggedInfo.size).toFloat())
            else -> viewportBottomLimit
        }

        return candidateTopY.coerceIn(minTop, maxOf(minTop, maxTop))
    }

    fun onDragMove(trackId: Long, deltaY: Float) {
        if (draggedTrackId != trackId) return
        totalDragDy += deltaY
        rawDesiredItemTopY += deltaY
        if (abs(deltaY) > 0.5f) {
            dragDirection = if (deltaY > 0f) 1 else -1
        }
        if (!hasMoved) {
            if (abs(totalDragDy) < touchSlopPx) return
            hasMoved = true
            dragDirection = if (totalDragDy > 0f) 1 else -1
        }
        desiredItemTopY = clampDesiredTopY(trackId, rawDesiredItemTopY)
        rawDesiredItemTopY = desiredItemTopY
        trySwap(trackId)
    }

    fun trySwap(trackId: Long) {
        if (draggedTrackId != trackId || !hasMoved) return
        val currentTracks = tracksProvider()
        val currentIdx = currentTracks.indexOfFirst { it.id == trackId }
        if (currentIdx < 0) return

        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val draggedInfo = visibleItems.firstOrNull { it.key == trackId } ?: return
        val draggedCenter = desiredItemTopY + draggedInfo.size / 2f

        if (currentIdx < currentTracks.lastIndex) {
            val nextTrack = currentTracks[currentIdx + 1]
            val nextInfo = visibleItems.firstOrNull { it.key == nextTrack.id }
            if (nextInfo != null && draggedInfo.index < nextInfo.index && draggedCenter > nextInfo.offset + nextInfo.size * 0.5f) {
                val mutable = currentTracks.toMutableList()
                mutable[currentIdx] = nextTrack
                mutable[currentIdx + 1] = currentTracks[currentIdx]
                onTracksReordered(mutable)
                return
            }
        }

        if (currentIdx > 0) {
            val prevTrack = currentTracks[currentIdx - 1]
            val prevInfo = visibleItems.firstOrNull { it.key == prevTrack.id }
            if (prevInfo != null && prevInfo.index < draggedInfo.index && draggedCenter < prevInfo.offset + prevInfo.size * 0.5f) {
                val mutable = currentTracks.toMutableList()
                mutable[currentIdx] = prevTrack
                mutable[currentIdx - 1] = currentTracks[currentIdx]
                onTracksReordered(mutable)
                return
            }
        }
    }

    fun endDrag(trackId: Long) {
        if (draggedTrackId != trackId) return
        val currentTranslation = translationYFor(trackId)
        draggedTrackId = null
        hasMoved = false
        totalDragDy = 0f
        dragDirection = 0
        if (abs(currentTranslation) > 0.5f) {
            settlingTrackId = trackId
            coroutineScope.launch {
                try {
                    settleOffsetY.snapTo(currentTranslation)
                    settleOffsetY.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                } finally {
                    if (settlingTrackId == trackId) {
                        settlingTrackId = null
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberPlaylistDragReorderState(
    lazyListState: LazyListState,
    tracks: List<Track>,
    topViewportInsetPx: Float,
    bottomViewportInsetPx: Float,
    onTracksReordered: (List<Track>) -> Unit
): PlaylistDragReorderState {
    val coroutineScope = rememberCoroutineScope()
    val tracksState = rememberUpdatedState(tracks)
    val onReorderState = rememberUpdatedState(onTracksReordered)
    val density = LocalDensity.current
    val edgeZonePx = with(density) { 56.dp.toPx() }
    val touchSlopPx = with(density) { 4.dp.toPx() }
    val maxScrollSpeedPxPerSec = with(density) { 800.dp.toPx() }

    val state = remember(lazyListState, coroutineScope, topViewportInsetPx, bottomViewportInsetPx, touchSlopPx) {
        PlaylistDragReorderState(
            lazyListState = lazyListState,
            topViewportInsetPx = topViewportInsetPx,
            bottomViewportInsetPx = bottomViewportInsetPx,
            touchSlopPx = touchSlopPx,
            tracksProvider = { tracksState.value },
            onTracksReordered = { reordered -> onReorderState.value(reordered) },
            coroutineScope = coroutineScope
        )
    }

    LaunchedEffect(state.draggedTrackId, state.hasMoved) {
        val activeId = state.draggedTrackId ?: return@LaunchedEffect
        if (!state.hasMoved) return@LaunchedEffect
        var lastFrameTimeNanos = 0L
        while (state.draggedTrackId == activeId && state.hasMoved) {
            withFrameNanos { frameTimeNanos ->
                val dtSec = if (lastFrameTimeNanos == 0L) {
                    0.016f
                } else {
                    ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                }
                lastFrameTimeNanos = frameTimeNanos

                val currentTracks = tracksState.value
                val currentIdx = currentTracks.indexOfFirst { it.id == activeId }
                val layoutInfo = lazyListState.layoutInfo
                val draggedInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.key == activeId }
                if (draggedInfo != null && currentIdx >= 0 && currentTracks.isNotEmpty()) {
                    val beforePadding = -layoutInfo.viewportStartOffset.toFloat()
                    val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
                    val topBound = topViewportInsetPx
                    val bottomBound = (viewportHeight - bottomViewportInsetPx).coerceAtLeast(topBound + 1f)
                    val effectiveEdge = minOf(edgeZonePx, (bottomBound - topBound) * 0.25f).coerceAtLeast(1f)

                    val itemScreenTop = beforePadding + state.desiredItemTopY
                    val itemScreenBottom = itemScreenTop + draggedInfo.size

                    var scrollDelta = 0f
                    if (state.dragDirection < 0 && currentIdx > 0 && itemScreenTop < topBound + effectiveEdge) {
                        val firstTrackInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.key == currentTracks.first().id }
                        val maxUpScroll = if (firstTrackInfo != null) {
                            (topBound - (beforePadding + firstTrackInfo.offset)).coerceAtLeast(0f)
                        } else {
                            Float.POSITIVE_INFINITY
                        }
                        if (maxUpScroll > 0.5f && lazyListState.canScrollBackward) {
                            val factor = (((topBound + effectiveEdge) - itemScreenTop) / effectiveEdge).coerceIn(0f, 1.5f)
                            scrollDelta = (-maxScrollSpeedPxPerSec * factor * dtSec).coerceAtLeast(-maxUpScroll)
                        }
                    } else if (state.dragDirection > 0 && currentIdx < currentTracks.lastIndex && itemScreenBottom > bottomBound - effectiveEdge) {
                        val lastTrackInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.key == currentTracks.last().id }
                        val maxDownScroll = if (lastTrackInfo != null) {
                            ((beforePadding + lastTrackInfo.offset + lastTrackInfo.size) - bottomBound).coerceAtLeast(0f)
                        } else {
                            Float.POSITIVE_INFINITY
                        }
                        if (maxDownScroll > 0.5f && lazyListState.canScrollForward) {
                            val factor = ((itemScreenBottom - (bottomBound - effectiveEdge)) / effectiveEdge).coerceIn(0f, 1.5f)
                            scrollDelta = (maxScrollSpeedPxPerSec * factor * dtSec).coerceAtMost(maxDownScroll)
                        }
                    }

                    if (scrollDelta != 0f) {
                        lazyListState.dispatchRawDelta(scrollDelta)
                    }
                    state.desiredItemTopY = state.clampDesiredTopY(activeId, state.rawDesiredItemTopY)
                    state.rawDesiredItemTopY = state.desiredItemTopY
                    state.trySwap(activeId)
                }
            }
        }
    }

    return state
}

@Composable
private fun Modifier.playlistReorderGesture(
    state: PlaylistDragReorderState,
    enabled: Boolean
): Modifier {
    if (!enabled) return this
    val handleZoneWidthPx = with(LocalDensity.current) { 64.dp.toPx() }
    return this.pointerInput(state, enabled) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.position.x < size.width - handleZoneWidthPx) return@awaitEachGesture
            if (down.position.y < state.topViewportInsetPx || down.position.y > size.height - state.bottomViewportInsetPx) {
                return@awaitEachGesture
            }

            val layoutInfo = state.lazyListState.layoutInfo
            val pointerOffsetInItemSpace = down.position.y + layoutInfo.viewportStartOffset
            val hitItem = layoutInfo.visibleItemsInfo.firstOrNull {
                it.key is Long && pointerOffsetInItemSpace >= it.offset && pointerOffsetInItemSpace <= it.offset + it.size
            } ?: return@awaitEachGesture

            val trackId = hitItem.key as Long
            down.consume()
            if (!state.startDrag(trackId)) return@awaitEachGesture

            var lastPointerY = down.position.y
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val pointerChange = event.changes.firstOrNull { it.id == down.id }
                        ?: event.changes.firstOrNull()
                        ?: break
                    if (!pointerChange.pressed) {
                        pointerChange.consume()
                        break
                    }
                    pointerChange.consume()
                    val deltaY = pointerChange.position.y - lastPointerY
                    lastPointerY = pointerChange.position.y
                    if (deltaY != 0f) {
                        state.onDragMove(trackId, deltaY)
                    }
                }
            } finally {
                state.endDrag(trackId)
            }
        }
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
    isEditMode: Boolean,
    canReorder: Boolean = false,
    selectedTrackIds: Set<Long>,
    onToggleSelect: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onExitEditMode: () -> Unit,
    onEnterEditMode: (Track) -> Unit,
    onTracksReordered: (List<Track>) -> Unit,
    onPlayNextSelected: () -> Unit,
    onAddToPlaylistSelected: () -> Unit,
    onDownloadSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    lazyListState: androidx.compose.foundation.lazy.LazyListState
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val context = LocalContext.current
    val density = LocalDensity.current

    val isPosterWall = currentStyle == PlaylistDetailStyle.POSTER_WALL || currentStyle == PlaylistDetailStyle.DYNAMIC_POSTER_WALL
    var heroTitleOffsetFromTopPx by remember(detail.id, currentStyle) { mutableFloatStateOf(0f) }
    var heroTitleLineCount by remember(detail.id, currentStyle) { mutableIntStateOf(1) }
    val contentTopPaddingPx = remember(density, isPosterWall, statusBarHeight) {
        with(density) { (if (isPosterWall) 0.dp else statusBarHeight + 12.dp).toPx() }
    }
    val dockCenterY = remember(density, statusBarHeight) {
        with(density) { (statusBarHeight + 27.dp).toPx() }
    }
    val morphBandPx = remember(density) {
        with(density) { 54.dp.toPx() }
    }
    val heroTitleCenterY by remember(lazyListState, contentTopPaddingPx) {
        derivedStateOf {
            val offsetInHero = heroTitleOffsetFromTopPx
            if (offsetInHero <= 0f) {
                Float.POSITIVE_INFINITY
            } else if (lazyListState.firstVisibleItemIndex == 0) {
                contentTopPaddingPx - lazyListState.firstVisibleItemScrollOffset + offsetInHero
            } else {
                Float.NEGATIVE_INFINITY
            }
        }
    }
    val topViewportInsetPx = with(density) { (statusBarHeight + 54.dp + 44.dp).toPx() }
    val bottomViewportInsetPx = with(density) { (60.dp + navBottom).toPx() }

    val dragReorderState = rememberPlaylistDragReorderState(
        lazyListState = lazyListState,
        tracks = detail.tracks,
        topViewportInsetPx = topViewportInsetPx,
        bottomViewportInsetPx = bottomViewportInsetPx,
        onTracksReordered = onTracksReordered
    )

    val constantBottomPadding = maxOf(LocalContentBottomPadding.current, 60.dp + navBottom) + 16.dp
    val pageBaseColor = if (isDark) dominantDarkColor else Color.White
    val playlistTopBackdrop = rememberLayerBackdrop {
        drawRect(pageBaseColor)
        drawContent()
    }
    val topBlurAlpha by remember(lazyListState, density) {
        val fadeDistancePx = with(density) { 44.dp.toPx().coerceAtLeast(1f) }
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex > 0) 1f
            else (lazyListState.firstVisibleItemScrollOffset / fadeDistancePx).coerceIn(0f, 1f)
        }
    }

    // 整页统一纯色沉浸背景，没有任何黑色断层和过渡色阶，彻底消除透光
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBaseColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(playlistTopBackdrop)
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .playlistReorderGesture(state = dragReorderState, enabled = isEditMode && canReorder)
                    .verticalElasticOverscroll(),
                contentPadding = PaddingValues(
                    top = if (isPosterWall) 0.dp else statusBarHeight + 12.dp,
                    bottom = constantBottomPadding,
                    start = if (isPosterWall) 0.dp else 16.dp,
                    end = if (isPosterWall) 0.dp else 16.dp
                )
            ) {
            // 1. Hero 头部（原地保持，长按与退出不自动跳动列表）
            item(key = "playlist_hero") {
                RenderPlaylistHero(
                    style = currentStyle,
                    detail = detail,
                    dominantDarkColor = dominantDarkColor,
                    onPlayAll = onPlayAll,
                    topInset = if (isPosterWall) statusBarHeight else 0.dp,
                    isDark = isDark,
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
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 2. 曲目列表（统一列表，不切换两套 LazyColumn，进入/退出原地过渡）
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
                    val isSelected = track.id in selectedTrackIds
                    val isActivelyDragging = dragReorderState.isDragging(track.id)
                    val isElevated = dragReorderState.isElevated(track.id)
                    val highlightAlpha by animateFloatAsState(
                        targetValue = if (isActivelyDragging) 0.10f else 0f,
                        animationSpec = tween(160),
                        label = "compactDragBg"
                    )

                    Column(
                        modifier = Modifier
                            .zIndex(if (isElevated) 10f else 0f)
                            .graphicsLayer {
                                translationY = dragReorderState.translationYFor(track.id)
                            }
                            .animateItem(
                                placementSpec = if (isActivelyDragging) null else spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .then(if (isPosterWall) Modifier.padding(horizontal = 16.dp) else Modifier)
                                .background(
                                    color = if (isDark) Color.White.copy(alpha = highlightAlpha) else Color.Black.copy(alpha = highlightAlpha * 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            PlaylistDetailTrackRow(
                                index = index + 1,
                                track = track,
                                isDark = isDark,
                                isEditMode = isEditMode,
                                isSelected = isSelected,
                                canReorder = canReorder,
                                onClick = { onPlayTrack(track) },
                                onLongClick = { onEnterEditMode(track) },
                                onToggleSelect = { onToggleSelect(track.id) },
                                onMoreClick = { onTrackMoreClick(track) }
                            )
                        }
                        if (index < detail.tracks.lastIndex && !isElevated) {
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
        }
        }

        // 正常模式下顶部渐进高斯模糊、连续一体式标题与左右圆钮（随编辑模式平滑淡出）
        AnimatedVisibility(
            visible = !isEditMode,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // 1. 顶部渐进高斯模糊（顶部深、底部浅自然过渡，无白色状态栏遮罩）
                if (isPosterWall) {
                    ProgressiveTopBlur(
                        backdrop = playlistTopBackdrop,
                        totalHeight = statusBarHeight + 24.dp,
                        statusBarHeight = statusBarHeight,
                        modifier = Modifier.align(Alignment.TopCenter),
                        alphaProvider = { 1f - topBlurAlpha },
                        deepBlurRadius = 16.dp,
                        mediumBlurRadius = 8.dp,
                        shallowBlurRadius = 3.dp
                    )
                }
                ProgressiveTopBlur(
                    backdrop = playlistTopBackdrop,
                    totalHeight = statusBarHeight + 82.dp,
                    statusBarHeight = statusBarHeight,
                    modifier = Modifier.align(Alignment.TopCenter),
                    alphaProvider = { topBlurAlpha }
                )

                // 2. 连续一体式歌单标题：封面下方歌单名进入顶部区域后随手指上滑缩小并固定到顶栏中央
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .align(Alignment.TopCenter)
                        .offset {
                            val currentY = heroTitleCenterY
                            val startY = dockCenterY + morphBandPx
                            val targetY = if (currentY <= startY) {
                                currentY.coerceAtLeast(dockCenterY)
                            } else {
                                startY
                            }
                            IntOffset(0, (targetY - 22.dp.toPx()).roundToInt())
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val scaleFactor = 16f / 22f
                    val dockedMaxTextWidth = ((maxWidth - 124.dp) / scaleFactor).coerceAtLeast(120.dp)
                    val layoutMaxWidth = minOf(maxWidth - 48.dp, dockedMaxTextWidth)
                    Text(
                        text = detail.displayTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else PlaylistTextPrimary
                        ),
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

                // 3. 左右两个常驻液态玻璃操作按钮
                val buttonTint = if (isDark) Color.White else Color(0xFF111111)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = statusBarHeight + 8.dp, start = 16.dp, end = 16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    LiquidGlassCircleButton(
                        onClick = onClose,
                        backdrop = playlistTopBackdrop,
                        size = 38.dp,
                        isDarkSurface = isDark,
                        contentDescription = "返回",
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = buttonTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    LiquidGlassCircleButton(
                        onClick = onOpenMoreMenu,
                        backdrop = playlistTopBackdrop,
                        size = 38.dp,
                        isDarkSurface = isDark,
                        contentDescription = "更多操作",
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        AppleMusicMoreIcon(
                            tint = buttonTint,
                            size = 20.dp
                        )
                    }
                }
            }
        }

        // 编辑模式顶部固定栏（平滑滑入滑出）
        AnimatedVisibility(
            visible = isEditMode,
            enter = slideInVertically(tween(260, easing = FastOutSlowInEasing)) { -it / 2 } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { -it / 2 } + fadeOut(tween(180)),
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).zIndex(5f)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                ProgressiveTopBlur(
                    backdrop = playlistTopBackdrop,
                    totalHeight = statusBarHeight + 122.dp,
                    statusBarHeight = statusBarHeight,
                    modifier = Modifier.align(Alignment.TopCenter),
                    deepBlurRadius = 28.dp,
                    mediumBlurRadius = 15.dp,
                    shallowBlurRadius = 5.dp
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                Modifier.background(pageBaseColor.copy(alpha = 0.92f))
                            } else {
                                Modifier
                            }
                        )
                        .statusBarsPadding()
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val editTint = if (isDark) Color.White else PlaylistTextPrimary
                    LiquidGlassCircleButton(
                        onClick = onExitEditMode,
                        backdrop = playlistTopBackdrop,
                        size = 38.dp,
                        isDarkSurface = isDark,
                        contentDescription = "返回"
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = editTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = detail.displayTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = editTint
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    LiquidGlassCircleButton(
                        onClick = { Toast.makeText(context, "搜索歌曲功能即将推出", Toast.LENGTH_SHORT).show() },
                        backdrop = playlistTopBackdrop,
                        size = 38.dp,
                        isDarkSurface = isDark,
                        contentDescription = "搜索"
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_nav_search),
                            contentDescription = null,
                            tint = editTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    LiquidGlassCircleButton(
                        onClick = onOpenMoreMenu,
                        backdrop = playlistTopBackdrop,
                        size = 38.dp,
                        isDarkSurface = isDark,
                        contentDescription = "更多操作"
                    ) {
                        AppleMusicMoreIcon(
                            tint = editTint,
                            size = 20.dp
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val allSelected = detail.tracks.isNotEmpty() && selectedTrackIds.size == detail.tracks.size
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onSelectAll)
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SelectionCheckbox(
                            isSelected = allSelected,
                            isDark = isDark
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "全选",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                color = if (isDark) Color.White else PlaylistTextPrimary
                            )
                        )
                    }

                    Text(
                        text = "完成",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PlaylistAccentColor
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onExitEditMode)
                            .padding(vertical = 6.dp, horizontal = 8.dp)
                    )
                }

                HorizontalDivider(
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else PlaylistDividerColor,
                    thickness = 0.5.dp
                )
            }
            }
        }

        // 编辑模式底部多选操作栏（平滑从底部滑入）
        AnimatedVisibility(
            visible = isEditMode,
            enter = slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(5f)
        ) {
            PlaylistEditBottomBar(
                selectedCount = selectedTrackIds.size,
                isDark = isDark,
                backgroundColor = if (isDark) dominantDarkColor else Color.White,
                onPlayNext = onPlayNextSelected,
                onAddToPlaylist = onAddToPlaylistSelected,
                onDownload = onDownloadSelected,
                onDelete = onDeleteSelected,
                canDelete = canReorder
            )
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
    artworkHeight: androidx.compose.ui.unit.Dp? = null,
    titleAlphaProvider: () -> Float = { 1f },
    onHeroTitleMetricsChanged: ((centerOffsetFromTopPx: Float) -> Unit)? = null,
    onTitleLineCount: ((Int) -> Unit)? = null
) {
    val coordsHolder = remember { arrayOfNulls<LayoutCoordinates>(2) }
    val reportTitleMetrics = {
        val root = coordsHolder[0]
        val title = coordsHolder[1]
        if (root != null && title != null && root.isAttached && title.isAttached) {
            val topY = root.localPositionOf(title, Offset.Zero).y
            onHeroTitleMetricsChanged?.invoke(topY + title.size.height / 2f)
        }
    }
    val onTitlePositioned: ((LayoutCoordinates) -> Unit)? = if (onHeroTitleMetricsChanged != null) {
        { coords ->
            coordsHolder[1] = coords
            reportTitleMetrics()
        }
    } else {
        null
    }

    Box(
        Modifier
            .fillMaxWidth()
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
            .testTag("playlist_hero")
            .semantics { stateDescription = style.title }
    ) {
        when (style) {
            PlaylistDetailStyle.APPLE_MUSIC -> {
                PlaylistHeroAppleMusicSection(
                    detail = detail,
                    isDark = isDark,
                    onPlayAll = onPlayAll,
                    coverWidthFraction = if (expanded) 0.88f else 0.68f,
                    showControls = !mediaOnly,
                    titleAlphaProvider = titleAlphaProvider,
                    onTitlePositioned = onTitlePositioned,
                    onTitleLineCount = onTitleLineCount
                )
            }
            PlaylistDetailStyle.POSTER_WALL -> {
                PlaylistHeroPosterWallSection(
                    detail = detail,
                    dominantDarkColor = dominantDarkColor,
                    isDark = isDark,
                    onPlayAll = onPlayAll,
                    topInset = topInset,
                    showControls = !mediaOnly,
                    titleAlphaProvider = titleAlphaProvider,
                    onTitlePositioned = onTitlePositioned,
                    onTitleLineCount = onTitleLineCount
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
                    artworkHeight = artworkHeight,
                    titleAlphaProvider = titleAlphaProvider,
                    onTitlePositioned = onTitlePositioned,
                    onTitleLineCount = onTitleLineCount
                )
            }
        }
    }
}

/**
 * 歌单曲目单行（支持深浅双色，支持正常与多选排序编辑模式）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistDetailTrackRow(
    index: Int,
    track: Track,
    isDark: Boolean = false,
    isEditMode: Boolean = false,
    isSelected: Boolean = false,
    canReorder: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onToggleSelect: (() -> Unit)? = null,
    onMoreClick: () -> Unit,
    dragHandleModifier: Modifier = Modifier
) {
    val leadingWidth by animateDpAsState(
        targetValue = if (isEditMode) 32.dp else 28.dp,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "trackRowLeadingWidth"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isEditMode) {
                    Modifier.clickable(onClick = { onToggleSelect?.invoke() })
                } else {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                }
            )
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 左侧区域：正常模式下展示序号，编辑模式下平滑切换为勾选圆圈
        Box(
            modifier = Modifier.width(leadingWidth),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isEditMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { onToggleSelect?.invoke() }),
                    contentAlignment = Alignment.CenterStart
                ) {
                    SelectionCheckbox(
                        isSelected = isSelected,
                        isDark = isDark
                    )
                }
            } else {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isDark) Color.White.copy(alpha = 0.45f) else PlaylistTextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                )
            }
        }

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

        // 4. 右侧区域：正常模式下展示竖置三点，支持排序的歌单在编辑模式下展示三道横线拖拽排序把手
        if (isEditMode) {
            if (canReorder) {
                ReorderDragHandle(
                    isDark = isDark,
                    modifier = dragHandleModifier
                )
            }
        } else {
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
}

/**
 * 多选模式勾选框：圆形设计，选中时红底白对勾，未选中时细边框空心圆
 */
@Composable
private fun SelectionCheckbox(
    isSelected: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier.background(PlaylistAccentColor)
                } else {
                    Modifier.border(
                        1.5.dp,
                        if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFFC7C7CC),
                        CircleShape
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * 拖拽排序把手：三道圆角横线，严格按截图还原
 */
@Composable
private fun ReorderDragHandle(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF9E9EA4))
                )
            }
        }
    }
}

/**
 * 多选底部悬浮操作栏（下一首播放、收藏到歌单、下载、删除）
 */
@Composable
private fun PlaylistEditBottomBar(
    selectedCount: Int,
    isDark: Boolean,
    backgroundColor: Color,
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean = true,
    modifier: Modifier = Modifier
) {
    val hasSelection = selectedCount > 0
    val activeAlpha = if (hasSelection) 1f else 0.38f
    val iconTint = (if (isDark) Color.White else Color(0xFF222222)).copy(alpha = activeAlpha)
    val textTint = (if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF444444)).copy(alpha = activeAlpha)
    val deleteAlpha = if (!canDelete) 0.28f else if (hasSelection) 1f else 0.38f
    val deleteIconTint = (if (isDark) Color.White else Color(0xFF222222)).copy(alpha = deleteAlpha)
    val deleteTextTint = (if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF444444)).copy(alpha = deleteAlpha)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = backgroundColor,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            HorizontalDivider(
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFEAEAEE),
                thickness = 0.5.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 下一首播放
                EditBottomActionItem(
                    icon = R.drawable.ic_action_play_next,
                    label = "下一首播放",
                    iconTint = iconTint,
                    textTint = textTint,
                    onClick = onPlayNext,
                    modifier = Modifier.weight(1f)
                )

                // 2. 收藏到歌单
                EditBottomActionItem(
                    icon = R.drawable.ic_action_add_to_playlist,
                    label = "收藏到歌单",
                    iconTint = iconTint,
                    textTint = textTint,
                    onClick = onAddToPlaylist,
                    modifier = Modifier.weight(1f)
                )

                // 3. 下载
                EditBottomActionItem(
                    icon = R.drawable.ic_action_download,
                    label = "下载",
                    iconTint = iconTint,
                    textTint = textTint,
                    onClick = onDownload,
                    modifier = Modifier.weight(1f)
                )

                // 4. 删除
                EditBottomActionItem(
                    icon = R.drawable.ic_delete_history,
                    label = "删除",
                    iconTint = deleteIconTint,
                    textTint = deleteTextTint,
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun EditBottomActionItem(
    icon: Int,
    label: String,
    iconTint: Color,
    textTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = textTint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
