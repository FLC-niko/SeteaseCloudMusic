package com.example.seteasecloudmusic.feature.player.domain.repository

import com.example.seteasecloudmusic.feature.player.domain.model.Lyrics

/** 原始歌词仓库接口，返回未经逐行解析的歌词领域模型。 */
interface LyricRepository {
    suspend fun getLyric(songId: Long): Result<Lyrics>
}
