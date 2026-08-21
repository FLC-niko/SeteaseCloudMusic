package com.example.seteasecloudmusic.feature.mine.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail

private val PlaylistPageBg = Color.White
private val PlaylistTextPrimary = Color(0xFF111111)
private val PlaylistTextSecondary = Color(0xFF8E8E93)
private val PlaylistDividerColor = Color(0xFFEAEAEE)
private val PlaylistAccentColor = Color(0xFFFA233B)

@Composable
fun PlaylistDetailScreen(
    detail: PlaylistDetail,
    isLoading: Boolean = false,
    onClose: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = PlaylistPageBg
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = statusBarHeight + 62.dp,
                    bottom = 160.dp,
                    start = 20.dp,
                    end = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 歌单封面与元信息 Hero 区域（秒开立即显示）
                item(key = "playlist_hero") {
                    PlaylistHeroSection(
                        detail = detail,
                        onPlayAll = onPlayAll
                    )
                }

                // 2. 歌曲列表头部统计
                item(key = "track_count_header") {
                    val countText = if (detail.tracks.isNotEmpty()) {
                        "歌曲列表 (${detail.tracks.size})"
                    } else if (detail.trackCount > 0) {
                        "歌曲列表 (${detail.trackCount})"
                    } else {
                        "歌曲列表"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = countText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlaylistTextPrimary
                            )
                        )
                    }
                }

                // 3. 曲目列表（若正在异步加载曲目展示柔和指示器）
                if (detail.tracks.isEmpty() && isLoading) {
                    item(key = "loading_tracks") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = PlaylistAccentColor,
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = detail.tracks,
                        key = { _, track -> track.id }
                    ) { index, track ->
                        PlaylistDetailTrackRow(
                            index = index + 1,
                            track = track,
                            onClick = { onPlayTrack(track) }
                        )
                        if (index < detail.tracks.lastIndex) {
                            HorizontalDivider(
                                color = PlaylistDividerColor,
                                modifier = Modifier.padding(start = 76.dp)
                            )
                        }
                    }
                }
            }

            // 顶部固定手机状态栏纯白背景 + 相册级浓郁多阶渐变悬浮返回栏
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                // 手机状态栏纯白遮罩（彻底杜绝与电量/时间/灵动岛重叠）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(statusBarHeight)
                        .background(Color.White)
                )

                // 浓郁渐变磨砂返回条（相册同款多阶渐变，降低透明度一档，文字/按钮锐利清晰）
                PlaylistDetailTopBar(
                    title = detail.name,
                    onClose = onClose
                )
            }
        }
    }
}

/**
 * 顶部悬浮返回导航栏（相册级浓郁渐变质感）
 */
@Composable
private fun PlaylistDetailTopBar(
    title: String,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(
                Brush.verticalGradient(
                    0.00f to Color.White.copy(alpha = 0.98f),
                    0.40f to Color.White.copy(alpha = 0.94f),
                    0.65f to Color.White.copy(alpha = 0.76f),
                    0.85f to Color.White.copy(alpha = 0.35f),
                    1.00f to Color.White.copy(alpha = 0.0f)
                )
            )
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEFEFF4).copy(alpha = 0.95f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PlaylistTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlaylistTextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 歌单 Hero 卡片头部
 */
@Composable
private fun PlaylistHeroSection(
    detail: PlaylistDetail,
    onPlayAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF7F7F9))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 封面图
        AsyncImage(
            model = detail.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .size(136.dp)
                .clip(RoundedCornerShape(20.dp))
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 歌单标题
        Text(
            text = detail.name,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PlaylistTextPrimary
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 创建者与歌曲数
        val count = if (detail.tracks.isNotEmpty()) detail.tracks.size else detail.trackCount
        Text(
            text = "$count 首歌曲" + if (!detail.creatorName.isNullOrBlank()) " · ${detail.creatorName}" else "",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                color = PlaylistTextSecondary
            )
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 播放全部按钮
        Button(
            onClick = onPlayAll,
            modifier = Modifier
                .height(44.dp)
                .fillMaxWidth(0.65f),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PlaylistAccentColor,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "播放全部",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

/**
 * 歌单曲目单行
 */
@Composable
private fun PlaylistDetailTrackRow(
    index: Int,
    track: Track,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = PlaylistTextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            ),
            modifier = Modifier.width(28.dp)
        )

        AsyncImage(
            model = track.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = PlaylistTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = track.artists.joinToString(" / ") { it.name } + if (!track.album.title.isNullOrBlank()) " - ${track.album.title}" else "",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = PlaylistTextSecondary,
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
