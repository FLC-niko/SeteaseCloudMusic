package com.example.seteasecloudmusic.feature.discover.domain.model

/**
 * 发现页歌单领域模型（推荐歌单与热门歌单共用）。
 */
data class DiscoverPlaylist(
    val id: Long,
    val name: String,
    val coverUrl: String? = null,
    val playCount: Long? = null,
    val trackCount: Int? = null,
    val copywriter: String? = null
)

/**
 * 榜单卡片内的歌曲预览条目。
 */
data class DiscoverTrackPreview(
    val title: String,
    val artistName: String
)

/**
 * 发现页榜单领域模型，包含封面、更新频率及前几首预览曲目。
 */
data class DiscoverToplist(
    val id: Long,
    val name: String,
    val coverUrl: String? = null,
    val updateFrequency: String? = null,
    val previews: List<DiscoverTrackPreview> = emptyList()
)
