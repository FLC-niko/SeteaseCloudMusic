package com.example.seteasecloudmusic.feature.player.domain.repository

import com.example.seteasecloudmusic.feature.player.data.LyricResponse

interface LyricRepository {
    suspend fun getLyric(songId: Long): Result<LyricResponse>
}

