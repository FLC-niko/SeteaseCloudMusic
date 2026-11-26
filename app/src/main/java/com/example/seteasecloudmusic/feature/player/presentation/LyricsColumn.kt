package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seteasecloudmusic.core.common.runCatchingCancellable
import com.example.seteasecloudmusic.feature.player.domain.model.LyricLine
import com.example.seteasecloudmusic.feature.player.domain.model.LyricWord
import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 120Hz 连续锁相环 (PLL) 亚毫秒平滑播放时钟，返回 [FloatState] 供绘制阶段按需读取：
 * - 正常播放时通过 0.90x~1.10x 速率微调追赶底层进度上报，保持单调递增；
 * - 拖动进度条或切歌（差值 >380ms）时瞬时对齐；
 * - 避免高频时钟导致歌词树在 60/120/144Hz 下逐帧重组。
 */
@Composable
internal fun rememberSmoothPlaybackTimeState(
    positionMs: Int,
    durationMs: Int,
    isPlaying: Boolean
): FloatState {
    val smoothMsState = remember { mutableFloatStateOf(positionMs.toFloat()) }
    val anchorPosRef = remember { intArrayOf(positionMs) }
    val anchorNanoRef = remember { longArrayOf(System.nanoTime()) }

    SideEffect {
        if (anchorPosRef[0] != positionMs) {
            anchorPosRef[0] = positionMs
            anchorNanoRef[0] = System.nanoTime()
        }
        if (!isPlaying && smoothMsState.floatValue != positionMs.toFloat()) {
            smoothMsState.floatValue = positionMs.toFloat()
        }
    }

    LaunchedEffect(positionMs, isPlaying) {
        if (!isPlaying) {
            smoothMsState.floatValue = positionMs.toFloat()
        } else if (abs(positionMs.toFloat() - smoothMsState.floatValue) > 380f) {
            // 进度跳变超过 380ms 时直接对齐（切歌或 Seek）
            smoothMsState.floatValue = positionMs.toFloat()
            anchorPosRef[0] = positionMs
            anchorNanoRef[0] = System.nanoTime()
        }
    }

    LaunchedEffect(isPlaying, durationMs) {
        if (!isPlaying) return@LaunchedEffect
        var lastFrameNanos = 0L
        while (isActive) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos == 0L) {
                    lastFrameNanos = frameNanos
                    return@withFrameNanos
                }
                // 单帧最大步长上限 64ms，防止切后台恢复后时间骤跳
                val dtMs = ((frameNanos - lastFrameNanos) / 1_000_000.0).toFloat().coerceIn(0f, 64f)
                lastFrameNanos = frameNanos

                val elapsedSinceAnchorMs =
                    ((System.nanoTime() - anchorNanoRef[0]) / 1_000_000.0).toFloat().coerceAtLeast(0f)
                val targetMs = anchorPosRef[0].toFloat() + elapsedSinceAnchorMs
                val current = smoothMsState.floatValue
                val errorMs = targetMs - current

                if (abs(errorMs) > 420f) {
                    smoothMsState.floatValue = targetMs
                } else {
                    // PLL 比例微调速率（0.90x ~ 1.10x）
                    val rate = (1f + errorMs * 0.0032f).coerceIn(0.90f, 1.10f)
                    val next = current + dtMs * rate
                    smoothMsState.floatValue = if (durationMs > 0) next.coerceAtMost(durationMs.toFloat()) else next
                }
            }
        }
    }

    return smoothMsState
}

@Composable
internal fun rememberSmoothPlaybackTimeMs(
    positionMs: Int,
    durationMs: Int,
    isPlaying: Boolean
): Float = rememberSmoothPlaybackTimeState(positionMs, durationMs, isPlaying).floatValue

@Composable
internal fun rememberInterpolatedPlaybackTimeMs(
    positionMs: Int,
    durationMs: Int,
    isPlaying: Boolean
): Int = rememberSmoothPlaybackTimeState(positionMs, durationMs, isPlaying).floatValue.roundToInt()

internal fun containsCjkChar(text: String): Boolean = text.any { ch ->
    val code = ch.code
    code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF || code in 0x3040..0x30FF || code in 0xAC00..0xD7AF
}

/**
 * 判断单行是否真正具备逐字时间轴（排除 LRC 整行歌词以及把整句歌词塞进单个 token 的伪逐字行）
 */
internal fun isLineEffectivelyWordTimed(line: LyricLine, songHasWordTiming: Boolean): Boolean {
    if (!songHasWordTiming) return false
    val nonBlank = line.words.filter { it.word.isNotBlank() }
    if (nonBlank.isEmpty()) return false
    if (nonBlank.size == 1) {
        val trimmed = nonBlank[0].word.trim()
        if (trimmed.any { it.isWhitespace() }) return false
        if (containsCjkChar(trimmed) && trimmed.length > 3) return false
    }
    return true
}

/**
 * 长音辉光触发判定：
 * - 完整视觉词持续时长 >= 1000ms
 * - 中日韩词长 1..5，或英文完整单词长 1..7
 */
internal fun shouldEmphasizeWord(word: LyricWord): Boolean {
    val duration = word.endTime - word.startTime
    if (duration < 1000) return false
    val trimmed = word.word.trim()
    if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return false
    return if (containsCjkChar(trimmed)) {
        trimmed.length in 1..5
    } else {
        trimmed.length in 1..7
    }
}

