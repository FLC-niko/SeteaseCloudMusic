package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.seteasecloudmusic.core.common.runCatchingCancellable
import com.example.seteasecloudmusic.feature.player.domain.model.LyricLine
import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Apple Music 风格歌词列表 —— 与 AMLL（applemusic-like-lyrics）逐像素对齐。
 *
 * 数值全部来自对 WebView 内 AMLL 的**实机测量**（通过 Chrome DevTools Protocol 读取
 * `getComputedStyle`，并用截图像素做校验），推导过程见 [AmllLyricSpec]。
 *
 * 三条决定"同款观感"的关键结论：
 * 1. WebView 里 `vh` 单位**恒为 0**，因此 AMLL 的 `clamp(30px, 4.2vh, 42px)` 永远落在下限：
 *    主歌词恒为 30 CSS px、翻译行恒为 15 CSS px，不随屏幕高度缩放。
 * 2. 逐字高亮的 `mask-image` 里是 `NaN%`（渐变非法 ⇒ 计算值 `none`），
 *    因此 AMLL 的激活行是**整行均匀纯色**，没有任何扫光。
 * 3. 层次感**完全来自模糊**而非透明度：非激活行是"满亮度 + 越远越糊"，
 *    激活行反而只有 0.85 不透明度。
 *
 * 因此本实现：整行单色绘制 + 按 AMLL 公式施加模糊 + 文本按 AMLL 宽度自动换行。
 */
