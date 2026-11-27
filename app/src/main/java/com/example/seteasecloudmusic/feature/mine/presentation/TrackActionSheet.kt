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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Close
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
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
import com.example.seteasecloudmusic.core.download.DownloadEnqueueResult
import com.example.seteasecloudmusic.core.download.DownloadStatus
import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.FavoriteTracksManager
import com.example.seteasecloudmusic.core.ui.adaptive.AdaptiveModalSheet
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import com.example.seteasecloudmusic.feature.main.PlayerEntryPoints
import com.example.seteasecloudmusic.feature.player.presentation.ResolvedAudioDetails

/**
 * 歌曲操作菜单的全局导航入口：从任意页面打开歌手详情页或专辑详情页。
 */
data class TrackActionNavigator(
    val onOpenArtist: (artistId: Long, artistName: String, artistCoverUrl: String?) -> Unit,
    val onOpenAlbum: (albumId: Long, albumTitle: String, artistName: String, coverUrl: String?) -> Unit
)

val LocalTrackNavigation = staticCompositionLocalOf<TrackActionNavigator?> { null }

/**
 * 歌曲操作面板页面状态：
 * ACTIONS: 默认动作菜单（下一首播放、喜欢/取消喜欢、收藏到歌单、下载、歌手、专辑）
 * ADD_TO_PLAYLIST: 收藏到歌单（内嵌切页，带返回按钮与新建歌单入口）
 * SELECT_ARTIST: 多歌手选择（内嵌切页，选择后跳转到对应艺人页）
 */
enum class TrackActionSheetPage {
    ACTIONS,
    ADD_TO_PLAYLIST,
    SELECT_ARTIST
}

