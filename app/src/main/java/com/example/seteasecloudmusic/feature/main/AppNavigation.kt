package com.example.seteasecloudmusic.feature.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
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
import com.example.seteasecloudmusic.feature.search.presentation.SearchRoute
import com.example.seteasecloudmusic.feature.search.presentation.SearchViewModel
import com.example.seteasecloudmusic.feature.player.presentation.NowPlayingScreen
import com.example.seteasecloudmusic.feature.player.presentation.PlayerViewModel
import com.example.seteasecloudmusic.core.ui.components.Ios26NetworkOfflineDialog
import com.example.seteasecloudmusic.core.ui.components.GlassDefaults
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
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch



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
 * @property icon 导航项显示的图标资源
 */
data class BottomNavItem(val title: String, val icon: ImageVector)

/**
 * 左侧主导航条承载的三个一级入口。
 *
 * 提到文件级常量：避免每次重组重新分配列表，也让它的身份保持稳定，
 * 便于 Compose 跳过依赖它的子组件。
 */
private val MainNavItems = listOf(
    BottomNavItem("首页", Icons.Filled.Home),
    BottomNavItem("电台", Icons.Filled.Radio),
    BottomNavItem("我的", Icons.Filled.Person)
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

/**
 * 迷你播放条真正需要的字段。
 *
 * [PlaybackState] 里的 [PlaybackState.currentPositionMs] 每 500ms 变化一次，
 * 而迷你播放条并不展示进度。把它派生为一个只含必要字段的值，
 * 进度推进时派生值保持相等，因此不会引发任何重组。
 */
private data class MiniPlayerUiState(
    val hasTrack: Boolean,
    val isPlaying: Boolean,
    val title: String?,
    val artworkUrl: String?,
    val hasNextTrack: Boolean
)

//底栏上方的玻璃滑块
@Composable
fun GlassSlider(
    backdrop: Backdrop,
    contentBackdrop: Backdrop,
    mainItemCount: Int,
    // 选中项与拖拽横坐标都以 State 形式下传：变化只让本组件内部的读取作用域失效，
    // 不会再向上重组整个 AppNavigation 组件树。
    selectedIndex: State<Int>,
    dragOffsetX: State<Float?>,
    navBarHeight: Dp,
    // 同理，按压放大进度也在 draw 阶段按需读取。
    mainBarProgress: State<Float>,
    horizontalPadding: Dp,
    mainSearchGap: Dp,
    searchButtonWidth: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = horizontalPadding)
            .fillMaxWidth()
            .height(navBarHeight),
        horizontalArrangement = Arrangement.spacedBy(mainSearchGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .graphicsLayer {
                    // 让滑块在点击状态下的放大程度增大。
                    // 在 draw 阶段读取进度 State：按压动画每帧只触发重绘，不触发重组。
                    val maxScale = (size.width + 32f.dp.toPx()) / size.width
                    val scale = lerp(1f, maxScale, mainBarProgress.value)
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // 滑块几何与 Gooey 拉伸统一由 core 的共用实现计算，避免与「我的」页各写一套。
            // 选中项在本作用域内读取：切换页面时只有滑块这一小块需要重建。
            val activeIndex = selectedIndex.value
            val geometry = rememberGlassThumbGeometry(
                containerWidth = maxWidth,
                itemCount = mainItemCount,
                selectedIndex = activeIndex,
                dragOffsetX = dragOffsetX.value,
                barHeight = navBarHeight,
                cornerRadius = cornerRadius
            )
            val thumbAlpha by animateFloatAsState(
                targetValue = if (activeIndex in 0 until mainItemCount) 1f else 0f,
                animationSpec = spring(stiffness = 500f, dampingRatio = 0.9f),
                label = "glassThumbAlpha"
            )

            GlassThumb(
                backdrop = rememberCombinedBackdrop(backdrop, contentBackdrop),
                geometry = geometry,
                alpha = thumbAlpha
            )
        }

        // 占位搜索按钮宽度，确保滑块计算区域与主导航条完全一致。
        Spacer(modifier = Modifier.width(searchButtonWidth))
    }
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
    var showAccountSheet by remember { mutableStateOf(false) }
    var mountAccountOverlay by remember { mutableStateOf(false) }
    var selectedArtist by remember { mutableStateOf<SelectedArtist?>(null) }
    var dailyRecommendState by remember { mutableStateOf<DailyRecommendState?>(null) }
    val expandProgress = remember { Animatable(0f) }
    val expandScope = rememberCoroutineScope()

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
    
    // 缩小底栏接收点击时的弹跳程度，增加 stiffness 使其更紧致，缩小弹跳幅度
    val animationSpec = remember { spring<Float>(0.8f, 500f, 0.001f) }
    
    val backdrop = rememberLayerBackdrop{
        drawRect(backgroundColor)
        drawContent()
    }
    val navBarContentBackdrop = rememberLayerBackdrop()

    // 记录当前手指 X 坐标准备拖拽交互。
    // 刻意保留 State 对象本体、不用 by 解构：解构会在 AppNavigation 作用域读取 .value，
    // 导致每次指针移动都重组整个导航外壳；改由 GlassSlider 内部按需读取。
    val dragOffsetXState = remember { mutableStateOf<Float?>(null) }

    // 记录当前选中的导航项：
    // 0~2 对应左侧主导航条，3 对应右侧搜索按钮。
    // 与 dragOffsetXState 同理保留 State 本体、不用 by 解构：
    // 让读取发生在真正需要它的子组件内部，切换页面时不再重组整个导航外壳。
    val selectedIndexState = remember { mutableIntStateOf(0) }

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
            val collapsedWidth = navBarHeight
            val expandedWidth = totalWidth - collapsedWidth - mainSearchGap

            // 用 derivedStateOf 让它只在「是否处于搜索态」真正翻转时才失效：
            // 首页 ↔ 我的 之间切换不会重组整个导航栏外壳。
            val isSearchExpanded by remember {
                derivedStateOf { selectedIndexState.value == SEARCH_PAGE_INDEX }
            }

            val leftWidth by animateDpAsState(
                targetValue = if (isSearchExpanded) collapsedWidth else expandedWidth,
                animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
                label = "leftWidth"
            )
            val rightWidth by animateDpAsState(
                targetValue = if (isSearchExpanded) expandedWidth else collapsedWidth,
                animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
                label = "rightWidth"
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(navBarContentBackdrop),
                horizontalArrangement = Arrangement.spacedBy(mainSearchGap)
            ) {
            
                // ============ 左侧主导航条 (包含主页、电台、我的) ============
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
                            indication = null
                        ) {
                            if (isSearchExpanded) {
                                selectedIndexState.value = 0
                            }
                        }
                        .glassSegmentDrag(
                            itemCount = MainNavItems.size,
                            onDragOffsetChange = { dragOffsetXState.value = it },
                            onSettle = { selectedIndexState.value = it },
                            onPressChanged = { pressed ->
                                mainBarAnimationScope.launch {
                                    mainBarProgressAnimation.animateTo(if (pressed) 1f else 0f, animationSpec)
                                }
                            },
                            // 搜索展开时底栏交给 clickable 处理（点击复位到首页），不响应滑动切换
                            enabled = !isSearchExpanded
                        )
                ) {
                    // 主导航栏内部负责均分三个一级入口。
                    // 容器缩小时（圆按钮）不显示导航图标，避免挤压
                    if (leftWidth > collapsedWidth * 1.5f) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MainNavItems.forEachIndexed { index, item ->
                                val isSelected = selectedIndexState.value == index
                                val itemColor = if (isSelected) Color(0xFFFA233B) else Color.DarkGray

                                Column(
                                    modifier = Modifier
                                        .weight(1f) // 使所有导航项等宽，实现 Apple Music 分段滑块风格
                                        .fillMaxHeight(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically)
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = itemColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Text(
                                        text = item.title,
                                        color = itemColor,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    } else {
                        // 缩小成圆形按钮时显示返回或主页图标
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "返回主导航",
                            tint = Color.DarkGray,
                            modifier = Modifier.align(Alignment.Center).size(26.dp)
                        )
                    }
                }

                // ============ 右侧独立的搜索按钮 / 搜索栏 ============
                val searchColor = if (isSearchExpanded) Color(0xFFFA233B) else Color.DarkGray
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
                if (isSearchExpanded && rightWidth > collapsedWidth * 1.5f) {
                    Row(
                        modifier = Modifier
                            .width(rightWidth)
                            .fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
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
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            SearchQueryField(viewModel = searchViewModel)
                        }

                        Box(
                            modifier = Modifier
                                .size(navBarHeight)
                                .liquidGlass(
                                    backdrop = backdrop,
                                    cornerRadius = cornerRadius,
                                    surfaceAlpha = GlassDefaults.SurfaceAlphaBright
                                )
                                .clickable(
                                    interactionSource = searchInteractionSource,
                                    indication = null
                                ) {
                                    searchViewModel.onClearQuery()
                                    selectedIndexState.value = 0
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "关闭搜索",
                                tint = Color.Black,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .width(rightWidth)
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
                                indication = null
                            ) {
                                if (!isSearchExpanded) {
                                    selectedIndexState.value = SEARCH_PAGE_INDEX
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "搜索",
                            tint = searchColor,
                            modifier = Modifier.size(26.dp)
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
            onNextClick = { playerViewModel.onNext() },
            onBarClick = { showNowPlaying = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 24.dp, end = 24.dp, bottom = 100.dp + animatedImeOffset)
                .graphicsLayer {
                    // 下沉到更低的位置（76dp），而不是移出屏幕
                    translationY = sinkProgress * 76.dp.toPx()
                }
        )

        GlassSlider(
            backdrop = backdrop,
            contentBackdrop = navBarContentBackdrop,
            mainItemCount = MainNavItems.size,
            selectedIndex = selectedIndexState,
            dragOffsetX = dragOffsetXState,
            navBarHeight = navBarHeight,
            // asState() 返回 Animatable 内部缓存的 AnimationState（同一实例），
            // 由 GlassSlider 在 draw 阶段读取，按压动画不再驱动整个外壳重组。
            mainBarProgress = mainBarProgressAnimation.asState(),
            horizontalPadding = horizontalPadding,
            mainSearchGap = mainSearchGap,
            searchButtonWidth = searchButtonWidth,
            cornerRadius = cornerRadius,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 24.dp + animatedImeOffset) // 底部导航栏间距 + 键盘偏移
                .graphicsLayer {
                    alpha = 1f - sinkProgress
                    translationY = sinkProgress * 200.dp.toPx()
                }
        )

        if (showNowPlaying) {
            NowPlayingScreen(
                playerViewModel = playerViewModel,
                onClose = { showNowPlaying = false },
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

    Box(modifier = Modifier.fillMaxSize()) {
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

                        1 -> AppPageBackground() // 电台（私人 FM 待实现）

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
    val hasCustomTopBar = index == 0 || index == 2
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
 * 顶部搜索输入框。
 *
 * 搜索状态在这里**独立订阅**，而不是在 [AppNavigation] 里读取：
 * `SearchViewModel.uiState` 每输入一个字符就会变化，若在外壳作用域读取，
 * 整个导航外壳（玻璃 Backdrop、导航项、各覆盖层）都会随每次按键一起重组。
 */
@Composable
private fun SearchQueryField(
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BasicTextField(
        value = uiState.query,
        onValueChange = { viewModel.onQueryChanged(it) },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(20.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (uiState.query.isEmpty()) {
                        Text("搜你想听的", color = Color.Gray)
                    }
                    innerTextField()
                }
                if (uiState.query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
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
private fun SearchMiniPlayerBar(
    backdrop: Backdrop,
    cornerRadius: Dp,
    playbackStateFlow: StateFlow<PlaybackState>,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onBarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 播放进度每 500ms 刷新一次。这里把订阅收回组件内部，并用 derivedStateOf 只保留
    // 迷你播放器真正关心的字段：进度推进时派生值不变，因此不会触发任何重组。
    val playbackState by playbackStateFlow.collectAsStateWithLifecycle()
    val miniState by remember {
        derivedStateOf {
            MiniPlayerUiState(
                hasTrack = playbackState.currentTrack != null,
                isPlaying = playbackState.status == PlayerStatus.PLAYING,
                title = playbackState.currentTrack?.title,
                artworkUrl = playbackState.currentTrack?.coverUrl
                    ?: playbackState.currentTrack?.album?.coverUrl,
                hasNextTrack = playbackState.currentQueueIndex in playbackState.queueTracks.indices &&
                    playbackState.currentQueueIndex < playbackState.queueTracks.lastIndex
            )
        }
    }

    val hasTrack = miniState.hasTrack
    val isPlaying = miniState.isPlaying
    val artworkUrl = miniState.artworkUrl
    val hasNextTrack = miniState.hasNextTrack
    val title = miniState.title ?: "未在播放"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable { onBarClick() }
            .liquidGlass(
                backdrop = backdrop,
                cornerRadius = cornerRadius,
                surfaceAlpha = GlassDefaults.SurfaceAlphaBright
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniPlayerArtwork(
                imageUrl = artworkUrl,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = Color.Black,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = if (hasTrack) Color.Black else Color(0xFFB8B8B8),
                modifier = Modifier
                    .size(30.dp)
                    .clickable(enabled = hasTrack) { onPlayPauseClick() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "下一首",
                tint = if (hasNextTrack) Color.Black else Color(0xFFB8B8B8),
                modifier = Modifier
                    .size(30.dp)
                    .clickable(enabled = hasNextTrack) { onNextClick() }
            )
        }
    }
}

@Composable
private fun MiniPlayerArtwork(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
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