@Composable
fun LyricsColumn(
    lyrics: ParsedLyrics,
    activeLineIndex: Int,
    currentTimeMs: Int,
    isPlaying: Boolean,
    onLineClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()

    // 字号：按 `CSS px → dp → sp` 换算。中间过一道 dp 是为了让最终的**物理像素**
    // 与 WebView 的 CSS px 完全一致，即使系统字体缩放被调过也不会跑偏。
    val mainFontSize = with(density) { AmllLyricSpec.MAIN_FONT_SIZE_PX.dp.toSp() }
    // 注意：这里用的是**实际渲染行距**（1.4em），而不是 CSS 的 line-height（1.22em）——
    // AMLL 的词 span 是 inline-block，行盒叠了 strut，实测换行间距就是 1.4em。
    val mainLineHeight = with(density) {
        (AmllLyricSpec.MAIN_FONT_SIZE_PX * AmllLyricSpec.MAIN_ROW_PITCH_EM).dp.toSp()
    }
    val subFontSize = with(density) { AmllLyricSpec.SUB_FONT_SIZE_PX.dp.toSp() }
    val subLineHeight = with(density) {
        (AmllLyricSpec.SUB_FONT_SIZE_PX * AmllLyricSpec.SUB_LINE_HEIGHT).dp.toSp()
    }
    val letterSpacing = with(density) {
        (AmllLyricSpec.MAIN_FONT_SIZE_PX * AmllLyricSpec.LETTER_SPACING_EM).dp.toSp()
    }

    val mainStyle = remember(mainFontSize, mainLineHeight, letterSpacing) {
        TextStyle(
            fontSize = mainFontSize,
            lineHeight = mainLineHeight,
            fontWeight = FontWeight.Black,
            letterSpacing = letterSpacing
        )
    }
    val subStyle = remember(subFontSize, subLineHeight) {
        TextStyle(
            fontSize = subFontSize,
            lineHeight = subLineHeight,
            fontWeight = FontWeight.SemiBold
        )
    }

    val mainLineHeightPx = with(density) { mainLineHeight.toPx() }
    // AMLL "盒子高" 的推导基准：CSS line-height（1.22em）+ 上下内边距
    val cssLineHeightPx = with(density) {
        (AmllLyricSpec.MAIN_FONT_SIZE_PX * AmllLyricSpec.MAIN_LINE_HEIGHT).dp.toPx()
    }
    val linePaddingYPx = with(density) { AmllLyricSpec.LINE_PADDING_Y.toPx() }
    val isNarrowViewport = screenWidthDp < AmllLyricSpec.NARROW_VIEWPORT_WIDTH_DP

    val listState = rememberLazyListState()
    val measurer = rememberTextMeasurer(cacheSize = 512)

    // ── 用户手动滚动优先：拖动时暂停自动对齐，松手一段时间后恢复 ──
    var userScrolling by remember { mutableStateOf(false) }
    var autoFollow by remember { mutableStateOf(true) }

    LaunchedEffect(userScrolling) {
        if (userScrolling) {
            autoFollow = false
        } else {
            delay(AmllLyricSpec.AUTO_ALIGN_RESUME_DELAY_MS)
            autoFollow = true
        }
    }

    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> userScrolling = true
                is DragInteraction.Stop, is DragInteraction.Cancel -> userScrolling = false
                else -> Unit
            }
        }
    }

    // 已测量过的行高，用于估算焦点切换时需要补间的像素距离
    val measuredLineHeights = remember { mutableStateMapOf<Int, Int>() }

    // 弹簧残差：列表先瞬移到目标位置，视觉上再"退回旧位置"，由弹簧归零 → 平滑滚动
    val scrollResidual = remember { Animatable(0f) }

    LaunchedEffect(activeLineIndex, autoFollow, lyrics.lines.size) {
        if (!autoFollow || activeLineIndex !in lyrics.lines.indices) return@LaunchedEffect

        val fallbackStep = max(
            AmllLyricSpec.LINE_STEP_FALLBACK_MIN_PX,
            mainLineHeightPx * AmllLyricSpec.LINE_STEP_FALLBACK_EM
        )
        val delta = estimateScrollDelta(
            from = listState.firstVisibleItemIndex,
            to = activeLineIndex,
            measured = measuredLineHeights,
            fallbackStep = fallbackStep
        )

        if (abs(delta) > AmllLyricSpec.SNAP_THRESHOLD_PX) {
            // 大跨度（点击了很远的一行 / seek）：直接瞬移，避免看到"飞过"整段列表
            scrollResidual.snapTo(0f)
            runCatchingCancellable { listState.scrollToItem(activeLineIndex) }
        } else {
            // 瞬移会带来一次视觉跳变，先用残差把它抵消（画面拉回旧位置），再让弹簧归零：
            // 视觉上是一次无过冲的过阻尼平滑滚动——新行从下方滑入、旧行向上移出。
            // 注意符号必须为 +delta（与列表滚动方向相反），写成 -delta 会出现"先向上跳过目标、再反向滑回"的抖动。
            scrollResidual.snapTo(delta)
            runCatchingCancellable { listState.scrollToItem(activeLineIndex) }
            scrollResidual.animateTo(0f, AmllLyricSpec.SCROLL_SPRING)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // AMLL：焦点行的中心落在视口 35% 高度处（LayoutConfig.alignPosition = 0.35）
        val topPadding = with(density) {
            (maxHeight.toPx() * AmllLyricSpec.ALIGN_POSITION - mainLineHeightPx / 2f)
                .coerceAtLeast(0f)
                .toDp()
        }
        // AMLL 主行可用宽度 = 视口宽 − 2 × 行内边距（文本在这里换行）
        val contentWidthPx = with(density) {
            (maxWidth - AmllLyricSpec.LINE_PADDING_X * 2).coerceAtLeast(0.dp).roundToPx()
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = scrollResidual.value },
            contentPadding = PaddingValues(top = topPadding, bottom = maxHeight)
        ) {
            itemsIndexed(
                items = lyrics.lines,
                key = { index, line -> "${line.startTime}_$index" }
            ) { index, line ->
                // 背景人声行不参与主歌词流
                if (line.isBG) return@itemsIndexed

                LyricLineItem(
                    line = line,
                    distanceFromActive = index - activeLineIndex,
                    isDuet = line.isDuet,
                    isPlaying = isPlaying,
                    measurer = measurer,
                    mainStyle = mainStyle,
                    subStyle = subStyle,
                    contentWidthPx = contentWidthPx,
                    cssLineHeightPx = cssLineHeightPx,
                    linePaddingYPx = linePaddingYPx,
                    isNarrowViewport = isNarrowViewport,
                    onMeasured = { height -> measuredLineHeights[index] = height },
                    onClick = { onLineClick?.invoke(line.startTime) }
                )
            }
        }
    }
}

/**
 * 单行歌词，对应 AMLL 的 `.lyricLine`：
 * - 内边距 `5px 18px`（App 的 CSS 覆盖值）；
 * - 主行 `rgb(237,238,240)`、激活时（且播放中）`opacity 0.85`；
 * - 字内换行行距 1.4em（AMLL 的 inline-block strut 效应，实测值）；
 * - 翻译行 15px / 1.2em / `opacity 0.75`，与主行间距 `gap 0.3em + margin-top 4px`；
 * - 模糊：焦点 0，其后的第 d 行 `(1+d)`，其前的行 `(2+|d|)`，窄视口 ×0.8，上限 5px。
 */
