package com.example.seteasecloudmusic.core.player

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 全局曲目“我喜欢的音乐”轻量持久化与状态同步管理器。
 * 确保播放器面板、歌单二级菜单与列表各处的喜欢状态保持实时一致，
 * 并支持自动向网易云音乐云端 API 同步及失败回滚。
 */
object FavoriteTracksManager {
    private const val PREFS_NAME = "favorite_tracks_prefs"
    private const val KEY_LIKED_IDS = "liked_track_ids"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _likedTrackIds = MutableStateFlow<Set<Long>>(emptySet())
    val likedTrackIds: StateFlow<Set<Long>> = _likedTrackIds.asStateFlow()

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var likeApiHandler: (suspend (trackId: Long, like: Boolean) -> Result<Boolean>)? = null

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getStringSet(KEY_LIKED_IDS, emptySet()) ?: emptySet()
        _likedTrackIds.value = saved.mapNotNull { it.toLongOrNull() }.toSet()
    }

    /**
     * 注册云端喜欢/取消喜欢 API 处理器
     */
    fun setLikeApiHandler(handler: suspend (trackId: Long, like: Boolean) -> Result<Boolean>) {
        likeApiHandler = handler
    }

    fun isLiked(trackId: Long): Boolean {
        return _likedTrackIds.value.contains(trackId)
    }

    /**
     * 批量同步已喜欢的音乐 ID 列表（例如从网络接口 /likelist 或“我喜欢的音乐”歌单加载）
     * @param ids 歌曲 ID 列表
     * @param replace true: 完全覆盖本地集合；false: 增量并入本地集合
     */
    fun syncLikedTrackIds(ids: Collection<Long>, replace: Boolean = false) {
        if (ids.isEmpty() && !replace) return
        val current = _likedTrackIds.value
        val updated = if (replace) ids.toSet() else current + ids
        _likedTrackIds.value = updated
        persist(updated)
    }

    fun setLiked(trackId: Long, liked: Boolean) {
        val current = _likedTrackIds.value
        val updated = if (liked) current + trackId else current - trackId
        if (updated != current) {
            _likedTrackIds.value = updated
            persist(updated)
        }
    }

    /**
     * 切换喜欢状态。
     * 本地立即乐观更新，并在后台异步通知网易云云端。若接口失败则自动回滚本地状态。
     */
    fun toggleLiked(trackId: Long): Boolean {
        val current = _likedTrackIds.value
        val willBeLiked = trackId !in current
        val updated = if (willBeLiked) current + trackId else current - trackId
        _likedTrackIds.value = updated
        persist(updated)

        // 异步向云端同步
        likeApiHandler?.let { handler ->
            scope.launch {
                val result = handler(trackId, willBeLiked)
                if (result.isFailure) {
                    // 云端失败，回滚本地状态
                    setLiked(trackId, !willBeLiked)
                }
            }
        }
        return willBeLiked
    }

    private fun persist(ids: Set<Long>) {
        appContext?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putStringSet(KEY_LIKED_IDS, ids.map { it.toString() }.toSet()).apply()
        }
    }
}
