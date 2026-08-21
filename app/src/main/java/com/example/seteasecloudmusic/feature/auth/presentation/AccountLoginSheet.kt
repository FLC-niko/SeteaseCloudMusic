package com.example.seteasecloudmusic.feature.auth.presentation

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.seteasecloudmusic.feature.main.components.UserAvatar
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun AccountLoginSheetContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val contentScrollState = rememberScrollState()

    LaunchedEffect(viewModel.snackbarMessage) {
        viewModel.snackbarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(viewModel.dismissSheet) {
        viewModel.dismissSheet.collectLatest {
            onDismiss()
        }
    }

    val red = Color(0xFFFA233B)
    val secondary = Color(0xFF8D8D93)

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(contentScrollState)
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 76.dp)
        ) {
            Header(onDismiss = onDismiss)

            Spacer(modifier = Modifier.height(14.dp))

            Crossfade(
                targetState = uiState.panel,
                animationSpec = tween(220),
                label = "accountPanelTransition",
                modifier = Modifier.fillMaxWidth()
            ) { current ->
                when (current) {
                    AuthPanel.METHODS -> {
                        MethodSelectionPanel(
                            red = red,
                            secondary = secondary,
                            isLoggedIn = uiState.isLoggedIn,
                            avatarUrl = uiState.authSession?.avatarUrl,
                            displayName = uiState.authSession?.nickname,
                            isDarkModeEnabled = uiState.isDarkModeEnabled,
                            onCaptchaClick = { viewModel.onCaptchaPanelOpened() },
                            onQrClick = { viewModel.onQrPanelOpened() },
                            onEmailClick = { scope.launch { snackbarHostState.showSnackbar("邮箱登录开发中") } },
                            onWechatClick = { scope.launch { snackbarHostState.showSnackbar("微信登录开发中") } },
                            onQqClick = { scope.launch { snackbarHostState.showSnackbar("QQ 登录开发中") } },
                            onWeiboClick = { scope.launch { snackbarHostState.showSnackbar("微博登录开发中") } },
                            onDarkModeToggled = { viewModel.onDarkModeToggled(it) },
                            onLogoutClick = { viewModel.onRequestLogout() },
                            onSettingsClick = { viewModel.onOpenSettings() },
                            onPlaceholderClick = { label ->
                                scope.launch { snackbarHostState.showSnackbar("$label 开发中") }
                            }
                        )
                    }

                    AuthPanel.CAPTCHA -> {
                        CaptchaPanel(
                            red = red,
                            helperText = uiState.errorMessage,
                            phone = uiState.phone,
                            captcha = uiState.captcha,
                            isLoading = uiState.isLoading,
                            onPhoneChange = { viewModel.onPhoneChanged(it) },
                            onCaptchaChange = { viewModel.onCaptchaChanged(it) },
                            onBack = { viewModel.onBackToMethods() },
                            onSendCaptcha = { viewModel.onSendCaptcha() },
                            onLogin = { viewModel.onCaptchaLogin() }
                        )
                    }

                    AuthPanel.QR -> {
                        QrPanel(
                            red = red,
                            secondary = secondary,
                            qrHint = uiState.qrHint,
                            qrImageBase64 = uiState.qrLoginStart?.qrImageBase64,
                            isLoading = uiState.isLoading,
                            onBack = { viewModel.onBackToMethods() },
                            onRefresh = { viewModel.onRefreshQr() }
                        )
                    }
                }
            }
        }

        if (uiState.showLogoutConfirmDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onDismissLogoutConfirm() },
                title = {
                    Text(
                        text = "退出登录",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(text = "确定要退出当前账号吗？退出后你仍可随时重新登录。")
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.onConfirmLogout() }) {
                        Text(text = "退出", color = red, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onDismissLogoutConfirm() }) {
                        Text(text = "取消", color = Color(0xFF5F5F67))
                    }
                }
            )
        }

        if (uiState.showSettingsDialog) {
            SettingsDialog(
                maxCacheMb = uiState.maxCacheMb,
                currentCacheMb = uiState.currentCacheMb,
                onSetMaxCacheMb = { viewModel.onSetMaxCacheMb(it) },
                onClearCache = { viewModel.onClearAudioCache() },
                onDismiss = { viewModel.onDismissSettings() }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun Header(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .size(44.dp)
                .background(Color(0xFFF0F0F4), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "关闭",
                tint = Color(0xFF1C1C1E)
            )
        }
    }
}

