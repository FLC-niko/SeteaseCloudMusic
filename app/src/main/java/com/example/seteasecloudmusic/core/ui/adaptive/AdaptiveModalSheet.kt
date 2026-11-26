package com.example.seteasecloudmusic.core.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import com.example.seteasecloudmusic.core.ui.components.AppLoadingIndicator
import com.example.seteasecloudmusic.core.ui.components.LiquidGlassPanel
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

/** Hosts modal content wholly in one display segment; its scrim can still cover the full window. */
@Composable
fun AdaptiveOverlayPane(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val window = LocalAdaptiveWindow.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    Layout(modifier = modifier.onGloballyPositioned { origin = it.positionInWindow() },
        content = { Box(Modifier.fillMaxSize(), content = content) }) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val fold = window.separatingFold
        var x = 0
        var y = 0
        var paneWidth = width
        var paneHeight = height
        if (fold != null) {
            if (window.horizontalFold) {
                val top = (fold.top - origin.y).roundToInt().coerceIn(0, height)
                val bottom = (fold.bottom - origin.y).roundToInt().coerceIn(0, height)
                if (bottom in 1 until height) { y = bottom; paneHeight = height - bottom }
                else if (top > 0) paneHeight = top
            } else {
                val left = (fold.left - origin.x).roundToInt().coerceIn(0, width)
                val right = (fold.right - origin.x).roundToInt().coerceIn(0, width)
                if (right in 1 until width) { x = right; paneWidth = width - right }
                else if (left > 0) paneWidth = left
            }
        }
        val child = measurables.single().measure(Constraints.fixed(paneWidth, paneHeight))
        layout(width, height) { child.placeRelative(x, y) }
    }
}

