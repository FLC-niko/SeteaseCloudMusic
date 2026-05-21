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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.seteasecloudmusic.core.player.PlaybackState
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.feature.auth.presentation.AccountLoginSheetContent
import com.example.seteasecloudmusic.feature.main.components.UserAvatar
import com.example.seteasecloudmusic.feature.auth.presentation.AuthViewModel
import com.example.seteasecloudmusic.feature.artist.presentation.ArtistDetailRoute
import com.example.seteasecloudmusic.feature.home.presentation.DailyRecommendDetailRoute
import com.example.seteasecloudmusic.feature.home.presentation.HomeRoute
import com.example.seteasecloudmusic.feature.home.presentation.HomeViewModel
import com.example.seteasecloudmusic.feature.search.presentation.SearchRoute
import com.example.seteasecloudmusic.feature.search.presentation.SearchViewModel
import com.example.seteasecloudmusic.feature.player.presentation.NowPlayingScreen
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
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
 * @property icon 导航项显示的图标资源
 */
data class BottomNavItem(val title: String, val icon: ImageVector)

private data class SelectedArtist(
    val id: Long,
    val name: String,
    val coverUrl: String?
)

private data class DailyRecommendState(
    val tracks: List<com.example.seteasecloudmusic.core.model.Track>,
    val posterBounds: Rect
)

