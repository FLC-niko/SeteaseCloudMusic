package com.example.seteasecloudmusic.core.local

import com.example.seteasecloudmusic.core.model.Track
import kotlinx.coroutines.flow.StateFlow

/**
 * 应用级本地音乐能力。
 *
 * 本地曲库既被“我的”页面展示，也被播放准备流程用于本地命中，
 * 因此它属于 core 能力，而不是某个具体 Feature 的领域模型。
 */
interface LocalMusicRepository {
    val localTracksFlow: StateFlow<List<Track>>

    suspend fun getLocalTracks(forceRefresh: Boolean = false): List<Track>

    suspend fun scanDirectory(directoryPath: String): List<Track>

    fun getCustomDirectoryPath(): String?

    fun setCustomDirectoryPath(path: String)

    suspend fun findMatchingLocalTrack(onlineTrack: Track): Track?
}
