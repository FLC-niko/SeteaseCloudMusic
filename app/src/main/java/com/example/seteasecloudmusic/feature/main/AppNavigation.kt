package com.example.seteasecloudmusic.feature.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.seteasecloudmusic.feature.main.navigation.horizontalChromeFrame
import com.example.seteasecloudmusic.feature.main.navigation.horizontalMiniPlayerFrame
import com.example.seteasecloudmusic.feature.main.navigation.BottomNavigationViewport
import com.example.seteasecloudmusic.feature.main.navigation.LocalNavigationScrollState
import com.example.seteasecloudmusic.feature.main.navigation.NavigationScrollState
import com.example.seteasecloudmusic.feature.main.navigation.MiniPlayerChromeMotion
import com.example.seteasecloudmusic.feature.main.navigation.rememberNavigationScrollState
import com.example.seteasecloudmusic.feature.main.navigation.rememberMiniPlayerChromeMotion
import com.example.seteasecloudmusic.feature.main.navigation.toNavigationViewport
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seteasecloudmusic.core.ui.adaptive.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import android.widget.Toast
import com.example.seteasecloudmusic.core.ui.components.AppLoadingIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.ui.components.UserAvatarButton
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import com.example.seteasecloudmusic.core.player.PlaybackState
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.feature.auth.presentation.AccountLoginSheetContent
import com.example.seteasecloudmusic.feature.auth.presentation.AuthViewModel
import com.example.seteasecloudmusic.feature.artist.presentation.ArtistDetailRoute
import com.example.seteasecloudmusic.feature.home.presentation.HomeRoute
import com.example.seteasecloudmusic.feature.home.presentation.HomeViewModel
import com.example.seteasecloudmusic.feature.mine.presentation.LocalTrackNavigation
import com.example.seteasecloudmusic.feature.mine.presentation.MineRoute
import com.example.seteasecloudmusic.feature.mine.presentation.MineViewModel
import com.example.seteasecloudmusic.feature.mine.presentation.PlaylistDetailScreen
import com.example.seteasecloudmusic.feature.mine.presentation.PlaylistDetailStyle
import com.example.seteasecloudmusic.feature.mine.presentation.TrackActionNavigator
import com.example.seteasecloudmusic.feature.radio.presentation.RadioRoute
import com.example.seteasecloudmusic.feature.radio.presentation.RadioViewModel
import com.example.seteasecloudmusic.feature.search.presentation.SearchRoute
import com.example.seteasecloudmusic.feature.search.presentation.SearchViewModel
import com.example.seteasecloudmusic.feature.player.presentation.NowPlayingScreen
import com.example.seteasecloudmusic.feature.player.presentation.PlayerViewModel
import com.example.seteasecloudmusic.core.ui.components.Ios26NetworkOfflineDialog
import com.example.seteasecloudmusic.core.ui.components.LocalGlassBackdrop
import com.example.seteasecloudmusic.core.ui.components.LocalPageVisible
import com.example.seteasecloudmusic.core.ui.components.LiquidGlassPanel
import com.example.seteasecloudmusic.core.ui.components.liquidGlassPanel
import com.example.seteasecloudmusic.core.ui.components.GlassDefaults
import com.example.seteasecloudmusic.core.ui.components.GlassSliderDefaults
import com.example.seteasecloudmusic.core.ui.components.LiquidGlassActiveDroplet
import com.example.seteasecloudmusic.core.ui.components.LiquidGlassActiveTabsRecorder
import com.example.seteasecloudmusic.core.ui.components.LiquidGlassDualColorTabs
import com.example.seteasecloudmusic.core.ui.components.LiquidSegmentState
import com.example.seteasecloudmusic.core.ui.components.interactiveGlassHighlight
import com.example.seteasecloudmusic.core.ui.components.liquidGlass
import com.example.seteasecloudmusic.core.ui.components.liquidSegmentDrag
import com.example.seteasecloudmusic.core.ui.components.rememberLiquidSegmentState
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.seteasecloudmusic.R
import com.example.seteasecloudmusic.feature.player.presentation.LocalPlayerArtwork
import com.example.seteasecloudmusic.feature.player.presentation.PlaylistQueueSheet
import com.example.seteasecloudmusic.feature.player.presentation.isUsable
import com.example.seteasecloudmusic.feature.player.presentation.mix
import com.example.seteasecloudmusic.feature.player.presentation.playerArtworkRect
import com.example.seteasecloudmusic.feature.player.presentation.playerBackgroundAlpha
import com.example.seteasecloudmusic.feature.player.presentation.playerContainerCornerRadius
import com.example.seteasecloudmusic.feature.player.presentation.playerContainerRect
import com.example.seteasecloudmusic.feature.player.presentation.playerContentAlpha
import com.example.seteasecloudmusic.feature.player.presentation.playerMiniContentAlpha
import com.example.seteasecloudmusic.feature.player.presentation.playerTopAnchoredArtworkRect
import com.example.seteasecloudmusic.feature.player.presentation.normalizeHighResCoverUrl
import com.example.seteasecloudmusic.feature.player.presentation.rememberHighResArtworkRequest
import com.example.seteasecloudmusic.feature.player.presentation.rememberPlayerScreenCornerRadiusPx
import androidx.compose.foundation.Image
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.seteasecloudmusic.core.common.toCoverThumbnailUrl
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.roundToInt



/**
 * 应用主导航外壳模块：组织一级页面容器、悬浮底栏/侧栏、迷你播放条及全屏播放页与账号抽屉覆盖层。
 */

/**
 * 底部导航栏的数据模型
 *
 * @property title 导航项显示的文字标题
 * @property iconRes 导航项未选中时显示的图标资源（空心）
 * @property selectedIconRes 导航项选中高亮时显示的图标资源（填充实心）
 */
data class BottomNavItem(
    val title: String,
    val iconRes: Int,
    val selectedIconRes: Int
)

/**
 * 左侧主导航条承载的三个一级入口。
 * 保持在文件级常量以稳定对象引用，便于 Compose 跳过依赖它的子组件。
 */
private val MainNavItems = listOf(
    BottomNavItem("首页", R.drawable.ic_nav_home, R.drawable.ic_nav_home_filled),
    BottomNavItem("电台", R.drawable.ic_nav_radio, R.drawable.ic_nav_radio_filled),
    BottomNavItem("我的", R.drawable.ic_nav_mine, R.drawable.ic_nav_mine_filled)
)

/** 左侧主导航条只承载 0~2，3 是右侧搜索按钮对应的页面。 */
private const val SEARCH_PAGE_INDEX = 3

private fun lerpDp(start: Dp, stop: Dp, fraction: Float): Dp = start + (stop - start) * fraction
private fun lerpColor(start: Color, stop: Color, fraction: Float): Color = androidx.compose.ui.graphics.lerp(start, stop, fraction)

/** 非 State 的迷你条坐标引用：形变动画期间更新坐标不触发 AppNavigation 根组件重组，仅在打开全屏播放页时读取。 */
private class MiniPlayerBoundsHolder(
    var container: Rect? = null,
    var artwork: Rect? = null
)

/** 独立可跳过的液态玻璃容器：隔绝外层交互状态切换导致的重组，防止 backdrop 重建 ShapeProvider。 */
@Composable
private fun LeftBarLiquidGlassContainer(
    backdrop: Backdrop,
    cornerRadius: Dp,
    layerBlock: GraphicsLayerScope.() -> Unit,
    highlightEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val barShape = remember(cornerRadius) { RoundedRectangle(cornerRadius) }
    val isDark = AppTheme.isDark
    Box(
        modifier = modifier
            .liquidGlass(
                backdrop = backdrop,
                cornerRadius = cornerRadius,
                isDarkSurface = isDark,
                bloomExtraDegrees = -45f, // 边缘高光偏转角度（度）
                // 按下放大时同步缩放被采样的内容层，避免折射采样与组件本体错位
                layerBlock = layerBlock
            )
            .interactiveGlassHighlight(
                shape = barShape,
                enabled = highlightEnabled,
                isDarkSurface = isDark
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadius)),
            content = content
        )
    }
}

