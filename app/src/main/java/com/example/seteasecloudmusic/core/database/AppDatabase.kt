package com.example.seteasecloudmusic.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.seteasecloudmusic.core.database.dao.RecentTrackDao
import com.example.seteasecloudmusic.core.database.entity.RecentTrackEntity

@Database(
    entities = [RecentTrackEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recentTrackDao(): RecentTrackDao
}
