package com.example.seteasecloudmusic.core.player

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局曲目“我喜欢的音乐”轻量持久化与状态同步管理器。
 * 确保播放器面板、歌单二级菜单与列表各处的喜欢状态保持实时一致。
 */
object FavoriteTracksManager {
    private const val PREFS_NAME = "favorite_tracks_prefs"
    private const val KEY_LIKED_IDS = "liked_track_ids"

    private val _likedTrackIds = MutableStateFlow<Set<Long>>(emptySet())
    val likedTrackIds: StateFlow<Set<Long>> = _likedTrackIds.asStateFlow()

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getStringSet(KEY_LIKED_IDS, emptySet()) ?: emptySet()
        _likedTrackIds.value = saved.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun isLiked(trackId: Long): Boolean {
        return _likedTrackIds.value.contains(trackId)
    }

    fun toggleLiked(trackId: Long): Boolean {
        val current = _likedTrackIds.value
        val willBeLiked = trackId !in current
        val updated = if (willBeLiked) current + trackId else current - trackId
        _likedTrackIds.value = updated

        appContext?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putStringSet(KEY_LIKED_IDS, updated.map { it.toString() }.toSet()).apply()
        }
        return willBeLiked
    }
}
