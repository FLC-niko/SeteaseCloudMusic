package com.example.seteasecloudmusic.core.database.di

import android.content.Context
import androidx.room.Room
import com.example.seteasecloudmusic.core.database.AppDatabase
import com.example.seteasecloudmusic.core.database.RecentTrackRecorder
import com.example.seteasecloudmusic.core.database.RoomRecentTrackRecorder
import com.example.seteasecloudmusic.core.database.dao.RecentTrackDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "setease_music.db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideRecentTrackDao(appDatabase: AppDatabase): RecentTrackDao {
        return appDatabase.recentTrackDao()
    }

    @Provides
    @Singleton
    fun provideRecentTrackRecorder(
        impl: RoomRecentTrackRecorder
    ): RecentTrackRecorder = impl
}
