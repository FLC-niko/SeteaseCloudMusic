package com.example.seteasecloudmusic.feature.player.di

import com.example.seteasecloudmusic.feature.player.data.LyricService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerLyricsModule {

    @Provides
    @Singleton
    fun provideLyricService(retrofit: Retrofit): LyricService =
        retrofit.create(LyricService::class.java)

    @Provides
    @Singleton
    @Named("ttmlClient")
    fun provideTTMLClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()
    }
}