@Composable
private fun BottomBarTabsLayer(
    items: List<BottomNavItem>,
    normalLeftWidth: Dp,
    ballSize: Dp,
    lastActiveTab: Int,
    selectedIndex: Int,
    isSearchExpanded: Boolean,
    searchProgressState: State<Float>,
    compactProgress: Animatable<Float, *>,
    isSelectedLayer: Boolean
) {
    val slotWidth = normalLeftWidth / items.size
    val colors = AppTheme.colors
    val accentRed = colors.accent
    val darkTint = colors.iconPrimary
    Box(modifier = Modifier.fillMaxSize()) {
        items.forEachIndexed { index, item ->
            val isActiveTab = index == lastActiveTab
            val normalCenterX = slotWidth * (index + 0.5f)
            val targetCenterX = ballSize / 2
            val normalCenterY = 18.dp // 展开态图标中心距底栏顶部的基准偏移
            val targetCenterY = ballSize / 2

            val iconColor: Color
            val iconRes: Int
            if (isSelectedLayer) {
                iconColor = accentRed
                iconRes = item.selectedIconRes
            } else {
                val collapseProgress = maxOf(searchProgressState.value, compactProgress.value)
                val collapsedSelected = isActiveTab && selectedIndex == index && !isSearchExpanded
                val blendFraction = if (collapsedSelected) {
                    // 紧凑态切换区间 [0.15, 0.60] 内将选中图标由深色渐变为强调色
                    ((collapseProgress - 0.15f) / 0.45f).coerceIn(0f, 1f)
                } else {
                    0f
                }
                iconColor = if (blendFraction > 0f) lerpColor(darkTint, accentRed, blendFraction) else darkTint
                // 收缩进度超过 0.35f 时切换为实心选中图标
                iconRes = if (collapsedSelected && collapseProgress > 0.35f) item.selectedIconRes else item.iconRes
            }

            Box(
                modifier = Modifier
                    .offset {
                        val collapseProgress = maxOf(
                            searchProgressState.value,
                            compactProgress.value
                        )
                        val currentCenterX = if (isActiveTab) {
                            lerpDp(normalCenterX, targetCenterX, collapseProgress)
                        } else {
                            normalCenterX
                        }
                        val currentCenterY = if (isActiveTab) {
                            lerpDp(normalCenterY, targetCenterY, collapseProgress)
                        } else {
                            normalCenterY
                        }
                        IntOffset(
                            (currentCenterX - 11.dp).roundToPx(),
                            (currentCenterY - 11.dp).roundToPx()
                        )
                    }
                    .size(22.dp)
                    .drawWithContent {
                        val iconAlpha = if (isActiveTab) {
                            1f
                        } else {
                            val collapseProgress = maxOf(
                                searchProgressState.value,
                                compactProgress.value
                            )
                            // 非激活 Tab 图标随收缩进度快速淡出（倍数 3.5f，约 0.29 时完全透明）
                            (1f - collapseProgress * 3.5f).coerceIn(0f, 1f)
                        }
                        if (iconAlpha > 0.001f) {
                            drawContent()
                        }
                    }
                    .graphicsLayer {
                        alpha = if (isActiveTab) {
                            1f
                        } else {
                            val collapseProgress = maxOf(
                                searchProgressState.value,
                                compactProgress.value
                            )
                            (1f - collapseProgress * 3.5f).coerceIn(0f, 1f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = if (isSelectedLayer) null else item.title,
                    tint = iconColor,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (normalCenterX - 28.dp).roundToPx(),
                            33.dp.roundToPx() // Tab 文字标签顶部偏移
                        )
                    }
                    .width(56.dp)
                    .drawWithContent {
                        val collapseProgress = maxOf(
                            searchProgressState.value,
                            compactProgress.value
                        )
                        val labelAlpha = (1f - collapseProgress * 3.5f).coerceIn(0f, 1f)
                        if (labelAlpha > 0.001f) {
                            drawContent()
                        }
                    }
                    .graphicsLayer {
                        val collapseProgress = maxOf(
                            searchProgressState.value,
                            compactProgress.value
                        )
                        alpha = (1f - collapseProgress * 3.5f).coerceIn(0f, 1f)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.title,
                    color = if (isSelectedLayer) accentRed else darkTint,
                    style = MiuixTheme.textStyles.footnote2.copy(
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp,
                        fontWeight = if (isSelectedLayer) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SearchButtonLiquidGlassContainer(
    backdrop: Backdrop,
    cornerRadius: Dp,
    pressProgressAnimation: Animatable<Float, *>,
    highlightEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val buttonShape = remember(cornerRadius) { RoundedRectangle(cornerRadius) }
    val isDark = AppTheme.isDark
    Box(
        modifier = modifier
            .liquidGlass(
                backdrop = backdrop,
                cornerRadius = cornerRadius,
                surfaceAlpha = GlassDefaults.SurfaceAlphaBright, // 高亮玻璃表面透明度
                isDarkSurface = isDark,
                bloomExtraDegrees = -45f, // 边缘高光偏转角度（度）
                layerBlock = {
                    val progress = pressProgressAnimation.value.coerceIn(0f, 1f)
                    if (size.width > 0f) {
                        val maxScale = (size.width + 6f.dp.toPx()) / size.width // 按压时最多额外放大 6.dp
                        val scale = lerp(1f, maxScale, progress)
                        scaleX = scale
                        scaleY = scale
                    }
                }
            )
            .interactiveGlassHighlight(
                shape = buttonShape,
                enabled = highlightEnabled,
                isDarkSurface = isDark
            ),
        content = content
    )
}

/**
 * 应用的主导航根组件：串联自适应侧栏/悬浮底栏、页面内容舞台、迷你播放条、全屏播放页过渡层、账号抽屉及断网弹窗。
 */
@Composable
fun AppNavigation(
    avatarUrl: String? = null,
    displayName: String? = null,
    onAvatarClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val window = LocalAdaptiveWindow.current
    val useRail = window.useNavigationRail
    val railPadding = if (useRail) window.sideControlWidth else 0.dp
    val railStartPadding = if (window.sideControlsAtStart) railPadding else 0.dp
    val railEndPadding = if (window.sideControlsAtStart) 0.dp else railPadding
    val navigationState: AppNavigationState = viewModel()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val radioViewModel: RadioViewModel = hiltViewModel()
    val mineViewModel: MineViewModel = hiltViewModel()
    val searchViewModel: SearchViewModel = hiltViewModel()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()
    val mainViewModel: MainViewModel = hiltViewModel()

    // 注意：这里刻意不订阅 searchViewModel.uiState 与 playerViewModel.playbackState。
    // 二者都是高频变化的流（输入每个字符 / 播放位置每 500ms），一旦在外壳作用域读取，
    // 整个导航外壳（含玻璃 Backdrop、导航项、正在播放、账号抽屉等覆盖层）都会被重组。
    // 真正的消费者在各自的子组件内部订阅：SearchQueryField / SearchMiniPlayerBar。
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val showOfflineDialog by mainViewModel.showOfflineDialog.collectAsStateWithLifecycle()
    val isRetrying by mainViewModel.isRetrying.collectAsStateWithLifecycle()
    val dialogTitle by mainViewModel.dialogTitle.collectAsStateWithLifecycle()
    val dialogDescription by mainViewModel.dialogDescription.collectAsStateWithLifecycle()

    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var showPlaylistQueueSheet by rememberSaveable { mutableStateOf(false) }
    val miniPlayerBoundsRef = remember { MiniPlayerBoundsHolder() }
    var nowPlayingBoundsRevision by remember { mutableIntStateOf(0) }
    var frozenOpeningContainerBounds by remember { mutableStateOf<Rect?>(null) }
    var frozenOpeningArtworkBounds by remember { mutableStateOf<Rect?>(null) }
    var nowPlayingHostBounds by remember { mutableStateOf<Rect?>(null) }
    var nowPlayingArtworkBounds by remember { mutableStateOf<Rect?>(null) }
    var nowPlayingArtworkCorner by remember { mutableStateOf(14.dp) }
    var nowPlayingArtworkShadow by remember { mutableStateOf(26.dp) }
    var nowPlayingArtworkTrackTop by remember { mutableStateOf(false) }
    var nowPlayingArtworkUrl by remember { mutableStateOf<String?>(null) }
    val nowPlayingProgress = remember { Animatable(if (showNowPlaying) 1f else 0f) }
    val nowPlayingScope = rememberCoroutineScope()
    var nowPlayingAnimJob by remember { mutableStateOf<Job?>(null) }
    val rootDensity = LocalDensity.current
    val rootContext = LocalContext.current

    // 仅在曲目封面 URL 变化时发射，避免每 500ms 播放进度更新触发导航外壳重组
    val activeArtworkUrlFlow = remember(playerViewModel) {
        playerViewModel.playbackState
            .map { state -> state.currentTrack?.coverUrl ?: state.currentTrack?.album?.coverUrl }
            .distinctUntilChanged()
    }
    val activeArtworkUrl by activeArtworkUrlFlow.collectAsStateWithLifecycle(initialValue = null)

    // 起播或网络超时进入 ERROR 状态时弹出提示，避免界面静默停在暂停图标
    val playbackErrorFlow = remember(playerViewModel) {
        playerViewModel.playbackState
            .map { state -> (state.status == PlayerStatus.ERROR) to state.errorMessage }
            .distinctUntilChanged()
    }
    val playbackError by playbackErrorFlow.collectAsStateWithLifecycle(initialValue = false to null)
    LaunchedEffect(playbackError) {
        val (isError, message) = playbackError
        if (isError && !message.isNullOrBlank()) {
            Toast.makeText(rootContext, message, Toast.LENGTH_SHORT).show()
        }
    }
    val hasSearchPlaylistDetailFlow = remember(searchViewModel) {
        searchViewModel.uiState
            .map { it.activePlaylistDetail != null }
            .distinctUntilChanged()
    }
    val hasSearchPlaylistDetail by hasSearchPlaylistDetailFlow.collectAsStateWithLifecycle(initialValue = false)
    val effectiveArtworkUrl = activeArtworkUrl ?: nowPlayingArtworkUrl
    // 仅在全屏播放页打开时加载高分辨率封面，避免与冷启动列表大图争抢解码与带宽；未打开时迷你播放条使用 128px 缩略图
    val highResArtworkRequest = rememberHighResArtworkRequest(
        effectiveArtworkUrl.takeIf { showNowPlaying }
    )
    val sharedArtworkPainter = if (highResArtworkRequest != null) {
        rememberAsyncImagePainter(
            model = highResArtworkRequest,
            filterQuality = FilterQuality.High
        )
    } else null
    val sharedArtworkPair = remember(effectiveArtworkUrl, sharedArtworkPainter) {
        val normalized = normalizeHighResCoverUrl(effectiveArtworkUrl)
        if (!normalized.isNullOrBlank() && sharedArtworkPainter != null) {
            normalized to sharedArtworkPainter
        } else null
    }

    var showAccountSheet by rememberSaveable { mutableStateOf(false) }
    var mountAccountOverlay by remember { mutableStateOf(showAccountSheet) }
    var selectedArtist by navigationState::selectedArtist
    val albumOverlayState by mainViewModel.albumOverlayState.collectAsStateWithLifecycle()
    val currentShowNowPlaying by rememberUpdatedState(showNowPlaying)
    var artistRequestSeq by remember { mutableLongStateOf(0L) }

    fun openPlayer(artworkUrl: String?) {
        nowPlayingAnimJob?.cancel()
        if (!artworkUrl.isNullOrBlank()) {
            nowPlayingArtworkUrl = artworkUrl
        }
        frozenOpeningContainerBounds = miniPlayerBoundsRef.container
        frozenOpeningArtworkBounds = miniPlayerBoundsRef.artwork
        val wasOpen = showNowPlaying
        showNowPlaying = true
        nowPlayingAnimJob = nowPlayingScope.launch {
            if (!wasOpen) {
                nowPlayingProgress.snapTo(0f)
                // 等待全屏播放页完成首帧布局以获取精确目标封面 Rect
                withFrameNanos { }
            }
            nowPlayingProgress.animateTo(
                targetValue = 1f,
                // 播放页展开弹簧：无回弹阻尼、刚度 310f（越大展开越快）
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = 310f,
                    visibilityThreshold = 0.0005f
                )
            )
            frozenOpeningContainerBounds = null
            frozenOpeningArtworkBounds = null
        }
    }

    fun closePlayer(initialVelocity: Float = 0f) {
        nowPlayingAnimJob?.cancel()
        frozenOpeningContainerBounds = null
        frozenOpeningArtworkBounds = null
        nowPlayingAnimJob = nowPlayingScope.launch {
            nowPlayingProgress.animateTo(
                targetValue = 0f,
                initialVelocity = initialVelocity,
                // 播放页收起弹簧：无回弹阻尼、刚度 320f
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = 320f,
                    visibilityThreshold = 0.001f
                )
            )
            showNowPlaying = false
            nowPlayingArtworkBounds = null
        }
    }

    LaunchedEffect(window.width, window.height, window.separatingFold) {
        miniPlayerBoundsRef.container = null
        miniPlayerBoundsRef.artwork = null
        frozenOpeningContainerBounds = null
        frozenOpeningArtworkBounds = null
        nowPlayingArtworkBounds = null
    }

    LaunchedEffect(authViewModel.dismissSheet) {
        authViewModel.dismissSheet.collect {
            showAccountSheet = false
        }
    }

    LaunchedEffect(showAccountSheet) {
        if (showAccountSheet) {
            mountAccountOverlay = true
        }
    }

    // 背景底色会参与毛玻璃采样，决定整个导航栏的基础明度。
    val isDark = AppTheme.isDark
    val colors = AppTheme.colors
    val backgroundColorState = rememberUpdatedState(colors.background)

    // 右侧独立搜索按钮使用单独的动画状态，避免和主导航条互相影响。
    val searchAnimationScope = rememberCoroutineScope()
    val searchProgressAnimation = remember { Animatable(0f) }

    // 搜索展开后的关闭按钮按下动画状态
    val closeAnimationScope = rememberCoroutineScope()
    val closeProgressAnimation = remember { Animatable(0f) }

    // 按钮按压反馈弹簧参数（阻尼比 0.8f，刚度 500f）
    val animationSpec = remember { spring<Float>(0.8f, 500f, 0.001f) }

    val backdrop = rememberLayerBackdrop {
        drawRect(backgroundColorState.value)
        drawContent()
    }
    val bottomTabsBackdrop = rememberLayerBackdrop()
    val combinedTabBackdrop = rememberCombinedBackdrop(backdrop, bottomTabsBackdrop)

    // 记录当前选中的导航项：
    // 0~2 对应左侧主导航条，3 对应右侧搜索按钮。
    val selectedIndexState = rememberSaveable { mutableIntStateOf(0) }
    var lastActiveTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(selectedIndexState.intValue) {
        if (selectedIndexState.intValue != SEARCH_PAGE_INDEX) {
            lastActiveTab = selectedIndexState.intValue
        }
    }

    val leftSegmentState = rememberLiquidSegmentState(
        itemCount = MainNavItems.size,
        selectedIndex = lastActiveTab
    )

    val isSearchExpanded by remember {
        derivedStateOf { selectedIndexState.intValue == SEARCH_PAGE_INDEX }
    }

    val searchProgressState = animateFloatAsState(
        targetValue = if (isSearchExpanded) 1f else 0f,
        // 搜索按钮展开/收起形态过渡弹簧
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
        label = "searchProgress"
    )

    val showGlassSlider = !isSearchExpanded
    val sliderAlphaState = animateFloatAsState(
        targetValue = if (showGlassSlider) 1f else 0f,
        // 展开搜索时立即隐藏滑块，收起时弹簧渐显
        animationSpec = if (isSearchExpanded) snap() else spring(stiffness = 500f, dampingRatio = 0.8f),
        label = "sliderAlpha"
    )

    BackHandler(enabled = isSearchExpanded) {
        searchViewModel.onClearQuery()
        selectedIndexState.intValue = lastActiveTab
    }

    // 共享底栏与迷你条几何参数（统一按此比例计算圆角与槽位）
    val horizontalPadding = 20.dp // 底栏及迷你播放条左右外边距
    val navBarHeight = 54.dp // 底部导航胶囊与搜索圆形按钮统一高度
    val mainSearchGap = 12.dp // 正常态主导航条与右侧搜索按钮的间距
    val miniPlayerNavGap = 12.dp // 展开态迷你播放条与底部导航栏之间的垂直间距
    val searchButtonWidth = navBarHeight
    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val searchContentTopPadding = statusBarTopPadding + 86.dp // 顶部安全区与大标题预留偏移
    // 使用 RoundedRectangle 实现 G² 连续圆角，与底栏半高对齐
    val cornerRadius = navBarHeight / 2

    // 键盘（IME）弹出时底栏与迷你播放条的平滑上抬偏移
    val imeBottomPx = WindowInsets.ime.getBottom(LocalDensity.current)
    val navBarsBottomPx = WindowInsets.navigationBars.getBottom(LocalDensity.current)
    // 键盘高度减去已经由 navigationBars inset 处理的底部距离
    val targetImeOffsetDp = with(LocalDensity.current) {
        (imeBottomPx - navBarsBottomPx).coerceAtLeast(0).toDp()
    }
    val animatedImeOffset by animateDpAsState(
        targetValue = targetImeOffsetDp,
        // IME 上抬弹簧参数（阻尼比 0.8f，刚度 500f）
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
        label = "imeOffset"
    )

    // 一级页面列表状态：供 NavigationScrollState 监听滚动以驱动底栏紧凑态
    val homeListState = rememberLazyListState()
    val radioListState = rememberLazyListState()
    val mineListState = rememberLazyListState()

    val activeViewport = remember(selectedIndexState.intValue, homeListState, radioListState, mineListState) {
        when (selectedIndexState.intValue) {
            0 -> homeListState.toNavigationViewport()
            1 -> radioListState.toNavigationViewport()
            2 -> mineListState.toNavigationViewport()
            else -> null
        }
    }

    val navigationScroll = rememberNavigationScrollState(
        viewport = activeViewport,
        enabled = !useRail && !isSearchExpanded && !showNowPlaying
    )

    val shouldCompactMiniPlayer = !useRail && navigationScroll.compactTarget && !isSearchExpanded && !showNowPlaying
    val miniPlayerCenterTravel = navBarHeight + miniPlayerNavGap
    val miniPlayerChromeMotion = rememberMiniPlayerChromeMotion(
        compact = shouldCompactMiniPlayer,
        playerVisible = showNowPlaying,
        centerTravel = miniPlayerCenterTravel
    )
    val isPlayerOpaque by remember {
        derivedStateOf { showNowPlaying && nowPlayingProgress.value >= 0.999f }
    }

    val trackNavigator = remember(selectedIndexState, mainViewModel) {
        TrackActionNavigator(
            onOpenArtist = { artistId, artistName, artistCoverUrl ->
                val targetPage = selectedIndexState.intValue
                if (currentShowNowPlaying) closePlayer()
                mainViewModel.closeAlbumDetail(targetPage)
                selectedArtist = SelectedArtist(
                    id = artistId,
                    name = artistName,
                    coverUrl = artistCoverUrl,
                    pageIndex = targetPage,
                    requestId = ++artistRequestSeq
                )
            },
            onOpenAlbum = { albumId, albumTitle, artistName, coverUrl ->
                val targetPage = selectedIndexState.intValue
                if (currentShowNowPlaying) closePlayer()
                mainViewModel.openAlbumDetail(
                    pageIndex = targetPage,
                    albumId = albumId,
                    title = albumTitle,
                    artistName = artistName,
                    coverUrl = coverUrl
                )
            }
        )
    }

    // 根层叠容器：自下而上依次为页面内容、迷你播放条、导航栏、播放页与各类浮层
    CompositionLocalProvider(
        LocalGlassBackdrop provides backdrop,
        LocalContentBottomPadding provides (if (useRail) 112.dp else 150.dp), // 页面底部安全留白（需覆盖底栏与迷你播放条）
        LocalPlayerArtwork provides sharedArtworkPair,
        LocalNavigationScrollState provides navigationScroll,
        LocalTrackNavigation provides trackNavigator
    ) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 底层页面内容与毛玻璃采样层
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 全屏播放页完全不透明覆盖时跳过底层页面树与毛玻璃取景层绘制，节省 GPU/RenderThread 开销
                .drawWithContent { if (!isPlayerOpaque) drawContent() }
                // 监听列表滚动驱动底栏与迷你播放器变动
                .nestedScroll(navigationScroll.nestedScrollConnection)
                // 毛玻璃取景层：内容先渲染到此底层纹理，再供上层悬浮玻璃组件模糊采样
                .layerBackdrop(backdrop)
        ) {
            if (useRail) {
                ExpandedChromeBackground(atStart = window.sideControlsAtStart)
                ExpandedPlaylistChromeBackground(selectedIndexState, homeViewModel, radioViewModel, mineViewModel, searchViewModel)
            }
            Box(Modifier.fillMaxSize().padding(start = railStartPadding, end = railEndPadding).testTag("chrome_content")) {
            AppPageContent(
                selectedIndex = selectedIndexState,
                isHostForeground = !isPlayerOpaque && !showAccountSheet,
                topContentPadding = searchContentTopPadding,
                imeOffset = animatedImeOffset,
                avatarUrl = authUiState.authSession?.avatarUrl,
                displayName = authUiState.authSession?.nickname,
                homeViewModel = homeViewModel,
                radioViewModel = radioViewModel,
                mineViewModel = mineViewModel,
                searchViewModel = searchViewModel,
                onAccountClick = { showAccountSheet = true },
                onSearchAvatarClick = {
                    showAccountSheet = true
                    onAvatarClick?.invoke()
                },
                selectedArtist = selectedArtist,
                onArtistClick = { pageIndex, artistId, artistName, artistCoverUrl ->
                    selectedArtist = SelectedArtist(
                        id = artistId,
                        name = artistName,
                        coverUrl = artistCoverUrl,
                        pageIndex = pageIndex,
                        requestId = ++artistRequestSeq
                    )
                },
                onCloseArtist = { closingPageIndex ->
                    if (closingPageIndex == null || selectedArtist?.pageIndex == closingPageIndex) {
                        selectedArtist = null
                    }
                },
                albumOverlayState = albumOverlayState,
                onCloseAlbumOverlay = { closingPageIndex ->
                    mainViewModel.closeAlbumDetail(closingPageIndex)
                },
                onPlayAlbumTracks = mainViewModel::playAlbumTracks,
                homeListState = homeListState,
                radioListState = radioListState,
                mineListState = mineListState
            )
            }
        }
        if (useRail && isSearchExpanded && selectedArtist?.pageIndex != SEARCH_PAGE_INDEX && albumOverlayState?.pageIndex != SEARCH_PAGE_INDEX && !hasSearchPlaylistDetail) {
            Box(Modifier.fillMaxWidth().padding(start = railStartPadding + 20.dp, end = railEndPadding + 20.dp)
                .statusBarsPadding().padding(top = 68.dp), contentAlignment = Alignment.TopCenter) {
                // 宽屏模式搜索页悬浮输入栏（最大宽度 680.dp、高度 54.dp、胶囊圆角 27.dp）
                LiquidGlassPanel(Modifier.widthIn(max = 680.dp).fillMaxWidth().height(54.dp), cornerRadius = 27.dp) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_nav_search), null, Modifier.size(20.dp), tint = colors.accent)
                        Spacer(Modifier.width(10.dp))
                        SearchQueryInputContent(searchViewModel, enabled = true, modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
        // 悬浮底部导航栏与搜索按钮
        val isBottomChromeHidden by PlayerEntryPoints.isBottomChromeHidden.collectAsStateWithLifecycle()
        val bottomChromeHideProgressState = animateFloatAsState(
            targetValue = if (isBottomChromeHidden) 1f else 0f,
            // 底部导航与迷你条整体显隐弹簧（无回弹、刚度 340f）
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 340f),
            label = "bottomChromeHideProgress"
        )
        val isBottomChromeVisible by remember {
            derivedStateOf { bottomChromeHideProgressState.value < 0.999f }
        }
        if (!useRail && isBottomChromeVisible) {
        AdaptiveOverlayPane(Modifier.fillMaxSize().zIndex(2f)) {
        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(2f)
                .drawWithContent { if (!isPlayerOpaque) drawContent() }
                .windowInsetsPadding(WindowInsets.navigationBars)
                // 底栏上下边距（底部预留 6.dp 悬浮感并叠加键盘偏移）
                .padding(top = 16.dp, bottom = 6.dp + animatedImeOffset)
                .horizontalChromeFrame(horizontalPadding)
                .height(navBarHeight).testTag("horizontal_glass_dock")
                .graphicsLayer {
                    val playerOpenSink = (nowPlayingProgress.value * 1.25f).coerceIn(0f, 1f) // 播放页展开时底栏提前加速淡出
                    val hideProgress = bottomChromeHideProgressState.value
                    val hintScale = 1f - 0.05f * navigationScroll.scrollHintProgress.value // 滚动提示触发时轻微缩小（最大 5%）
                    alpha = (1f - playerOpenSink) * (1f - hideProgress)
                    translationY = playerOpenSink * 48.dp.toPx() + hideProgress * 150.dp.toPx()
                    scaleX = hintScale
                    scaleY = hintScale
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
        ) {
            val totalWidth = maxWidth
            val ballSize = navBarHeight
            val normalLeftWidth = totalWidth - ballSize - mainSearchGap

            val searchGap = 10.dp // 搜索展开态下两侧圆形按钮与中间搜索框的间距
            val searchBarStartX = totalWidth - ballSize // 搜索按钮收起态起始 X（右侧圆钮位置）
            val searchBarTargetX = ballSize + searchGap // 搜索栏展开态目标 X（紧邻左侧主导航圆钮）
            val searchBarTargetWidth = totalWidth - (ballSize * 2) - (searchGap * 2) // 搜索栏展开态宽度

            val isLeftBarCompactClickable by remember {
                derivedStateOf { isSearchExpanded || navigationScroll.compactProgress.value > 0.5f }
            }
            val isLeftBarDragEnabled by remember {
                derivedStateOf {
                    !isSearchExpanded &&
                        searchProgressState.value < 0.05f &&
                        navigationScroll.compactProgress.value < 0.05f
                }
            }
            val showCloseButton by remember {
                derivedStateOf { searchProgressState.value > 0.01f }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val thumbAlphaProvider: () -> Float = {
                    if (isSearchExpanded) {
                        0f
                    } else {
                        val collapseProgress = maxOf(
                            searchProgressState.value,
                            navigationScroll.compactProgress.value
                        )
                        // 滑块水滴在底栏收起进度达到 35% 前完成淡出
                        val thumbExpansionAlpha = ((1f - collapseProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)
                        sliderAlphaState.value * thumbExpansionAlpha
                    }
                }

                val outerBarLayerBlock: GraphicsLayerScope.() -> Unit = {
                    val progress = leftSegmentState.pressProgress.value.coerceIn(0f, 1f)
                    if (size.width > 0f) {
                        val maxScale = (size.width + 10f.dp.toPx()) / size.width // 主导航条按压时横向最多额外放大 10.dp
                        val scale = lerp(1f, maxScale, progress)
                        scaleX = scale
                        scaleY = scale
                    }
                }

                val selectedTabsLayer: @Composable BoxScope.() -> Unit = {
                    BottomBarTabsLayer(
                        items = MainNavItems,
                        normalLeftWidth = normalLeftWidth,
                        ballSize = ballSize,
                        lastActiveTab = lastActiveTab,
                        selectedIndex = selectedIndexState.intValue,
                        isSearchExpanded = isSearchExpanded,
                        searchProgressState = searchProgressState,
                        compactProgress = navigationScroll.compactProgress,
                        isSelectedLayer = true
                    )
                }

                // 左侧主导航条：包含首页/电台/我的，搜索展开或列表下滚时两边缩减为圆形按钮
                Box(
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            val collapseProgress = maxOf(
                                searchProgressState.value,
                                navigationScroll.compactProgress.value
                            )
                            val widthPx = lerpDp(normalLeftWidth, ballSize, collapseProgress)
                                .roundToPx()
                                .coerceIn(0, constraints.maxWidth)
                            val placeable = measurable.measure(
                                constraints.copy(minWidth = widthPx, maxWidth = widthPx)
                            )
                            layout(widthPx, placeable.height) {
                                placeable.placeRelative(0, 0)
                            }
                        }
                        .fillMaxHeight().testTag("bottom_navigation_button")
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = isLeftBarCompactClickable
                        ) {
                            if (isSearchExpanded) {
                                searchViewModel.onClearQuery()
                                selectedIndexState.intValue = lastActiveTab
                            } else if (navigationScroll.compactProgress.value > 0.5f) {
                                navigationScroll.expand()
                            }
                        }
                        .liquidSegmentDrag(
                            state = leftSegmentState,
                            itemCount = MainNavItems.size,
                            selectedIndex = lastActiveTab,
                            onSettle = {
                                if (selectedIndexState.intValue == it) {
                                    // 重复点击当前已选中的 Tab 时收起歌单与艺人页
                                    when (it) {
                                        0 -> homeViewModel.closePlaylistDetail()
                                        1 -> radioViewModel.closePlaylistDetail()
                                        2 -> mineViewModel.closePlaylistDetail()
                                    }
                                    if (selectedArtist?.pageIndex == it) {
                                        selectedArtist = null
                                    }
                                }
                                selectedIndexState.intValue = it
                                lastActiveTab = it
                            },
                            // 搜索展开或处于紧凑态时底栏交给 clickable 处理（点击展开），不响应滑动切换
                            enabled = isLeftBarDragEnabled
                        )
                ) {
                    LeftBarLiquidGlassContainer(
                        backdrop = backdrop,
                        cornerRadius = cornerRadius,
                        layerBlock = outerBarLayerBlock,
                        highlightEnabled = isLeftBarDragEnabled,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 静息双色切割：滑块外主图标色、滑块内强调色填充
                        LiquidGlassDualColorTabs(
                            state = leftSegmentState,
                            containerWidth = normalLeftWidth,
                            containerHeight = navBarHeight,
                            cornerRadius = cornerRadius,
                            alphaProvider = thumbAlphaProvider,
                            unselectedContent = {
                                BottomBarTabsLayer(
                                    items = MainNavItems,
                                    normalLeftWidth = normalLeftWidth,
                                    ballSize = ballSize,
                                    lastActiveTab = lastActiveTab,
                                    selectedIndex = selectedIndexState.intValue,
                                    isSearchExpanded = isSearchExpanded,
                                    searchProgressState = searchProgressState,
                                    compactProgress = navigationScroll.compactProgress,
                                    isSelectedLayer = false
                                )
                            },
                            selectedContent = selectedTabsLayer
                        )
                    }

                    // 离屏激活录制层（同级节点）：按压/拖动时将柔和玻璃底板与高亮红 Tab 写入 bottomTabsBackdrop
                    LiquidGlassActiveTabsRecorder(
                        pageBackdrop = backdrop,
                        activeTabsBackdrop = bottomTabsBackdrop,
                        state = leftSegmentState,
                        containerWidth = normalLeftWidth,
                        containerHeight = navBarHeight,
                        cornerRadius = cornerRadius,
                        layerBlock = outerBarLayerBlock,
                        alphaProvider = thumbAlphaProvider,
                        modifier = Modifier.align(Alignment.TopStart),
                        selectedContent = selectedTabsLayer
                    )

                    // 激活态 3D 放大镜水滴滑块（同级兄弟节点，不被外框裁剪，折射下方高亮图标/文字与玻璃表面）
                    LiquidGlassActiveDroplet(
                        combinedBackdrop = combinedTabBackdrop,
                        state = leftSegmentState,
                        containerWidth = normalLeftWidth,
                        containerHeight = navBarHeight,
                        cornerRadius = cornerRadius,
                        maxPressScale = GlassSliderDefaults.MaxPressScale,
                        alphaProvider = thumbAlphaProvider,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }

                // 搜索按钮与展开态搜索栏：从右侧圆形按钮向左侧展开为输入长条
                val searchInteractionSource = remember { MutableInteractionSource() }

                LaunchedEffect(searchInteractionSource) {
                    searchInteractionSource.interactions.collect { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> {
                                searchAnimationScope.launch { searchProgressAnimation.animateTo(1f, animationSpec) }
                            }
                            is PressInteraction.Release, is PressInteraction.Cancel -> {
                                searchAnimationScope.launch { searchProgressAnimation.animateTo(0f, animationSpec) }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                lerpDp(searchBarStartX, searchBarTargetX, searchProgressState.value).roundToPx(),
                                0
                            )
                        }
                        .layout { measurable, constraints ->
                            val widthPx = lerpDp(ballSize, searchBarTargetWidth, searchProgressState.value)
                                .roundToPx()
                                .coerceIn(0, constraints.maxWidth)
                            val placeable = measurable.measure(
                                constraints.copy(minWidth = widthPx, maxWidth = widthPx)
                            )
                            layout(widthPx, placeable.height) {
                                placeable.placeRelative(0, 0)
                            }
                        }
                        .fillMaxHeight().testTag("bottom_search_button")
                        .clickable(
                            interactionSource = searchInteractionSource,
                            indication = null,
                            enabled = !isSearchExpanded
                        ) {
                            navigationScroll.expand()
                            lastActiveTab = selectedIndexState.intValue
                            selectedIndexState.intValue = SEARCH_PAGE_INDEX
                        }
                ) {
                    SearchButtonLiquidGlassContainer(
                        backdrop = backdrop,
                        cornerRadius = cornerRadius,
                        pressProgressAnimation = searchProgressAnimation,
                        highlightEnabled = !isSearchExpanded,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val showSearchInput by remember {
                            derivedStateOf { searchProgressState.value > 0.15f }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .layout { measurable, constraints ->
                                    val startPx = lerpDp((ballSize - 20.dp) / 2, 16.dp, searchProgressState.value).roundToPx()
                                    val endPx = 16.dp.roundToPx()
                                    val maxW = (constraints.maxWidth - startPx - endPx).coerceAtLeast(0)
                                    val placeable = measurable.measure(
                                        constraints.copy(minWidth = 0, maxWidth = maxW)
                                    )
                                    layout(constraints.maxWidth, placeable.height) {
                                        placeable.placeRelative(startPx, (constraints.maxHeight - placeable.height) / 2)
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val iconTint = if (isSearchExpanded) {
                                lerpColor(colors.iconSecondary, colors.accent, searchProgressState.value)
                            } else {
                                colors.iconSecondary
                            }
                            Icon(
                                painter = painterResource(id = R.drawable.ic_nav_search),
                                contentDescription = "搜索",
                                tint = iconTint,
                                modifier = Modifier.layout { measurable, _ ->
                                    val px = lerpDp(20.dp, 17.dp, searchProgressState.value).roundToPx().coerceAtLeast(1)
                                    val placeable = measurable.measure(Constraints.fixed(px, px))
                                    layout(px, px) { placeable.place(0, 0) }
                                }
                            )

                            if (showSearchInput) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .graphicsLayer {
                                            alpha = ((searchProgressState.value - 0.2f) / 0.8f).coerceIn(0f, 1f)
                                        },
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    SearchQueryInputContent(
                                        viewModel = searchViewModel,
                                        enabled = isSearchExpanded,
                                        modifier = Modifier.fillMaxWidth().fillMaxHeight()
                                    )
                                }
                            }
                        }
                    }
                }

                // 右侧关闭按钮：搜索展开时在右下角搜索位缩放出现
                if (showCloseButton) {
                    val closeInteractionSource = remember { MutableInteractionSource() }

                    LaunchedEffect(closeInteractionSource) {
                        closeInteractionSource.interactions.collect { interaction ->
                            when (interaction) {
                                is PressInteraction.Press -> {
                                    closeAnimationScope.launch { closeProgressAnimation.animateTo(1f, animationSpec) }
                                }
                                is PressInteraction.Release, is PressInteraction.Cancel -> {
                                    closeAnimationScope.launch { closeProgressAnimation.animateTo(0f, animationSpec) }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .offset { IntOffset((totalWidth - ballSize).roundToPx(), 0) }
                            .size(ballSize)
                            .drawWithContent {
                                if (searchProgressState.value > 0.01f) {
                                    drawContent()
                                }
                            }
                            .liquidGlass(
                                backdrop = backdrop,
                                cornerRadius = cornerRadius,
                                surfaceAlpha = GlassDefaults.SurfaceAlphaBright,
                                isDarkSurface = isDark,
                                bloomExtraDegrees = -45f, // 边缘高光偏转角度（度）
                                layerBlock = {
                                    val progress = closeProgressAnimation.value.coerceIn(0f, 1f)
                                    val appearScale = searchProgressState.value.coerceIn(0f, 1f)
                                    alpha = appearScale
                                    if (appearScale > 0.01f && size.width > 0f) {
                                        val maxScale = (size.width + 4f.dp.toPx()) / size.width // 关闭按钮按压时最多额外放大 4.dp
                                        val pressScale = lerp(1f, maxScale, progress)
                                        scaleX = appearScale * pressScale
                                        scaleY = appearScale * pressScale
                                    }
                                }
                            )
                            .interactiveGlassHighlight(
                                shape = RoundedRectangle(cornerRadius),
                                enabled = isSearchExpanded,
                                isDarkSurface = isDark
                            )
                            .clickable(
                                interactionSource = closeInteractionSource,
                                indication = null,
                                enabled = isSearchExpanded
                            ) {
                                searchViewModel.onClearQuery()
                                selectedIndexState.intValue = lastActiveTab
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_close),
                            contentDescription = "关闭搜索",
                            tint = colors.iconPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        }
        }

        // 迷你播放条常规态距底部偏移（底栏高度 + 间距 + 键盘偏移）
        val normalMiniPlayerBottom = (if (useRail) 18.dp else 6.dp + navBarHeight + miniPlayerNavGap) + animatedImeOffset
        // 迷你播放条高度（侧栏模式 60.dp，底栏模式与底栏等高）
        val currentMiniPlayerHeight = if (useRail) 60.dp else navBarHeight

        if (isBottomChromeVisible && !useRail) {
            val sharesArtworkNow = if (showNowPlaying) {
                nowPlayingBoundsRevision // 仅在打开/关闭全屏播放页时订阅坐标变更
                val activeSourceArtwork = frozenOpeningArtworkBounds ?: miniPlayerBoundsRef.artwork
                activeSourceArtwork?.isUsable() == true && nowPlayingArtworkBounds?.isUsable() == true
            } else {
                false
            }
            AdaptiveOverlayPane(Modifier.fillMaxSize().zIndex(1f)) {
            SearchMiniPlayerBar(
                backdrop = backdrop,
                cornerRadius = if (useRail) 30.dp else cornerRadius,
                playbackStateFlow = playerViewModel.playbackState,
                contentProgress = miniPlayerChromeMotion.contentProgress,
                playerTransitionProgress = nowPlayingProgress.asState(),
                enabled = !showNowPlaying && !isBottomChromeHidden,
                onPlayPauseClick = { playerViewModel.onPlayPause() },
                onPlaylistClick = { showPlaylistQueueSheet = true },
                onNextClick = { playerViewModel.onNext() },
                onPreviousClick = { playerViewModel.onPrevious(forcePreviousSong = true) },
                onBarClick = { artworkUrl ->
                    if (!showNowPlaying) {
                        openPlayer(artworkUrl)
                    }
                },
                onContainerBoundsChanged = { bounds ->
                    if (miniPlayerBoundsRef.container != bounds) {
                        miniPlayerBoundsRef.container = bounds
                        if (showNowPlaying) nowPlayingBoundsRevision++
                    }
                },
                onArtworkBoundsChanged = { bounds ->
                    if (miniPlayerBoundsRef.artwork != bounds) {
                        miniPlayerBoundsRef.artwork = bounds
                        if (showNowPlaying) nowPlayingBoundsRevision++
                    }
                },
                hideArtwork = sharesArtworkNow || (showNowPlaying && nowPlayingProgress.value > 0.02f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(1f)
                    .drawWithContent { if (!isPlayerOpaque) drawContent() }
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(
                        start = railStartPadding,
                        end = railEndPadding,
                        bottom = normalMiniPlayerBottom
                    )
                    .offset {
                        IntOffset(
                            0,
                            (miniPlayerChromeMotion.verticalProgress.value * miniPlayerCenterTravel.toPx()).roundToInt()
                        )
                    }
                    .horizontalMiniPlayerFrame(
                        progressProvider = { miniPlayerChromeMotion.horizontalProgress.value },
                        horizontalPadding = horizontalPadding,
                        buttonSize = navBarHeight
                    )
                    .height(currentMiniPlayerHeight)
                    .graphicsLayer {
                        val shellFade = if (showNowPlaying) {
                            // 播放页展开期间提前淡出迷你播放条外壳
                            (1f - playerBackgroundAlpha(nowPlayingProgress.value) * 1.15f).coerceIn(0f, 1f)
                        } else 1f
                        val hideProgress = bottomChromeHideProgressState.value
                        val hintScale = 1f - 0.05f * navigationScroll.scrollHintProgress.value // 滚动提示触发时轻微缩小（最大 5%）
                        alpha = shellFade * (1f - hideProgress)
                        translationY = hideProgress * 180.dp.toPx() // 隐藏底部组件时向下移出视口距离
                        scaleX = hintScale
                        scaleY = hintScale
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
            )
        }

        }
        if (useRail && !showNowPlaying) {
            MusicNavigationRail(selectedIndexState.intValue, { index ->
                if (index == SEARCH_PAGE_INDEX && isSearchExpanded) {
                    searchViewModel.onClearQuery()
                    selectedIndexState.intValue = lastActiveTab
                } else {
                    if (selectedIndexState.intValue == index) {
                        when (index) {
                            0 -> homeViewModel.closePlaylistDetail()
                            1 -> radioViewModel.closePlaylistDetail()
                            2 -> mineViewModel.closePlaylistDetail()
                        }
                        if (selectedArtist?.pageIndex == index) {
                            selectedArtist = null
                        }
                    }
                    selectedIndexState.intValue = index
                }
            }, backdrop, Modifier.align(if (window.sideControlsAtStart) Alignment.CenterStart else Alignment.CenterEnd).zIndex(2f),
                header = {
                    UserAvatarButton(authUiState.authSession?.avatarUrl, authUiState.authSession?.nickname,
                        { showAccountSheet = true }, size = 44.dp, sideControl = true)
                }, miniPlayer = { inOverflow, closeOverflow ->
                    SideMiniPlayer(playerViewModel.playbackState, backdrop,
                        { closeOverflow(); openPlayer(it) }, { closeOverflow(); playerViewModel.onPlayPause() }, { closeOverflow(); showPlaylistQueueSheet = true },
                        {
                            val next = if (inOverflow) null else it
                            if (miniPlayerBoundsRef.container != next) {
                                miniPlayerBoundsRef.container = next
                                if (showNowPlaying) nowPlayingBoundsRevision++
                            }
                        },
                        {
                            val next = if (inOverflow) null else it
                            if (miniPlayerBoundsRef.artwork != next) {
                                miniPlayerBoundsRef.artwork = next
                                if (showNowPlaying) nowPlayingBoundsRevision++
                            }
                        })
                })
        }
        if (showNowPlaying) {
            nowPlayingBoundsRevision
            val activeSourceContainer = frozenOpeningContainerBounds ?: miniPlayerBoundsRef.container
            val activeSourceArtwork = frozenOpeningArtworkBounds ?: miniPlayerBoundsRef.artwork
            NowPlayingTransitionLayer(
                playerViewModel = playerViewModel,
                progress = nowPlayingProgress.asState(),
                artworkUrl = effectiveArtworkUrl,
                sourceContainerBounds = activeSourceContainer,
                sourceArtworkBounds = activeSourceArtwork,
                targetArtworkBounds = nowPlayingArtworkBounds,
                targetArtworkCorner = nowPlayingArtworkCorner,
                targetArtworkShadow = nowPlayingArtworkShadow,
                targetArtworkTrackTop = nowPlayingArtworkTrackTop,
                onHostBoundsChanged = { bounds ->
                    if (nowPlayingHostBounds != bounds) {
                        nowPlayingHostBounds = bounds
                    }
                },
                onTargetArtworkBoundsChanged = { bounds ->
                    if (nowPlayingArtworkBounds != bounds) {
                        nowPlayingArtworkBounds = bounds
                    }
                },
                onTargetArtworkCornerChanged = { corner ->
                    if (nowPlayingArtworkCorner != corner) {
                        nowPlayingArtworkCorner = corner
                    }
                },
                onTargetArtworkShadowChanged = { shadow ->
                    if (nowPlayingArtworkShadow != shadow) {
                        nowPlayingArtworkShadow = shadow
                    }
                },
                onTargetArtworkTrackTopChanged = { trackTop ->
                    if (nowPlayingArtworkTrackTop != trackTop) {
                        nowPlayingArtworkTrackTop = trackTop
                    }
                },
                onGrabberDragStart = {
                    nowPlayingAnimJob?.cancel()
                    frozenOpeningContainerBounds = null
                    frozenOpeningArtworkBounds = null
                },
                onGrabberDragDelta = { deltaPx ->
                    val hostH = nowPlayingHostBounds?.height ?: with(rootDensity) { window.height.toPx() }
                    val miniH = miniPlayerBoundsRef.container?.height ?: with(rootDensity) { navBarHeight.toPx() }
                    val travelPx = (hostH - miniH).coerceAtLeast(1f)
                    val nextP = (nowPlayingProgress.value - deltaPx / travelPx).coerceIn(0f, 1f)
                    nowPlayingScope.launch {
                        nowPlayingProgress.snapTo(nextP)
                    }
                },
                onGrabberDragEnd = { velocityY ->
                    val hostH = nowPlayingHostBounds?.height ?: with(rootDensity) { window.height.toPx() }
                    val miniH = miniPlayerBoundsRef.container?.height ?: with(rootDensity) { navBarHeight.toPx() }
                    val travelPx = (hostH - miniH).coerceAtLeast(1f)
                    val progressVelocity = -velocityY / travelPx
                    // 下拉关闭速度阈值（580.dp/s）、上推回弹速度阈值（-260.dp/s）与进度阈值（0.78f）
                    val flingClosePx = with(rootDensity) { 580.dp.toPx() }
                    val flingOpenPx = -with(rootDensity) { 260.dp.toPx() }
                    val shouldClose = velocityY > flingClosePx ||
                        (velocityY >= flingOpenPx && nowPlayingProgress.value < 0.78f)
                    if (shouldClose) {
                        closePlayer(initialVelocity = progressVelocity)
                    } else {
                        nowPlayingAnimJob?.cancel()
                        nowPlayingAnimJob = nowPlayingScope.launch {
                            nowPlayingProgress.animateTo(
                                targetValue = 1f,
                                initialVelocity = progressVelocity,
                                // 松手回弹展开播放页的弹簧参数（无回弹阻尼、刚度 360f）
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = 360f,
                                    visibilityThreshold = 0.0005f
                                )
                            )
                        }
                    }
                },
                onClose = { closePlayer() }
            )
        }

        if (showPlaylistQueueSheet) {
            val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
            PlaylistQueueSheet(
                playbackState = playbackState,
                onTrackClick = { index ->
                    playerViewModel.playQueueTrackAt(index)
                },
                onRemoveTrack = { index ->
                    playerViewModel.removeQueueTrackAt(index)
                },
                onClearQueue = {
                    playerViewModel.clearQueue()
                },
                onTogglePlaybackMode = {
                    playerViewModel.togglePlaybackMode()
                },
                onDismiss = {
                    showPlaylistQueueSheet = false
                }
            )
        }

        if (mountAccountOverlay) {
            AccountFullScreenOverlay(
                visible = showAccountSheet,
                backdrop = backdrop,
                onDismissRequest = { showAccountSheet = false },
                onDismissed = { mountAccountOverlay = false },
                viewModel = authViewModel,
                modifier = Modifier.zIndex(12f)
            )
        }

        BackHandler(enabled = showNowPlaying) { closePlayer() }

        // 液态玻璃断网重试弹窗（采样底层页面 Backdrop）
        Ios26NetworkOfflineDialog(
            isVisible = showOfflineDialog,
            backdrop = backdrop,
            isRetrying = isRetrying,
            title = dialogTitle,
            description = dialogDescription,
            onRetry = { mainViewModel.retry() },
            onDismiss = { mainViewModel.dismissOfflineDialog() }
        )
    }
    }
}

/**
 * 一级页面内容区域（首页 / 电台 / 我的 / 搜索）。
 *
 * 单独抽成组件是为了收窄重组范围：[selectedIndex] 以 [State] 形式传入并在组件内部读取，
 * 切换页面时仅此区域重组，外部导航栏、迷你播放条及各类浮层不受影响。
 *
 * 已访问页面保留在组合树中而不销毁重建，避免反复触发组件释放/重建及内部玻璃图层资源的开销；
 * 非当前页通过 [drawWithContent] 跳过绘制，并通过 [offset] 移出父容器范围以跳过触摸命中测试。
 */
/**
 * 一级页面横向切页过渡描述
 *
 * @property fromIndex 切出页索引
 * @property toIndex 切入页索引
 * @property direction 页面移动方向：1 为目标页索引更大（切入页从右向左滑入，切出页向左滑出）；-1 为目标页索引更小（切入页从左向右滑入，切出页向右滑出）
 */
private data class PageSlideTransition(
    val fromIndex: Int,
    val toIndex: Int,
    val direction: Int
)

@Composable
private fun AppPageContent(
    selectedIndex: State<Int>,
    isHostForeground: Boolean,
    topContentPadding: Dp,
    imeOffset: Dp,
    avatarUrl: String?,
    displayName: String?,
    homeViewModel: HomeViewModel,
    radioViewModel: RadioViewModel,
    mineViewModel: MineViewModel,
    searchViewModel: SearchViewModel,
    onAccountClick: () -> Unit,
    onSearchAvatarClick: () -> Unit,
    selectedArtist: SelectedArtist?,
    onArtistClick: (Int, Long, String, String?) -> Unit,
    onCloseArtist: (Int?) -> Unit,
    albumOverlayState: GlobalAlbumOverlayState?,
    onCloseAlbumOverlay: (Int?) -> Unit,
    onPlayAlbumTracks: (List<Track>, Int) -> Unit,
    homeListState: LazyListState,
    radioListState: LazyListState,
    mineListState: LazyListState
) {
    val index = selectedIndex.value
    val colors = AppTheme.colors
    val bottomContentPadding = LocalContentBottomPadding.current + imeOffset
    val pageBackgroundColor = when (index) {
        2 -> colors.backgroundSecondary
        else -> colors.background
    }
    val animatedPageBackgroundColor by animateColorAsState(
        targetValue = pageBackgroundColor,
        // 页面背景色切换弹簧（阻尼比 0.9f，刚度 450f）
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 450f),
        label = "pageBgColor"
    )

    // 页面常驻会保留搜索输入框节点，切离搜索页时显式清除焦点以同步收起软键盘
    val focusManager = LocalFocusManager.current
    LaunchedEffect(index) {
        if (index != SEARCH_PAGE_INDEX) {
            focusManager.clearFocus(force = true)
        }
    }

    // 一级页面横向切换动画状态
    var displayedIndex by rememberSaveable { mutableIntStateOf(index) }
    var transitionState by remember { mutableStateOf<PageSlideTransition?>(null) }
    val slideProgress = remember { Animatable(0f) }

    LaunchedEffect(index) {
        if (index != displayedIndex) {
            val from = displayedIndex
            val to = index
            val dir = if (to > from) 1 else -1
            transitionState = PageSlideTransition(fromIndex = from, toIndex = to, direction = dir)
            slideProgress.snapTo(0f)
            try {
                slideProgress.animateTo(
                    targetValue = 1f,
                    // 一级页面横向切页弹簧参数（阻尼比 0.92f，刚度 340f）
                    animationSpec = spring(
                        dampingRatio = 0.92f,
                        stiffness = 340f
                    )
                )
            } finally {
                displayedIndex = to
                transitionState = null
                // 切页动画完成后退出切出页的歌单页和艺人页（滑动期间保持二级页随宿主一起滑出）
                when (from) {
                    0 -> homeViewModel.closePlaylistDetail()
                    1 -> radioViewModel.closePlaylistDetail()
                    2 -> mineViewModel.closePlaylistDetail()
                    3 -> searchViewModel.closePlaylistDetail()
                }
                onCloseArtist(from)
                onCloseAlbumOverlay(from)
            }
        }
    }

    // 已访问过的页面集合。用普通 Set 而非 snapshot 状态：只在组合期读写，不驱动额外重组
    val mountedPages = remember { mutableSetOf(index) }
    mountedPages.add(index)
    mountedPages.add(displayedIndex)
    transitionState?.let {
        mountedPages.add(it.fromIndex)
        mountedPages.add(it.toIndex)
    }

    val density = LocalDensity.current
    var containerWidthPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(animatedPageBackgroundColor)
            .onSizeChanged { containerWidthPx = it.width.toFloat() }
    ) {
        mountedPages.forEach { pageIndex ->
            val transition = transitionState
            val isTarget = pageIndex == (transition?.toIndex ?: displayedIndex)
            val isFrom = transition != null && pageIndex == transition.fromIndex
            val isVisible = isTarget || isFrom

            key(pageIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent { if (isVisible) drawContent() }
                        .offset {
                            IntOffset(0, if (isVisible) 0 else OFFSCREEN_PAGE_OFFSET_PX)
                        }
                        .zIndex(if (isTarget) 1f else 0f)
                        .graphicsLayer {
                            if (transition != null && isVisible) {
                                val width = if (containerWidthPx > 0f) {
                                    containerWidthPx
                                } else {
                                    with(density) { 360.dp.toPx() }
                                }
                                val progress = slideProgress.value
                                val dir = transition.direction
                                translationX = when {
                                    isTarget -> dir * width * (1f - progress)
                                    isFrom -> -dir * width * progress
                                    else -> 0f
                                }
                            } else {
                                translationX = 0f
                            }
                        }
                ) {
                    val pageArtist = selectedArtist?.takeIf { it.pageIndex == pageIndex }
                    val pageAlbum = albumOverlayState?.takeIf { it.pageIndex == pageIndex }
                    val isPageForeground = isHostForeground && displayedIndex == pageIndex && transitionState == null
                    val isPageContentVisible = isHostForeground && isVisible && pageArtist == null && pageAlbum == null
                    CompositionLocalProvider(LocalPageVisible provides isPageContentVisible) {
                    when (pageIndex) {
                        0 -> HomeRoute(
                            topContentPadding = topContentPadding,
                            bottomContentPadding = bottomContentPadding,
                            avatarUrl = avatarUrl,
                            displayName = displayName,
                            onAvatarClick = onAccountClick,
                            viewModel = homeViewModel,
                            lazyListState = homeListState,
                            isForeground = isPageForeground
                        )

                        1 -> RadioRoute(
                            topContentPadding = topContentPadding,
                            bottomContentPadding = bottomContentPadding,
                            avatarUrl = avatarUrl,
                            displayName = displayName,
                            onAvatarClick = onAccountClick,
                            onArtistClick = { artistId, artistName, artistCoverUrl ->
                                onArtistClick(1, artistId, artistName, artistCoverUrl)
                            },
                            viewModel = radioViewModel,
                            lazyListState = radioListState,
                            isForeground = isPageForeground
                        )

                        2 -> MineRoute(
                            topContentPadding = topContentPadding,
                            bottomContentPadding = bottomContentPadding,
                            onLoginClick = onAccountClick,
                            viewModel = mineViewModel,
                            lazyListState = mineListState,
                            isForeground = isPageForeground
                        )

                        3 -> SearchRoute(
                            viewModel = searchViewModel,
                            topContentPadding = topContentPadding + (if (LocalAdaptiveWindow.current.useNavigationRail) 60.dp else 0.dp),
                            bottomContentPadding = bottomContentPadding,
                            onArtistClick = { artistId, artistName, artistCoverUrl ->
                                onArtistClick(3, artistId, artistName, artistCoverUrl)
                            },
                            onTrackLikedChanged = mineViewModel::onTrackLikedChanged,
                            onTrackAddedToPlaylist = mineViewModel::onTrackAddedToPlaylist,
                            isForeground = isPageForeground,
                            topHeader = {
                                SearchPageTopHeader(
                                    avatarUrl = avatarUrl,
                                    displayName = displayName,
                                    onAvatarClick = onSearchAvatarClick
                                )
                            }
                        )

                        else -> AppPageBackground()
                    }
                    }

                    val lastArtist = remember { mutableStateOf<SelectedArtist?>(null) }
                    val artistOpenCounter = remember { mutableIntStateOf(0) }
                    LaunchedEffect(pageArtist) {
                        if (pageArtist != null) {
                            lastArtist.value = pageArtist
                            artistOpenCounter.intValue++
                        }
                    }
                    LaunchedEffect(isPageForeground) {
                        if (!isPageForeground && pageArtist == null) {
                            lastArtist.value = null
                        }
                    }

                    AnimatedVisibility(
                        visible = pageArtist != null,
                        enter = slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth },
                            // 艺人详情页滑入弹簧参数（阻尼比 0.92f，刚度 260f）
                            animationSpec = spring(dampingRatio = 0.92f, stiffness = 260f)
                        ),
                        exit = if (isPageForeground) {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                // 艺人详情页滑出弹簧参数（阻尼比 0.92f，刚度 300f）
                                animationSpec = spring(dampingRatio = 0.92f, stiffness = 300f)
                            )
                        } else {
                            ExitTransition.None
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(25f)
                    ) {
                        val displayArtist = pageArtist ?: lastArtist.value
                        displayArtist?.let { artist ->
                            key(artistOpenCounter.intValue) {
                                ArtistDetailRoute(
                                    artistId = artist.id,
                                    artistName = artist.name,
                                    artistCoverUrl = artist.coverUrl,
                                    onClose = { onCloseArtist(pageIndex) },
                                    backHandlerEnabled = isPageForeground && pageArtist != null && pageAlbum == null
                                )
                            }
                        }
                    }

                    val lastAlbum = remember { mutableStateOf<GlobalAlbumOverlayState?>(null) }
                    val lastOpenedAlbumId = remember { mutableStateOf<Long?>(null) }
                    val albumOpenCounter = remember { mutableIntStateOf(0) }
                    val fallbackAlbumBackdrop = rememberLayerBackdrop { drawContent() }
                    LaunchedEffect(pageAlbum) {
                        if (pageAlbum != null) {
                            if (lastOpenedAlbumId.value != pageAlbum.detail.id) {
                                albumOpenCounter.intValue++
                                lastOpenedAlbumId.value = pageAlbum.detail.id
                            }
                            lastAlbum.value = pageAlbum
                        } else {
                            lastOpenedAlbumId.value = null
                        }
                    }
                    LaunchedEffect(isPageForeground) {
                        if (!isPageForeground && pageAlbum == null) {
                            lastAlbum.value = null
                        }
                    }
                    val albumDetailListState = rememberSaveable(
                        pageAlbum?.detail?.id,
                        saver = LazyListState.Saver
                    ) {
                        LazyListState()
                    }

                    AnimatedVisibility(
                        visible = pageAlbum != null,
                        enter = slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth },
                            // 专辑详情页滑入弹簧参数（阻尼比 0.92f，刚度 260f）
                            animationSpec = spring(dampingRatio = 0.92f, stiffness = 260f)
                        ),
                        exit = if (isPageForeground) {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                // 专辑详情页滑出弹簧参数（阻尼比 0.92f，刚度 300f）
                                animationSpec = spring(dampingRatio = 0.92f, stiffness = 300f)
                            )
                        } else {
                            ExitTransition.None
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(30f)
                    ) {
                        val displayAlbum = pageAlbum ?: lastAlbum.value
                        displayAlbum?.let { albumState ->
                            key(albumOpenCounter.intValue) {
                                val glassBackdrop = LocalGlassBackdrop.current ?: fallbackAlbumBackdrop
                                PlaylistDetailScreen(
                                    detail = albumState.detail,
                                    backdrop = glassBackdrop,
                                    onClose = { onCloseAlbumOverlay(pageIndex) },
                                    onPlayTrack = { track ->
                                        val idx = albumState.detail.tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                                        onPlayAlbumTracks(albumState.detail.tracks, idx)
                                    },
                                    onPlayAll = { onPlayAlbumTracks(albumState.detail.tracks, 0) },
                                    onShufflePlay = { onPlayAlbumTracks(albumState.detail.tracks.shuffled(), 0) },
                                    onTrackLikedChanged = mineViewModel::onTrackLikedChanged,
                                    onTrackAddedToPlaylist = mineViewModel::onTrackAddedToPlaylist,
                                    isLoading = pageAlbum?.isLoading ?: false,
                                    listState = albumDetailListState,
                                    filterByCurrentUserLikes = false,
                                    defaultStyle = PlaylistDetailStyle.APPLE_MUSIC,
                                    isAlbum = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 隐藏页被挪出的距离（px）。取远大于任何屏幕高度的值，
 * 保证其整体落在父容器之外，从而不参与命中测试。
 */
private const val OFFSCREEN_PAGE_OFFSET_PX = 100_000

/**
 * 搜索页大标题顶栏，位于搜索页内部随页面一起横向滑动，并处于二级页面（如艺人详情）下层。
 */
@Composable
private fun SearchPageTopHeader(
    avatarUrl: String?,
    displayName: String?,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        LargePageTitle(
            title = "搜索",
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 20.dp, top = 14.dp)
        )

        UserAvatarButton(
            avatarUrl = avatarUrl,
            displayName = displayName,
            onClick = onAvatarClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(end = 20.dp, top = 14.dp)
        )
    }
}

/** 账号与登录全屏抽屉浮层（支持背景模糊遮罩与下拉手势关闭）。 */
@Composable
private fun AccountFullScreenOverlay(
    visible: Boolean,
    backdrop: Backdrop,
    onDismissRequest: () -> Unit,
    onDismissed: () -> Unit,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scrimInteractionSource = remember { MutableInteractionSource() }
    BackHandler(enabled = visible, onBack = onDismissRequest)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val hostHeightPx = with(density) { maxHeight.toPx() }
        var measuredPanelHeightPx by remember { mutableFloatStateOf(0f) }
        val panelHeightPx = measuredPanelHeightPx.takeIf { it > 0f } ?: (hostHeightPx * 0.95f) // 抽屉默认占屏幕高度比例 95%
        val hiddenOffsetPx = panelHeightPx + with(density) { 24.dp.toPx() } // 隐藏时额外下移 24.dp 以露出阴影完全移出屏幕

        val panelOffsetFraction = remember { Animatable(1f) }
        var dragOffsetPx by remember { mutableFloatStateOf(0f) }
        var isDragging by remember { mutableStateOf(false) }

        val settledDragOffsetPx by animateFloatAsState(
            targetValue = if (isDragging || !visible) dragOffsetPx else 0f,
            // 抽屉松手回弹弹簧参数
            animationSpec = spring(stiffness = 550f, dampingRatio = 0.84f),
            label = "accountPanelDragOffset"
        )
        val panelDragState = rememberDraggableState { delta ->
            if (delta > 0f || dragOffsetPx > 0f) {
                dragOffsetPx = (dragOffsetPx + delta).coerceIn(0f, hiddenOffsetPx)
            }
        }

        LaunchedEffect(visible) {
            if (visible) {
                dragOffsetPx = 0f
                panelOffsetFraction.animateTo(
                    targetValue = 0f,
                    // 抽屉打开弹簧（阻尼比 0.84f，刚度 550f）
                    animationSpec = spring(dampingRatio = 0.84f, stiffness = 550f)
                )
            } else {
                panelOffsetFraction.animateTo(
                    targetValue = 1f,
                    // 抽屉关闭弹簧（阻尼比 0.88f，刚度 500f）
                    animationSpec = spring(dampingRatio = 0.88f, stiffness = 500f)
                )
                onDismissed()
            }
        }

        val isDark = AppTheme.isDark
        val colors = AppTheme.colors
        val centeredPanel = LocalAdaptiveWindow.current.width >= 600.dp || LocalAdaptiveWindow.current.separatingFold != null
        val panelShape = if (centeredPanel) RoundedRectangle(30.dp) else RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)

        // 遮罩层：模糊 + 压暗只在绘制阶段计算，Animatable 每帧只让 RenderNode 失效，不触发重组。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(0.dp) },
                    effects = {
                        blur(8f.dp.toPx()) // 遮罩背景模糊半径（dp）
                        vibrancy()
                    },
                    onDrawSurface = {
                        // 深浅色模式遮罩压暗透明度
                        drawRect(Color.Black.copy(alpha = if (isDark) 0.56f else 0.32f))
                    }
                )
                .graphicsLayer {
                    // 下拉拖动时遮罩同步变淡：状态全部在 draw 阶段读取
                    val openProgress = (1f - panelOffsetFraction.value).coerceIn(0f, 1f)
                    val effectiveDragOffset = if (isDragging) dragOffsetPx else settledDragOffsetPx
                    val dragProgress = (effectiveDragOffset / panelHeightPx).coerceIn(0f, 1f)
                    alpha = (openProgress * (1f - dragProgress * 0.35f)).coerceIn(0f, 1f)
                }
                .clickable(
                    enabled = visible,
                    interactionSource = scrimInteractionSource,
                    indication = null,
                    onClick = onDismissRequest
                )
        )

        AdaptiveOverlayPane(Modifier.fillMaxSize()) {
        // 主抽屉卡片：位移与缩放全部在 draw 阶段读取状态，既不触发重组也不触发布局。
        Box(
            modifier = Modifier
                .align(if (centeredPanel) Alignment.Center else Alignment.BottomCenter)
                .widthIn(max = if (centeredPanel) 560.dp else Dp.Unspecified) // 宽屏居中卡片最大宽度 560.dp
                .fillMaxWidth()
                .fillMaxHeight(if (centeredPanel) 0.88f else 0.95f).testTag("account_glass_surface")
                .onGloballyPositioned { measuredPanelHeightPx = it.size.height.toFloat() }
                .graphicsLayer {
                    // 抽屉整体位移（收起 + 下拉拖动）统一走 RenderNode 的 translationY
                    val offsetFraction = panelOffsetFraction.value
                    val effectiveDragOffset = if (isDragging) dragOffsetPx else settledDragOffsetPx
                    translationY = offsetFraction * hiddenOffsetPx + effectiveDragOffset

                    val dragProgress = (effectiveDragOffset / panelHeightPx).coerceIn(0f, 1f)
                    scaleX = 1f - dragProgress * 0.018f
                    scaleY = 1f - dragProgress * 0.03f
                }
                .shadow(
                    elevation = 28.dp,
                    shape = panelShape,
                    clip = false
                )
                .then(if (centeredPanel) Modifier.liquidGlassPanel(backdrop, colors.groupedBackground, 30.dp, 0.dp)
                    else Modifier.clip(panelShape).background(colors.groupedBackground))
        ) {
            AccountLoginSheetContent(
                onDismiss = onDismissRequest,
                backdrop = backdrop,
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(top = 6.dp),
                viewModel = viewModel
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp)
                    .size(width = 76.dp, height = 28.dp)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = panelDragState,
                        enabled = visible,
                        onDragStarted = { isDragging = true },
                        onDragStopped = { velocity ->
                            isDragging = false
                            // 下拉关闭阈值：位移超过抽屉高度 16% 或向下甩动速度超过 1100.dp/s
                            val dismissByDistance = dragOffsetPx > panelHeightPx * 0.16f
                            val dismissByVelocity = velocity > with(density) { 1100.dp.toPx() }
                            if (dismissByDistance || dismissByVelocity) {
                                onDismissRequest()
                            } else {
                                dragOffsetPx = 0f
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(colors.handleBar)
                )
            }
        }
    }
    }
}

@Composable
private fun LargePageTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        fontSize = 34.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.8).sp,
        fontWeight = FontWeight.Bold,
        color = AppTheme.colors.textPrimary,
        modifier = modifier
    )
}

/**
 * 顶部/底部搜索输入框内容。
 *
 * 搜索状态在这里**独立订阅**，而不是在 [AppNavigation] 里读取：
 * `SearchViewModel.uiState` 每输入一个字符就会变化，若在外壳作用域读取，
 * 整个导航外壳（玻璃 Backdrop、导航项、各覆盖层）都会随每次按键一起重组。
 */
@Composable
private fun SearchQueryInputContent(
    viewModel: SearchViewModel,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val colors = AppTheme.colors

    val searchTextStyle = MiuixTheme.textStyles.body1.copy(
        color = colors.textPrimary,
        fontSize = 15.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both
        )
    )

    val performSearch = {
        keyboardController?.hide()
        focusManager.clearFocus()
        viewModel.onSearchSubmit()
    }

    BasicTextField(
        value = uiState.query,
        onValueChange = { viewModel.onQueryChanged(it) },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { performSearch() }),
        cursorBrush = SolidColor(colors.accent),
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .onKeyEvent { keyEvent ->
                if (keyEvent.key == Key.Enter) {
                    performSearch()
                    true
                } else {
                    false
                }
            },
        singleLine = true,
        textStyle = searchTextStyle,
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (uiState.query.isEmpty()) {
                        Text(
                            text = "搜你想听的",
                            color = colors.textSecondary,
                            style = searchTextStyle.copy(color = colors.textSecondary),
                            maxLines = 1
                        )
                    }
                    innerTextField()
                }
                if (uiState.query.isNotEmpty()) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_close),
                        contentDescription = "清空搜索",
                        tint = colors.textSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { viewModel.onClearQuery() }
                    )
                }
            }
        }
    )
}

