package com.example.seteasecloudmusic.feature.mine.presentation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.R
import com.example.seteasecloudmusic.core.common.toCoverThumbnailUrl
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.FavoriteTracksManager
import com.example.seteasecloudmusic.core.ui.adaptive.AdaptiveAlertDialog
import com.example.seteasecloudmusic.core.ui.adaptive.AdaptiveModalSheet
import com.example.seteasecloudmusic.core.ui.components.AppLoadingIndicator
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Close
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.example.seteasecloudmusic.feature.mine.domain.model.UserPlaylist
import com.example.seteasecloudmusic.feature.mine.domain.repository.PlaylistTrackAlreadyExistsException
import kotlinx.coroutines.launch

/**
 * 收藏到歌单底栏弹窗：
 * 展示当前用户创建的歌单列表以及“我喜欢的音乐”，支持一键加入歌单或直接新建歌单并收藏。
 */
@Composable
fun AddToPlaylistSheet(
    track: Track,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    currentPlaylistId: Long? = null,
    viewModel: AddToPlaylistViewModel = hiltViewModel(),
    onTrackLikedChanged: ((Track, Boolean) -> Unit)? = null,
    onTrackAdded: ((playlistId: Long, track: Track) -> Unit)? = null
) {
    AdaptiveModalSheet(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.surface,
        modifier = modifier
    ) {
        AddToPlaylistContent(
            track = track,
            onDismiss = onDismiss,
            onBack = null,
            currentPlaylistId = currentPlaylistId,
            viewModel = viewModel,
            onTrackLikedChanged = onTrackLikedChanged,
            onTrackAdded = onTrackAdded
        )
    }
}

/**
 * 收藏到歌单内部核心视图：支持内嵌于 TrackActionSheet（无缝平滑切页）或单独弹窗展示
 */
