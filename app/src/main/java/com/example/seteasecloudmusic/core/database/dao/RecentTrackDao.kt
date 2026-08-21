package com.example.seteasecloudmusic.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seteasecloudmusic.core.database.entity.RecentTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentTrackDao {
    @Query("SELECT * FROM recent_tracks ORDER BY playedAt DESC LIMIT :limit")
    fun getRecentTracks(limit: Int): Flow<List<RecentTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(track: RecentTrackEntity)

    @Query("DELETE FROM recent_tracks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recent_tracks")
    suspend fun clearAll()
}
