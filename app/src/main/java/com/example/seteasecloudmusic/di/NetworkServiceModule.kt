package com.example.seteasecloudmusic.di

import com.example.seteasecloudmusic.feature.artist.data.ArtistService
import com.example.seteasecloudmusic.feature.auth.data.AuthService
import com.example.seteasecloudmusic.feature.discover.data.DiscoverService
import com.example.seteasecloudmusic.feature.home.data.DailyRecommendService
import com.example.seteasecloudmusic.feature.mine.data.MineService
import com.example.seteasecloudmusic.feature.search.data.NeteaseMusicService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkServiceModule {
    @Provides
    @Singleton
    fun provideNeteaseMusicService(retrofit: Retrofit): NeteaseMusicService =
        retrofit.create(NeteaseMusicService::class.java)

    @Provides
    @Singleton
    fun provideArtistService(retrofit: Retrofit): ArtistService =
        retrofit.create(ArtistService::class.java)

    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService =
        retrofit.create(AuthService::class.java)

    @Provides
    @Singleton
    fun provideDailyRecommendService(retrofit: Retrofit): DailyRecommendService =
        retrofit.create(DailyRecommendService::class.java)

    @Provides
    @Singleton
    fun provideDiscoverService(retrofit: Retrofit): DiscoverService =
        retrofit.create(DiscoverService::class.java)

    @Provides
    @Singleton
    fun provideMineService(retrofit: Retrofit): MineService =
        retrofit.create(MineService::class.java)
}