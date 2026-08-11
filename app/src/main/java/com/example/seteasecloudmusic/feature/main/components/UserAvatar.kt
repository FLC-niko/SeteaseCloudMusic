package com.example.seteasecloudmusic.feature.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.core.util.rememberCoverRequest

@Composable
fun UserAvatar(
    avatarUrl: String?,
    displayName: String?,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    showBorder: Boolean = false,
    borderWidth: Dp = 1.dp,
    borderColor: Color = Color.White.copy(alpha = 0.42f),
    isGuest: Boolean = false
) {
    val monogram = remember(displayName) { buildAvatarMonogram(displayName) }
    val hasAvatarImage = !avatarUrl.isNullOrBlank()
    val hasMonogram = !monogram.isNullOrBlank()
    val isGuestPlaceholder = !hasAvatarImage && !hasMonogram && isGuest
    val appleMusicRed = Color(0xFFFA233B)

    if (isGuestPlaceholder) {
        Box(
            modifier = modifier
                .size(size)
                .border(2.dp, appleMusicRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = appleMusicRed,
                modifier = Modifier.size(size * 0.48f)
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
            val avatarRequest = rememberCoverRequest(imageUrl = avatarUrl, targetSize = size, crossfade = true)
            if (avatarRequest != null) {
                AsyncImage(
                    model = avatarRequest,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
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
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.43f)
            )
        }
    }
}

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