/**
 * 歌单曲目与播放页共用的歌曲更多操作二级菜单（底栏抽屉）。
 * 菜单项顺序：
 * 1. 下一首播放
 * 2. 喜欢 / 取消喜欢
 * 3. 收藏到歌单
 * 4. 下载
 * 5. 歌手
 * 6. 专辑
 * 7. 音频详细信息（可选，仅在传入 [audioDetails] 时展示）
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
    onTrackAddedToPlaylist: ((Long, Track) -> Unit)? = null,
    onArtistClick: ((Artist) -> Unit)? = null,
    onAlbumClick: ((albumId: Long, albumTitle: String, artistName: String, coverUrl: String?) -> Unit)? = null
) {
    val context = LocalContext.current
    val navigator = LocalTrackNavigation.current
    var currentPage by remember(track.id) { mutableStateOf(TrackActionSheetPage.ACTIONS) }
    val colors = AppTheme.colors

    val isLocalTrack = remember(track) {
        track.audioQualityLevel == "local" ||
            track.id < 0L ||
            track.playableUrl?.let { url ->
                url.startsWith("content:") || url.startsWith("file:") || url.startsWith("/")
            } == true
    }
    val navigableArtists = remember(track, isLocalTrack) {
        if (isLocalTrack) {
            emptyList()
        } else {
            track.artists
                .filter { it.id > 0L && it.name.isNotBlank() }
                .distinctBy { it.id }
        }
    }
    val hasNavigableAlbum = !isLocalTrack && track.album.id > 0L

    val navigateToArtist: (Artist) -> Unit = { artist ->
        onDismiss()
        if (onArtistClick != null) {
            onArtistClick(artist)
        } else {
            navigator?.onOpenArtist(artist.id, artist.name, artist.coverUrl)
        }
    }

    AdaptiveModalSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        modifier = modifier
    ) {
        when (currentPage) {
            TrackActionSheetPage.ADD_TO_PLAYLIST -> {
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
            }

            TrackActionSheetPage.SELECT_ARTIST -> {
                SelectArtistContent(
                    artists = navigableArtists,
                    onBack = { currentPage = TrackActionSheetPage.ACTIONS },
                    onDismiss = onDismiss,
                    onArtistClick = navigateToArtist
                )
            }

            TrackActionSheetPage.ACTIONS -> {
                Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            ) {
            // 头部歌曲信息预览区：左侧封面，右侧歌名与歌手名
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 歌曲封面
                val coverUrl = (track.coverUrl ?: track.album.coverUrl)?.takeIf { it.isNotBlank() }
                Box(
                    modifier = Modifier
                        .size(60.dp) // 头部预览封面尺寸
                        .clip(RoundedCornerShape(8.dp)) // 封面圆角
                        .background(colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (coverUrl != null) {
                        AsyncImage(
                            model = coverUrl.toCoverThumbnailUrl(180),
                            contentDescription = track.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = MiuixIcons.Music,
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
                    // 歌名
                    Text(
                        text = track.title,
                        style = MiuixTheme.textStyles.headline1.copy(
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
                        style = MiuixTheme.textStyles.footnote1.copy(
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val likedTrackIds by FavoriteTracksManager.likedTrackIds.collectAsState()
            val isLiked = track.id in likedTrackIds
            val artistNames = track.artists.joinToString(" / ") { it.name }.ifBlank { "未知歌手" }
            val albumTitle = track.album.title.ifBlank { "未知专辑" }
            val downloadManager = remember { PlayerEntryPoints.downloadManagerOrNull() }
            val downloadTasks by (downloadManager?.tasksFlow?.collectAsState()
                ?: remember { androidx.compose.runtime.mutableStateOf(emptyList()) })
            val downloadTask = downloadTasks.firstOrNull { it.trackId == track.id }

            Card(
                colors = CardDefaults.defaultColors(color = colors.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
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
                    badge = downloadTask?.let { task ->
                        {
                            val statusText = when (task.status) {
                                DownloadStatus.PENDING -> "等待中"
                                DownloadStatus.DOWNLOADING -> "${task.progress}%"
                                DownloadStatus.COMPLETED -> "已下载"
                                DownloadStatus.FAILED -> "失败 · 重试"
                                DownloadStatus.PAUSED -> "已暂停"
                            }
                            Text(
                                text = statusText,
                                style = MiuixTheme.textStyles.footnote1.copy(
                                    color = if (task.status == DownloadStatus.FAILED) colors.accent else colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    },
                    onClick = {
                        val message = when {
                            downloadManager == null -> "下载服务未初始化"
                            downloadTask?.status == DownloadStatus.FAILED ||
                                downloadTask?.status == DownloadStatus.PAUSED -> {
                                downloadManager.resume(track.id)
                                "已重新加入下载队列"
                            }
                            else -> when (downloadManager.enqueue(track)) {
                                DownloadEnqueueResult.ENQUEUED -> "已加入下载队列"
                                DownloadEnqueueResult.ALREADY_DOWNLOADED -> "该歌曲已下载到本地"
                                DownloadEnqueueResult.ALREADY_IN_QUEUE -> "该歌曲已在下载队列中"
                                DownloadEnqueueResult.ALREADY_LOCAL -> "该歌曲已是本地音频"
                            }
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )

                // 5. 歌手
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
                        val canNavigateArtist = onArtistClick != null || navigator != null
                        when {
                            !canNavigateArtist || navigableArtists.isEmpty() -> {
                                Toast.makeText(context, "暂无该歌手详情页", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                            navigableArtists.size == 1 -> {
                                navigateToArtist(navigableArtists.first())
                            }
                            else -> {
                                currentPage = TrackActionSheetPage.SELECT_ARTIST
                            }
                        }
                    }
                )

                // 6. 专辑
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
                        val canNavigateAlbum = onAlbumClick != null || navigator != null
                        if (canNavigateAlbum && hasNavigableAlbum) {
                            val albumCover = (track.album.coverUrl ?: track.coverUrl)?.takeIf { it.isNotBlank() }
                            onDismiss()
                            if (onAlbumClick != null) {
                                onAlbumClick(track.album.id, albumTitle, artistNames, albumCover)
                            } else {
                                navigator?.onOpenAlbum(track.album.id, albumTitle, artistNames, albumCover)
                            }
                        } else {
                            Toast.makeText(context, "暂无该专辑详情页", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
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
}
}

@Composable
private fun SelectArtistContent(
    artists: List<Artist>,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onArtistClick: (Artist) -> Unit
) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = MiuixIcons.Back,
                    contentDescription = "返回",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "选择歌手",
                style = MiuixTheme.textStyles.headline1.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = MiuixIcons.Basic.Close,
                    contentDescription = "关闭",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(
            color = colors.dividerSubtle,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            colors = CardDefaults.defaultColors(color = colors.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            artists.forEach { artist ->
                BasicComponent(
                    startAction = {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            val coverUrl = artist.coverUrl?.takeIf { it.isNotBlank() }
                            if (coverUrl != null) {
                                AsyncImage(
                                    model = coverUrl.toCoverThumbnailUrl(160),
                                    contentDescription = artist.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_action_artist),
                                    contentDescription = null,
                                    tint = colors.iconSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    endActions = {
                        Icon(
                            imageVector = MiuixIcons.ChevronForward,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    onClick = { onArtistClick(artist) }
                ) {
                    Text(
                        text = artist.name,
                        style = MiuixTheme.textStyles.body1.copy(
                            color = colors.textPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
    // 箭头旋转动画（无回弹阻尼，中低刚度）
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "trackAudioDetailsChevron"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        BasicComponent(
            startAction = {
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_audio_info),
                        contentDescription = null,
                        tint = colors.iconSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            endActions = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = details.summaryBadge,
                        color = colors.textSecondary,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = MiuixIcons.ExpandMore,
                        contentDescription = if (isExpanded) "收起音频详细信息" else "展开音频详细信息",
                        tint = colors.textSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = chevronRotation }
                    )
                }
            },
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
            onClick = { isExpanded = !isExpanded }
        ) {
            Text(
                text = "音频详细信息",
                style = MiuixTheme.textStyles.body1.copy(
                    color = colors.textPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 音频详情面板展开/收起动画（无回弹弹簧 + 渐显 180ms / 渐隐 140ms）
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
            val chipBg = if (isDark) Color(0xFF3A3A3E) else Color(0xFFE9E9EE) // 规格标签背景色（深色/浅色）
            val chipText = if (isDark) Color(0xFFE5E5EA) else Color(0xFF3A3A3C) // 规格标签文字颜色（深色/浅色）
            val detailsBg = if (isDark) Color.Black.copy(alpha = 0.25f) else colors.surface // 音频详情面板底色
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(detailsBg)
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
    BasicComponent(
        startAction = {
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
        },
        endActions = badge?.let { { it() } },
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
        onClick = onClick
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.body1.copy(
                color = AppTheme.colors.textPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Normal
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
