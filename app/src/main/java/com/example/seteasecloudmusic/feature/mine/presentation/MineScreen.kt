package com.example.seteasecloudmusic.feature.mine.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.core.common.toCoverThumbnailUrl
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.ui.components.AppleMusicCollapsedTopBar
import com.example.seteasecloudmusic.core.ui.components.AppleMusicLargeTitle
import com.example.seteasecloudmusic.core.ui.components.UserAvatar
import com.example.seteasecloudmusic.core.ui.components.rememberAppleMusicCollapseFraction
import com.example.seteasecloudmusic.feature.mine.domain.model.UserPlaylist
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val MineTextPrimary = Color(0xFF111111)
private val MineTextSecondary = Color(0xFF767680)
private val MineAccentRed = Color(0xFFFA233B)
private val MinePageBase = Color(0xFFF7F7FA)

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
        if (isGranted) onScanLocal(uiState.localDirectoryPath)
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

    val collapseFraction by rememberAppleMusicCollapseFraction(
        lazyListState = lazyListState,
        collapseThresholdDp = 76.dp
    )

    // 不要在 AppNavigation 的全局 layerBackdrop 中再次采样同一个 Backdrop。
    // “我的”页面只采集自己的静态氛围背景，玻璃卡片从这张局部纹理取景，
    // 这样既保留液态玻璃效果，也避免渲染反馈导致点击页面后主线程/渲染线程卡死。
    val mineBackdrop = rememberLayerBackdrop {
        drawRect(MinePageBase)
        drawContent()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(mineBackdrop)
        ) {
            MineAmbientBackground()
        }

        val session = uiState.authSession
        val isLoggedIn = session?.isLoggedIn == true && session.userId != null

        if (!isLoggedIn) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = statusBarHeight + 52.dp, bottom = bottomContentPadding)
            ) {
                AppleMusicLargeTitle(
                    title = "我的",
                    collapseFraction = 0f
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MineLoggedOutView(
                        backdrop = mineBackdrop,
                        onLoginClick = onLoginClick
                    )
                }
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = statusBarHeight + 52.dp,
                    bottom = bottomContentPadding,
                    start = 20.dp,
                    end = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "large_page_title") {
                    AppleMusicLargeTitle(
                        title = "我的",
                        collapseFraction = collapseFraction
                    )
                }

                item(key = "user_header") {
                    UserProfileCenterpiece(
                        backdrop = mineBackdrop,
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

                item(key = "liked_hero") {
                    LikedSongsHeroCard(
                        backdrop = mineBackdrop,
                        playlist = uiState.likedPlaylist,
                        onClick = { uiState.likedPlaylist?.let(onPlaylistClick) }
                    )
                }

                // 原生吸顶：零延迟同一帧渲染，彻底解决上下滑动时的抖动问题
                stickyHeader(key = "playlist_tabs") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MinePageBase)
                            .padding(vertical = 2.dp)
                    ) {
                        MinePlaylistTabs(
                            backdrop = mineBackdrop,
                            selectedTab = uiState.selectedTab,
                            createdCount = uiState.createdPlaylists.size,
                            favoritedCount = uiState.favoritedPlaylists.size,
                            localCount = uiState.localSongs.size,
                            onTabSelected = onTabSelected
                        )
                    }
                }

                when (uiState.selectedTab) {
                    MinePlaylistTab.CREATED -> {
                        if (uiState.createdPlaylists.isEmpty() && !uiState.isLoading) {
                            if (!uiState.errorMessage.isNullOrBlank()) {
                                item(key = "error_created") {
                                    MineErrorNotice(
                                        message = uiState.errorMessage,
                                        onRetry = onRefresh
                                    )
                                }
                            } else {
                                item(key = "empty_created") { EmptyPlaylistNotice("暂无自建歌单") }
                            }
                        } else {
                            items(uiState.createdPlaylists, key = { it.id }) { playlist ->
                                PlaylistRowItem(playlist = playlist, onClick = { onPlaylistClick(playlist) })
                            }
                        }
                    }

                    MinePlaylistTab.FAVORITED -> {
                        if (uiState.favoritedPlaylists.isEmpty() && !uiState.isLoading) {
                            if (!uiState.errorMessage.isNullOrBlank()) {
                                item(key = "error_favorited") {
                                    MineErrorNotice(
                                        message = uiState.errorMessage,
                                        onRetry = onRefresh
                                    )
                                }
                            } else {
                                item(key = "empty_favorited") { EmptyPlaylistNotice("暂无收藏歌单") }
                            }
                        } else {
                            items(uiState.favoritedPlaylists, key = { it.id }) { playlist ->
                                PlaylistRowItem(playlist = playlist, onClick = { onPlaylistClick(playlist) })
                            }
                        }
                    }

                    MinePlaylistTab.LOCAL -> {
                        item(key = "local_playlist_entry") {
                            PlaylistRowItem(
                                playlist = uiState.localPlaylist,
                                onClick = { onPlaylistClick(uiState.localPlaylist) }
                            )
                        }
                        item(key = "local_dir_control_bar") {
                            LocalMusicDirectoryBar(
                                backdrop = mineBackdrop,
                                directoryPath = uiState.localDirectoryPath,
                                hasPermission = hasPermission,
                                isScanning = uiState.isScanningLocal,
                                onChangeDirectory = { showDirectoryDialog = true },
                                onRequestPermission = { permissionLauncher.launch(permissionToRequest) },
                                onRescan = {
                                    if (!hasPermission) permissionLauncher.launch(permissionToRequest)
                                    else onScanLocal(uiState.localDirectoryPath)
                                }
                            )
                        }
                    }
                }

                if (uiState.isLoading) {
                    item(key = "loading_indicator") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                color = MineAccentRed,
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                }
            }

            AppleMusicCollapsedTopBar(
                title = "我的",
                collapseFraction = collapseFraction,
                statusBarHeight = statusBarHeight,
                backdrop = mineBackdrop,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(4f)
            )
        }

        if (showDirectoryDialog) {
            ChangeLocalDirectoryDialog(
                currentPath = uiState.localDirectoryPath ?: "",
                onDismiss = { showDirectoryDialog = false },
                onConfirm = { newPath ->
                    showDirectoryDialog = false
                    if (!hasPermission) permissionLauncher.launch(permissionToRequest)
                    onSetLocalDirectory(newPath)
                }
            )
        }

        AnimatedVisibility(
            visible = uiState.activePlaylistDetail != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize().zIndex(20f)
        ) {
            uiState.activePlaylistDetail?.let { detail ->
                PlaylistDetailScreen(
                    detail = detail,
                    isLoading = uiState.isLoadingDetail,
                    backdrop = mineBackdrop,
                    onClose = onCloseDetail,
                    onPlayTrack = { track -> onPlayTrack(track, detail.tracks) },
                    onPlayAll = { onPlayAll(detail.tracks) }
                )
            }
        }
    }
}

