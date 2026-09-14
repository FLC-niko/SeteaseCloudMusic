package com.example.seteasecloudmusic.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seteasecloudmusic.core.network.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 宿主导航与全局应用级状态机 ViewModel。
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

