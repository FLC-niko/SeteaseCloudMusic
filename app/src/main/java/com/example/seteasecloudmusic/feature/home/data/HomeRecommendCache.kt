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
    @param:ApplicationContext context: Context
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
            gson.fromJson<List<Track>>(
                json,
                object : TypeToken<List<Track>>() {}.type
            )
        }.getOrNull()
    }

    companion object {
        // 与旧 DataCacheManager 保持相同键，升级后不丢失已有缓存。
        private const val PREF_NAME = "app_data_cache_prefs"
        private const val KEY_DAILY_RECOMMEND = "key_daily_recommend"
    }
}
