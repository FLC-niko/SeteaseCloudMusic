package com.example.seteasecloudmusic.feature.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.ui.components.UserAvatarButton
import com.example.seteasecloudmusic.core.player.PlaybackState
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.feature.auth.presentation.AccountLoginSheetContent
import com.example.seteasecloudmusic.feature.auth.presentation.AuthViewModel
import com.example.seteasecloudmusic.feature.artist.presentation.ArtistDetailRoute
import com.example.seteasecloudmusic.feature.home.presentation.DailyRecommendDetailRoute
import com.example.seteasecloudmusic.feature.home.presentation.HomeRoute
import com.example.seteasecloudmusic.feature.home.presentation.HomeViewModel
import com.example.seteasecloudmusic.feature.mine.presentation.MineRoute
import com.example.seteasecloudmusic.feature.radio.presentation.RadioRoute
import com.example.seteasecloudmusic.feature.search.presentation.SearchRoute
import com.example.seteasecloudmusic.feature.search.presentation.SearchViewModel
import com.example.seteasecloudmusic.feature.player.presentation.NowPlayingScreen
import com.example.seteasecloudmusic.feature.player.presentation.PlayerViewModel
import com.example.seteasecloudmusic.core.ui.components.Ios26NetworkOfflineDialog
import com.example.seteasecloudmusic.core.ui.components.GlassDefaults
import com.example.seteasecloudmusic.core.ui.components.GlassSliderDefaults
import com.example.seteasecloudmusic.core.ui.components.GlassThumb
import com.example.seteasecloudmusic.core.ui.components.glassSegmentDrag
import com.example.seteasecloudmusic.core.ui.components.liquidGlass
import com.example.seteasecloudmusic.core.ui.components.rememberGlassThumbGeometry
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
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
import com.example.seteasecloudmusic.feature.player.presentation.PlaylistQueueSheet
import coil.compose.AsyncImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.roundToInt



/**
 * `presentation.navigation` 模块说明：
 *
 * 这一层属于表现层，主要负责：
 * 1. 组织页面入口与页面切换结构。
 * 2. 承载纯 UI 的状态，例如当前选中的导航项、按压动画进度。
 * 3. 把视觉效果和交互组合成最终可展示的界面。
 *
 * 当前文件是应用的导航外壳，暂时还没有接入真正的 NavHost，
 * 因此它更像“首页容器 + 底部导航栏”的组合入口。
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
 *
 * 提到文件级常量：避免每次重组重新分配列表，也让它的身份保持稳定，
 * 便于 Compose 跳过依赖它的子组件。
 */
private val MainNavItems = listOf(
    BottomNavItem("首页", R.drawable.ic_nav_home, R.drawable.ic_nav_home_filled),
    BottomNavItem("电台", R.drawable.ic_nav_radio, R.drawable.ic_nav_radio_filled),
    BottomNavItem("我的", R.drawable.ic_nav_mine, R.drawable.ic_nav_mine_filled)
)

/** 左侧主导航条只承载 0~2，3 是右侧搜索按钮对应的页面。 */
private const val SEARCH_PAGE_INDEX = 3

private data class SelectedArtist(
    val id: Long,
    val name: String,
    val coverUrl: String?
)

private data class DailyRecommendState(
    val tracks: List<Track>,
    val posterBounds: Rect,
    val title: String = "每日推荐"
)


private fun lerpDp(start: Dp, stop: Dp, fraction: Float): Dp = start + (stop - start) * fraction
private fun lerpColor(start: Color, stop: Color, fraction: Float): Color = androidx.compose.ui.graphics.lerp(start, stop, fraction)

// 左侧主导航条内部分段滑块
@Composable
private fun LeftBarGlassThumb(
    backdrop: Backdrop,
    containerWidth: Dp,
    itemCount: Int,
    selectedIndex: Int,
    dragOffsetX: State<Float?>,
    barHeight: Dp,
    cornerRadius: Dp,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    if (alpha <= 0f) return
    val geometry = rememberGlassThumbGeometry(
        containerWidth = containerWidth,
        itemCount = itemCount,
        selectedIndex = selectedIndex,
        dragOffsetX = dragOffsetX.value,
        barHeight = barHeight,
        cornerRadius = cornerRadius
    )
    GlassThumb(
        backdrop = backdrop,
        geometry = geometry,
        alpha = alpha,
        surfaceAlpha = GlassSliderDefaults.ThumbSurfaceAlpha,
        modifier = modifier
    )
}

/**
 * 应用的主导航入口组件
 *
 * 此组件构建了整个应用的基础布局结构，包含：
 * 1. 底层的 [AppPageBackground] 背景
 * 2. 中间的内容区域（目前与背景层合并）
 * 3. 顶层悬浮的毛玻璃效果底部导航栏
 *
 * 核心使用了 [com.kyant.backdrop] 库来实现高性能的实时模糊与透镜效果。
 */
