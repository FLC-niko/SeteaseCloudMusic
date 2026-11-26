package com.example.seteasecloudmusic.feature.player.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/** 提供歌词模块专用的网络客户端依赖。 */
@Module
@InstallIn(SingletonComponent::class)
object PlayerLyricsModule {

    /** 提供 TTML 镜像竞速请求专用 [OkHttpClient]（单镜像连接/读取超时各 2 s）。 */
    @Provides
    @Singleton
    @Named("ttmlClient")
    fun provideTTMLClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS) // 单镜像建连超时（s）
            .readTimeout(2, TimeUnit.SECONDS)    // 单镜像读取超时（s）
            .build()
    }
}
