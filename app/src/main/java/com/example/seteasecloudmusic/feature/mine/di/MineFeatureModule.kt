package com.example.seteasecloudmusic.feature.mine.di

import com.example.seteasecloudmusic.feature.mine.data.LocalMusicRepositoryImpl
import com.example.seteasecloudmusic.feature.mine.data.MineRepositoryImpl
import com.example.seteasecloudmusic.feature.mine.domain.repository.LocalMusicRepository
import com.example.seteasecloudmusic.feature.mine.domain.repository.MineRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MineFeatureModule {

    @Binds
    @Singleton
    abstract fun bindMineRepository(
        impl: MineRepositoryImpl
    ): MineRepository

    @Binds
    @Singleton
    abstract fun bindLocalMusicRepository(
        impl: LocalMusicRepositoryImpl
    ): LocalMusicRepository
}