internal data class EmphasizeParams(
    val amount: Float,
    val blur: Float,
    val durationMs: Float
)

/**
 * 计算长音辉光与呼吸参数：行尾收音词适度增强并延长呼吸余韵，限制峰值上限防止过曝
 */
internal fun calculateEmphasizeParams(word: LyricWord, isLastWordOfLine: Boolean): EmphasizeParams {
    val dur = max(1000f, (word.endTime - word.startTime).toFloat())
    val a0 = dur / 2000f
    var amount = 0.52f * if (a0 > 1f) sqrt(a0) else a0 * a0
    val b0 = dur / 1800f
    var blur = 0.28f * if (b0 > 1f) sqrt(b0) else b0 * b0
    var effDur = max(1450f, dur * 1.20f)

    if (isLastWordOfLine) {
        amount *= 1.35f
        blur *= 1.28f
        effDur *= 1.18f
    }

    return EmphasizeParams(
        amount = min(0.95f, amount),
        blur = min(0.38f, blur),
        durationMs = effDur
    )
}

/**
 * 钟形余弦辉光包络曲线（起点 x=0、峰值 x=0.5、终点 x=1 处一阶导数为 0，平滑亮灭过渡）
 */
internal fun amllEmpEasing(x: Float): Float {
    val clamped = x.coerceIn(0f, 1f)
    return (0.5f * (1f - cos(2.0 * PI * clamped)).toFloat()).coerceIn(0f, 1f)
}

/**
 * 单字上浮 S 型缓动曲线：
 * - 起唱点与终点速度均为 0，避免起始帧位移突变
 * - 前段加速托起，后段减速落位
 */
internal fun amllWordFloatEasing(t: Float): Float {
    val clamped = t.coerceIn(0f, 1f)
    val u = sin(clamped * (PI * 0.5)).toFloat()
    return (u * u * (3f - 2f * u)).coerceIn(0f, 1f)
}

/**
 * 完整视觉单词分组：
 * 将没有空白分隔的连续非中日韩音节（如 TTML 中拆分的 ["a", "way"]）合并为完整视觉单词组，
 * 使其共享长音辉光判定与连贯字距，同时保留内部各音节的逐音节扫光时间轴。
 */
internal data class VisualWordGroup(
    val mergedWord: LyricWord,
    val syllables: List<LyricWord>,
    val charStartInLine: Int,
    val charEndInLine: Int
)

internal fun groupVisualWords(words: List<LyricWord>): List<VisualWordGroup> {
    if (words.isEmpty()) return emptyList()
    val groups = ArrayList<VisualWordGroup>(words.size)

    var cursor = 0
    var pendingSyllables = mutableListOf<LyricWord>()
    var pendingStartChar = 0
    var pendingEndChar = 0

    fun flushPending() {
        if (pendingSyllables.isEmpty()) return
        val combinedText = buildString { pendingSyllables.forEach { append(it.word) } }
        val minStart = pendingSyllables.minOf { it.startTime }
        val maxEnd = pendingSyllables.maxOf { it.endTime }
        val anyObscene = pendingSyllables.any { it.obscene }
        groups.add(
            VisualWordGroup(
                mergedWord = LyricWord(
                    word = combinedText,
                    startTime = minStart,
                    endTime = maxEnd,
                    obscene = anyObscene
                ),
                syllables = pendingSyllables.toList(),
                charStartInLine = pendingStartChar,
                charEndInLine = pendingEndChar
            )
        )
        pendingSyllables = mutableListOf()
    }

    for (w in words) {
        val raw = w.word
        if (raw.isEmpty()) continue
        val wStartChar = cursor
        val wEndChar = cursor + raw.length
        cursor = wEndChar

        if (raw.isBlank()) {
            flushPending()
            continue
        }

        val isCjk = containsCjkChar(raw)
        val startsWithSpace = raw.first().isWhitespace()
        val prevEndsWithSpace = pendingSyllables.lastOrNull()?.word?.lastOrNull()?.isWhitespace() == true

        val canMergeWithPrev = pendingSyllables.isNotEmpty() &&
            !isCjk &&
            !startsWithSpace &&
            !prevEndsWithSpace &&
            !containsCjkChar(pendingSyllables.last().word)

        if (canMergeWithPrev) {
            pendingSyllables.add(w)
            pendingEndChar = wEndChar
        } else {
            flushPending()
            pendingSyllables.add(w)
            pendingStartChar = wStartChar
            pendingEndChar = wEndChar
            if (isCjk || raw.last().isWhitespace()) {
                flushPending()
            }
        }
    }
    flushPending()
    return groups
}

private sealed interface LyricDisplayItem {
    val key: String

    data class Interlude(
        override val key: String,
        val startTime: Int,
        val endTime: Int
    ) : LyricDisplayItem

    data class VocalGroup(
        override val key: String,
        val mainLineIndex: Int,
        val mainLine: LyricLine,
        val bgLines: List<LyricLine>
    ) : LyricDisplayItem
}