@Composable
private fun NowPlayingTransitionLayer(
    playerViewModel: PlayerViewModel,
    progress: State<Float>,
    artworkUrl: String?,
    sourceContainerBounds: Rect?,
    sourceArtworkBounds: Rect?,
    targetArtworkBounds: Rect?,
    targetArtworkCorner: Dp,
    targetArtworkShadow: Dp,
    targetArtworkTrackTop: Boolean,
    onHostBoundsChanged: (Rect) -> Unit,
    onTargetArtworkBoundsChanged: (Rect) -> Unit,
    onTargetArtworkCornerChanged: (Dp) -> Unit,
    onTargetArtworkShadowChanged: (Dp) -> Unit,
    onTargetArtworkTrackTopChanged: (Boolean) -> Unit,
    onGrabberDragStart: () -> Unit,
    onGrabberDragDelta: (Float) -> Unit,
    onGrabberDragEnd: (Float) -> Unit,
    onClose: () -> Unit
) {
    var hostBounds by remember { mutableStateOf(Rect.Zero) }
    val screenCornerPx = rememberPlayerScreenCornerRadiusPx(hostBounds)

    val canShareArtwork = remember(sourceArtworkBounds, targetArtworkBounds) {
        sourceArtworkBounds?.isUsable() == true && targetArtworkBounds?.isUsable() == true
    }

    val backgroundAlpha = remember(progress) {
        derivedStateOf { playerBackgroundAlpha(progress.value) }
    }
    val contentAlpha = remember(progress) {
        derivedStateOf { playerContentAlpha(progress.value) }
    }
    val targetArtworkAlpha = remember(progress, canShareArtwork) {
        derivedStateOf {
            if (!canShareArtwork) {
                progress.value.coerceIn(0f, 1f)
            } else if (progress.value >= 0.998f) {
                1f
            } else {
                0f
            }
        }
    }
    val enableLightStatusBar by remember(progress) {
        derivedStateOf { progress.value > 0.55f }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f)
            .onGloballyPositioned { coords ->
                val bounds = coords.boundsInRoot()
                if (hostBounds != bounds) {
                    hostBounds = bounds
                    onHostBoundsChanged(bounds)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = progress.value.coerceIn(0f, 1f)
                    val srcContainer = sourceContainerBounds
                    if (p < 0.999f && srcContainer?.isUsable() == true && hostBounds.isUsable()) {
                        val currentContainer = playerContainerRect(srcContainer, hostBounds, p)
                            .translate(-hostBounds.topLeft)
                        val radius = playerContainerCornerRadius(
                            source = srcContainer.height * 0.5f,
                            target = screenCornerPx,
                            progress = p
                        )
                        shape = object : Shape {
                            override fun createOutline(
                                size: Size,
                                layoutDirection: LayoutDirection,
                                density: Density
                            ): Outline = Outline.Rounded(
                                RoundRect(currentContainer, CornerRadius(radius, radius))
                            )
                        }
                        clip = true
                    } else {
                        clip = false
                    }
                }
        ) {
            NowPlayingScreen(
                playerViewModel = playerViewModel,
                onClose = onClose,
                artworkAlpha = targetArtworkAlpha,
                onArtworkBoundsChanged = onTargetArtworkBoundsChanged,
                enableLightStatusBarSwitch = enableLightStatusBar,
                contentAlpha = contentAlpha,
                backgroundAlpha = backgroundAlpha,
                onArtworkCornerRadiusChanged = onTargetArtworkCornerChanged,
                onArtworkShadowChanged = onTargetArtworkShadowChanged,
                onArtworkTrackTopChanged = onTargetArtworkTrackTopChanged,
                onGrabberDragStart = onGrabberDragStart,
                onGrabberDragDelta = onGrabberDragDelta,
                onGrabberDragEnd = onGrabberDragEnd
            )
        }

        if (canShareArtwork && sourceArtworkBounds != null && targetArtworkBounds != null) {
            MovingPlayerArtwork(
                imageUrl = artworkUrl,
                sourceBounds = sourceArtworkBounds,
                targetBounds = targetArtworkBounds,
                sourceContainerBounds = sourceContainerBounds,
                hostBounds = hostBounds,
                targetCorner = targetArtworkCorner,
                targetShadow = targetArtworkShadow,
                trackContainerTop = targetArtworkTrackTop,
                progress = progress,
                visible = targetArtworkAlpha.value < 0.5f
            )
        }
    }
}

