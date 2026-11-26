package com.example.seteasecloudmusic.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局网络状态监控器与断网重试事件调度中心。
 *
 * 1. 通过 [ConnectivityManager] 实时监听设备网络连通与切换。
 * 2. 网络中断或接口请求遭遇网络故障时，管理离线重试弹窗状态。
 * 3. 管理重试流程并向业务层广播重试事件。
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    // 当前设备是否处于联网状态
    private val _isOnline = MutableStateFlow(checkOnlineStatus())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // 是否展示离线重试弹窗
    private val _showOfflineDialog = MutableStateFlow(false)
    val showOfflineDialog: StateFlow<Boolean> = _showOfflineDialog.asStateFlow()

    // 正在重试检测中的加载状态
    private val _isRetrying = MutableStateFlow(false)
    val isRetrying: StateFlow<Boolean> = _isRetrying.asStateFlow()

    // 离线原因标题与描述
    private val _dialogTitle = MutableStateFlow("网络连接已断开")
    val dialogTitle: StateFlow<String> = _dialogTitle.asStateFlow()

    private val _dialogDescription = MutableStateFlow("未能连接到互联网，部分内容无法加载。\n请检查网络设置后重试。")
    val dialogDescription: StateFlow<String> = _dialogDescription.asStateFlow()

    // 业务重试触发流（供各页面 ViewModel / Navigation 响应重新拉取）
    private val _retryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val retryTrigger: SharedFlow<Unit> = _retryTrigger.asSharedFlow()

    // 底层网络切换事件流（例如 Wi-Fi <-> 蜂窝切换时通知依赖网络出口的组件刷新缓存）
    private val _networkChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val networkChanged: SharedFlow<Unit> = _networkChanged.asSharedFlow()

    private var activeCustomRetryAction: (() -> Unit)? = null

    // 断网判定防抖 Job。
    // 网络切换（例如 WiFi → 蜂窝）时系统会先回调旧网络的 onLost，紧接着回调新网络的 onAvailable，
    // 若在 onLost 里立即弹窗，会出现“弹出来又立刻消失”的误报，因此延迟一小段时间后重查真实联网状态再决定。
    private var offlineCheckJob: Job? = null

    /**
     * 延迟重查真实联网状态，确认仍离线时再显示断网弹窗。
     * @param delayMillis 断网判定防抖延迟（ms），避免 Wi-Fi 与蜂窝切换瞬间误报
     */
    private fun scheduleOfflineCheck(delayMillis: Long = 600L) {
        offlineCheckJob?.cancel()
        offlineCheckJob = scope.launch {
            delay(delayMillis)
            val online = checkOnlineStatus()
            _isOnline.value = online
            if (!online) {
                _showOfflineDialog.value = true
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            // 新网络可用时取消待定的断网判定，避免切换瞬间误报
            offlineCheckJob?.cancel()
            val wasOffline = !_isOnline.value
            val online = checkOnlineStatus()
            android.util.Log.d("NetworkMonitor", "onAvailable: online=$online, wasOffline=$wasOffline")
            _isOnline.value = online
            _networkChanged.tryEmit(Unit)
            if (online && wasOffline && _showOfflineDialog.value) {
                // 网络恢复时等待路由稳定后再触发重试（ms）
                scope.launch {
                    delay(500)
                    triggerRetry()
                }
            }
        }

        override fun onLost(network: Network) {
            android.util.Log.d("NetworkMonitor", "onLost: network disconnected")
            // 不在此处直接弹窗：先防抖重查，避免网络切换瞬间的误报
            scheduleOfflineCheck()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val validated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val online = hasInternet && validated
            android.util.Log.d("NetworkMonitor", "onCapabilitiesChanged: hasInternet=$hasInternet, validated=$validated")
            _isOnline.value = online
            if (!hasInternet) {
                // 同样走防抖路径，与 onLost 保持一致的判定口径
                scheduleOfflineCheck()
            }
        }
    }

    init {
        val initialOnline = checkOnlineStatus()
        _isOnline.value = initialOnline
        android.util.Log.d("NetworkMonitor", "init: initialOnline=$initialOnline")
        if (!initialOnline) {
            _showOfflineDialog.value = true
        }

        try {
            connectivityManager?.registerDefaultNetworkCallback(networkCallback)
        } catch (e: Exception) {
            android.util.Log.w("NetworkMonitor", "Failed to register default network callback", e)
        }
    }

    /**
     * 检查当前活跃网络是否有效
     */
    fun checkOnlineStatus(): Boolean {
        val activeNetwork = connectivityManager?.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * 主动请求呼起离线弹窗（例如接口抛出网络异常时调用）
     */
    fun requestOfflineDialog(
        title: String = "网络连接已断开",
        description: String = "未能连接到互联网，请检查 Wi-Fi 或移动蜂窝网络设置。",
        onRetry: (() -> Unit)? = null
    ) {
        _dialogTitle.value = title
        _dialogDescription.value = description
        activeCustomRetryAction = onRetry
        _showOfflineDialog.value = true
    }

    /**
     * 关闭离线重试弹窗，并取消待定的断网防抖任务以防重复弹出。
     */
    fun dismissOfflineDialog() {
        offlineCheckJob?.cancel()
        _showOfflineDialog.value = false
        _isRetrying.value = false
        activeCustomRetryAction = null
    }

    /**
     * 触发网络重试：检测当前连通性，恢复时执行自定义回调并广播 [retryTrigger]。
     */
    fun triggerRetry() {
        scope.launch {
            _isRetrying.value = true
            // 重试转圈最短展示时长（ms）
            delay(400)
            val online = checkOnlineStatus()
            _isOnline.value = online

            if (online) {
                activeCustomRetryAction?.invoke()
                _retryTrigger.emit(Unit)
                // 成功态收尾后关闭弹窗的延迟（ms）
                delay(300)
                _showOfflineDialog.value = false
                _isRetrying.value = false
                activeCustomRetryAction = null
            } else {
                _isRetrying.value = false
            }
        }
    }
}
