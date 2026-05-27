package com.example.seteasecloudmusic.feature.player.presentation.lyric

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

val lyricEasing = CubicBezierEasing(0.75f, 0.0f, 0.25f, 1.0f)

/**
 * FlamingoLyricView - Apple Music 风格歌词组件
 *
 * @param lyrics 解析后的歌词数据，格式为 List<List<Pair<时间戳(ms), 文本>>>
 * @param sideFlags 对唱标记，true 表示该句靠右对齐
 * @param currentTimeMs 当前播放进度（毫秒）
 * @param onSeek 点击歌词行时的跳转回调
 * @param translationEnabled 是否显示翻译
 * @param blurEnabled 是否启用非当前行模糊
 * @param isCompact 是否使用紧凑模式（无歌词时高度比例）
 * @param uiConfig UI 配置
 * @param modifier Modifier
 * @param onEmptyAreaClick 点击空白区域回调
 */
@Composable
fun FlamingoLyricView(
    lyrics: List<List<Pair<Float, String>>>,
    sideFlags: List<Boolean> = emptyList(),
    currentTimeMs: () -> Int,
    onSeek: (Int) -> Unit,
    translationEnabled: Boolean = true,
    blurEnabled: Boolean = false,
    isCompact: Boolean = false,
    uiConfig: LyricUIConfig = LyricUIConfig(),
    modifier: Modifier = Modifier,
    onEmptyAreaClick: () -> Unit = {}
) {

    val context = LocalContext.current
    val mainTextBasicColor = Color(uiConfig.mainTextBasicColor)
    val subTextBasicColor = Color(uiConfig.subTextBasicColor)

    val mainTextStyle = rememberLyricTextStyle(uiConfig)

    if (lyrics.isEmpty() || sideFlags.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxHeight(if (isCompact) 0.56f else 1f)
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onEmptyAreaClick()
                }
        ) {
            Text(
                text = uiConfig.noLrcText,
                fontSize = 18.sp,
                color = mainTextBasicColor
            )
        }
    } else {
        val scrollState = rememberLazyListState()
        val currentLyricIndex = remember { mutableIntStateOf(-1) }

        // 内部同步当前歌词行
        LaunchedEffect(lyrics) {
            currentLyricIndex.intValue = -1
        }

        LaunchedEffect(Unit) {
            while (isActive) {
                if (lyrics.isNotEmpty()) {
                    val liveTime = currentTimeMs()
                    val nextIndex = lyrics.indexOfFirst { line ->
                        line.first().first > liveTime
                    }
                    val newIndex = when {
                        nextIndex > 0 -> nextIndex - 1
                        nextIndex == 0 -> -1
                        else -> lyrics.size - 1
                    }
                    if (newIndex >= 0 && newIndex != currentLyricIndex.intValue) {
                        currentLyricIndex.intValue = newIndex
                    }
                }
                delay(70L)
            }
        }

        val blankSpacer: (LazyListScope.() -> Unit) = {
            item {
                Box(modifier = Modifier.height(uiConfig.blankHeight.dp)) {}
            }
        }

        val enableLyricScroll = remember { mutableStateOf(true) }
        val height = rememberSaveable(key = "FlamingoLyricView_height") { mutableIntStateOf(0) }

        val targetWeight = 0.0618f
        val targetOffset = rememberSaveable(height.intValue, key = "FlamingoLyricView_targetOffset") {
            height.intValue * targetWeight
        }

        val space = 0.dp
        val measurer = rememberTextMeasurer(cacheSize = 32)

        val visibleItems = remember {
            derivedStateOf { scrollState.layoutInfo.visibleItemsInfo }
        }
        val targetItem = remember {
            derivedStateOf {
                visibleItems.value.find { it.index == currentLyricIndex.intValue + 1 }
            }
        }
        val currentOffset = remember(targetOffset) {
            derivedStateOf { targetItem.value?.offset ?: targetOffset.toInt() }
        }
        val scrollDistance = remember(targetOffset) {
            derivedStateOf { currentOffset.value - targetOffset }
        }
        val nowFirst = remember {
            derivedStateOf { scrollState.firstVisibleItemIndex }
        }
        val supportBlur = rememberSaveable(key = "supportBlur") {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        }

        val isUserScrolling = remember { mutableStateOf(false) }
        val nestedScrollConnection = remember {
            @Stable
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    isUserScrolling.value = true
                    return Offset.Zero
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity
                ): Velocity {
                    isUserScrolling.value = false
                    return super.onPostFling(consumed, available)
                }
            }
        }

        LaunchedEffect(isUserScrolling.value) {
            if (isUserScrolling.value) {
                enableLyricScroll.value = false
            } else {
                delay(1600)
                enableLyricScroll.value = true
            }
        }

        LazyColumn(
            state = scrollState,
            contentPadding = PaddingValues(vertical = 16.dp),
            modifier = modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onEmptyAreaClick()
                }
                .nestedScroll(nestedScrollConnection)
                .onSizeChanged {
                    if (height.intValue == 0 && it.height != 0) {
                        height.intValue = it.height
                    }
                }
        ) {
            blankSpacer()
            itemsIndexed(
                items = lyrics,
                key = { _, lines -> lines }
            ) { index, lines ->
                val isCurrent = remember(lines) {
                    derivedStateOf { index == currentLyricIndex.intValue }
                }
                val isTop = remember(lines) {
                    derivedStateOf { index == (currentLyricIndex.intValue - 1) }
                }
                val showStateAnimation = remember(index) {
                    derivedStateOf {
                        (currentLyricIndex.intValue in scrollState.layoutInfo.visibleItemsInfo.map { it.index - 1 }
                            && currentLyricIndex.intValue >= 0) && enableLyricScroll.value
                    }
                }
                val isLyricEmpty = remember(lines) {
                    lines.all { it.second.isBlank() }
                }

                val translation = remember(index) {
                    val str = lines.last().second
                    str.ifBlank { null }
                }
                val blur = remember(index) {
                    derivedStateOf {
                        if (!showStateAnimation.value || index == currentLyricIndex.intValue || !blurEnabled || !supportBlur) {
                            0f
                        } else {
                            (abs(index - currentLyricIndex.intValue) * 3.5f).coerceAtMost(12f)
                        }
                    }
                }
                val otherSide = remember(index) {
                    sideFlags.getOrElse(index) { false }
                }

                LyricItem(
                    isCurrentLambda = { isCurrent.value },
                    isTopLambda = { isTop.value },
                    mainLyric = lines.dropLast(1),
                    translation = translation,
                    showTranslation = translationEnabled,
                    mainTextStyle = mainTextStyle,
                    subTextSize = uiConfig.subTextSize,
                    blur = { blur.value },
                    mainTextBasicColor = mainTextBasicColor,
                    subTextBasicColor = subTextBasicColor,
                    measurer = measurer,
                    isLyricEmpty = { isLyricEmpty },
                    nextTime = {
                        if (index + 1 > lyrics.size - 1) {
                            0f
                        } else {
                            lyrics[(index + 1)].first().first
                        }
                    },
                    otherSide = otherSide,
                    liveTimeMs = currentTimeMs,
                    onClick = {
                        LyricVibrator.doubleClick(context)
                        currentLyricIndex.intValue = index
                        onSeek(lines.first().first.toInt())
                    }
                )

                val show = remember(index) {
                    derivedStateOf { !isLyricEmpty || isCurrent.value }
                }

                val thisScrollDistance = if (targetItem.value != null) {
                    (scrollDistance.value / (visibleItems.value.size)).toDp()
                } else {
                    0.dp
                }

                val thisTargetHeight = remember(index) { mutableStateOf(space) }

                LaunchedEffect(currentLyricIndex.intValue) {
                    if (visibleItems.value.isEmpty()) {
                        return@LaunchedEffect
                    }
                    if (index >= currentLyricIndex.intValue - 1 && showStateAnimation.value && show.value) {
                        val weight = (1f - ((index - (nowFirst.value)) / visibleItems.value.size))
                        delay((550 * (1f - weight)).toLong())
                        thisTargetHeight.value = (thisScrollDistance * weight).plus(space)
                        delay(((550 / 1.95f) * weight).toLong())
                        thisTargetHeight.value = space
                    } else if (show.value) {
                        thisTargetHeight.value = space
                    } else {
                        thisTargetHeight.value = 0.dp
                    }
                }

                val offset = animateDpAsState(
                    targetValue = thisTargetHeight.value,
                    animationSpec = if (thisTargetHeight.value == 0.dp || thisTargetHeight.value == space) {
                        spring(
                            stiffness = 105F,
                            dampingRatio = 1f,
                            visibilityThreshold = 0.0001.dp
                        )
                    } else {
                        tween(durationMillis = 550, easing = lyricEasing)
                    }
                )

                Spacer(modifier = Modifier.height(offset.value))
            }
            blankSpacer()
            item("extra_blank") {
                Spacer(modifier = Modifier.height(500.dp))
            }
        }

        LaunchedEffect(currentLyricIndex.intValue, translationEnabled) {
            try {
                if (enableLyricScroll.value) {
                    if (
                        try {
                            if (currentLyricIndex.intValue - 1 < 0) false
                            else (
                                (lyrics[(currentLyricIndex.intValue - 1)][1].second.isBlank())
                            )
                        } catch (_: Exception) {
                            false
                        }
                    ) {
                        return@LaunchedEffect
                    }

                    if (targetItem.value != null) {
                        scrollState.animateScrollBy(
                            scrollDistance.value,
                            animationSpec = tween(
                                durationMillis = 550,
                                easing = lyricEasing
                            )
                        )
                    } else {
                        scrollState.animateScrollToItem(
                            index = (currentLyricIndex.intValue + 1).coerceAtLeast(0),
                            scrollOffset = -targetOffset.toInt()
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }

        LaunchedEffect(Unit) {
            try {
                if (currentLyricIndex.intValue != -1) {
                    return@LaunchedEffect
                }
                val liveTime = currentTimeMs()
                val nextIndex = lyrics.indexOfFirst { line ->
                    line.first().first > liveTime
                }

                if (nextIndex != -1 && nextIndex - 1 != currentLyricIndex.intValue) {
                    scrollState.scrollToItem(
                        index = (nextIndex).coerceAtLeast(0),
                        scrollOffset = -targetOffset.toInt()
                    )
                    currentLyricIndex.intValue = nextIndex - 1
                } else if (nextIndex == -1 && currentLyricIndex.intValue != lyrics.size - 1) {
                    scrollState.scrollToItem(
                        index = (lyrics.size).coerceAtLeast(0),
                        scrollOffset = -targetOffset.toInt()
                    )
                    currentLyricIndex.intValue = lyrics.size - 1
                }
            } catch (_: Exception) {
            }
        }
    }
}

@Composable
private fun Float.toDp(): Dp {
    val density = LocalDensity.current
    return (this / density.density).dp
}

@Composable
private fun LazyItemScope.Line(
    lines: List<Pair<Float, String>>,
    style: TextStyle,
    measurer: TextMeasurer,
    modifier: Modifier,
    viewAlign: Alignment.Horizontal,
    draw: CacheDrawScope.(Constraints, TextLayoutResult) -> DrawResult
) {
    val styledString = remember(style, lines) {
        buildString {
            lines.forEach { char ->
                if (char.second.isNotEmpty()) {
                    append(char.second)
                }
            }
        }
    }

    Column(
        horizontalAlignment = viewAlign,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
    ) {
        SubcomposeLayout(modifier = modifier) { constraints ->
            val measureResult = measurer.measure(
                text = styledString,
                style = style,
                constraints = Constraints(minWidth = 0, maxWidth = constraints.maxWidth),
                layoutDirection = LayoutDirection.Ltr
            )

            val height = (style.lineHeight * measureResult.lineCount)
            val width = runCatching {
                (0 until measureResult.lineCount).maxOf {
                    measureResult.getBoundingBox(
                        measureResult.getLineEnd(it, visibleEnd = true) - 1
                    ).right
                }
            }.getOrDefault(constraints.maxWidth.toFloat())

            val content = subcompose(lines) {
                Spacer(
                    Modifier
                        .fillMaxSize()
                        .drawWithCache { draw(constraints, measureResult) }
                )
            }.first()

            val placeable = content.measure(
                Constraints.fixed(width.roundToInt(), height.roundToPx())
            )

            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
    }
}

val easing: Easing = EaseInOutQuad

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun LazyItemScope.LyricItem(
    isCurrentLambda: () -> Boolean,
    isTopLambda: () -> Boolean,
    mainLyric: List<Pair<Float, String>>,
    translation: String?,
    showTranslation: Boolean,
    mainTextStyle: TextStyle,
    subTextSize: Int,
    blur: () -> Float,
    mainTextBasicColor: Color,
    subTextBasicColor: Color,
    measurer: TextMeasurer,
    isLyricEmpty: () -> Boolean,
    nextTime: () -> Float,
    otherSide: Boolean,
    liveTimeMs: () -> Int,
    onClick: () -> Unit
) {


    val viewAlign = if (otherSide) Alignment.End else Alignment.Start
    val focusedColor = Color(0xFFFFFFFF)
    val unfocusedColor = Color(0x2EFFFFFF)
    val unfocusedSolidBrush = SolidColor(unfocusedColor)

    val isNotOneByOne = remember(mainLyric) {
        mainLyric.all { it.first == mainLyric.firstOrNull()?.first }
    }

    val liveTime = remember(mainLyric) { mutableIntStateOf(liveTimeMs()) }

    val launch = remember(mainLyric) {
        derivedStateOf {
            isLyricEmpty() || !isNotOneByOne
        }
    }
    if (launch.value) {
        LaunchedEffect(Unit) {
            while (isActive) {
                withContext(Dispatchers.Main) {
                    liveTime.intValue = liveTimeMs()
                }
                delay(10L)
            }
        }
    }

    Column(
        Modifier.padding(horizontal = 9.dp),
        horizontalAlignment = viewAlign
    ) {
        val otherSideAnimate = if (otherSide) {
            TransformOrigin(1f, 0.25f)
        } else {
            TransformOrigin(0f, 0.25f)
        }

        val otherSideTransformOrigin = if (otherSide) {
            TransformOrigin(1f, 0.5f)
        } else {
            TransformOrigin(0f, 0.5f)
        }

        val tweenSpecWithDelay: AnimationSpec<Float> = remember(mainLyric) {
            TweenSpec(durationMillis = 270, easing = lyricEasing, delay = 110)
        }
        val tweenSpecWithoutDelay: AnimationSpec<Float> = remember(mainLyric) {
            TweenSpec(durationMillis = 300, easing = lyricEasing, delay = 45)
        }

        val scale = animateFloatAsState(
            targetValue = if (isCurrentLambda()) 1.005f else 1f,
            animationSpec = if (isCurrentLambda()) tweenSpecWithDelay else tweenSpecWithoutDelay
        )

        val cardPadding = if (otherSide) {
            Modifier.padding(start = 28.dp)
        } else {
            Modifier.padding(end = 28.dp)
        }

        if (isLyricEmpty()) {
            Column(Modifier.animateContentSize()) {
                val percent = remember(mainLyric) {
                    derivedStateOf {
                        val m = mainLyric.first().first
                        ((liveTime.intValue - m).coerceAtLeast(0f) / (nextTime() - m)).coerceAtMost(1f)
                    }
                }
                val show = remember(mainLyric) {
                    derivedStateOf { (isLyricEmpty() && isCurrentLambda() && percent.value != 0f) }
                }
                AnimatedVisibility(
                    visible = show.value,
                    enter = fadeIn(
                        animationSpec = TweenSpec(
                            durationMillis = 550,
                            easing = lyricEasing,
                            delay = 300
                        )
                    ) + scaleIn(
                        initialScale = 0.85f,
                        transformOrigin = otherSideAnimate,
                        animationSpec = TweenSpec(
                            durationMillis = 550,
                            easing = lyricEasing,
                            delay = 300
                        )
                    ),
                    exit = fadeOut() + scaleOut(
                        targetScale = 0.85f,
                        transformOrigin = otherSideAnimate,
                        animationSpec = TweenSpec(
                            durationMillis = 340,
                            easing = lyricEasing
                        )
                    )
                ) {
                    LyricCard(
                        { scale.value },
                        cardPadding,
                        otherSideTransformOrigin,
                        viewAlign
                    ) {
                        Column(
                            Modifier
                                .padding(start = 20.dp, end = 20.dp)
                                .padding(top = 8.dp, bottom = 10.dp),
                            horizontalAlignment = viewAlign
                        ) {
                            CountdownAnimation(
                                { percent.value },
                                colorLambda = { mainTextBasicColor }
                            )
                        }
                    }
                }
            }
        } else {
            LyricCard(
                { scale.value },
                cardPadding,
                otherSideTransformOrigin,
                viewAlign
            ) {
                val blurValue = animateDpAsState(
                    blur().dp,
                    SnapSpec(delay = if (isTopLambda()) 260 else 0)
                )

                val blurModifier = remember(mainLyric) {
                    derivedStateOf {
                        val thisBlur = blur()
                        if (thisBlur == 0f) {
                            Modifier
                        } else {
                            Modifier.blur(
                                blurValue.value,
                                edgeTreatment = BlurredEdgeTreatment.Unbounded
                            )
                        }
                    }
                }

                Column(
                    Modifier
                        .then(blurModifier.value)
                        .fillMaxWidth(),
                    horizontalAlignment = viewAlign
                ) {
                    val textAlign = if (otherSide) TextAlign.End else TextAlign.Start

                    val alphaTweenSpecWithDelay: AnimationSpec<Float> = remember(mainLyric) {
                        TweenSpec(durationMillis = 350, easing = lyricEasing, delay = 145)
                    }
                    val alphaTweenSpecWithoutDelay: AnimationSpec<Float> = remember(mainLyric) {
                        TweenSpec(durationMillis = 350, easing = lyricEasing, delay = 80)
                    }

                    val thisAlphaAnimated = animateFloatAsState(
                        targetValue = if (isCurrentLambda()) 1f else 0.18f,
                        animationSpec = if (isCurrentLambda()) alphaTweenSpecWithDelay else alphaTweenSpecWithoutDelay
                    )

                    val thisAlpha = remember(mainLyric) {
                        derivedStateOf {
                            if (isNotOneByOne) {
                                thisAlphaAnimated.value
                            } else {
                                1f
                            }
                        }
                    }

                    val otherSidePadding = remember(mainLyric) {
                        derivedStateOf {
                            if (otherSide) {
                                Modifier.padding(
                                    start = 20.dp,
                                    end = if (mainLyric.lastOrNull()?.second?.endsWith("：") == true) 3.dp else 20.dp
                                )
                            } else {
                                Modifier.padding(start = 20.dp, end = 20.dp)
                            }
                        }
                    }

                    val showHighLight = remember(mainLyric) {
                        derivedStateOf {
                            if (isNotOneByOne) {
                                true
                            } else {
                                liveTime.intValue >= mainLyric[mainLyric.size - (if (translation != null) 3 else 1)].first
                            }
                        }
                    }

                    Line(
                        lines = mainLyric,
                        style = if (otherSide) mainTextStyle.copy(textAlign = TextAlign.End) else mainTextStyle,
                        measurer = measurer,
                        modifier = Modifier
                            .graphicsLayer {
                                this.alpha = thisAlpha.value
                                compositingStrategy = CompositingStrategy.ModulateAlpha
                            }
                            .padding(vertical = 4.dp)
                            .then(otherSidePadding.value)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                onClick()
                            },
                        viewAlign = viewAlign
                    ) { _, measureResult ->
                        if (isNotOneByOne) {
                            return@Line onDrawWithContent {
                                drawText(textLayoutResult = measureResult, color = focusedColor)
                            }
                        }

                        if (!isCurrentLambda()) {
                            if (showHighLight.value) {
                                return@Line onDrawWithContent {
                                    drawText(
                                        textLayoutResult = measureResult,
                                        color = focusedColor,
                                        topLeft = Offset(0F, -4F)
                                    )
                                }
                            } else {
                                return@Line onDrawWithContent {
                                    drawText(
                                        textLayoutResult = measureResult,
                                        color = unfocusedColor
                                    )
                                }
                            }
                        }

                        // 以下为逐字处理
                        var sum = 0
                        var lastTime = 0f
                        val wordsToDraw = arrayListOf<DrawWord>()
                        var averageTime = 0f
                        lastTime = mainLyric.first().first

                        mainLyric.fastForEachIndexed { wordIndex, word ->
                            val thisWord = word.second
                            if (thisWord.isEmpty()) {
                                return@fastForEachIndexed
                            }

                            averageTime = (word.first - lastTime) / thisWord.length

                            val thisWordGroupLastTime = if (wordIndex - 1 < 0) {
                                mainLyric.first().first
                            } else {
                                mainLyric[(wordIndex - 1)].first
                            }
                            val groupPercent = if ((word.first - thisWordGroupLastTime) == 0f) {
                                0f
                            } else {
                                ((liveTime.intValue - thisWordGroupLastTime).coerceAtLeast(0f)
                                    / (word.first - thisWordGroupLastTime)).coerceIn(0f, 1f)
                            }
                            val easedPercent = easing.transform(groupPercent.coerceIn(0f, 1f))
                            val topLeftWeight = 4 * easedPercent

                            thisWord.forEach { char ->
                                val charWord = char.toString()
                                val layout = measurer.measure(
                                    text = charWord,
                                    style = if (otherSide) mainTextStyle.copy(textAlign = TextAlign.End) else mainTextStyle,
                                    constraints = measureResult.layoutInput.constraints
                                )

                                val thisWordLastTime = lastTime
                                val thisWordAverageTime = averageTime

                                wordsToDraw += DrawWord(
                                    time = lastTime + averageTime,
                                    word = charWord,
                                    layout = layout,
                                    topLeft = measureResult.getBoundingBox(
                                        sum.coerceAtMost(mainLyric.sumOf { it.second.length } - 1)
                                            .coerceAtLeast(0)
                                    ).topLeft.minus(Offset(0F, topLeftWeight)),
                                    brush = { px, percent ->
                                        if (thisWord == " ") {
                                            return@DrawWord unfocusedSolidBrush
                                        }

                                        val beforeColor = if (percent <= -0.5f) {
                                            unfocusedColor
                                        } else {
                                            focusedColor
                                        }

                                        val afterColor = if (percent >= 1f) {
                                            focusedColor
                                        } else {
                                            unfocusedColor
                                        }
                                        Brush.horizontalGradient(
                                            0f to beforeColor,
                                            (percent - px).coerceIn(0f, 1f) to beforeColor,
                                            (percent + px).coerceIn(0f, 1f) to afterColor
                                        )
                                    },
                                    percent = {
                                        if (thisWord == " ") {
                                            return@DrawWord 0f
                                        }
                                        ((liveTime.intValue - thisWordLastTime) / thisWordAverageTime)
                                    }
                                ).also {
                                    sum += charWord.length
                                    lastTime += averageTime
                                }
                            }
                        }

                        onDrawBehind {
                            wordsToDraw.fastForEach { l ->
                                drawText(
                                    textLayoutResult = l.layout,
                                    topLeft = l.topLeft,
                                    brush = l.brush(0.3f, l.percent())
                                )
                            }
                        }
                    }

                    AnimatedVisibility(showTranslation && translation != null) {
                        translation?.let {
                            val translationAlpha = animateFloatAsState(
                                targetValue = if (isCurrentLambda()) 0.5f else 0.18f,
                                animationSpec = if (isCurrentLambda()) alphaTweenSpecWithDelay else alphaTweenSpecWithoutDelay
                            )

                            val translationOtherSidePadding = if (otherSide) {
                                Modifier.padding(start = 20.dp, end = 20.dp)
                            } else {
                                Modifier.padding(start = 20.dp, end = 20.dp)
                            }

                            Text(
                                text = it,
                                fontSize = subTextSize.sp,
                                color = subTextBasicColor,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier
                                    .graphicsLayer {
                                        this.alpha = translationAlpha.value
                                        compositingStrategy = CompositingStrategy.ModulateAlpha
                                    }
                                    .then(translationOtherSidePadding)
                                    .padding(top = 5.dp),
                                lineHeight = (subTextSize + 5).sp,
                                letterSpacing = 0.3.sp,
                                textAlign = textAlign
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricCard(
    scale: () -> Float,
    cardPadding: Modifier,
    otherSideTransformOrigin: TransformOrigin,
    viewAlign: Alignment.Horizontal,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .graphicsLayer {
                val scaleValue = scale()
                scaleX = scaleValue
                scaleY = scaleValue
                transformOrigin = otherSideTransformOrigin
            }
            .fillMaxWidth()
            .then(cardPadding)
            .padding(top = 9.dp, bottom = 9.dp),
        horizontalAlignment = viewAlign
    ) {
        content()
    }
}

@Composable
fun CountdownAnimation(progress: () -> Float, colorLambda: () -> Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale = infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = lyricEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            alpha = 0.8f
        },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 5.dp)
        ) {
            for (i in 1..3) {
                val average = 1f / 3f
                val beforePadding = (i - 1) * average
                val thisPercent = (progress() - beforePadding) / ((i * average) - beforePadding)
                val alpha = 0.2f + (0.8f * thisPercent).coerceIn(0f, 0.8f)

                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .background(
                            colorLambda().copy(alpha = alpha),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
internal fun rememberLyricTextStyle(config: LyricUIConfig): TextStyle {
    return remember(config.mainTextSize, config.fontWeight, config.lineBalance, config.letterSpacing) {
        TextStyle(
            fontSize = config.mainTextSize.sp,
            lineHeight = (config.mainTextSize + 8).sp,
            fontWeight = config.fontWeight,
            letterSpacing = config.letterSpacing.sp,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None
            ),
            lineBreak = LineBreak(
                strategy = if (config.lineBalance) LineBreak.Strategy.Balanced else LineBreak.Strategy.Simple,
                LineBreak.Strictness.Default,
                LineBreak.WordBreak.Default
            )
        )
    }
}

@Stable
private data class DrawWord(
    val time: Float,
    val word: String,
    val layout: TextLayoutResult,
    val topLeft: Offset,
    val brush: (px: Float, percent: Float) -> Brush,
    val percent: () -> Float
)
