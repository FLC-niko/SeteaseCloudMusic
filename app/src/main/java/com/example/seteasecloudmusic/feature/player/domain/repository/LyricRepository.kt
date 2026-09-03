package com.example.seteasecloudmusic.feature.player.domain.repository

import com.example.seteasecloudmusic.feature.player.domain.model.Lyrics

interface LyricRepository {
    suspend fun getLyric(songId: Long): Result<Lyrics>
}
