package com.example.seteasecloudmusic.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seteasecloudmusic.feature.auth.domain.model.AuthSession
import com.example.seteasecloudmusic.feature.auth.domain.model.QrLoginStart
import com.example.seteasecloudmusic.feature.auth.domain.model.QrStatus
import com.example.seteasecloudmusic.feature.auth.usecase.LogoutUseCase
import com.example.seteasecloudmusic.feature.auth.usecase.ObserveAuthStateUseCase
import com.example.seteasecloudmusic.feature.auth.usecase.PollQrStatusUseCase
import com.example.seteasecloudmusic.feature.auth.usecase.RefreshSessionUseCase
import com.example.seteasecloudmusic.feature.auth.usecase.SendCaptchaUseCase
import com.example.seteasecloudmusic.feature.auth.usecase.StartQrLoginUseCase
import com.example.seteasecloudmusic.feature.auth.usecase.VerifyCaptchaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val panel: AuthPanel = AuthPanel.METHODS,
    val phone: String = "",
    val captcha: String = "",
    val qrLoginStart: QrLoginStart? = null,
    val qrHint: String = "等待扫码登录",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val authSession: AuthSession? = null,
    val accountDetailsDestination: AccountDetailsDestination? = null,
    val personalizedRecommendationEnabled: Boolean = true,
    val showLogoutConfirmDialog: Boolean = false
)

enum class AuthPanel {
    METHODS,
    CAPTCHA,
    QR,
    ACCOUNT_DETAILS,
    ACCOUNT_DETAIL_SUBPAGE
}

enum class AccountDetailsDestination(val title: String) {
    APPLE_ACCOUNT("Apple 账户"),
    MANAGE_PAYMENT("管理付款方式"),
    SUBSCRIPTIONS("订阅"),
    PURCHASE_HISTORY("购买记录"),
    ADD_FUNDS("为账户充值"),
    COUNTRY_REGION("国家/地区"),
    RATINGS_AND_REVIEWS("评分与评论")
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val sendCaptchaUseCase: SendCaptchaUseCase,
    private val verifyCaptchaUseCase: VerifyCaptchaUseCase,
    private val startQrLoginUseCase: StartQrLoginUseCase,
    private val pollQrStatusUseCase: PollQrStatusUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val refreshSessionUseCase: RefreshSessionUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _dismissSheet = MutableSharedFlow<Unit>()
    val dismissSheet: SharedFlow<Unit> = _dismissSheet.asSharedFlow()

    private var qrPollJob: Job? = null
    private var isRefreshingSessionProfile = false

