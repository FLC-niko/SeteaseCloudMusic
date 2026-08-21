package com.example.seteasecloudmusic.feature.artist.di

import com.example.seteasecloudmusic.feature.artist.data.ArtistRepositoryImpl
import com.example.seteasecloudmusic.feature.artist.domain.repository.ArtistRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ArtistRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindArtistRepository(
        impl: ArtistRepositoryImpl
    ): ArtistRepository
}