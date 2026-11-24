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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import com.example.seteasecloudmusic.core.settings.AppThemeMode
import com.example.seteasecloudmusic.core.settings.OnlineAudioQuality
import com.example.seteasecloudmusic.core.ui.adaptive.AdaptiveAlertDialog
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.seteasecloudmusic.core.ui.components.BloomStrokeDefaults
import com.example.seteasecloudmusic.core.ui.components.UserAvatar
import com.example.seteasecloudmusic.core.ui.components.bloomStroke
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AccountLoginSheetContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val contentScrollState = rememberScrollState()
    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }
    val audioQuality by viewModel.playerSettingsManager.audioQuality.collectAsStateWithLifecycle()
    val themeMode by viewModel.playerSettingsManager.themeMode.collectAsStateWithLifecycle()
    val bluetoothLyricsEnabled by viewModel.playerSettingsManager.bluetoothLyricsEnabled.collectAsStateWithLifecycle()
    val superLyricApiEnabled by viewModel.playerSettingsManager.superLyricApiEnabled.collectAsStateWithLifecycle()
    val predictiveBackEnabled by viewModel.playerSettingsManager.predictiveBackEnabled.collectAsStateWithLifecycle()
    val localDirectApiEnabled by viewModel.playerSettingsManager.localDirectApiEnabled.collectAsStateWithLifecycle()

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

    val colors = AppTheme.colors
    val red = colors.accent
    val secondary = colors.textSecondary

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
                            isProcessingLogout = uiState.isLoading,
                            backdrop = backdrop,
                            onCaptchaClick = { viewModel.onCaptchaPanelOpened() },
                            onQrClick = { viewModel.onQrPanelOpened() },
                            audioQuality = audioQuality,
                            onAudioQualityClick = { showAudioQualityDialog = true },
                            themeMode = themeMode,
                            onThemeModeClick = { showThemeModeDialog = true },
                            bluetoothLyricsEnabled = bluetoothLyricsEnabled,
                            onBluetoothLyricsChange = { viewModel.playerSettingsManager.setBluetoothLyricsEnabled(it) },
                            superLyricApiEnabled = superLyricApiEnabled,
                            onSuperLyricApiChange = { viewModel.playerSettingsManager.setSuperLyricApiEnabled(it) },
                            predictiveBackEnabled = predictiveBackEnabled,
                            onPredictiveBackChange = { viewModel.playerSettingsManager.setPredictiveBackEnabled(it) },
                            localDirectApiEnabled = localDirectApiEnabled,
                            onLocalDirectApiChange = { viewModel.playerSettingsManager.setLocalDirectApiEnabled(it) },
                            onLogoutClick = { viewModel.onRequestLogout() }
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
            AdaptiveAlertDialog(
                onDismissRequest = { viewModel.onDismissLogoutConfirm() },
                title = "退出登录",
                text = "确定要退出当前账号吗？退出后你仍可随时重新登录。",
                confirmText = "退出",
                confirmColor = red,
                onConfirm = { viewModel.onConfirmLogout() },
                dismissText = "取消",
                onDismiss = { viewModel.onDismissLogoutConfirm() }
            )
        }

        if (showAudioQualityDialog) {
            AudioQualitySettingsDialog(
                currentQuality = audioQuality,
                onSelect = { quality ->
                    viewModel.playerSettingsManager.setAudioQuality(quality)
                    showAudioQualityDialog = false
                },
                onDismiss = { showAudioQualityDialog = false }
            )
        }

        if (showThemeModeDialog) {
            ThemeModeSettingsDialog(
                currentMode = themeMode,
                onSelect = { mode ->
                    viewModel.playerSettingsManager.setThemeMode(mode)
                    showThemeModeDialog = false
                },
                onDismiss = { showThemeModeDialog = false }
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
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.accentSubtle)
                    .border(1.dp, colors.accent.copy(alpha = 0.30f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Sms,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = "Setease 账户",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .size(44.dp)
                .background(colors.surfaceVariant, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "关闭",
                tint = colors.iconPrimary
            )
        }
    }
}