@Composable
fun AppNavigation(
    avatarUrl: String? = null,
    displayName: String? = null,
    onAvatarClick: (() -> Unit)? = null
) {
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

    var showNowPlaying by remember { mutableStateOf(false) }
    var showPlaylistQueueSheet by remember { mutableStateOf(false) }
    var miniPlayerArtworkBounds by remember { mutableStateOf<Rect?>(null) }
    var nowPlayingArtworkBounds by remember { mutableStateOf<Rect?>(null) }
    var nowPlayingArtworkUrl by remember { mutableStateOf<String?>(null) }
    val nowPlayingProgress = remember { Animatable(0f) }
    val nowPlayingScope = rememberCoroutineScope()
    var showAccountSheet by remember { mutableStateOf(false) }
    var mountAccountOverlay by remember { mutableStateOf(false) }
    var selectedArtist by remember { mutableStateOf<SelectedArtist?>(null) }
    var dailyRecommendState by remember { mutableStateOf<DailyRecommendState?>(null) }
    val expandProgress = remember { Animatable(0f) }
    val expandScope = rememberCoroutineScope()

    LaunchedEffect(showNowPlaying) {
        if (showNowPlaying) {
            nowPlayingProgress.snapTo(0f)
            // 先让全屏播放器完成一次测量，拿到大封面的真实目标位置，再开始移动。
            withFrameNanos { }
            withFrameNanos { }
            nowPlayingProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 560,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    LaunchedEffect(dailyRecommendState) {
        if (dailyRecommendState != null) {
            expandProgress.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 400f))
        }
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
    val backgroundColor = Color.White
    
    // 主导航栏按下/抬起时的缩放动画进度。
    val mainBarAnimationScope = rememberCoroutineScope()
    val mainBarProgressAnimation = remember { Animatable(0f) }
    
    // 右侧独立搜索按钮使用单独的动画状态，避免和主导航条互相影响。
    val searchAnimationScope = rememberCoroutineScope()
    val searchProgressAnimation = remember { Animatable(0f) }

    // 搜索展开后的关闭按钮按下动画状态
    val closeAnimationScope = rememberCoroutineScope()
    val closeProgressAnimation = remember { Animatable(0f) }
    
    // 缩小底栏接收点击时的弹跳程度，增加 stiffness 使其更紧致，缩小弹跳幅度
    val animationSpec = remember { spring<Float>(0.8f, 500f, 0.001f) }
    
    val backdrop = rememberLayerBackdrop{
        drawRect(backgroundColor)
        drawContent()
    }

    // 记录当前手指 X 坐标准备拖拽交互。
    // 刻意保留 State 对象本体、不用 by 解构：解构会在 AppNavigation 作用域读取 .value，
    // 导致每次指针移动都重组整个导航外壳；改由 GlassSlider 内部按需读取。
    val dragOffsetXState = remember { mutableStateOf<Float?>(null) }

    // 记录当前选中的导航项：
    // 0~2 对应左侧主导航条，3 对应右侧搜索按钮。
    val selectedIndexState = remember { mutableIntStateOf(0) }
    var lastActiveTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedIndexState.intValue) {
        if (selectedIndexState.intValue != SEARCH_PAGE_INDEX) {
            lastActiveTab = selectedIndexState.intValue
        }
    }

    val isSearchExpanded by remember {
        derivedStateOf { selectedIndexState.intValue == SEARCH_PAGE_INDEX }
    }

    val searchProgress by animateFloatAsState(
        targetValue = if (isSearchExpanded) 1f else 0f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
        label = "searchProgress"
    )

    val showGlassSlider = !isSearchExpanded && searchProgress < 0.01f
    val sliderAlpha by animateFloatAsState(
        targetValue = if (showGlassSlider) 1f else 0f,
        animationSpec = if (isSearchExpanded) snap() else tween(durationMillis = 200),
        label = "sliderAlpha"
    )

    BackHandler(enabled = isSearchExpanded) {
        searchViewModel.onClearQuery()
        selectedIndexState.intValue = lastActiveTab
    }

    // 抽成共享布局参数，保证底栏和滑块按同一套比例计算。
    val horizontalPadding = 24.dp
    val navBarHeight = 60.dp
    val mainSearchGap = 16.dp
    val searchButtonWidth = navBarHeight
    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val searchContentTopPadding = statusBarTopPadding + 86.dp
    // 统一形状：使用 RoundedRectangle 实现 G² 连续圆角（squircle），
    // 让玻璃滑块、主导航条、搜索按钮共享一致的圆角半径。
    val cornerRadius = navBarHeight / 2

    // ── 每日推荐展开进度（0f ~ 1f）──
    val sinkProgress = expandProgress.value

    // ── 键盘（IME）感知：实现 Apple Music 风格的平滑上抬效果 ──
    val imeBottomPx = WindowInsets.ime.getBottom(LocalDensity.current)
    val navBarsBottomPx = WindowInsets.navigationBars.getBottom(LocalDensity.current)
    // 键盘高度减去已经由 navigationBars inset 处理的底部距离
    val targetImeOffsetDp = with(LocalDensity.current) {
        (imeBottomPx - navBarsBottomPx).coerceAtLeast(0).toDp()
    }
    // 使用弹簧动画平滑过渡，避免键盘弹出时的生硬跳动
    val animatedImeOffset by animateDpAsState(
        targetValue = targetImeOffsetDp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
        label = "imeOffset"
    )

    // 2. 【舞台】：整个屏幕的根容器，使用 Box 以支持 Z 轴方向的层叠排列
    Box(modifier = Modifier.fillMaxSize()) {
        
        // --- 底层内容区域 ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 这里是毛玻璃的“取景层”。
                // 被包进来的内容会先渲染到底层纹理，再提供给上面的导航栏做模糊采样。
                .layerBackdrop(backdrop)
        ) {
            AppPageContent(
                selectedIndex = selectedIndexState,
                topContentPadding = searchContentTopPadding,
                imeOffset = animatedImeOffset,
                avatarUrl = authUiState.authSession?.avatarUrl,
                displayName = authUiState.authSession?.nickname,
                searchViewModel = searchViewModel,
                onAccountClick = { showAccountSheet = true },
                onPosterWallClick = { tracks, bounds, title ->
                    dailyRecommendState = DailyRecommendState(tracks, bounds, title)
                },
                onArtistClick = { artistId, artistName, artistCoverUrl ->
                    selectedArtist = SelectedArtist(
                        id = artistId,
                        name = artistName,
                        coverUrl = artistCoverUrl
                    )
                }
            )
        }

        AppTopArea(
            selectedIndex = selectedIndexState,
            sinkProgress = sinkProgress,
            avatarUrl = authUiState.authSession?.avatarUrl,
            displayName = authUiState.authSession?.nickname,
            onAvatarClick = {
                showAccountSheet = true
                onAvatarClick?.invoke()
            }
        )

        // --- 顶层悬浮导航栏及独立搜索按钮 ---
        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // 先避开系统导航栏（小白条）
                .windowInsetsPadding(WindowInsets.navigationBars)
                // 手动添加上下左右的间距，左右缩进，底部悬浮
                .padding(horizontal = horizontalPadding)
                .padding(top = 24.dp, bottom = 24.dp + animatedImeOffset)
                .fillMaxWidth()
                // 保持 Apple Music 的视觉高度
                .height(navBarHeight)
                .graphicsLayer {
                    alpha = 1f - sinkProgress
                    translationY = sinkProgress * 200.dp.toPx()
                }
        ) {
            val totalWidth = maxWidth
            val ballSize = navBarHeight
            val normalLeftWidth = totalWidth - ballSize - mainSearchGap

            val leftWidth = lerpDp(normalLeftWidth, ballSize, searchProgress)

            val searchGap = 10.dp
            val searchBarStartX = totalWidth - ballSize
            val searchBarTargetX = ballSize + searchGap
            val searchBarTargetWidth = totalWidth - (ballSize * 2) - (searchGap * 2)

            val currentSearchBarX = lerpDp(searchBarStartX, searchBarTargetX, searchProgress)
            val currentSearchBarWidth = lerpDp(ballSize, searchBarTargetWidth, searchProgress)

            Box(modifier = Modifier.fillMaxSize()) {

                // ============ 左侧主导航条 (包含主页、电台、我的，展开搜索时两边同步缩减为球状，只保留当前页图标并居中) ============
                Box(
                    modifier = Modifier
                        .width(leftWidth)
                        .fillMaxHeight()
                        .graphicsLayer {
                            // 按下时整体轻微放大，模拟液态玻璃被“压出张力”的感觉。缩小底栏放大比例。
                            val progress = mainBarProgressAnimation.value
                            val maxScale = (size.width + 8f.dp.toPx()) / size.width
                            val scale = lerp(1f, maxScale, progress)
                            scaleX = scale
                            scaleY = scale
                        }
                        .liquidGlass(
                            backdrop = backdrop,
                            cornerRadius = cornerRadius,
                            // 按下放大时同步缩放被采样的内容层，避免折射采样与组件本体错位
                            layerBlock = {
                                val progress = mainBarProgressAnimation.value
                                val maxScale = (size.width + 8f.dp.toPx()) / size.width
                                val scale = lerp(1f, maxScale, progress)
                                scaleX = scale
                                scaleY = scale
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = isSearchExpanded
                        ) {
                            searchViewModel.onClearQuery()
                            selectedIndexState.intValue = lastActiveTab
                        }
                        .glassSegmentDrag(
                            itemCount = MainNavItems.size,
                            onDragOffsetChange = { dragOffsetXState.value = it },
                            onSettle = {
                                selectedIndexState.intValue = it
                                lastActiveTab = it
                            },
                            onPressChanged = { pressed ->
                                mainBarAnimationScope.launch {
                                    mainBarProgressAnimation.animateTo(if (pressed) 1f else 0f, animationSpec)
                                }
                            },
                            // 搜索展开时底栏交给 clickable 处理（点击复位到原先保留页面），不响应滑动切换
                            enabled = !isSearchExpanded && searchProgress < 0.05f
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(cornerRadius))
                    ) {
                        LeftBarGlassThumb(
                            backdrop = backdrop,
                            containerWidth = normalLeftWidth,
                            itemCount = MainNavItems.size,
                            selectedIndex = lastActiveTab,
                            dragOffsetX = dragOffsetXState,
                            barHeight = navBarHeight,
                            cornerRadius = cornerRadius,
                            alpha = sliderAlpha,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )

                        val slotWidth = normalLeftWidth / MainNavItems.size
                        val inactiveAlpha = (1f - searchProgress * 3.5f).coerceIn(0f, 1f)
                        val labelAlpha = (1f - searchProgress * 3.5f).coerceIn(0f, 1f)

                        MainNavItems.forEachIndexed { index, item ->
                            val isActiveTab = index == lastActiveTab
                            val isSelected = selectedIndexState.intValue == index && !isSearchExpanded
                            val iconAlpha = if (isActiveTab) 1f else inactiveAlpha

                            if (iconAlpha > 0f) {
                                val normalCenterX = slotWidth * (index + 0.5f)
                                val targetCenterX = ballSize / 2
                                val currentCenterX = if (isActiveTab) {
                                    lerpDp(normalCenterX, targetCenterX, searchProgress)
                                } else {
                                    normalCenterX
                                }

                                val normalCenterY = 22.dp
                                val targetCenterY = ballSize / 2
                                val currentCenterY = if (isActiveTab) {
                                    lerpDp(normalCenterY, targetCenterY, searchProgress)
                                } else {
                                    normalCenterY
                                }

                                val targetColor = if (isSelected) Color(0xFFFA233B) else Color.DarkGray
                                val iconColor = if (isActiveTab && isSearchExpanded) {
                                    lerpColor(targetColor, Color.DarkGray, searchProgress)
                                } else {
                                    targetColor
                                }

                                val iconRes = if (isSelected && searchProgress < 0.5f) item.selectedIconRes else item.iconRes

                                Box(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                (currentCenterX - 13.dp).roundToPx(),
                                                (currentCenterY - 13.dp).roundToPx()
                                            )
                                        }
                                        .size(26.dp)
                                        .graphicsLayer { alpha = iconAlpha },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = iconRes),
                                        contentDescription = item.title,
                                        tint = iconColor,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            if (labelAlpha > 0f) {
                                val normalCenterX = slotWidth * (index + 0.5f)
                                val isSelected = selectedIndexState.intValue == index
                                val itemColor = if (isSelected) Color(0xFFFA233B) else Color.DarkGray

                                Box(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                (normalCenterX - 28.dp).roundToPx(),
                                                37.dp.roundToPx()
                                            )
                                        }
                                        .width(56.dp)
                                        .graphicsLayer { alpha = labelAlpha },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.title,
                                        color = itemColor,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // ============ 搜索按钮 / 展开成长条的搜索栏 (从右圆球缓缓向左移动并展开成长条) ============
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
                        .offset { IntOffset(currentSearchBarX.roundToPx(), 0) }
                        .width(currentSearchBarWidth)
                        .fillMaxHeight()
                        .graphicsLayer {
                            val progress = searchProgressAnimation.value
                            val maxScale = (size.width + 4f.dp.toPx()) / size.width
                            val scale = lerp(1f, maxScale, progress)
                            scaleX = scale
                            scaleY = scale
                        }
                        .liquidGlass(
                            backdrop = backdrop,
                            cornerRadius = cornerRadius,
                            surfaceAlpha = GlassDefaults.SurfaceAlphaBright
                        )
                        .clickable(
                            interactionSource = searchInteractionSource,
                            indication = null,
                            enabled = !isSearchExpanded
                        ) {
                            lastActiveTab = selectedIndexState.intValue
                            selectedIndexState.intValue = SEARCH_PAGE_INDEX
                        }
                ) {
                    val iconStartPadding = lerpDp((ballSize - 22.dp) / 2, 16.dp, searchProgress)
                    val iconSize = lerpDp(22.dp, 18.dp, searchProgress)
                    val iconTint = lerpColor(Color.DarkGray, Color(0xFFFA233B), searchProgress)

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = iconStartPadding, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_search),
                            contentDescription = "搜索",
                            tint = iconTint,
                            modifier = Modifier.size(iconSize)
                        )

                        if (searchProgress > 0.15f) {
                            val inputAlpha = ((searchProgress - 0.2f) / 0.8f).coerceIn(0f, 1f)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer { alpha = inputAlpha }
                            ) {
                                SearchQueryInputContent(
                                    viewModel = searchViewModel,
                                    enabled = isSearchExpanded
                                )
                            }
                        }
                    }
                }

                // ============ 右侧关闭按钮 (在原先搜索位置从小放大) ============
                if (searchProgress > 0.01f) {
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
                            .graphicsLayer {
                                val progress = closeProgressAnimation.value
                                val maxScale = (size.width + 4f.dp.toPx()) / size.width
                                val pressScale = lerp(1f, maxScale, progress)
                                val appearScale = searchProgress.coerceIn(0f, 1f)
                                scaleX = appearScale * pressScale
                                scaleY = appearScale * pressScale
                                alpha = appearScale
                            }
                            .liquidGlass(
                                backdrop = backdrop,
                                cornerRadius = cornerRadius,
                                surfaceAlpha = GlassDefaults.SurfaceAlphaBright
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
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        dailyRecommendState?.let { state ->
            val progress = expandProgress.value
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val startScaleX = state.posterBounds.width / size.width
                            val startScaleY = state.posterBounds.height / size.height
                            scaleX = lerp(startScaleX, 1f, progress)
                            scaleY = lerp(startScaleY, 1f, progress)
                            translationX = lerp(state.posterBounds.left, 0f, progress)
                            translationY = lerp(state.posterBounds.top, 0f, progress)
                            transformOrigin = TransformOrigin(0f, 0f)
                            alpha = progress
                        }
                ) {
                    val homeViewModel: HomeViewModel = hiltViewModel()
                    DailyRecommendDetailRoute(
                        tracks = state.tracks,
                        title = state.title,
                        onClose = {
                            expandScope.launch {
                                expandProgress.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 500f))
                                dailyRecommendState = null
                            }
                        }
                    )
                }
            }
        }

        SearchMiniPlayerBar(
            backdrop = backdrop,
            cornerRadius = cornerRadius,
            playbackStateFlow = playerViewModel.playbackState,
            onPlayPauseClick = { playerViewModel.onPlayPause() },
            onPlaylistClick = { showPlaylistQueueSheet = true },
            onNextClick = { playerViewModel.onNext() },
            onPreviousClick = { playerViewModel.onPrevious(forcePreviousSong = true) },
            onBarClick = { artworkUrl ->
                if (!showNowPlaying) {
                    nowPlayingArtworkUrl = artworkUrl
                    nowPlayingArtworkBounds = null
                    showNowPlaying = true
                }
            },
            onArtworkBoundsChanged = { bounds ->
                if (miniPlayerArtworkBounds != bounds) {
                    miniPlayerArtworkBounds = bounds
                }
            },
            hideArtwork = showNowPlaying && nowPlayingArtworkBounds != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 24.dp, end = 24.dp, bottom = 100.dp + animatedImeOffset)
                .graphicsLayer {
                    // 下沉到更低的位置（76dp），而不是移出屏幕
                    translationY = sinkProgress * 76.dp.toPx()
                }
        )

        if (showNowPlaying) {
            NowPlayingTransitionLayer(
                playerViewModel = playerViewModel,
                progress = nowPlayingProgress.asState(),
                artworkUrl = nowPlayingArtworkUrl,
                sourceArtworkBounds = miniPlayerArtworkBounds,
                targetArtworkBounds = nowPlayingArtworkBounds,
                onTargetArtworkBoundsChanged = { bounds ->
                    if (nowPlayingArtworkBounds != bounds) {
                        nowPlayingArtworkBounds = bounds
                    }
                },
                onClose = {
                    nowPlayingScope.launch {
                        nowPlayingProgress.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(
                                durationMillis = 440,
                                easing = FastOutSlowInEasing
                            )
                        )
                        showNowPlaying = false
                        nowPlayingArtworkBounds = null
                    }
                }
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
                viewModel = authViewModel
            )
        }

        selectedArtist?.let { artist ->
            ArtistDetailRoute(
                artistId = artist.id,
                artistName = artist.name,
                artistCoverUrl = artist.coverUrl,
                onClose = { selectedArtist = null }
            )
        }

        // 依据 iOS 26 解析图复刻的液态玻璃断网重试小弹窗（物理采样底层真实的动态 Backdrop）
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

