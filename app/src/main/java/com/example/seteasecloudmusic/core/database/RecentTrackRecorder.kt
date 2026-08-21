package com.example.seteasecloudmusic.core.database

import com.example.seteasecloudmusic.core.model.Track

interface RecentTrackRecorder {
    suspend fun record(track: Track)
}
