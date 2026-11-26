package com.example.seteasecloudmusic.feature.player.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.example.seteasecloudmusic.core.settings.OnlineAudioQuality
import com.example.seteasecloudmusic.core.ui.components.BloomStrokeDefaults
import com.example.seteasecloudmusic.core.ui.components.GlassSliderDefaults
import com.example.seteasecloudmusic.core.ui.components.LiquidGlassActiveDroplet
import com.example.seteasecloudmusic.core.ui.components.LiquidGlassActiveTabsRecorder
import com.example.seteasecloudmusic.core.ui.components.LiquidGlassDualColorTabs
import com.example.seteasecloudmusic.core.ui.components.bloomStroke
import com.example.seteasecloudmusic.core.ui.components.interactiveGlassHighlight
import com.example.seteasecloudmusic.core.ui.components.liquidGlass
import com.example.seteasecloudmusic.core.ui.components.liquidSegmentDrag
import com.example.seteasecloudmusic.core.ui.components.rememberLiquidSegmentState
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * 播放页音质切换液态玻璃悬浮小弹窗。
 *
 * 外壳材质、选中液态玻璃滑块与跟手物理动画（Gooey 拉伸 + 按压张力缩放）与底部导航栏完全一致，
 * 仅将水平分段滑块旋转为垂直分段滑块，在播放页居中音质按钮上方就近弹出。
 */
@Composable
internal fun AudioQualityLiquidGlassPopup(
    visibleState: MutableTransitionState<Boolean>,
    currentQuality: OnlineAudioQuality,
    backdrop: Backdrop?,
    anchorBoundsInRoot: Rect?,
    onSelectQuality: (OnlineAudioQuality) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = visibleState.currentState || visibleState.targetState
    if (!isActive) return

    BackHandler(enabled = visibleState.targetState) {
        onDismiss()
    }

    val density = LocalDensity.current
    val edgeMarginPx = with(density) { 16.dp.roundToPx() }
    val anchorGapPx = with(density) { 12.dp.roundToPx() }
    var hostOriginInRoot by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(20f)
    ) {
        // 透明触控蒙层：保持底部导航栏同款通透采样，同时支持点击弹窗外关闭
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.06f))
                    .pointerInput(Unit) {
                        detectTapGestures { onDismiss() }
                    }
            )
        }

        // 在安全区内按锚点上方居中放置弹窗（无锚点时水平垂直居中）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .onGloballyPositioned { coords ->
                    if (coords.isAttached) {
                        val pos = coords.positionInRoot()
                        if (hostOriginInRoot != pos) {
                            hostOriginInRoot = pos
                        }
                    }
                }
        ) {
            val anchorBoundsInHost = anchorBoundsInRoot?.translate(-hostOriginInRoot)
            Layout(
                modifier = Modifier.fillMaxSize(),
                content = {
                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = fadeIn(animationSpec = tween(170)) + scaleIn(
                            initialScale = 0.76f,
                            transformOrigin = TransformOrigin(0.5f, 1f),
                            animationSpec = spring(
                                dampingRatio = 0.78f,
                                stiffness = 460f
                            )
                        ),
                        exit = fadeOut(animationSpec = tween(140)) + scaleOut(
                            targetScale = 0.82f,
                            transformOrigin = TransformOrigin(0.5f, 1f),
                            animationSpec = spring(
                                dampingRatio = 0.90f,
                                stiffness = 560f
                            )
                        )
                    ) {
                        AudioQualityLiquidGlassCard(
                            currentQuality = currentQuality,
                            backdrop = backdrop,
                            onSelectQuality = onSelectQuality,
                            onDismiss = onDismiss
                        )
                    }
                }
            ) { measurables, constraints ->
                val childConstraints = Constraints(
                    minWidth = 0,
                    maxWidth = (constraints.maxWidth - edgeMarginPx * 2).coerceAtLeast(0),
                    minHeight = 0,
                    maxHeight = (constraints.maxHeight - edgeMarginPx * 2).coerceAtLeast(0)
                )
                val placeables = measurables.map { it.measure(childConstraints) }
                val cardWidth = placeables.maxOfOrNull { it.width } ?: 0
                val cardHeight = placeables.maxOfOrNull { it.height } ?: 0

                val maxX = (constraints.maxWidth - cardWidth - edgeMarginPx).coerceAtLeast(edgeMarginPx)
                val maxY = (constraints.maxHeight - cardHeight - edgeMarginPx).coerceAtLeast(edgeMarginPx)

                val validAnchor = anchorBoundsInHost?.takeIf { it.width > 0f && it.height > 0f }
                val x = if (validAnchor != null) {
                    (validAnchor.center.x - cardWidth * 0.5f).roundToInt().coerceIn(edgeMarginPx, maxX)
                } else {
                    ((constraints.maxWidth - cardWidth) / 2).coerceIn(edgeMarginPx, maxX)
                }

                val y = if (validAnchor != null) {
                    val preferredAboveY = (validAnchor.top - anchorGapPx - cardHeight).roundToInt()
                    if (preferredAboveY >= edgeMarginPx) {
                        preferredAboveY.coerceAtMost(maxY)
                    } else {
                        val fallbackBelowY = (validAnchor.bottom + anchorGapPx).roundToInt()
                        if (fallbackBelowY <= maxY) fallbackBelowY else preferredAboveY.coerceIn(edgeMarginPx, maxY)
                    }
                } else {
                    ((constraints.maxHeight - cardHeight) / 2).coerceIn(edgeMarginPx, maxY)
                }

                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeables.forEach { it.placeRelative(x, y) }
                }
            }
        }
    }
}