/**
 * 一级页面内容区域（首页 / 电台 / 我的 / 搜索）。
 *
 * 单独抽成组件是为了**收窄重组范围**：[selectedIndex] 以 [State] 形式传入、在本组件内部读取，
 * 因此切换页面时只有这一块需要重新执行，导航栏、迷你播放条、各类覆盖层（正在播放、
 * 账号抽屉、歌手详情、断网弹窗）都不会被牵连重组。
 *
 * 页面采用「首次访问即常驻」策略，而不是每次切换都销毁重建：
 * 逐帧 trace 显示，切换页面那一帧的 `Compose:onForgotten`（销毁旧页）单帧可达 40ms 以上，
 * 与「首次组合新页」叠加后整帧达 85ms —— 这正是切换时页面与按钮动效一起卡顿一下的根因。
 * 页面内的液态玻璃节点会各自持有采样图层，销毁/重建意味着反复释放与重建这些 GPU 资源。
 *
 * 因此：访问过的页面全部保留在组合树中，状态与玻璃图层都不再抖振；
 * 靠下面的两个修饰符让非当前页**零开销**地待着：
 * - [drawWithContent] 跳过整页绘制（不产生 RenderThread / GPU 开销）
 * - [offset] 把整页挪到屏幕外（不参与命中测试，隐藏页不会吞掉当前页的手势）
 */