@Composable
private fun SettingsSectionCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun SettingsDivider(color: Color) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = color,
        thickness = 1.dp
    )
}

@Composable
private fun SettingsNavigationRow(
    title: String,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF121212),
            modifier = Modifier.weight(1f)
        )
        if (!trailingText.isNullOrBlank()) {
            Text(
                text = trailingText,
                fontSize = 16.sp,
                color = Color(0xFF8D8D93)
            )
            Spacer(modifier = Modifier.size(4.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFC7C7CC),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    backdrop: Backdrop?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF121212),
            modifier = Modifier.weight(1f)
        )
        LiquidGlassSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            backdrop = backdrop
        )
    }
}

@Composable
private fun LiquidGlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackWidth = 51.dp
    val trackHeight = 31.dp
    val thumbSize = 27.dp
    val thumbPadding = 2.dp

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val trackColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF30D158) else Color(0xFF3A3A3C),
        animationSpec = tween(durationMillis = 170),
        label = "liquidSwitchTrackColor"
    )
    val thumbOffsetX by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - thumbPadding else thumbPadding,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 640f),
        label = "liquidSwitchThumbOffset"
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 850f),
        label = "liquidSwitchThumbScale"
    )

    val baseModifier = modifier
        .size(trackWidth, trackHeight)
        .clip(CircleShape)
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null
        ) {
            onCheckedChange(!checked)
        }

    val decoratedModifier = if (backdrop != null) {
        baseModifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedRectangle(trackHeight / 2) },
            effects = {
                vibrancy()
                blur(1.2f.dp.toPx())
                lens(
                    refractionHeight = 2.2f.dp.toPx(),
                    refractionAmount = 4.5f.dp.toPx(),
                    chromaticAberration = false
                )
            },
            onDrawSurface = {
                drawRect(trackColor)
            }
        )
    } else {
        baseModifier.background(trackColor, CircleShape)
    }

    Box(
        modifier = decoratedModifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffsetX)
                .size(thumbSize)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .shadow(
                    elevation = if (isPressed) 1.dp else 2.dp,
                    shape = CircleShape,
                    clip = false
                )
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
private fun MethodSelectionPanel(
    red: Color,
    secondary: Color,
    isLoggedIn: Boolean,
    avatarUrl: String?,
    displayName: String?,
    isDarkModeEnabled: Boolean,
    onCaptchaClick: () -> Unit,
    onQrClick: () -> Unit,
    onEmailClick: () -> Unit,
    onWechatClick: () -> Unit,
    onQqClick: () -> Unit,
    onWeiboClick: () -> Unit,
    onDarkModeToggled: (Boolean) -> Unit,
    onLogoutClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPlaceholderClick: (String) -> Unit
) {
    if (isLoggedIn) {
        ProfileHomePanel(
            red = red,
            secondary = secondary,
            avatarUrl = avatarUrl,
            displayName = displayName,
            isDarkModeEnabled = isDarkModeEnabled,
            onDarkModeToggled = onDarkModeToggled,
            onLogoutClick = onLogoutClick,
            onSettingsClick = onSettingsClick,
            onPlaceholderClick = onPlaceholderClick
        )
    } else {
        LoginHomePanel(
            red = red,
            secondary = secondary,
            onCaptchaClick = onCaptchaClick,
            onQrClick = onQrClick,
            onEmailClick = onEmailClick,
            onWechatClick = onWechatClick,
            onQqClick = onQqClick,
            onWeiboClick = onWeiboClick
        )
    }
}

