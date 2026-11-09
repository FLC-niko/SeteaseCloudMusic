package com.example.seteasecloudmusic.feature.mine.presentation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seteasecloudmusic.core.ui.adaptive.AdaptiveModalSheet
import com.kyant.backdrop.Backdrop

/**
 * 更多操作弹窗层级定义
 */
private enum class PlaylistMoreMenuLevel {
    MAIN,
    WALLPAPER
}

/**
 * 歌单详情页「更多操作」底栏（类似播放列表 PlaylistQueueSheet 从底部冒出贴底展示）：
 * 一级菜单包含「歌单壁纸」（二级菜单入口）及「歌曲排序」等扩展功能；
 * 歌单壁纸二级菜单支持在大封面墙、静态海报墙、动态海报墙之间切换，关闭键统一使用主题色红。
 */
@Composable
fun PlaylistMoreActionSheet(
    currentStyle: PlaylistDetailStyle,
    useColorBackground: Boolean,
    onToggleColorBackground: (Boolean) -> Unit,
    onSelectStyle: (PlaylistDetailStyle) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentLevel by remember { mutableStateOf(PlaylistMoreMenuLevel.MAIN) }

    // 当处于二级菜单（歌单壁纸）时，点击物理返回键优先返回一级菜单
    BackHandler(enabled = currentLevel == PlaylistMoreMenuLevel.WALLPAPER) {
        currentLevel = PlaylistMoreMenuLevel.MAIN
    }

    AdaptiveModalSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            AnimatedContent(
                targetState = currentLevel,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "playlistMoreSheetLevelTransition"
            ) { level ->
                when (level) {
                    PlaylistMoreMenuLevel.MAIN -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // 头部：仅展示标题，去除右上角的叉号
                            Text(
                                text = "更多操作",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1E24),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // 1. 歌单壁纸（二级菜单入口）
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFFF5F5F7))
                                    .border(
                                        width = 0.5.dp,
                                        color = Color.Black.copy(alpha = 0.04f),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .clickable {
                                        currentLevel = PlaylistMoreMenuLevel.WALLPAPER
                                    }
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFA233B).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Wallpaper,
                                        contentDescription = "歌单壁纸",
                                        tint = Color(0xFFFA233B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Text(
                                    text = "歌单壁纸",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E1E24)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    text = currentStyle.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        color = Color(0xFF8E8E93)
                                    )
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFFC7C7CC),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // 2. 歌曲排序（待实现功能入口）
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFFF5F5F7))
                                    .border(
                                        width = 0.5.dp,
                                        color = Color.Black.copy(alpha = 0.04f),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .clickable {
                                        Toast.makeText(context, "歌曲排序功能即将推出", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF007AFF).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "歌曲排序",
                                        tint = Color(0xFF007AFF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Text(
                                    text = "歌曲排序",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E1E24)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    text = "默认",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        color = Color(0xFF8E8E93)
                                    )
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFFC7C7CC),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 底部主题红关闭按钮
                            Button(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFA233B),
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "关闭",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    PlaylistMoreMenuLevel.WALLPAPER -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // 头部：仅展示标题，去除返回和关闭按钮
                            Text(
                                text = "歌单壁纸",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1E24),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // 1. 取色背景开关选项
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFFF5F5F7))
                                    .border(
                                        width = 0.5.dp,
                                        color = Color.Black.copy(alpha = 0.04f),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .clickable {
                                        onToggleColorBackground(!useColorBackground)
                                    }
                                    .padding(horizontal = 18.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "取色背景",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1E1E24)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (useColorBackground) "根据封面智能取色沉浸背景" else "已恢复纯白背景与灰色遮罩",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            color = Color(0xFF8E8E93)
                                        )
                                    )
                                }

                                Switch(
                                    checked = useColorBackground,
                                    onCheckedChange = onToggleColorBackground,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFFA233B),
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color(0xFFE5E5EA),
                                        uncheckedBorderColor = Color.Transparent
                                    )
                                )
                            }

                            // 2. 歌单壁纸选项列表（仅保留标题，去除描述，选中高亮红钩）
                            PlaylistDetailStyle.values().forEach { style ->
                                val isSelected = style == currentStyle

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            if (isSelected) Color(0xFFFA233B).copy(alpha = 0.10f)
                                            else Color(0xFFF5F5F7)
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.5.dp,
                                            color = if (isSelected) Color(0xFFFA233B) else Color.Black.copy(alpha = 0.04f),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .clickable {
                                            onSelectStyle(style)
                                        }
                                        .padding(horizontal = 18.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = style.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 16.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) Color(0xFFFA233B) else Color(0xFF1E1E24)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFA233B)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "已选中",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 底部操作区：返回上一级 + 主题红关闭
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = { currentLevel = PlaylistMoreMenuLevel.MAIN },
                                    shape = RoundedCornerShape(24.dp),
                                    color = Color(0xFFF2F2F7),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "返回",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1E1E24)
                                        )
                                    }
                                }

                                Button(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFA233B),
                                        contentColor = Color.White
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = "关闭",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 兼容旧命名的别名函数（自动转为贴底底栏）
 */
@Composable
fun PlaylistMoreActionDialog(
    isVisible: Boolean,
    currentStyle: PlaylistDetailStyle,
    onSelectStyle: (PlaylistDetailStyle) -> Unit,
    onDismiss: () -> Unit,
    useColorBackground: Boolean = true,
    onToggleColorBackground: (Boolean) -> Unit = {},
    backdrop: Backdrop? = null
) {
    if (isVisible) {
        PlaylistMoreActionSheet(
            currentStyle = currentStyle,
            useColorBackground = useColorBackground,
            onToggleColorBackground = onToggleColorBackground,
            onSelectStyle = onSelectStyle,
            onDismiss = onDismiss
        )
    }
}

/**
 * 兼容旧命名的别名函数（自动转为贴底底栏）
 */
@Composable
fun PlaylistStyleSelectorDialog(
    isVisible: Boolean,
    currentStyle: PlaylistDetailStyle,
    onSelectStyle: (PlaylistDetailStyle) -> Unit,
    onDismiss: () -> Unit,
    useColorBackground: Boolean = true,
    onToggleColorBackground: (Boolean) -> Unit = {},
    backdrop: Backdrop? = null
) {
    if (isVisible) {
        PlaylistMoreActionSheet(
            currentStyle = currentStyle,
            useColorBackground = useColorBackground,
            onToggleColorBackground = onToggleColorBackground,
            onSelectStyle = onSelectStyle,
            onDismiss = onDismiss
        )
    }
}
