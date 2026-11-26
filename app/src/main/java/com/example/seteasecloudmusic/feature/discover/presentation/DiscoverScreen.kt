package com.example.seteasecloudmusic.feature.discover.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.seteasecloudmusic.core.ui.adaptive.*
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.R
import com.example.seteasecloudmusic.core.common.toCoverThumbnailUrl
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.ui.components.AppLoadingIndicator
import com.example.seteasecloudmusic.core.ui.components.AppPullToRefresh
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleClip
import com.example.seteasecloudmusic.core.ui.components.ProgressiveTopBlur
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import com.example.seteasecloudmusic.feature.discover.domain.model.DiscoverPlaylist
import com.example.seteasecloudmusic.feature.discover.domain.model.DiscoverToplist
import com.example.seteasecloudmusic.feature.discover.domain.model.DiscoverTrackPreview
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

private val DiscoverBackground: Color
    @Composable
    get() = AppTheme.colors.backgroundSecondary
private val DiscoverPrimary: Color
    @Composable
    get() = AppTheme.colors.textPrimary
private val DiscoverSecondary: Color
    @Composable
    get() = AppTheme.colors.textSecondary
private val DiscoverDivider: Color
    @Composable
    get() = AppTheme.colors.divider
private val DiscoverAccent: Color
    @Composable
    get() = AppTheme.colors.accent
private val DiscoverSurface: Color
    @Composable
    get() = AppTheme.colors.surfaceVariant
private val DiscoverCardSurface: Color
    @Composable
    get() = AppTheme.colors.surface

/**
 * 发现页主界面，按分区展示推荐歌单、榜单、推荐新歌与热门歌单，并支持下拉刷新与顶部渐进模糊。
 */
@Composable
fun DiscoverScreen(
    uiState: DiscoverUiState,
    topContentPadding: Dp,
    bottomContentPadding: Dp,
    onRetryPersonalized: () -> Unit,
    onRetryHotPlaylists: () -> Unit,
    onRetryNewsongs: () -> Unit,
    onRetryToplists: () -> Unit,
    onRefreshAll: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val discoverBgState = rememberUpdatedState(DiscoverBackground)
    val discoverBackdrop = rememberLayerBackdrop {
        drawRect(discoverBgState.value)
        drawContent()
    }
    val hasAnyContent = uiState.personalizedPlaylists.isNotEmpty() ||
        uiState.hotPlaylists.isNotEmpty() ||
        uiState.newsongs.isNotEmpty() ||
        uiState.toplists.isNotEmpty()
    val isRefreshing = hasAnyContent && (
        uiState.isPersonalizedLoading ||
            uiState.isHotPlaylistsLoading ||
            uiState.isNewsongsLoading ||
            uiState.isToplistsLoading
        )
    val topBlurAlpha by remember(listState, density) {
        // 顶部渐进模糊从透明过渡到完全显示的滚动距离（越小越早触发）
        val fadeDistancePx = with(density) { 40.dp.toPx().coerceAtLeast(1f) }
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / fadeDistancePx).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscoverBackground)
    ) {
        AppPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRefreshAll,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(discoverBackdrop),
            contentPadding = PaddingValues(top = topContentPadding)
        ) {
        AdaptiveSectionColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = topContentPadding, bottom = bottomContentPadding + 16.dp), // 底部在悬浮栏之上追加安全留白
            verticalArrangement = Arrangement.spacedBy(20.dp) // 各发现分区之间的垂直间距
        ) {
            item {
                DiscoverChipSection(
                    title = "推荐歌单",
                    playlists = uiState.personalizedPlaylists.ifEmpty { uiState.hotPlaylists },
                    isLoading = uiState.isPersonalizedLoading,
                    errorMessage = uiState.personalizedErrorMessage,
                    onRetry = onRetryPersonalized
                )
            }

            item {
                DiscoverNowPlayingSection(
                    toplists = uiState.toplists,
                    isLoading = uiState.isToplistsLoading,
                    errorMessage = uiState.toplistsErrorMessage,
                    onRetry = onRetryToplists
                )
            }

            item {
                Column {
                DiscoverSectionHeader(
                    title = "推荐新歌",
                    subtitle = "近期值得听的新鲜声音"
                )
                DiscoverSongSection(
                    tracks = uiState.newsongs,
                    isLoading = uiState.isNewsongsLoading,
                    errorMessage = uiState.newsongsErrorMessage,
                    onRetry = onRetryNewsongs
                )
                }
            }

            item {
                Column {
                DiscoverSectionHeader(
                    title = "热门歌单",
                    subtitle = "持续更新的流行内容"
                )
                DiscoverPlaylistListSection(
                    playlists = uiState.hotPlaylists,
                    isLoading = uiState.isHotPlaylistsLoading,
                    errorMessage = uiState.hotPlaylistsErrorMessage,
                    onRetry = onRetryHotPlaylists
                )
                }
            }
        }
        }

        ProgressiveTopBlur(
            backdrop = discoverBackdrop,
            // 顶部模糊区高度，至少覆盖状态栏加顶栏最小高度（56.dp）
            totalHeight = (topContentPadding + 16.dp).coerceAtLeast(statusBarHeight + 56.dp),
            statusBarHeight = statusBarHeight,
            modifier = Modifier.align(Alignment.TopCenter),
            alphaProvider = { topBlurAlpha }
        )
    }
}

