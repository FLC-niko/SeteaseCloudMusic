package com.example.seteasecloudmusic.feature.mine.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.mine.domain.model.UserPlaylist

private val MineTextPrimary = Color(0xFF111111)
private val MineTextSecondary = Color(0xFF8E8E93)
private val MineCardBg = Color(0xFFF7F7F9)
private val MineCardBorder = Color(0xFFEAEAEE)
private val MineAccentRed = Color(0xFFFA233B)

@Composable
fun MineScreen(
    uiState: MineUiState,
    topContentPadding: Dp,
    bottomContentPadding: Dp,
    onLoginClick: () -> Unit,
    onRefresh: () -> Unit,
    onTabSelected: (MinePlaylistTab) -> Unit,
    onPlaylistClick: (UserPlaylist) -> Unit,
    onCloseDetail: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    onScanLocal: (String?) -> Unit,
    onSetLocalDirectory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    var showDirectoryDialog by remember { mutableStateOf(false) }

    // 动态权限适配
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permissionToRequest) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            onScanLocal(uiState.localDirectoryPath)
        }
    }

    LaunchedEffect(uiState.selectedTab) {
        if (uiState.selectedTab == MinePlaylistTab.LOCAL) {
            val granted = ContextCompat.checkSelfPermission(context, permissionToRequest) == PackageManager.PERMISSION_GRANTED
            hasPermission = granted
            if (!granted) {
                permissionLauncher.launch(permissionToRequest)
            } else if (uiState.localSongs.isEmpty()) {
                onScanLocal(uiState.localDirectoryPath)
            }
        }
    }

    // 线性平滑滚动计算
    val collapseFraction by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex == 0) {
                (lazyListState.firstVisibleItemScrollOffset.toFloat() / 200f).coerceIn(0f, 1f)
            } else {
                1f
            }
        }
    }

    // 顶部小标题平滑淡入
    val topTitleAlpha by remember {
        derivedStateOf {
            ((collapseFraction - 0.45f) / 0.55f).coerceIn(0f, 1f)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.White)) {
        val session = uiState.authSession
        val isLoggedIn = session?.isLoggedIn == true && session.userId != null

        if (!isLoggedIn) {
            // 未登录引导界面
            MineLoggedOutView(
                topPadding = topContentPadding,
                bottomPadding = bottomContentPadding,
                onLoginClick = onLoginClick
            )
        } else {
            // 列表内容区域
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = statusBarHeight + 52.dp,
                    bottom = bottomContentPadding,
                    start = 20.dp,
                    end = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 0. 大标题「我的」
                item(key = "large_page_title") {
                    Text(
                        text = "我的",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = MineTextPrimary,
                        letterSpacing = (-1).sp,
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = 4.dp)
                            .graphicsLayer {
                                alpha = (1f - collapseFraction).coerceIn(0f, 1f)
                                scaleX = lerp(1f, 0.78f, collapseFraction)
                                scaleY = lerp(1f, 0.78f, collapseFraction)
                                translationY = -collapseFraction * 20.dp.toPx()
                            }
                    )
                }

                // 1. 用户个人信息展台 (中心化大卡片)
                item(key = "user_header") {
                    UserProfileCenterpiece(
                        nickname = session.nickname ?: "云音乐用户",
                        userId = session.userId ?: 0L,
                        avatarUrl = session.avatarUrl,
                        createdCount = uiState.createdPlaylists.size,
                        favoritedCount = uiState.favoritedPlaylists.size,
                        likedCount = uiState.likedPlaylist?.trackCount ?: 0,
                        localCount = uiState.localSongs.size,
                        onRefresh = onRefresh,
                        onAccountClick = onLoginClick,
                        isLoading = uiState.isLoading
                    )
                }

                // 2. 「我喜欢的音乐」Hero 精选大卡片
                item(key = "liked_hero") {
                    LikedSongsHeroCard(
                        playlist = uiState.likedPlaylist,
                        onClick = {
                            uiState.likedPlaylist?.let { onPlaylistClick(it) }
                        }
                    )
                }

                // 3. 歌单模块 Tabs（创建歌单 / 收藏歌单 / 本地音乐）
                item(key = "playlist_tabs") {
                    MinePlaylistTabs(
                        selectedTab = uiState.selectedTab,
                        createdCount = uiState.createdPlaylists.size,
                        favoritedCount = uiState.favoritedPlaylists.size,
                        localCount = uiState.localSongs.size,
                        onTabSelected = onTabSelected
                    )
                }

                // 4. Tab 内容展示
                when (uiState.selectedTab) {
                    MinePlaylistTab.CREATED -> {
                        if (uiState.createdPlaylists.isEmpty() && !uiState.isLoading) {
                            item(key = "empty_created") {
                                EmptyPlaylistNotice(message = "暂无自建歌单")
                            }
                        } else {
                            items(
                                items = uiState.createdPlaylists,
                                key = { it.id }
                            ) { playlist ->
                                PlaylistRowItem(
                                    playlist = playlist,
                                    onClick = { onPlaylistClick(playlist) }
                                )
                            }
                        }
                    }

                    MinePlaylistTab.FAVORITED -> {
                        if (uiState.favoritedPlaylists.isEmpty() && !uiState.isLoading) {
                            item(key = "empty_favorited") {
                                EmptyPlaylistNotice(message = "暂无收藏歌单")
                            }
                        } else {
                            items(
                                items = uiState.favoritedPlaylists,
                                key = { it.id }
                            ) { playlist ->
                                PlaylistRowItem(
                                    playlist = playlist,
                                    onClick = { onPlaylistClick(playlist) }
                                )
                            }
                        }
                    }

                    MinePlaylistTab.LOCAL -> {
                        // 统一歌单入口卡片：作为本地歌单入口，点击后与在线歌单完全一致进入全屏沉浸歌单详情
                        item(key = "local_playlist_entry") {
                            PlaylistRowItem(
                                playlist = uiState.localPlaylist,
                                onClick = { onPlaylistClick(uiState.localPlaylist) }
                            )
                        }

                        // 目录设置与刷新工具条
                        item(key = "local_dir_control_bar") {
                            LocalMusicDirectoryBar(
                                directoryPath = uiState.localDirectoryPath,
                                hasPermission = hasPermission,
                                isScanning = uiState.isScanningLocal,
                                onChangeDirectory = { showDirectoryDialog = true },
                                onRequestPermission = { permissionLauncher.launch(permissionToRequest) },
                                onRescan = {
                                    if (!hasPermission) {
                                        permissionLauncher.launch(permissionToRequest)
                                    } else {
                                        onScanLocal(uiState.localDirectoryPath)
                                    }
                                }
                            )
                        }
                    }
                }

                if (uiState.isLoading) {
                    item(key = "loading_indicator") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = MineAccentRed,
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                }
            }

            // 顶部浓郁渐变悬浮导航条
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(statusBarHeight)
                        .background(Color.White)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(66.dp)
                        .background(
                            Brush.verticalGradient(
                                0.00f to Color.White.copy(alpha = 0.98f * collapseFraction),
                                0.38f to Color.White.copy(alpha = 0.94f * collapseFraction),
                                0.65f to Color.White.copy(alpha = 0.76f * collapseFraction),
                                0.85f to Color.White.copy(alpha = 0.35f * collapseFraction),
                                1.00f to Color.White.copy(alpha = 0.0f)
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Text(
                        text = "我的",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MineTextPrimary,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .graphicsLayer {
                                alpha = topTitleAlpha
                            }
                    )
                }
            }
        }

        // 修改扫描目录弹窗
        if (showDirectoryDialog) {
            ChangeLocalDirectoryDialog(
                currentPath = uiState.localDirectoryPath ?: "",
                onDismiss = { showDirectoryDialog = false },
                onConfirm = { newPath ->
                    showDirectoryDialog = false
                    if (!hasPermission) {
                        permissionLauncher.launch(permissionToRequest)
                    }
                    onSetLocalDirectory(newPath)
                }
            )
        }

        // 全屏歌单详情曲目页面（在线歌单与本地音乐歌单完全统一）
        AnimatedVisibility(
            visible = uiState.activePlaylistDetail != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(20f)
        ) {
            uiState.activePlaylistDetail?.let { detail ->
                PlaylistDetailScreen(
                    detail = detail,
                    isLoading = uiState.isLoadingDetail,
                    onClose = onCloseDetail,
                    onPlayTrack = { track -> onPlayTrack(track, detail.tracks) },
                    onPlayAll = { onPlayAll(detail.tracks) }
                )
            }
        }
    }
}

