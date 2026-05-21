package com.example.seteasecloudmusic.feature.player.presentation.lyric

/**
 * 歌词解析结果
 *
 * @param entries 解析后的歌词条目，每个条目是 List<Pair<时间戳(毫秒), 文本>>
 * @param sideFlags 对唱标记，true 表示该句应靠右对齐
 */
data class LyricResult(
    val entries: List<List<Pair<Float, String>>>,
    val sideFlags: List<Boolean>
)
