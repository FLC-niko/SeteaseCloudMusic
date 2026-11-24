package com.example.seteasecloudmusic.feature.mine.presentation

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.R
import com.example.seteasecloudmusic.core.common.toCoverThumbnailUrl
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.FavoriteTracksManager
import com.example.seteasecloudmusic.core.ui.adaptive.AdaptiveModalSheet
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import com.example.seteasecloudmusic.feature.main.PlayerEntryPoints
import com.example.seteasecloudmusic.feature.player.presentation.ResolvedAudioDetails

/**
 * 歌曲操作面板页面状态：
 * ACTIONS: 默认动作菜单（下一首播放、喜欢/取消喜欢、收藏到歌单、下载、歌手、专辑）
 * ADD_TO_PLAYLIST: 收藏到歌单（内嵌平滑无缝切页，带返回箭头与新建歌单）
 */
enum class TrackActionSheetPage {
    ACTIONS,
    ADD_TO_PLAYLIST
}

/**
 * 歌单曲目与播放页共用的歌曲更多操作二级菜单（底栏抽屉）。
 * 背景为纯白色，顺序严格为：
 * 1. 下一首播放
 * 2. 喜欢 / 取消喜欢
 * 3. 收藏到歌单
 * 4. 下载
 * 5. 歌手
 * 6. 专辑
 * 7. 音频详细信息（可选，仅在传入 [audioDetails] 时融合展示）
 */
