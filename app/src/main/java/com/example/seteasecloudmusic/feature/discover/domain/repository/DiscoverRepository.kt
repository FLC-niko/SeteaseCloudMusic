package com.example.seteasecloudmusic.feature.discover.domain.repository

import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.discover.domain.model.DiscoverPlaylist
import com.example.seteasecloudmusic.feature.discover.domain.model.DiscoverToplist

interface DiscoverRepository {
    suspend fun getPersonalizedPlaylists(limit: Int = 8): Result<List<DiscoverPlaylist>>

    suspend fun getPersonalizedNewsongs(limit: Int = 10): Result<List<Track>>

    suspend fun getToplists(): Result<List<DiscoverToplist>>

    suspend fun getHotPlaylists(
        limit: Int = 10,
        offset: Int = 0
    ): Result<List<DiscoverPlaylist>>
}
