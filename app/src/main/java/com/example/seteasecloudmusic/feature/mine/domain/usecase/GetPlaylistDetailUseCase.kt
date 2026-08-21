package com.example.seteasecloudmusic.feature.mine.domain.usecase

import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.mine.domain.repository.MineRepository
import javax.inject.Inject

class GetPlaylistDetailUseCase @Inject constructor(
    private val mineRepository: MineRepository
) {
    suspend operator fun invoke(playlistId: Long): Result<PlaylistDetail> {
        if (playlistId <= 0L) {
            return Result.failure(IllegalArgumentException("歌单 ID 不合法"))
        }
        return mineRepository.getPlaylistDetail(playlistId)
    }
}
