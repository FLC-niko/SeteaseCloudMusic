package com.example.seteasecloudmusic.feature.player.domain.usecase

import com.example.seteasecloudmusic.feature.player.domain.model.Lyrics
import com.example.seteasecloudmusic.feature.player.domain.repository.LyricRepository
import javax.inject.Inject

class GetLyricUseCase @Inject constructor(
    private val lyricRepository: LyricRepository
) {
    suspend operator fun invoke(songId: Long): Result<Lyrics> {
        return lyricRepository.getLyric(songId)
    }
}
