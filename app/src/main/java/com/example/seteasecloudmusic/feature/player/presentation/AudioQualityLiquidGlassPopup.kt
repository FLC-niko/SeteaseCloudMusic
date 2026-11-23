package com.example.seteasecloudmusic.feature.player.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.example.seteasecloudmusic.core.settings.OnlineAudioQuality
import com.example.seteasecloudmusic.core.ui.components.GlassSliderDefaults
import com.example.seteasecloudmusic.core.ui.components.GlassThumb
import com.example.seteasecloudmusic.core.ui.components.glassSegmentDrag
import com.example.seteasecloudmusic.core.ui.components.liquidGlass
import com.example.seteasecloudmusic.core.ui.components.rememberGlassThumbGeometry
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    var dragOffsetY by remember { mutableStateOf<Float?>(null) }
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
    val scope = rememberCoroutineScope()
    val pressProgress = remember { Animatable(0f) }
    val pressSpringSpec = remember {
        spring<Float>(dampingRatio = 0.65f, stiffness = 500f)
    }

    LaunchedEffect(isCommitting, selectedIndex) {
        if (isCommitting) {
            val changed = selectedIndex != initialIndex
            // 切换后等待滑块落位动画（约 220ms）结束，再额外停留 500ms 后关闭
            delay(if (changed) 720L else 120L)
            onDismiss()
        }
    }

    val activeVisualIndex = dragOffsetY?.let { offsetPx ->
        val slotPx = with(density) { slotHeight.toPx() }.coerceAtLeast(1f)
        (offsetPx / slotPx).toInt().coerceIn(0, qualities.lastIndex)
    } ?: selectedIndex

    val geometry = rememberGlassThumbGeometry(
        containerWidth = popupHeight,
        itemCount = qualities.size,
        selectedIndex = selectedIndex,
        dragOffsetX = dragOffsetY,
        barHeight = popupWidth,
        cornerRadius = cornerRadius
    )

    val commitSelection: (Int) -> Unit = { settledIndex ->
        if (!isCommitting) {
            isCommitting = true
            selectedIndex = settledIndex
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSelectQuality(qualities[settledIndex])
        }
    }

    Box(
        modifier = modifier
            .testTag("audio_quality_liquid_glass_popup")
            .width(popupWidth)
            .height(popupHeight)
            .graphicsLayer {
                val progress = pressProgress.value
                val maxScale = (size.width + 8f.dp.toPx()) / size.width
                val scale = lerp(1f, maxScale, progress)
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (backdrop != null) {
                    Modifier.liquidGlass(
                        backdrop = backdrop,
                        cornerRadius = cornerRadius,
                        layerBlock = {
                            val progress = pressProgress.value
                            val maxScale = (size.width + 8f.dp.toPx()) / size.width
                            val scale = lerp(1f, maxScale, progress)
                            scaleX = scale
                            scaleY = scale
                        }
                    )
                } else {
                    Modifier
                        .background(
                            color = Color(0xFF23232A).copy(alpha = 0.84f),
                            shape = shape
                        )
                        .border(
                            width = 0.75.dp,
                            color = Color.White.copy(alpha = 0.36f),
                            shape = shape
                        )
                }
            )
            .clip(shape)
            .glassSegmentDrag(
                itemCount = qualities.size,
                onDragOffsetChange = { dragOffsetY = it },
                onSettle = commitSelection,
                onPressChanged = { pressed ->
                    scope.launch {
                        pressProgress.animateTo(if (pressed) 1f else 0f, pressSpringSpec)
                    }
                },
                enabled = !isCommitting,
                orientation = Orientation.Vertical
            )
    ) {
        if (backdrop != null) {
            GlassThumb(
                backdrop = backdrop,
                geometry = geometry,
                surfaceAlpha = GlassSliderDefaults.ThumbSurfaceAlpha,
                orientation = Orientation.Vertical,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        } else {
            val thumbShape = remember { RoundedRectangle(geometry.innerCornerRadius) }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, geometry.offsetX.roundToPx()) }
                    .graphicsLayer {
                        scaleY = geometry.stretchScale
                        transformOrigin = TransformOrigin(0.5f, if (geometry.anchorAtEnd) 1f else 0f)
                    }
                    .size(geometry.height, geometry.width)
                    .background(Color.White.copy(alpha = 0.88f), thumbShape)
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            qualities.forEachIndexed { index, quality ->
                val isSelected = index == activeVisualIndex
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFFFA233B) else Color.White.copy(alpha = 0.92f),
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "qualityOptionTextColor"
                )
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
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                            shadow = if (isSelected) {
                                null
                            } else {
                                Shadow(
                                    color = Color.Black.copy(alpha = 0.24f),
                                    offset = Offset(0f, 1f),
                                    blurRadius = 4f
                                )
                            }
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
