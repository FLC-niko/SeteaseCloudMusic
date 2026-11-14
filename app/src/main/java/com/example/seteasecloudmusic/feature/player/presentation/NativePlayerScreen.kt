package com.example.seteasecloudmusic.feature.player.presentation

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.R
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import com.example.seteasecloudmusic.core.player.PlaybackState
import com.example.seteasecloudmusic.core.player.PlaybackMode
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.core.settings.OnlineAudioQuality
import com.example.seteasecloudmusic.core.ui.adaptive.AdaptiveModalSheet
import com.example.seteasecloudmusic.core.ui.adaptive.LocalAdaptiveWindow
import com.example.seteasecloudmusic.feature.player.domain.model.LyricSource
import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 播放页原生页面类型：
 * - [ALBUM]: 截图 1 大封面专辑页（播放/暂停弹性呼吸缩放、曲目信息、五角星收藏与三点菜单）
 * - [LYRIC]: 截图 2 / 3 / 4 AMLL 沉浸式逐字辉光歌词页（含控制栏自动沉浸收起）
 */
enum class NativePlayerPage {
    ALBUM,
    LYRIC
}

private val ExplicitLyricsRegex = Regex(
    "\\b(shit|fuck|fucking|fucked|motherfucker|bitch|bitches|asshole|dick|pussy|damn|goddamn)\\b",
    RegexOption.IGNORE_CASE
)

internal fun isExplicitTrack(track: Track?, lyrics: ParsedLyrics): Boolean {
    if (track == null) return false
    if (track.title.contains("Explicit", ignoreCase = true)) return true
    if (lyrics.lines.any { line -> line.words.any { it.obscene } }) return true
    return lyrics.lines.any { line ->
        val text = line.words.joinToString("") { it.word }
        ExplicitLyricsRegex.containsMatchIn(text)
    }
}

@Composable
fun NativePlayerScreen(
    playerViewModel: PlayerViewModel,
    onClose: () -> Unit,
    artworkAlpha: State<Float>,
    onArtworkBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    contentAlpha: State<Float>? = null,
    backgroundAlpha: State<Float>? = null,
    onArtworkCornerRadiusChanged: (Dp) -> Unit = {},
    onArtworkShadowChanged: (Dp) -> Unit = {},
    onArtworkTrackTopChanged: (Boolean) -> Unit = {},
    onGrabberDragStart: (() -> Unit)? = null,
    onGrabberDragDelta: ((Float) -> Unit)? = null,
    onGrabberDragEnd: ((Float) -> Unit)? = null
) {
    val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
    val parsedLyrics by playerViewModel.parsedLyrics.collectAsStateWithLifecycle()
    val isLyricsLoading by playerViewModel.isLyricsLoading.collectAsStateWithLifecycle()
    val currentAudioQuality by playerViewModel.currentAudioQuality.collectAsStateWithLifecycle()
    val likedTrackIds by playerViewModel.likedTrackIds.collectAsStateWithLifecycle()

    NativePlayerContent(playbackState, parsedLyrics, currentAudioQuality, onClose, artworkAlpha, onArtworkBoundsChanged,
        modifier, contentAlpha, backgroundAlpha, onArtworkCornerRadiusChanged, onArtworkShadowChanged,
        onArtworkTrackTopChanged, onGrabberDragStart, onGrabberDragDelta, onGrabberDragEnd,
        isLyricsLoading, likedTrackIds, playerViewModel::onPlayPause, playerViewModel::onNext,
        { playerViewModel.onPrevious() }, playerViewModel::seekTo, playerViewModel::toggleTrackLiked,
        playerViewModel::togglePlaybackMode, playerViewModel::setPlaybackMode, playerViewModel::selectAudioQuality,
        playerViewModel::playQueueTrackAt, playerViewModel::removeQueueTrackAt, playerViewModel::clearQueue)
}

