package com.example.seteasecloudmusic.feature.search.domain

import com.example.seteasecloudmusic.core.model.Track
import javax.inject.Inject

/**
 * 为曲目填充可播放直链并更新 `isPlayable` 状态的业务用例。
 */
class PrepareTrackForPlaybackUseCase @Inject constructor(
    private val getTrackUrlUseCase: GetTrackUrlUseCase
) {
    suspend operator fun invoke(track: Track): Result<Track> {
        return getTrackUrlUseCase(track.id).map { url ->
            track.copy(playableUrl = url, isPlayable = url.isNotBlank())
        }
    }
}