private fun buildLyricDisplayItems(lines: List<LyricLine>): List<LyricDisplayItem> {
    if (lines.isEmpty()) return emptyList()
    val result = ArrayList<LyricDisplayItem>(lines.size + 4)
    var i = 0
    var prevEndTime = 0

    while (i < lines.size) {
        val line = lines[i]
        if (line.isBG) {
            i++
            continue
        }
        val lineText = line.words.joinToString("") { it.word }.trim()
        if (lineText.isEmpty()) {
            i++
            continue
        }

        val gap = line.startTime - prevEndTime
        // 句间空白 >= 7000ms 时插入间奏呼吸圆点提示项
        if (gap >= 7000 && line.startTime > 0) {
            result.add(
                LyricDisplayItem.Interlude(
                    key = "interlude_${prevEndTime}_${line.startTime}_$i",
                    startTime = prevEndTime,
                    endTime = line.startTime
                )
            )
        }

        val bgList = ArrayList<LyricLine>(2)
        var groupEnd = max(line.endTime, line.startTime)
        var nextIdx = i + 1
        while (nextIdx < lines.size && lines[nextIdx].isBG) {
            val bg = lines[nextIdx]
            if (bg.words.any { it.word.isNotBlank() }) {
                bgList.add(bg)
                groupEnd = max(groupEnd, bg.endTime)
            }
            nextIdx++
        }

        result.add(
            LyricDisplayItem.VocalGroup(
                key = "vocal_${line.startTime}_$i",
                mainLineIndex = i,
                mainLine = line,
                bgLines = bgList
            )
        )
        prevEndTime = max(prevEndTime, groupEnd)
        i = nextIdx
    }
    return result
}

/**
 * 逐字歌词滚动列表：
 * - 逐字横向扫光、字体上浮缓动与长音辉光（非逐字整行歌词不启用辉光）
 * - 按行内字符包围盒切分跨行单词，防止自动换行时相邻行字符重叠
 * - 支持随距离递增的景深模糊阶梯与上下边缘 DstIn 渐隐遮罩
 */
