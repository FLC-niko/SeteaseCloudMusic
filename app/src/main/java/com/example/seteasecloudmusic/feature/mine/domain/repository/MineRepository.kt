package com.example.seteasecloudmusic.feature.mine.domain.repository

import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.mine.domain.model.UserPlaylist
import com.example.seteasecloudmusic.feature.mine.domain.model.UserPlaylistsGroup

/**
 * 歌曲已存在于目标歌单的特定业务异常
 */
class PlaylistTrackAlreadyExistsException(message: String = "该歌曲已在歌单中") : Exception(message)

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

    /**
     * 保存本地轻量持久化的歌单详情首屏曲目
     */
    fun savePlaylistDetailPreview(detail: PlaylistDetail)

    /**
     * 清除本地轻量持久化的歌单详情首屏曲目缓存
     */
    fun evictPlaylistDetailPreview(playlistId: Long)

    /**
     * 获取用户喜欢的音乐 ID 列表，并自动同步到本地管理器
     */
    suspend fun getLikelist(userId: Long): Result<List<Long>>

    /**
     * 喜欢或取消喜欢音乐（调用云端 API 并持久化）
     */
    suspend fun likeTrack(trackId: Long, like: Boolean): Result<Boolean>

    /**
     * 添加歌曲到目标歌单
     */
    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long): Result<Unit>

    /**
     * 批量添加歌曲到目标歌单
     */
    suspend fun addTracksToPlaylist(playlistId: Long, trackIds: List<Long>): Result<Unit>

    /**
     * 从目标歌单移除歌曲
     */
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long): Result<Unit>

    /**
     * 批量从目标歌单移除歌曲
     */
    suspend fun removeTracksFromPlaylist(playlistId: Long, trackIds: List<Long>): Result<Unit>

    /**
     * 新建歌单
     */
    suspend fun createPlaylist(name: String, privacy: Int = 0): Result<UserPlaylist>

    /**
     * 更新歌单内歌曲顺序（同步至网易云服务端）
     */
    suspend fun updatePlaylistTrackOrder(playlistId: Long, trackIds: List<Long>): Result<Unit>
}