/** Same content and saved hierarchy for every width; the screen wrapper supplies live playback actions. */
@Composable
internal fun NativePlayerContent(
    playbackState: PlaybackState,
    parsedLyrics: ParsedLyrics,
    currentAudioQuality: OnlineAudioQuality,
    onClose: () -> Unit,
    artworkAlpha: State<Float>,
    onArtworkBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    contentAlpha: State<Float>? = null,
    backgroundAlpha: State<Float>? = null,
    onArtworkCornerRadiusChanged: (Dp) -> Unit = {},
    onArtworkShadowChanged: (Dp) -> Unit = {},
    onArtworkTrackTopChanged: (Boolean) -> Unit = {},
    onGrabberDragStart: (() -> Unit)? = null,
    onGrabberDragDelta: ((Float) -> Unit)? = null,
    onGrabberDragEnd: ((Float) -> Unit)? = null,
    isLyricsLoading: Boolean = false,
    likedTrackIds: Set<Long> = emptySet(),
    onPlayPause: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onSeek: (Int) -> Unit = {},
    onToggleStar: (Long) -> Unit = {},
    onToggleMode: () -> Unit = {},
    onSetMode: (PlaybackMode) -> Unit = {},
    onQuality: (OnlineAudioQuality) -> Unit = {},
    onQueueTrack: (Int) -> Unit = {},
    onRemoveTrack: (Int) -> Unit = {},
    onClearQueue: () -> Unit = {}
) {
    val currentPosition = playbackState.currentPositionMs
    val isPlaying = playbackState.status == PlayerStatus.PLAYING
    val track = playbackState.currentTrack
    val hasTrack = track != null
    val isLocalTrack = remember(track) {
        track?.let {
            it.id < 0 || it.playableUrl?.let { url ->
                url.startsWith("content:") || url.startsWith("file:") || url.startsWith("/")
            } == true
        } == true
    }
    val isStarred = track?.id?.let { it in likedTrackIds } == true
    val isExplicit = remember(track?.id, track?.title, parsedLyrics) {
        isExplicitTrack(track, parsedLyrics)
    }

    var currentPage by rememberSaveable { mutableStateOf(NativePlayerPage.ALBUM) }
    var showQualitySheet by rememberSaveable { mutableStateOf(false) }
    var showQueueSheet by rememberSaveable { mutableStateOf(false) }
    var showMoreSheet by rememberSaveable { mutableStateOf(false) }

    // 截图 3 沉浸态控制栏显隐状态（无操作播放 4.8s 自动收起控制栏，点击或滑动立即唤醒）
    var lyricsControlsVisible by rememberSaveable { mutableStateOf(true) }
    var interactionTick by remember { mutableLongStateOf(0L) }
    val notifyUserActivity = remember {
        {
            interactionTick++
            if (!lyricsControlsVisible) {
                lyricsControlsVisible = true
            }
        }
    }

    LaunchedEffect(hasTrack) {
        if (!hasTrack) {
            currentPage = NativePlayerPage.ALBUM
            lyricsControlsVisible = true
        }
    }

    LaunchedEffect(currentPage) {
        if (currentPage == NativePlayerPage.ALBUM) {
            lyricsControlsVisible = true
        } else {
            interactionTick++
        }
    }

    val anySheetOpen = showQualitySheet || showQueueSheet || showMoreSheet
    LaunchedEffect(
        currentPage,
        isPlaying,
        lyricsControlsVisible,
        anySheetOpen,
        parsedLyrics.lines.isNotEmpty(),
        interactionTick
    ) {
        if (currentPage == NativePlayerPage.LYRIC &&
            isPlaying &&
            lyricsControlsVisible &&
            !anySheetOpen &&
            parsedLyrics.lines.isNotEmpty()
        ) {
            delay(4800L)
            lyricsControlsVisible = false
        }
    }

    // 在歌词页按系统返回键优先顺滑退回专辑大封面页；在专辑页按返回交由上层关闭播放器
    BackHandler(enabled = currentPage == NativePlayerPage.LYRIC && !anySheetOpen) {
        currentPage = NativePlayerPage.ALBUM
    }

    // 折叠屏与大屏分栏自适应
    val moreSheet: @Composable () -> Unit = {
        if (showMoreSheet) {
            AppleMusicMoreActionsSheet(
                track = track,
                isStarred = isStarred,
                playbackMode = playbackState.playbackMode,
                currentQuality = currentAudioQuality,
                isLocalTrack = isLocalTrack,
                parsedLyrics = parsedLyrics,
                queueSource = playbackState.queueSource,
                onToggleStar = { track?.let { onToggleStar(it.id) } },
                onSelectPlaybackMode = { mode -> onSetMode(mode) },
                onOpenQuality = {
                    showMoreSheet = false
                    if (!isLocalTrack) showQualitySheet = true
                },
                onOpenQueue = {
                    showMoreSheet = false
                    showQueueSheet = true
                },
                onDismiss = { showMoreSheet = false }
            )
        }
    }

    val adaptiveWindow = LocalAdaptiveWindow.current
    if (adaptiveWindow.useExpandedPlayer) {
        ExpandedNativePlayerContent(
            state = playbackState,
            lyrics = parsedLyrics,
            quality = currentAudioQuality,
            onClose = onClose,
            artworkAlpha = artworkAlpha,
            onArtworkBoundsChanged = onArtworkBoundsChanged,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            onSeek = onSeek,
            onQuality = { if (hasTrack) showQualitySheet = true },
            onQueue = { showQueueSheet = true },
            isLyricsLoading = isLyricsLoading,
            likedTrackIds = likedTrackIds,
            onToggleStar = onToggleStar,
            onToggleMode = onToggleMode,
            modifier = modifier,
            isLyricsPage = currentPage == NativePlayerPage.LYRIC,
            onToggleLyrics = { currentPage = if (currentPage == NativePlayerPage.LYRIC) NativePlayerPage.ALBUM else NativePlayerPage.LYRIC },
            onMore = { showMoreSheet = true }
        )
        if (showQueueSheet) {
            PlaylistQueueSheet(
                playbackState = playbackState,
                onTrackClick = onQueueTrack,
                onRemoveTrack = onRemoveTrack,
                onClearQueue = onClearQueue,
                onTogglePlaybackMode = onToggleMode,
                onDismiss = { showQueueSheet = false }
            )
        }
        if (showQualitySheet) {
            AudioQualitySelectionSheet(
                currentQuality = currentAudioQuality,
                onSelectQuality = onQuality,
                onDismiss = { showQualitySheet = false }
            )
        }
        moreSheet()
        return
    }

    val activeLineIndex = remember(parsedLyrics, currentPosition) {
        if (parsedLyrics.lines.isEmpty()) {
            -1
        } else {
            parsedLyrics.lines.indexOfLast { it.startTime <= currentPosition }.coerceAtLeast(0)
        }
    }

    // ── ALBUM <-> LYRIC 连续可逆弹簧进度（参照 MeiloX-main lyricAnimFraction）──
    val lyricAnimFraction = remember {
        Animatable(if (currentPage == NativePlayerPage.LYRIC) 1f else 0f)
    }
    LaunchedEffect(currentPage) {
        lyricAnimFraction.animateTo(
            targetValue = if (currentPage == NativePlayerPage.LYRIC) 1f else 0f,
            animationSpec = appleMusicLyricSpring()
        )
    }

    val playbackScale = rememberAppleMusicPlaybackScale(isPlaying = isPlaying)
    val albumInfoAlpha = remember { derivedStateOf { appleMusicAlbumInfoAlpha(lyricAnimFraction.value) } }
    val headerInfoAlpha = remember { derivedStateOf { appleMusicHeaderInfoAlpha(lyricAnimFraction.value) } }
    val lyricsLayerAlpha = remember { derivedStateOf { appleMusicLyricsAlpha(lyricAnimFraction.value) } }

    // 独立预览/测试模式（未注入外层容器拖拽回调时）仅响应顶部小白条的本地下拉回弹
    val fallbackScope = rememberCoroutineScope()
    val fallbackOffsetY = remember { Animatable(0f) }

    val density = LocalDensity.current
    var rootOriginInWindow by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize().testTag("player_stacked")
            .semantics { stateDescription = if (currentPage == NativePlayerPage.LYRIC) "\u6b4c\u8bcd" else "\u4e13\u8f91" }
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                if (rootOriginInWindow != pos) {
                    rootOriginInWindow = pos
                }
            }
            .offset {
                IntOffset(0, fallbackOffsetY.value.roundToInt())
            }
    ) {
        val viewportWidthDp = maxWidth
        val maxWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val maxHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        val statusBarTopPx = WindowInsets.statusBars.getTop(density).toFloat()
        val navBarBottomPx = WindowInsets.navigationBars.getBottom(density).toFloat()
        val statusBarTopDp = with(density) { statusBarTopPx.toDp() }
        val navBarBottomDp = with(density) { navBarBottomPx.toDp() }

        var measuredControlsHeightPx by remember {
            mutableIntStateOf(with(density) { 232.dp.roundToPx() })
        }

        val contentWidthDp = maxWidth.coerceAtMost(680.dp)
        val horizontalPaddingDp = (maxWidth - contentWidthDp) / 2 + 28.dp
        val horizontalPaddingPx = with(density) { horizontalPaddingDp.toPx() }
        val grabberRegionHeightPx = with(density) { 36.dp.toPx() }

        // 专辑页歌曲信息行位置（位于底部控制区上方 18dp）
        val albumInfoHeightPx = with(density) { 56.dp.toPx() }
        val albumInfoBottomGapPx = with(density) { 18.dp.toPx() }
        val controlsBottomPaddingPx = navBarBottomPx + with(density) { 4.dp.toPx() }
        val controlsTotalOccupiedPx = measuredControlsHeightPx + controlsBottomPaddingPx
        val lyricsControlsViewportBottomPx = controlsTotalOccupiedPx + with(density) { 10.dp.toPx() }
        val albumInfoTopPx = (maxHeightPx - controlsTotalOccupiedPx - albumInfoBottomGapPx - albumInfoHeightPx)
            .coerceAtLeast(statusBarTopPx + grabberRegionHeightPx + with(density) { 120.dp.toPx() })

        // 截图 1 大封面几何边界（左右 28dp 边距，在顶栏与曲目信息之间垂直居中）
        val coverBandTopPx = statusBarTopPx + grabberRegionHeightPx + with(density) { 8.dp.toPx() }
        val coverBandBottomPx = (albumInfoTopPx - with(density) { 22.dp.toPx() }).coerceAtLeast(coverBandTopPx + 1f)
        val maxCoverSizePx = min(
            (maxWidthPx - horizontalPaddingPx * 2f).coerceAtLeast(1f),
            (coverBandBottomPx - coverBandTopPx).coerceAtLeast(1f)
        )
        val normalCoverLeftPx = (maxWidthPx - maxCoverSizePx) * 0.5f
        val normalCoverTopPx = coverBandTopPx + ((coverBandBottomPx - coverBandTopPx - maxCoverSizePx) * 0.5f).coerceAtLeast(0f)
        val normalBounds = Rect(
            left = normalCoverLeftPx,
            top = normalCoverTopPx,
            right = normalCoverLeftPx + maxCoverSizePx,
            bottom = normalCoverTopPx + maxCoverSizePx
        )

        // 截图 2 / 3 / 4 歌词页顶部 54dp 小封面几何边界
        val headerCoverSizeDp = 54.dp
        val headerCoverSizePx = with(density) { headerCoverSizeDp.toPx() }
        val headerTopPx = statusBarTopPx + grabberRegionHeightPx + with(density) { 2.dp.toPx() }
        val headerBounds = Rect(
            left = horizontalPaddingPx,
            top = headerTopPx,
            right = horizontalPaddingPx + headerCoverSizePx,
            bottom = headerTopPx + headerCoverSizePx
        )

        val normalRadiusPx = with(density) { 14.dp.toPx() }
        val headerRadiusPx = with(density) { 8.dp.toPx() }
        val normalShadowPx = with(density) { 26.dp.toPx() }

        val liveArtworkFrame = {
            appleMusicArtworkFrame(
                normalBounds = normalBounds,
                headerBounds = headerBounds,
                normalRadius = normalRadiusPx,
                headerRadius = headerRadiusPx,
                normalShadow = normalShadowPx,
                playbackScale = playbackScale.value,
                progress = lyricAnimFraction.value
            )
        }

        // 向外层 MiniPlayer <-> FullPlayer 双向共享封面报告精确终点几何信息
        val settledTargetProgress = if (currentPage == NativePlayerPage.LYRIC) 1f else 0f
        val settledTargetScale = if (isPlaying) 1f else 0.78f
        val settledTargetFrame = appleMusicArtworkFrame(
            normalBounds = normalBounds,
            headerBounds = headerBounds,
            normalRadius = normalRadiusPx,
            headerRadius = headerRadiusPx,
            normalShadow = normalShadowPx,
            playbackScale = settledTargetScale,
            progress = settledTargetProgress
        )
        val settledRootBounds = settledTargetFrame.bounds.translate(rootOriginInWindow)
        val targetCornerDp = if (currentPage == NativePlayerPage.LYRIC) 8.dp else 14.dp
        val targetShadowDp = if (currentPage == NativePlayerPage.LYRIC) 0.dp else (26.dp * settledTargetScale)
        val trackContainerTop = currentPage == NativePlayerPage.LYRIC || lyricAnimFraction.value > 0.5f

        SideEffect {
            if (settledRootBounds.width > 1f && settledRootBounds.height > 1f) {
                onArtworkBoundsChanged(settledRootBounds)
                onArtworkCornerRadiusChanged(targetCornerDp)
                onArtworkShadowChanged(targetShadowDp)
                onArtworkTrackTopChanged(trackContainerTop)
            }
        }

        val controlsShown = currentPage != NativePlayerPage.LYRIC || lyricsControlsVisible
        val lyricsBottomInsetDp by animateDpAsState(
            targetValue = if (controlsShown) {
                with(density) { lyricsControlsViewportBottomPx.toDp() }
            } else {
                navBarBottomDp + 10.dp
            },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = if (controlsShown) 380f else 190f
            ),
            label = "lyricsBottomInset"
        )

        // ── Layer 0: 1:1 AMLL 流体网格封面取色背景 ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = backgroundAlpha?.value ?: 1f
                }
        ) {
            if (LocalPlayerBackdrop.current == null) PlayerBackdrop(track, isPlaying = isPlaying)
        }

        // ── Layer 1: 前景交互内容层（随外层容器展开/收起渐变）──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val alphaVal = contentAlpha?.value ?: 1f
                    alpha = alphaVal
                    compositingStrategy = if (alphaVal < 0.999f) {
                        CompositingStrategy.ModulateAlpha
                    } else {
                        CompositingStrategy.Auto
                    }
                }
        ) {
            // ── 1. 歌词视口层（截图 2 / 3 / 4）：外层视口固定驻留在顶部小封面与底部控制栏之间并严格裁切遮挡，
            //       仅内部歌词内容执行 translationY 上滑/下滑过渡，彻底杜绝歌词穿过下方控制栏 ──
            val lyricViewportTopPx = headerTopPx + headerCoverSizePx + with(density) { 4.dp.toPx() }
            val lyricViewportTopDp = with(density) { lyricViewportTopPx.toDp() }
            val lyricEntryOffsetY = {
                appleMusicLyricsTranslationY(
                    progress = lyricAnimFraction.value,
                    albumInformationBottom = albumInfoTopPx + albumInfoHeightPx,
                    lyricViewportTop = lyricViewportTopPx
                )
            }
            if (currentPage == NativePlayerPage.LYRIC || lyricsLayerAlpha.value > 0.001f) {
                Box(
                    modifier = Modifier
                        .zIndex(0f)
                        .fillMaxSize()
                        .padding(horizontal = horizontalPaddingDp - 28.dp)
                        .padding(top = lyricViewportTopDp, bottom = lyricsBottomInsetDp)
                        .clipToBounds()
                        .graphicsLayer {
                            alpha = lyricsLayerAlpha.value
                        }
                        .then(
                            if (currentPage == NativePlayerPage.LYRIC) Modifier
                            else Modifier.clearAndSetSemantics { }
                        )
                ) {
                    when {
                        parsedLyrics.lines.isNotEmpty() -> {
                            LyricsColumn(
                                lyrics = parsedLyrics,
                                activeLineIndex = activeLineIndex,
                                currentTimeMs = currentPosition,
                                isPlaying = isPlaying,
                                onLineClick = { seekMs ->
                                    notifyUserActivity()
                                    onSeek(seekMs)
                                },
                                controlsVisible = controlsShown,
                                onToggleControls = {
                                    interactionTick++
                                    lyricsControlsVisible = !lyricsControlsVisible
                                },
                                onUserActivity = notifyUserActivity,
                                entryTranslationY = lyricEntryOffsetY,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        isLyricsLoading && hasTrack -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { translationY = lyricEntryOffsetY() },
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White.copy(alpha = 0.78f),
                                    strokeWidth = 2.8.dp,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { translationY = lyricEntryOffsetY() }
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            interactionTick++
                                            lyricsControlsVisible = !lyricsControlsVisible
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (hasTrack) "暂无歌词" else "未在播放",
                                    color = Color.White.copy(alpha = 0.52f),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // ── 2. 截图 1 专辑页曲目信息与收藏/更多按钮（随切页平滑上移淡出）──
            val albumTextOrigin = Offset(horizontalPaddingPx, albumInfoTopPx)
            val headerTextLeftPx = horizontalPaddingPx + headerCoverSizePx + with(density) { 14.dp.toPx() }
            val headerTextOrigin = Offset(headerTextLeftPx, headerTopPx)

            if (albumInfoAlpha.value > 0.001f) {
                Row(
                    modifier = Modifier
                        .zIndex(1f)
                        .align(AbsoluteAlignment.TopLeft)
                        .absoluteOffset {
                            IntOffset(horizontalPaddingPx.roundToInt(), albumInfoTopPx.roundToInt())
                        }
                        .width(viewportWidthDp - horizontalPaddingDp * 2)
                        .height(56.dp)
                        .graphicsLayer {
                            val offset = appleMusicInfoOffset(
                                albumOrigin = albumTextOrigin,
                                headerOrigin = headerTextOrigin,
                                progress = lyricAnimFraction.value,
                                fromAlbum = true
                            )
                            translationY = offset.y * 0.35f
                            alpha = albumInfoAlpha.value
                        }
                        .then(
                            if (currentPage == NativePlayerPage.ALBUM) Modifier
                            else Modifier.clearAndSetSemantics { }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = track?.title ?: "未在播放",
                                color = Color.White,
                                fontSize = 21.sp,
                                lineHeight = 26.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isExplicit) {
                                Spacer(modifier = Modifier.width(6.dp))
                                AppleMusicExplicitBadge(size = 15.5.dp)
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track?.artists?.joinToString(" / ") { it.name }
                                ?.ifBlank { "未知艺术家" }
                                ?: "请选择曲目播放",
                            color = Color.White.copy(alpha = 0.62f),
                            fontSize = 17.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (track != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = currentPage == NativePlayerPage.ALBUM,
                                    onClick = { onToggleStar(track.id) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AppleMusicHeartIcon(
                                isLiked = isStarred,
                                size = 24.dp,
                                tint = Color.White.copy(alpha = 0.92f)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = currentPage == NativePlayerPage.ALBUM,
                                    onClick = { showMoreSheet = true }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AppleMusicMoreIcon(
                                size = 24.dp,
                                tint = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }
                }
            }

            // ── 3. 截图 2 / 3 / 4 歌词页顶部紧凑 Header 信息与收藏/更多按钮 ──
            if (headerInfoAlpha.value > 0.001f) {
                val headerTextWidthDp = viewportWidthDp - with(density) { headerTextLeftPx.toDp() } - horizontalPaddingDp
                Row(
                    modifier = Modifier
                        .zIndex(1f)
                        .align(AbsoluteAlignment.TopLeft)
                        .absoluteOffset {
                            IntOffset(headerTextLeftPx.roundToInt(), headerTopPx.roundToInt())
                        }
                        .width(headerTextWidthDp.coerceAtLeast(1.dp))
                        .height(headerCoverSizeDp)
                        .graphicsLayer {
                            alpha = headerInfoAlpha.value
                            translationY = (1f - lyricAnimFraction.value) * with(density) { 18.dp.toPx() }
                        }
                        .then(
                            if (currentPage == NativePlayerPage.LYRIC) Modifier
                            else Modifier.clearAndSetSemantics { }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = currentPage == NativePlayerPage.LYRIC,
                                onClick = {
                                    notifyUserActivity()
                                    currentPage = NativePlayerPage.ALBUM
                                }
                            )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = track?.title ?: "未在播放",
                                color = Color.White,
                                fontSize = 16.5.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isExplicit) {
                                Spacer(modifier = Modifier.width(6.dp))
                                AppleMusicExplicitBadge(size = 14.5.dp)
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = track?.artists?.joinToString(" / ") { it.name }
                                ?.ifBlank { "未知艺术家" }
                                ?: "",
                            color = Color.White.copy(alpha = 0.62f),
                            fontSize = 14.5.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (track != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = currentPage == NativePlayerPage.LYRIC,
                                    onClick = {
                                        notifyUserActivity()
                                        onToggleStar(track.id)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AppleMusicHeartIcon(
                                isLiked = isStarred,
                                size = 23.dp,
                                tint = Color.White.copy(alpha = 0.92f)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = currentPage == NativePlayerPage.LYRIC,
                                    onClick = {
                                        notifyUserActivity()
                                        showMoreSheet = true
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AppleMusicMoreIcon(
                                size = 23.dp,
                                tint = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }
                }
            }

            // ── 4. 底部统一播放控制栏（置顶遮挡下边界，截图 1 / 2 / 4 显示，截图 3 沉浸态下顺滑收起隐藏）──
            Column(
                modifier = Modifier
                    .zIndex(2f)
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 680.dp).fillMaxWidth()
                    .padding(bottom = navBarBottomDp + 4.dp)
            ) {
                AnimatedVisibility(
                    visible = controlsShown,
                    enter = fadeIn(tween(220)) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = 380f
                        ),
                        initialOffsetY = { it }
                    ),
                    exit = fadeOut(tween(220)) + slideOutVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = 190f
                        ),
                        targetOffsetY = { it }
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth().testTag("player_controls")
                            .onSizeChanged { sz ->
                                if (sz.height > 0) {
                                    measuredControlsHeightPx = sz.height
                                }
                            }
                            .pointerInput(Unit) {
                                // 监听任意按下事件以重置歌词页沉浸倒计时，但不消费子控件手势
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.changes.any { it.pressed }) {
                                            interactionTick++
                                        }
                                    }
                                }
                            }
                    ) {
                        AppleMusicPlaybackControls(
                            currentPositionMs = currentPosition,
                            durationMs = playbackState.durationMs,
                            isPlaying = isPlaying,
                            isLyricsPage = currentPage == NativePlayerPage.LYRIC,
                            hasTrack = hasTrack,
                            currentAudioQuality = currentAudioQuality,
                            isLocalTrack = isLocalTrack,
                            playbackMode = playbackState.playbackMode,
                            onAudioQualityClick = {
                                notifyUserActivity()
                                if (hasTrack && !isLocalTrack) {
                                    showQualitySheet = true
                                }
                            },
                            onPlayPause = {
                                notifyUserActivity()
                                onPlayPause()
                            },
                            onNext = {
                                notifyUserActivity()
                                onNext()
                            },
                            onPrevious = {
                                notifyUserActivity()
                                onPrevious()
                            },
                            onSeekTo = { posMs ->
                                notifyUserActivity()
                                onSeek(posMs)
                            },
                            onToggleLyrics = {
                                notifyUserActivity()
                                currentPage = if (currentPage == NativePlayerPage.LYRIC) {
                                    NativePlayerPage.ALBUM
                                } else {
                                    NativePlayerPage.LYRIC
                                }
                            },
                            onTogglePlaybackMode = {
                                notifyUserActivity()
                                onToggleMode()
                            },
                            onQueueClick = {
                                notifyUserActivity()
                                showQueueSheet = true
                            }
                        )
                    }
                }
            }

            // ── 5. 顶部唯一下滑小白条（严格限制仅顶部小白条区域可下拉收起/点击收起，底部与页面主体绝不误触下拉！）──
            Box(
                modifier = Modifier
                    .zIndex(3f)
                    .align(Alignment.TopCenter)
                    .padding(top = statusBarTopDp + 2.dp)
                    .size(width = 124.dp, height = 34.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    )
                    .pointerInput(onGrabberDragStart, onGrabberDragDelta, onGrabberDragEnd) {
                        val velocityTracker = VelocityTracker()
                        var localTotalDrag = 0f
                        detectVerticalDragGestures(
                            onDragStart = {
                                velocityTracker.resetTracking()
                                localTotalDrag = 0f
                                onGrabberDragStart?.invoke()
                            },
                            onVerticalDrag = { change, dragAmount ->
                                velocityTracker.addPointerInputChange(change)
                                change.consume()
                                if (onGrabberDragDelta != null) {
                                    onGrabberDragDelta(dragAmount)
                                } else {
                                    localTotalDrag = (localTotalDrag + dragAmount).coerceAtLeast(0f)
                                    fallbackScope.launch {
                                        fallbackOffsetY.snapTo(localTotalDrag)
                                    }
                                }
                            },
                            onDragCancel = {
                                if (onGrabberDragEnd != null) {
                                    onGrabberDragEnd(0f)
                                } else {
                                    fallbackScope.launch {
                                        fallbackOffsetY.animateTo(0f, spring(stiffness = 500f))
                                    }
                                }
                            },
                            onDragEnd = {
                                val velocityY = velocityTracker.calculateVelocity().y
                                if (onGrabberDragEnd != null) {
                                    onGrabberDragEnd(velocityY)
                                } else {
                                    val dismissThresholdPx = 120.dp.toPx()
                                    if (localTotalDrag > dismissThresholdPx || velocityY > 900.dp.toPx()) {
                                        onClose()
                                    } else {
                                        fallbackScope.launch {
                                            fallbackOffsetY.animateTo(0f, spring(stiffness = 500f))
                                        }
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // 截图 1 / 2 / 3 / 4 顶部半透明胶囊横条
                Box(
                    modifier = Modifier
                        .size(width = 42.dp, height = 4.5.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.34f),
                            shape = RoundedCornerShape(2.25.dp)
                        )
                )
            }
        }

        // ── Layer 2: 常驻共享封面层（ALBUM 大封面 <-> LYRIC 顶栏 54dp 小封面 连续插值，并与外层 MiniPlayer 无缝交接）──
        val sharedArtwork = LocalPlayerArtwork.current
        val layoutCoverSizeDp = with(density) { maxCoverSizePx.roundToInt().coerceAtLeast(1).toDp() }
        Box(
            modifier = Modifier
                .playerArtworkMotion(liveArtworkFrame)
                .size(layoutCoverSizeDp)
                .graphicsLayer {
                    val frame = liveArtworkFrame()
                    val scale = (frame.bounds.width / size.width.coerceAtLeast(1f)).coerceAtLeast(0.001f)
                    alpha = artworkAlpha.value
                    shadowElevation = frame.shadowElevation / scale
                    ambientShadowColor = Color.Black.copy(alpha = 0.68f)
                    spotShadowColor = Color.Black.copy(alpha = 0.82f)
                    shape = RoundedRectangle(with(density) { (frame.cornerRadius / scale).toDp() })
                    clip = true
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = hasTrack,
                    onClick = {
                        notifyUserActivity()
                        currentPage = if (currentPage == NativePlayerPage.ALBUM) {
                            NativePlayerPage.LYRIC
                        } else {
                            NativePlayerPage.ALBUM
                        }
                    }
                )
        ) {
            if (track != null && !track.coverUrl.isNullOrBlank()) {
                AnimatedContent(
                    targetState = track.coverUrl,
                    transitionSpec = {
                        (fadeIn(tween(340)) + scaleIn(initialScale = 0.94f, animationSpec = tween(340)))
                            .togetherWith(fadeOut(tween(280)))
                    },
                    label = "residentCoverTransition",
                    modifier = Modifier.fillMaxSize()
                ) { coverUrl ->
                    val normalizedUrl = remember(coverUrl) { normalizeHighResCoverUrl(coverUrl) }
                    val painter = sharedArtwork?.takeIf { it.first == normalizedUrl }?.second
                    if (painter != null) {
                        Image(
                            painter = painter,
                            contentDescription = "专辑封面",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = rememberHighResArtworkRequest(coverUrl),
                            contentDescription = "专辑封面",
                            contentScale = ContentScale.Crop,
                            filterQuality = FilterQuality.High,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "未在播放",
                        tint = Color.White.copy(alpha = 0.36f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }

        // 待播清单弹窗
        if (showQueueSheet) {
            PlaylistQueueSheet(
                playbackState = playbackState,
                onTrackClick = { index -> onQueueTrack(index) },
                onRemoveTrack = { index -> onRemoveTrack(index) },
                onClearQueue = { onClearQueue() },
                onTogglePlaybackMode = { onToggleMode() },
                onDismiss = { showQueueSheet = false }
            )
        }

        // 音质切换弹窗（从进度条下方中间 Dolby Atmos 槽位触发）
        if (showQualitySheet) {
            AudioQualitySelectionSheet(
                currentQuality = currentAudioQuality,
                onSelectQuality = { quality -> onQuality(quality) },
                onDismiss = { showQualitySheet = false }
            )
        }

        // 更多操作弹窗（从右侧 ••• 按钮触发）
        moreSheet()
    }
}

/**
 * Apple Music 风格更多菜单（••• 按钮弹出：曲目收藏、播放模式切换、音质与待播入口、可下拉展开的音频详细信息）
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AppleMusicMoreActionsSheet(
    track: Track?,
    isStarred: Boolean,
    playbackMode: PlaybackMode,
    currentQuality: OnlineAudioQuality,
    isLocalTrack: Boolean,
    parsedLyrics: ParsedLyrics = ParsedLyrics(emptyList()),
    queueSource: String? = null,
    onToggleStar: () -> Unit,
    onSelectPlaybackMode: (PlaybackMode) -> Unit,
    onOpenQuality: () -> Unit,
    onOpenQueue: () -> Unit,
    onDismiss: () -> Unit
) {
    var isDetailsExpanded by rememberSaveable(track?.id) { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isDetailsExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "audioDetailsChevron"
    )
    val resolvedDetails = rememberResolvedTrackAudioDetails(
        track = track,
        currentQuality = currentQuality,
        isLocalTrack = isLocalTrack,
        parsedLyrics = parsedLyrics,
        queueSource = queueSource
    )

    AdaptiveModalSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .padding(bottom = 28.dp)
        ) {
            if (track != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = track.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF2F2F7))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            color = Color(0xFF1E1E24),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artists.joinToString(" / ") { it.name }.ifBlank { "未知艺术家" },
                            color = Color(0xFF8E8E93),
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onToggleStar),
                        contentAlignment = Alignment.Center
                    ) {
                        AppleMusicHeartIcon(
                            isLiked = isStarred,
                            size = 22.dp,
                            tint = if (isStarred) Color(0xFFFA233B) else Color(0xFF515151)
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color(0xFFF0F0F2)
                )
            }

            Text(
                text = "播放模式",
                color = Color(0xFF8E8E93),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val modes = listOf(
                    Triple(PlaybackMode.SEQUENTIAL, "顺序循环", R.drawable.ic_repeat),
                    Triple(PlaybackMode.SHUFFLE, "随机播放", R.drawable.ic_shuffle)
                )
                modes.forEach { (mode, label, iconRes) ->
                    val selected = playbackMode == mode
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) Color(0xFFFA233B).copy(alpha = 0.12f)
                                else Color(0xFFF5F5F7)
                            )
                            .clickable { onSelectPlaybackMode(mode) }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = label,
                            tint = if (selected) Color(0xFFFA233B) else Color(0xFF515151),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            color = if (selected) Color(0xFFFA233B) else Color(0xFF1E1E24),
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!isLocalTrack) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F7))
                        .clickable(onClick = onOpenQuality)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFF515151),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "切换音频质量",
                        color = Color(0xFF1E1E24),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = currentQuality.badgeText,
                        color = Color(0xFF8E8E93),
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F7))
                    .clickable(onClick = onOpenQueue)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = Color(0xFF515151),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "查看待播清单",
                    color = Color(0xFF1E1E24),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (resolvedDetails != null) {
                Spacer(modifier = Modifier.height(8.dp))
                AudioDetailsExpandableCard(
                    details = resolvedDetails,
                    isExpanded = isDetailsExpanded,
                    chevronRotation = chevronRotation,
                    onToggleExpand = { isDetailsExpanded = !isDetailsExpanded }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudioDetailsExpandableCard(
    details: ResolvedAudioDetails,
    isExpanded: Boolean,
    chevronRotation: Float,
    onToggleExpand: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F7))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = Color(0xFF515151),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "音频详细信息",
                color = Color(0xFF1E1E24),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = details.summaryBadge,
                color = Color(0xFF8E8E93),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) "收起音频详细信息" else "展开音频详细信息",
                tint = Color(0xFF8E8E93),
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = chevronRotation }
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(tween(180)),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeOut(tween(140))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            ) {
                HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AudioSpecTagChip(
                        text = details.sourceTag,
                        backgroundColor = Color(0xFFFA233B).copy(alpha = 0.12f),
                        textColor = Color(0xFFFA233B)
                    )
                    AudioSpecTagChip(
                        text = details.formatTag,
                        backgroundColor = Color(0xFFE9E9EE),
                        textColor = Color(0xFF3A3A3C)
                    )
                    AudioSpecTagChip(
                        text = details.bitrateTag,
                        backgroundColor = Color(0xFFE9E9EE),
                        textColor = Color(0xFF3A3A3C)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val rows = listOf(
                    "音频来源" to details.sourceText,
                    "音质规格" to details.qualityText,
                    "编码格式" to details.formatText,
                    "音频码率" to details.bitrateText,
                    "采样率" to details.sampleRateText,
                    "文件大小" to details.fileSizeText,
                    "歌词来源" to details.lyricSourceText,
                    "所属专辑" to details.albumText,
                    details.identifierLabel to details.identifierValue
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { (label, value) ->
                        AudioDetailRow(label = label, value = value)
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioSpecTagChip(
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun AudioDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = Color(0xFF8E8E93),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.width(76.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            color = Color(0xFF1E1E24),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

private data class LocalAudioProbeResult(
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val fileSizeBytes: Long? = null,
    val format: String? = null
)

internal data class ResolvedAudioDetails(
    val summaryBadge: String,
    val sourceTag: String,
    val formatTag: String,
    val bitrateTag: String,
    val sourceText: String,
    val qualityText: String,
    val formatText: String,
    val bitrateText: String,
    val sampleRateText: String,
    val fileSizeText: String,
    val lyricSourceText: String,
    val albumText: String,
    val identifierLabel: String,
    val identifierValue: String
)

@Composable
private fun rememberResolvedTrackAudioDetails(
    track: Track?,
    currentQuality: OnlineAudioQuality,
    isLocalTrack: Boolean,
    parsedLyrics: ParsedLyrics,
    queueSource: String?
): ResolvedAudioDetails? {
    if (track == null) return null

    val context = LocalContext.current
    var probedLocal by remember(track.id, track.playableUrl) {
        mutableStateOf<LocalAudioProbeResult?>(null)
    }

    LaunchedEffect(track.id, track.playableUrl, isLocalTrack) {
        val url = track.playableUrl
        val needsProbe = isLocalTrack && !url.isNullOrBlank() && (
            track.audioBitrateKbps == null ||
                track.audioSampleRateHz == null ||
                track.audioFileSizeBytes == null ||
                track.audioFormat == null
            )
        if (needsProbe) {
            probedLocal = probeLocalAudioMetadata(context, url, track.durationMs)
        } else {
            probedLocal = null
        }
    }

    return remember(track, currentQuality, isLocalTrack, parsedLyrics.source, queueSource, probedLocal) {
        resolveTrackAudioDetails(
            track = track,
            currentQuality = currentQuality,
            isLocalTrack = isLocalTrack,
            parsedLyrics = parsedLyrics,
            queueSource = queueSource,
            probedBitrateKbps = probedLocal?.bitrateKbps,
            probedSampleRateHz = probedLocal?.sampleRateHz,
            probedFileSizeBytes = probedLocal?.fileSizeBytes,
            probedFormat = probedLocal?.format
        )
    }
}

internal fun resolveTrackAudioDetails(
    track: Track,
    currentQuality: OnlineAudioQuality,
    isLocalTrack: Boolean,
    parsedLyrics: ParsedLyrics,
    queueSource: String?,
    probedBitrateKbps: Int? = null,
    probedSampleRateHz: Int? = null,
    probedFileSizeBytes: Long? = null,
    probedFormat: String? = null
): ResolvedAudioDetails {
    val rawUrl = track.playableUrl
    val effectiveQualityLevel = if (isLocalTrack) {
        "local"
    } else {
        track.audioQualityLevel?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.isNotBlank() }
            ?: currentQuality.levelKey
    }

    val actualFormat = track.audioFormat?.trim()?.takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT)
        ?: probedFormat?.trim()?.takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT)
        ?: extractAudioFormatFromPathOrUrl(rawUrl)

    val actualFileSizeBytes = track.audioFileSizeBytes?.takeIf { it > 0L }
        ?: probedFileSizeBytes?.takeIf { it > 0L }
    val durationMs = track.durationMs?.takeIf { it > 0L }

    val actualBitrateKbps = track.audioBitrateKbps?.takeIf { it > 0 }
        ?: probedBitrateKbps?.takeIf { it > 0 }
        ?: if (actualFileSizeBytes != null && durationMs != null) {
            ((actualFileSizeBytes * 8L) / durationMs).toInt().takeIf { it > 0 }
        } else {
            null
        }

    val actualSampleRateHz = track.audioSampleRateHz?.takeIf { it > 0 }
        ?: probedSampleRateHz?.takeIf { it > 0 }

    val sourceText = when {
        !track.audioSourceLabel.isNullOrBlank() -> {
            if (!isLocalTrack &&
                queueSource == MusicPlayerController.QUEUE_SOURCE_PRIVATE_DJ &&
                track.audioSourceLabel == "网易云音乐 · 在线流媒体"
            ) {
                "网易云音乐 · 私人 DJ 漫游"
            } else {
                track.audioSourceLabel
            }
        }
        isLocalTrack -> "本地音乐 · 设备存储"
        queueSource == MusicPlayerController.QUEUE_SOURCE_PRIVATE_DJ -> "网易云音乐 · 私人 DJ 漫游"
        else -> "网易云音乐 · 在线流媒体"
    }

    val sourceTag = when {
        sourceText.contains("智能匹配") -> "本地省流"
        isLocalTrack -> "本地音源"
        queueSource == MusicPlayerController.QUEUE_SOURCE_PRIVATE_DJ -> "私人 DJ"
        else -> "在线流媒体"
    }

    val qualityText = when (effectiveQualityLevel) {
        "local" -> if (actualFormat in LosslessAudioFormats) "本地无损音频" else "本地原盘音频"
        "hires", "jymaster" -> "Hi-Res 超清母带"
        "lossless" -> "SQ 无损音质"
        "sky" -> "Dolby Atmos 杜比全景声"
        "exhigh" -> "极高音质 (HQ)"
        "standard" -> "标准音质"
        else -> currentQuality.title
    }

    val formatText = when (actualFormat) {
        "M4A" -> "AAC / M4A"
        null -> when (effectiveQualityLevel) {
            "hires", "jymaster", "lossless" -> "FLAC (预设)"
            "sky" -> "Dolby Atmos / E-AC-3"
            "exhigh", "standard" -> "MP3 / AAC"
            else -> "未知格式"
        }
        else -> actualFormat
    }

    val formatTag = actualFormat ?: when (effectiveQualityLevel) {
        "hires", "jymaster", "lossless" -> "FLAC"
        "sky" -> "ATMOS"
        "exhigh", "standard" -> "MP3/AAC"
        else -> "AUDIO"
    }

    val bitrateText = when {
        actualBitrateKbps != null -> "$actualBitrateKbps kbps"
        effectiveQualityLevel == "hires" || effectiveQualityLevel == "jymaster" -> "1411+ kbps (预设)"
        effectiveQualityLevel == "lossless" -> "约 900-1411 kbps"
        effectiveQualityLevel == "sky" -> "约 768 kbps (Dolby)"
        effectiveQualityLevel == "exhigh" -> "320 kbps (预设)"
        effectiveQualityLevel == "standard" -> "128 kbps (预设)"
        else -> "待播放解析"
    }

    val bitrateTag = when {
        actualBitrateKbps != null -> "$actualBitrateKbps kbps"
        effectiveQualityLevel == "hires" || effectiveQualityLevel == "jymaster" -> "Hi-Res"
        effectiveQualityLevel == "lossless" -> "Lossless"
        effectiveQualityLevel == "sky" -> "Dolby"
        effectiveQualityLevel == "exhigh" -> "320 kbps"
        effectiveQualityLevel == "standard" -> "128 kbps"
        else -> "本地文件"
    }

    val sampleRateText = when {
        actualSampleRateHz != null -> String.format(Locale.US, "%.1f kHz", actualSampleRateHz / 1000.0)
        effectiveQualityLevel == "hires" || effectiveQualityLevel == "jymaster" -> "最高 192.0 kHz"
        effectiveQualityLevel == "lossless" -> "44.1 / 48.0 kHz"
        effectiveQualityLevel == "sky" -> "48.0 kHz"
        effectiveQualityLevel == "exhigh" || effectiveQualityLevel == "standard" -> "44.1 kHz"
        else -> "待播放解析"
    }

    val fileSizeText = when {
        actualFileSizeBytes != null -> formatAudioFileSize(actualFileSizeBytes)
        actualBitrateKbps != null && durationMs != null -> {
            val estBytes = (actualBitrateKbps.toLong() * durationMs) / 8L
            "约 ${formatAudioFileSize(estBytes)}"
        }
        else -> "待起播获取"
    }

    val lyricSourceText = when (parsedLyrics.source) {
        LyricSource.TTML_DB -> "AMLL TTML 逐字词库"
        LyricSource.YRC -> "网易云 YRC 逐字歌词"
        LyricSource.LRC -> "网易云 LRC 歌词"
        LyricSource.NONE -> "暂无歌词"
    }

    val albumText = track.album.title.ifBlank { "未知专辑" }

    val identifierLabel = if (isLocalTrack) "文件路径" else "歌曲 ID"
    val identifierValue = if (isLocalTrack) {
        rawUrl?.takeIf { it.isNotBlank() } ?: "本地存储 (${track.id})"
    } else {
        val host = extractHttpHost(rawUrl)
        if (host != null) "${track.id} · $host" else track.id.toString()
    }

    val summaryBadge = when {
        isLocalTrack && actualFormat != null && actualBitrateKbps != null -> "本地 · $actualFormat · $actualBitrateKbps kbps"
        isLocalTrack && actualFormat != null -> "本地 · $actualFormat"
        isLocalTrack -> "本地音频"
        actualFormat != null && actualBitrateKbps != null -> "$actualFormat · $actualBitrateKbps kbps"
        actualFormat != null -> "$actualFormat · ${currentQuality.badgeText}"
        else -> currentQuality.badgeText
    }

    return ResolvedAudioDetails(
        summaryBadge = summaryBadge,
        sourceTag = sourceTag,
        formatTag = formatTag,
        bitrateTag = bitrateTag,
        sourceText = sourceText,
        qualityText = qualityText,
        formatText = formatText,
        bitrateText = bitrateText,
        sampleRateText = sampleRateText,
        fileSizeText = fileSizeText,
        lyricSourceText = lyricSourceText,
        albumText = albumText,
        identifierLabel = identifierLabel,
        identifierValue = identifierValue
    )
}

private val KnownAudioExtensions = setOf(
    "flac", "mp3", "m4a", "aac", "wav", "ogg", "opus", "ape", "dff", "dsf", "wma", "alac"
)

private val LosslessAudioFormats = setOf(
    "FLAC", "WAV", "APE", "ALAC", "DFF", "DSF"
)

private fun extractAudioFormatFromPathOrUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val pathPart = url.substringBefore('?').substringBefore('#')
    val fileName = pathPart.substringAfterLast('/', "")
    val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return if (ext in KnownAudioExtensions) ext.uppercase(Locale.ROOT) else null
}

private fun extractHttpHost(url: String?): String? {
    if (url.isNullOrBlank()) return null
    if (!url.startsWith("http://") && !url.startsWith("https://")) return null
    val host = url.substringAfter("://", "")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore(':')
        .trim()
    return host.takeIf { it.isNotBlank() }
}

private fun formatAudioFileSize(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

private suspend fun probeLocalAudioMetadata(
    context: Context,
    url: String,
    fallbackDurationMs: Long?
): LocalAudioProbeResult = withContext(Dispatchers.IO) {
    var fileSizeBytes: Long? = null
    var bitrateKbps: Int? = null
    var sampleRateHz: Int? = null
    var format: String? = extractAudioFormatFromPathOrUrl(url)

    try {
        when {
            url.startsWith("content:") -> {
                val uri = Uri.parse(url)
                runCatching {
                    context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                        val col = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (col >= 0 && cursor.moveToFirst()) {
                            fileSizeBytes = cursor.getLong(col).takeIf { it > 0L }
                        }
                    }
                }
                if (fileSizeBytes == null) {
                    runCatching {
                        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                            fileSizeBytes = afd.length.takeIf { it > 0L }
                        }
                    }
                }
            }
            url.startsWith("file:") || url.startsWith("/") -> {
                val file = File(url.removePrefix("file://"))
                if (file.exists()) {
                    fileSizeBytes = file.length().takeIf { it > 0L }
                }
            }
        }

        val retriever = MediaMetadataRetriever()
        var durationMs = fallbackDurationMs
        try {
            if (url.startsWith("content:") || url.startsWith("file:")) {
                retriever.setDataSource(context, Uri.parse(url))
            } else {
                retriever.setDataSource(url)
            }
            val rawBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
            bitrateKbps = rawBitrate?.let { if (it > 10000) (it + 500) / 1000 else it }?.takeIf { it > 0 }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                sampleRateHz = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                    ?.toIntOrNull()?.takeIf { it > 0 }
            }
            if (durationMs == null || durationMs <= 0L) {
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            }
            if (format == null) {
                val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)?.lowercase(Locale.ROOT)
                format = when {
                    mime == null -> null
                    mime.contains("flac") -> "FLAC"
                    mime.contains("mpeg") || mime.contains("mp3") -> "MP3"
                    mime.contains("mp4") || mime.contains("m4a") -> "M4A"
                    mime.contains("aac") -> "AAC"
                    mime.contains("wav") -> "WAV"
                    mime.contains("ogg") -> "OGG"
                    mime.contains("opus") -> "OPUS"
                    else -> null
                }
            }
        } finally {
            runCatching { retriever.release() }
        }

        if (sampleRateHz == null) {
            val extractor = MediaExtractor()
            try {
                if (url.startsWith("content:") || url.startsWith("file:")) {
                    extractor.setDataSource(context, Uri.parse(url), null)
                } else {
                    extractor.setDataSource(url)
                }
                for (i in 0 until extractor.trackCount) {
                    val trackFormat = extractor.getTrackFormat(i)
                    val mime = trackFormat.getString(MediaFormat.KEY_MIME)
                    if (mime?.startsWith("audio/") == true && trackFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRateHz = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).takeIf { it > 0 }
                        if (format == null) {
                            format = when {
                                mime.contains("flac") -> "FLAC"
                                mime.contains("mpeg") -> "MP3"
                                mime.contains("mp4a") -> "AAC"
                                mime.contains("raw") -> "WAV"
                                mime.contains("opus") -> "OPUS"
                                mime.contains("vorbis") -> "OGG"
                                else -> null
                            }
                        }
                        break
                    }
                }
            } finally {
                runCatching { extractor.release() }
            }
        }

        val resolvedSize = fileSizeBytes
        val resolvedDuration = durationMs
        if (bitrateKbps == null && resolvedSize != null && resolvedDuration != null && resolvedDuration > 0L) {
            bitrateKbps = ((resolvedSize * 8L) / resolvedDuration).toInt().takeIf { it > 0 }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        // 探测失败时静默降级至已有元数据
    }

    LocalAudioProbeResult(
        bitrateKbps = bitrateKbps,
        sampleRateHz = sampleRateHz,
        fileSizeBytes = fileSizeBytes,
        format = format
    )
}

/**
 * 音质选择底栏（从进度条正下方 Dolby Atmos 槽位点击弹出）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioQualitySelectionSheet(
    currentQuality: OnlineAudioQuality,
    onSelectQuality: (OnlineAudioQuality) -> Unit,
    onDismiss: () -> Unit
) {
    AdaptiveModalSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C22)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "选择播放音质",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "点击进度条下方音质徽章可随时切换音频格式",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.55f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OnlineAudioQuality.entries.forEach { quality ->
                val isSelected = quality == currentQuality
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable {
                            onSelectQuality(quality)
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (quality == OnlineAudioQuality.DOLBY) quality.title else quality.title + "音质",
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) Color(0xFFFA233B) else Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            when (quality) {
                                OnlineAudioQuality.DOLBY -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White.copy(alpha = 0.14f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Dolby Atmos",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                                OnlineAudioQuality.LOSSLESS, OnlineAudioQuality.HIRES -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFFA233B).copy(alpha = 0.2f))
                                            .padding(horizontal = 5.dp, vertical = 1.5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (quality == OnlineAudioQuality.HIRES) "Hi-Res" else "Lossless",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFA233B)
                                        )
                                    }
                                }
                                else -> Unit
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = quality.desc,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "已选中",
                            tint = Color(0xFFFA233B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