@Composable
fun LyricsColumn(
    lyrics: ParsedLyrics,
    activeLineIndex: Int,
    currentTimeMs: Int,
    isPlaying: Boolean,
    onLineClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    controlsVisible: Boolean = true,
    onToggleControls: (() -> Unit)? = null,
    onUserActivity: (() -> Unit)? = null,
    entryTranslationY: () -> Float = { 0f }
) {
    val smoothTimeState = rememberSmoothPlaybackTimeState(
        positionMs = currentTimeMs,
        durationMs = 0,
        isPlaying = isPlaying
    )

    val displayItems = remember(lyrics) { buildLyricDisplayItems(lyrics.lines) }

    // 仅在激活歌词项索引变化时驱动列表滚动，避免每帧整页重组
    val activeItemIndex by remember(displayItems, activeLineIndex, smoothTimeState) {
        derivedStateOf {
            if (displayItems.isEmpty()) {
                -1
            } else {
                val nowMs = smoothTimeState.floatValue.roundToInt()
                val bySmoothTime = displayItems.indexOfLast { item ->
                    when (item) {
                        is LyricDisplayItem.VocalGroup -> item.mainLine.startTime <= nowMs
                        is LyricDisplayItem.Interlude -> nowMs in item.startTime until item.endTime
                    }
                }
                if (bySmoothTime >= 0) {
                    bySmoothTime
                } else {
                    displayItems.indexOfFirst {
                        it is LyricDisplayItem.VocalGroup && it.mainLineIndex >= activeLineIndex.coerceAtLeast(0)
                    }.coerceAtLeast(0)
                }
            }
        }
    }
    val smoothTimeProvider = remember(smoothTimeState) { { smoothTimeState.floatValue } }

    BoxWithConstraints(
        modifier = modifier.pointerInput(onToggleControls, onUserActivity) {
            if (onToggleControls != null || onUserActivity != null) {
                detectTapGestures(
                    onTap = {
                        onUserActivity?.invoke()
                        onToggleControls?.invoke()
                    }
                )
            }
        }
    ) {
        val density = LocalDensity.current
        val screenWidthDp = maxWidth.value

        val sharedLineHeightStyle = remember {
            LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None
            )
        }
        // 主歌词与背景和声歌词排版样式
        val mainStyle = remember {
            TextStyle(
                fontSize = 30.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = sharedLineHeightStyle
            )
        }
        val bgStyle = remember {
            TextStyle(
                fontSize = 20.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.25).sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = sharedLineHeightStyle
            )
        }

        // 当前高亮行锚定在视口顶部下方 58dp 处
        val anchorOffsetPx = with(density) { 58.dp.roundToPx() }
        val initialIndex = remember { activeItemIndex.coerceAtLeast(0) }
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = initialIndex,
            initialFirstVisibleItemScrollOffset = if (initialIndex > 0) -anchorOffsetPx else 0
        )
        val measurer = rememberTextMeasurer(cacheSize = 768)

        var userScrolling by remember { mutableStateOf(false) }
        var manualBrowseActive by remember { mutableStateOf(false) }
        val onUserActivityUpdated by rememberUpdatedState(onUserActivity)

        LaunchedEffect(userScrolling) {
            if (userScrolling) {
                manualBrowseActive = true
                onUserActivityUpdated?.invoke()
            } else if (manualBrowseActive) {
                // 用户松手停止浏览 2400ms 后恢复自动跟随滚动
                delay(2400)
                manualBrowseActive = false
            }
        }

        LaunchedEffect(listState) {
            listState.interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is DragInteraction.Start -> {
                        userScrolling = true
                        onUserActivityUpdated?.invoke()
                    }
                    is DragInteraction.Stop, is DragInteraction.Cancel -> {
                        userScrolling = false
                    }
                }
            }
        }

        val measuredItemHeights = remember(displayItems, screenWidthDp, density.fontScale) {
            mutableStateMapOf<Int, Int>()
        }
        val scrollResidual = remember { Animatable(0f) }
        var hasPositionedInitially by remember(displayItems) { mutableStateOf(false) }

        LaunchedEffect(activeItemIndex, manualBrowseActive, displayItems, screenWidthDp) {
            if (manualBrowseActive || activeItemIndex !in displayItems.indices) return@LaunchedEffect

            val targetScrollOffset = -anchorOffsetPx
            if (!hasPositionedInitially) {
                hasPositionedInitially = true
                scrollResidual.snapTo(0f)
                runCatchingCancellable {
                    listState.scrollToItem(activeItemIndex, scrollOffset = targetScrollOffset)
                }
                return@LaunchedEffect
            }

            val fallbackStep = with(density) { 64.dp.toPx() }
            val prevResidual = scrollResidual.value
            val beforeTargetY = listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == activeItemIndex }
                ?.offset

            val estimatedDelta = estimateScrollDelta(
                from = listState.firstVisibleItemIndex,
                to = activeItemIndex,
                measured = measuredItemHeights,
                fallbackStep = fallbackStep
            ) + (targetScrollOffset - listState.firstVisibleItemScrollOffset)

            // 跨行距离超过 420dp 时直接跳转定位，近距离切行执行带阻尼的残差平滑滚动
            if (abs(estimatedDelta) > with(density) { 420.dp.toPx() }) {
                scrollResidual.snapTo(0f)
                runCatchingCancellable {
                    listState.scrollToItem(activeItemIndex, scrollOffset = targetScrollOffset)
                }
            } else {
                runCatchingCancellable {
                    listState.scrollToItem(activeItemIndex, scrollOffset = targetScrollOffset)
                }
                val afterTargetY = listState.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == activeItemIndex }
                    ?.offset
                val exactJump = if (beforeTargetY != null && afterTargetY != null) {
                    (beforeTargetY - afterTargetY).toFloat()
                } else {
                    estimatedDelta
                }
                val startResidual = prevResidual + exactJump
                if (abs(startResidual) > 0.5f) {
                    scrollResidual.snapTo(startResidual)
                    // 切行滚动弹簧（高阻尼 0.92f 抑制过冲，低刚度 135f 保持柔和跟进）
                    scrollResidual.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 0.92f, stiffness = 135f)
                    )
                } else {
                    scrollResidual.snapTo(0f)
                }
            }
        }

        // 顶部留白与底部 60% 视口留白（确保末尾歌词可滚至顶部锚点）
        val topPadding = 62.dp
        val bottomPadding = with(density) { (maxHeight.toPx() * 0.60f).toDp() }
        val contentWidthPx = with(density) { (maxWidth - 58.dp).coerceAtLeast(1.dp).roundToPx() }

        // 上下边缘 DstIn 渐隐遮罩高度（控制栏显示时底部渐隐区略高）
        val topFadePx = with(density) { 54.dp.toPx() }
        val bottomFadePx = with(density) { (if (controlsVisible) 66.dp else 56.dp).toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        val h = size.height.coerceAtLeast(1f)
                        val topStop = (topFadePx / h).coerceIn(0.01f, 0.35f)
                        val bottomStop = (1f - (bottomFadePx / h)).coerceIn(0.55f, 0.99f)
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Transparent,
                                topStop to Color.Black,
                                bottomStop to Color.Black,
                                1f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = scrollResidual.value + entryTranslationY()
                    },
                contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding)
            ) {
                itemsIndexed(
                    items = displayItems,
                    key = { _, item -> item.key }
                ) { itemIdx, item ->
                    when (item) {
                        is LyricDisplayItem.Interlude -> {
                            val showDots by remember(item, smoothTimeState) {
                                derivedStateOf {
                                    val nowInt = smoothTimeState.floatValue.roundToInt()
                                    // 间奏圆点提前 250ms 展开、延后 200ms 收起
                                    nowInt in (item.startTime - 250)..(item.endTime + 200)
                                }
                            }
                            AnimatedVisibility(
                                visible = showDots,
                                enter = fadeIn(tween(260)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                                exit = fadeOut(tween(220)) + shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow))
                            ) {
                                AmllInterludeDots(
                                    startTimeMs = item.startTime,
                                    endTimeMs = item.endTime,
                                    smoothTimeProvider = smoothTimeProvider,
                                    onMeasured = { h -> measuredItemHeights[itemIdx] = h }
                                )
                            }
                        }

                        is LyricDisplayItem.VocalGroup -> {
                            AmllVocalGroupItem(
                                group = item,
                                itemIndex = itemIdx,
                                activeItemIndex = activeItemIndex,
                                smoothTimeProvider = smoothTimeProvider,
                                hasWordTiming = lyrics.hasWordTiming,
                                manualBrowseActive = manualBrowseActive,
                                measurer = measurer,
                                mainStyle = mainStyle,
                                bgStyle = bgStyle,
                                contentWidthPx = contentWidthPx,
                                onMeasured = { h -> measuredItemHeights[itemIdx] = h },
                                onClick = {
                                    onUserActivity?.invoke()
                                    if (!controlsVisible && onToggleControls != null) {
                                        onToggleControls()
                                    } else {
                                        manualBrowseActive = false
                                        onLineClick?.invoke(item.mainLine.startTime)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AmllInterludeDots(
    startTimeMs: Int,
    endTimeMs: Int,
    smoothTimeProvider: () -> Float,
    onMeasured: (Int) -> Unit
) {
    val duration = (endTimeMs - startTimeMs).coerceAtLeast(1).toFloat()

    val infiniteTransition = rememberInfiniteTransition(label = "interludeBreath")
    val breathState = infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )

    val reportedHeight = remember { intArrayOf(0) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                if (reportedHeight[0] != placeable.height) {
                    reportedHeight[0] = placeable.height
                    onMeasured(placeable.height)
                }
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }
            .padding(horizontal = 28.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(
            modifier = Modifier
                .size(width = 52.dp, height = 22.dp)
                .graphicsLayer {
                    val breath = breathState.value
                    scaleX = breath
                    scaleY = breath
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
        ) {
            val progress = ((smoothTimeProvider() - startTimeMs) / duration).coerceIn(0f, 1f)
            val r = 5.2.dp.toPx()
            val cy = size.height * 0.5f
            val spacing = 16.dp.toPx()
            for (dotIdx in 0 until 3) {
                val thresholdStart = dotIdx / 3f
                val thresholdEnd = (dotIdx + 1) / 3f
                val fill = ((progress - thresholdStart) / (thresholdEnd - thresholdStart)).coerceIn(0f, 1f)
                val alpha = 0.24f + 0.72f * fill
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = r,
                    center = Offset(r + dotIdx * spacing, cy)
                )
            }
        }
    }
}

private data class SyllableSpanInSegment(
    val startTime: Int,
    val endTime: Int,
    val pixelStartFraction: Float,
    val pixelEndFraction: Float
)

private data class PositionedWordSegment(
    val groupWord: LyricWord,
    val syllableSpans: List<SyllableSpanInSegment>,
    val layout: TextLayoutResult,
    val xLeft: Float,
    val yTop: Float,
    val shouldEmphasize: Boolean,
    val emphasizeParams: EmphasizeParams?
)

private data class PreparedLyricLineLayout(
    val fullLayout: TextLayoutResult,
    val segments: List<PositionedWordSegment>,
    val isWordTimed: Boolean,
    val widthPx: Int,
    val heightPx: Int
)

private fun prepareLyricLineLayout(
    line: LyricLine,
    style: TextStyle,
    measurer: TextMeasurer,
    maxWidthPx: Int,
    isDuet: Boolean,
    songHasWordTiming: Boolean
): PreparedLyricLineLayout {
    val fullText = buildString {
        line.words.forEach { append(it.word) }
    }
    val alignedStyle = style.copy(
        color = Color.White,
        textAlign = if (isDuet) TextAlign.End else TextAlign.Start
    )
    val fullLayout = measurer.measure(
        text = AnnotatedString(fullText),
        style = alignedStyle,
        constraints = Constraints(minWidth = maxWidthPx, maxWidth = maxWidthPx),
        overflow = TextOverflow.Clip
    )

    val effectiveWordTimed = isLineEffectivelyWordTimed(line, songHasWordTiming)
    if (!effectiveWordTimed || fullText.isEmpty()) {
        return PreparedLyricLineLayout(
            fullLayout = fullLayout,
            segments = emptyList(),
            isWordTimed = false,
            widthPx = fullLayout.size.width,
            heightPx = fullLayout.size.height
        )
    }

    val visualGroups = groupVisualWords(line.words)
    if (visualGroups.isEmpty()) {
        return PreparedLyricLineLayout(
            fullLayout = fullLayout,
            segments = emptyList(),
            isWordTimed = false,
            widthPx = fullLayout.size.width,
            heightPx = fullLayout.size.height
        )
    }

    val lastGroupIndex = visualGroups.lastIndex
    val segments = ArrayList<PositionedWordSegment>(visualGroups.size + 2)

    visualGroups.forEachIndexed { groupIdx, vGroup ->
        var visCharStart = vGroup.charStartInLine.coerceIn(0, fullText.length)
        var visCharEnd = vGroup.charEndInLine.coerceIn(0, fullText.length)
        while (visCharStart < visCharEnd && fullText[visCharStart].isWhitespace()) visCharStart++
        while (visCharEnd > visCharStart && fullText[visCharEnd - 1].isWhitespace()) visCharEnd--
        if (visCharStart >= visCharEnd) return@forEachIndexed

        val emphasize = shouldEmphasizeWord(vGroup.mergedWord)
        val empParams = if (emphasize) {
            calculateEmphasizeParams(
                vGroup.mergedWord,
                isLastWordOfLine = groupIdx == lastGroupIndex
            )
        } else null

        val firstLine = fullLayout.getLineForOffset(visCharStart)
        val lastLine = fullLayout.getLineForOffset(visCharEnd - 1)

        // 记录组内每个音节在 fullText 中的字符区间，以实现像 "a" + "way" 的精确分音节扫光速度
        val syllableRanges = ArrayList<Triple<Int, Int, LyricWord>>(vGroup.syllables.size)
        var sylCursor = vGroup.charStartInLine
        for (syl in vGroup.syllables) {
            val sStart = sylCursor
            val sEnd = sylCursor + syl.word.length
            sylCursor = sEnd
            syllableRanges.add(Triple(sStart, sEnd, syl))
        }

        for (visLine in firstLine..lastLine) {
            val lineStart = fullLayout.getLineStart(visLine)
            val lineEndExclusive = fullLayout.getLineEnd(visLine, visibleEnd = true)
            var segStart = max(visCharStart, lineStart)
            var segEnd = min(visCharEnd, lineEndExclusive)
            while (segStart < segEnd && fullText[segStart].isWhitespace()) segStart++
            while (segEnd > segStart && fullText[segEnd - 1].isWhitespace()) segEnd--
            if (segEnd <= segStart) continue

            val segStr = fullText.substring(segStart, segEnd)
            if (segStr.isBlank()) continue

            // 定位防重叠：仅取当前物理行 visLine 内的首字符 segStart 与末字符 (segEnd - 1) 包围盒，
            // 不读取跨到下一行 (visLine + 1) 的光标位置
            val firstGlyphBox = fullLayout.getBoundingBox(segStart)
            val lastGlyphBox = fullLayout.getBoundingBox(segEnd - 1)
            val xLeft = min(firstGlyphBox.left, lastGlyphBox.left)
            val xRight = max(firstGlyphBox.right, lastGlyphBox.right)
            val segBoxWidth = (xRight - xLeft).coerceAtLeast(1f)

            val segLayout = measurer.measure(
                text = AnnotatedString(segStr),
                style = style.copy(color = Color.White),
                maxLines = 1,
                softWrap = false
            )

            // 按基线对齐单行 segLayout 与多行 fullLayout，消除垂直偏移
            val targetBaselineY = fullLayout.getLineBaseline(visLine)
            val yTop = targetBaselineY - segLayout.firstBaseline

            // 计算该行切片内部各音节对应的像素比例区间
            val spans = ArrayList<SyllableSpanInSegment>(syllableRanges.size)
            for ((sStart, sEnd, syl) in syllableRanges) {
                var oStart = max(segStart, sStart)
                var oEnd = min(segEnd, sEnd)
                while (oStart < oEnd && fullText[oStart].isWhitespace()) oStart++
                while (oEnd > oStart && fullText[oEnd - 1].isWhitespace()) oEnd--
                if (oEnd <= oStart) continue

                val b0 = fullLayout.getBoundingBox(oStart)
                val b1 = fullLayout.getBoundingBox(oEnd - 1)
                val pLeft = ((min(b0.left, b1.left) - xLeft) / segBoxWidth).coerceIn(0f, 1f)
                val pRight = ((max(b0.right, b1.right) - xLeft) / segBoxWidth).coerceIn(pLeft, 1f)

                val sylNonSpaceLen = syl.word.count { !it.isWhitespace() }.coerceAtLeast(1)
                val overlapLen = (oEnd - oStart).coerceAtLeast(1)
                val fullSylDur = (syl.endTime - syl.startTime).coerceAtLeast(1)
                val relBefore = (oStart - sStart).coerceAtLeast(0).toFloat() / sylNonSpaceLen.toFloat()
                val relSpan = (overlapLen.toFloat() / sylNonSpaceLen.toFloat()).coerceAtMost(1f)
                val sliceStartMs = syl.startTime + (fullSylDur * relBefore).roundToInt()
                val sliceEndMs = (sliceStartMs + (fullSylDur * relSpan).roundToInt()).coerceAtLeast(sliceStartMs + 1)

                spans.add(
                    SyllableSpanInSegment(
                        startTime = sliceStartMs,
                        endTime = sliceEndMs,
                        pixelStartFraction = pLeft,
                        pixelEndFraction = pRight
                    )
                )
            }

            if (spans.isEmpty()) {
                spans.add(
                    SyllableSpanInSegment(
                        startTime = vGroup.mergedWord.startTime,
                        endTime = max(vGroup.mergedWord.startTime + 1, vGroup.mergedWord.endTime),
                        pixelStartFraction = 0f,
                        pixelEndFraction = 1f
                    )
                )
            }

            segments.add(
                PositionedWordSegment(
                    groupWord = vGroup.mergedWord,
                    syllableSpans = spans,
                    layout = segLayout,
                    xLeft = xLeft,
                    yTop = yTop,
                    shouldEmphasize = emphasize,
                    emphasizeParams = empParams
                )
            )
        }
    }

    return PreparedLyricLineLayout(
        fullLayout = fullLayout,
        segments = segments,
        isWordTimed = segments.isNotEmpty(),
        widthPx = fullLayout.size.width,
        heightPx = fullLayout.size.height
    )
}

private fun computeSegmentSweepFraction(
    spans: List<SyllableSpanInSegment>,
    smoothTimeMs: Float
): Float {
    if (spans.isEmpty()) return 0f
    if (smoothTimeMs <= spans.first().startTime.toFloat()) return 0f
    if (smoothTimeMs >= spans.last().endTime.toFloat()) return 1f

    for (i in spans.indices) {
        val sp = spans[i]
        val s0 = sp.startTime.toFloat()
        val s1 = sp.endTime.toFloat().coerceAtLeast(s0 + 1f)
        if (smoothTimeMs <= s1) {
            if (smoothTimeMs <= s0) {
                return sp.pixelStartFraction
            }
            val t = ((smoothTimeMs - s0) / (s1 - s0)).coerceIn(0f, 1f)
            return sp.pixelStartFraction + (sp.pixelEndFraction - sp.pixelStartFraction) * t
        }
    }
    return 1f
}

@Composable
private fun AmllVocalGroupItem(
    group: LyricDisplayItem.VocalGroup,
    itemIndex: Int,
    activeItemIndex: Int,
    smoothTimeProvider: () -> Float,
    hasWordTiming: Boolean,
    manualBrowseActive: Boolean,
    measurer: TextMeasurer,
    mainStyle: TextStyle,
    bgStyle: TextStyle,
    contentWidthPx: Int,
    onMeasured: (Int) -> Unit,
    onClick: () -> Unit
) {
    val line = group.mainLine
    val rel = itemIndex - activeItemIndex
    val isActive = rel == 0

    // 景深模糊半径阶梯（dp）：手动浏览时全清，自动跟随时按距当前行偏移量递增
    val targetBlurDp = if (manualBrowseActive) {
        0f
    } else {
        when {
            rel == 0 -> 0f
            rel == -1 -> 3.0f
            rel <= -2 -> 4.6f
            rel == 1 -> 1.7f
            rel == 2 -> 2.8f
            rel == 3 -> 3.8f
            else -> 4.6f
        }
    }

    // 行级透明度阶梯：当前行 1.0f，已唱行衰减更快，未唱后续行逐级淡出
    val targetLineAlpha = if (manualBrowseActive) {
        if (isActive) 1.0f else 0.54f
    } else {
        when {
            rel == 0 -> 1.0f
            rel == -1 -> 0.26f
            rel <= -2 -> 0.16f
            rel == 1 -> 0.40f
            rel == 2 -> 0.31f
            rel == 3 -> 0.23f
            else -> 0.17f
        }
    }

    val animatedBlurDp by animateFloatAsState(
        targetValue = targetBlurDp,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "lyricGroupBlur"
    )
    val animatedAlphaState = animateFloatAsState(
        targetValue = targetLineAlpha,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "lyricGroupAlpha"
    )
    // 当前行 1.0x，非当前行轻微缩小至 0.975x
    val animatedScaleState = animateFloatAsState(
        targetValue = if (isActive) 1.0f else 0.975f,
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 175f),
        label = "lyricGroupScale"
    )
    // 激活态混合系数（520ms）：换行退出高亮时让行尾词的上浮与辉光渐进回落，避免瞬时截断
    val activeBlendState = animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "lyricActiveBlend"
    )

    val preparedMain = remember(line, measurer, mainStyle, contentWidthPx, hasWordTiming) {
        prepareLyricLineLayout(
            line = line,
            style = mainStyle,
            measurer = measurer,
            maxWidthPx = contentWidthPx,
            isDuet = line.isDuet,
            songHasWordTiming = hasWordTiming
        )
    }

    val density = LocalDensity.current
    // 主歌词与背景和声歌词的最大单字上浮位移（dp）
    val mainFloatMaxPx = with(density) { 2.5.dp.toPx() }
    val bgFloatMaxPx = with(density) { 2.2.dp.toPx() }
    val reportedHeight = remember { intArrayOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val totalH = placeable.height
                if (reportedHeight[0] != totalH) {
                    reportedHeight[0] = totalH
                    onMeasured(totalH)
                }
                layout(placeable.width, totalH) {
                    placeable.place(0, 0)
                }
            }
            .padding(horizontal = 28.dp, vertical = 13.dp)
            .graphicsLayer {
                alpha = animatedAlphaState.value
                val scaleVal = animatedScaleState.value
                scaleX = scaleVal
                scaleY = scaleVal
                transformOrigin = if (line.isDuet) TransformOrigin(1f, 0.5f) else TransformOrigin(0f, 0.5f)
            }
            .then(
                if (animatedBlurDp > 0.08f) {
                    Modifier.blur(radius = animatedBlurDp.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = if (line.isDuet) Alignment.End else Alignment.Start
    ) {
        AmllRenderedLyricLine(
            prepared = preparedMain,
            activeBlendState = activeBlendState,
            smoothTimeProvider = smoothTimeProvider,
            floatMaxPx = mainFloatMaxPx,
            unplayedAlpha = 0.34f,
            playedAlpha = 1.0f
        )

        if (group.bgLines.isNotEmpty()) {
            AnimatedVisibility(
                visible = isActive || manualBrowseActive,
                enter = fadeIn(tween(260)) + expandVertically(spring(dampingRatio = 0.86f, stiffness = 240f)),
                exit = fadeOut(tween(220)) + shrinkVertically(spring(dampingRatio = 0.86f, stiffness = 240f))
            ) {
                Column(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalAlignment = if (line.isDuet) Alignment.End else Alignment.Start
                ) {
                    group.bgLines.forEachIndexed { bgIdx, bgLine ->
                        if (bgIdx > 0) Spacer(modifier = Modifier.height(4.dp))
                        val preparedBg = remember(bgLine, measurer, bgStyle, contentWidthPx, hasWordTiming) {
                            prepareLyricLineLayout(
                                line = bgLine,
                                style = bgStyle,
                                measurer = measurer,
                                maxWidthPx = contentWidthPx,
                                isDuet = bgLine.isDuet || line.isDuet,
                                songHasWordTiming = hasWordTiming
                            )
                        }
                        AmllRenderedLyricLine(
                            prepared = preparedBg,
                            activeBlendState = activeBlendState,
                            smoothTimeProvider = smoothTimeProvider,
                            floatMaxPx = bgFloatMaxPx,
                            unplayedAlpha = 0.32f,
                            playedAlpha = 0.82f
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AmllRenderedLyricLine(
    prepared: PreparedLyricLineLayout,
    activeBlendState: State<Float>,
    smoothTimeProvider: () -> Float,
    floatMaxPx: Float,
    unplayedAlpha: Float,
    playedAlpha: Float
) {
    val density = LocalDensity.current
    val heightDp = with(density) { prepared.heightPx.toDp() }
    val glowBlurRadiusPx = with(density) { 8.5.dp.toPx() }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp)
    ) {
        val activeBlend = activeBlendState.value
        // 非激活行或非逐字整行歌词（如 LRC）：直接返回，不读取 smoothTimeProvider()，
        // 让 Compose 快照系统完全不订阅每帧时钟，避免非激活模糊行随逐帧进度空重绘。
        if (!prepared.isWordTimed || prepared.segments.isEmpty() || activeBlend <= 0.004f) {
            val alpha = if (!prepared.isWordTimed && activeBlend > 0.5f) playedAlpha else 1f
            drawText(
                textLayoutResult = prepared.fullLayout,
                color = Color.White.copy(alpha = alpha),
                topLeft = Offset.Zero
            )
            return@Canvas
        }

        val smoothTimeMs = smoothTimeProvider()

        // 当行刚进入/退出激活态时，未唱部分平滑向整行纯色收敛，过渡自然不跳变
        val effectiveUnplayedAlpha = unplayedAlpha + (1f - unplayedAlpha) * (1f - activeBlend)
        val unplayedColor = Color.White.copy(alpha = effectiveUnplayedAlpha)
        val playedColor = Color.White.copy(alpha = playedAlpha)

        prepared.segments.forEach { seg ->
            val w = seg.groupWord
            val wordDur = (w.endTime - w.startTime).coerceAtLeast(1).toFloat()

            // 1. 字体上浮波浪：提前 110ms 启动（最小时长 1250ms，按词长 1.35x 延伸），退出高亮时随 activeBlend 收敛回落
            val floatStartMs = w.startTime.toFloat() - 110f
            val floatDurMs = max(1250f, wordDur * 1.35f)
            val floatT = ((smoothTimeMs - floatStartMs) / floatDurMs).coerceIn(0f, 1f)
            val floatEase = amllWordFloatEasing(floatT)
            var yOffset = -floatMaxPx * floatEase * activeBlend

            // 2. 分音节精确横向扫光比例
            val localProgress = computeSegmentSweepFraction(seg.syllableSpans, smoothTimeMs)

            // 3. 长音辉光与微呼吸缩放（仅逐字模式达标长音启用）
            var empScale = 1f
            var glowAlpha = 0f
            val emp = seg.emphasizeParams
            if (seg.shouldEmphasize && emp != null) {
                val empStartMs = w.startTime.toFloat()
                val empT = (smoothTimeMs - empStartMs) / emp.durationMs
                if (empT in 0f..1f) {
                    val e = amllEmpEasing(empT) * activeBlend
                    // 长音呼吸膨胀上限 4.0%，并叠加轻微上浮
                    empScale = 1f + 0.040f * emp.amount * e
                    yOffset -= floatMaxPx * 0.38f * emp.amount * e
                    // 阴影透明度上限 0.38f，防止叠加过曝
                    glowAlpha = (emp.blur * e).coerceIn(0f, 0.38f)
                }
            }

            val glowShadow = if (glowAlpha > 0.004f) {
                Shadow(
                    color = Color.White.copy(alpha = glowAlpha),
                    offset = Offset.Zero,
                    blurRadius = glowBlurRadiusPx
                )
            } else {
                null
            }

            val segW = seg.layout.size.width.toFloat().coerceAtLeast(1f)
            val segH = seg.layout.size.height.toFloat().coerceAtLeast(1f)
            val topLeft = Offset(seg.xLeft, seg.yTop + yOffset)
            val pivot = Offset(seg.xLeft + segW * 0.5f, seg.yTop + segH * 0.55f)

            withTransform({
                if (empScale != 1f) {
                    scale(empScale, empScale, pivot = pivot)
                }
            }) {
                // 单遍逐字柔和渐变羽化染色 + 原生底层柔光阴影（同时兼顾 "away" 整体柔光与分音节精确扫光）
                when {
                    localProgress <= 0.001f -> {
                        drawText(
                            textLayoutResult = seg.layout,
                            color = unplayedColor,
                            topLeft = topLeft,
                            shadow = glowShadow
                        )
                    }

                    localProgress >= 0.999f -> {
                        drawText(
                            textLayoutResult = seg.layout,
                            color = playedColor,
                            topLeft = topLeft,
                            shadow = glowShadow
                        )
                    }

                    else -> {
                        // 横向扫光渐隐边缘半宽（至少 8px）
                        val fadeHalf = max(segH * 0.32f, 8f)
                        val sweepCenter = -fadeHalf + localProgress * (segW + fadeHalf * 2f)
                        val sweepBrush = Brush.horizontalGradient(
                            0f to playedColor,
                            1f to unplayedColor,
                            startX = sweepCenter - fadeHalf,
                            endX = sweepCenter + fadeHalf
                        )
                        drawText(
                            textLayoutResult = seg.layout,
                            brush = sweepBrush,
                            topLeft = topLeft,
                            shadow = glowShadow
                        )
                    }
                }
            }
        }
    }
}

private fun estimateScrollDelta(
    from: Int,
    to: Int,
    measured: Map<Int, Int>,
    fallbackStep: Float
): Float {
    if (from == to) return 0f
    var sum = 0f
    if (to > from) {
        for (i in from until to) sum += measured[i]?.toFloat() ?: fallbackStep
        return sum
    }
    for (i in to until from) sum += measured[i]?.toFloat() ?: fallbackStep
    return -sum
}
