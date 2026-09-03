package com.example.seteasecloudmusic.feature.mine.data

import android.content.Context
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.mine.domain.model.UserPlaylistsGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** “我的” Feature 的缓存，只依赖本 Feature 的领域模型。 */
@Singleton
class MineCacheManager @Inject constructor(
    @param:ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveUserPlaylists(userId: Long, group: UserPlaylistsGroup) {
        runCatching {
            prefs.edit()
                .putString("${KEY_USER_PLAYLISTS}_$userId", gson.toJson(group))
                .apply()
        }
    }

    fun getUserPlaylists(userId: Long): UserPlaylistsGroup? {
        val json = prefs.getString("${KEY_USER_PLAYLISTS}_$userId", null) ?: return null
        return runCatching {
            gson.fromJson<UserPlaylistsGroup>(
                json,
                object : TypeToken<UserPlaylistsGroup>() {}.type
            )
        }.getOrNull()
    }

    fun savePlaylistDetailPreview(detail: PlaylistDetail) {
        runCatching {
            val previewDetail = detail.copy(tracks = detail.tracks.take(20))
            prefs.edit()
                .putString(
                    "${KEY_PLAYLIST_DETAIL_PREVIEW}_${detail.id}",
                    gson.toJson(previewDetail)
                )
                .apply()
        }
    }

    fun getPlaylistDetailPreview(playlistId: Long): PlaylistDetail? {
        val json = prefs.getString("${KEY_PLAYLIST_DETAIL_PREVIEW}_$playlistId", null)
            ?: return null
        return runCatching {
            gson.fromJson<PlaylistDetail>(
                json,
                object : TypeToken<PlaylistDetail>() {}.type
            )
        }.getOrNull()
    }

    companion object {
        // 与旧 DataCacheManager 保持相同键，升级后不丢失已有缓存。
        private const val PREF_NAME = "app_data_cache_prefs"
        private const val KEY_USER_PLAYLISTS = "key_user_playlists"
        private const val KEY_PLAYLIST_DETAIL_PREVIEW = "key_playlist_detail_preview"
    }
}