@Composable
private fun MethodSelectionPanel(
    red: Color,
    secondary: Color,
    isLoggedIn: Boolean,
    avatarUrl: String?,
    displayName: String?,
    isProcessingLogout: Boolean,
    backdrop: Backdrop?,
    onCaptchaClick: () -> Unit,
    onQrClick: () -> Unit,
    audioQuality: OnlineAudioQuality,
    onAudioQualityClick: () -> Unit,
    themeMode: AppThemeMode,
    onThemeModeClick: () -> Unit,
    bluetoothLyricsEnabled: Boolean,
    onBluetoothLyricsChange: (Boolean) -> Unit,
    superLyricApiEnabled: Boolean,
    onSuperLyricApiChange: (Boolean) -> Unit,
    predictiveBackEnabled: Boolean,
    onPredictiveBackChange: (Boolean) -> Unit,
    localDirectApiEnabled: Boolean,
    onLocalDirectApiChange: (Boolean) -> Unit,
    onLogoutClick: () -> Unit
) {
    val colors = AppTheme.colors
    val dividerColor = colors.divider

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isLoggedIn) {
            ProfileCard(
                avatarUrl = avatarUrl,
                displayName = displayName
            )
        } else {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                LoginMethodRow(
                    text = "手机验证码登录",
                    red = red,
                    onClick = onCaptchaClick
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    color = dividerColor,
                    thickness = 1.dp
                )

                LoginMethodRow(
                    text = "二维码登录",
                    red = red,
                    onClick = onQrClick
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "登录账户以同步你的歌单、收藏和听歌记录。",
                color = secondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "音乐设置",
            color = secondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsSectionCard {
            SettingsNavigationRow(
                title = "在线播放音质",
                trailingText = audioQuality.playerMenuLabel,
                onClick = onAudioQualityClick
            )
            SettingsDivider(dividerColor)
            SettingsNavigationRow(
                title = "外观模式",
                trailingText = themeMode.title,
                onClick = onThemeModeClick
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsSectionCard {
            SettingsToggleRow(
                title = "车载蓝牙歌词",
                checked = bluetoothLyricsEnabled,
                onCheckedChange = onBluetoothLyricsChange,
                backdrop = backdrop
            )
            SettingsDivider(dividerColor)
            SettingsToggleRow(
                title = "灵动岛与状态栏歌词",
                checked = superLyricApiEnabled,
                onCheckedChange = onSuperLyricApiChange,
                backdrop = backdrop
            )
            SettingsDivider(dividerColor)
            SettingsToggleRow(
                title = "预测性返回手势",
                checked = predictiveBackEnabled,
                onCheckedChange = onPredictiveBackChange,
                backdrop = backdrop
            )
            SettingsDivider(dividerColor)
            SettingsToggleRow(
                title = "本地直连接口（免服务器）",
                checked = localDirectApiEnabled,
                onCheckedChange = onLocalDirectApiChange,
                backdrop = backdrop
            )
        }

        if (isLoggedIn) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = !isProcessingLogout,
                        onClick = onLogoutClick
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessingLogout) {
                        CircularProgressIndicator(
                            color = red,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "退出登录",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = red
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    avatarUrl: String?,
    displayName: String?
) {
    val colors = AppTheme.colors
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                avatarUrl = avatarUrl,
                displayName = displayName,
                size = 52.dp,
                showBorder = false
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName ?: "Setease 用户",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "账户",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
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
    val colors = AppTheme.colors
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
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (!trailingText.isNullOrBlank()) {
            Text(
                text = trailingText,
                fontSize = 16.sp,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.size(4.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textTertiary,
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
            color = AppTheme.colors.textPrimary,
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
    val isDark = AppTheme.isDark
    val trackWidth = 51.dp
    val trackHeight = 31.dp
    val thumbSize = 27.dp
    val thumbPadding = 2.dp

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val uncheckedTrackColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFD6D6DC)
    val trackColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF30D158) else uncheckedTrackColor,
        animationSpec = tween(durationMillis = 170),
        label = "liquidSwitchTrackColor"
    )
    val thumbOffsetX by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - thumbPadding else thumbPadding,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 640f),
        label = "liquidSwitchThumbOffset"
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (isPressed) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 700f),
        label = "liquidSwitchThumbScale"
    )
    val switchBloomStyle = remember(isDark) {
        BloomStrokeDefaults.CompactControl.copy(
            lightSurfaceSubtleOutlineAlpha = if (isDark) 0f else 0.06f
        )
    }

    val baseModifier = modifier
        .size(trackWidth, trackHeight)
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null
        ) {
            onCheckedChange(!checked)
        }

    val decoratedModifier = if (backdrop != null) {
        baseModifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(trackHeight / 2) },
                highlight = null,
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
            .bloomStroke(
                cornerRadius = trackHeight / 2,
                style = switchBloomStyle,
                extraDegrees = -45f
            )
    } else {
        baseModifier
            .background(trackColor, CircleShape)
            .bloomStroke(
                cornerRadius = trackHeight / 2,
                style = switchBloomStyle,
                extraDegrees = -45f
            )
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
                    elevation = if (isPressed) 3.dp else 2.dp,
                    shape = CircleShape,
                    clip = false
                )
                .background(
                    color = Color.White.copy(alpha = if (isPressed) 0.88f else 1f),
                    shape = CircleShape
                )
                .then(
                    if (isPressed) {
                        Modifier.bloomStroke(
                            cornerRadius = thumbSize / 2,
                            style = BloomStrokeDefaults.DropletPill,
                            extraDegrees = 90f
                        )
                    } else {
                        Modifier
                    }
                )
        )
    }
}