@Composable
private fun MineAmbientBackground() {
    Box(modifier = Modifier.fillMaxSize().background(MinePageBase))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFE8ED).copy(alpha = 0.82f),
                        Color(0xFFF2EEFF).copy(alpha = 0.32f),
                        Color.Transparent
                    )
                )
            )
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFE9F5FF).copy(alpha = 0.30f),
                        Color.Transparent,
                        Color(0xFFFFF1E8).copy(alpha = 0.24f)
                    )
                )
            )
    )
}

@Composable
private fun MineGlassSurface(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    surfaceAlpha: Float = 0.48f,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderModifier = if (borderWidth > 0.dp) {
        Modifier.border(borderWidth, Color.White.copy(alpha = 0.62f), shape)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(cornerRadius) },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(16f.dp.toPx(), 32f.dp.toPx())
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = surfaceAlpha)) }
            )
            .then(borderModifier),
        content = content
    )
}

@Composable
private fun LocalMusicDirectoryBar(
    backdrop: Backdrop,
    directoryPath: String?,
    hasPermission: Boolean,
    isScanning: Boolean,
    onChangeDirectory: () -> Unit,
    onRequestPermission: () -> Unit,
    onRescan: () -> Unit
) {
    MineGlassSurface(
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        surfaceAlpha = 0.40f
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
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
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = directoryPath?.ifBlank { "全部系统音频 / 默认目录" } ?: "全部系统音频 / 默认目录",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = MineTextSecondary),
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
                                contentDescription = "重新扫描",
                                tint = MineTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserProfileCenterpiece(
    backdrop: Backdrop,
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
    MineGlassSurface(
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onAccountClick),
        cornerRadius = 28.dp,
        surfaceAlpha = 0.47f
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    avatarUrl = avatarUrl,
                    displayName = nickname,
                    size = 64.dp,
                    showBorder = true,
                    borderWidth = 2.dp,
                    borderColor = Color.White.copy(alpha = 0.82f)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nickname,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MineTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "UID $userId",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = MineTextSecondary)
                    )
                }
                GlassIconButton(
                    backdrop = backdrop,
                    icon = Icons.Filled.Refresh,
                    contentDescription = "刷新歌单",
                    tint = if (isLoading) MineAccentRed else MineTextPrimary,
                    onClick = onRefresh,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(17.dp),
                            color = MineAccentRed,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.24f))
                    .padding(vertical = 12.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileStatItem("喜欢", "$likedCount")
                ProfileStatDivider()
                ProfileStatItem("创建", "$createdCount")
                ProfileStatDivider()
                ProfileStatItem("收藏", "$favoritedCount")
                ProfileStatDivider()
                ProfileStatItem("本地", "$localCount")
            }
        }
    }
}

