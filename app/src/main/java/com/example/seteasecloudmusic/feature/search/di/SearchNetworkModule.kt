package com.example.seteasecloudmusic.feature.search.di

import com.example.seteasecloudmusic.feature.search.data.NeteaseMusicService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SearchNetworkModule {
    @Provides
    @Singleton
    fun provideNeteaseMusicService(retrofit: Retrofit): NeteaseMusicService =
        retrofit.create(NeteaseMusicService::class.java)
}