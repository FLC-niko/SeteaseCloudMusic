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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
        containerColor = Color.White,
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
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color(0xFF222226),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = "收藏到歌单",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFF1E1E24),
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
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(
            color = Color(0xFFF0F0F2),
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        // 2. “新建歌单”快捷入口
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showCreateDialog = true }
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF2F2F7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新建歌单",
                    tint = Color(0xFF515151),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "新建歌单",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF1E1E24),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "创建新歌单并加入此歌曲",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF8E8E93),
                        fontSize = 12.sp
                    )
                )
            }
        }

        HorizontalDivider(
            color = Color(0xFFF0F0F2),
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        // 3. 歌单列表区域（包括首位的“我喜欢的音乐”和随后的自建歌单）
        if (!uiState.isLoggedIn) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "登录网易云音乐账号后即可收藏到自建歌单",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF8E8E93),
                        fontSize = 14.sp
                    )
                )
            }
        } else if (uiState.isLoading && uiState.createdPlaylists.isEmpty() && uiState.likedPlaylist == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFFA233B),
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp
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
                    .heightIn(max = 380.dp)
            ) {
                items(
                    items = allPlaylists,
                    key = { it.id }
                ) { playlist ->
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
                }
            }
        }
    }

    // 4. 新建歌单对话框
    if (showCreateDialog) {
        AdaptiveAlertDialog(
            onDismissRequest = {
                if (!isCreating) {
                    showCreateDialog = false
                    newPlaylistTitle = ""
                }
            },
            title = {
                Text(
                    text = "新建歌单",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "输入歌单标题后将自动创建并收录本首单曲",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF8E8E93))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPlaylistTitle,
                        onValueChange = { newPlaylistTitle = it },
                        placeholder = { Text("请输入歌单标题") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val title = newPlaylistTitle.trim()
                        if (title.isBlank() || isCreating) return@TextButton
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
                    enabled = newPlaylistTitle.isNotBlank() && !isCreating
                ) {
                    Text("创建并收藏", color = if (newPlaylistTitle.isNotBlank() && !isCreating) Color(0xFFFA233B) else Color.Gray)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isCreating) {
                            showCreateDialog = false
                            newPlaylistTitle = ""
                        }
                    }
                ) {
                    Text("取消", color = Color(0xFF8E8E93))
                }
            }
        )
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
                .background(if (isLikedHero && !isAlreadyAdded) Color(0xFFFF3B50) else Color(0xFFF2F2F7))
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
                    tint = if (isAlreadyAdded) Color(0xFF8E8E93) else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFF8E8E93),
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
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isAlreadyAdded) Color(0xFF8E8E93) else Color(0xFF1E1E24),
                    fontSize = 15.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${playlist.trackCount} 首",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF8E8E93),
                    fontSize = 12.sp
                )
            )
        }

        if (isAlreadyAdded) {
            Text(
                text = "已添加",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF8E8E93),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier
                    .background(Color(0xFFF2F2F7), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}
