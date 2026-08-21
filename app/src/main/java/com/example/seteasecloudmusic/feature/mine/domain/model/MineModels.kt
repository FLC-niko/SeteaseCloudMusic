package com.example.seteasecloudmusic.feature.mine.domain.model

import com.example.seteasecloudmusic.core.model.Track

/**
 * 用户歌单领域模型
 */
data class UserPlaylist(
    val id: Long,
    val name: String,
    val coverUrl: String?,
    val trackCount: Int,
    val playCount: Long,
    val isLikedHero: Boolean = false,
    val creatorName: String? = null,
    val description: String? = null
)

/**
 * 用户歌单分类聚合模型
 */
data class UserPlaylistsGroup(
    val likedPlaylist: UserPlaylist? = null,
    val createdPlaylists: List<UserPlaylist> = emptyList(),
    val favoritedPlaylists: List<UserPlaylist> = emptyList()
)

/**
 * 歌单详情聚合领域模型
 */
data class PlaylistDetail(
    val id: Long,
    val name: String,
    val coverUrl: String?,
    val description: String?,
    val trackCount: Int,
    val playCount: Long,
    val creatorName: String?,
    val tracks: List<Track> = emptyList()
)
