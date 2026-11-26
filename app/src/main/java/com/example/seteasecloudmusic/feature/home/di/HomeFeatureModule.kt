package com.example.seteasecloudmusic.feature.home.di

import com.example.seteasecloudmusic.feature.home.data.HomeRecommendRepositoryImpl
import com.example.seteasecloudmusic.feature.home.domain.repository.HomeRecommendRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 首页模块的 Hilt 依赖绑定配置，提供推荐仓库单例实现。 */
@Module
@InstallIn(SingletonComponent::class)
abstract class HomeFeatureModule {
    @Binds
    @Singleton
    abstract fun bindHomeRecommendRepository(
        impl: HomeRecommendRepositoryImpl
    ): HomeRecommendRepository
}
