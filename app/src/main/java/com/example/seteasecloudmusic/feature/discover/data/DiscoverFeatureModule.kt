package com.example.seteasecloudmusic.feature.discover.data

import com.example.seteasecloudmusic.feature.discover.domain.repository.DiscoverRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DiscoverFeatureModule {
    @Binds
    @Singleton
    abstract fun bindDiscoverRepository(
        impl: DiscoverRepositoryImpl
    ): DiscoverRepository

    companion object {
        @Provides
        @Singleton
        fun provideDiscoverService(retrofit: Retrofit): DiscoverService =
            retrofit.create(DiscoverService::class.java)
    }
}