/**
 * 本地音乐目录与扫描管理小组件
 */
@Composable
private fun LocalMusicDirectoryBar(
    directoryPath: String?,
    hasPermission: Boolean,
    isScanning: Boolean,
    onChangeDirectory: () -> Unit,
    onRequestPermission: () -> Unit,
    onRescan: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MineCardBg)
            .border(1.dp, MineCardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = MineAccentRed,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = directoryPath?.ifBlank { "全部系统音频 / 默认目录" } ?: "全部系统音频 / 默认目录",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = MineTextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!hasPermission) {
                TextButton(
                    onClick = onRequestPermission,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("授权权限", fontSize = 12.sp, color = MineAccentRed, fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(
                    onClick = onChangeDirectory,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("指定目录", fontSize = 12.sp, color = MineTextPrimary)
                }

                IconButton(
                    onClick = onRescan,
                    enabled = !isScanning,
                    modifier = Modifier.size(28.dp)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = MineAccentRed,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Rescan",
                            tint = MineTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 用户个人信息展台 (中心化优雅卡片)
 */
@Composable
private fun UserProfileCenterpiece(
    nickname: String,
    userId: Long,
    avatarUrl: String?,
    createdCount: Int,
    favoritedCount: Int,
    likedCount: Int,
    localCount: Int,
    onRefresh: () -> Unit,
    onAccountClick: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MineCardBg)
            .border(1.dp, MineCardBorder, RoundedCornerShape(24.dp))
            .clickable(onClick = onAccountClick)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "User Avatar",
                modifier = Modifier
                    .size(76.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .shadow(elevation = 6.dp, shape = CircleShape),
                contentScale = ContentScale.Crop
            )

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.TopEnd)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MineAccentRed,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = MineTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = nickname,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = MineTextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFEAEAEE))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                text = "UID: $userId",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = MineTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ProfileStatItem(title = "喜欢的音乐", value = "$likedCount")
            ProfileStatItem(title = "创建歌单", value = "$createdCount")
            ProfileStatItem(title = "收藏歌单", value = "$favoritedCount")
            ProfileStatItem(title = "本地音乐", value = "$localCount")
        }
    }
}

