package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.core.common.toCoverThumbnailUrl
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.core.settings.OnlineAudioQuality
import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics
import com.example.seteasecloudmusic.feature.player.util.LyricParser

/**
 * 播放页原生页面类型：
 * ALBUM: Apple Music 风格大封面专辑页（支持播放/暂停动态弹簧缩放、曲目信息）
 * LYRIC: 歌词滚动页（顶部迷你信息栏，点击切回 ALBUM）
 */
enum class NativePlayerPage {
    ALBUM,
    LYRIC
}

/**
 * 沉浸式模糊背景的降采样策略。
 *
 * 全屏 50dp 高斯模糊是播放页最大的单项 GPU 开销（1080x2460 全分辨率采样）。
 * 这里把背景图按 [BLUR_DOWNSCALE] 分之一直径渲染后再放大铺满：
 * 模糊本身会抹掉高频细节，因此观感几乎无损，而模糊的采样面积降到约 1/[BLUR_DOWNSCALE]²。
 * [BLUR_VISUAL_RADIUS] 是放大到全屏后希望呈现的模糊强度。
 */
private const val BLUR_DOWNSCALE = 6f
private const val BLUR_SOURCE_SIZE = 300

/**
 * AMLL 模式背景：`filter: blur(60px) saturate(180%)` 且封面放大到 140%。
 * 这里保持降采样模糊（见 [BLUR_DOWNSCALE]）以控制 GPU 开销，视觉半径与 WebView 完全一致。
 */
private val BLUR_VISUAL_RADIUS = 60.dp
private const val BLUR_BACKGROUND_SCALE = 1.4f

/** AMLL 背景遮罩：`background-color: rgba(0, 0, 0, 0.35)`。 */
private const val BLUR_SCRIM_ALPHA = 0.35f

/** 饱和度提升：`saturate(180%)`。 */
private const val BLUR_SATURATION = 1.8f