@Composable
private fun LoginMethodRow(
    text: String,
    red: Color,
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
            text = text,
            color = red,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
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
    val colors = AppTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        PanelTitle(
            title = "手机验证码登录",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val fieldColors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                focusedPlaceholderColor = colors.textSecondary,
                unfocusedPlaceholderColor = colors.textSecondary,
                cursorColor = red
            )
            TextField(
                value = phone,
                onValueChange = onPhoneChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                placeholder = { Text("请输入手机号") },
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = colors.divider,
                thickness = 1.dp
            )

            TextField(
                value = captcha,
                onValueChange = onCaptchaChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("请输入验证码") },
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSendCaptcha,
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("发送验证码")
                    }
                }

                Button(
                    onClick = onLogin,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = red),
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("登录")
                    }
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
internal fun QrPanel(
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

    val isErrorState = imageBitmap == null && !isLoading
    val colors = AppTheme.colors

    Column(modifier = Modifier.fillMaxWidth()) {
        PanelTitle(
            title = "二维码登录",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
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
                        .background(
                            if (imageBitmap != null) Color.White else colors.surfaceVariant,
                            RoundedCornerShape(20.dp)
                        )
                        .border(
                            1.dp,
                            colors.divider,
                            RoundedCornerShape(20.dp)
                        )
                        .then(if (isErrorState) Modifier.clickable { onRefresh() } else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading && imageBitmap == null -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = red,
                                strokeWidth = 3.dp
                            )
                        }
                        imageBitmap != null -> {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "登录二维码",
                                modifier = Modifier.size(170.dp)
                            )
                        }
                        else -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudOff,
                                    contentDescription = "加载失败",
                                    tint = colors.textTertiary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "点击重新加载",
                                    color = colors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isErrorState) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = colors.errorIcon,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = qrHint,
                                color = colors.errorContent,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "提示：请确认手机已联网，或服务器正常运行",
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = qrHint,
                        color = secondary,
                        fontSize = 14.sp
                    )
                }

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
                        Text(if (isErrorState) "重新获取二维码" else "刷新二维码")
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
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = colors.textPrimary
            )
        }
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AudioQualitySettingsDialog(
    currentQuality: OnlineAudioQuality,
    onSelect: (OnlineAudioQuality) -> Unit,
    onDismiss: () -> Unit
) {
    SettingsOptionDialog(
        title = "选择在线播放音质",
        options = OnlineAudioQuality.entries,
        selectedOption = currentQuality,
        optionTitle = { it.playerMenuLabel },
        optionSubtitle = { it.desc },
        onSelect = onSelect,
        onDismiss = onDismiss
    )
}

@Composable
private fun ThemeModeSettingsDialog(
    currentMode: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    SettingsOptionDialog(
        title = "选择外观模式",
        options = AppThemeMode.entries,
        selectedOption = currentMode,
        optionTitle = { it.title },
        optionSubtitle = { it.desc },
        onSelect = onSelect,
        onDismiss = onDismiss
    )
}

@Composable
private fun <T> SettingsOptionDialog(
    title: String,
    options: List<T>,
    selectedOption: T,
    optionTitle: (T) -> String,
    optionSubtitle: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    val isDark = AppTheme.isDark
    val unselectedItemBg = if (isDark) Color(0xFF1C1C1E) else colors.surfaceVariant

    AdaptiveAlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmText = "完成",
        confirmColor = colors.accent,
        dismissText = null,
        onConfirm = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            options.forEach { option ->
                val isSelected = selectedOption == option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) colors.accentSubtle else unselectedItemBg
                        )
                        .clickable { onSelect(option) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = optionTitle(option),
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colors.accent else colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = optionSubtitle(option),
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
