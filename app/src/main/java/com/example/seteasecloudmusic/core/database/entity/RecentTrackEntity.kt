package com.example.seteasecloudmusic.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_tracks")
data class RecentTrackEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val artist: String,
    val album: String,
    val picUrl: String,
    val durationMs: Long,
    val playedAt: Long = System.currentTimeMillis()
)
