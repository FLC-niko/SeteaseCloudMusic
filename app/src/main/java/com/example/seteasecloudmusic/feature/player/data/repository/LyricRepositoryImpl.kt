package com.example.seteasecloudmusic.feature.player.data.repository

import com.example.seteasecloudmusic.feature.player.data.LyricDataDto
import com.example.seteasecloudmusic.feature.player.data.LyricResponseDto
import com.example.seteasecloudmusic.feature.player.data.LyricService
import com.example.seteasecloudmusic.feature.player.domain.model.LyricContent
import com.example.seteasecloudmusic.feature.player.domain.model.Lyrics
import com.example.seteasecloudmusic.feature.player.domain.repository.LyricRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LyricRepositoryImpl @Inject constructor(
    private val lyricService: LyricService
) : LyricRepository {
    override suspend fun getLyric(songId: Long): Result<Lyrics> = withContext(Dispatchers.IO) {
        try {
            val response = lyricService.getYrcLyrics(songId)
            if (response.code == 200) {
                Result.success(response.toDomain())
            } else {
                Result.failure(Exception("API Error with code: ${response.code}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun LyricResponseDto.toDomain(): Lyrics {
        return Lyrics(
            lrc = lrc.toDomain(),
            klyric = klyric.toDomain(),
            tlyric = tlyric.toDomain(),
            romalrc = romalrc.toDomain(),
            yrc = yrc.toDomain(),
            code = code
        )
    }

    private fun LyricDataDto?.toDomain(): LyricContent? {
        return this?.let { LyricContent(version = it.version, lyric = it.lyric) }
    }
}