@Composable
private fun DiscoverSectionHeader(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp) // 分区标题水平安全边距
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = DiscoverPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = null,
                tint = DiscoverSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = DiscoverSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DiscoverChipSection(
    title: String,
    playlists: List<DiscoverPlaylist>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DiscoverSectionHeader(
            title = title,
            subtitle = "根据你的口味推荐"
        )

        when {
            isLoading && playlists.isEmpty() -> DiscoverLoadingRow()
            !errorMessage.isNullOrBlank() && playlists.isEmpty() -> DiscoverErrorPanel(errorMessage, onRetry)
            playlists.isEmpty() -> DiscoverEmptyPanel("暂无推荐歌单")
            else -> LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp) // 横向推荐歌单卡片间距
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    DiscoverChipCard(playlist = playlist)
                }
            }
        }
    }
}

@Composable
private fun DiscoverChipCard(
    playlist: DiscoverPlaylist
) {
    Column(
        modifier = Modifier.width(136.dp), // 推荐歌单卡片宽度
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            cornerRadius = 28.dp, // 歌单卡片圆角
            colors = CardDefaults.defaultColors(color = DiscoverCardSurface),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .squircleBorder(1.dp, DiscoverDivider, 28.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = playlist.coverUrl.toCoverThumbnailUrl(200), // 缩略图请求尺寸（px）
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.16f)) // 封面高光提亮遮罩透明度
                )
            }
        }
        Text(
            text = playlist.name,
            color = DiscoverPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        playlist.copywriter?.let { copywriter ->
            Text(
                text = copywriter,
                color = DiscoverSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DiscoverNowPlayingSection(
    toplists: List<DiscoverToplist>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DiscoverSectionHeader(
            title = "榜单",
            subtitle = "榜单热点与播放摘要"
        )

        when {
            isLoading && toplists.isEmpty() -> DiscoverLoadingRow(cardHeight = 420.dp) // 榜单加载占位卡片高度
            !errorMessage.isNullOrBlank() && toplists.isEmpty() -> DiscoverErrorPanel(errorMessage, onRetry)
            toplists.isEmpty() -> DiscoverEmptyPanel("暂无榜单内容")
            else -> LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp) // 榜单卡片横向间距
            ) {
                items(toplists, key = { it.id }) { toplist ->
                    DiscoverToplistCard(toplist = toplist)
                }
            }
        }
    }
}

@Composable
private fun DiscoverToplistCard(
    toplist: DiscoverToplist
) {
    Card(
        cornerRadius = 28.dp,
        colors = CardDefaults.defaultColors(color = DiscoverCardSurface),
        modifier = Modifier
            .width(320.dp) // 榜单大卡片宽度
            .squircleBorder(1.dp, DiscoverDivider, 28.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp) // 榜单顶部封面区高度
                    .background(DiscoverSurface)
            ) {
                AsyncImage(
                    model = toplist.coverUrl.toCoverThumbnailUrl(320), // 榜单封面缩略图请求尺寸（px）
                    contentDescription = toplist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.15f)
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = toplist.name,
                    color = DiscoverPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                toplist.updateFrequency?.let {
                    Text(
                        text = it,
                        color = DiscoverSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                // 每张榜单卡片最多预览前 3 首代表曲目
                toplist.previews.take(3).forEachIndexed { index, preview ->
                    DiscoverPreviewRow(index = index + 1, preview = preview)
                }
            }
        }
    }
}

