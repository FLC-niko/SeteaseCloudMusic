package com.example.seteasecloudmusic.core.cache

import android.content.Context
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.PlaybackMode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 持久化到本地的播放状态快照，用于应用重启后恢复播放队列与进度。
 */
data class SavedPlaybackState(
    val queueTracks: List<Track> = emptyList(),
    val currentQueueIndex: Int = -1,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val playbackMode: PlaybackMode = PlaybackMode.SEQUENTIAL
)

/**
 * 播放状态本地缓存管理器，使用 SharedPreferences 持久化最近一次播放队列与进度。
 */
@Singleton
class PlaybackCacheManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    /** 保存播放状态；队列为空时跳过写入以避免覆盖有效缓存。 */
    fun savePlaybackState(state: SavedPlaybackState) {
        if (state.queueTracks.isEmpty()) return
        runCatching {
            val json = gson.toJson(state)
            prefs.edit().putString(KEY_PLAYBACK_STATE, json).apply()
        }
    }

    /** 读取本地缓存的播放状态，反序列化失败或不存在时返回 null。 */
    fun getSavedPlaybackState(): SavedPlaybackState? {
        val json = prefs.getString(KEY_PLAYBACK_STATE, null) ?: return null
        return runCatching {
            gson.fromJson<SavedPlaybackState>(json, object : TypeToken<SavedPlaybackState>() {}.type)
        }.getOrNull()
    }

    /** 清除已保存的播放状态缓存。 */
    fun clearPlaybackState() {
        prefs.edit().remove(KEY_PLAYBACK_STATE).apply()
    }

    companion object {
        private const val PREF_NAME = "playback_cache_prefs"             // 播放状态缓存 SharedPreferences 文件名
        private const val KEY_PLAYBACK_STATE = "saved_playback_state"    // 序列化播放状态 JSON 的存储 Key
    }
}