//底栏上方的玻璃滑块
@Composable
fun GlassSlider(
    backdrop: Backdrop,
    contentBackdrop: Backdrop,
    mainItemCount: Int,
    selectedIndex: Int,
    dragOffsetX: Float?, // 新增加：拖拽时的横坐标 (px)
    navBarHeight: Dp,
    mainBarProgress: Float,
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
                    // 让滑块在点击状态下的放大程度增大
                    val maxScale = (size.width + 32f.dp.toPx()) / size.width
                    val scale = lerp(1f, maxScale, mainBarProgress)
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val slotWidth = if (mainItemCount > 0) maxWidth / mainItemCount.toFloat() else 0.dp
            val activeMainIndex = selectedIndex.takeIf { it in 0 until mainItemCount } ?: 0
            // 设置内边距，使滑块不会产生锯齿或溢出。这里使用 0.dp 让滑块与父容器圆角完全重合并填满分段，不产生突出。
            // 也可改为 2.dp / 4.dp 来实现具有内陷感的嵌套分段控件。
            val thumbPadding = 4.dp // <-- 刚刚要求改成 4.dp
            val targetThumbWidth = (slotWidth - thumbPadding * 2).coerceAtLeast(0.dp)
            val baseOffsetX = slotWidth * activeMainIndex + thumbPadding
            
            val density = LocalDensity.current
            val targetThumbOffsetX = if (dragOffsetX != null) {
                // 如果正在拖拽，滑块中心跟随手指 X 坐标
                val fingerXDp = with(density) { dragOffsetX.toDp() }
                val halfThumb = targetThumbWidth / 2
                (fingerXDp - halfThumb).coerceIn(thumbPadding, maxWidth - targetThumbWidth - thumbPadding)
            } else {
                baseOffsetX
            }

            val tracking = dragOffsetX != null
            val animatedThumbWidth by animateDpAsState(
                targetValue = targetThumbWidth,
                // 根据是否在拖拽，切换弹簧参数实现更顺手的跟随效果
                animationSpec = if (tracking) spring(stiffness = 800f, dampingRatio = 0.8f) else spring(stiffness = 300f, dampingRatio = 0.6f),
                label = "glassThumbWidth"
            )
            val animatedThumbOffsetX by animateDpAsState(
                targetValue = targetThumbOffsetX,
                animationSpec = if (tracking) spring(stiffness = 800f, dampingRatio = 0.8f) else spring(stiffness = 300f, dampingRatio = 0.6f),
                label = "glassThumbOffset"
            )
            val thumbAlpha by animateFloatAsState(
                targetValue = if (selectedIndex in 0 until mainItemCount) 1f else 0f,
                animationSpec = spring(stiffness = 500f, dampingRatio = 0.9f),
                label = "glassThumbAlpha"
            )
            // 增加苹果风格的滑动拖拽弹性（Gooey Stretch Effect）
            // 根据目标与实际的当前动画差值，动态增加滑块的宽度，使其在运动时产生“拉丝”或加速度拉长的效果。
            val offsetDiff = targetThumbOffsetX - animatedThumbOffsetX
            val stretchFactor = 0.35f // 弹性强度
            
            val renderedOffsetX = if (offsetDiff.value < 0f) {
                // 如果是往左边滑，真正的视觉起点应该提前向左探出
                animatedThumbOffsetX + offsetDiff * stretchFactor
            } else {
                // 如果是往右边滑，起点不变，右侧宽度增加即可
                animatedThumbOffsetX
            }
            // 宽度在原有基础上叠加差距的绝对值比例
            val renderedWidth = animatedThumbWidth + offsetDiff.value.absoluteValue.dp * stretchFactor

            val thumbHeight = navBarHeight - thumbPadding * 2
            val innerCornerRadius = cornerRadius - thumbPadding

            Box(
                Modifier
                    .graphicsLayer { alpha = thumbAlpha }
                    .offset(x = renderedOffsetX)
                    .drawBackdrop(
                        backdrop = rememberCombinedBackdrop(backdrop, contentBackdrop),
                        shape = { RoundedRectangle(innerCornerRadius) },
                        effects = {
                            lens(
                                refractionHeight = 6f.dp.toPx(),
                                refractionAmount = 12f.dp.toPx(),
                                chromaticAberration = true
                            )
                        }
                    )
                    .size(animatedThumbWidth, thumbHeight)
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
    val authViewModel: AuthViewModel = hiltViewModel()

    val searchUiState by searchViewModel.uiState.collectAsState()
    val playbackState by searchViewModel.playbackState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()

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

    // 记录当前手指 X 坐标准备拖拽交互
    var dragOffsetX by remember { mutableStateOf<Float?>(null) }

    // 记录当前选中的导航项：
    // 0~2 对应左侧主导航条，3 对应右侧搜索按钮。
    var selectedIndex by remember { mutableIntStateOf(0) }

    // 左侧主导航条目前承载三个一级入口。
    val mainNavItems = listOf(
        BottomNavItem("主页", Icons.Filled.Home),
        BottomNavItem("电台", Icons.Filled.Radio),
        BottomNavItem("我的", Icons.Filled.Person)
    )

    // 抽成共享布局参数，保证底栏和滑块按同一套比例计算。
    val horizontalPadding = 24.dp
    val navBarHeight = 60.dp
    val mainSearchGap = 16.dp
    val searchButtonWidth = navBarHeight
    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val pageTitle = when (selectedIndex) {
        0 -> "主页"
        1 -> "电台"
        2 -> "我的"
        3 -> "搜索"
        else -> "主页"
    }
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
            // 根据 selectedIndex 显示不同页面
            when (selectedIndex) {
                0 -> HomeRoute(
                    topContentPadding = searchContentTopPadding,
                    bottomContentPadding = 180.dp + animatedImeOffset,
                    onPosterWallClick = { tracks, bounds ->
                        dailyRecommendState = DailyRecommendState(tracks, bounds)
                    }
                )
                1 -> AppPageBackground() // 电台
                2 -> AppPageBackground() // 我的
                3 -> SearchRoute(
                    viewModel = searchViewModel,
                    topContentPadding = searchContentTopPadding,
                    bottomContentPadding = 180.dp + animatedImeOffset,
                    onArtistClick = { artistId, artistName, artistCoverUrl ->
                        selectedArtist = SelectedArtist(
                            id = artistId,
                            name = artistName,
                            coverUrl = artistCoverUrl
                        )
                    }
                ) // 搜索
                else -> AppPageBackground()
            }
        }

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
                .padding(start = 24.dp, top = 16.dp)
                .graphicsLayer { alpha = 1f - sinkProgress }
        ) { animatedTitle ->
            LargePageTitle(title = animatedTitle)
        }

        UserAvatarButton(
            avatarUrl = authUiState.authSession?.avatarUrl,
            displayName = authUiState.authSession?.nickname,
            onClick = {
                showAccountSheet = true
                onAvatarClick?.invoke()
            },
            enabled = true,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(end = 24.dp, top = 12.dp)
                .graphicsLayer { alpha = 1f - sinkProgress }
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

            val isSearchExpanded = selectedIndex == 3

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
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(cornerRadius) },
                            effects = {
                                vibrancy()
                                blur(2f.dp.toPx())
                                lens(16f.dp.toPx(), 32f.dp.toPx())
                            },
                            layerBlock = {
                                val progress = mainBarProgressAnimation.value
                                val maxScale = (size.width + 8f.dp.toPx()) / size.width
                                val scale = lerp(1f, maxScale, progress)
                                scaleX = scale
                                scaleY = scale
                            },
                            // 半透明白色表面让底层色块在模糊后保留一点“磨砂感”。
                            onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) },
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (isSearchExpanded) {
                                selectedIndex = 0
                            }
                        }
                        .pointerInput(mainBarAnimationScope, isSearchExpanded, mainNavItems.size) {
                            if (isSearchExpanded) return@pointerInput // 当搜索展开时，直接通过 clickable 重置，不响应滑动切换
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                var currentX = down.position.x
                                dragOffsetX = currentX

                                val updateSelection = { x: Float ->
                                    val slotWidthPx = if (mainNavItems.isNotEmpty()) size.width.toFloat() / mainNavItems.size.toFloat() else 0f
                                    if (slotWidthPx > 0f) {
                                        val newIndex = (x / slotWidthPx).toInt().coerceIn(0, mainNavItems.size - 1)
                                        selectedIndex = newIndex
                                    }
                                }
                                // 第一次按下时立即响应切换选中状态
                                updateSelection(currentX)

                                mainBarAnimationScope.launch { mainBarProgressAnimation.animateTo(1f, animationSpec) }
                                
                                var inGesture = true
                                while (inGesture) {
                                    val event = awaitPointerEvent()
                                    val dragEvent = event.changes.firstOrNull()
                                    if (dragEvent != null && dragEvent.pressed) {
                                        currentX = dragEvent.position.x
                                        dragOffsetX = currentX
                                        updateSelection(currentX) // 拖拽时动态更新选中项
                                        dragEvent.consume() // 消耗事件防止底层组件响应
                                    } else {
                                        inGesture = false
                                    }
                                }
                                
                                dragOffsetX = null
                                mainBarAnimationScope.launch { mainBarProgressAnimation.animateTo(0f, animationSpec) }
                            }
                        }
                ) {
                    // 主导航栏内部负责均分三个一级入口。
                    // 容器缩小时（圆按钮）不显示导航图标，避免挤压
                    if (leftWidth > collapsedWidth * 1.5f) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            mainNavItems.forEachIndexed { index, item ->
                                val isSelected = selectedIndex == index
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
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { RoundedRectangle(cornerRadius) },
                                    effects = {
                                        vibrancy()
                                        blur(2f.dp.toPx())
                                        lens(16f.dp.toPx(), 32f.dp.toPx())
                                    },
                                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.56f)) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = searchUiState.query,
                                onValueChange = { searchViewModel.onQueryChanged(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp)
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.key == Key.Enter) {
                                            searchViewModel.onSearchSubmit()
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
                                            if (searchUiState.query.isEmpty()) {
                                                Text("搜你想听的", color = Color.Gray)
                                            }
                                            innerTextField()
                                        }
                                        if (searchUiState.query.isNotEmpty()) {
                                            Icon(
                                                imageVector = Icons.Filled.Clear,
                                                contentDescription = "清空搜索",
                                                tint = Color.Gray,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clickable { searchViewModel.onClearQuery() }
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(navBarHeight)
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { RoundedRectangle(cornerRadius) },
                                    effects = {
                                        vibrancy()
                                        blur(2f.dp.toPx())
                                        lens(16f.dp.toPx(), 32f.dp.toPx())
                                    },
                                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.56f)) }
                                )
                                .clickable(
                                    interactionSource = searchInteractionSource,
                                    indication = null
                                ) {
                                    searchViewModel.onClearQuery()
                                    selectedIndex = 0
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
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(cornerRadius) },
                                effects = {
                                    vibrancy()
                                    blur(2f.dp.toPx())
                                    lens(16f.dp.toPx(), 32f.dp.toPx())
                                },
                                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.56f)) }
                            )
                            .clickable(
                                interactionSource = searchInteractionSource,
                                indication = null
                            ) {
                                if (!isSearchExpanded) {
                                    selectedIndex = 3
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
            playbackState = playbackState,
            onPlayPauseClick = { searchViewModel.onMiniPlayerPlayPause() },
            onNextClick = { searchViewModel.onMiniPlayerNext() },
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
            mainItemCount = mainNavItems.size,
            selectedIndex = selectedIndex,
            dragOffsetX = dragOffsetX,
            navBarHeight = navBarHeight,
            mainBarProgress = mainBarProgressAnimation.value,
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
                onClose = { showNowPlaying = false }
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
            animationSpec = spring(stiffness = 550f, dampingRatio = 0.82f),
            label = "accountPanelDragOffset"
        )
        val effectiveDragOffsetPx = if (isDragging) dragOffsetPx else settledDragOffsetPx
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
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 360f)
                )
            } else {
                panelOffsetFraction.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.88f, stiffness = 460f)
                )
                onDismissed()
            }
        }

        val openProgress = (1f - panelOffsetFraction.value).coerceIn(0f, 1f)
        val dragProgress = (effectiveDragOffsetPx / panelHeightPx).coerceIn(0f, 1f)
        val visualProgress = (openProgress * (1f - dragProgress * 0.35f)).coerceIn(0f, 1f)
        val panelOffsetY = panelOffsetFraction.value * hiddenOffsetPx + effectiveDragOffsetPx
        val panelShape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)

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
                        drawRect(Color.Black.copy(alpha = 0.32f * visualProgress))
                    }
                )
                .clickable(
                    enabled = visible,
                    interactionSource = scrimInteractionSource,
                    indication = null,
                    onClick = onDismissRequest
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .offset { IntOffset(0, panelOffsetY.roundToInt()) }
                .graphicsLayer {
                    val scaleXTarget = 1f - dragProgress * 0.018f
                    val scaleYTarget = 1f - dragProgress * 0.03f
                    scaleX = scaleXTarget
                    scaleY = scaleYTarget
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
        color = Color.Black,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1).sp,
        fontWeight = FontWeight.Black,
        modifier = modifier
    )
}

