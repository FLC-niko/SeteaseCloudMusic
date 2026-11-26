package com.example.seteasecloudmusic.core

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.hilt.android.HiltAndroidApp
import com.example.seteasecloudmusic.core.player.FavoriteTracksManager

/**
 * 应用入口，负责初始化收藏状态并配置全局 Coil 图片缓存策略。
 */
@HiltAndroidApp
class SeteaseCloudMusicApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        FavoriteTracksManager.init(this)
        // 注册全局 ImageLoader，开启本地内存与磁盘缓存
        coil.Coil.setImageLoader(newImageLoader())
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // 内存缓存占可用堆上限的比例（降低可减少低内存设备的 GC 抖动）
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    // 磁盘图片缓存目录名
                    .directory(cacheDir.resolve("image_cache"))
                    // 磁盘图片缓存上限（字节，当前为 200 MB）
                    .maxSizeBytes(200L * 1024 * 1024)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            // 忽略服务端缓存头，统一按本地策略缓存封面与头像
            .respectCacheHeaders(false)
            // 默认关闭淡入动画以降低列表同时加载多图时的渲染开销，需渐显处由调用方显式开启
            .crossfade(false)
            .build()
    }
}