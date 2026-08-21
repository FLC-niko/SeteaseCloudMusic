package com.example.seteasecloudmusic.feature.mine.domain.repository

import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.mine.domain.model.UserPlaylistsGroup

/**
 * 我的页面业务仓库接口
 */
interface MineRepository {

    /**
     * 获取指定用户的歌单列表（含分类：我喜欢的、创建的、收藏的）
     */
    suspend fun getUserPlaylists(userId: Long): Result<UserPlaylistsGroup>

    /**
     * 读取本地持久化的用户歌单列表
     */
    fun getCachedUserPlaylists(userId: Long): UserPlaylistsGroup?

    /**
     * 获取歌单详情及其全部曲目
     */
    suspend fun getPlaylistDetail(playlistId: Long): Result<PlaylistDetail>

    /**
     * 读取本地轻量持久化的歌单详情首屏曲目（前20首）
     */
    fun getCachedPlaylistDetailPreview(playlistId: Long): PlaylistDetail?
}
