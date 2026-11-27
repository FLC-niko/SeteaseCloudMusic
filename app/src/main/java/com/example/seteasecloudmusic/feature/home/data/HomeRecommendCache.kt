package com.example.seteasecloudmusic.feature.home.data

import android.content.Context
import com.example.seteasecloudmusic.core.model.Track
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 首页推荐数据的本地持久化缓存（无固定 TTL，每次联网成功后覆盖更新）。 */
@Singleton
@Suppress("SENSELESS_COMPARISON")
class HomeRecommendCache @Inject constructor(
    @ApplicationContext context: Context
) {
    // 惰性初始化 SharedPreferences 与 Gson，确保在 IO 线程首次读取时才触发加载
    private val prefs by lazy { context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) }
    private val gson by lazy { Gson() }

    fun saveDailyRecommend(tracks: List<Track>) = saveTrackList(KEY_DAILY_RECOMMEND, tracks)

    fun getDailyRecommend(): List<Track>? = getTrackList(KEY_DAILY_RECOMMEND)

    fun savePrivateDjSongs(tracks: List<Track>) = saveTrackList(KEY_PRIVATE_DJ_SONGS, tracks)

    fun getPrivateDjSongs(): List<Track>? = getTrackList(KEY_PRIVATE_DJ_SONGS)

    fun saveSimiSongs(tracks: List<Track>) = saveTrackList(KEY_SIMI_SONGS, tracks)

    fun getSimiSongs(): List<Track>? = getTrackList(KEY_SIMI_SONGS)

    fun saveHeartModeSongs(tracks: List<Track>) = saveTrackList(KEY_HEART_MODE_SONGS, tracks)

    fun getHeartModeSongs(): List<Track>? = getTrackList(KEY_HEART_MODE_SONGS)

    private fun saveTrackList(key: String, tracks: List<Track>) {
        if (tracks.isEmpty()) return
        runCatching {
            prefs.edit()
                .putString(key, gson.toJson(tracks))
                .apply()
        }
    }

    private fun getTrackList(key: String): List<Track>? {
        val json = prefs.getString(key, null) ?: return null
        return runCatching {
            val tracks = gson.fromJson<List<Track>>(
                json,
                // 不用匿名 TypeToken 子类：R8 下匿名类信息不可靠，
                // getParameterized 直接构造泛型类型，行为确定。
                TypeToken.getParameterized(List::class.java, Track::class.java).type
            )
            // Gson 反序列化会绕过 Kotlin 构造器与非空检查，缺字段时可能留下 null；
            // 此处直接过滤无效条目，结果为空时视为未命中缓存。
            tracks
                ?.filterNotNull()
                ?.filter { !it.title.isNullOrBlank() && it.album != null }
                ?.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }

    fun saveRadarPlaylist(data: com.example.seteasecloudmusic.feature.home.domain.repository.RadarPlaylistData) =
        saveRadarData(KEY_RADAR_PLAYLIST, data)

    fun getRadarPlaylist(): com.example.seteasecloudmusic.feature.home.domain.repository.RadarPlaylistData? =
        getRadarData(KEY_RADAR_PLAYLIST)

    fun saveNewSongRadarPlaylist(data: com.example.seteasecloudmusic.feature.home.domain.repository.RadarPlaylistData) =
        saveRadarData(KEY_NEW_SONG_RADAR_PLAYLIST, data)

    fun getNewSongRadarPlaylist(): com.example.seteasecloudmusic.feature.home.domain.repository.RadarPlaylistData? =
        getRadarData(KEY_NEW_SONG_RADAR_PLAYLIST)

    private fun saveRadarData(
        key: String,
        data: com.example.seteasecloudmusic.feature.home.domain.repository.RadarPlaylistData
    ) {
        runCatching {
            prefs.edit()
                .putString(key, gson.toJson(data))
                .apply()
        }
    }

    private fun getRadarData(key: String): com.example.seteasecloudmusic.feature.home.domain.repository.RadarPlaylistData? {
        val json = prefs.getString(key, null) ?: return null
        return runCatching {
            gson.fromJson<com.example.seteasecloudmusic.feature.home.domain.repository.RadarPlaylistData>(
                json,
                com.example.seteasecloudmusic.feature.home.domain.repository.RadarPlaylistData::class.java
            )?.takeIf { data ->
                // 同上：Gson 可能留下 null 字段，非法直接视为无缓存
                !data.name.isNullOrBlank() && data.tracks != null &&
                    data.tracks.filterNotNull().any { !it.title.isNullOrBlank() && it.album != null }
            }?.let { data ->
                data.copy(tracks = data.tracks.filterNotNull()
                    .filter { !it.title.isNullOrBlank() && it.album != null })
            }
        }.getOrNull()
    }

    fun saveFavoriteRecommend(data: com.example.seteasecloudmusic.feature.home.domain.repository.FavoriteRecommendSectionData) {
        if (data.tracks.isEmpty()) return
        runCatching {
            prefs.edit()
                .putString(KEY_FAVORITE_RECOMMEND, gson.toJson(data))
                .apply()
        }
    }

    fun getFavoriteRecommend(): com.example.seteasecloudmusic.feature.home.domain.repository.FavoriteRecommendSectionData? {
        val json = prefs.getString(KEY_FAVORITE_RECOMMEND, null) ?: return null
        return runCatching {
            gson.fromJson<com.example.seteasecloudmusic.feature.home.domain.repository.FavoriteRecommendSectionData>(
                json,
                com.example.seteasecloudmusic.feature.home.domain.repository.FavoriteRecommendSectionData::class.java
            )?.takeIf { data ->
                data.tracks != null && data.tracks.filterNotNull().isNotEmpty()
            }?.let { data ->
                data.copy(tracks = data.tracks.filterNotNull()
                    .filter { !it.title.isNullOrBlank() && it.album != null })
            }
        }.getOrNull()
    }

    companion object {
        // SharedPreferences 文件名与各推荐模块的缓存键（无 TTL 过期时间）
        private const val PREF_NAME = "app_data_cache_prefs"
        private const val KEY_DAILY_RECOMMEND = "key_daily_recommend"
        private const val KEY_RADAR_PLAYLIST = "key_radar_playlist"
        private const val KEY_NEW_SONG_RADAR_PLAYLIST = "key_new_song_radar_playlist"
        private const val KEY_PRIVATE_DJ_SONGS = "key_private_dj_songs"
        private const val KEY_SIMI_SONGS = "key_simi_songs"
        private const val KEY_HEART_MODE_SONGS = "key_heart_mode_songs"
        private const val KEY_FAVORITE_RECOMMEND = "key_favorite_recommend"
    }
}
