package com.example.seteasecloudmusic.core.player

import com.example.seteasecloudmusic.core.model.Track

interface TrackPlaybackPreparer {
    suspend operator fun invoke(track: Track): Result<Track>
}
