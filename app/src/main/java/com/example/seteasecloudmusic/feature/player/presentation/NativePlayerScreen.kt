package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
private val BLUR_VISUAL_RADIUS = 50.dp

@Composable
fun NativePlayerScreen(
    playerViewModel: PlayerViewModel,
    onClose: () -> Unit,
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
        // ── Layer 0: 沉浸式背景（专辑封面高斯模糊流光底色）──
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
                    modifier = Modifier
                        .fillMaxSize(1f / BLUR_DOWNSCALE)
                        .graphicsLayer {
                            scaleX = BLUR_DOWNSCALE
                            scaleY = BLUR_DOWNSCALE
                        }
                        .blur(radius = BLUR_VISUAL_RADIUS / BLUR_DOWNSCALE)
                )
            }
        }

        // ── Layer 1: 渐变暗色遮罩层，保证文字与操作清晰易读 ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (track != null) {
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.50f),
                            0.4f to Color.Black.copy(alpha = 0.40f),
                            0.8f to Color.Black.copy(alpha = 0.75f),
                            1.0f to Color.Black.copy(alpha = 0.95f)
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

            // 中部主内容区：Apple Music 专辑页 (ALBUM) 与 歌词页 (LYRIC) 平滑切换
            Crossfade(
                targetState = currentPage,
                label = "playerPageTransition",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = sidePad)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .graphicsLayer {
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

                            Spacer(modifier = Modifier.height(18.dp))

                            // 歌曲标题、艺术家及操作按钮
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track?.title ?: "未在播放",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = track?.artists?.joinToString(" / ") { it.name }
                                            ?.ifBlank { "未知艺术家" }
                                            ?: "从音乐库或搜索中挑选音乐播放",
                                        color = Color.White.copy(alpha = if (track != null) 0.65f else 0.45f),
                                        fontSize = 15.sp,
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
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    NativePlayerPage.LYRIC -> {
                        // ── 歌词滚动页 ──
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 顶部小封面条（点击切换回专辑大封面页）
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentPage = NativePlayerPage.ALBUM }
                                    .padding(horizontal = 28.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = track?.coverUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )

Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track?.title ?: "未在播放",
                                        color = Color.White,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = track?.artists?.joinToString(" / ") { it.name }
                                            ?.ifBlank { "未知艺术家" } ?: "",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 歌词滚动区域
                            if (parsedLyrics.lines.isNotEmpty()) {
                                LyricsColumn(
                                    lyrics = parsedLyrics,
                                    activeLineIndex = activeLineIndex,
                                    currentTimeMs = currentPosition,
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