@Composable
fun AdaptiveModalSheet(
    onDismissRequest: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val window = LocalAdaptiveWindow.current
    val scrimColor = AppTheme.colors.scrim
    val handleColor = AppTheme.colors.handleBar
    if (window.width < 600.dp && window.separatingFold == null) {
        var sheetVisible by remember { mutableStateOf(true) }
        WindowBottomSheet(
            show = sheetVisible,
            onDismissRequest = { sheetVisible = false },
            onDismissFinished = onDismissRequest,
            backgroundColor = containerColor,
            cornerRadius = 28.dp,
            insideMargin = DpSize.Zero,
            dragHandleColor = handleColor,
            modifier = modifier
        ) { content() }
    } else {
        Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            var appeared by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { appeared = true }
            val reveal by animateFloatAsState(if (appeared) 1f else 0f,
                spring(dampingRatio = 0.82f, stiffness = 500f), label = "glassSheetReveal")
            Box(Modifier.fillMaxSize().background(scrimColor).clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismissRequest)) {
                AdaptiveOverlayPane(Modifier.fillMaxSize().systemBarsPadding()) {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        LiquidGlassPanel(modifier.align(Alignment.Center).padding(20.dp).widthIn(max = 460.dp)
                            .fillMaxWidth().testTag("adaptive_modal_surface").heightIn(max = (maxHeight - 40.dp).coerceAtLeast(0.dp))
                            .graphicsLayer { alpha = reveal; scaleX = 0.94f + 0.06f * reveal; scaleY = scaleX }
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
                            containerColor = containerColor) {
                            content()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdaptiveAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(32.dp),
    containerColor: Color = if (AppTheme.isDark) Color.Black else Color.White
) {
    FloatingBottomDialogContainer(
        onDismissRequest = onDismissRequest,
        shape = shape,
        containerColor = containerColor
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            title?.invoke()
            text?.invoke()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dismissButton?.invoke()
                confirmButton()
            }
        }
    }
}

/**
 * 贴底悬浮圆角弹窗（深色模式纯黑 #000000，浅色模式纯白 #FFFFFF），
 * 底部配备对称胶囊操作按钮（单按钮铺满 / 双按钮左右等宽）。
 */
@Composable
fun AdaptiveAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    dismissText: String? = "取消",
    onDismiss: () -> Unit = onDismissRequest,
    confirmColor: Color = AppTheme.colors.accent,
    confirmEnabled: Boolean = true,
    dismissEnabled: Boolean = true,
    isConfirmLoading: Boolean = false,
    shape: Shape = RoundedCornerShape(32.dp),
    containerColor: Color = if (AppTheme.isDark) Color.Black else Color.White,
    content: (@Composable () -> Unit)? = null
) {
    val colors = AppTheme.colors
    val centerHeader = content == null

    FloatingBottomDialogContainer(
        onDismissRequest = onDismissRequest,
        shape = shape,
        containerColor = containerColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = if (centerHeader) Alignment.CenterHorizontally else Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = if (centerHeader) TextAlign.Center else TextAlign.Start,
                lineHeight = 24.sp
            )

            if (!text.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = text,
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    textAlign = if (centerHeader) TextAlign.Center else TextAlign.Start,
                    lineHeight = 20.sp
                )
            }

            if (content != null) {
                Spacer(modifier = Modifier.height(16.dp))
                content()
            }

            Spacer(modifier = Modifier.height(22.dp))

            if (!dismissText.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingDialogPillButton(
                        text = dismissText,
                        isPrimary = false,
                        enabled = dismissEnabled,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    FloatingDialogPillButton(
                        text = confirmText,
                        isPrimary = true,
                        primaryColor = confirmColor,
                        enabled = confirmEnabled,
                        isLoading = isConfirmLoading,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                FloatingDialogPillButton(
                    text = confirmText,
                    isPrimary = true,
                    primaryColor = confirmColor,
                    enabled = confirmEnabled,
                    isLoading = isConfirmLoading,
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FloatingBottomDialogContainer(
    onDismissRequest: () -> Unit,
    shape: Shape,
    containerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = AppTheme.isDark
    val scrimColor = AppTheme.colors.scrim
    val borderColor = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.06f)
    val slideDistancePx = with(LocalDensity.current) { 42.dp.toPx() }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        var appeared by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { appeared = true }
        val reveal by animateFloatAsState(
            targetValue = if (appeared) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.84f, stiffness = 460f),
            label = "floatingDialogReveal"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor.copy(alpha = scrimColor.alpha * reveal))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                )
        ) {
            AdaptiveOverlayPane(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .imePadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        modifier = modifier
                            .widthIn(max = 440.dp)
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = reveal
                                translationY = (1f - reveal) * slideDistancePx
                                val scale = 0.96f + 0.04f * reveal
                                scaleX = scale
                                scaleY = scale
                            }
                            .shadow(
                                elevation = if (isDark) 24.dp else 18.dp,
                                shape = shape,
                                clip = false,
                                spotColor = Color.Black.copy(alpha = if (isDark) 0.55f else 0.18f),
                                ambientColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.10f)
                            )
                            .border(0.5.dp, borderColor, shape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            ),
                        shape = shape,
                        color = containerColor
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingDialogPillButton(
    text: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryColor: Color = AppTheme.colors.accent,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val isDark = AppTheme.isDark
    val colors = AppTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 620f),
        label = "floatingDialogButtonScale"
    )

    val pillShape = RoundedCornerShape(24.dp)
    val bgColor = when {
        isPrimary && enabled -> primaryColor
        isPrimary -> primaryColor.copy(alpha = 0.42f)
        isDark -> Color(0xFF1C1C1E)
        else -> Color(0xFFF2F2F7)
    }
    val textColor = when {
        isPrimary && enabled -> Color.White
        isPrimary -> Color.White.copy(alpha = 0.75f)
        enabled -> colors.textPrimary
        else -> colors.textTertiary
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(pillShape)
            .background(bgColor)
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            AppLoadingIndicator(
                modifier = Modifier.size(18.dp),
                color = textColor,
                strokeWidth = 2.dp,
                size = 18.dp
            )
        } else {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}