@Composable
private fun AppPageContent(
    selectedIndex: State<Int>,
    topContentPadding: Dp,
    imeOffset: Dp,
    avatarUrl: String?,
    displayName: String?,
    searchViewModel: SearchViewModel,
    onAccountClick: () -> Unit,
    onPosterWallClick: (tracks: List<Track>, posterBounds: Rect, title: String) -> Unit,
    onArtistClick: (Long, String, String?) -> Unit
) {
    val index = selectedIndex.value
    val bottomContentPadding = 180.dp + imeOffset
    val pageBackgroundColor = when (index) {
        2 -> Color(0xFFF7F7FA)
        else -> Color.White
    }

    // 页面常驻意味着离开搜索页后它的输入框仍然可聚焦，
    // 因此显式清一次焦点，保证键盘随页面一起收起（与原先「切走即销毁」的行为一致）。
    val focusManager = LocalFocusManager.current
    LaunchedEffect(index) {
        if (index != SEARCH_PAGE_INDEX) {
            focusManager.clearFocus(force = true)
        }
    }

    // 已访问过的页面集合。用普通 Set 而非 snapshot 状态：它只在组合期读写，
    // 不需要（也不应该）驱动重组。首页在启动时就已挂载。
    val mountedPages = remember { mutableSetOf(index) }
    mountedPages.add(index)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackgroundColor)
    ) {
        mountedPages.forEach { pageIndex ->
            val isActive = pageIndex == index
            key(pageIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent { if (isActive) drawContent() }
                        .offset { IntOffset(0, if (isActive) 0 else OFFSCREEN_PAGE_OFFSET_PX) }
                ) {
                    when (pageIndex) {
                        0 -> HomeRoute(
                            topContentPadding = topContentPadding,
                            bottomContentPadding = bottomContentPadding,
                            avatarUrl = avatarUrl,
                            displayName = displayName,
                            onAvatarClick = onAccountClick,
                            onPosterWallClick = onPosterWallClick
                        )

                        1 -> RadioRoute(
                            topContentPadding = topContentPadding,
                            bottomContentPadding = bottomContentPadding,
                            avatarUrl = avatarUrl,
                            displayName = displayName,
                            onAvatarClick = onAccountClick,
                            onArtistClick = onArtistClick
                        )

                        2 -> MineRoute(
                            topContentPadding = topContentPadding,
                            bottomContentPadding = bottomContentPadding,
                            onLoginClick = onAccountClick
                        )

                        3 -> SearchRoute(
                            viewModel = searchViewModel,
                            topContentPadding = topContentPadding,
                            bottomContentPadding = bottomContentPadding,
                            onArtistClick = onArtistClick
                        )

                        else -> AppPageBackground()
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
 * 页面级大标题顶栏（仅服务「电台」「搜索」）。
 *
 * 首页与「我的」由各自页面内自带的 Apple Music 大标题承载，这里直接返回不渲染任何内容 ——
 * 这与原来 `if (!hasCustomTopBar)` 的行为一致，但把判断搬到本组件内部，
 * 使 [selectedIndex] 的变化不会向上重组整个导航外壳。
 */
@Composable
private fun AppTopArea(
    selectedIndex: State<Int>,
    sinkProgress: Float,
    avatarUrl: String?,
    displayName: String?,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val index = selectedIndex.value
    val hasCustomTopBar = index == 0 || index == 1 || index == 2
    if (hasCustomTopBar) return

    val pageTitle = when (index) {
        1 -> "电台"
        3 -> "搜索"
        else -> "首页"
    }
    val topLargeTitleAlpha by animateFloatAsState(
        targetValue = 1f - sinkProgress,
        animationSpec = tween(durationMillis = 220),
        label = "topLargeTitleAlpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = pageTitle,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 30)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 140))
            },
            label = "pageTitleTransition",
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 20.dp, top = 14.dp)
                .graphicsLayer { alpha = topLargeTitleAlpha }
        ) { animatedTitle ->
            LargePageTitle(title = animatedTitle)
        }

        UserAvatarButton(
            avatarUrl = avatarUrl,
            displayName = displayName,
            onClick = onAvatarClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(end = 20.dp, top = 14.dp)
                .graphicsLayer { alpha = 1f - sinkProgress }
        )
    }
}

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

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val hostHeightPx = with(density) { maxHeight.toPx() }
        val panelHeightPx = hostHeightPx * 0.95f
        val hiddenOffsetPx = panelHeightPx + with(density) { 24.dp.toPx() }

        val panelOffsetFraction = remember { Animatable(1f) }
        var dragOffsetPx by remember { mutableFloatStateOf(0f) }
        var isDragging by remember { mutableStateOf(false) }

        val settledDragOffsetPx by animateFloatAsState(
            targetValue = if (isDragging || !visible) dragOffsetPx else 0f,
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
                    animationSpec = spring(dampingRatio = 0.84f, stiffness = 550f)
                )
            } else {
                panelOffsetFraction.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.88f, stiffness = 500f)
                )
                onDismissed()
            }
        }

        val panelShape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)

        // 遮罩层：模糊 + 压暗只在绘制阶段计算，Animatable 每帧只让 RenderNode 失效，不触发重组。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(0.dp) },
                    effects = {
                        blur(8f.dp.toPx())
                        vibrancy()
                    },
                    onDrawSurface = {
                        drawRect(Color.Black.copy(alpha = 0.32f))
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

        // 主抽屉卡片：位移与缩放全部在 draw 阶段读取状态，既不触发重组也不触发布局。
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
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
                .clip(panelShape)
                .background(Color(0xFFF2F2F7))
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
                        .background(Color(0xFFD2D2D8))
                )
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
        color = Color(0xFF111111),
        modifier = modifier
    )
}

