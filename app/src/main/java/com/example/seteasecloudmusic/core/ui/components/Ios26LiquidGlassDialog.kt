package com.example.seteasecloudmusic.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seteasecloudmusic.core.ui.adaptive.LocalAdaptiveWindow
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 基于 Miuix [WindowDialog] 的通用自适应提示与输入弹窗。
 */
@Composable
fun Ios26LiquidGlassDialog(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") backdrop: Backdrop? = null,
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
    val isDark = AppTheme.isDark
    val colors = AppTheme.colors
    val window = LocalAdaptiveWindow.current

    WindowDialog(
        show = isVisible,
        title = title?.takeIf { it.isNotBlank() },
        titleColor = colors.textPrimary,
        summary = description?.takeIf { it.isNotBlank() },
        summaryColor = colors.textSecondary,
        backgroundColor = if (isDark) Color.Black else Color.White,
        onDismissRequest = onDismissRequest,
        // 宽度 >= 600.dp 或存在折叠屏分离折痕时使用大屏居中样式
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

            if (!secondaryButtonText.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        text = secondaryButtonText,
                        onClick = { onSecondaryClick?.invoke() },
                        colors = ButtonDefaults.textButtonColors(),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { if (!isPrimaryLoading) onPrimaryClick() },
                        enabled = !isPrimaryLoading,
                        colors = ButtonDefaults.buttonColorsPrimary(
                            color = primaryButtonColor,
                            contentColor = Color.White,
                            disabledColor = primaryButtonColor.copy(alpha = 0.42f),
                            disabledContentColor = Color.White.copy(alpha = 0.75f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isPrimaryLoading) {
                            AppLoadingIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                                size = 18.dp
                            )
                        } else {
                            Text(
                                text = primaryButtonText,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                Button(
                    onClick = { if (!isPrimaryLoading) onPrimaryClick() },
                    enabled = !isPrimaryLoading,
                    colors = ButtonDefaults.buttonColorsPrimary(
                        color = primaryButtonColor,
                        contentColor = Color.White,
                        disabledColor = primaryButtonColor.copy(alpha = 0.42f),
                        disabledContentColor = Color.White.copy(alpha = 0.75f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isPrimaryLoading) {
                        AppLoadingIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                            size = 18.dp
                        )
                    } else {
                        Text(
                            text = primaryButtonText,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * 专用于断网重连提示的弹窗，仅展示标题、说明与底部操作按钮。
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
        content = null
    )
}

/**
 * 分组圆角输入框卡片（18.dp 圆角，支持单行或带 0.6.dp 分割线的双行输入）。
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
 * 分组只读信息展示卡片（18.dp 圆角，支持单行或带分割线的双行键值展示）。
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
