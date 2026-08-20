package com.example.seteasecloudmusic.feature.discover.domain.model

data class DiscoverPlaylist(
    val id: Long,
    val name: String,
    val coverUrl: String? = null,
    val playCount: Long? = null,
    val trackCount: Int? = null,
    val copywriter: String? = null
)

data class DiscoverTrackPreview(
    val title: String,
    val artistName: String
)

data class DiscoverToplist(
    val id: Long,
    val name: String,
    val coverUrl: String? = null,
    val updateFrequency: String? = null,
    val previews: List<DiscoverTrackPreview> = emptyList()
)
