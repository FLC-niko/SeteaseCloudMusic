package com.example.seteasecloudmusic.feature.search.di

import com.example.seteasecloudmusic.core.player.TrackPlaybackPreparer
import com.example.seteasecloudmusic.feature.search.data.SearchRepositoryImpl
import com.example.seteasecloudmusic.feature.search.domain.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SearchModule {
    @Binds
    @Singleton
    abstract fun bindSearchRepository(
        impl: SearchRepositoryImpl
    ): SearchRepository

    @Binds
    @Singleton
    abstract fun bindTrackPlaybackPreparer(
        impl: SearchRepositoryImpl
    ): TrackPlaybackPreparer
}