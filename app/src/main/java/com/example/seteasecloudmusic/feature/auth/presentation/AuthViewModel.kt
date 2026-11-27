package com.example.seteasecloudmusic.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seteasecloudmusic.core.auth.AuthSession
import com.example.seteasecloudmusic.core.common.isRiskControlError
import com.example.seteasecloudmusic.core.common.toUserFriendlyMessage
import com.example.seteasecloudmusic.core.local.LocalMusicRepository
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

/**
 * 账户弹窗 UI 状态模型，包含当前面板、表单输入、倒计时与登录会话信息。
 */
data class AuthUiState(
    val panel: AuthPanel = AuthPanel.METHODS,
    val phone: String = "",
    val captcha: String = "",
    val captchaCooldownSeconds: Int = 0,
    val qrLoginStart: QrLoginStart? = null,
    val qrHint: String = "等待扫码登录",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRiskControlError: Boolean = false,
    val isLoggedIn: Boolean = false,
    val authSession: AuthSession? = null,
    val showLogoutConfirmDialog: Boolean = false
)

/**
 * 账户弹窗内的子面板类型。
 */
enum class AuthPanel {
    METHODS,
    CAPTCHA,
    QR
}

/**
 * 认证流程 ViewModel，负责手机验证码登录、二维码生成与状态轮询、登出及会话资料补全。
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val sendCaptchaUseCase: SendCaptchaUseCase,
    private val verifyCaptchaUseCase: VerifyCaptchaUseCase,
    private val startQrLoginUseCase: StartQrLoginUseCase,
    private val pollQrStatusUseCase: PollQrStatusUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val refreshSessionUseCase: RefreshSessionUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val localMusicRepository: LocalMusicRepository,
    val playerSettingsManager: com.example.seteasecloudmusic.core.settings.PlayerSettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _dismissSheet = MutableSharedFlow<Unit>()
    val dismissSheet: SharedFlow<Unit> = _dismissSheet.asSharedFlow()

    private var qrStartJob: Job? = null
    private var qrPollJob: Job? = null
    private var captchaCooldownJob: Job? = null
    private var qrRequestId = 0L
    private var isRefreshingSessionProfile = false

    fun updateScanDirectories(directories: List<String>) {
        localMusicRepository.setScanDirectories(directories)
        viewModelScope.launch {
            runCatching { localMusicRepository.scanConfiguredDirectories() }
        }
    }

    init {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { session ->
                val isLoggedIn = session?.isLoggedIn == true
                if (isLoggedIn) {
                    stopQrLoginFlow()
                    stopCaptchaCooldown()
                }
                _uiState.update {
                    it.copy(
                        isLoggedIn = isLoggedIn,
                        authSession = session,
                        // 一旦确认已登录，自动切回账户主页（展示头像与设置），避免停留在验证码/扫码子面板
                        panel = if (isLoggedIn) AuthPanel.METHODS else it.panel,
                        captcha = if (isLoggedIn) "" else it.captcha,
                        captchaCooldownSeconds = if (isLoggedIn) 0 else it.captchaCooldownSeconds,
                        qrLoginStart = if (isLoggedIn) null else it.qrLoginStart,
                        qrHint = if (isLoggedIn) "等待扫码登录" else it.qrHint,
                        errorMessage = if (isLoggedIn) null else it.errorMessage,
                        isRiskControlError = if (isLoggedIn) false else it.isRiskControlError,
                        showLogoutConfirmDialog = if (isLoggedIn) it.showLogoutConfirmDialog else false
                    )
                }
                maybeRefreshProfileIfNeeded(session)
            }
        }
    }

    fun onPhoneChanged(phone: String) {
        _uiState.update {
            it.copy(
                phone = phone.filter(Char::isDigit).take(11), // 限制中国大陆手机号最大长度为 11 位
                errorMessage = null,
                isRiskControlError = false
            )
        }
    }

    fun onCaptchaChanged(captcha: String) {
        _uiState.update {
            it.copy(
                captcha = captcha.filter(Char::isDigit),
                errorMessage = null,
                isRiskControlError = false
            )
        }
    }

    fun onCaptchaPanelOpened() {
        stopQrLoginFlow()
        _uiState.update {
            it.copy(
                panel = AuthPanel.CAPTCHA,
                captcha = "",
                errorMessage = null,
                isRiskControlError = false,
                showLogoutConfirmDialog = false
            )
        }
    }

    fun onQrPanelOpened() {
        _uiState.update {
            it.copy(
                panel = AuthPanel.QR,
                qrLoginStart = null,
                qrHint = "等待扫码登录",
                errorMessage = null,
                isRiskControlError = false,
                showLogoutConfirmDialog = false
            )
        }
        startQrLogin()
    }

    fun onAccountSheetOpened() {
        val current = _uiState.value
        if (current.isLoggedIn && current.panel != AuthPanel.METHODS) {
            stopQrLoginFlow()
            _uiState.update {
                it.copy(
                    panel = AuthPanel.METHODS,
                    captcha = "",
                    qrLoginStart = null,
                    qrHint = "等待扫码登录",
                    errorMessage = null,
                    isRiskControlError = false
                )
            }
        }
        maybeRefreshProfileIfNeeded(current.authSession)
    }

    fun onRequestLogout() {
        _uiState.update { it.copy(showLogoutConfirmDialog = true) }
    }

    fun onDismissLogoutConfirm() {
        _uiState.update { it.copy(showLogoutConfirmDialog = false) }
    }

    fun onConfirmLogout() {
        if (_uiState.value.isLoading) return

        stopQrLoginFlow()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showLogoutConfirmDialog = false) }

            val result = logoutUseCase()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    panel = AuthPanel.METHODS,
                    qrLoginStart = null,
                    qrHint = "等待扫码登录"
                )
            }

            result.fold(
                onSuccess = {
                    _snackbarMessage.emit("已退出登录")
                },
                onFailure = { error ->
                    val friendlyMsg = error.toUserFriendlyMessage("同步退出状态")
                    _snackbarMessage.emit("已在本地退出登录（$friendlyMsg）")
                }
            )
        }
    }

    fun onBackToMethods() {
        stopQrLoginFlow()
        _uiState.update {
            it.copy(
                panel = AuthPanel.METHODS,
                qrLoginStart = null,
                qrHint = "等待扫码登录",
                errorMessage = null,
                isRiskControlError = false,
                showLogoutConfirmDialog = false,
                isLoading = false
            )
        }
    }

    fun onSendCaptcha() {
        val current = _uiState.value
        if (current.isLoading || current.captchaCooldownSeconds > 0) return
        val phone = current.phone
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isRiskControlError = false) }
            val result = sendCaptchaUseCase(phone)
            _uiState.update { it.copy(isLoading = false) }
            result.fold(
                onSuccess = {
                    startCaptchaCooldown()
                    _snackbarMessage.emit("验证码已发送")
                },
                onFailure = { error ->
                    val msg = error.toUserFriendlyMessage("发送验证码")
                    _uiState.update {
                        it.copy(
                            errorMessage = msg,
                            isRiskControlError = error.isRiskControlError()
                        )
                    }
                    _snackbarMessage.emit(msg)
                }
            )
        }
    }

    fun onCaptchaLogin() {
        val current = _uiState.value
        if (current.isLoading) return
        val phone = current.phone
        val captcha = current.captcha
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isRiskControlError = false) }
            val result = verifyCaptchaUseCase(phone, captcha)
            _uiState.update { it.copy(isLoading = false) }
            result.fold(
                onSuccess = { session ->
                    stopCaptchaCooldown()
                    _uiState.update {
                        it.copy(
                            isLoggedIn = true,
                            authSession = session,
                            panel = AuthPanel.METHODS,
                            captcha = "",
                            captchaCooldownSeconds = 0,
                            errorMessage = null,
                            isRiskControlError = false
                        )
                    }
                    maybeRefreshProfileIfNeeded(session)
                    _snackbarMessage.emit("登录成功")
                    _dismissSheet.emit(Unit)
                },
                onFailure = { error ->
                    val msg = error.toUserFriendlyMessage("登录")
                    _uiState.update {
                        it.copy(
                            errorMessage = msg,
                            isRiskControlError = error.isRiskControlError()
                        )
                    }
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
        _uiState.update { it.copy(errorMessage = null, isRiskControlError = false) }
    }

    override fun onCleared() {
        stopQrLoginFlow()
        captchaCooldownJob?.cancel()
        captchaCooldownJob = null
        super.onCleared()
    }

    private fun startCaptchaCooldown(durationSeconds: Int = CAPTCHA_COOLDOWN_SECONDS) {
        stopCaptchaCooldown()
        captchaCooldownJob = viewModelScope.launch {
            for (remaining in durationSeconds downTo 1) {
                _uiState.update { it.copy(captchaCooldownSeconds = remaining) }
                delay(1_000L) // 倒计时每秒递减步长（ms）
            }
            _uiState.update { it.copy(captchaCooldownSeconds = 0) }
        }
    }

    private fun stopCaptchaCooldown() {
        captchaCooldownJob?.cancel()
        captchaCooldownJob = null
    }

    private fun startQrLogin() {
        stopQrLoginFlow()
        val requestId = qrRequestId
        qrStartJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, qrHint = "正在生成登录二维码...") }
            val result = startQrLoginUseCase()

            if (!isActive || requestId != qrRequestId || _uiState.value.panel != AuthPanel.QR) {
                return@launch
            }

            _uiState.update { it.copy(isLoading = false) }
            result.fold(
                onSuccess = { qrLoginStart ->
                    if (!isActive || requestId != qrRequestId || _uiState.value.panel != AuthPanel.QR) {
                        return@fold
                    }
                    _uiState.update { it.copy(qrLoginStart = qrLoginStart, qrHint = "请使用网易云音乐App扫码登录") }
                    startQrPolling(qrLoginStart.key, requestId)
                },
                onFailure = { error ->
                    if (!isActive || requestId != qrRequestId || _uiState.value.panel != AuthPanel.QR) {
                        return@fold
                    }
                    val msg = error.toUserFriendlyMessage("获取二维码")
                    _uiState.update { it.copy(qrHint = msg) }
                    _snackbarMessage.emit(msg)
                }
            )
        }
    }

    // 按固定间隔轮询扫码状态；二维码过期（状态码 800）由服务端判定并终止轮询
    private fun startQrPolling(key: String, requestId: Long) {
        stopQrPolling()
        qrPollJob = viewModelScope.launch {
            while (isActive) {
                delay(2000L) // 二维码状态轮询间隔（ms），过短会增加服务端请求压力
                val result = pollQrStatusUseCase(key)
                if (!isActive || requestId != qrRequestId || _uiState.value.panel != AuthPanel.QR) {
                    return@launch
                }
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
                                        panel = AuthPanel.METHODS,
                                        qrLoginStart = null,
                                        qrHint = "等待扫码登录",
                                        errorMessage = null,
                                        isRiskControlError = false
                                    )
                                }
                                maybeRefreshProfileIfNeeded(pollResult.session)
                                _dismissSheet.emit(Unit)
                                stopQrPolling()
                            }
                        }
                    },
                    onFailure = { error ->
                        val friendly = error.toUserFriendlyMessage("检测扫码状态")
                        _uiState.update {
                            it.copy(qrHint = "$friendly，请点击刷新")
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

    private fun stopQrLoginFlow() {
        qrRequestId += 1L // 递增请求序号，使在途异步回调自动失效
        qrStartJob?.cancel()
        qrStartJob = null
        stopQrPolling()
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

    companion object {
        private const val CAPTCHA_COOLDOWN_SECONDS = 60 // 发送验证码后的重发冷却时长（秒）
    }
}