/**
 * 搜索模式底部迷你播放条。
 */
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

    BasicTextField(
        value = uiState.query,
        onValueChange = { viewModel.onQueryChanged(it) },
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .onKeyEvent { keyEvent ->
                if (keyEvent.key == Key.Enter) {
                    viewModel.onSearchSubmit()
                    true
                } else {
                    false
                }
            },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (uiState.query.isEmpty()) {
                        Text(
                            text = "搜你想听的",
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                    innerTextField()
                }
                if (uiState.query.isNotEmpty()) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_close),
                        contentDescription = "清空搜索",
                        tint = Color.Gray,
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
    sourceArtworkBounds: Rect?,
    targetArtworkBounds: Rect?,
    onTargetArtworkBoundsChanged: (Rect) -> Unit,
    onClose: () -> Unit
) {
    val pageAlpha = remember(progress) {
        derivedStateOf {
            ((progress.value - 0.04f) / 0.50f).coerceIn(0f, 1f)
        }
    }
    val targetArtworkAlpha = remember(progress) {
        derivedStateOf {
            ((progress.value - 0.86f) / 0.14f).coerceIn(0f, 1f)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = pageAlpha.value
                }
        ) {
            NowPlayingScreen(
                playerViewModel = playerViewModel,
                onClose = onClose,
                artworkAlpha = targetArtworkAlpha,
                onArtworkBoundsChanged = onTargetArtworkBoundsChanged
            )
        }

        if (sourceArtworkBounds != null && targetArtworkBounds != null) {
            MovingPlayerArtwork(
                imageUrl = artworkUrl,
                sourceBounds = sourceArtworkBounds,
                targetBounds = targetArtworkBounds,
                progress = progress,
                targetArtworkAlpha = targetArtworkAlpha
            )
        }
    }
}

