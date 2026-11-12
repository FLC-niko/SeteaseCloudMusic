package com.example.seteasecloudmusic.feature.mine.presentation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.R
import com.example.seteasecloudmusic.core.common.toCoverThumbnailUrl
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.ui.adaptive.AdaptiveModalSheet
import com.example.seteasecloudmusic.feature.main.PlayerEntryPoints

/**
 * 歌单曲目更多操作二级菜单（底栏抽屉）。
 * 背景为纯白色，顺序严格为：
 * 1. 下一首播放
 * 2. 收藏到歌单
 * 3. 下载
 * 4. 歌手
 * 5. 专辑
 */
@Composable
fun TrackActionSheet(
    track: Track,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AdaptiveModalSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                        .background(Color(0xFFF2F2F7)),
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
                            tint = Color(0xFF8E8E93),
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
                            color = Color(0xFF1E1E24),
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
                            color = Color(0xFF8E8E93),
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            HorizontalDivider(
                color = Color(0xFFF0F0F2),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 1. 下一首播放
            TrackActionItem(
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_play_next),
                        contentDescription = "下一首播放",
                        tint = Color(0xFF515151),
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

            // 2. 收藏到歌单
            TrackActionItem(
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_add_to_playlist),
                        contentDescription = "收藏到歌单",
                        tint = Color(0xFF515151),
                        modifier = Modifier.size(22.dp)
                    )
                },
                title = "收藏到歌单",
                onClick = {
                    Toast.makeText(context, "已收藏到歌单", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            // 3. 下载
            TrackActionItem(
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_download),
                        contentDescription = "下载",
                        tint = Color(0xFF515151),
                        modifier = Modifier.size(22.dp)
                    )
                },
                title = "下载",
                onClick = {
                    Toast.makeText(context, "已加入下载队列", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            // 4. 歌手
            val artistNames = track.artists.joinToString(" / ") { it.name }.ifBlank { "未知歌手" }
            TrackActionItem(
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_artist),
                        contentDescription = "歌手",
                        tint = Color(0xFF515151),
                        modifier = Modifier.size(22.dp)
                    )
                },
                title = "歌手：$artistNames",
                onClick = {
                    Toast.makeText(context, "歌手：$artistNames", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            // 5. 专辑
            val albumTitle = track.album.title.ifBlank { "未知专辑" }
            TrackActionItem(
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_album),
                        contentDescription = "专辑",
                        tint = Color(0xFF515151),
                        modifier = Modifier.size(22.dp)
                    )
                },
                title = "专辑：$albumTitle",
                onClick = {
                    Toast.makeText(context, "专辑：$albumTitle", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )
        }
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
                color = Color(0xFF222226),
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
