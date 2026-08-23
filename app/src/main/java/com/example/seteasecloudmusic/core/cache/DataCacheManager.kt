package com.example.seteasecloudmusic.core.cache

import android.content.Context
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.mine.domain.model.UserPlaylistsGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataCacheManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // ================= 1. 首页每日推荐本地持久化 =================

    fun saveDailyRecommend(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        runCatching {
            val json = gson.toJson(tracks)
            prefs.edit().putString(KEY_DAILY_RECOMMEND, json).apply()
        }
    }

    fun getDailyRecommend(): List<Track>? {
        val json = prefs.getString(KEY_DAILY_RECOMMEND, null) ?: return null
        return runCatching {
            gson.fromJson<List<Track>>(json, object : TypeToken<List<Track>>() {}.type)
        }.getOrNull()
    }

    // ================= 2. 用户歌单列表本地持久化 =================

    fun saveUserPlaylists(userId: Long, group: UserPlaylistsGroup) {
        runCatching {
            val json = gson.toJson(group)
            prefs.edit().putString("${KEY_USER_PLAYLISTS}_$userId", json).apply()
        }
    }

    fun getUserPlaylists(userId: Long): UserPlaylistsGroup? {
        val json = prefs.getString("${KEY_USER_PLAYLISTS}_$userId", null) ?: return null
        return runCatching {
            gson.fromJson<UserPlaylistsGroup>(json, object : TypeToken<UserPlaylistsGroup>() {}.type)
        }.getOrNull()
    }

    // ================= 3. 歌单详情首屏曲目轻量持久化 (仅存前20首, 极小占用) =================

    fun savePlaylistDetailPreview(detail: PlaylistDetail) {
        runCatching {
            // 遵循轻量原则：仅保留前20首歌曲作为首屏秒开缓存（体积极小，<5KB）
            val previewDetail = detail.copy(
                tracks = detail.tracks.take(20)
            )
            val json = gson.toJson(previewDetail)
            prefs.edit().putString("${KEY_PLAYLIST_DETAIL_PREVIEW}_${detail.id}", json).apply()
        }
    }

    fun getPlaylistDetailPreview(playlistId: Long): PlaylistDetail? {
        val json = prefs.getString("${KEY_PLAYLIST_DETAIL_PREVIEW}_$playlistId", null) ?: return null
        return runCatching {
            gson.fromJson<PlaylistDetail>(json, object : TypeToken<PlaylistDetail>() {}.type)
        }.getOrNull()
    }

    companion object {
        private const val PREF_NAME = "app_data_cache_prefs"
        private const val KEY_DAILY_RECOMMEND = "key_daily_recommend"
        private const val KEY_USER_PLAYLISTS = "key_user_playlists"
        private const val KEY_PLAYLIST_DETAIL_PREVIEW = "key_playlist_detail_preview"
    }
}
