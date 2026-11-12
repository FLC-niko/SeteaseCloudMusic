package com.example.seteasecloudmusic.core

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.hilt.android.HiltAndroidApp
import com.example.seteasecloudmusic.core.player.FavoriteTracksManager

@HiltAndroidApp
class SeteaseCloudMusicApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        FavoriteTracksManager.init(this)
        // 全局注册带磁盘持久化缓存的 ImageLoader，确保冷启动与离线秒显图片
        coil.Coil.setImageLoader(newImageLoader())
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // 这台小米堆上限仅 256m：30%≈77MB 图片缓存太激进，GC 抖动会吞手势帧，先降到 25%
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(200L * 1024 * 1024) // 200 MB 磁盘图片持久化
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false) // 强制本地持久化缓存所有封面图与头像，即使冷启动也能瞬间出图
            // 列表/海报墙默认不 crossfade：几十张图同时做 alpha 动画是启动掉帧大户，
            // 需要渐显的地方各自 .crossfade(true) 显式开启
            .crossfade(false)
            .build()
    }
}