package com.example.seteasecloudmusic.feature.player.domain

import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics
import com.example.seteasecloudmusic.feature.player.domain.repository.ParsedLyricsRepository
import javax.inject.Inject

/** 获取已解析的逐字/逐行歌词用例（支持 TTML / YRC / LRC 多源降级）。 */
class GetLyricsUseCase @Inject constructor(
    private val lyricsRepository: ParsedLyricsRepository
) {
    suspend operator fun invoke(songId: Long): Result<ParsedLyrics> {
        return lyricsRepository.getLyrics(songId)
    }
}
