package com.example.seteasecloudmusic.feature.player.domain.repository

import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics

/** 已解析歌词仓库接口，直接返回带时间轴和词元结构的 [ParsedLyrics]。 */
interface ParsedLyricsRepository {
    suspend fun getLyrics(songId: Long): Result<ParsedLyrics>
}
