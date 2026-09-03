package com.example.seteasecloudmusic.feature.player.domain

import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics
import com.example.seteasecloudmusic.feature.player.domain.repository.ParsedLyricsRepository
import javax.inject.Inject

class GetLyricsUseCase @Inject constructor(
    private val lyricsRepository: ParsedLyricsRepository
) {
    suspend operator fun invoke(songId: Long): Result<ParsedLyrics> {
        return lyricsRepository.getLyrics(songId)
    }
}