@Composable
private fun DiscoverPreviewRow(
    index: Int,
    preview: DiscoverTrackPreview
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = index.toString(),
            color = DiscoverAccent,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.width(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preview.title,
                color = DiscoverPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = preview.artistName,
                color = DiscoverSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DiscoverSongSection(
    tracks: List<Track>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    when {
        isLoading && tracks.isEmpty() -> DiscoverLoadingColumn(rowCount = 4)
        !errorMessage.isNullOrBlank() && tracks.isEmpty() -> DiscoverErrorPanel(errorMessage, onRetry)
        tracks.isEmpty() -> DiscoverEmptyPanel("暂无新歌")
        else -> Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .squircleClip(24.dp)
                .background(DiscoverCardSurface)
        ) {
            // 推荐新歌列表最多展示 10 首
            tracks.take(10).forEachIndexed { index, track ->
                DiscoverSongRow(track = track)
                if (index != tracks.take(10).lastIndex) {
                    HorizontalDivider(color = DiscoverDivider, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun DiscoverSongRow(
    track: Track
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            cornerRadius = 14.dp,
            colors = CardDefaults.defaultColors(color = DiscoverSurface),
            modifier = Modifier.size(56.dp) // 新歌行封面尺寸
        ) {
            AsyncImage(
                model = track.coverUrl.toCoverThumbnailUrl(140), // 新歌封面缩略图请求尺寸（px）
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = DiscoverPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artists.joinToString(" / ") { it.name },
                color = DiscoverSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = { }) {
            Icon(
                imageVector = MiuixIcons.More,
                contentDescription = "更多",
                tint = DiscoverSecondary
            )
        }
    }
}

@Composable
private fun DiscoverPlaylistListSection(
    playlists: List<DiscoverPlaylist>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    when {
        isLoading && playlists.isEmpty() -> DiscoverLoadingColumn(rowCount = 3)
        !errorMessage.isNullOrBlank() && playlists.isEmpty() -> DiscoverErrorPanel(errorMessage, onRetry)
        playlists.isEmpty() -> DiscoverEmptyPanel("暂无热门歌单")
        else -> Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .squircleClip(24.dp)
                .background(DiscoverCardSurface)
        ) {
            // 热门歌单分区最多展示 6 个条目
            playlists.take(6).forEachIndexed { index, playlist ->
                DiscoverPlaylistRow(playlist = playlist)
                if (index != playlists.take(6).lastIndex) {
                    HorizontalDivider(color = DiscoverDivider, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun DiscoverPlaylistRow(
    playlist: DiscoverPlaylist
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            cornerRadius = 16.dp,
            colors = CardDefaults.defaultColors(color = DiscoverSurface),
            modifier = Modifier.size(72.dp) // 热门歌单封面尺寸
        ) {
            AsyncImage(
                model = playlist.coverUrl.toCoverThumbnailUrl(160), // 热门歌单封面缩略图请求尺寸（px）
                contentDescription = playlist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                color = DiscoverPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    playlist.playCount?.let { append("${formatPlayCount(it)} 次播放") }
                    playlist.trackCount?.let {
                        if (isNotEmpty()) append(" · ")
                        append("$it 首")
                    }
                    playlist.copywriter?.let {
                        if (isNotEmpty()) append(" · ")
                        append(it)
                    }
                },
                color = DiscoverSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_warning),
            contentDescription = null,
            tint = DiscoverSecondary
        )
    }
}

@Composable
private fun DiscoverLoadingRow(
    cardHeight: Dp = 120.dp // 默认横向加载占位卡片高度
) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) { // 占位卡片数量
            Box(
                modifier = Modifier
                    .width(132.dp) // 占位卡片宽度
                    .height(cardHeight)
                    .squircleClip(28.dp)
                    .background(DiscoverCardSurface),
                contentAlignment = Alignment.Center
            ) {
                AppLoadingIndicator(
                    color = DiscoverAccent,
                    strokeWidth = 2.dp,
                    size = 20.dp
                )
            }
        }
    }
}

@Composable
private fun DiscoverLoadingColumn(
    rowCount: Int
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .squircleClip(24.dp)
            .background(DiscoverCardSurface)
    ) {
        repeat(rowCount) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .squircleClip(14.dp)
                        .background(DiscoverSurface),
                    contentAlignment = Alignment.Center
                ) {
                    AppLoadingIndicator(color = DiscoverAccent, strokeWidth = 2.dp, size = 18.dp)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.height(14.dp).fillMaxWidth(0.68f).background(DiscoverSurface))
                    Box(modifier = Modifier.height(10.dp).fillMaxWidth(0.40f).background(DiscoverSurface))
                }
            }
            if (index != rowCount - 1) {
                HorizontalDivider(color = DiscoverDivider, thickness = 1.dp)
            }
        }
    }
}

@Composable
private fun DiscoverErrorPanel(
    message: String,
    onRetry: () -> Unit
) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .squircleClip(24.dp)
            .background(DiscoverCardSurface)
            .padding(20.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = null,
                tint = colors.errorIcon,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "内容加载受阻",
                color = DiscoverPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = message,
            color = DiscoverSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        Text(
            text = "提示：请检查设备网络连接后重试",
            color = DiscoverSecondary,
            fontSize = 12.sp
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColorsPrimary()
        ) {
            Icon(painter = painterResource(R.drawable.ic_refresh), contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "重新加载")
        }
    }
}

@Composable
private fun DiscoverEmptyPanel(
    message: String
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .squircleClip(24.dp)
            .background(DiscoverCardSurface)
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = DiscoverSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatPlayCount(playCount: Long): String {
    return when {
        playCount >= 100_000_000L -> "${(playCount / 100_000_000L)}亿"
        playCount >= 10_000L -> "${(playCount / 10_000L)}万"
        else -> playCount.toString()
    }
}
