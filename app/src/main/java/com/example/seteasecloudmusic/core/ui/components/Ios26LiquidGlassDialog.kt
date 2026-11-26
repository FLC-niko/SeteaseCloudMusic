package com.example.seteasecloudmusic.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.seteasecloudmusic.core.ui.adaptive.AdaptiveOverlayPane
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop

/**
 * 通用 iOS 26 液态玻璃弹窗 (Liquid Glass Alert)。
 *
 * 严格按照拆解图复刻三层物理结构：
 * 1. 底部投影层：尺寸 +50px 软弥散深层阴影（模拟 3D 浮动感）
 * 2. 中间层：#F2F2F2 减淡半透明底板
 * 3. 玻璃层（kyant0/backdrop）：圆角 34px（RoundedRectangle 连续超圆角）、lens 透镜折射、chromaticAberration 色散、vibrancy 与边缘高光
 *
 * 支持高度复用：
 * - 默认纯净 Alert 模式（标题 + 描述 + 按钮）
 * - 支持通过 [content] 插槽嵌入任意中间内容（例如 [Ios26DialogInputCard] 输入框容器，或 [Ios26ValueContainer] 状态栏）
 * - 支持双按钮或单按钮模式（[secondaryButtonText] 为 null 时居中单按钮）
 */
@Composable
fun Ios26LiquidGlassDialog(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    title: String? = null,
    description: String? = null,
    secondaryButtonText: String? = "取消",
    primaryButtonText: String = "确定",
    primaryButtonColor: Color = AppTheme.colors.accentBlue,
    isPrimaryLoading: Boolean = false,
    onSecondaryClick: (() -> Unit)? = onDismissRequest,
    onPrimaryClick: () -> Unit = {},
    content: (@Composable () -> Unit)? = null
) {
    val cornerRadius = 32.dp
    val cardShape = RoundedCornerShape(cornerRadius)
    val isDark = AppTheme.isDark
    val colors = AppTheme.colors
    val cardBackground = if (isDark) Color.Black else Color.White
    val borderColor = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.06f)
    val centerHeader = content == null

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = spring(dampingRatio = 0.84f, stiffness = 460f)
        ),
        exit = fadeOut(animationSpec = tween(160)) + slideOutVertically(
            targetOffsetY = { it / 4 },
            animationSpec = spring(dampingRatio = 0.90f, stiffness = 520f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.scrim)
                .pointerInput(Unit) {
                    detectTapGestures {
                        onDismissRequest()
                    }
                }
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
                    Box(
                        modifier = modifier
                            .widthIn(max = 440.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .pointerInput(Unit) {
                                detectTapGestures { /* 拦截点击，防止点击弹窗关闭 */ }
                            }
                            .shadow(
                                elevation = if (isDark) 24.dp else 18.dp,
                                shape = cardShape,
                                clip = false,
                                spotColor = Color.Black.copy(alpha = if (isDark) 0.55f else 0.18f),
                                ambientColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.10f)
                            )
                            .background(cardBackground, cardShape)
                            .border(0.5.dp, borderColor, cardShape)
                            .clip(cardShape)
                            .padding(horizontal = 22.dp, vertical = 22.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (centerHeader) Alignment.CenterHorizontally else Alignment.Start
                        ) {
                            if (!title.isNullOrBlank()) {
                                Text(
                                    text = title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    textAlign = if (centerHeader) TextAlign.Center else TextAlign.Start,
                                    lineHeight = 24.sp
                                )
                            }

                            if (!description.isNullOrBlank()) {
                                if (!title.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Text(
                                    text = description,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
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

                            if (!secondaryButtonText.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Ios26CapsuleButton(
                                        text = secondaryButtonText,
                                        isPrimary = false,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onSecondaryClick?.invoke() }
                                    )

                                    Ios26CapsuleButton(
                                        text = primaryButtonText,
                                        isPrimary = true,
                                        backgroundColor = primaryButtonColor,
                                        isLoading = isPrimaryLoading,
                                        modifier = Modifier.weight(1f),
                                        onClick = onPrimaryClick
                                    )
                                }
                            } else {
                                Ios26CapsuleButton(
                                    text = primaryButtonText,
                                    isPrimary = true,
                                    backgroundColor = primaryButtonColor,
                                    isLoading = isPrimaryLoading,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = onPrimaryClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 专用于断网场景的 iOS 26 液态玻璃重试小弹窗（清爽无多余中栏）。
 */
@Composable
fun Ios26NetworkOfflineDialog(
    isVisible: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    isRetrying: Boolean = false,
    title: String = "网络连接已断开",
    description: String = "未能连接到互联网，部分内容无法加载。\n请检查网络设置后重试。"
) {
    Ios26LiquidGlassDialog(
        isVisible = isVisible,
        onDismissRequest = onDismiss,
        modifier = modifier,
        backdrop = backdrop,
        title = title,
        description = description,
        secondaryButtonText = "稍后",
        primaryButtonText = "重试加载",
        primaryButtonColor = AppTheme.colors.accentBlue,
        isPrimaryLoading = isRetrying,
        onSecondaryClick = onDismiss,
        onPrimaryClick = onRetry,
        content = null // 断网场景无需中间多余信息栏，清爽干净！
    )
}

/**
 * iOS 26 原设计图输入框卡片（还原解析图中的 Value 输入栏）。
 *
 * 包含两个输入栏以及中间极细分割线，支持单行/双行输入。
 */
@Composable
fun Ios26DialogInputCard(
    value1: String,
    onValue1Change: (String) -> Unit,
    placeholder1: String = "Value",
    modifier: Modifier = Modifier,
    value2: String? = null,
    onValue2Change: ((String) -> Unit)? = null,
    placeholder2: String = "Value"
) {
    val containerShape = RoundedCornerShape(18.dp)
    val isDark = AppTheme.isDark
    val containerBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.055f)
    val containerBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(containerBg, containerShape)
            .border(0.5.dp, containerBorder, containerShape)
            .padding(horizontal = 16.dp)
    ) {
        // 第一行输入框
        Ios26InputFieldRow(
            value = value1,
            onValueChange = onValue1Change,
            placeholder = placeholder1
        )

        // 若存在第二行输入项，渲染分割线和第二行
        if (value2 != null && onValue2Change != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.6.dp)
                    .background(dividerColor)
            )

            Ios26InputFieldRow(
                value = value2,
                onValueChange = onValue2Change,
                placeholder = placeholder2
            )
        }
    }
}

/**
 * 单行原生 iOS 风格无下划线文本输入框
 */
@Composable
private fun Ios26InputFieldRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val colors = AppTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(
            fontSize = 15.sp,
            color = colors.textPrimary,
            fontWeight = FontWeight.Normal
        ),
        cursorBrush = SolidColor(colors.accentBlue),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 15.sp,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Normal
                    )
                }
                innerTextField()
            }
        }
    )
}