@Composable
private fun LyricLineItem(
    line: LyricLine,
    distanceFromActive: Int,
    isDuet: Boolean,
    isPlaying: Boolean,
    measurer: TextMeasurer,
    mainStyle: TextStyle,
    subStyle: TextStyle,
    contentWidthPx: Int,
    cssLineHeightPx: Float,
    linePaddingYPx: Float,
    isNarrowViewport: Boolean,
    onMeasured: (Int) -> Unit,
    onClick: () -> Unit
) {
    val text = remember(line) { line.words.joinToString("") { it.word } }
    if (text.isBlank()) return

    val density = LocalDensity.current
    val isActive = distanceFromActive == 0

    // ── 文本布局：按 AMLL 的可用宽度测量，因此**换行位置与 WebView 一致** ──
    val layout: TextLayoutResult = remember(text, measurer, mainStyle, contentWidthPx) {
        measurer.measure(
            text = AnnotatedString(text),
            style = mainStyle,
            constraints = Constraints(maxWidth = contentWidthPx.coerceAtLeast(1)),
            overflow = TextOverflow.Clip
        )
    }
    val textHeightPx = layout.size.height.toFloat()

    // ── 亮度 / 模糊 ──
    // AMLL 实测：只有**播放中**的激活行会降不透明度到 0.85 并被豁免模糊；
    // 暂停时激活行与其它行一样满亮度，只是模糊量少了"豁免"那一档（0.8px）。
    val isFocused = isActive && isPlaying
    val mainAlpha = if (isFocused) {
        AmllLyricSpec.ACTIVE_LINE_ALPHA
    } else {
        AmllLyricSpec.INACTIVE_LINE_ALPHA
    }

    // 模糊：焦点行 0；其后第 d 行 (1+d)，其前第 d 行 (2+|d|)；窄视口整体 ×0.8；上限 5px
    val blurPx = if (isFocused) {
        0f
    } else {
        val distance = if (distanceFromActive < 0) {
            abs(distanceFromActive) + 1
        } else {
            distanceFromActive
        }
        min(
            AmllLyricSpec.MAX_BLUR_PX,
            (1 + distance).toFloat() *
                    (if (isNarrowViewport) AmllLyricSpec.NARROW_BLUR_FACTOR else 1f)
        )
    }

    // 上一次上报过的行高（普通引用，避免 layout 阶段写 snapshot state 造成多余失效）
    val reportedHeight = remember { intArrayOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                // 行间槽位：AMLL 的行既是文档流（占一个"盒子高"）又带一个
                // translateY(盒子高 × 0.72) 的位移，两者叠加后相邻行距离 = 盒子高 × 1.72。
                // 盒子高按 CSS line-height 推算，因此换行行数用 layout.lineCount。
                val boxHeightPx = linePaddingYPx * 2 + cssLineHeightPx * layout.lineCount
                val slot = (boxHeightPx * AmllLyricSpec.LINE_PITCH_MULTIPLIER).roundToInt()
                if (reportedHeight[0] != slot) {
                    reportedHeight[0] = slot
                    onMeasured(slot)
                }
                layout(placeable.width, slot) {
                    placeable.place(0, 0)
                }
            }
            .padding(horizontal = AmllLyricSpec.LINE_PADDING_X)
            .padding(vertical = AmllLyricSpec.LINE_PADDING_Y)
            .then(
                if (blurPx > 0.01f) {
                    Modifier.blur(radius = blurPx.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                } else {
                    Modifier
                }
            )
            .graphicsLayer {
                // AMLL `.lyricLine { transform-origin: left }`；对唱行靠右
                transformOrigin = if (isDuet) TransformOrigin(1f, 0.5f) else TransformOrigin(0f, 0.5f)
            }
            .clickable(onClick = onClick),
        horizontalAlignment = if (isDuet) Alignment.End else Alignment.Start
    ) {
        // ── 主歌词行（整行单色，与 AMLL 激活行的实际渲染一致）──
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { textHeightPx.toDp() })
                .drawBehind {
                    drawText(
                        textLayoutResult = layout,
                        color = AmllLyricSpec.TEXT_COLOR.copy(alpha = mainAlpha),
                        topLeft = Offset.Zero
                    )
                }
        )

        // ── 翻译行 ──
        if (line.translatedLyric.isNotBlank()) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AmllLyricSpec.MAIN_SUB_GAP)
            )
            Text(
                text = line.translatedLyric,
                style = subStyle,
                color = AmllLyricSpec.TEXT_COLOR.copy(alpha = AmllLyricSpec.SUB_LINE_ALPHA),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 估算从 [from] 行滚到 [to] 行需要的像素位移。
 *
 * 已测量的行用真实高度，未进入过视野的行用 AMLL 的兜底步进近似；
 * 误差只影响一次补间的起点，弹簧会在随后的帧里把这个误差平滑吃掉。
 */
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
