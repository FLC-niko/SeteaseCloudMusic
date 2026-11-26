package com.example.seteasecloudmusic.feature.mine.presentation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seteasecloudmusic.R
import com.example.seteasecloudmusic.core.ui.adaptive.AdaptiveModalSheet
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.RadioButtonLocation
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

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
 * 歌单壁纸二级菜单支持在大封面墙、静态海报墙、动态海报墙、全屏封面之间切换，关闭键统一使用主题色红。
 */
@Composable
fun PlaylistMoreActionSheet(
    currentStyle: PlaylistDetailStyle,
    useColorBackground: Boolean,
    onToggleColorBackground: (Boolean) -> Unit,
    onSelectStyle: (PlaylistDetailStyle) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onEnterEditMode: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var currentLevel by remember { mutableStateOf(PlaylistMoreMenuLevel.MAIN) }
    val colors = AppTheme.colors

    // 当处于二级菜单（歌单壁纸）时，点击物理返回键优先返回一级菜单
    BackHandler(enabled = currentLevel == PlaylistMoreMenuLevel.WALLPAPER) {
        currentLevel = PlaylistMoreMenuLevel.MAIN
    }

    AdaptiveModalSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
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
                            Text(
                                text = "更多操作",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Card(
                                colors = CardDefaults.defaultColors(color = colors.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ArrowPreference(
                                    title = "歌单壁纸",
                                    startAction = {
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 14.dp)
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(colors.accentSubtle),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_wallpaper),
                                                contentDescription = "歌单壁纸",
                                                tint = colors.accent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    endActions = {
                                        Text(
                                            text = currentStyle.title,
                                            fontSize = 14.sp,
                                            color = colors.textSecondary
                                        )
                                    },
                                    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                    onClick = { currentLevel = PlaylistMoreMenuLevel.WALLPAPER }
                                )

                                ArrowPreference(
                                    title = "歌曲排序",
                                    startAction = {
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 14.dp)
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(colors.accentBlue.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = MiuixIcons.Sort,
                                                contentDescription = "歌曲排序",
                                                tint = colors.accentBlue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    endActions = {
                                        Text(
                                            text = "默认",
                                            fontSize = 14.sp,
                                            color = colors.textSecondary
                                        )
                                    },
                                    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                    onClick = {
                                        if (onEnterEditMode != null) {
                                            onDismiss()
                                            onEnterEditMode()
                                        } else {
                                            Toast.makeText(context, "歌曲排序功能即将推出", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Button(
                                onClick = onDismiss,
                                cornerRadius = 24.dp,
                                colors = ButtonDefaults.buttonColorsPrimary(
                                    color = colors.accent,
                                    contentColor = Color.White
                                ),
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
                            Text(
                                text = "歌单壁纸",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Card(
                                colors = CardDefaults.defaultColors(color = colors.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SwitchPreference(
                                    title = "取色背景",
                                    summary = if (useColorBackground) "根据封面智能取色沉浸背景" else "已恢复默认纯色背景与遮罩",
                                    checked = useColorBackground,
                                    onCheckedChange = onToggleColorBackground
                                )
                            }

                            Card(
                                colors = CardDefaults.defaultColors(color = colors.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                PlaylistDetailStyle.entries.forEach { style ->
                                    val isSelected = style == currentStyle
                                    RadioButtonPreference(
                                        title = style.title,
                                        selected = isSelected,
                                        radioButtonLocation = RadioButtonLocation.End,
                                        onClick = { onSelectStyle(style) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    text = "返回",
                                    onClick = { currentLevel = PlaylistMoreMenuLevel.MAIN },
                                    cornerRadius = 24.dp,
                                    colors = ButtonDefaults.textButtonColors(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                )

                                Button(
                                    onClick = onDismiss,
                                    cornerRadius = 24.dp,
                                    colors = ButtonDefaults.buttonColorsPrimary(
                                        color = colors.accent,
                                        contentColor = Color.White
                                    ),
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
