package com.example.seteasecloudmusic.feature.main

import androidx.lifecycle.ViewModel
import com.example.seteasecloudmusic.core.network.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * 宿主导航层 ViewModel：向界面透传全局网络状态与断网提示弹窗状态，并转发重试与关闭操作。
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    val networkMonitor: NetworkMonitor
) : ViewModel() {

    val showOfflineDialog: StateFlow<Boolean> = networkMonitor.showOfflineDialog
    val isRetrying: StateFlow<Boolean> = networkMonitor.isRetrying
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
    val dialogTitle: StateFlow<String> = networkMonitor.dialogTitle
    val dialogDescription: StateFlow<String> = networkMonitor.dialogDescription

    fun retry() {
        networkMonitor.triggerRetry()
    }

    fun dismissOfflineDialog() {
        networkMonitor.dismissOfflineDialog()
    }
}