/** 专辑页大封面与歌词页迷你封面共用的共享元素 key。 */
private const val PLAYER_ARTWORK_SHARED_KEY = "player_artwork"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NativePlayerScreen(
    playerViewModel: PlayerViewModel,
    onClose: () -> Unit,
    artworkAlpha: State<Float>,
    onArtworkBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
    val lyricResponseResult by playerViewModel.lyricState.collectAsStateWithLifecycle()
    val currentAudioQuality by playerViewModel.currentAudioQuality.collectAsStateWithLifecycle()

    val currentPosition = playbackState.currentPositionMs
    val isPlaying = playbackState.status == PlayerStatus.PLAYING
    val track = playbackState.currentTrack
    val hasTrack = track != null

    // 默认展示 Apple Music 大封面专辑页
    var currentPage by rememberSaveable { mutableStateOf(NativePlayerPage.ALBUM) }

    // 未在播放时，自动保持在专辑大封面页（展示质感占位封面与提示）
    LaunchedEffect(hasTrack) {
        if (!hasTrack) {
            currentPage = NativePlayerPage.ALBUM
        }
    }

    // 判断当前正在播放的曲目是否为本地音频（包括直链本地文件、SAF ContentUri 及负数 LocalId）
    val isLocalTrack = remember(track?.playableUrl, track?.id) {
        val url = track?.playableUrl ?: ""
        url.startsWith("content://") ||
                url.startsWith("file://") ||
                (url.startsWith("/") && !url.startsWith("http")) ||
                (track?.id != null && track.id < 0)
    }

    var showQualitySheet by remember { mutableStateOf(false) }

    // 解析歌词
    val parsedLyrics: ParsedLyrics = remember(lyricResponseResult) {
        LyricParser.parseLyricResponse(lyricResponseResult?.getOrNull())
    }

    // 计算当前高亮歌词行索引
    val activeLineIndex = remember(parsedLyrics, currentPosition) {
        if (parsedLyrics.lines.isEmpty()) {
            -1
        } else {
            val idx = parsedLyrics.lines.indexOfLast { it.startTime <= currentPosition }
            idx.coerceAtLeast(0)
        }
    }

    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF141416))
            .offset { IntOffset(0, offsetY.toInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY > 160.dp.toPx()) {
                            onClose()
                        } else {
                            offsetY = 0f
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount > 0f || offsetY > 0f) {
                            offsetY = (offsetY + dragAmount).coerceAtLeast(0f)
                        }
                    }
                )
            }
    ) {
        // ── Layer 0: 沉浸式背景（AMLL 规格的专辑封面模糊流光底色）──
        // 模糊背景按 1/BLUR_DOWNSCALE 分辨率渲染、再放大铺满屏幕：
        // 高斯模糊本身会抹掉高频细节，所以缩小渲染几乎不影响观感，
        // 但模糊的采样面积降到约 1/BLUR_DOWNSCALE²，播放页最主要的 GPU 开销随之消失。
        val blurArtworkUrl = remember(track?.coverUrl) {
            track?.coverUrl.toCoverThumbnailUrl(BLUR_SOURCE_SIZE)
        }
        if (!blurArtworkUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = blurArtworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    // saturate(180%)：饱和度提升必须在模糊之前语义上成立，
                    // 这里作为颜色滤镜作用在同一层上，等价于 CSS 的 filter 链
                    colorFilter = ColorFilter.colorMatrix(
                        ColorMatrix().apply { setToSaturation(BLUR_SATURATION) }
                    ),
                    modifier = Modifier
                        .fillMaxSize(BLUR_BACKGROUND_SCALE / BLUR_DOWNSCALE)
                        .graphicsLayer {
                            scaleX = BLUR_DOWNSCALE
                            scaleY = BLUR_DOWNSCALE
                        }
                        .blur(radius = BLUR_VISUAL_RADIUS / BLUR_DOWNSCALE)
                )
            }
        }

        // ── Layer 1: 暗色遮罩（AMLL 均匀 0.35 黑 + 底部加强保证控制栏可读）──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (track != null) {
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = BLUR_SCRIM_ALPHA + 0.10f),
                            0.45f to Color.Black.copy(alpha = BLUR_SCRIM_ALPHA),
                            0.75f to Color.Black.copy(alpha = BLUR_SCRIM_ALPHA + 0.22f),
                            1.0f to Color.Black.copy(alpha = BLUR_SCRIM_ALPHA + 0.45f)
                        )
                    } else {
                        // 未在播放：Apple Music 标志性深空自然环境渐变，杜绝纯黑死板
                        Brush.verticalGradient(
                            0.0f to Color(0xFF26262E),
                            0.45f to Color(0xFF1B1B20),
                            0.85f to Color(0xFF131316),
                            1.0f to Color(0xFF0F0F11)
                        )
                    }
                )
        )

        // ── Layer 2: 核心内容与交互层 ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // 顶部下拉指示条与返回收起按钮
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .height(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .background(
                                Color.White.copy(alpha = 0.45f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = "收起",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // 中部主内容区：Apple Music 专辑页 (ALBUM) 与 歌词页 (LYRIC) 平滑切换。
            // 外层 SharedTransitionLayout 负责"专辑大封面 ↔ 歌词页顶部迷你封面"的共享元素过渡。
            SharedTransitionLayout(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
            Crossfade(
                targetState = currentPage,
                label = "playerPageTransition",
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    NativePlayerPage.ALBUM -> {
                        // ── Apple Music 风格大封面专辑页 ──
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 大封面展示区
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (track != null) {
                                    // 播放中：Apple Music 经典弹性缩放与边缘微淡出
                                    val springSpec = remember {
                                        SpringSpec<Float>(
                                            stiffness = 300f,
                                            dampingRatio = 1f,
                                            visibilityThreshold = 0.001f
                                        )
                                    }
                                    val tweenSpec = remember {
                                        TweenSpec<Float>(
                                            durationMillis = 350,
                                            easing = EaseOutQuart
                                        )
                                    }
                                    val scale by animateFloatAsState(
                                        targetValue = if (isPlaying) 0f else 1f,
                                        animationSpec = if (isPlaying) springSpec else tweenSpec,
                                        visibilityThreshold = 0.001f,
                                        label = "albumScale"
                                    )
                                    // 播放中封面满宽展示；暂停时轻微收窄，形成 Apple Music 式的呼吸感
                                    val sidePad = (16 * scale).dp
                                    // 封面阴影：暂停时贴地、播放时浮起，强化"正在播放"的立体层次
                                    val shadowSpringSpec = remember {
                                        spring<Dp>(stiffness = 300f, dampingRatio = 1f)
                                    }
                                    val shadowTweenSpec = remember {
                                        tween<Dp>(durationMillis = 350, easing = EaseOutQuart)
                                    }
                                    val artworkShadow by animateDpAsState(
                                        targetValue = if (isPlaying) 26.dp else 12.dp,
                                        animationSpec = if (isPlaying) shadowSpringSpec else shadowTweenSpec,
                                        label = "albumShadow"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = sidePad)
                                            .aspectRatio(1f)
                                            // 与歌词页顶部迷你封面共享同一元素：切页时封面平滑缩小飞入/飞回
                                            .sharedElementWithCallerManagedVisibility(
                                                sharedContentState = rememberSharedContentState(
                                                    key = PLAYER_ARTWORK_SHARED_KEY
                                                ),
                                                visible = page == NativePlayerPage.ALBUM
                                            )
                                            // 展开动画依赖此处的 bounds 上报，务必保持；
                                            // 仅在专辑页为当前页时同步，避免切页过渡期间与歌词页迷你封面互相覆盖
                                            .onGloballyPositioned { coordinates ->
                                                if (currentPage == NativePlayerPage.ALBUM) {
                                                    onArtworkBoundsChanged(coordinates.boundsInRoot())
                                                }
                                            }
                                            // 阴影放在 alpha 图层外层：它属于"页面"的一部分，随整页淡入，
                                            // 而封面图本身则由 artworkAlpha 在共享元素落位后接管显现
                                            .shadow(
                                                elevation = artworkShadow,
                                                shape = RoundedCornerShape(16.dp),
                                                ambientColor = Color.Black,
                                                spotColor = Color.Black
                                            )
                                            .clip(RoundedCornerShape(16.dp))
                                            .graphicsLayer {
                                                alpha = artworkAlpha.value
                                                compositingStrategy = CompositingStrategy.Offscreen
                                            }
                                            .drawWithContent {
                                                drawContent()
                                                // 上下边缘柔和淡出，模拟 Apple Music 封面融入背景光影效果
                                                drawRect(
                                                    brush = Brush.verticalGradient(
                                                        0.00f to Color.Black,
                                                        0.05f to Color.Black,
                                                        0.12f to Color.White,
                                                        0.88f to Color.White,
                                                        0.95f to Color.Black,
                                                        1.00f to Color.Black
                                                    ),
                                                    blendMode = BlendMode.DstIn
                                                )
                                            }
                                            .clickable {
                                                if (parsedLyrics.lines.isNotEmpty()) {
                                                    currentPage = NativePlayerPage.LYRIC
                                                }
                                            }
                                    ) {
                                        AsyncImage(
                                            model = track.coverUrl,
                                            contentDescription = "专辑大封面",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                } else {
                                    // 未在播放：Apple Music 经典圆角毛玻璃微质感音符占位封面
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .aspectRatio(1f)
                                            .onGloballyPositioned { coordinates ->
                                                onArtworkBoundsChanged(coordinates.boundsInRoot())
                                            }
                                            .graphicsLayer {
                                                alpha = artworkAlpha.value
                                            }
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(Color.White.copy(alpha = 0.06f))
                                            .border(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(18.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.MusicNote,
                                            contentDescription = "未在播放",
                                            tint = Color.White.copy(alpha = 0.28f),
                                            modifier = Modifier.size(72.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // 歌曲标题、艺术家及操作按钮（Apple Music 专辑页信息区：左对齐标题 + 右侧收藏/更多）
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 2.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track?.title ?: "未在播放",
                                        color = Color.White,
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = track?.artists?.joinToString(" / ") { it.name }
                                            ?.ifBlank { "未知艺术家" }
                                            ?: "从音乐库或搜索中挑选音乐播放",
                                        color = Color.White.copy(alpha = if (track != null) 0.62f else 0.45f),
                                        fontSize = 17.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (track != null) {
                                    IconButton(
                                        onClick = { /* 收藏逻辑 */ },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.FavoriteBorder,
                                            contentDescription = "收藏",
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(25.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { /* 更多操作 */ },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreHoriz,
                                            contentDescription = "更多",
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(25.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    NativePlayerPage.LYRIC -> {
                        // ── Apple Music 风格歌词页 ──
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 顶部迷你封面条（Apple Music 歌词页头部，点击任意处切回专辑大封面页）
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentPage = NativePlayerPage.ALBUM }
                                    .padding(start = 28.dp, end = 20.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = track?.coverUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .sharedElementWithCallerManagedVisibility(
                                            sharedContentState = rememberSharedContentState(
                                                key = PLAYER_ARTWORK_SHARED_KEY
                                            ),
                                            visible = page == NativePlayerPage.LYRIC
                                        )
                                        .clip(RoundedCornerShape(8.dp))
                                        // 从歌词页直接收起/展开播放页时，迷你封面同样由共享元素动画接管显现时机
                                        .graphicsLayer { alpha = artworkAlpha.value }
                                        // 停留在歌词页时，收起播放页的动画会从这个迷你封面位置飞回，
                                        // 因此这里同样要把当前封面位置同步给外层过渡层
                                        .onGloballyPositioned { coordinates ->
                                            onArtworkBoundsChanged(coordinates.boundsInRoot())
                                        },
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track?.title ?: "未在播放",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = track?.artists?.joinToString(" / ") { it.name }
                                            ?.ifBlank { "未知艺术家" } ?: "",
                                        color = Color.White.copy(alpha = 0.62f),
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (track != null) {
                                    IconButton(
                                        onClick = { /* 收藏逻辑 */ },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.FavoriteBorder,
                                            contentDescription = "收藏",
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { /* 更多操作 */ },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreHoriz,
                                            contentDescription = "更多",
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 歌词滚动区域
                            if (parsedLyrics.lines.isNotEmpty()) {
                                LyricsColumn(
                                    lyrics = parsedLyrics,
                                    activeLineIndex = activeLineIndex,
                                    currentTimeMs = currentPosition,
                                    isPlaying = isPlaying,
                                    onLineClick = { seekMs -> playerViewModel.seekTo(seekMs) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else if (lyricResponseResult == null && hasTrack) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.8f))
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (hasTrack) "暂无歌词" else "未在播放",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                    }
            }
        }

        // 底部原生控制栏（整合进度条、音质胶囊、Apple Music 操作栏）
        PlayerControls(
                currentPositionMs = currentPosition,
                durationMs = playbackState.durationMs,
                isPlaying = isPlaying,
                playbackMode = playbackState.playbackMode,
                isLocalTrack = isLocalTrack,
                currentQuality = currentAudioQuality,
                hasTrack = hasTrack,
                isLyricsPage = currentPage == NativePlayerPage.LYRIC,
                onPlayPause = { playerViewModel.onPlayPause() },
                onNext = { playerViewModel.onNext() },
                onPrevious = { playerViewModel.onPrevious() },
                onSeekTo = { posMs -> playerViewModel.seekTo(posMs) },
                onTogglePlaybackMode = { playerViewModel.togglePlaybackMode() },
                onSelectQualityClick = { if (hasTrack) showQualitySheet = true },
                onToggleLyrics = if (hasTrack) {
                    {
                        currentPage = if (currentPage == NativePlayerPage.LYRIC) {
                            NativePlayerPage.ALBUM
                        } else {
                            NativePlayerPage.LYRIC
                        }
                    }
                } else null
            )
        }

        // 音质选择弹窗（纯在线音质切换列表，不包含本地选项）
        if (showQualitySheet) {
            AudioQualitySelectionSheet(
                currentQuality = currentAudioQuality,
                onSelectQuality = { quality ->
                    playerViewModel.selectAudioQuality(quality)
                },
                onDismiss = { showQualitySheet = false }
            )
        }
    }
}

/**
 * 原生质感音质选择底栏（不包含本地选项，在线播放优先匹配本地）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioQualitySelectionSheet(
    currentQuality: OnlineAudioQuality,
    onSelectQuality: (OnlineAudioQuality) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF1E1E24),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                text = "若本地媒体库中存在同名匹配歌曲，将优先播放本地以省流量",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.55f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OnlineAudioQuality.values().forEach { quality ->
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
                                text = quality.title + "音质",
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) Color(0xFFFA233B) else Color.White
                            )
                            if (quality == OnlineAudioQuality.LOSSLESS || quality == OnlineAudioQuality.HIRES) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFFA233B).copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (quality == OnlineAudioQuality.HIRES) "Hi-Res" else "SQ",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFA233B)
                                    )
                                }
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
                            contentDescription = "Selected",
                            tint = Color(0xFFFA233B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
