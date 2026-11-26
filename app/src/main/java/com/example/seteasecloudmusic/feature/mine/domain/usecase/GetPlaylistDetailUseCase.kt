package com.example.seteasecloudmusic.feature.mine.domain.usecase

import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.mine.domain.repository.MineRepository
import javax.inject.Inject

/** 获取歌单详情及读取本地预览/完整缓存的用例。 */
class GetPlaylistDetailUseCase @Inject constructor(
    private val mineRepository: MineRepository
) {
    suspend operator fun invoke(playlistId: Long): Result<PlaylistDetail> {
        if (playlistId <= 0L) {
            return Result.failure(IllegalArgumentException("歌单 ID 不合法"))
        }
        return mineRepository.getPlaylistDetail(playlistId)
    }

    fun getCachedPreview(playlistId: Long): PlaylistDetail? {
        if (playlistId <= 0L) return null
        return mineRepository.getCachedPlaylistDetailPreview(playlistId)
    }

    suspend fun getCachedFull(playlistId: Long): PlaylistDetail? {
        if (playlistId <= 0L) return null
        return mineRepository.getCachedFullPlaylistDetail(playlistId)
    }
}
