package com.example.seteasecloudmusic.feature.player.domain.usecase

import com.example.seteasecloudmusic.feature.player.domain.model.Lyrics
import com.example.seteasecloudmusic.feature.player.domain.repository.LyricRepository
import javax.inject.Inject

/** 获取原始歌词领域模型 [Lyrics] 的用例。 */
class GetLyricUseCase @Inject constructor(
    private val lyricRepository: LyricRepository
) {
    suspend operator fun invoke(songId: Long): Result<Lyrics> {
        return lyricRepository.getLyric(songId)
    }
}
