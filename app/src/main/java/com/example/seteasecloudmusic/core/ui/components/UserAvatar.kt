package com.example.seteasecloudmusic.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.LocalIndication
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts
import com.example.seteasecloudmusic.core.ui.adaptive.LocalAdaptiveWindow
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * 用户头像展示组件，按头像 URL -> 姓名首字母缩略 -> 访客/默认图标依次降级。
 *
 * @param size 头像外径（默认 46.dp）
 * @param isGuest 为 true 且无头像和昵称时，展示主题强调色访客图标（缩放比例 0.58）
 */
@Composable
fun UserAvatar(
    avatarUrl: String?,
    displayName: String?,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    showBorder: Boolean = false,
    borderWidth: Dp = 0.dp,
    borderColor: Color = Color.Transparent,
    isGuest: Boolean = false
) {
    val monogram = remember(displayName) { buildAvatarMonogram(displayName) }
    val hasAvatarImage = !avatarUrl.isNullOrBlank()
    val hasMonogram = !monogram.isNullOrBlank()
    val isGuestPlaceholder = !hasAvatarImage && !hasMonogram && isGuest
    val appleMusicRed = AppTheme.colors.accent

    if (isGuestPlaceholder) {
        Box(
            modifier = modifier
                .size(size),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = MiuixIcons.Contacts,
                contentDescription = null,
                tint = appleMusicRed,
                modifier = Modifier.size(size * 0.58f)
            )
        }
        return
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF5A5678), Color(0xFF2D2948))
                )
            )
            .then(
                if (showBorder) {
                    Modifier.border(borderWidth, borderColor, CircleShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (hasAvatarImage) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else if (hasMonogram) {
            Text(
                text = monogram,
                color = Color.White,
                fontSize = (size.value * 0.33f).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        } else {
            Icon(
                imageVector = MiuixIcons.Contacts,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.43f)
            )
        }
    }
}

/** 从显示名称提取最多两位大写首字母缩略（多词取首尾词首字母，单词取前两字符）。 */
fun buildAvatarMonogram(displayName: String?): String? {
    val normalized = displayName?.trim().orEmpty()
    if (normalized.isEmpty()) return null

    val parts = normalized
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    return if (parts.size >= 2) {
        val first = parts.first().firstOrNull() ?: return null
        val last = parts.last().firstOrNull() ?: return null
        "$first$last".uppercase()
    } else {
        normalized.take(2).uppercase()
    }
}

/**
 * 可点击圆形用户头像按钮（在侧边栏模式下非侧栏入口时自动隐藏）。
 *
 * @param sideControl 是否位于侧边导航栏内；非侧栏按钮在启用 NavigationRail 时隐藏
 */
@Composable
fun UserAvatarButton(
    avatarUrl: String?,
    displayName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    enabled: Boolean = true,
    sideControl: Boolean = false
) {
    if (LocalAdaptiveWindow.current.useNavigationRail && !sideControl) return
    val hasDisplayName = !displayName.isNullOrBlank()
    val hasAvatar = !avatarUrl.isNullOrBlank()
    val isGuest = !hasAvatar && !hasDisplayName
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    // 按压缩放至 0.94f，弹簧参数：刚度 760、阻尼比 0.76
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = 760f, dampingRatio = 0.76f),
        label = "avatarButtonScale"
    )
    val indication = LocalIndication.current

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "打开账户" }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .indication(interactionSource = interactionSource, indication = indication)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        UserAvatar(
            avatarUrl = avatarUrl,
            displayName = displayName,
            size = size,
            showBorder = false,
            borderWidth = 0.dp,
            isGuest = isGuest
        )
    }
}