    init {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { session ->
                _uiState.update {
                    val isLoggedIn = session?.isLoggedIn == true
                    val shouldResetDetailsPanel = !isLoggedIn &&
                        (it.panel == AuthPanel.ACCOUNT_DETAILS || it.panel == AuthPanel.ACCOUNT_DETAIL_SUBPAGE)
                    it.copy(
                        isLoggedIn = isLoggedIn,
                        authSession = session,
                        panel = if (shouldResetDetailsPanel) AuthPanel.METHODS else it.panel,
                        accountDetailsDestination = if (isLoggedIn) it.accountDetailsDestination else null,
                        showLogoutConfirmDialog = if (isLoggedIn) it.showLogoutConfirmDialog else false
                    )
                }
                maybeRefreshProfileIfNeeded(session)
            }
        }
    }

    fun onPhoneChanged(phone: String) {
        _uiState.update { it.copy(phone = phone.filter(Char::isDigit).take(11), errorMessage = null) }
    }

    fun onCaptchaChanged(captcha: String) {
        _uiState.update { it.copy(captcha = captcha.filter(Char::isDigit), errorMessage = null) }
    }

    fun onCaptchaPanelOpened() {
        _uiState.update {
            it.copy(
                panel = AuthPanel.CAPTCHA,
                captcha = "",
                errorMessage = null,
                showLogoutConfirmDialog = false
            )
        }
    }

    fun onQrPanelOpened() {
        _uiState.update {
            it.copy(
                panel = AuthPanel.QR,
                qrHint = "等待扫码登录",
                errorMessage = null,
                showLogoutConfirmDialog = false
            )
        }
        startQrLogin()
    }

    fun onAccountSheetOpened() {
        maybeRefreshProfileIfNeeded(_uiState.value.authSession)
    }

    fun onAccountDetailsOpened() {
        if (!_uiState.value.isLoggedIn) return
        stopQrPolling()
        _uiState.update {
            it.copy(
                panel = AuthPanel.ACCOUNT_DETAILS,
                accountDetailsDestination = null,
                errorMessage = null,
                showLogoutConfirmDialog = false
            )
        }
        maybeRefreshProfileIfNeeded(_uiState.value.authSession)
    }

    fun onAccountDetailsDestinationOpened(destination: AccountDetailsDestination) {
        _uiState.update {
            it.copy(
                panel = AuthPanel.ACCOUNT_DETAIL_SUBPAGE,
                accountDetailsDestination = destination,
                showLogoutConfirmDialog = false
            )
        }
    }

    fun onBackFromAccountDetailsSubpage() {
        _uiState.update {
            it.copy(
                panel = AuthPanel.ACCOUNT_DETAILS,
                accountDetailsDestination = null
            )
        }
    }

    fun onPersonalizedRecommendationToggled(enabled: Boolean) {
        _uiState.update { it.copy(personalizedRecommendationEnabled = enabled) }
    }

    fun onRequestLogout() {
        _uiState.update { it.copy(showLogoutConfirmDialog = true) }
    }

    fun onDismissLogoutConfirm() {
        _uiState.update { it.copy(showLogoutConfirmDialog = false) }
    }

    fun onConfirmLogout() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            stopQrPolling()
            _uiState.update { it.copy(isLoading = true, showLogoutConfirmDialog = false) }

            val result = logoutUseCase()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    panel = AuthPanel.METHODS,
                    accountDetailsDestination = null,
                    qrLoginStart = null,
                    qrHint = "等待扫码登录"
                )
            }

            result.fold(
                onSuccess = {
                    _snackbarMessage.emit("已退出登录")
                },
                onFailure = { error ->
                    val detail = error.message?.takeIf { it.isNotBlank() }
                    if (detail == null) {
                        _snackbarMessage.emit("已退出登录")
                    } else {
                        _snackbarMessage.emit("已退出登录（服务端请求失败：$detail）")
                    }
                }
            )
        }
    }

    fun onBackToMethods() {
        stopQrPolling()
        _uiState.update {
            it.copy(
                panel = AuthPanel.METHODS,
                qrLoginStart = null,
                qrHint = "等待扫码登录",
                errorMessage = null,
                accountDetailsDestination = null,
                showLogoutConfirmDialog = false
            )
        }
    }

    fun onSendCaptcha() {
        val phone = _uiState.value.phone
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = sendCaptchaUseCase(phone)
            _uiState.update { it.copy(isLoading = false) }
            result.fold(
                onSuccess = {
                    _snackbarMessage.emit("验证码已发送")
                },
                onFailure = { error ->
                    val msg = error.message ?: "发送失败"
                    _uiState.update { it.copy(errorMessage = msg) }
                    _snackbarMessage.emit(msg)
                }
            )
        }
    }

    fun onCaptchaLogin() {
        val phone = _uiState.value.phone
        val captcha = _uiState.value.captcha
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = verifyCaptchaUseCase(phone, captcha)
            _uiState.update { it.copy(isLoading = false) }
            result.fold(
                onSuccess = { session ->
                    _uiState.update { it.copy(isLoggedIn = true, authSession = session) }
                    maybeRefreshProfileIfNeeded(session)
                    _snackbarMessage.emit("登录成功")
                    _dismissSheet.emit(Unit)
                },
                onFailure = { error ->
                    val msg = error.message ?: "登录失败"
                    _uiState.update { it.copy(errorMessage = msg) }
                    _snackbarMessage.emit(msg)
                }
            )
        }
    }

    fun onRefreshQr() {
        stopQrPolling()
        startQrLogin()
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopQrPolling()
    }

    private fun startQrLogin() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = startQrLoginUseCase()
            _uiState.update { it.copy(isLoading = false) }
            result.fold(
                onSuccess = { qrLoginStart ->
                    _uiState.update { it.copy(qrLoginStart = qrLoginStart) }
                    startQrPolling(qrLoginStart.key)
                },
                onFailure = { error ->
                    val msg = error.message ?: "获取二维码失败"
                    _uiState.update { it.copy(qrHint = msg) }
                    _snackbarMessage.emit(msg)
                }
            )
        }
    }

    private fun startQrPolling(key: String) {
        stopQrPolling()
        qrPollJob = viewModelScope.launch {
            while (isActive) {
                delay(2000L)
                val result = pollQrStatusUseCase(key)
                result.fold(
                    onSuccess = { pollResult ->
                        when (pollResult.state) {
                            QrStatus.WAIT_SCAN -> {
                                _uiState.update { it.copy(qrHint = "请使用网易云音乐App扫码") }
                            }
                            QrStatus.WAIT_CONFIRM -> {
                                _uiState.update {
                                    it.copy(qrHint = pollResult.message ?: "扫码成功，请在手机上确认")
                                }
                            }
                            QrStatus.EXPIRED -> {
                                _uiState.update { it.copy(qrHint = "二维码已过期，请点击刷新") }
                                stopQrPolling()
                            }
                            QrStatus.SUCCESS -> {
                                _uiState.update {
                                    it.copy(
                                        isLoggedIn = true,
                                        authSession = pollResult.session,
                                        qrHint = "登录成功"
                                    )
                                }
                                maybeRefreshProfileIfNeeded(pollResult.session)
                                _dismissSheet.emit(Unit)
                                stopQrPolling()
                            }
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(qrHint = "轮询失败: ${error.message}")
                        }
                    }
                )
            }
        }
    }

    private fun stopQrPolling() {
        qrPollJob?.cancel()
        qrPollJob = null
    }

    private fun maybeRefreshProfileIfNeeded(session: AuthSession?) {
        if (!needsProfileRefresh(session) || isRefreshingSessionProfile) {
            return
        }

        viewModelScope.launch {
            isRefreshingSessionProfile = true
            try {
                refreshSessionUseCase()
            } finally {
                isRefreshingSessionProfile = false
            }
        }
    }

    private fun needsProfileRefresh(session: AuthSession?): Boolean {
        return session?.isLoggedIn == true &&
            (session.userId == null || session.nickname.isNullOrBlank() || session.avatarUrl.isNullOrBlank())
    }
}
