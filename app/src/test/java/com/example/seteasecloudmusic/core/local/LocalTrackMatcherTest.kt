package com.example.seteasecloudmusic.core.local

import com.example.seteasecloudmusic.core.model.Album
import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalTrackMatcherTest {

    private fun track(
        id: Long,
        title: String,
        artist: String,
        durationMs: Long? = 240_000L,
        playableUrl: String? = null
    ) = Track(
        id = id,
        title = title,
        artists = if (artist.isBlank()) emptyList() else listOf(Artist(1L, artist)),
        album = Album(1L, "Test Album"),
        durationMs = durationMs,
        playableUrl = playableUrl,
        isPlayable = true
    )

    @Test
    fun findMatchingLocalTrack_doesNotFalseMatchEmptyNormalizedOrBracketedTitlesWithMediaStoreContentUri() {
        val localMediaStoreTrack = track(
            id = 42L,
            title = "(Intro)",
            artist = "未知歌手",
            durationMs = 30_000L,
            playableUrl = "content://media/external/audio/media/42"
        )
        val onlineParenthesizedTrack = track(
            id = 3448488384L,
            title = "(feat. Lana Del Rey) Lies (Remaster)",
            artist = "Zach Bryan",
            durationMs = 240_000L
        )
        val onlinePunctuationTrack = track(
            id = 999L,
            title = "---",
            artist = "Some Artist",
            durationMs = 200_000L
        )

        assertNull(LocalTrackMatcher.findMatchingLocalTrack(onlineParenthesizedTrack, listOf(localMediaStoreTrack)))
        assertNull(LocalTrackMatcher.findMatchingLocalTrack(onlinePunctuationTrack, listOf(localMediaStoreTrack)))
        assertEquals("lies", LocalTrackMatcher.normalizeTitle("(feat. Lana Del Rey) Lies (Remaster)"))
    }

    @Test
    fun findMatchingLocalTrack_doesNotMatchDifferentArtistOrLargeDurationGapWhenLocalArtistIsUnknown() {
        val unknownArtistLocal = track(
            id = 100L,
            title = "Hero",
            artist = "<unknown>",
            durationMs = 200_000L,
            playableUrl = "/storage/emulated/0/Music/Hero.flac"
        )
        val knownArtistLocal = track(
            id = 101L,
            title = "Hero",
            artist = "Mariah Carey",
            durationMs = 259_000L,
            playableUrl = "/storage/emulated/0/Music/Mariah Carey - Hero.flac"
        )
        val onlineOtherArtist = track(
            id = 200L,
            title = "Hero",
            artist = "Enrique Iglesias",
            durationMs = 264_000L
        )

        // Unknown local artist requires filename artist mention or tight 5s duration match
        assertNull(LocalTrackMatcher.findMatchingLocalTrack(onlineOtherArtist, listOf(unknownArtistLocal)))
        // Known different artist must never match even when duration is within 5s
        assertNull(LocalTrackMatcher.findMatchingLocalTrack(onlineOtherArtist, listOf(knownArtistLocal)))
    }

    @Test
    fun findMatchingLocalTrack_doesNotMatchPartialTitleWithUnknownArtist() {
        val localPlaylistFile = track(
            id = 102L,
            title = "Local Mix",
            artist = "未知歌手",
            durationMs = 250_000L,
            playableUrl = "/storage/emulated/0/Music/周杰伦 - 最伟大的作品.flac"
        )
        val unrelatedShortOnlineSong = track(
            id = 201L,
            title = "作品",
            artist = "陈奕迅",
            durationMs = 248_000L
        )

        assertNull(LocalTrackMatcher.findMatchingLocalTrack(unrelatedShortOnlineSong, listOf(localPlaylistFile)))
    }

    @Test
    fun findMatchingLocalTrack_matchesExactTrackByTitleArtistOrFilename() {
        val localFile = track(
            id = 103L,
            title = "Track 01",
            artist = "未知歌手",
            durationMs = 215_000L,
            playableUrl = "/storage/emulated/0/Music/The Weeknd - Hong Kong.flac"
        )
        val onlineMatch = track(
            id = 301L,
            title = "Hong Kong (feat. 王菲季候风)",
            artist = "The Weeknd",
            durationMs = 217_000L
        )

        assertEquals(localFile, LocalTrackMatcher.findMatchingLocalTrack(onlineMatch, listOf(localFile)))
    }

    @Test
    fun shouldReusePlayerMediaItem_requiresMatchingTrackId() {
        assertFalse(MusicPlayerController.shouldReusePlayerMediaItem(null, 64377L))
        assertFalse(MusicPlayerController.shouldReusePlayerMediaItem("42", 64377L))
        assertFalse(MusicPlayerController.shouldReusePlayerMediaItem("42", null))
        assertTrue(MusicPlayerController.shouldReusePlayerMediaItem("64377", 64377L))
    }
}
