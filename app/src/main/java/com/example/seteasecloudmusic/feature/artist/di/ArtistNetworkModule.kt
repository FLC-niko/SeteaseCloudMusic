package com.example.seteasecloudmusic.feature.artist.di

import com.example.seteasecloudmusic.feature.artist.data.ArtistService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ArtistNetworkModule {
    @Provides
    @Singleton
    fun provideArtistService(retrofit: Retrofit): ArtistService =
        retrofit.create(ArtistService::class.java)
}