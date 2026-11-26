package com.example.seteasecloudmusic.feature.discover.domain.repository

import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.discover.domain.model.DiscoverPlaylist
import com.example.seteasecloudmusic.feature.discover.domain.model.DiscoverToplist

/**
 * 发现页领域仓储接口，负责拉取推荐歌单、推荐新歌、榜单及热门歌单数据。
 */
interface DiscoverRepository {
    suspend fun getPersonalizedPlaylists(limit: Int = 8): Result<List<DiscoverPlaylist>> // 默认拉取 8 个推荐歌单

    suspend fun getPersonalizedNewsongs(limit: Int = 10): Result<List<Track>> // 默认拉取 10 首推荐新歌

    suspend fun getToplists(): Result<List<DiscoverToplist>>

    suspend fun getHotPlaylists(
        limit: Int = 10, // 单页拉取条数
        offset: Int = 0 // 分页偏移量
    ): Result<List<DiscoverPlaylist>>
}
