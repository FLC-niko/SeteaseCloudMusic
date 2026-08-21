package com.example.seteasecloudmusic.feature.player.domain.usecase

import com.example.seteasecloudmusic.feature.player.data.LyricResponse
import com.example.seteasecloudmusic.feature.player.domain.repository.LyricRepository
import javax.inject.Inject

class GetLyricUseCase @Inject constructor(
    private val lyricRepository: LyricRepository
) {
    suspend operator fun invoke(songId: Long): Result<LyricResponse> {
        return lyricRepository.getLyric(songId)
    }
}

