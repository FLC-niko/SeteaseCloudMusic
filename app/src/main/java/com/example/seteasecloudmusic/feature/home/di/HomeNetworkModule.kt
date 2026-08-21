package com.example.seteasecloudmusic.feature.home.di

import com.example.seteasecloudmusic.feature.home.data.DailyRecommendService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HomeNetworkModule {
    @Provides
    @Singleton
    fun provideDailyRecommendService(retrofit: Retrofit): DailyRecommendService =
        retrofit.create(DailyRecommendService::class.java)
}