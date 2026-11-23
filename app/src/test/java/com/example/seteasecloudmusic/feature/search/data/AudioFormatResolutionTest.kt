package com.example.seteasecloudmusic.feature.search.data

import androidx.media3.common.MimeTypes
import com.example.seteasecloudmusic.core.model.Album
import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import com.example.seteasecloudmusic.core.settings.OnlineAudioQuality
import com.example.seteasecloudmusic.feature.player.domain.model.LyricSource
import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics
import com.example.seteasecloudmusic.feature.player.presentation.resolveTrackAudioDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioFormatResolutionTest {

    @Test
    fun resolveOnlineAudioFormat_prefersFlacOverMislabeledMp3EncodeType() {
        val format = SearchRepositoryImpl.resolveOnlineAudioFormat(
            url = "https://m801.music.126.net/20261123170000/abc/2113559247.flac?vuutv=test",
            type = "flac",
            encodeType = "mp3",
            level = "hires",
            bitrateKbps = 1829
        )
        assertEquals("FLAC", format)
    }

    @Test
    fun resolveOnlineAudioFormat_usesTypeWhenUrlHasNoExtension() {
        val format = SearchRepositoryImpl.resolveOnlineAudioFormat(
            url = "https://m801.music.126.net/stream?id=2113559247",
            type = "flac",
            encodeType = "mp3",
            level = "lossless",
            bitrateKbps = 960
        )
        assertEquals("FLAC", format)
    }

    @Test
    fun resolveOnlineAudioFormat_rejectsImpossibleMp3EncodeTypeOnHighBitrateLossless() {
        val format = SearchRepositoryImpl.resolveOnlineAudioFormat(
            url = "https://m801.music.126.net/stream?id=2113559247",
            type = null,
            encodeType = "mp3",
            level = "hires",
            bitrateKbps = 1829
        )
        assertNull(format)
    }

    @Test
    fun resolveOnlineAudioFormat_keepsMp3ForStandardStream() {
        val format = SearchRepositoryImpl.resolveOnlineAudioFormat(
            url = "https://m801.music.126.net/20261123170000/abc/12345.mp3",
            type = "mp3",
            encodeType = "mp3",
            level = "standard",
            bitrateKbps = 128
        )
        assertEquals("MP3", format)
    }

    @Test
    fun resolveOnlineAudioFormat_prefersM4aUrlExtensionOverMp4ContainerType() {
        val format = SearchRepositoryImpl.resolveOnlineAudioFormat(
            url = "https://m801.music.126.net/20261123170000/abc/12345.m4a",
            type = "mp4",
            encodeType = "aac",
            level = "exhigh",
            bitrateKbps = 320
        )
        assertEquals("M4A", format)
    }

    @Test
    fun mapMimeTypeToAudioFormat_mapsCommonExoPlayerMimeTypes() {
        assertEquals("FLAC", MusicPlayerController.mapMimeTypeToAudioFormat(MimeTypes.AUDIO_FLAC))
        assertEquals("MP3", MusicPlayerController.mapMimeTypeToAudioFormat(MimeTypes.AUDIO_MPEG))
        assertEquals("M4A", MusicPlayerController.mapMimeTypeToAudioFormat(MimeTypes.AUDIO_AAC, MimeTypes.AUDIO_MP4))
        assertEquals("AAC", MusicPlayerController.mapMimeTypeToAudioFormat(MimeTypes.AUDIO_AAC))
        assertEquals("E-AC-3 (Atmos)", MusicPlayerController.mapMimeTypeToAudioFormat(MimeTypes.AUDIO_E_AC3_JOC))
    }

    @Test
    fun resolveTrackAudioDetails_selfCorrectsStaleMp3FormatOnFlacHiResTrack() {
        val track = Track(
            id = 2113559247L,
            title = "黑玻璃",
            durationMs = 219_000L,
            artists = listOf(Artist(id = 1L, name = "洪嘉豪")),
            album = Album(id = 2L, title = "黑玻璃"),
            playableUrl = "https://m801.music.126.net/20261123170000/abc/2113559247.flac",
            audioBitrateKbps = 1829,
            audioSampleRateHz = 48_000,
            audioFileSizeBytes = 50_121_932L,
            audioFormat = "MP3",
            audioQualityLevel = "hires"
        )

        val details = resolveTrackAudioDetails(
            track = track,
            currentQuality = OnlineAudioQuality.HIRES,
            isLocalTrack = false,
            parsedLyrics = ParsedLyrics(lines = emptyList(), source = LyricSource.LRC),
            queueSource = null
        )

        assertEquals("FLAC", details.formatTag)
        assertEquals("FLAC", details.formatText)
        assertEquals("FLAC · 1829 kbps", details.summaryBadge)
    }

    @Test
    fun resolveTrackAudioDetails_ignoresImpossibleStaleMp3WhenUrlWasStripped() {
        val track = Track(
            id = 2113559247L,
            title = "黑玻璃",
            durationMs = 219_000L,
            artists = listOf(Artist(id = 1L, name = "洪嘉豪")),
            album = Album(id = 2L, title = "黑玻璃"),
            playableUrl = null,
            audioBitrateKbps = 1829,
            audioSampleRateHz = 48_000,
            audioFileSizeBytes = 50_121_932L,
            audioFormat = "MP3",
            audioQualityLevel = "hires"
        )

        val details = resolveTrackAudioDetails(
            track = track,
            currentQuality = OnlineAudioQuality.HIRES,
            isLocalTrack = false,
            parsedLyrics = ParsedLyrics(lines = emptyList(), source = LyricSource.LRC),
            queueSource = null
        )

        assertEquals("FLAC", details.formatTag)
        assertEquals("FLAC (预设)", details.formatText)
        assertEquals("Hi-Res Lossless", details.summaryBadge)
    }
}
