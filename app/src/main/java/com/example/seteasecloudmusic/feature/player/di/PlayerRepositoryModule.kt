package com.example.seteasecloudmusic.feature.player.di

import com.example.seteasecloudmusic.feature.player.data.LyricsRepository
import com.example.seteasecloudmusic.feature.player.data.repository.LyricRepositoryImpl
import com.example.seteasecloudmusic.feature.player.domain.repository.ParsedLyricsRepository
import com.example.seteasecloudmusic.feature.player.domain.repository.LyricRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 绑定播放器歌词仓库接口与实现。 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindLyricRepository(
        impl: LyricRepositoryImpl
    ): LyricRepository

    @Binds
    @Singleton
    abstract fun bindParsedLyricsRepository(
        impl: LyricsRepository
    ): ParsedLyricsRepository
}
