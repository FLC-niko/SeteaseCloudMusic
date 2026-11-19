package com.example.seteasecloudmusic.core.ui.components

import androidx.compose.runtime.compositionLocalOf

/**
 * 标识当前页面内容是否真实处于前台可见状态（未被切到后台 Tab、未被全屏播放页或全屏二级页遮挡）。
 * 持续型背景动效（如动态海报墙）应据此自动暂停，避免离屏时继续空跑帧时钟与脏化底层毛玻璃缓存。
 */
val LocalPageVisible = compositionLocalOf { true }
