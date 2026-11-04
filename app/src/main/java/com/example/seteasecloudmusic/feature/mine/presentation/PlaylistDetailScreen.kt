package com.example.seteasecloudmusic.feature.mine.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.seteasecloudmusic.core.ui.adaptive.*
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.seteasecloudmusic.core.common.toCoverThumbnailUrl
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.ui.components.AppleMusicCollapsedTopBar
import com.example.seteasecloudmusic.core.ui.components.rememberAppleMusicCollapseFraction
import com.example.seteasecloudmusic.core.ui.components.verticalElasticOverscroll
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.kyant.backdrop.Backdrop

private val PlaylistPageBg = Color(0xFFF7F7FA)
private val PlaylistTextPrimary = Color(0xFF111111)
private val PlaylistTextSecondary = Color(0xFF8E8E93)
private val PlaylistDividerColor = Color(0xFFEAEAEE)
private val PlaylistAccentColor = Color(0xFFFA233B)

private val PlaylistDetail.displayTitle: String
    get() = if (name.endsWith("喜欢的音乐")) "我喜欢的音乐" else name

@Composable
fun PlaylistDetailScreen(
    detail: PlaylistDetail,
    backdrop: Backdrop,
    onClose: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    embedded: Boolean = false,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState()
) {
    val bottom = LocalContentBottomPadding.current
    val window = LocalAdaptiveWindow.current
    BoxWithConstraints(modifier.fillMaxSize().background(PlaylistPageBg)) {
        val split = !embedded && ((maxWidth >= 720.dp && maxHeight >= 360.dp) || window.isBook || window.isTabletop)
        if (embedded || split) {
            BackHandler(onBack = onClose)
            val songs: @Composable () -> Unit = {
                LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(top = 12.dp, bottom = bottom + 16.dp)) {
                    item(key = "detail_title") { MusicPaneHeader(if (embedded) detail.displayTitle else "歌曲", "${detail.trackCount.coerceAtLeast(detail.tracks.size)} 首歌曲", if (embedded) onClose else null) }
                    if (embedded) item(key = "playlist_hero") { Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) { PlaylistHeroSection(detail, onPlayAll) } }
                    if (isLoading && detail.tracks.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PlaylistAccentColor) } }
                    itemsIndexed(detail.tracks, key = { _, track -> track.id }) { index, track ->
                        Box(Modifier.padding(horizontal = 12.dp)) {
                            PlaylistDetailTrackRow(index + 1, track, { onPlayTrack(track) })
                        }
                        HorizontalDivider(Modifier.padding(start = 88.dp, end = 20.dp), color = PlaylistDividerColor)
                    }
                }
            }
            Box(Modifier.fillMaxSize().statusBarsPadding()) {
                if (split) AdaptiveTwoPane(Modifier.fillMaxSize(), primaryFraction = 0.38f,
                    primary = {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
                            MusicPaneHeader("歌单", "为此刻，挑选喜欢的声音", onClose)
                            PlaylistHeroSection(detail, onPlayAll)
                        }
                    }, secondary = songs)
                else songs()
            }
        } else CompactPlaylistDetailScreen(detail, backdrop, onClose, onPlayTrack, onPlayAll, isLoading = isLoading, lazyListState = listState)
    }
}

@Composable
private fun CompactPlaylistDetailScreen(
    detail: PlaylistDetail,
    backdrop: Backdrop,
    onClose: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    lazyListState: androidx.compose.foundation.lazy.LazyListState
) {
    BackHandler(onBack = onClose)

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val collapseFractionState = rememberAppleMusicCollapseFraction(
        lazyListState = lazyListState,
        collapseThresholdDp = 140.dp
    )

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
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalElasticOverscroll(),
                contentPadding = PaddingValues(
                    top = statusBarHeight + 14.dp,
                    bottom = LocalContentBottomPadding.current + 16.dp,
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

            // 顶部 Apple Music 风格渐变模糊导航栏（使用轻量级磨砂玻璃，滑动与弹出 0 卡顿）
            AppleMusicCollapsedTopBar(
                title = detail.displayTitle,
                collapseFraction = collapseFractionState.value,
                statusBarHeight = statusBarHeight,
                backdrop = null,
                showBackButton = true,
                onBackClick = onClose,
                modifier = Modifier.align(Alignment.TopCenter)
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
            .background(Color.White.copy(alpha = 0.65f))
            .border(1.dp, Color.White.copy(alpha = 0.75f), RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 封面图或默认图标
        if (!detail.coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = detail.coverUrl.toCoverThumbnailUrl(300),
                contentDescription = null,
                modifier = Modifier
                    .size(136.dp)
                    .clip(RoundedCornerShape(20.dp)),
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
                    ),
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
        val isLiked = detail.name.endsWith("喜欢的音乐")
        Text(
            text = detail.displayTitle,
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
        val creatorSuffix = if (!detail.creatorName.isNullOrBlank() && !isLiked) " · ${detail.creatorName}" else ""
        Text(
            text = "$count 首歌曲$creatorSuffix",
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
                    model = track.coverUrl.toCoverThumbnailUrl(140),
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
            val albumText = track.album.title
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
