package com.example.seteasecloudmusic.feature.discover.data

import com.example.seteasecloudmusic.feature.discover.domain.repository.DiscoverRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DiscoverFeatureModule {
    @Binds
    @Singleton
    abstract fun bindDiscoverRepository(
        impl: DiscoverRepositoryImpl
    ): DiscoverRepository
}
