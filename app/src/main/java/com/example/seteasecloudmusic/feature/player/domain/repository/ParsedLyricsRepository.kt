package com.example.seteasecloudmusic.feature.player.domain.repository

import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics

interface ParsedLyricsRepository {
    suspend fun getLyrics(songId: Long): Result<ParsedLyrics>
}
