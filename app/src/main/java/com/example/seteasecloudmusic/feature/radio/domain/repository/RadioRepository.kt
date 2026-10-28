package com.example.seteasecloudmusic.feature.radio.domain.repository

import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.radio.domain.model.ArtistHotSectionData
import com.example.seteasecloudmusic.feature.radio.domain.model.FollowedArtistSectionData
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioArtistItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioMoodItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioPlaylistItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioRadarItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioRoamItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioToplistItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioTrendItem

interface RadioRepository {
    suspend fun getToplists(): Result<List<RadioToplistItem>>
    suspend fun getRadarPlaylists(): Result<List<RadioRadarItem>>
    suspend fun getRoamItems(): Result<List<RadioRoamItem>>
    suspend fun getRecommendedSingers(): Result<List<RadioArtistItem>>
    suspend fun getRecommendedPlaylists(): Result<List<RadioPlaylistItem>>
    suspend fun getArtistHotSection(): Result<ArtistHotSectionData>
    suspend fun getTrends(): Result<List<RadioTrendItem>>
    suspend fun getMoodPlaylists(): Result<List<RadioMoodItem>>
    suspend fun getFollowedArtistSection(): Result<FollowedArtistSectionData>
    suspend fun getPlaylistTracks(playlistId: Long): Result<List<Track>>
    suspend fun getArtistTopTracks(artistId: Long): Result<List<Track>>
}

