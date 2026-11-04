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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
 * 120Hz 连续锁相环 (PLL) 亚毫秒平滑播放时钟：
 * - 彻底避免底层 500ms 轮询上报造成的帧重启或时间跳变；
 * - 正常播放时通过 0.90x~1.10x 连续微调速率平滑追赶真实硬件时钟，严格单调递增；
 * - 仅在用户拖动进度条 / 切歌 (>380ms 跳变) 时瞬时对齐。
 */
@Composable
internal fun rememberSmoothPlaybackTimeMs(
    positionMs: Int,
    durationMs: Int,
    isPlaying: Boolean
): Float {
    var smoothMs by remember { mutableFloatStateOf(positionMs.toFloat()) }
    val anchorPosRef = remember { intArrayOf(positionMs) }
    val anchorNanoRef = remember { longArrayOf(System.nanoTime()) }

    SideEffect {
        if (anchorPosRef[0] != positionMs) {
            anchorPosRef[0] = positionMs
            anchorNanoRef[0] = System.nanoTime()
        }
    }

    LaunchedEffect(positionMs, isPlaying) {
        if (!isPlaying) {
            smoothMs = positionMs.toFloat()
        } else if (abs(positionMs.toFloat() - smoothMs) > 380f) {
            smoothMs = positionMs.toFloat()
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
                val dtMs = ((frameNanos - lastFrameNanos) / 1_000_000.0).toFloat().coerceIn(0f, 64f)
                lastFrameNanos = frameNanos

                val elapsedSinceAnchorMs =
                    ((System.nanoTime() - anchorNanoRef[0]) / 1_000_000.0).toFloat().coerceAtLeast(0f)
                val targetMs = anchorPosRef[0].toFloat() + elapsedSinceAnchorMs
                val errorMs = targetMs - smoothMs

                if (abs(errorMs) > 420f) {
                    smoothMs = targetMs
                } else {
                    val rate = (1f + errorMs * 0.0032f).coerceIn(0.90f, 1.10f)
                    val next = smoothMs + dtMs * rate
                    smoothMs = if (durationMs > 0) next.coerceAtMost(durationMs.toFloat()) else next
                }
            }
        }
    }

    return if (isPlaying) smoothMs else positionMs.toFloat()
}

@Composable
internal fun rememberInterpolatedPlaybackTimeMs(
    positionMs: Int,
    durationMs: Int,
    isPlaying: Boolean
): Int = rememberSmoothPlaybackTimeMs(positionMs, durationMs, isPlaying).roundToInt()

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
 * AMLL 长音辉光判定算法：
 * - 完整视觉词持续时长 >= 1000ms
 * - 中日韩词长 1..5，或英文完整单词长 1..7（如 "wife"、"away"）
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
 * 计算柔和长音强调参数：行尾收音词适度增强并延长呼吸余韵，峰值克制淡雅不过曝
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
 * C∞ 无穷阶光滑钟形辉光包络曲线（起点 x=0、峰值 x=0.5、终点 x=1 处一阶导数均为 0，彻底消除亮灭折角突变）
 */
internal fun amllEmpEasing(x: Float): Float {
    val clamped = x.coerceIn(0f, 1f)
    return (0.5f * (1f - cos(2.0 * PI * clamped)).toFloat()).coerceIn(0f, 1f)
}

/**
 * C¹ 光滑 S 型上浮缓动曲线：
 * - t=0 处速度为 0（彻底消除起唱帧“直接位移跳变”感）
 * - 前段轻盈加速托起，后段长尾丝滑减速落位（t=1 处速度亦为 0）
 */
internal fun amllWordFloatEasing(t: Float): Float {
    val clamped = t.coerceIn(0f, 1f)
    val u = sin(clamped * (PI * 0.5)).toFloat()
    return (u * u * (3f - 2f * u)).coerceIn(0f, 1f)
}

