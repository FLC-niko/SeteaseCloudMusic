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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import com.example.seteasecloudmusic.core.settings.PlayerStyle
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
    var showPlayerStyleDialog by remember { mutableStateOf(false) }
    val playerStyle by viewModel.playerSettingsManager.playerStyle.collectAsState()

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
            val isAccountDetailsFlow =
                uiState.panel == AuthPanel.ACCOUNT_DETAILS ||
                    uiState.panel == AuthPanel.ACCOUNT_DETAIL_SUBPAGE

            if (isAccountDetailsFlow) {
                val detailsTitle = if (uiState.panel == AuthPanel.ACCOUNT_DETAILS) {
                    "账户设置"
                } else {
                    uiState.accountDetailsDestination?.title ?: "账户详情"
                }
                AccountDetailsHeader(
                    title = detailsTitle,
                    onBack = {
                        if (uiState.panel == AuthPanel.ACCOUNT_DETAIL_SUBPAGE) {
                            viewModel.onBackFromAccountDetailsSubpage()
                        } else {
                            viewModel.onBackToMethods()
                        }
                    },
                    onDismiss = onDismiss
                )
            } else {
                Header(onDismiss = onDismiss)
            }

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
                            onCaptchaClick = { viewModel.onCaptchaPanelOpened() },
                            onQrClick = { viewModel.onQrPanelOpened() },
                            onSettingsClick = {
                                showPlayerStyleDialog = true
                            },
                            onProfileClick = {
                                viewModel.onAccountDetailsOpened()
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

                    AuthPanel.ACCOUNT_DETAILS -> {
                        AccountDetailsPanel(
                            avatarUrl = uiState.authSession?.avatarUrl,
                            displayName = uiState.authSession?.nickname,
                            personalizedRecommendationEnabled = uiState.personalizedRecommendationEnabled,
                            isProcessingLogout = uiState.isLoading,
                            backdrop = backdrop,
                            onAppleAccountClick = {
                                viewModel.onAccountDetailsDestinationOpened(AccountDetailsDestination.APPLE_ACCOUNT)
                            },
                            onManagePaymentClick = {
                                viewModel.onAccountDetailsDestinationOpened(AccountDetailsDestination.MANAGE_PAYMENT)
                            },
                            onSubscriptionsClick = {
                                viewModel.onAccountDetailsDestinationOpened(AccountDetailsDestination.SUBSCRIPTIONS)
                            },
                            onPurchaseHistoryClick = {
                                viewModel.onAccountDetailsDestinationOpened(AccountDetailsDestination.PURCHASE_HISTORY)
                            },
                            onAddFundsClick = {
                                viewModel.onAccountDetailsDestinationOpened(AccountDetailsDestination.ADD_FUNDS)
                            },
                            onCountryRegionClick = {
                                viewModel.onAccountDetailsDestinationOpened(AccountDetailsDestination.COUNTRY_REGION)
                            },
                            onRatingsAndReviewsClick = {
                                viewModel.onAccountDetailsDestinationOpened(AccountDetailsDestination.RATINGS_AND_REVIEWS)
                            },
                            onPersonalizedRecommendationChange = {
                                viewModel.onPersonalizedRecommendationToggled(it)
                            },
                            playerStyle = playerStyle,
                            onPlayerStyleClick = { showPlayerStyleDialog = true },
                            onLogoutClick = { viewModel.onRequestLogout() }
                        )
                    }

                    AuthPanel.ACCOUNT_DETAIL_SUBPAGE -> {
                        AccountDetailsSubpagePanel(
                            destination = uiState.accountDetailsDestination,
                            secondary = secondary
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

        if (showPlayerStyleDialog) {
            PlayerStyleSelectionDialog(
                currentStyle = playerStyle,
                onSelect = { style ->
                    viewModel.playerSettingsManager.setPlayerStyle(style)
                    showPlayerStyleDialog = false
                },
                onDismiss = { showPlayerStyleDialog = false }
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
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFE8EC))
                    .border(1.dp, Color(0xFFFFBAC7), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Sms,
                    contentDescription = null,
                    tint = Color(0xFFFA233B),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = "Setease 账户",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF121212)
            )
        }

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
private fun AccountDetailsHeader(
    title: String,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .background(Color(0xFFF0F0F4), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color(0xFF1C1C1E)
            )
        }

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF121212)
        )

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
private fun AccountDetailsPanel(
    avatarUrl: String?,
    displayName: String?,
    personalizedRecommendationEnabled: Boolean,
    isProcessingLogout: Boolean,
    backdrop: Backdrop?,
    onAppleAccountClick: () -> Unit,
    onManagePaymentClick: () -> Unit,
    onSubscriptionsClick: () -> Unit,
    onPurchaseHistoryClick: () -> Unit,
    onAddFundsClick: () -> Unit,
    onCountryRegionClick: () -> Unit,
    onRatingsAndReviewsClick: () -> Unit,
    onPersonalizedRecommendationChange: (Boolean) -> Unit,
    playerStyle: PlayerStyle,
    onPlayerStyleClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val secondary = Color(0xFF8D8D93)
    val dividerColor = Color(0xFFE2E2E8)
    val red = Color(0xFFFA233B)

    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSectionCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAppleAccountClick)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    avatarUrl = avatarUrl,
                    displayName = displayName,
                    size = 44.dp,
                    showBorder = false
                )
                Spacer(modifier = Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Apple 账户",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF121212)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = displayName ?: "Setease 用户",
                        fontSize = 14.sp,
                        color = secondary
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFFC7C7CC),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsSectionCard {
            SettingsNavigationRow(
                title = "播放页风格",
                trailingText = if (playerStyle == PlayerStyle.AMLL_WEB) "Apple Music 动效" else "原生质感",
                onClick = onPlayerStyleClick
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsSectionCard {
            SettingsNavigationRow(
                title = "管理付款方式",
                onClick = onManagePaymentClick
            )
            SettingsDivider(dividerColor)
            SettingsNavigationRow(
                title = "订阅",
                onClick = onSubscriptionsClick
            )
            SettingsDivider(dividerColor)
            SettingsNavigationRow(
                title = "购买记录",
                onClick = onPurchaseHistoryClick
            )
            SettingsDivider(dividerColor)
            SettingsNavigationRow(
                title = "为账户充值",
                onClick = onAddFundsClick
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsSectionCard {
            SettingsNavigationRow(
                title = "国家/地区",
                trailingText = "中国大陆",
                onClick = onCountryRegionClick
            )
            SettingsDivider(dividerColor)
            SettingsNavigationRow(
                title = "评分与评论",
                onClick = onRatingsAndReviewsClick
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsSectionCard {
            SettingsToggleRow(
                title = "个性化推荐",
                checked = personalizedRecommendationEnabled,
                onCheckedChange = onPersonalizedRecommendationChange,
                backdrop = backdrop
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "开启后，系统将使用你的 App 使用数据、下载内容和购买行为优化推荐内容。",
            color = secondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
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

@Composable
private fun AccountDetailsSubpagePanel(
    destination: AccountDetailsDestination?,
    secondary: Color
) {
    val title = destination?.title ?: "账户详情"
    val description = destination?.placeholderDescription() ?: "该功能正在开发中。"

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF121212)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = secondary
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

private fun AccountDetailsDestination.placeholderDescription(): String {
    return when (this) {
        AccountDetailsDestination.APPLE_ACCOUNT -> "这里会展示账户信息、登录设备与安全设置。"
        AccountDetailsDestination.MANAGE_PAYMENT -> "这里将支持管理银行卡、支付方式和账单地址。"
        AccountDetailsDestination.SUBSCRIPTIONS -> "这里将展示你的订阅状态、续费周期和方案变更入口。"
        AccountDetailsDestination.PURCHASE_HISTORY -> "这里会提供购买项目、订单详情和发票记录。"
        AccountDetailsDestination.ADD_FUNDS -> "这里将支持账户充值与余额明细管理。"
        AccountDetailsDestination.COUNTRY_REGION -> "这里将支持切换国家或地区与货币配置。"
        AccountDetailsDestination.RATINGS_AND_REVIEWS -> "这里会展示你的评分、评论与编辑入口。"
    }
}

@Composable
private fun MethodSelectionPanel(
    red: Color,
    secondary: Color,
    isLoggedIn: Boolean,
    avatarUrl: String?,
    displayName: String?,
    onCaptchaClick: () -> Unit,
    onQrClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (isLoggedIn) {
            ProfileCard(
                avatarUrl = avatarUrl,
                displayName = displayName,
                onClick = onProfileClick
            )

            Spacer(modifier = Modifier.height(14.dp))
        } else {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    color = Color(0xFFE8E8ED),
                    thickness = 1.dp
                )

                LoginMethodRow(
                    text = "二维码登录",
                    red = red,
                    onClick = onQrClick
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "账户用于同步你的收藏、播放记录和偏好设置。",
                color = secondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSettingsClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color(0xFFFFE8EC), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        tint = red,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = "音乐设置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = red
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    avatarUrl: String?,
    displayName: String?,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                size = 56.dp,
                showBorder = false
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName ?: "Setease 用户",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF121212)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "账户信息、付款方式和设置",
                    fontSize = 14.sp,
                    color = Color(0xFF8D8D93)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFC7C7CC),
                modifier = Modifier.size(22.dp)
            )
        }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color(0xFFE8E8ED),
                thickness = 1.dp
            )

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
                    text = qrHint,
                    color = secondary,
                    fontSize = 14.sp
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

@Composable
private fun PlayerStyleSelectionDialog(
    currentStyle: PlayerStyle,
    onSelect: (PlayerStyle) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "选择播放页风格",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111)
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlayerStyle.values().forEach { style ->
                    val isSelected = currentStyle == style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) Color(0xFFFA233B).copy(alpha = 0.08f) else Color(0xFFF5F5F7)
                            )
                            .clickable { onSelect(style) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = style.title,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFFFA233B) else Color(0xFF111111)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = style.desc,
                                fontSize = 12.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color(0xFFFA233B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFA233B))
            ) {
                Text("完成", color = Color.White)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}

