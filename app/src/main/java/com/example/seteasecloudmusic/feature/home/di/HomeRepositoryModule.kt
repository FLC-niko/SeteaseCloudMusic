package com.example.seteasecloudmusic.feature.home.di

import com.example.seteasecloudmusic.feature.home.data.HomeRecommendRepositoryImpl
import com.example.seteasecloudmusic.feature.home.domain.repository.HomeRecommendRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindHomeRecommendRepository(
        impl: HomeRecommendRepositoryImpl
    ): HomeRecommendRepository
}