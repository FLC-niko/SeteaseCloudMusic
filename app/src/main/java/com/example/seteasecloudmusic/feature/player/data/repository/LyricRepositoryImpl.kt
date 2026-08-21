package com.example.seteasecloudmusic.feature.player.data.repository

import com.example.seteasecloudmusic.feature.player.data.LyricResponse
import com.example.seteasecloudmusic.feature.player.domain.repository.LyricRepository
import com.example.seteasecloudmusic.feature.search.data.NeteaseMusicService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LyricRepositoryImpl @Inject constructor(
    private val musicService: NeteaseMusicService
) : LyricRepository {
    override suspend fun getLyric(songId: Long): Result<LyricResponse> = withContext(Dispatchers.IO) {
        try {
            val response = musicService.getNewLyric(songId)
            if (response.code == 200) {
                Result.success(response)
            } else {
                Result.failure(Exception("API Error with code: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