@Composable
private fun MovingPlayerArtwork(
    imageUrl: String?,
    sourceBounds: Rect,
    targetBounds: Rect,
    sourceContainerBounds: Rect?,
    hostBounds: Rect,
    targetCorner: Dp,
    targetShadow: Dp,
    trackContainerTop: Boolean,
    progress: State<Float>,
    visible: Boolean
) {
    if (!visible || !sourceBounds.isUsable() || !targetBounds.isUsable()) {
        return
    }

    val density = LocalDensity.current
    val sharedArtwork = LocalPlayerArtwork.current
    val baseWidthDp = with(density) { targetBounds.width.coerceAtLeast(1f).toDp() }
    val baseHeightDp = with(density) { targetBounds.height.coerceAtLeast(1f).toDp() }
    val sourceCornerPx = with(density) { 7.dp.toPx() } // 迷你条封面初始圆角（与 MiniPlayerArtwork 对齐）
    val targetCornerPx = with(density) { targetCorner.toPx() }
    val targetShadowPx = with(density) { targetShadow.toPx() }

    Box(
        modifier = Modifier
            .size(baseWidthDp, baseHeightDp)
            .graphicsLayer {
                val p = progress.value.coerceIn(0f, 1f)
                val worldRect = if (trackContainerTop && sourceContainerBounds?.isUsable() == true && hostBounds.isUsable()) {
                    playerTopAnchoredArtworkRect(
                        source = sourceBounds,
                        target = targetBounds,
                        sourceContainer = sourceContainerBounds,
                        targetContainer = hostBounds,
                        progress = p
                    )
                } else {
                    playerArtworkRect(
                        source = sourceBounds,
                        target = targetBounds,
                        progress = p
                    )
                }
                val localRect = if (hostBounds.isUsable()) {
                    worldRect.translate(-hostBounds.topLeft)
                } else {
                    worldRect
                }

                val currentScaleX = (localRect.width / size.width.coerceAtLeast(1f)).coerceAtLeast(0.001f)
                val currentScaleY = (localRect.height / size.height.coerceAtLeast(1f)).coerceAtLeast(0.001f)

                transformOrigin = TransformOrigin(0f, 0f)
                translationX = localRect.left
                translationY = localRect.top
                scaleX = currentScaleX
                scaleY = currentScaleY
                alpha = 1f

                val visualCornerPx = mix(sourceCornerPx, targetCornerPx, p)
                val localCornerPx = visualCornerPx / minOf(currentScaleX, currentScaleY)
                shape = RoundedRectangle(with(density) { localCornerPx.toDp() })
                clip = true

                shadowElevation = (targetShadowPx * p) / currentScaleX
                // 展开过渡期间封面的环境光与主投影透明度
                ambientShadowColor = Color.Black.copy(alpha = 0.65f)
                spotShadowColor = Color.Black.copy(alpha = 0.80f)
            }
            .background(Color(0xFF282830)),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            val normalizedUrl = remember(imageUrl) { normalizeHighResCoverUrl(imageUrl) }
            val painter = sharedArtwork?.takeIf { it.first == normalizedUrl }?.second
            if (painter != null) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = rememberHighResArtworkRequest(imageUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.High,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_mini_play),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private data class MiniPlayerDisplayState(
    val currentTrack: Track?,
    val status: PlayerStatus,
    val canSwitch: Boolean
)

/** 底部悬浮液态玻璃迷你播放条：支持左右滑动切歌、点击展开全屏播放页及紧凑态收缩动画。 */
@Composable
internal fun SearchMiniPlayerBar(
    backdrop: Backdrop,
    cornerRadius: Dp,
    playbackStateFlow: StateFlow<PlaybackState>,
    contentProgress: State<Float> = remember { mutableFloatStateOf(0f) },
    playerTransitionProgress: State<Float> = remember { mutableFloatStateOf(0f) },
    enabled: Boolean = true,
    onPlayPauseClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onBarClick: (String?) -> Unit,
    onContainerBoundsChanged: (Rect) -> Unit = {},
    onArtworkBoundsChanged: (Rect) -> Unit,
    hideArtwork: Boolean,
    modifier: Modifier = Modifier
) {
    // 仅订阅曲目、播放状态与切歌能力，屏蔽每 500ms 一次的 currentPositionMs 推送，
    // 避免整个液态玻璃迷你播放条在播放期间持续重组。
    val displayState by remember(playbackStateFlow) {
        playbackStateFlow
            .map { state ->
                MiniPlayerDisplayState(
                    currentTrack = state.currentTrack,
                    status = state.status,
                    canSwitch = state.currentTrack != null && state.queueTracks.size > 1
                )
            }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(
        initialValue = playbackStateFlow.value.let { state ->
            MiniPlayerDisplayState(
                currentTrack = state.currentTrack,
                status = state.status,
                canSwitch = state.currentTrack != null && state.queueTracks.size > 1
            )
        }
    )
    val currentTrack = displayState.currentTrack
    val hasTrack = currentTrack != null
    val isPlaying = displayState.status == PlayerStatus.PLAYING
    val isBuffering = displayState.status == PlayerStatus.BUFFERING

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // 当前展示的曲目信息（滑动切歌动画期间保持旧歌曲滑出，新歌曲就绪后滑入）
    var displayedTrack by remember { mutableStateOf(currentTrack) }
    var isTransitioning by remember { mutableStateOf(false) }
    var transitionJob by remember { mutableStateOf<Job?>(null) }

    // 左右滑动手势状态：实时位移与动画控制器
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val animOffsetX = remember { Animatable(0f) }
    val animAlpha = remember { Animatable(1f) }

    // 是否允许切歌（有曲目且待播列表大于 1 首）
    val canSwitch = displayState.canSwitch

    // 外部切歌或播放列表切换时，自动同步 displayedTrack
    LaunchedEffect(currentTrack?.id) {
        val newTrack = currentTrack
        if (!isTransitioning && !isDragging && displayedTrack?.id != newTrack?.id) {
            if (displayedTrack == null || newTrack == null) {
                displayedTrack = newTrack
                animOffsetX.snapTo(0f)
                animAlpha.snapTo(1f)
            } else {
                animAlpha.animateTo(0f, tween(durationMillis = 80))
                displayedTrack = newTrack
                animAlpha.animateTo(1f, tween(durationMillis = 120))
            }
        }
    }

    val currentArtworkUrl = displayedTrack?.coverUrl
        ?: displayedTrack?.album?.coverUrl
    val title = displayedTrack?.title ?: "未在播放"

    val swipeThreshold = with(density) { 36.dp.toPx() } // 左右滑动触发切歌的距离阈值
    val exitDistance = with(density) { 220.dp.toPx() } // 旧歌曲滑出与新歌曲滑入的水平位移距离

    val handleDragRelease: (Float) -> Unit = { finalDrag ->
        transitionJob?.cancel()
        transitionJob = coroutineScope.launch {
            val isNext = finalDrag < -swipeThreshold
            val isPrev = finalDrag > swipeThreshold

            if (canSwitch && (isNext || isPrev)) {
                isTransitioning = true
                val targetExit = if (isNext) -exitDistance else exitDistance
                val targetEnter = if (isNext) exitDistance else -exitDistance

                // 1. 同步当前拖拽起始点到动画控制器
                animOffsetX.snapTo(finalDrag)
                val releaseAlpha = (1f - (abs(finalDrag) / exitDistance) * 0.35f).coerceIn(0.65f, 1f)
                animAlpha.snapTo(releaseAlpha)

                // 2. 仅在 animOffsetX 与 animAlpha 对齐 finalDrag 后再解除 isDragging 并清零 dragOffsetX，保证衔接帧连续无闪烁
                isDragging = false
                dragOffsetX = 0f

                // 3. 旧歌曲（封面+歌名）朝滑动方向加速滑出并淡出
                val exitMove = launch {
                    animOffsetX.animateTo(
                        targetValue = targetExit,
                        animationSpec = tween(durationMillis = 140, easing = FastOutLinearInEasing)
                    )
                }
                val exitFade = launch {
                    animAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 120, easing = LinearEasing)
                    )
                }
                joinAll(exitMove, exitFade)

                val oldTrackId = displayedTrack?.id

                // 4. 瞬移至对侧入口等待切歌，此时 alpha 保持为 0f 保持不可见
                animOffsetX.snapTo(targetEnter)
                animAlpha.snapTo(0f)

                // 5. 触发切歌
                if (isNext) {
                    onNextClick()
                } else {
                    onPreviousClick()
                }

                // 6. 等待新曲目状态推送的超时上限（600ms），超时后走原地渐入兜底
                val newTrack = withTimeoutOrNull(600) {
                    playbackStateFlow
                        .map { it.currentTrack }
                        .filter { it != null && it.id != oldTrackId }
                        .first()
                }

                if (newTrack != null) {
                    // 在透明状态下切换曲目后再滑入
                    displayedTrack = newTrack

                    // 7. 新曲目从对侧弹簧滑入并淡入复位（阻尼比 0.82f，刚度 450f）
                    val enterMove = launch {
                        animOffsetX.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = 0.82f,
                                stiffness = 450f
                            )
                        )
                    }
                    val enterFade = launch {
                        animAlpha.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing)
                        )
                    }
                    joinAll(enterMove, enterFade)
                } else {
                    // 超时兜底：在原位渐入当前曲目，避免旧曲目再次在对侧滑入
                    displayedTrack = playbackStateFlow.value.currentTrack
                    animOffsetX.snapTo(0f)
                    animAlpha.animateTo(1f, tween(durationMillis = 150))
                }

                isTransitioning = false
            } else {
                // 未达阈值或单曲队列，弹簧回弹复位
                animOffsetX.snapTo(finalDrag)
                val releaseAlpha = (1f - (abs(finalDrag) / exitDistance) * 0.35f).coerceIn(0.65f, 1f)
                animAlpha.snapTo(releaseAlpha)
                isDragging = false
                dragOffsetX = 0f

                val reboundMove = launch {
                    animOffsetX.animateTo(
                        targetValue = 0f,
                        // 未达切歌阈值时的回弹弹簧参数
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                val reboundFade = launch {
                    animAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                    )
                }
                joinAll(reboundMove, reboundFade)
            }
        }
    }

    val isDark = AppTheme.isDark
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth().testTag("bottom_mini_player_surface")
            .onGloballyPositioned { coordinates ->
                onContainerBoundsChanged(coordinates.boundsInRoot())
            }
            .liquidGlass(
                backdrop = backdrop,
                cornerRadius = cornerRadius,
                surfaceAlpha = GlassDefaults.SurfaceAlphaBright,
                isDarkSurface = isDark,
                enableNoiseDither = true
            )
            .interactiveGlassHighlight(
                shape = RoundedRectangle(cornerRadius),
                enabled = hasTrack && !isTransitioning && enabled,
                isDarkSurface = isDark
            )
            // 使用无 indication 的点击，避免按压叠加默认灰色水波纹
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = hasTrack && !isTransitioning && enabled
            ) {
                onBarClick(currentArtworkUrl)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = playerMiniContentAlpha(playerTransitionProgress.value)
                }
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧可滑动区域：封面与歌名同时随手势平移
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clipToBounds()
                    .padding(start = 12.dp)
                    .pointerInput(hasTrack, canSwitch, enabled) {
                        if (!hasTrack || !enabled) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            // 立即消费 Down 事件，拦截底层列表按压与水波纹透传
                            down.consume()
                            var totalDragX = 0f
                            var isDrag = false
                            var gestureEnded = false
                            val touchSlop = viewConfiguration.touchSlop
                            val pointerId = down.id

                            // 手势启动时清零 dragOffsetX，避免残留上次拖拽位移
                            dragOffsetX = 0f

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                if (change == null) break

                                if (!change.pressed) {
                                    change.consume()
                                    gestureEnded = true
                                    if (isDrag) {
                                        val releaseDrag = totalDragX
                                        handleDragRelease(releaseDrag)
                                    } else {
                                        isDragging = false
                                        dragOffsetX = 0f
                                        onBarClick(currentArtworkUrl)
                                    }
                                    break
                                }

                                val deltaX = change.position.x - down.position.x
                                val deltaY = change.position.y - down.position.y

                                if (!isDrag) {
                                    if (abs(deltaX) > touchSlop && abs(deltaX) > abs(deltaY)) {
                                        isDrag = true
                                        transitionJob?.cancel()
                                        transitionJob = null
                                        isTransitioning = false
                                        // 首次进入拖拽态时同步写入 dragOffsetX 后再置位 isDragging，避免首帧位移跳变
                                        val effectiveDeltaX = if (canSwitch) deltaX else deltaX * 0.25f // 无法切歌时阻尼系数 0.25f
                                        totalDragX = effectiveDeltaX
                                        dragOffsetX = totalDragX
                                        isDragging = true
                                        change.consume()
                                    } else if (abs(deltaY) > touchSlop) {
                                        break
                                    }
                                } else {
                                    val effectiveDeltaX = if (canSwitch) deltaX else deltaX * 0.25f
                                    totalDragX = effectiveDeltaX
                                    dragOffsetX = totalDragX
                                    change.consume()
                                }
                            }

                            if (!gestureEnded && isDrag) {
                                handleDragRelease(totalDragX)
                            }
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = if (isDragging) dragOffsetX else animOffsetX.value
                            alpha = if (isDragging) {
                                (1f - (abs(dragOffsetX) / exitDistance) * 0.35f).coerceIn(0.65f, 1f)
                            } else {
                                animAlpha.value
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniPlayerArtwork(
                        imageUrl = currentArtworkUrl,
                        modifier = Modifier
                            .layout { measurable, _ ->
                                // 紧凑态收缩期间封面尺寸从 34.dp 缩至 30.dp
                                val sizePx = lerpDp(34.dp, 30.dp, contentProgress.value)
                                    .roundToPx()
                                    .coerceAtLeast(1)
                                val placeable = measurable.measure(Constraints.fixed(sizePx, sizePx))
                                layout(sizePx, sizePx) { placeable.place(0, 0) }
                            }
                            .graphicsLayer { alpha = if (hideArtwork) 0f else 1f },
                        onBoundsChanged = { bounds ->
                            if (!isDragging && !isTransitioning) {
                                onArtworkBoundsChanged(bounds)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val artistText = displayedTrack?.artists?.joinToString(", ") { it.name }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            color = colors.textPrimary,
                            style = MiuixTheme.textStyles.body2.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                lineHeight = 17.sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!artistText.isNullOrEmpty()) {
                            Text(
                                text = artistText,
                                color = colors.textSecondary,
                                style = MiuixTheme.textStyles.footnote2.copy(fontSize = 10.5.sp, lineHeight = 14.sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 播放列表图标（在展开状态展示，紧凑状态随 contentProgress 在布局/绘制阶段缩小并淡出，避免外层 SearchMiniPlayerBar 重组）
            Box(
                modifier = Modifier
                    .layout { measurable, _ ->
                        val visibility = (1f - contentProgress.value).coerceIn(0f, 1f)
                        val iconBoxSizePx = 28.dp.roundToPx() // 右侧操作按钮槽位尺寸
                        val boxWidthPx = if (visibility > 0.001f) (28.dp * visibility).roundToPx() else 0
                        val placeable = measurable.measure(Constraints.fixed(boxWidthPx, iconBoxSizePx))
                        layout(boxWidthPx, iconBoxSizePx) {
                            if (visibility > 0.001f) {
                                placeable.placeRelative(0, 0)
                            }
                        }
                    }
                    .drawWithContent {
                        if ((1f - contentProgress.value) > 0.001f) {
                            drawContent()
                        }
                    }
                    .graphicsLayer {
                        val visibility = (1f - contentProgress.value).coerceIn(0f, 1f)
                        alpha = visibility
                        scaleX = 0.8f + 0.2f * visibility
                        scaleY = 0.8f + 0.2f * visibility
                        clip = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(28.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = enabled
                        ) {
                            if ((1f - contentProgress.value) > 0.001f) {
                                onPlaylistClick()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_mini_playlist),
                        contentDescription = "播放列表",
                        tint = if (hasTrack) colors.iconPrimary else colors.textTertiary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Spacer(
                modifier = Modifier.layout { _, _ ->
                    val p = contentProgress.value.coerceIn(0f, 1f)
                    val visibility = (1f - p).coerceIn(0f, 1f)
                    val spacePx = if (visibility > 0.001f) {
                        lerpDp(8.dp, 0.dp, p).roundToPx()
                    } else {
                        2.dp.roundToPx()
                    }
                    layout(spacePx, 0) {}
                }
            )

            // 播放暂停按钮：外圈包裹播放进度圆环
            MiniPlayerProgressPlayButton(
                playbackStateFlow = playbackStateFlow,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                hasTrack = hasTrack && enabled,
                isProgressActive = hasTrack && enabled,
                onClick = onPlayPauseClick
            )
        }
    }
}

/**
 * 迷你播放条右侧的播放/暂停按钮：
 * 外层包裹播放进度指示圆环，在独立的 graphicsLayer Canvas 绘制阶段读取进度，
 * 避免连带外层 SearchMiniPlayerBar 及 liquidGlass(backdrop) 逐帧重组与重录。
 */
@Composable
private fun MiniPlayerProgressPlayButton(
    playbackStateFlow: StateFlow<PlaybackState>,
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    hasTrack: Boolean,
    isProgressActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = AppTheme.isDark
    val colors = AppTheme.colors
    val ringColor = if (hasTrack) colors.iconPrimary else colors.textTertiary
    val trackColor = if (isDark) {
        if (hasTrack) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.08f)
    } else {
        if (hasTrack) Color(0x1F000000) else Color(0x0F000000)
    }
    val strokeWidth = 1.6.dp // 外圈进度条线宽

    val targetProgress by remember(playbackStateFlow, isProgressActive) {
        if (!isProgressActive) {
            flowOf(0f)
        } else {
            playbackStateFlow
                .map { state ->
                    if (state.durationMs > 0) {
                        (state.currentPositionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                }
                .distinctUntilChanged()
        }
    }.collectAsStateWithLifecycle(
        initialValue = if (isProgressActive) {
            playbackStateFlow.value.let { state ->
                if (state.durationMs > 0) {
                    (state.currentPositionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f
            }
        } else 0f
    )
    // 28dp 圆环正常播放每 500ms 仅推进约 0.5px：常规推进直接 snapTo，
    // 仅在切歌或拖动跳变（差值 > 2.5%）时启用弹簧过渡，减少静止播放期间 RenderThread 空耗
    val progressAnim = remember { Animatable(targetProgress) }
    LaunchedEffect(targetProgress, isPlaying) {
        val delta = abs(targetProgress - progressAnim.value)
        if (isPlaying && targetProgress > 0f && delta <= 0.025f) {
            progressAnim.snapTo(targetProgress)
        } else {
            progressAnim.animateTo(
                targetValue = targetProgress,
                animationSpec = spring(stiffness = 500f)
            )
        }
    }

    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = hasTrack,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // 独立 28dp RenderNode：圆环更新只重绘此小图层，不脏化外层液态玻璃 Backdrop
                .graphicsLayer {}
        ) {
            val progress = progressAnim.value
            val strokePx = strokeWidth.toPx()
            val radius = (size.minDimension - strokePx) / 2f
            val centerOffset = Offset(size.width / 2f, size.height / 2f)

            // 1. 底层浅色完整圆环轨道
            drawCircle(
                color = trackColor,
                radius = radius,
                center = centerOffset,
                style = Stroke(width = strokePx)
            )

            // 2. 顶层根据播放进度推进的圆弧（从顶部 12 点钟方向顺时针旋转，末端带圆角）
            if (progress > 0f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        // 3. 居中播放/暂停/缓冲图标
        if (isBuffering) {
            AppLoadingIndicator(
                modifier = Modifier.size(14.dp),
                color = ringColor,
                strokeWidth = 1.8.dp,
                size = 14.dp
            )
        } else {
            Icon(
                painter = painterResource(
                    id = if (isPlaying) R.drawable.ic_mini_pause else R.drawable.ic_mini_play
                ),
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = ringColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun MiniPlayerArtwork(
    imageUrl: String?,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedRectangle(7.dp)
    val sharedArtwork = LocalPlayerArtwork.current
    val isDark = AppTheme.isDark
    val colors = AppTheme.colors

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                onBoundsChanged(coordinates.boundsInRoot())
            }
            .clip(shape)
            .background(colors.placeholder),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            val normalizedUrl = remember(imageUrl) { normalizeHighResCoverUrl(imageUrl) }
            val painter = sharedArtwork?.takeIf { it.first == normalizedUrl }?.second
            if (painter != null) {
                Image(
                    painter = painter,
                    contentDescription = "当前歌曲封面",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 迷你条显示尺寸为 30~34dp：未打开全屏播放页时拉取 128px 缩略图并限制 256px 解码上限，降低内存与解码开销
                val context = LocalContext.current
                val thumbRequest = remember(imageUrl) {
                    ImageRequest.Builder(context)
                        .data(imageUrl.toCoverThumbnailUrl(128) ?: imageUrl)
                        .size(256)
                        .crossfade(false)
                        .build()
                }
                AsyncImage(
                    model = thumbRequest,
                    contentDescription = "当前歌曲封面",
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.Low,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_mini_play),
                contentDescription = null,
                tint = if (isDark) Color.White.copy(alpha = 0.65f) else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 一级页面默认主题背景占位容器。
 *
 * @param modifier 修饰符
 */
@Composable
private fun AppPageBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    )
}