@Composable
private fun GlassIconButton(
    backdrop: Backdrop,
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(19.dp) },
                effects = {
                    vibrancy()
                    blur(1.5f.dp.toPx())
                    lens(8f.dp.toPx(), 16f.dp.toPx())
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.32f)) }
            )
            .border(1.dp, Color.White.copy(alpha = 0.56f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        overlay()
    }
}

@Composable
private fun ProfileStatDivider() {
    Box(
        modifier = Modifier
            .height(26.dp)
            .width(1.dp)
            .background(Color.White.copy(alpha = 0.42f))
    )
}

@Composable
private fun ProfileStatItem(title: String, value: String) {
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
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MineTextSecondary)
        )
    }
}

@Composable
private fun MineLoggedOutView(
    backdrop: Backdrop,
    onLoginClick: () -> Unit,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MineGlassSurface(
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 30.dp,
            surfaceAlpha = 0.50f
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.32f))
                        .border(1.dp, Color.White.copy(alpha = 0.72f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Headphones,
                        contentDescription = null,
                        tint = MineAccentRed,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "登录网易云音乐",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = MineTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "同步红心歌单、自建歌单与收藏资产",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MineTextSecondary)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MineAccentRed,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text("立即登录 / 扫码登录", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun LikedSongsHeroCard(
    backdrop: Backdrop,
    playlist: UserPlaylist?,
    onClick: () -> Unit
) {
    MineGlassSurface(
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        cornerRadius = 26.dp,
        surfaceAlpha = 0.40f
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.32f))
                    .border(1.dp, Color.White.copy(alpha = 0.58f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!playlist?.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = playlist?.coverUrl.toCoverThumbnailUrl(180),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = MineAccentRed,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist?.name ?: "我喜欢的音乐",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MineTextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "${playlist?.trackCount ?: 0} 首歌曲",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, color = MineTextSecondary)
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MineAccentRed.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "播放我喜欢的音乐",
                    tint = Color.White,
                    modifier = Modifier.size(27.dp)
                )
            }
        }
    }
}

