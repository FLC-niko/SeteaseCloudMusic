package com.example.seteasecloudmusic.core.database

import com.example.seteasecloudmusic.core.database.dao.RecentTrackDao
import com.example.seteasecloudmusic.core.database.entity.RecentTrackEntity
import com.example.seteasecloudmusic.core.model.Track
import javax.inject.Inject

class RoomRecentTrackRecorder @Inject constructor(
    private val recentTrackDao: RecentTrackDao
) : RecentTrackRecorder {

    override suspend fun record(track: Track) {
        recentTrackDao.insertOrUpdate(
            RecentTrackEntity(
                id = track.id,
                name = track.title,
                artist = track.artists.joinToString(" / ") { it.name },
                album = track.album.title,
                picUrl = track.coverUrl.orEmpty(),
                durationMs = track.durationMs ?: 0L
            )
        )
    }
}
