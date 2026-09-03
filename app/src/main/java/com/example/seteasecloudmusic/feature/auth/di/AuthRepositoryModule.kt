package com.example.seteasecloudmusic.feature.auth.di

import com.example.seteasecloudmusic.core.auth.AuthStateProvider
import com.example.seteasecloudmusic.feature.auth.data.repository.AuthRepositoryImpl
import com.example.seteasecloudmusic.feature.auth.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAuthStateProvider(
        impl: AuthRepositoryImpl
    ): AuthStateProvider
}
