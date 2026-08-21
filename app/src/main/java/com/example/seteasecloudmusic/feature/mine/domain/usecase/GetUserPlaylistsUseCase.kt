package com.example.seteasecloudmusic.feature.mine.domain.usecase

import com.example.seteasecloudmusic.feature.mine.domain.model.UserPlaylistsGroup
import com.example.seteasecloudmusic.feature.mine.domain.repository.MineRepository
import javax.inject.Inject

class GetUserPlaylistsUseCase @Inject constructor(
    private val mineRepository: MineRepository
) {
    suspend operator fun invoke(userId: Long): Result<UserPlaylistsGroup> {
        if (userId <= 0L) {
            return Result.failure(IllegalArgumentException("用户 ID 不合法"))
        }
        return mineRepository.getUserPlaylists(userId)
    }
}
