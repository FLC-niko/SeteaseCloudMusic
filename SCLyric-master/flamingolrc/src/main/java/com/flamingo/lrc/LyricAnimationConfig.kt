package com.flamingo.lrc

import androidx.compose.runtime.Stable

/**
 * FlamingoLyricView 动画效果控制类
 */
@Stable
data class LyricAnimationConfig(
    val ignoreSystemAnimationScale: Boolean = false
)