@Composable
internal fun AudioQualityLiquidGlassCard(
    currentQuality: OnlineAudioQuality,
    backdrop: Backdrop?,
    onSelectQuality: (OnlineAudioQuality) -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val qualities = OnlineAudioQuality.PlayerSelectableQualities
    val initialIndex = remember {
        qualities.indexOf(currentQuality).coerceAtLeast(0)
    }
    var selectedIndex by remember { mutableIntStateOf(initialIndex) }
    var isCommitting by remember { mutableStateOf(false) }

    LaunchedEffect(currentQuality) {
        if (!isCommitting) {
            selectedIndex = qualities.indexOf(currentQuality).coerceAtLeast(0)
        }
    }

    val density = LocalDensity.current
    val fontScaleFactor = density.fontScale.coerceIn(1f, 1.3f)
    val popupWidth = 192.dp * fontScaleFactor
    val slotHeight = 50.dp * density.fontScale.coerceIn(1f, 1.2f)
    val popupHeight = slotHeight * qualities.size
    val cornerRadius = slotHeight / 2

    val shape = remember(cornerRadius) { RoundedRectangle(cornerRadius) }
    val haptics = LocalHapticFeedback.current
    val segmentState = rememberLiquidSegmentState(itemCount = qualities.size, selectedIndex = selectedIndex)
    val optionsBackdrop = rememberLayerBackdrop()
    val combinedBackdrop = if (backdrop != null) {
        rememberCombinedBackdrop(backdrop, optionsBackdrop)
    } else {
        null
    }

    LaunchedEffect(isCommitting, selectedIndex) {
        if (isCommitting) {
            val changed = selectedIndex != initialIndex
            // 切换后等待滑块落位动画（约 220ms）结束，再额外停留 500ms 后关闭
            delay(if (changed) 720L else 120L)
            onDismiss()
        }
    }

    val commitSelection: (Int) -> Unit = { settledIndex ->
        if (!isCommitting) {
            isCommitting = true
            selectedIndex = settledIndex
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSelectQuality(qualities[settledIndex])
        }
    }

    val outerLayerBlock: androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit = {
        val progress = segmentState.pressProgress.value.coerceIn(0f, 1f)
        if (size.width > 0f) {
            val maxScale = (size.width + 8f.dp.toPx()) / size.width
            val scale = lerp(1f, maxScale, progress)
            scaleX = scale
            scaleY = scale
        }
    }

    val selectedOptionsContent: @Composable BoxScope.() -> Unit = {
        Column(modifier = Modifier.fillMaxSize()) {
            qualities.forEach { quality ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(slotHeight)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = quality.playerMenuLabel,
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFA233B)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    Box(
        modifier = modifier
            .testTag("audio_quality_liquid_glass_popup")
            .width(popupWidth)
            .height(popupHeight)
    ) {
        // 1. 外框暗色液态玻璃弹窗（不裁剪兄弟节点，保持水滴放大镜自由溢出边缘）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (backdrop != null) {
                        Modifier.liquidGlass(
                            backdrop = backdrop,
                            cornerRadius = cornerRadius,
                            isDarkSurface = true,
                            enableNoiseDither = true,
                            layerBlock = outerLayerBlock
                        )
                    } else {
                        Modifier
                            .graphicsLayer(outerLayerBlock)
                            .background(
                                color = Color(0xFF23232A).copy(alpha = 0.84f),
                                shape = shape
                            )
                            .bloomStroke(
                                cornerRadius = cornerRadius,
                                style = BloomStrokeDefaults.OuterDark
                            )
                    }
                )
                .interactiveGlassHighlight(
                    shape = shape,
                    enabled = !isCommitting,
                    isDarkSurface = true
                )
                .liquidSegmentDrag(
                    state = segmentState,
                    itemCount = qualities.size,
                    selectedIndex = selectedIndex,
                    onSettle = commitSelection,
                    enabled = !isCommitting,
                    orientation = Orientation.Vertical
                )
        ) {
            LiquidGlassDualColorTabs(
                state = segmentState,
                containerWidth = popupWidth,
                containerHeight = popupHeight,
                cornerRadius = cornerRadius,
                isDarkSurface = true,
                orientation = Orientation.Vertical,
                unselectedContent = {
                    Column(modifier = Modifier.fillMaxSize()) {
                        qualities.forEach { quality ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(slotHeight)
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = quality.playerMenuLabel,
                                    style = TextStyle(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.88f),
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.24f),
                                            offset = Offset(0f, 1f),
                                            blurRadius = 4f
                                        )
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                selectedContent = selectedOptionsContent
            )
        }

        // 2. 激活态暗色背景 + 高亮音质文字录制器
        LiquidGlassActiveTabsRecorder(
            pageBackdrop = backdrop,
            activeTabsBackdrop = optionsBackdrop,
            state = segmentState,
            containerWidth = popupWidth,
            containerHeight = popupHeight,
            cornerRadius = cornerRadius,
            isDarkSurface = true,
            layerBlock = outerLayerBlock,
            modifier = Modifier.align(Alignment.TopStart),
            selectedContent = selectedOptionsContent
        )

        // 3. 激活态纵向 3D 水滴放大镜（采样页面 + 高亮音质文字双层背景）
        LiquidGlassActiveDroplet(
            combinedBackdrop = combinedBackdrop,
            state = segmentState,
            containerWidth = popupWidth,
            containerHeight = popupHeight,
            cornerRadius = cornerRadius,
            maxPressScale = GlassSliderDefaults.CompactMaxPressScale,
            isDarkSurface = true,
            orientation = Orientation.Vertical,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}
