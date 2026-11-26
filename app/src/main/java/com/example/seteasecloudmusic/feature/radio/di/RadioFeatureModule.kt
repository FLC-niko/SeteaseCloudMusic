package com.example.seteasecloudmusic.feature.radio.di

import com.example.seteasecloudmusic.feature.radio.data.RadioRepositoryImpl
import com.example.seteasecloudmusic.feature.radio.domain.repository.RadioRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 电台功能模块 Hilt 依赖绑定，将 [RadioRepository] 绑定到 [RadioRepositoryImpl]。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RadioFeatureModule {

    @Binds
    @Singleton
    abstract fun bindRadioRepository(
        impl: RadioRepositoryImpl
    ): RadioRepository
}