@Composable
private fun MinePlaylistTabs(
    backdrop: Backdrop,
    selectedTab: MinePlaylistTab,
    createdCount: Int,
    favoritedCount: Int,
    localCount: Int,
    onTabSelected: (MinePlaylistTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        MinePlaylistTab.CREATED to "创建 $createdCount",
        MinePlaylistTab.FAVORITED to "收藏 $favoritedCount",
        MinePlaylistTab.LOCAL to "本地 $localCount"
    )

    val currentOnTabSelected by rememberUpdatedState(onTabSelected)
    val currentSelectedTab by rememberUpdatedState(selectedTab)
    val selectedIndex = tabs.indexOfFirst { it.first == currentSelectedTab }.coerceAtLeast(0)

    val navBarHeight = 50.dp
    val cornerRadius = navBarHeight / 2
    val thumbPadding = 4.dp
    val innerCornerRadius = cornerRadius - thumbPadding

    val animationScope = rememberCoroutineScope()
    val pressAnimation = remember { Animatable(0f) }
    val animationSpec = remember { spring<Float>(0.8f, 500f, 0.001f) }

    var dragOffsetX by remember { mutableStateOf<Float?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(navBarHeight)
            .graphicsLayer {
                val progress = pressAnimation.value
                val maxScale = (size.width + 8f.dp.toPx()) / size.width
                val scale = lerp(1f, maxScale, progress)
                scaleX = scale
                scaleY = scale
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(cornerRadius) },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(16f.dp.toPx(), 32f.dp.toPx())
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) }
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var currentX = down.position.x
                    dragOffsetX = currentX

                    val updateSelection = { x: Float ->
                        val slotWidthPx = size.width.toFloat() / tabs.size.toFloat()
                        if (slotWidthPx > 0f) {
                            val newIndex = (x / slotWidthPx).toInt().coerceIn(0, tabs.size - 1)
                            currentOnTabSelected(tabs[newIndex].first)
                        }
                    }
                    updateSelection(currentX)

                    animationScope.launch { pressAnimation.animateTo(1f, animationSpec) }

                    var inGesture = true
                    try {
                        while (inGesture) {
                            val event = awaitPointerEvent()
                            val dragEvent = event.changes.firstOrNull()
                            if (dragEvent != null && dragEvent.pressed) {
                                currentX = dragEvent.position.x
                                dragOffsetX = currentX
                                updateSelection(currentX)
                                dragEvent.consume()
                            } else {
                                inGesture = false
                            }
                        }
                    } finally {
                        animationScope.launch { pressAnimation.animateTo(0f, animationSpec) }
                        dragOffsetX = null
                    }
                }
            }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = thumbPadding),
            contentAlignment = Alignment.CenterStart
        ) {
            val slotWidth = maxWidth / tabs.size.toFloat()
            val targetThumbWidth = (slotWidth - thumbPadding * 2).coerceAtLeast(0.dp)
            val baseOffsetX = slotWidth * selectedIndex + thumbPadding

            val density = LocalDensity.current
            val targetThumbOffsetX = if (dragOffsetX != null) {
                val fingerXDp = with(density) { dragOffsetX!!.toDp() }
                val halfThumb = targetThumbWidth / 2
                (fingerXDp - halfThumb).coerceIn(thumbPadding, maxWidth - targetThumbWidth - thumbPadding)
            } else {
                baseOffsetX
            }

            val tracking = dragOffsetX != null
            val animatedThumbWidth by animateDpAsState(
                targetValue = targetThumbWidth,
                animationSpec = if (tracking) spring(stiffness = 800f, dampingRatio = 0.8f) else spring(stiffness = 300f, dampingRatio = 0.6f),
                label = "mineThumbWidth"
            )
            val animatedThumbOffsetX by animateDpAsState(
                targetValue = targetThumbOffsetX,
                animationSpec = if (tracking) spring(stiffness = 800f, dampingRatio = 0.8f) else spring(stiffness = 300f, dampingRatio = 0.6f),
                label = "mineThumbOffset"
            )

            // 弹性拉伸（Gooey Stretch 拉丝拉长效果）
            val offsetDiff = targetThumbOffsetX - animatedThumbOffsetX
            val stretchFactor = 0.35f
            val renderedOffsetX = if (offsetDiff.value < 0f) {
                animatedThumbOffsetX + offsetDiff * stretchFactor
            } else {
                animatedThumbOffsetX
            }
            val renderedWidth = animatedThumbWidth + offsetDiff.value.absoluteValue.dp * stretchFactor
            val thumbHeight = navBarHeight - thumbPadding * 2

            // 底栏同款折射参数液体玻璃滑块
            Box(
                Modifier
                    .offset(x = renderedOffsetX)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(innerCornerRadius) },
                        effects = {
                            lens(
                                refractionHeight = 6f.dp.toPx(),
                                refractionAmount = 12f.dp.toPx(),
                                chromaticAberration = true
                            )
                        }
                    )
                    .size(renderedWidth, thumbHeight)
            )

            // 文字层（各项具备独立点击保障与高亮指示）
            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEachIndexed { index, (tab, title) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                currentOnTabSelected(tab)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Medium,
                                color = if (index == selectedIndex) MineAccentRed else MineTextSecondary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

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
        title = { Text(text = "设置音乐扫描目录", fontWeight = FontWeight.Bold) },
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
                        SuggestionChip(
                            onClick = { inputPath = path },
                            label = { Text(path.substringAfterLast("/"), fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(inputPath.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = MineAccentRed)
            ) { Text("确定并扫描") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = MineTextSecondary) }
        }
    )
}

@Composable
private fun PlaylistRowItem(
    playlist: UserPlaylist,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Color.White.copy(alpha = 0.42f))
                .border(1.dp, Color.White.copy(alpha = 0.62f), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (!playlist.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = playlist.coverUrl.toCoverThumbnailUrl(140),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = MineAccentRed,
                    modifier = Modifier.size(27.dp)
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
                text = "${playlist.trackCount} 首" + if (!playlist.creatorName.isNullOrBlank()) " · ${playlist.creatorName}" else "",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = MineTextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFB7B7C0),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EmptyPlaylistNotice(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(color = MineTextSecondary)
        )
    }
}

@Composable
private fun MineErrorNotice(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = Color(0xFF9E9EA7),
            modifier = Modifier.size(44.dp)
        )
        Text(
            text = "加载歌单受阻",
            style = MaterialTheme.typography.titleSmall.copy(color = MineTextPrimary, fontWeight = FontWeight.SemiBold)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(color = MineTextSecondary),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Text(
            text = "提示：请检查网络设置后重试",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFAAAAAF), fontSize = 11.sp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Button(onClick = onRetry) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("重新加载")
        }
    }
}