@Composable
fun TrackActionSheet(
    track: Track,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    currentPlaylistId: Long? = null,
    audioDetails: ResolvedAudioDetails? = null,
    onToggleLiked: ((Track, Boolean) -> Unit)? = null,
    onAddToPlaylistClick: ((Track) -> Unit)? = null,
    onTrackAddedToPlaylist: ((Long, Track) -> Unit)? = null
) {
    val context = LocalContext.current
    var currentPage by remember { mutableStateOf(TrackActionSheetPage.ACTIONS) }
    val colors = AppTheme.colors

    AdaptiveModalSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        modifier = modifier
    ) {
        if (currentPage == TrackActionSheetPage.ADD_TO_PLAYLIST) {
            AddToPlaylistContent(
                track = track,
                onDismiss = onDismiss,
                onBack = { currentPage = TrackActionSheetPage.ACTIONS },
                currentPlaylistId = currentPlaylistId,
                onTrackLikedChanged = onToggleLiked,
                onTrackAdded = { pid, t ->
                    onTrackAddedToPlaylist?.invoke(pid, t)
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            ) {
            // 头部歌曲信息预览区：左侧较大封面（60dp），右侧歌曲名（去除“歌曲：”）及歌手名
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 歌曲封面（较原图更大，圆角 8.dp）
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!track.coverUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = track.coverUrl.toCoverThumbnailUrl(180),
                            contentDescription = track.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // 2. 歌名与歌手
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    // 歌名（无“歌曲：”前缀）
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 歌手
                    val artistNames = track.artists.joinToString(" / ") { it.name }.ifBlank { "未知歌手" }
                    Text(
                        text = artistNames,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            HorizontalDivider(
                color = colors.dividerSubtle,
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            val likedTrackIds by FavoriteTracksManager.likedTrackIds.collectAsState()
            val isLiked = track.id in likedTrackIds

            // 1. 下一首播放
            TrackActionItem(
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_play_next),
                        contentDescription = "下一首播放",
                        tint = colors.iconSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                },
                title = "下一首播放",
                onClick = {
                    runCatching {
                        PlayerEntryPoints.playerController().insertNext(track)
                    }
                    Toast.makeText(context, "已添加到下一首播放", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            // 2. 喜欢 / 取消喜欢
            TrackActionItem(
                icon = {
                    Icon(
                        painter = painterResource(id = if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart),
                        contentDescription = if (isLiked) "取消喜欢" else "喜欢",
                        tint = if (isLiked) colors.accent else colors.iconSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                },
                title = if (isLiked) "取消喜欢" else "喜欢",
                onClick = {
                    val willBeLiked = FavoriteTracksManager.toggleLiked(track.id)
                    onToggleLiked?.invoke(track, willBeLiked)
                    val msg = if (willBeLiked) "已添加到我喜欢的音乐" else "已从我喜欢的音乐中移除"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (!willBeLiked) {
                        onDismiss()
                    }
                }
            )

            // 3. 收藏到歌单
            TrackActionItem(
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_add_to_playlist),
                        contentDescription = "收藏到歌单",
                        tint = colors.iconSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                },
                title = "收藏到歌单",
                onClick = {
                    currentPage = TrackActionSheetPage.ADD_TO_PLAYLIST
                    onAddToPlaylistClick?.invoke(track)
                }
            )

            // 4. 下载
            TrackActionItem(
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_download),
                        contentDescription = "下载",
                        tint = colors.iconSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                },
                title = "下载",
                onClick = {
                    Toast.makeText(context, "已加入下载队列", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            // 5. 歌手
            val artistNames = track.artists.joinToString(" / ") { it.name }.ifBlank { "未知歌手" }
            TrackActionItem(
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_artist),
                        contentDescription = "歌手",
                        tint = colors.iconSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                },
                title = "歌手：$artistNames",
                onClick = {
                    Toast.makeText(context, "歌手：$artistNames", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            // 6. 专辑
            val albumTitle = track.album.title.ifBlank { "未知专辑" }
            TrackActionItem(
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_album),
                        contentDescription = "专辑",
                        tint = colors.iconSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                },
                title = "专辑：$albumTitle",
                onClick = {
                    Toast.makeText(context, "专辑：$albumTitle", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            // 7. 音频详细信息（播放页更多菜单融合项）
            if (audioDetails != null) {
                TrackAudioDetailsItem(
                    trackId = track.id,
                    details = audioDetails
                )
            }
        }
    }
}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrackAudioDetailsItem(
    trackId: Long,
    details: ResolvedAudioDetails
) {
    val isDark = AppTheme.isDark
    val colors = AppTheme.colors
    var isExpanded by rememberSaveable(trackId) { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "trackAudioDetailsChevron"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 20.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_action_audio_info),
                    contentDescription = null,
                    tint = colors.iconSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "音频详细信息",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                ),
                maxLines = 1
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = details.summaryBadge,
                color = colors.textSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) "收起音频详细信息" else "展开音频详细信息",
                tint = colors.textSecondary,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = chevronRotation }
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(tween(180)),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeOut(tween(140))
        ) {
            val chipBg = if (isDark) Color(0xFF3A3A3E) else Color(0xFFE9E9EE)
            val chipText = if (isDark) Color(0xFFE5E5EA) else Color(0xFF3A3A3C)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 2.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AudioSpecTagChip(
                        text = details.sourceTag,
                        backgroundColor = colors.accentSubtle,
                        textColor = colors.accent
                    )
                    AudioSpecTagChip(
                        text = details.formatTag,
                        backgroundColor = chipBg,
                        textColor = chipText
                    )
                    AudioSpecTagChip(
                        text = details.bitrateTag,
                        backgroundColor = chipBg,
                        textColor = chipText
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val rows = listOf(
                    "音频来源" to details.sourceText,
                    "音质规格" to details.qualityText,
                    "编码格式" to details.formatText,
                    "音频码率" to details.bitrateText,
                    "采样率" to details.sampleRateText,
                    "文件大小" to details.fileSizeText,
                    "歌词来源" to details.lyricSourceText,
                    "所属专辑" to details.albumText,
                    details.identifierLabel to details.identifierValue
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { (label, value) ->
                        AudioDetailRow(label = label, value = value)
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioSpecTagChip(
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun AudioDetailRow(
    label: String,
    value: String
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.width(76.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            color = colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TrackActionItem(
    icon: @Composable () -> Unit,
    title: String,
    badge: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = AppTheme.colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )

        if (badge != null) {
            Spacer(modifier = Modifier.width(8.dp))
            badge()
        }
    }
}
