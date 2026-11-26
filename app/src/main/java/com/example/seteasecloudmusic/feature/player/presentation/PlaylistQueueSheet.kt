package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Music
import com.example.seteasecloudmusic.core.ui.adaptive.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.seteasecloudmusic.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.PlaybackMode
import com.example.seteasecloudmusic.core.player.PlaybackState
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.core.ui.theme.AppTheme

/**
 * 播放列表二级菜单底栏 (当前待播清单与播放顺序)
 */
@Composable
fun PlaylistQueueSheet(
    playbackState: PlaybackState,
    onTrackClick: (Int) -> Unit,
    onRemoveTrack: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onTogglePlaybackMode: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // 打开时自动平滑滚动到当前正在播放的曲目
    LaunchedEffect(playbackState.currentQueueIndex) {
        if (playbackState.currentQueueIndex in playbackState.queueTracks.indices) {
            val target = (playbackState.currentQueueIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    val colors = AppTheme.colors
    AdaptiveModalSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(if (LocalAdaptiveWindow.current.width >= 600.dp || LocalAdaptiveWindow.current.separatingFold != null) 1f else 0.72f)
                .navigationBarsPadding()
        ) {
            // 头部标题与控制栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "播放列表",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${playbackState.queueTracks.size})",
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Normal
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 播放模式切换胶囊
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.surfaceVariant)
                            .clickable(onClick = onTogglePlaybackMode)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        val isShuffle = playbackState.playbackMode == PlaybackMode.SHUFFLE
                        Icon(
                            painter = painterResource(id = if (isShuffle) R.drawable.ic_shuffle else R.drawable.ic_repeat),
                            contentDescription = null,
                            tint = if (isShuffle) colors.accent else colors.iconPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isShuffle) "随机播放" else "顺序播放",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isShuffle) colors.accent else colors.iconPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 清空队列按键
                    if (playbackState.queueTracks.isNotEmpty()) {
                        IconButton(
                            onClick = onClearQueue,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_delete_history),
                                contentDescription = "清空播放列表",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                thickness = 0.5.dp,
                color = colors.dividerSubtle
            )

            // 歌曲列表区域
            if (playbackState.queueTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = MiuixIcons.Music,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "播放列表为空",
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 12.dp)
                ) {
                    itemsIndexed(
                        items = playbackState.queueTracks,
                        key = { index, track -> "${track.id}_$index" }
                    ) { index, track ->
                        val isCurrent = index == playbackState.currentQueueIndex
                        val isPlaying = isCurrent && playbackState.status == PlayerStatus.PLAYING

                        QueueTrackItemRow(
                            index = index,
                            track = track,
                            isCurrent = isCurrent,
                            isPlaying = isPlaying,
                            onClick = { onTrackClick(index) },
                            onRemove = { onRemoveTrack(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueTrackItemRow(
    index: Int,
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val artistText = remember(track) {
        val names = track.artists.joinToString(" / ") { it.name }
        val album = track.album.title
        if (album.isNotBlank()) "$names - $album" else names
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isCurrent) colors.accentSubtle else Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        BasicComponent(
            startAction = {
                Box(
                    modifier = Modifier.width(32.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (isCurrent) {
                        if (isPlaying) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_volume_high),
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_mini_play),
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "${index + 1}",
                            fontSize = 13.sp,
                            color = colors.textTertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            title = track.title,
            titleColor = BasicComponentDefaults.titleColor(
                color = if (isCurrent) colors.accent else colors.textPrimary
            ),
            summary = artistText,
            summaryColor = BasicComponentDefaults.summaryColor(
                color = if (isCurrent) colors.accent.copy(alpha = 0.82f) else colors.textSecondary
            ),
            endActions = {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_close),
                        contentDescription = "移出播放列表",
                        tint = colors.textTertiary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            },
            insideMargin = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            onClick = onClick
        )
    }
}

