package com.example.seteasecloudmusic.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
 * 1. 通过 Android 系统 [ConnectivityManager] 实时回调监听设备网络的连通与断开。
 * 2. 在网络彻底中断或数据请求遇到网络故障时，触发 iOS 26 液态玻璃风格弹窗。
 * 3. 管理重试状态并向业务层广播重试事件。
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

    // 控制是否展示 iOS 26 离线重试弹窗
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

    private var activeCustomRetryAction: (() -> Unit)? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val wasOffline = !_isOnline.value
            val online = checkOnlineStatus()
            android.util.Log.d("NetworkMonitor", "onAvailable: online=$online, wasOffline=$wasOffline")
            _isOnline.value = online
            if (online && wasOffline && _showOfflineDialog.value) {
                // 网络恢复连接时自动通知刷新并关闭弹窗
                scope.launch {
                    delay(500)
                    triggerRetry()
                }
            }
        }

        override fun onLost(network: Network) {
            android.util.Log.d("NetworkMonitor", "onLost: network disconnected")
            _isOnline.value = false
            // 网络掉线时，自动拉起重试弹窗提示用户
            _showOfflineDialog.value = true
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
                _showOfflineDialog.value = true
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
     * 关闭弹窗
     */
    fun dismissOfflineDialog() {
        _showOfflineDialog.value = false
        _isRetrying.value = false
        activeCustomRetryAction = null
    }

    /**
     * 触发重试逻辑
     */
    fun triggerRetry() {
        scope.launch {
            _isRetrying.value = true
            // 轻微延迟给予用户可见的转圈物理反馈
            delay(400)
            val online = checkOnlineStatus()
            _isOnline.value = online

            if (online) {
                // 网络已恢复，执行重试动作并关闭弹窗
                activeCustomRetryAction?.invoke()
                _retryTrigger.emit(Unit)
                delay(300)
                _showOfflineDialog.value = false
                _isRetrying.value = false
                activeCustomRetryAction = null
            } else {
                // 仍无网络，停留在弹窗，恢复按钮状态
                _isRetrying.value = false
            }
        }
    }
}
