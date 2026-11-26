package com.example.seteasecloudmusic.core.ui.adaptive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.seteasecloudmusic.core.ui.components.AppLoadingIndicator
import com.example.seteasecloudmusic.core.ui.components.LiquidGlassPanel
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog

/** 将弹窗内容约束在折叠屏单侧有效显示区域内，遮罩层仍可铺满整个窗口。 */
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

/**
 * 自适应模态面板：
 * - 紧凑屏幕（宽度 < 600.dp 且无分离折痕）：使用贴底 [WindowBottomSheet]（顶部圆角 28.dp）。
 * - 宽屏或折叠屏：在单侧显示区居中弹出 [LiquidGlassPanel]（最大宽度 460.dp，外边距 20.dp）。
 */
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
    // 600dp 为紧凑屏/大屏断点
    if (window.width < 600.dp && window.separatingFold == null) {
        var sheetVisible by remember { mutableStateOf(true) }
        WindowBottomSheet(
            show = sheetVisible,
            onDismissRequest = { sheetVisible = false },
            onDismissFinished = onDismissRequest,
            backgroundColor = containerColor,
            cornerRadius = 28.dp, // 底部弹窗顶部圆角（dp）
            insideMargin = DpSize.Zero,
            dragHandleColor = handleColor,
            modifier = modifier
        ) { content() }
    } else {
        Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            var appeared by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { appeared = true }
            // 居中浮层入场弹簧（阻尼比 0.82 轻微回弹，刚度 500）
            val reveal by animateFloatAsState(if (appeared) 1f else 0f,
                spring(dampingRatio = 0.82f, stiffness = 500f), label = "glassSheetReveal")
            Box(Modifier.fillMaxSize().background(scrimColor).clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismissRequest)) {
                AdaptiveOverlayPane(Modifier.fillMaxSize().systemBarsPadding()) {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        // 大屏浮层卡片：最大宽度 460.dp，四周最小留白 20.dp（总扣减 40.dp），入场缩放 0.94f -> 1f
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

/**
 * 基于 Miuix [WindowDialog] 的通用自适应槽位弹窗（宽度 >= 600.dp 或存在折痕时切换为大屏居中样式）。
 */
@Composable
fun AdaptiveAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") shape: Shape = RoundedCornerShape(32.dp),
    containerColor: Color = if (AppTheme.isDark) Color.Black else Color.White
) {
    val window = LocalAdaptiveWindow.current
    var dialogVisible by remember { mutableStateOf(true) }
    WindowDialog(
        show = dialogVisible,
        onDismissRequest = { dialogVisible = false },
        onDismissFinished = onDismissRequest,
        backgroundColor = containerColor,
        largeScreen = window.width >= 600.dp || window.separatingFold != null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
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
 * 基于 Miuix [WindowDialog] 的自适应弹窗（紧凑屏幕贴底弹出、大屏居中缩放），
 * 底部使用 Miuix [TextButton] 与 [Button] 操作按钮（单按钮铺满 / 双按钮左右等宽）。
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
    @Suppress("UNUSED_PARAMETER") shape: Shape = RoundedCornerShape(32.dp),
    containerColor: Color = if (AppTheme.isDark) Color.Black else Color.White,
    content: (@Composable () -> Unit)? = null
) {
    val colors = AppTheme.colors
    val window = LocalAdaptiveWindow.current
    var dialogVisible by remember { mutableStateOf(true) }

    WindowDialog(
        show = dialogVisible,
        title = title,
        titleColor = colors.textPrimary,
        summary = text?.takeIf { it.isNotBlank() },
        summaryColor = colors.textSecondary,
        backgroundColor = containerColor,
        onDismissRequest = { dialogVisible = false },
        onDismissFinished = onDismissRequest,
        largeScreen = window.width >= 600.dp || window.separatingFold != null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            if (content != null) {
                content()
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (!dismissText.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        text = dismissText,
                        onClick = onDismiss,
                        enabled = dismissEnabled,
                        colors = ButtonDefaults.textButtonColors(),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onConfirm,
                        enabled = confirmEnabled && !isConfirmLoading,
                        colors = ButtonDefaults.buttonColorsPrimary(
                            color = confirmColor,
                            contentColor = Color.White,
                            disabledColor = confirmColor.copy(alpha = 0.42f),
                            disabledContentColor = Color.White.copy(alpha = 0.75f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isConfirmLoading) {
                            AppLoadingIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                                size = 18.dp
                            )
                        } else {
                            Text(
                                text = confirmText,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                Button(
                    onClick = onConfirm,
                    enabled = confirmEnabled && !isConfirmLoading,
                    colors = ButtonDefaults.buttonColorsPrimary(
                        color = confirmColor,
                        contentColor = Color.White,
                        disabledColor = confirmColor.copy(alpha = 0.42f),
                        disabledContentColor = Color.White.copy(alpha = 0.75f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isConfirmLoading) {
                        AppLoadingIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                            size = 18.dp
                        )
                    } else {
                        Text(
                            text = confirmText,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
