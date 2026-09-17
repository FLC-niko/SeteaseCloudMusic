package com.example.seteasecloudmusic.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 为纵向滚动内容提供轻量的边界缓冲。
 *
 * 只有子滚动容器已经到达顶部或底部后仍有手指拖动时，页面才会产生带阻尼的位移；
 * 松手后页面回到原位。这个修饰符不包含刷新阈值、刷新回调或加载状态。
 */
fun Modifier.verticalElasticOverscroll(): Modifier = composed {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val maximumOffsetPx = remember(density) { with(density) { 72.dp.toPx() } }

    var offsetY by remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }

    val connection = remember(maximumOffsetPx, scope) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || offsetY == 0f) return Offset.Zero

                // 手指反向移动时，先收回已经拉出的缓冲距离，再交给列表继续滚动。
                val isReturning = available.y * offsetY < 0f
                if (!isReturning) return Offset.Zero

                settleJob?.cancel()
                val consumedY = when {
                    offsetY > 0f -> available.y.coerceAtLeast(-offsetY)
                    else -> available.y.coerceAtMost(-offsetY)
                }
                offsetY += consumedY
                return Offset(x = 0f, y = consumedY)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source != NestedScrollSource.UserInput || available.y == 0f) return Offset.Zero

                settleJob?.cancel()
                val distanceFraction = (kotlin.math.abs(offsetY) / maximumOffsetPx).coerceIn(0f, 1f)
                val resistance = 0.34f * (1f - distanceFraction * 0.72f)
                offsetY = (offsetY + available.y * resistance)
                    .coerceIn(-maximumOffsetPx, maximumOffsetPx)

                // 接管已到边界后的剩余位移，避免再叠加系统默认的边缘拉伸或光晕。
                return Offset(x = 0f, y = available.y)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                settleJob?.cancel()
                if (offsetY != 0f) {
                    val startOffset = offsetY
                    settleJob = scope.launch {
                        Animatable(startOffset).animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = 0.82f,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) {
                            offsetY = value
                        }
                        offsetY = 0f
                    }
                }
                return Velocity.Zero
            }
        }
    }

    this
        .graphicsLayer { translationY = offsetY }
        .nestedScroll(connection)
}