@Composable
fun AddToPlaylistContent(
    track: Track,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    currentPlaylistId: Long? = null,
    viewModel: AddToPlaylistViewModel = hiltViewModel(),
    onTrackLikedChanged: ((Track, Boolean) -> Unit)? = null,
    onTrackAdded: ((playlistId: Long, track: Track) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val likedTrackIds by FavoriteTracksManager.likedTrackIds.collectAsState()
    val colors = AppTheme.colors

    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistTitle by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    // 初始化本首歌曲已存在哪些歌单中
    LaunchedEffect(track.id, currentPlaylistId) {
        viewModel.initForTrack(track.id, currentPlaylistId)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // 1. 顶部栏：返回按钮（若有）、标题与关闭按钮（已移除单曲简明预览区域）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
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
            }

            Text(
                text = "收藏到歌单",
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

        // 2. 歌单列表区域（包括首位的“新建歌单”、“我喜欢的音乐”和随后的自建歌单，完全合并流动，按数量自适应高度）
        if (!uiState.isLoggedIn) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp, horizontal = 24.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "登录网易云音乐账号后即可收藏到自建歌单",
                    style = MiuixTheme.textStyles.body2.copy(
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                )
            }
        } else if (uiState.isLoading && uiState.createdPlaylists.isEmpty() && uiState.likedPlaylist == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                contentAlignment = Alignment.Center
            ) {
                AppLoadingIndicator(
                    color = colors.accent,
                    strokeWidth = 2.5.dp,
                    size = 28.dp
                )
            }
        } else {
            // 合并歌单列表：第一项始终为“我喜欢的音乐”
            val allPlaylists = remember(uiState.likedPlaylist, uiState.createdPlaylists) {
                val liked = uiState.likedPlaylist ?: UserPlaylist(
                    id = -1L,
                    name = "我喜欢的音乐",
                    coverUrl = null,
                    trackCount = 0,
                    playCount = 0L,
                    isLikedHero = true
                )
                listOf(liked) + uiState.createdPlaylists
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                contentPadding = PaddingValues(bottom = 6.dp)
            ) {
                // 1. “新建歌单”入口：画一条分割线但依然在同一滚动流中
                item(key = "create_new_playlist") {
                    NewPlaylistPickerItem(
                        onClick = { showCreateDialog = true }
                    )
                    HorizontalDivider(
                        color = colors.dividerSubtle,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                // 2. 歌单项（首项为“我喜欢的音乐”，随后为自建歌单；我喜欢的音乐后面也加一条分割线）
                itemsIndexed(
                    items = allPlaylists,
                    key = { _, playlist -> playlist.id }
                ) { index, playlist ->
                    val isLikedHero = playlist.isLikedHero || playlist.name.contains("喜欢")
                    // 判断该歌单是否已包含当前歌曲
                    val isAlreadyAdded = if (isLikedHero) {
                        track.id in likedTrackIds ||
                            (playlist.id > 0 && playlist.id in uiState.existingPlaylistIds) ||
                            (currentPlaylistId != null && currentPlaylistId == playlist.id)
                    } else {
                        playlist.id in uiState.existingPlaylistIds ||
                            (currentPlaylistId != null && playlist.id == currentPlaylistId)
                    }

                    PlaylistPickerItem(
                        playlist = playlist,
                        isAlreadyAdded = isAlreadyAdded,
                        onClick = {
                            if (uiState.isOperating) return@PlaylistPickerItem

                            // 若歌单已存在该歌曲：给出明确提示，不重复执行网络请求，不提示“已收藏”
                            if (isAlreadyAdded) {
                                Toast.makeText(context, "歌单「${playlist.name}」中已存在该歌曲", Toast.LENGTH_SHORT).show()
                                return@PlaylistPickerItem
                            }

                            // 1. 收藏到“我喜欢的音乐”
                            if (isLikedHero) {
                                scope.launch {
                                    val result = viewModel.likeTrack(track)
                                    result.fold(
                                        onSuccess = {
                                            Toast.makeText(context, "已收藏到歌单「${playlist.name}」", Toast.LENGTH_SHORT).show()
                                            onTrackLikedChanged?.invoke(track, true)
                                            onTrackAdded?.invoke(playlist.id, track)
                                            onDismiss()
                                        },
                                        onFailure = { err ->
                                            Toast.makeText(context, err.message ?: "收藏失败，请重试", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            } else {
                                // 2. 收藏到普通自建歌单
                                scope.launch {
                                    val result = viewModel.addTrackToPlaylist(playlist.id, track.id)
                                    result.fold(
                                        onSuccess = {
                                            Toast.makeText(context, "已收藏到歌单「${playlist.name}」", Toast.LENGTH_SHORT).show()
                                            onTrackAdded?.invoke(playlist.id, track)
                                            onDismiss()
                                        },
                                        onFailure = { err ->
                                            if (err is PlaylistTrackAlreadyExistsException || err.message?.contains("存在") == true) {
                                                Toast.makeText(context, "歌单「${playlist.name}」中已存在该歌曲", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, err.message ?: "收藏失败，请重试", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    )

                    // 我喜欢的音乐下方紧跟一条细分割线，保持整洁一体
                    if (isLikedHero) {
                        HorizontalDivider(
                            color = colors.dividerSubtle,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
            }
        }
    }

    // 4. 新建歌单对话框
    if (showCreateDialog) {
        val dismissCreateDialog = {
            if (!isCreating) {
                showCreateDialog = false
                newPlaylistTitle = ""
            }
        }
        AdaptiveAlertDialog(
            onDismissRequest = dismissCreateDialog,
            title = "新建歌单",
            confirmText = "创建并收藏",
            confirmColor = colors.accent,
            confirmEnabled = newPlaylistTitle.isNotBlank() && !isCreating,
            dismissEnabled = !isCreating,
            isConfirmLoading = isCreating,
            onConfirm = {
                val title = newPlaylistTitle.trim()
                if (title.isBlank() || isCreating) return@AdaptiveAlertDialog
                isCreating = true
                scope.launch {
                    val result = viewModel.createPlaylistAndAddTrack(title, track.id)
                    isCreating = false
                    result.fold(
                        onSuccess = { newPl ->
                            Toast.makeText(context, "已创建并收藏到「${newPl.name}」", Toast.LENGTH_SHORT).show()
                            onTrackAdded?.invoke(newPl.id, track)
                            showCreateDialog = false
                            newPlaylistTitle = ""
                            onDismiss()
                        },
                        onFailure = { err ->
                            Toast.makeText(context, err.message ?: "创建失败，请重试", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            dismissText = "取消",
            onDismiss = dismissCreateDialog
        ) {
            Column {
                Text(
                    text = "输入歌单标题后将自动创建并收录本首单曲",
                    style = MiuixTheme.textStyles.footnote1.copy(color = colors.textSecondary)
                )
                Spacer(modifier = Modifier.height(12.dp))
                @Suppress("DEPRECATION")
                TextField(
                    value = newPlaylistTitle,
                    onValueChange = { newPlaylistTitle = it },
                    label = "请输入歌单标题",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    textStyle = MiuixTheme.textStyles.body1.copy(color = colors.textPrimary),
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = colors.surfaceVariant,
                        labelColor = colors.textSecondary,
                        borderColor = colors.accent
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PlaylistPickerItem(
    playlist: UserPlaylist,
    isAlreadyAdded: Boolean,
    onClick: () -> Unit
) {
    val isLikedHero = playlist.isLikedHero || playlist.name.contains("喜欢")
    val contentAlpha = if (isAlreadyAdded) 0.52f else 1f
    val colors = AppTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isLikedHero && !isAlreadyAdded) colors.accent else colors.surfaceVariant)
                .graphicsLayer { alpha = contentAlpha },
            contentAlignment = Alignment.Center
        ) {
            if (!playlist.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = playlist.coverUrl.toCoverThumbnailUrl(120),
                    contentDescription = playlist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (isLikedHero) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_heart_filled),
                    contentDescription = "我喜欢的音乐",
                    tint = if (isAlreadyAdded) colors.textSecondary else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = MiuixIcons.Music,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { alpha = contentAlpha }
        ) {
            Text(
                text = playlist.name,
                style = MiuixTheme.textStyles.body2.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isAlreadyAdded) colors.textSecondary else colors.textPrimary,
                    fontSize = 15.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${playlist.trackCount} 首",
                style = MiuixTheme.textStyles.footnote1.copy(
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            )
        }

        if (isAlreadyAdded) {
            Text(
                text = "已添加",
                style = MiuixTheme.textStyles.footnote1.copy(
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier
                    .background(colors.surfaceVariant, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun NewPlaylistPickerItem(
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = MiuixIcons.Add,
                contentDescription = "新建歌单",
                tint = colors.iconSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "新建歌单",
                style = MiuixTheme.textStyles.body2.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "创建新歌单并加入此歌曲",
                style = MiuixTheme.textStyles.footnote1.copy(
                    color = colors.textSecondary,
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 批量收藏到歌单底栏弹窗：
 * 支持多选歌曲后一次性批量收藏到指定歌单或我喜欢的音乐
 */
@Composable
fun AddToPlaylistBatchSheet(
    tracks: List<Track>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    currentPlaylistId: Long? = null,
    viewModel: AddToPlaylistViewModel = hiltViewModel(),
    onTracksAdded: ((playlistId: Long, tracks: List<Track>) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val colors = AppTheme.colors

    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistTitle by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    AdaptiveModalSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 1. 顶部栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "收藏到歌单（已选 ${tracks.size} 首）",
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
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // 2. 歌单列表
            val combinedPlaylists = remember(uiState.likedPlaylist, uiState.createdPlaylists) {
                val list = mutableListOf<UserPlaylist>()
                val liked = uiState.likedPlaylist ?: UserPlaylist(
                    id = -1L,
                    name = "我喜欢的音乐",
                    coverUrl = null,
                    trackCount = 0,
                    playCount = 0L,
                    isLikedHero = true
                )
                list.add(liked.copy(isLikedHero = true))
                list.addAll(uiState.createdPlaylists.filter { !it.isLikedHero && it.id != liked.id })
                list
            }

            val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                contentPadding = PaddingValues(
                    top = 4.dp,
                    bottom = navBottom + 16.dp,
                    start = 16.dp,
                    end = 16.dp
                )
            ) {
                item(key = "create_new_playlist") {
                    NewPlaylistPickerItem(
                        onClick = {
                            newPlaylistTitle = ""
                            showCreateDialog = true
                        }
                    )
                    HorizontalDivider(
                        color = colors.dividerSubtle,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 68.dp, end = 4.dp)
                    )
                }

                itemsIndexed(
                    items = combinedPlaylists,
                    key = { _, pl -> pl.id }
                ) { index, playlist ->
                    val isLikedHero = index == 0 || playlist.isLikedHero || playlist.name.contains("喜欢")
                    PlaylistPickerItem(
                        playlist = playlist,
                        isAlreadyAdded = false,
                        onClick = {
                            if (uiState.isOperating) return@PlaylistPickerItem

                            if (isLikedHero) {
                                scope.launch {
                                    val result = viewModel.likeTracks(tracks)
                                    result.fold(
                                        onSuccess = {
                                            Toast.makeText(context, "已收藏 ${tracks.size} 首歌曲到「${playlist.name}」", Toast.LENGTH_SHORT).show()
                                            onTracksAdded?.invoke(playlist.id, tracks)
                                            onDismiss()
                                        },
                                        onFailure = { err ->
                                            Toast.makeText(context, err.message ?: "收藏失败，请重试", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            } else {
                                scope.launch {
                                    val result = viewModel.addTracksToPlaylist(playlist.id, tracks.map { it.id })
                                    result.fold(
                                        onSuccess = {
                                            Toast.makeText(context, "已收藏 ${tracks.size} 首歌曲到歌单「${playlist.name}」", Toast.LENGTH_SHORT).show()
                                            onTracksAdded?.invoke(playlist.id, tracks)
                                            onDismiss()
                                        },
                                        onFailure = { err ->
                                            Toast.makeText(context, err.message ?: "收藏失败，请重试", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    )

                    if (isLikedHero) {
                        HorizontalDivider(
                            color = colors.divider,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                        )
                    } else if (index < combinedPlaylists.lastIndex) {
                        HorizontalDivider(
                            color = colors.dividerSubtle,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 68.dp, end = 4.dp)
                        )
                    }
                }
            }
        }

        if (showCreateDialog) {
            val dismissBatchCreateDialog = {
                if (!isCreating) {
                    showCreateDialog = false
                    newPlaylistTitle = ""
                }
            }
            AdaptiveAlertDialog(
                onDismissRequest = dismissBatchCreateDialog,
                title = "新建歌单",
                confirmText = "创建并收藏",
                confirmColor = colors.accent,
                confirmEnabled = newPlaylistTitle.isNotBlank() && !isCreating,
                dismissEnabled = !isCreating,
                isConfirmLoading = isCreating,
                onConfirm = {
                    val title = newPlaylistTitle.trim()
                    if (title.isBlank() || isCreating) return@AdaptiveAlertDialog
                    isCreating = true
                    scope.launch {
                        try {
                            val result = viewModel.createPlaylistAndAddTracks(title, tracks.map { it.id })
                            result.fold(
                                onSuccess = { created ->
                                    Toast.makeText(context, "已创建歌单「${created.name}」并收藏 ${tracks.size} 首歌曲", Toast.LENGTH_SHORT).show()
                                    onTracksAdded?.invoke(created.id, tracks)
                                    showCreateDialog = false
                                    newPlaylistTitle = ""
                                    onDismiss()
                                },
                                onFailure = { err ->
                                    Toast.makeText(context, err.message ?: "新建歌单失败", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } finally {
                            isCreating = false
                        }
                    }
                },
                dismissText = "取消",
                onDismiss = dismissBatchCreateDialog
            ) {
                Column {
                    Text(
                        text = "输入歌单标题后将自动创建并收录所选歌曲",
                        style = MiuixTheme.textStyles.footnote1.copy(color = colors.textSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    @Suppress("DEPRECATION")
                    TextField(
                        value = newPlaylistTitle,
                        onValueChange = { newPlaylistTitle = it },
                        label = "请输入歌单标题",
                        useLabelAsPlaceholder = true,
                        singleLine = true,
                        textStyle = MiuixTheme.textStyles.body1.copy(color = colors.textPrimary),
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = colors.surfaceVariant,
                            labelColor = colors.textSecondary,
                            borderColor = colors.accent
                        ),
                        cursorBrush = SolidColor(colors.accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
