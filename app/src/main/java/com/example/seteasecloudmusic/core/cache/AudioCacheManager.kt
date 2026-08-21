package com.example.seteasecloudmusic.core.cache

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioCacheManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val cacheDir = File(context.cacheDir, "media3_audio_cache")
    private val databaseProvider = StandaloneDatabaseProvider(context)

    private var simpleCache: SimpleCache? = null

    private val _maxCacheMb = MutableStateFlow(loadMaxCacheMb())
    val maxCacheMb: StateFlow<Int> = _maxCacheMb.asStateFlow()

    private val _currentCacheSizeMb = MutableStateFlow(calculateCurrentCacheSizeMb())
    val currentCacheSizeMb: StateFlow<Float> = _currentCacheSizeMb.asStateFlow()

    init {
        initSimpleCache(_maxCacheMb.value)
    }

    @Synchronized
    private fun initSimpleCache(maxMb: Int) {
        if (simpleCache == null) {
            val maxBytes = maxMb.toLong() * 1024L * 1024L
            val evictor = LeastRecentlyUsedCacheEvictor(maxBytes)
            simpleCache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
    }

    @Synchronized
    fun getCache(): SimpleCache {
        var cache = simpleCache
        if (cache == null) {
            initSimpleCache(_maxCacheMb.value)
            cache = simpleCache!!
        }
        return cache
    }

    fun createCacheDataSourceFactory(): DataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)

        val defaultDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        return CacheDataSource.Factory()
            .setCache(getCache())
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun setMaxCacheMb(mb: Int) {
        val safeMb = mb.coerceIn(50, 2048)
        prefs.edit().putInt(KEY_MAX_CACHE_MB, safeMb).apply()
        _maxCacheMb.value = safeMb
    }

    fun clearCache(): Boolean {
        return runCatching {
            val cache = simpleCache
            if (cache != null) {
                for (key in cache.keys) {
                    cache.removeResource(key)
                }
            } else {
                cacheDir.deleteRecursively()
            }
            refreshCacheSize()
            true
        }.getOrDefault(false)
    }

    fun refreshCacheSize() {
        _currentCacheSizeMb.value = calculateCurrentCacheSizeMb()
    }

    private fun loadMaxCacheMb(): Int {
        return prefs.getInt(KEY_MAX_CACHE_MB, DEFAULT_CACHE_MB)
    }

    private fun calculateCurrentCacheSizeMb(): Float {
        return runCatching {
            val cache = simpleCache
            if (cache != null) {
                cache.cacheSpace.toFloat() / (1024f * 1024f)
            } else if (cacheDir.exists()) {
                cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum().toFloat() / (1024f * 1024f)
            } else {
                0f
            }
        }.getOrDefault(0f)
    }

    companion object {
        private const val PREFS_NAME = "audio_cache_settings"
        private const val KEY_MAX_CACHE_MB = "max_audio_cache_mb"
        const val DEFAULT_CACHE_MB = 200 // 默认 200MB
    }
}
