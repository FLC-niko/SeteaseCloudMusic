package com.example.seteasecloudmusic.feature.mine.data

import android.content.Context
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.mine.domain.model.UserPlaylistsGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** “我的”页面本地缓存管理器：基于 SharedPreferences 缓存歌单列表与首屏预览，完整曲目以 JSON 文件存储。 */
@Singleton
class MineCacheManager @Inject constructor(
    @ApplicationContext context: Context
) {
    // 惰性初始化磁盘缓存与 Gson，避免依赖注入阶段阻塞主线程
    private val prefs by lazy { context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) }
    private val fullDetailDir by lazy { File(context.cacheDir, FULL_DETAIL_DIR_NAME) }
    private val gson by lazy { Gson() }

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
            val previewDetail = detail.copy(tracks = detail.tracks.take(PREVIEW_TRACK_LIMIT))
            prefs.edit()
                .putString(
                    "${KEY_PLAYLIST_DETAIL_PREVIEW}_${detail.id}",
                    gson.toJson(previewDetail)
                )
                .apply()
            // 若调用方传入的已是完整曲目列表（多于预览上限或已覆盖全部曲目），同步更新完整磁盘缓存，避免新操作被旧缓存覆盖
            if (detail.tracks.size > PREVIEW_TRACK_LIMIT ||
                (detail.trackCount > 0 && detail.tracks.size >= detail.trackCount)
            ) {
                writeFullPlaylistDetailFile(detail)
            }
        }
    }

    fun saveFullPlaylistDetail(detail: PlaylistDetail) {
        runCatching {
            val previewDetail = detail.copy(tracks = detail.tracks.take(PREVIEW_TRACK_LIMIT))
            prefs.edit()
                .putString(
                    "${KEY_PLAYLIST_DETAIL_PREVIEW}_${detail.id}",
                    gson.toJson(previewDetail)
                )
                .apply()
            writeFullPlaylistDetailFile(detail)
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

    fun getFullPlaylistDetail(playlistId: Long): PlaylistDetail? {
        val fullFile = File(fullDetailDir, "$playlistId.json")
        if (fullFile.exists()) {
            val parsed = runCatching {
                gson.fromJson<PlaylistDetail>(
                    fullFile.readText(),
                    object : TypeToken<PlaylistDetail>() {}.type
                )
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return getPlaylistDetailPreview(playlistId)
    }

    fun evictPlaylistDetail(playlistId: Long) {
        runCatching {
            prefs.edit().remove("${KEY_PLAYLIST_DETAIL_PREVIEW}_$playlistId").apply()
            File(fullDetailDir, "$playlistId.json").delete()
        }
    }

    private fun writeFullPlaylistDetailFile(detail: PlaylistDetail) {
        if (!fullDetailDir.exists()) {
            fullDetailDir.mkdirs()
        }
        val target = File(fullDetailDir, "${detail.id}.json")
        val temp = File(fullDetailDir, "${detail.id}.json.tmp")
        temp.writeText(gson.toJson(detail))
        if (!temp.renameTo(target)) {
            target.writeText(temp.readText())
            temp.delete()
        }
    }

    companion object {
        // SharedPreferences 文件名与缓存键（保持键名稳定以保留本地已有缓存）
        private const val PREF_NAME = "app_data_cache_prefs"
        private const val KEY_USER_PLAYLISTS = "key_user_playlists"
        private const val KEY_PLAYLIST_DETAIL_PREVIEW = "key_playlist_detail_preview"
        private const val FULL_DETAIL_DIR_NAME = "playlist_detail_full"
        private const val PREVIEW_TRACK_LIMIT = 20 // 首屏轻量预览缓存最大曲目数（控制 SharedPreferences 体积）
    }
}
