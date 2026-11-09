package com.example.seteasecloudmusic.feature.home.data

import android.content.Context
import com.example.seteasecloudmusic.core.model.Track
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 首页推荐数据的本地缓存，只保存首页自己的模型。 */
@Singleton
class HomeRecommendCache @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveDailyRecommend(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        runCatching {
            prefs.edit()
                .putString(KEY_DAILY_RECOMMEND, gson.toJson(tracks))
                .apply()
        }
    }

    fun getDailyRecommend(): List<Track>? {
        val json = prefs.getString(KEY_DAILY_RECOMMEND, null) ?: return null
        return runCatching {
            val tracks = gson.fromJson<List<Track>>(
                json,
                // 不用匿名 TypeToken 子类：R8 下匿名类信息不可靠，
                // getParameterized 直接构造泛型类型，行为确定。
                TypeToken.getParameterized(List::class.java, Track::class.java).type
            )
            // Gson 绕过 Kotlin 构造器与非空检查：旧缓存缺字段时会留下 null，
            // 在 R8 去掉 Intrinsics 检查后延迟爆 NPE。这里直接过滤成合法列表，
            // 无效则视为无缓存（调用方走网络刷新）。
            tracks
                ?.filterNotNull()
                ?.filter { !it.title.isNullOrBlank() && it.album != null }
                ?.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }

    fun saveRadarPlaylist(data: com.example.seteasecloudmusic.feature.home.domain.repository.RadarPlaylistData) {
        runCatching {
            prefs.edit()
                .putString(KEY_RADAR_PLAYLIST, gson.toJson(data))
                .apply()
        }
    }

    fun getRadarPlaylist(): com.example.seteasecloudmusic.feature.home.domain.repository.RadarPlaylistData? {
        val json = prefs.getString(KEY_RADAR_PLAYLIST, null) ?: return null
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
        // 与旧 DataCacheManager 保持相同键，升级后不丢失已有缓存。
        private const val PREF_NAME = "app_data_cache_prefs"
        private const val KEY_DAILY_RECOMMEND = "key_daily_recommend"
        private const val KEY_RADAR_PLAYLIST = "key_radar_playlist"
        private const val KEY_FAVORITE_RECOMMEND = "key_favorite_recommend"
    }
}
