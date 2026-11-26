package com.example.seteasecloudmusic.feature.auth.di

import com.example.seteasecloudmusic.feature.auth.domain.repository.AuthRepository
import com.example.seteasecloudmusic.feature.auth.usecase.EmailLoginUseCase
import com.example.seteasecloudmusic.feature.auth.usecase.GuestLoginUseCase
import com.example.seteasecloudmusic.feature.auth.usecase.LogoutUseCase
import com.example.seteasecloudmusic.feature.auth.usecase.ObserveAuthStateUseCase
import com.example.seteasecloudmusic.feature.auth.usecase.PhoneLoginUseCase
import com.example.seteasecloudmusic.feature.auth.usecase.PollQrStatusUseCase
import com.example.seteasecloudmusic.feature.auth.usecase.RefreshSessionUseCase
import com.example.seteasecloudmusic.feature.auth.usecase.SendCaptchaUseCase
import com.example.seteasecloudmusic.feature.auth.usecase.StartQrLoginUseCase
import com.example.seteasecloudmusic.feature.auth.usecase.VerifyCaptchaUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 提供认证模块各类 UseCase 实例的 Hilt 模块。
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthUseCaseModule {

    @Provides
    fun providePhoneLoginUseCase(repository: AuthRepository): PhoneLoginUseCase =
        PhoneLoginUseCase(repository)

    @Provides
    fun provideVerifyCaptchaUseCase(repository: AuthRepository): VerifyCaptchaUseCase =
        VerifyCaptchaUseCase(repository)

    @Provides
    fun provideSendCaptchaUseCase(repository: AuthRepository): SendCaptchaUseCase =
        SendCaptchaUseCase(repository)

    @Provides
    fun provideStartQrLoginUseCase(repository: AuthRepository): StartQrLoginUseCase =
        StartQrLoginUseCase(repository)

    @Provides
    fun providePollQrStatusUseCase(repository: AuthRepository): PollQrStatusUseCase =
        PollQrStatusUseCase(repository)

    @Provides
    fun provideObserveAuthStateUseCase(repository: AuthRepository): ObserveAuthStateUseCase =
        ObserveAuthStateUseCase(repository)

    @Provides
    fun provideEmailLoginUseCase(repository: AuthRepository): EmailLoginUseCase =
        EmailLoginUseCase(repository)

    @Provides
    fun provideGuestLoginUseCase(repository: AuthRepository): GuestLoginUseCase =
        GuestLoginUseCase(repository)

    @Provides
    fun provideLogoutUseCase(repository: AuthRepository): LogoutUseCase =
        LogoutUseCase(repository)

    @Provides
    fun provideRefreshSessionUseCase(repository: AuthRepository): RefreshSessionUseCase =
        RefreshSessionUseCase(repository)
}
