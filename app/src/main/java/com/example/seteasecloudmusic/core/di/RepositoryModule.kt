package com.example.seteasecloudmusic.core.di

import com.example.seteasecloudmusic.feature.artist.data.ArtistRepositoryImpl
import com.example.seteasecloudmusic.feature.artist.domain.repository.ArtistRepository
import com.example.seteasecloudmusic.feature.auth.domain.repository.AuthRepository
import com.example.seteasecloudmusic.feature.auth.domain.repository.AuthRepositoryImpl
import com.example.seteasecloudmusic.feature.home.data.HomeRecommendRepositoryImpl
import com.example.seteasecloudmusic.feature.home.domain.repository.HomeRecommendRepository
import com.example.seteasecloudmusic.feature.player.data.repository.LyricRepositoryImpl
import com.example.seteasecloudmusic.feature.player.domain.repository.LyricRepository
import com.example.seteasecloudmusic.feature.search.data.SearchRepositoryImpl
import com.example.seteasecloudmusic.feature.search.domain.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindArtistRepository(
        impl: ArtistRepositoryImpl
    ): ArtistRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(
        impl: SearchRepositoryImpl
    ): SearchRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindHomeRecommendRepository(
        impl: HomeRecommendRepositoryImpl
    ): HomeRecommendRepository

    @Binds
    @Singleton
    abstract fun bindLyricRepository(
        impl: LyricRepositoryImpl
    ): LyricRepository
}