@Composable
private fun UserAvatarButton(
    avatarUrl: String?,
    displayName: String?,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val hasDisplayName = !displayName.isNullOrBlank()
    val hasAvatar = !avatarUrl.isNullOrBlank()
    val isGuest = !hasAvatar && !hasDisplayName
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = 760f, dampingRatio = 0.76f),
        label = "avatarButtonScale"
    )
    val indication = LocalIndication.current

    Box(
        modifier = modifier
            .size(46.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .indication(interactionSource = interactionSource, indication = indication)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        UserAvatar(
            avatarUrl = avatarUrl,
            displayName = displayName,
            size = 46.dp,
            showBorder = !isGuest,
            borderWidth = if (isGuest) 0.dp else 1.dp,
            isGuest = isGuest
        )
    }
}

/**
 * 搜索模式底部迷你播放条。
 */
@Composable
private fun SearchMiniPlayerBar(
    backdrop: Backdrop,
    cornerRadius: Dp,
    playbackState: PlaybackState,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onBarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasTrack = playbackState.currentTrack != null
    val isPlaying = playbackState.status == PlayerStatus.PLAYING
    val artworkUrl = playbackState.currentTrack?.coverUrl ?: playbackState.currentTrack?.album?.coverUrl
    val hasNextTrack =
        playbackState.currentQueueIndex in playbackState.queueTracks.indices &&
            playbackState.currentQueueIndex < playbackState.queueTracks.lastIndex

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable { onBarClick() }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(cornerRadius) },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(16f.dp.toPx(), 32f.dp.toPx())
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.56f)) }
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
                text = playbackState.currentTrack?.title ?: "未在播放",
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
