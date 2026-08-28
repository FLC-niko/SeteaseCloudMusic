package com.example.seteasecloudmusic.feature.mine.domain.repository

import com.example.seteasecloudmusic.core.model.Track
import kotlinx.coroutines.flow.StateFlow

/**
 * 本地音乐仓库接口
 */
interface LocalMusicRepository {
    /**
     * 响应式监听本地音乐曲目列表
     */
    val localTracksFlow: StateFlow<List<Track>>

    /**
     * 获取或刷新本地歌曲列表
     */
    suspend fun getLocalTracks(forceRefresh: Boolean = false): List<Track>

    /**
     * 扫描指定目录路径
     */
    suspend fun scanDirectory(directoryPath: String): List<Track>

    /**
     * 获取当前配置的自定义音乐扫描目录
     */
    fun getCustomDirectoryPath(): String?

    /**
     * 保存自定义音乐扫描目录
     */
    fun setCustomDirectoryPath(path: String)

    /**
     * 核心流量优化：在线歌曲匹配本地音频
     * 当播放在线歌单时，若本地库中已存在同名同歌手的本地音频，返回该本地 Track 以直接播放本地文件
     */
    suspend fun findMatchingLocalTrack(onlineTrack: Track): Track?
}

