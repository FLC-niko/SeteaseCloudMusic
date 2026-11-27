package com.example.seteasecloudmusic.feature.auth.presentation

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.example.seteasecloudmusic.R
import com.example.seteasecloudmusic.core.settings.AppThemeMode
import com.example.seteasecloudmusic.core.settings.OnlineAudioQuality
import com.example.seteasecloudmusic.core.ui.adaptive.AdaptiveAlertDialog
import com.example.seteasecloudmusic.core.ui.components.AppLoadingIndicator
import com.example.seteasecloudmusic.core.ui.components.UserAvatar
import com.example.seteasecloudmusic.core.ui.components.formatTopAmbientColorLabel
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Close
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.RadioButtonLocation
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.flow.collectLatest

/**
 * 账户与偏好设置半屏弹窗内容。
 * 承载登录方式选择、手机号验证码登录、二维码扫码登录，以及音质、外观、顶部氛围色与歌词等设置项。
 */
@Composable
fun AccountLoginSheetContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") backdrop: Backdrop? = null,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val contentScrollState = rememberScrollState()
    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showTopAmbientColorDialog by remember { mutableStateOf(false) }
    val audioQuality by viewModel.playerSettingsManager.audioQuality.collectAsStateWithLifecycle()
    val themeMode by viewModel.playerSettingsManager.themeMode.collectAsStateWithLifecycle()
    val topAmbientEnabled by viewModel.playerSettingsManager.topAmbientEnabled.collectAsStateWithLifecycle()
    val topAmbientColorArgb by viewModel.playerSettingsManager.topAmbientColorArgb.collectAsStateWithLifecycle()
    val liquidGlassOpacity by viewModel.playerSettingsManager.liquidGlassOpacity.collectAsStateWithLifecycle()
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
                .padding(horizontal = 16.dp) // 弹窗内容左右安全边距
                .padding(top = 12.dp, bottom = 76.dp) // 底部留白为 Snackbar 提示预留空间
        ) {
            Header(onDismiss = onDismiss)

            Spacer(modifier = Modifier.height(14.dp)) // 顶栏与主体内容间距

            // 已登录状态下强制展示账户主页，兜底避免再次打开时停留在登录子面板
            val effectivePanel = if (uiState.isLoggedIn) AuthPanel.METHODS else uiState.panel
            Crossfade(
                targetState = effectivePanel,
                animationSpec = tween(220), // 子面板切换淡入淡出时长（ms）
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
                            onCaptchaClick = { viewModel.onCaptchaPanelOpened() },
                            onQrClick = { viewModel.onQrPanelOpened() },
                            audioQuality = audioQuality,
                            onAudioQualityClick = { showAudioQualityDialog = true },
                            themeMode = themeMode,
                            onThemeModeClick = { showThemeModeDialog = true },
                            topAmbientEnabled = topAmbientEnabled,
                            onTopAmbientEnabledChange = { viewModel.playerSettingsManager.setTopAmbientEnabled(it) },
                            topAmbientColorArgb = topAmbientColorArgb,
                            onTopAmbientColorClick = { showTopAmbientColorDialog = true },
                            liquidGlassOpacity = liquidGlassOpacity,
                            onLiquidGlassOpacityChange = {
                                viewModel.playerSettingsManager.setLiquidGlassOpacity(it, persist = false)
                            },
                            onLiquidGlassOpacityChangeFinished = {
                                viewModel.playerSettingsManager.setLiquidGlassOpacity(it, persist = true)
                            },
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
                            isRiskControlError = uiState.isRiskControlError,
                            phone = uiState.phone,
                            captcha = uiState.captcha,
                            captchaCooldownSeconds = uiState.captchaCooldownSeconds,
                            isLoading = uiState.isLoading,
                            onPhoneChange = { viewModel.onPhoneChanged(it) },
                            onCaptchaChange = { viewModel.onCaptchaChanged(it) },
                            onBack = { viewModel.onBackToMethods() },
                            onSendCaptcha = { viewModel.onSendCaptcha() },
                            onLogin = { viewModel.onCaptchaLogin() },
                            onSwitchToQr = { viewModel.onQrPanelOpened() }
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

        if (showTopAmbientColorDialog) {
            TopAmbientColorPickerDialog(
                currentColorArgb = topAmbientColorArgb,
                isAmbientEnabled = topAmbientEnabled,
                onApply = { argb ->
                    viewModel.playerSettingsManager.setTopAmbientColorArgb(argb)
                    if (!topAmbientEnabled) {
                        viewModel.playerSettingsManager.setTopAmbientEnabled(true)
                    }
                    showTopAmbientColorDialog = false
                },
                onDismiss = { showTopAmbientColorDialog = false }
            )
        }

        SnackbarHost(
            state = snackbarHostState,
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
                    painter = painterResource(R.drawable.ic_sms),
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
                .size(44.dp) // 遵循 44.dp 最小触控区域尺寸
                .background(colors.surfaceVariant, CircleShape)
        ) {
            Icon(
                imageVector = MiuixIcons.Basic.Close,
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
    onCaptchaClick: () -> Unit,
    onQrClick: () -> Unit,
    audioQuality: OnlineAudioQuality,
    onAudioQualityClick: () -> Unit,
    themeMode: AppThemeMode,
    onThemeModeClick: () -> Unit,
    topAmbientEnabled: Boolean,
    onTopAmbientEnabledChange: (Boolean) -> Unit,
    topAmbientColorArgb: Int,
    onTopAmbientColorClick: () -> Unit,
    liquidGlassOpacity: Float,
    onLiquidGlassOpacityChange: (Float) -> Unit,
    onLiquidGlassOpacityChangeFinished: (Float) -> Unit,
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

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isLoggedIn) {
            ProfileCard(
                avatarUrl = avatarUrl,
                displayName = displayName
            )
        } else {
            Card(
                colors = CardDefaults.defaultColors(color = colors.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                ArrowPreference(
                    title = "手机验证码登录",
                    titleColor = BasicComponentDefaults.titleColor(color = red),
                    onClick = onCaptchaClick
                )
                ArrowPreference(
                    title = "二维码登录",
                    titleColor = BasicComponentDefaults.titleColor(color = red),
                    onClick = onQrClick
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "登录账户以同步你的歌单、收藏和听歌记录。",
                color = secondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SmallTitle(
            text = "音乐设置",
            insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        )

        Card(
            colors = CardDefaults.defaultColors(color = colors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            ArrowPreference(
                title = "在线播放音质",
                endActions = {
                    Text(
                        text = audioQuality.playerMenuLabel,
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                },
                onClick = onAudioQualityClick
            )
            ArrowPreference(
                title = "外观模式",
                endActions = {
                    Text(
                        text = themeMode.title,
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                },
                onClick = onThemeModeClick
            )
            SwitchPreference(
                title = "顶部氛围颜色",
                checked = topAmbientEnabled,
                onCheckedChange = onTopAmbientEnabledChange
            )
            ArrowPreference(
                title = "氛围调色盘",
                endActions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp) // 调色预览圆点直径
                                .clip(CircleShape)
                                .background(
                                    Color(topAmbientColorArgb).copy(
                                        alpha = if (topAmbientEnabled) 1f else 0.35f // 关闭氛围色时弱化圆点透明度
                                    )
                                )
                                .border(1.dp, colors.coverBorder, CircleShape)
                        )
                        Text(
                            text = if (topAmbientEnabled) {
                                formatTopAmbientColorLabel(topAmbientColorArgb)
                            } else {
                                "已关闭"
                            },
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )
                    }
                },
                onClick = onTopAmbientColorClick
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallTitle(
                text = "液态玻璃",
                insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            )
            if (kotlin.math.abs(liquidGlassOpacity) > 0.01f) {
                Text(
                    text = "恢复默认",
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            onLiquidGlassOpacityChange(0f)
                            onLiquidGlassOpacityChangeFinished(0f)
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        LiquidGlassSettingsCard(
            opacityLevel = liquidGlassOpacity,
            onOpacityChange = onLiquidGlassOpacityChange,
            onOpacityChangeFinished = onLiquidGlassOpacityChangeFinished
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "实时应用于底部导航栏、播放条与浮层液态玻璃效果。",
            color = secondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        SmallTitle(
            text = "歌词与实验性功能",
            insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        )

        Card(
            colors = CardDefaults.defaultColors(color = colors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            SwitchPreference(
                title = "车载蓝牙歌词",
                checked = bluetoothLyricsEnabled,
                onCheckedChange = onBluetoothLyricsChange
            )
            SwitchPreference(
                title = "超级岛与背屏歌词（SuperLyric）",
                checked = superLyricApiEnabled,
                onCheckedChange = onSuperLyricApiChange
            )
            SwitchPreference(
                title = "预测性返回手势",
                checked = predictiveBackEnabled,
                onCheckedChange = onPredictiveBackChange
            )
            SwitchPreference(
                title = "本地直连接口（免服务器）",
                checked = localDirectApiEnabled,
                onCheckedChange = onLocalDirectApiChange
            )
        }

        if (isLoggedIn) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLogoutClick,
                enabled = !isProcessingLogout,
                colors = ButtonDefaults.buttonColors(
                    color = colors.surface,
                    disabledColor = colors.surface,
                    contentColor = red,
                    disabledContentColor = red.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isProcessingLogout) {
                    AppLoadingIndicator(
                        color = red,
                        strokeWidth = 2.dp,
                        size = 20.dp
                    )
                } else {
                    Text(
                        text = "退出登录",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = red
                    )
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
        colors = CardDefaults.defaultColors(color = colors.surface),
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
                size = 52.dp, // 账户主页头像展示尺寸
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
private fun CaptchaPanel(
    red: Color,
    helperText: String?,
    isRiskControlError: Boolean,
    phone: String,
    captcha: String,
    captchaCooldownSeconds: Int,
    isLoading: Boolean,
    onPhoneChange: (String) -> Unit,
    onCaptchaChange: (String) -> Unit,
    onBack: () -> Unit,
    onSendCaptcha: () -> Unit,
    onLogin: () -> Unit,
    onSwitchToQr: () -> Unit
) {
    val colors = AppTheme.colors
    val canSendCaptcha = !isLoading && captchaCooldownSeconds <= 0
    Column(modifier = Modifier.fillMaxWidth()) {
        PanelTitle(
            title = "手机验证码登录",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            cornerRadius = 24.dp,
            colors = CardDefaults.defaultColors(color = colors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            val fieldColors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                labelColor = colors.textSecondary,
                borderColor = Color.Transparent
            )
            @Suppress("DEPRECATION")
            TextField(
                value = phone,
                onValueChange = onPhoneChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                label = "请输入手机号",
                useLabelAsPlaceholder = true,
                textStyle = MiuixTheme.textStyles.body1.copy(color = colors.textPrimary),
                colors = fieldColors,
                cursorBrush = SolidColor(red),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = colors.divider,
                thickness = 1.dp
            )

            @Suppress("DEPRECATION")
            TextField(
                value = captcha,
                onValueChange = onCaptchaChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = "请输入验证码",
                useLabelAsPlaceholder = true,
                textStyle = MiuixTheme.textStyles.body1.copy(color = colors.textPrimary),
                colors = fieldColors,
                cursorBrush = SolidColor(red),
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
                Button(
                    onClick = onSendCaptcha,
                    cornerRadius = 16.dp,
                    colors = ButtonDefaults.buttonColors(
                        color = Color.Transparent,
                        disabledColor = Color.Transparent,
                        contentColor = red,
                        disabledContentColor = colors.textTertiary
                    ),
                    enabled = canSendCaptcha,
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, colors.divider, RoundedCornerShape(16.dp))
                ) {
                    if (isLoading) {
                        AppLoadingIndicator(color = red, strokeWidth = 2.dp, size = 18.dp)
                    } else if (captchaCooldownSeconds > 0) {
                        Text("${captchaCooldownSeconds}s 后重试")
                    } else {
                        Text("发送验证码")
                    }
                }

                Button(
                    onClick = onLogin,
                    cornerRadius = 16.dp,
                    colors = ButtonDefaults.buttonColorsPrimary(color = red, contentColor = Color.White),
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoading) {
                        AppLoadingIndicator(color = Color.White, strokeWidth = 2.dp, size = 18.dp)
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
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )

            if (isRiskControlError) {
                Card(
                    cornerRadius = 20.dp,
                    colors = CardDefaults.defaultColors(color = colors.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "网易云对手机号验证码登录风控较严，建议直接改用更稳定的二维码扫码登录。",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                        Button(
                            onClick = onSwitchToQr,
                            cornerRadius = 14.dp,
                            colors = ButtonDefaults.buttonColors(
                                color = Color.Transparent,
                                disabledColor = Color.Transparent,
                                contentColor = red,
                                disabledContentColor = colors.textTertiary
                            ),
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.divider, RoundedCornerShape(14.dp))
                        ) {
                            Text(
                                text = "改用二维码登录",
                                color = red,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
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
            cornerRadius = 24.dp,
            colors = CardDefaults.defaultColors(color = colors.surface),
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
                        .size(190.dp) // 二维码卡片外框尺寸
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
                            AppLoadingIndicator(
                                color = red,
                                strokeWidth = 3.dp,
                                size = 36.dp
                            )
                        }
                        imageBitmap != null -> {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "登录二维码",
                                modifier = Modifier.size(170.dp) // 二维码实际绘制尺寸（四周保留 10.dp 静区）
                            )
                        }
                        else -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_cloud_off),
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
                        cornerRadius = 14.dp,
                        colors = CardDefaults.defaultColors(color = colors.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_error_outline),
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

                Button(
                    onClick = onRefresh,
                    cornerRadius = 16.dp,
                    colors = ButtonDefaults.buttonColors(
                        color = Color.Transparent,
                        disabledColor = Color.Transparent,
                        contentColor = red,
                        disabledContentColor = colors.textTertiary
                    ),
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.divider, RoundedCornerShape(16.dp))
                ) {
                    if (isLoading) {
                        AppLoadingIndicator(color = red, strokeWidth = 2.dp, size = 18.dp)
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
                imageVector = MiuixIcons.Back,
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
    val containerColor = if (isDark) Color(0xFF1C1C1E) else colors.surfaceVariant // 深色模式下选项卡片底色

    AdaptiveAlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmText = "完成",
        confirmColor = colors.accent,
        dismissText = null,
        onConfirm = onDismiss
    ) {
        Card(
            colors = CardDefaults.defaultColors(color = containerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                val isSelected = selectedOption == option
                RadioButtonPreference(
                    title = optionTitle(option),
                    summary = optionSubtitle(option),
                    selected = isSelected,
                    radioButtonLocation = RadioButtonLocation.End,
                    onClick = { onSelect(option) }
                )
            }
        }
    }
}
