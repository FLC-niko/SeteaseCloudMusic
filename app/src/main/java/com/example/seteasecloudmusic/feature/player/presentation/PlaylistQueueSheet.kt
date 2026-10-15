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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * 播放列表二级菜单底栏 (当前待播清单与播放顺序)
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    // 打开时自动平滑滚动到当前正在播放的曲目
    LaunchedEffect(playbackState.currentQueueIndex) {
        if (playbackState.currentQueueIndex in playbackState.queueTracks.indices) {
            val target = (playbackState.currentQueueIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color(0xFFE0E0E0),
                width = 36.dp,
                height = 4.dp
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
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
                        color = Color(0xFF1E1E24)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${playbackState.queueTracks.size})",
                        fontSize = 14.sp,
                        color = Color(0xFF8E8E93),
                        fontWeight = FontWeight.Normal
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 播放模式切换胶囊
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF2F2F7))
                            .clickable(onClick = onTogglePlaybackMode)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        val isShuffle = playbackState.playbackMode == PlaybackMode.SHUFFLE
                        Icon(
                            imageVector = if (isShuffle) Icons.Filled.Shuffle else Icons.Filled.Repeat,
                            contentDescription = null,
                            tint = if (isShuffle) Color(0xFFFA233B) else Color(0xFF2C2C2C),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isShuffle) "随机播放" else "顺序播放",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isShuffle) Color(0xFFFA233B) else Color(0xFF2C2C2C)
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
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "清空播放列表",
                                tint = Color(0xFF8E8E93),
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
                color = Color(0xFFEBEBEF)
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
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFFD1D1D6),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "播放列表为空",
                            fontSize = 14.sp,
                            color = Color(0xFF8E8E93)
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) Color(0xFFFA233B).copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 序号或当前播放状态图标
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isCurrent) {
                Icon(
                    imageVector = if (isPlaying) Icons.AutoMirrored.Filled.VolumeUp else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFFFA233B),
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = "${index + 1}",
                    fontSize = 13.sp,
                    color = Color(0xFFC7C7CC),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 歌曲标题与副标题（歌手 / 专辑）
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontSize = 15.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) Color(0xFFFA233B) else Color(0xFF1E1E24),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            val artistText = remember(track) {
                val names = track.artists.joinToString(" / ") { it.name }
                val album = track.album.title
                if (album.isNotBlank()) "$names - $album" else names
            }
            Text(
                text = artistText,
                fontSize = 12.sp,
                color = if (isCurrent) Color(0xFFFA233B).copy(alpha = 0.82f) else Color(0xFF8E8E93),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 右侧从队列移除按钮
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "移出播放列表",
                tint = Color(0xFFC7C7CC),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