@Composable
private fun ProfileStatItem(
    title: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MineTextPrimary
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = MineTextSecondary
            )
        )
    }
}

/**
 * 未登录引导界面
 */
@Composable
private fun MineLoggedOutView(
    topPadding: Dp,
    bottomPadding: Dp,
    onLoginClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding, bottom = bottomPadding, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFFA233B), Color(0xFFFF5252))
                    )
                )
                .shadow(elevation = 10.dp, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Headphones,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "登录网易云音乐",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MineTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "即刻同步您的红心歌单、自建歌单与收藏资产",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MineTextSecondary
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MineAccentRed,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = "立即登录 / 扫码登录",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
        }
    }
}

/**
 * 「我喜欢的音乐」Hero 精选卡片
 */
@Composable
private fun LikedSongsHeroCard(
    playlist: UserPlaylist?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFA233B),
                            Color(0xFFFF5252),
                            Color(0xFFFF7A7A)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!playlist?.coverUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = playlist?.coverUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist?.name ?: "我喜欢的音乐",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${playlist?.trackCount ?: 0} 首歌曲",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = MineAccentRed,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

/**
 * 歌单分类 Tabs（创建 / 收藏 / 本地 - 液态玻璃微质感）
 */
@Composable
private fun MinePlaylistTabs(
    selectedTab: MinePlaylistTab,
    createdCount: Int,
    favoritedCount: Int,
    localCount: Int,
    onTabSelected: (MinePlaylistTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFEEEEF2).copy(alpha = 0.85f))
            .border(1.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MineTabButton(
                title = "创建歌单 (${createdCount})",
                isSelected = selectedTab == MinePlaylistTab.CREATED,
                onClick = { onTabSelected(MinePlaylistTab.CREATED) },
                modifier = Modifier.weight(1f)
            )
            MineTabButton(
                title = "收藏歌单 (${favoritedCount})",
                isSelected = selectedTab == MinePlaylistTab.FAVORITED,
                onClick = { onTabSelected(MinePlaylistTab.FAVORITED) },
                modifier = Modifier.weight(1f)
            )
            MineTabButton(
                title = "本地音乐 (${localCount})",
                isSelected = selectedTab == MinePlaylistTab.LOCAL,
                onClick = { onTabSelected(MinePlaylistTab.LOCAL) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MineTabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabShape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (isSelected) {
                    Modifier
                        .shadow(elevation = 3.dp, shape = tabShape)
                        .clip(tabShape)
                        .background(Color.White.copy(alpha = 0.95f))
                        .border(0.5.dp, Color.White, tabShape)
                } else {
                    Modifier.clip(tabShape)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MineTextPrimary else MineTextSecondary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 切换扫描目录弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangeLocalDirectoryDialog(
    currentPath: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var inputPath by remember { mutableStateOf(currentPath) }

    val quickPaths = listOf(
        "/storage/emulated/0/Music",
        "/storage/emulated/0/Download",
        "/storage/emulated/0/netease/cloudmusic/Music",
        "/sdcard/Music"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "设置音乐扫描目录", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "请输入存放音频文件的完整目录路径（支持递归扫描子目录）：",
                    style = MaterialTheme.typography.bodySmall.copy(color = MineTextSecondary)
                )

                OutlinedTextField(
                    value = inputPath,
                    onValueChange = { inputPath = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("/storage/emulated/0/Music") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MineAccentRed,
                        cursorColor = MineAccentRed
                    )
                )

                Text(
                    text = "常用快捷目录：",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPaths.take(3).forEach { path ->
                        val shortName = path.substringAfterLast("/")
                        SuggestionChip(
                            onClick = { inputPath = path },
                            label = { Text(shortName, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(inputPath.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = MineAccentRed)
            ) {
                Text("确定并扫描")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MineTextSecondary)
            }
        }
    )
}

/**
 * 歌单单项行
 */
@Composable
private fun PlaylistRowItem(
    playlist: UserPlaylist,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, MineCardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (!playlist.coverUrl.isNullOrBlank()) Color.Transparent
                    else Color(0xFFF3F3F7)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!playlist.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = playlist.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = MineAccentRed,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MineTextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${playlist.trackCount} 首" + if (!playlist.creatorName.isNullOrBlank()) " · by ${playlist.creatorName}" else "",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = MineTextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFC7C7CC),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EmptyPlaylistNotice(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MineTextSecondary
            )
        )
    }
}
