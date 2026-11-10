package com.example.seteasecloudmusic.feature.search.domain

/**
 * 热门搜索条目。
 */
data class HotSearchItem(
    val searchWord: String,
    val score: Long = 0L,
    val content: String = "",
    val iconUrl: String? = null,
    val iconType: Int = 0
)