/**
 * 完整视觉单词分组（参照 AMLL chunkAndSplitLyricWords）：
 * 将没有空白分隔的连续非中日韩音节（如 TTML 中被拆成 ["a", "way"] 的 "away"）合并为同一个完整视觉单词组，
 * 使其共享完整的辉光判定、整体柔和呼吸与连贯字距，同时保留内部各音节精确的逐音节扫光时间轴。
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
 * 1:1 Apple Music / AMLL 歌词列表：
 * - 逐字流光染色 + C¹ 丝滑字体上浮 + 完整视觉词柔和长音辉光（非逐字的整行歌词不加任何辉光）
 * - 基于严格行内字符包围盒定位，彻底杜绝自动换行行尾字符跨行重叠
 * - 距离阶梯景深模糊 + 上下边缘 DstIn 渐隐遮罩
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
    val smoothTimeMs = rememberSmoothPlaybackTimeMs(
        positionMs = currentTimeMs,
        durationMs = 0,
        isPlaying = isPlaying
    )

    val displayItems = remember(lyrics) { buildLyricDisplayItems(lyrics.lines) }

    val activeItemIndex = remember(displayItems, smoothTimeMs.toInt() / 16, activeLineIndex) {
        if (displayItems.isEmpty()) {
            -1
        } else {
            val nowMs = smoothTimeMs.roundToInt()
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
                    scrollResidual.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 0.92f, stiffness = 135f)
                    )
                } else {
                    scrollResidual.snapTo(0f)
                }
            }
        }

        val topPadding = 62.dp
        val bottomPadding = with(density) { (maxHeight.toPx() * 0.60f).toDp() }
        val contentWidthPx = with(density) { (maxWidth - 58.dp).coerceAtLeast(1.dp).roundToPx() }

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
                            val nowInt = smoothTimeMs.roundToInt()
                            val showDots = nowInt in (item.startTime - 250)..(item.endTime + 200)
                            AnimatedVisibility(
                                visible = showDots,
                                enter = fadeIn(tween(260)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                                exit = fadeOut(tween(220)) + shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow))
                            ) {
                                AmllInterludeDots(
                                    startTimeMs = item.startTime,
                                    endTimeMs = item.endTime,
                                    currentTimeMs = smoothTimeMs,
                                    onMeasured = { h -> measuredItemHeights[itemIdx] = h }
                                )
                            }
                        }

                        is LyricDisplayItem.VocalGroup -> {
                            AmllVocalGroupItem(
                                group = item,
                                itemIndex = itemIdx,
                                activeItemIndex = activeItemIndex,
                                smoothTimeMs = smoothTimeMs,
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
    currentTimeMs: Float,
    onMeasured: (Int) -> Unit
) {
    val duration = (endTimeMs - startTimeMs).coerceAtLeast(1).toFloat()
    val progress = ((currentTimeMs - startTimeMs) / duration).coerceIn(0f, 1f)

    val infiniteTransition = rememberInfiniteTransition(label = "interludeBreath")
    val breath by infiniteTransition.animateFloat(
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
                    scaleX = breath
                    scaleY = breath
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
        ) {
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

            // 核心防重叠定位：仅查询严格属于当前行 visLine 的首字符 segStart 与末字符 (segEnd - 1) 的字形包围盒，
            // 绝不读取跨到下一行 (visLine + 1) 的 segEnd 光标位置！
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

            // 使用基线严格对齐，确保单行 segLayout 与多行 fullLayout 垂直方向 0.00px 误差
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
    smoothTimeMs: Float,
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
    val animatedAlpha by animateFloatAsState(
        targetValue = targetLineAlpha,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "lyricGroupAlpha"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isActive) 1.0f else 0.975f,
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 175f),
        label = "lyricGroupScale"
    )
    // 激活态平滑混合系数：换行退出高亮时让行尾词的上浮与辉光在 520ms 内丝滑回落，绝不一帧生硬截断
    val activeBlend by animateFloatAsState(
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
                alpha = animatedAlpha
                scaleX = animatedScale
                scaleY = animatedScale
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
            activeBlend = activeBlend,
            smoothTimeMs = smoothTimeMs,
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
                            activeBlend = activeBlend,
                            smoothTimeMs = smoothTimeMs,
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
    activeBlend: Float,
    smoothTimeMs: Float,
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
        // 非激活行或非逐字整行歌词（如 LRC）：仅渲染纯净锐利白色文字，绝不附加任何辉光阴影
        if (!prepared.isWordTimed || prepared.segments.isEmpty() || activeBlend <= 0.004f) {
            val alpha = if (!prepared.isWordTimed && activeBlend > 0.5f) playedAlpha else 1f
            drawText(
                textLayoutResult = prepared.fullLayout,
                color = Color.White.copy(alpha = alpha),
                topLeft = Offset.Zero
            )
            return@Canvas
        }

        // 当行刚进入/退出激活态时，未唱部分平滑向整行纯色收敛，过渡自然不跳变
        val effectiveUnplayedAlpha = unplayedAlpha + (1f - unplayedAlpha) * (1f - activeBlend)
        val unplayedColor = Color.White.copy(alpha = effectiveUnplayedAlpha)
        val playedColor = Color.White.copy(alpha = playedAlpha)

        prepared.segments.forEach { seg ->
            val w = seg.groupWord
            val wordDur = (w.endTime - w.startTime).coerceAtLeast(1).toFloat()

            // 1. 丝滑字体上浮 (Word Float)：
            // 起唱前保持 0f（激活瞬间零跳变），提前 110ms 启动 C¹ 光滑 S 曲线轻柔托起至 -floatMaxPx，退出激活时随 activeBlend 平滑回落
            val floatStartMs = w.startTime.toFloat() - 110f
            val floatDurMs = max(1250f, wordDur * 1.35f)
            val floatT = ((smoothTimeMs - floatStartMs) / floatDurMs).coerceIn(0f, 1f)
            val floatEase = amllWordFloatEasing(floatT)
            var yOffset = -floatMaxPx * floatEase * activeBlend

            // 2. 分音节精确横向扫光比例
            val localProgress = computeSegmentSweepFraction(seg.syllableSpans, smoothTimeMs)

            // 3. 柔和长音辉光与微呼吸缩放（仅逐字模式完整达标词启用，强度清透克制，C∞ 余弦全程无折角平滑进出）
            var empScale = 1f
            var glowAlpha = 0f
            val emp = seg.emphasizeParams
            if (seg.shouldEmphasize && emp != null) {
                val empStartMs = w.startTime.toFloat()
                val empT = (smoothTimeMs - empStartMs) / emp.durationMs
                if (empT in 0f..1f) {
                    val e = amllEmpEasing(empT) * activeBlend
                    // 轻微克制的呼吸膨胀（约 1.8%~4.0%），与柔和上浮自然叠加
                    empScale = 1f + 0.040f * emp.amount * e
                    yOffset -= floatMaxPx * 0.38f * emp.amount * e
                    // 峰值透明度严格控制在清透柔和区间（<= 0.38f），依靠原生高斯阴影底层漫射，绝不过曝刺眼
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
