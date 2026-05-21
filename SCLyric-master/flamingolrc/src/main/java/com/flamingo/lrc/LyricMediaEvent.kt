package com.flamingo.lrc

/**
 * FlamingoLyricView 媒体事件接口
 */
interface LyricMediaEvent {
    /**
     * 进度跳转事件
     * @param position 要跳转到的进度（毫秒）
     */
    fun onSeek(position: Int)
}