@Composable
private fun MovingPlayerArtwork(
    imageUrl: String?,
    sourceBounds: Rect,
    targetBounds: Rect,
    progress: State<Float>,
    targetArtworkAlpha: State<Float>
) {
    if (sourceBounds.width <= 0f || sourceBounds.height <= 0f ||
        targetBounds.width <= 0f || targetBounds.height <= 0f
    ) {
        return
    }

    val density = LocalDensity.current
    val targetWidth = with(density) { targetBounds.width.toDp() }
    val targetHeight = with(density) { targetBounds.height.toDp() }
    val sourceCornerPx = with(density) { 8.dp.toPx() }
    val targetCornerPx = with(density) { 16.dp.toPx() }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    targetBounds.left.roundToInt(),
                    targetBounds.top.roundToInt()
                )
            }
            .size(targetWidth, targetHeight)
            .graphicsLayer {
                val fraction = progress.value.coerceIn(0f, 1f)
                val currentScaleX = lerp(
                    sourceBounds.width / targetBounds.width,
                    1f,
                    fraction
                )
                val currentScaleY = lerp(
                    sourceBounds.height / targetBounds.height,
                    1f,
                    fraction
                )

                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = currentScaleX
                scaleY = currentScaleY
                translationX = lerp(sourceBounds.left - targetBounds.left, 0f, fraction)
                translationY = lerp(sourceBounds.top - targetBounds.top, 0f, fraction)
                alpha = 1f - targetArtworkAlpha.value

                // 图层本身会被缩放，因此反算局部圆角，确保视觉圆角从 MiniPlayer 的 8dp
                // 平滑过渡到大封面的 16dp，而不是随缩放突然变成夸张的大圆角。
                val visualCornerPx = lerp(sourceCornerPx, targetCornerPx, fraction)
                val localCornerPx = visualCornerPx / minOf(currentScaleX, currentScaleY)
                shape = RoundedCornerShape(with(density) { localCornerPx.toDp() })
                clip = true
            }
            .background(Color(0xFFE3E3E6)),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SearchMiniPlayerBar(
    backdrop: Backdrop,
    cornerRadius: Dp,
    playbackStateFlow: StateFlow<PlaybackState>,
    onPlayPauseClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onBarClick: (String?) -> Unit,
    onArtworkBoundsChanged: (Rect) -> Unit,
    hideArtwork: Boolean,
    modifier: Modifier = Modifier
) {
    val playbackState by playbackStateFlow.collectAsStateWithLifecycle()
    val hasTrack = playbackState.currentTrack != null
    val isPlaying = playbackState.status == PlayerStatus.PLAYING
    val duration = playbackState.durationMs
    val currentPosition = playbackState.currentPositionMs

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // 当前展示的曲目信息（滑动切歌动画期间保持旧歌曲滑出，新歌曲就绪后滑入）
    var displayedTrack by remember { mutableStateOf(playbackState.currentTrack) }
    var isTransitioning by remember { mutableStateOf(false) }
    var transitionJob by remember { mutableStateOf<Job?>(null) }

    // 左右滑动手势状态：实时位移与平启动画控制器
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val animOffsetX = remember { Animatable(0f) }
    val animAlpha = remember { Animatable(1f) }

    // 是否允许切歌（有曲目且待播列表大于 1 首）
    val canSwitch = hasTrack && playbackState.queueTracks.size > 1

    // 外部切歌或播放列表切换时，自动同步 displayedTrack
    LaunchedEffect(playbackState.currentTrack?.id) {
        val newTrack = playbackState.currentTrack
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

    val targetProgress = remember(playbackState.currentTrack?.id, currentPosition, duration) {
        if (duration > 0) {
            (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else 0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = if (isPlaying && targetProgress > 0f) {
            tween(durationMillis = 500, easing = LinearEasing)
        } else {
            spring(stiffness = 500f)
        },
        label = "miniPlayerProgress"
    )

    val swipeThreshold = with(density) { 36.dp.toPx() }
    val exitDistance = with(density) { 220.dp.toPx() }

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

                // 2. 只有在 animOffsetX 与 animAlpha 已经对齐 finalDrag 时，才解除 isDragging 并重置 dragOffsetX。
                // 此时 translationX 从 dragOffsetX (finalDrag) 切换至 animOffsetX.value (finalDrag)，差值为 0，彻底杜绝闪烁！
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

                // 4. 将位移瞬移到对侧入口，注意此时 alpha 保持为 0f，对用户绝对隐形！
                animOffsetX.snapTo(targetEnter)
                animAlpha.snapTo(0f)

                // 5. 触发切歌
                if (isNext) {
                    onNextClick()
                } else {
                    onPreviousClick()
                }

                // 6. 等待新歌曲就绪（超时放宽至 600ms，确保异步分发时不因超时退化）
                val newTrack = withTimeoutOrNull(600) {
                    snapshotFlow { playbackState.currentTrack }
                        .filter { it != null && it.id != oldTrackId }
                        .first()
                }

                if (newTrack != null) {
                    // 在透明隐形状态下切换曲目，无任何视觉撕裂
                    displayedTrack = newTrack

                    // 7. 新歌曲（封面+歌名）从对侧优雅滑入复位，带有自然弹簧感
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
                    // 超时未就绪兜底：在原位渐入当前曲目，绝不把旧歌曲在对侧滑入
                    displayedTrack = playbackState.currentTrack
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .liquidGlass(
                backdrop = backdrop,
                cornerRadius = cornerRadius,
                surfaceAlpha = GlassDefaults.SurfaceAlphaBright
            )
            .clip(RoundedCornerShape(cornerRadius))
            // 移除默认水波纹（灰色），使用无 indication 的点击，避免按压变灰
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = hasTrack && !isTransitioning
            ) {
                onBarClick(currentArtworkUrl)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧可滑动区域：封面与歌名同时随手势平移
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clipToBounds()
                    .padding(start = 14.dp)
                    .pointerInput(hasTrack, canSwitch) {
                        if (!hasTrack) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            // 立即消费 Down 事件，彻底切断底层列表接收按压，杜绝底层水波纹与灰色渗透！
                            down.consume()
                            var totalDragX = 0f
                            var isDrag = false
                            var gestureEnded = false
                            val touchSlop = viewConfiguration.touchSlop
                            val pointerId = down.id

                            // 关键：手势重新开始时，立即将 dragOffsetX 重置为 0，防止历史手势残存数值！
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
                                        // 关键：在触发拖拽的第一帧，必须在开启 isDragging 前立即同步更新 dragOffsetX！
                                        // 绝不能进入下个分支才更新，彻底解决“首帧读取历史残存位移导致在中间闪现”的问题！
                                        val effectiveDeltaX = if (canSwitch) deltaX else deltaX * 0.25f
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
                            .size(40.dp)
                            .graphicsLayer { alpha = if (hideArtwork) 0f else 1f },
                        onBoundsChanged = { bounds ->
                            if (!isDragging && !isTransitioning) {
                                onArtworkBoundsChanged(bounds)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        color = Color.Black,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 播放列表图标（在播放暂停左边，点击展开二级菜单显示歌曲播放顺序）
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = true,
                        onClick = onPlaylistClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mini_playlist),
                    contentDescription = "播放列表",
                    tint = if (hasTrack) Color(0xFF2C2C2C) else Color(0xFFB8B8B8),
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 播放暂停在最靠右，外面加上一个圈，显示播放进度，与组件的圆角和大小对齐
            MiniPlayerProgressPlayButton(
                isPlaying = isPlaying,
                hasTrack = hasTrack,
                progress = animatedProgress,
                onClick = onPlayPauseClick
            )
        }
    }
}

/**
 * 迷你播放条右侧的播放/暂停按钮：
 * 外层包裹播放进度指示圆环，与组件的大小与圆角对齐
 */
@Composable
private fun MiniPlayerProgressPlayButton(
    isPlaying: Boolean,
    hasTrack: Boolean,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ringColor = if (hasTrack) Color(0xFF2C2C2C) else Color(0xFFB8B8B8)
    val trackColor = if (hasTrack) Color(0x1F000000) else Color(0x0F000000)
    val strokeWidth = 1.8.dp

    Box(
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = hasTrack,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
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

        // 3. 居中播放/暂停图标（尺寸放大至 19dp）
        Icon(
            painter = painterResource(
                id = if (isPlaying) R.drawable.ic_mini_pause else R.drawable.ic_mini_play
            ),
            contentDescription = if (isPlaying) "暂停" else "播放",
            tint = ringColor,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun MiniPlayerArtwork(
    imageUrl: String?,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                onBoundsChanged(coordinates.boundsInRoot())
            }
            .clip(shape)
            .background(Color(0xFFE3E3E6)),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "当前歌曲封面",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}





/**
 * 应用页面底色
 *
 * 主页 / 电台 / 我的 这几个入口目前还没有独立内容页时，
 * 先用纯白底保持和 Apple Music 接近的简洁观感。
 *
 * @param modifier 修饰符
 */
@Composable
private fun AppPageBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    )
}
