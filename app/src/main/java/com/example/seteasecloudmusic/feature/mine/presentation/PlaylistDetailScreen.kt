package com.example.seteasecloudmusic.feature.mine.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.MusicNote
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
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle

private val PlaylistPageBg = Color(0xFFF7F7FA)
private val PlaylistTextPrimary = Color(0xFF111111)
private val PlaylistTextSecondary = Color(0xFF8E8E93)
private val PlaylistDividerColor = Color(0xFFEAEAEE)
private val PlaylistAccentColor = Color(0xFFFA233B)

@Composable
fun PlaylistDetailScreen(
    detail: PlaylistDetail,
    backdrop: Backdrop,
    onClose: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    BackHandler(onBack = onClose)

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = PlaylistPageBg
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFE8ED).copy(alpha = 0.72f),
                            PlaylistPageBg,
                            PlaylistPageBg
                        )
                    )
                )
        ) {
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
                        backdrop = backdrop,
                        onPlayAll = onPlayAll
                    )
                }

                // 2. 歌曲列表头部统计 (优先使用真实总歌曲数 detail.trackCount)
                item(key = "track_count_header") {
                    val totalCount = if (detail.trackCount > 0) detail.trackCount else detail.tracks.size
                    val countText = if (totalCount > 0) {
                        "歌曲列表 ($totalCount)"
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

            // 顶部固定状态栏与液态玻璃返回栏
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(statusBarHeight)
                )

                PlaylistDetailTopBar(
                    backdrop = backdrop,
                    title = detail.name,
                    onClose = onClose
                )
            }
        }
    }
}

/**
 * 顶部悬浮返回导航栏
 */
@Composable
private fun PlaylistDetailTopBar(
    backdrop: Backdrop,
    title: String,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(0.dp) },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(16f.dp.toPx(), 32f.dp.toPx())
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.46f)) }
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
                    .background(Color.White.copy(alpha = 0.30f))
                    .border(1.dp, Color.White.copy(alpha = 0.64f), CircleShape)
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

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlaylistTextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
    backdrop: Backdrop,
    onPlayAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(24.dp) },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(16f.dp.toPx(), 32f.dp.toPx())
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.42f)) }
            )
            .border(1.dp, Color.White.copy(alpha = 0.62f), RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 封面图或默认图标
        if (!detail.coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = detail.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(136.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.56f), RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFA233B), Color(0xFFFF9EAE))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.56f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

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
        val count = if (detail.trackCount > 0) detail.trackCount else detail.tracks.size
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
            enabled = count > 0,
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

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF0F0F3)),
            contentAlignment = Alignment.Center
        ) {
            if (!track.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = PlaylistAccentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

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

            val artistText = track.artists.joinToString(" / ") { it.name }.ifBlank { "未知歌手" }
            val albumText = track.album?.title ?: ""
            val subtitle = if (albumText.isNotBlank() && albumText != "本地音乐") {
                "$artistText · $albumText"
            } else {
                artistText
            }

            Text(
                text = subtitle,
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
