package com.example.seteasecloudmusic.feature.mine.di

import com.example.seteasecloudmusic.feature.mine.data.LocalMusicRepositoryImpl
import com.example.seteasecloudmusic.feature.mine.data.MineRepositoryImpl
import com.example.seteasecloudmusic.core.local.LocalMusicRepository
import com.example.seteasecloudmusic.feature.mine.domain.repository.MineRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** “我的”页面依赖注入模块：绑定歌单仓库与本地音乐仓库实现。 */
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