@Composable
private fun LoginHomePanel(
    red: Color,
    secondary: Color,
    onCaptchaClick: () -> Unit,
    onQrClick: () -> Unit,
    onEmailClick: () -> Unit,
    onWechatClick: () -> Unit,
    onQqClick: () -> Unit,
    onWeiboClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UserAvatar(
            avatarUrl = null,
            displayName = null,
            size = 88.dp,
            isGuest = true
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "登录 Setease",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF121212)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "同步你的收藏、播放记录和个性化推荐",
            fontSize = 15.sp,
            color = secondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        LoginMethodButton(
            text = "手机号登录",
            iconVector = Icons.Filled.Sms,
            containerColor = red,
            contentColor = Color.White,
            onClick = onCaptchaClick
        )

        Spacer(modifier = Modifier.height(10.dp))

        LoginMethodButton(
            text = "二维码登录",
            iconVector = Icons.Filled.QrCode2,
            containerColor = Color.White,
            contentColor = Color(0xFF121212),
            onClick = onQrClick
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "其他登录方式",
            fontSize = 13.sp,
            color = secondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SocialLoginButton(
                text = "邮箱",
                iconVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                onClick = onEmailClick
            )
            SocialLoginButton(
                text = "微信",
                iconVector = Icons.Filled.Person,
                onClick = onWechatClick
            )
            SocialLoginButton(
                text = "QQ",
                iconVector = Icons.Filled.Person,
                onClick = onQqClick
            )
            SocialLoginButton(
                text = "微博",
                iconVector = Icons.Filled.Person,
                onClick = onWeiboClick
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "登录即代表你同意《用户协议》和《隐私政策》",
            fontSize = 12.sp,
            color = secondary,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun LoginMethodButton(
    text: String,
    iconVector: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = text,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun SocialLoginButton(
    text: String,
    iconVector: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = Color(0xFF121212),
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color(0xFF8D8D93)
        )
    }
}

@Composable
private fun ProfileHomePanel(
    red: Color,
    secondary: Color,
    avatarUrl: String?,
    displayName: String?,
    isDarkModeEnabled: Boolean,
    onDarkModeToggled: (Boolean) -> Unit,
    onLogoutClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPlaceholderClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        UserInfoCard(
            avatarUrl = avatarUrl,
            displayName = displayName
        )

        Spacer(modifier = Modifier.height(16.dp))

        StatsRow(secondary = secondary)

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSectionCard {
            ProfileMenuItem(
                title = "我的消息",
                iconVector = Icons.Filled.Sms,
                onClick = { onPlaceholderClick("我的消息") }
            )
            SettingsDivider(Color(0xFFE2E2E8))
            ProfileMenuItem(
                title = "本地音乐",
                iconVector = Icons.Filled.Settings,
                onClick = { onPlaceholderClick("本地音乐") }
            )
            SettingsDivider(Color(0xFFE2E2E8))
            ProfileMenuItem(
                title = "下载管理",
                iconVector = Icons.Filled.Settings,
                onClick = { onPlaceholderClick("下载管理") }
            )
            SettingsDivider(Color(0xFFE2E2E8))
            ProfileMenuItem(
                title = "我的收藏",
                iconVector = Icons.Filled.Settings,
                onClick = { onPlaceholderClick("我的收藏") }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsSectionCard {
            ProfileMenuItem(
                title = "云盘",
                iconVector = Icons.Filled.Settings,
                onClick = { onPlaceholderClick("云盘") }
            )
            SettingsDivider(Color(0xFFE2E2E8))
            ProfileMenuItem(
                title = "已购",
                iconVector = Icons.Filled.Settings,
                onClick = { onPlaceholderClick("已购") }
            )
            SettingsDivider(Color(0xFFE2E2E8))
            ProfileMenuItem(
                title = "设置",
                iconVector = Icons.Filled.Settings,
                onClick = onSettingsClick
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsSectionCard {
            SettingsToggleRow(
                title = "夜间模式",
                checked = isDarkModeEnabled,
                onCheckedChange = onDarkModeToggled,
                backdrop = null
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onLogoutClick)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "退出登录",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = red
                )
            }
        }
    }
}

@Composable
private fun SettingsDialog(
    maxCacheMb: Int,
    currentCacheMb: Float,
    onSetMaxCacheMb: (Int) -> Unit,
    onClearCache: () -> Unit,
    onDismiss: () -> Unit
) {
    val cacheOptions = listOf(100, 200, 500, 1024)

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "设置",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFE5E5EA), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "关闭",
                            tint = Color(0xFF1C1C1E),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "播放与缓存",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                )

                SettingsSectionCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "歌曲缓存上限",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1C1C1E)
                            )
                            Text(
                                text = "${maxCacheMb}MB",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFA233B)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            cacheOptions.forEach { size ->
                                val isSelected = maxCacheMb == size
                                val label = if (size >= 1024) "1GB" else "${size}MB"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (isSelected) Color(0xFFFA233B) else Color(0xFFF2F2F7),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onSetMaxCacheMb(size) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color(0xFF1C1C1E)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        SettingsDivider(Color(0xFFE2E2E8))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "当前缓存占用",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1C1C1E)
                                )
                                Text(
                                    text = String.format(java.util.Locale.getDefault(), "%.1f MB", currentCacheMb),
                                    fontSize = 13.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                            TextButton(
                                onClick = onClearCache,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFA233B))
                            ) {
                                Text(
                                    text = "清理缓存",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun UserInfoCard(
    avatarUrl: String?,
    displayName: String?
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                avatarUrl = avatarUrl,
                displayName = displayName,
                size = 72.dp,
                showBorder = false
            )
            Spacer(modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName ?: "Setease 用户",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF121212)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "VIP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF8F00)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(
    secondary: Color
) {
    val stats = listOf(
        "我的喜欢" to "0",
        "最近播放" to "0",
        "我的歌单" to "0",
        "下载" to "0"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            stats.forEach { (label, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = value,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF121212)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    title: String,
    iconVector: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(Color(0xFFF2F2F7), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = Color(0xFFFA233B),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF121212),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFC7C7CC),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun CaptchaPanel(
    red: Color,
    helperText: String?,
    phone: String,
    captcha: String,
    isLoading: Boolean,
    onPhoneChange: (String) -> Unit,
    onCaptchaChange: (String) -> Unit,
    onBack: () -> Unit,
    onSendCaptcha: () -> Unit,
    onLogin: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        PanelTitle(
            title = "手机验证码登录",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+86",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF121212),
                    modifier = Modifier.padding(end = 12.dp)
                )
                TextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    placeholder = { Text("请输入手机号") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color(0xFFE8E8ED),
                thickness = 1.dp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = captcha,
                    onValueChange = onCaptchaChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("请输入验证码") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = onSendCaptcha,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = red),
                    enabled = !isLoading,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("发送", fontSize = 13.sp)
                    }
                }
            }

            Button(
                onClick = onLogin,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = red),
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("登录", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (!helperText.isNullOrBlank()) {
            Text(
                text = helperText,
                color = red,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun QrPanel(
    red: Color,
    secondary: Color,
    qrHint: String,
    qrImageBase64: String?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    val imageBitmap = remember(qrImageBase64) {
        qrImageBase64?.let { raw ->
            val base64 = raw.substringAfter(",", raw)
            runCatching {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        PanelTitle(
            title = "二维码登录",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .background(Color(0xFFF3F3F7), RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFFE5E5EA), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "登录二维码",
                            modifier = Modifier.size(170.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.QrCode2,
                            contentDescription = null,
                            tint = Color(0xFF1F1F21),
                            modifier = Modifier.size(86.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "打开网易云音乐 App，扫描下方二维码登录",
                    color = Color(0xFF121212),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = qrHint,
                    color = secondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("刷新二维码")
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelTitle(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color(0xFF121212)
            )
        }
        Text(
            text = title,
            color = Color(0xFF121212),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