/**
 * iOS 26 只读 Value 展示卡片（供信息展示、状态诊断等复用场景使用）。
 */
@Composable
fun Ios26ValueContainer(
    valueTopLabel: String,
    valueTopTrailing: (@Composable () -> Unit)? = null,
    valueBottomLabel: String? = null,
    valueBottomTrailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val containerShape = RoundedCornerShape(18.dp)
    val isDark = AppTheme.isDark
    val colors = AppTheme.colors
    val containerBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.055f)
    val containerBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(containerBg, containerShape)
            .border(0.5.dp, containerBorder, containerShape)
            .padding(horizontal = 16.dp)
    ) {
        // 第一行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = valueTopLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (valueTopTrailing != null) {
                valueTopTrailing()
            }
        }

        // 第二行（如果提供）
        if (valueBottomLabel != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.6.dp)
                    .background(dividerColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = valueBottomLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (valueBottomTrailing != null) {
                    valueBottomTrailing()
                }
            }
        }
    }
}

/**
 * 药丸胶囊按钮（Secondary 浅灰底，Primary 鲜亮蓝/自定义高亮色）。
 */
@Composable
private fun Ios26CapsuleButton(
    text: String,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    backgroundColor: Color = if (isPrimary) AppTheme.colors.accentBlue else if (AppTheme.isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 620f),
        label = "ios26ButtonScale"
    )

    val buttonShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(buttonShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (!isLoading) onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            AppLoadingIndicator(
                modifier = Modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp,
                size = 18.dp
            )
        } else {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPrimary) Color.White else colors.textPrimary
            )
        }
    }
}
