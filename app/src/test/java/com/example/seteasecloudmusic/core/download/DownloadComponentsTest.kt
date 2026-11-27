package com.example.seteasecloudmusic.core.download

import com.example.seteasecloudmusic.core.settings.PlayerSettingsManager
import com.example.seteasecloudmusic.feature.player.domain.model.LyricLine
import com.example.seteasecloudmusic.feature.player.domain.model.LyricSource
import com.example.seteasecloudmusic.feature.player.domain.model.LyricWord
import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics
import com.example.seteasecloudmusic.feature.search.data.SearchRepositoryImpl
import com.example.seteasecloudmusic.feature.search.data.SongUrlItemResponse
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.io.path.createTempDirectory

class DownloadComponentsTest {

    @Test
    fun downloadFileNames_sanitizesIllegalReservedAndLongNames() {
        assertEquals("我的_歌单_精选_", DownloadFileNames.playlistName("我的/歌单:精选*"))
        assertEquals("默认下载", DownloadFileNames.playlistName("...   "))
        assertEquals("_CON", DownloadFileNames.playlistName("CON"))
        assertEquals("_LPT1", DownloadFileNames.playlistName("LPT1"))
        assertEquals("晴天 - 周杰伦", DownloadFileNames.songBaseName("晴天", "周杰伦"))
        val longName = DownloadFileNames.songBaseName("a".repeat(150), "b".repeat(150))
        assertTrue(longName.toByteArray(Charsets.UTF_8).size <= 180)
    }

    @Test
    fun detectAudioFileFormat_detectsFlacWavMp3M4aOggAndRejectsHtml() {
        val tempDir = createTempDirectory("audio_fmt_test").toFile()
        try {
            // 1. FLAC: fLaC + last-metadata-block flag (0x80) + STREAMINFO length 34
            val flacFile = File(tempDir, "test.bin")
            flacFile.writeBytes(byteArrayOf('f'.code.toByte(), 'L'.code.toByte(), 'a'.code.toByte(), 'C'.code.toByte(), 0x00, 0x00, 0x00, 34) + ByteArray(34))
            assertEquals(AudioFileFormat.FLAC, detectAudioFileFormat(flacFile))

            // 2. WAV: RIFF + size + WAVE + fmt (16 bytes) + data (4 bytes)
            val wavBytes = ByteArrayOutputStream().apply {
                write("RIFF".toByteArray())
                write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(40).array())
                write("WAVE".toByteArray())
                write("fmt ".toByteArray())
                write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(16).array())
                write(ByteArray(16))
                write("data".toByteArray())
                write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(4).array())
                write(ByteArray(4))
            }.toByteArray()
            val wavFile = File(tempDir, "test.wav.bin")
            wavFile.writeBytes(wavBytes)
            assertEquals(AudioFileFormat.WAV, detectAudioFileFormat(wavFile))

            // 3. MP3: ID3v2 header (10 bytes, size 0) + 3 consecutive MPEG1 Layer3 128kbps 44.1kHz frames (417 bytes each)
            val mp3FrameHeader = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x00)
            val singleFrame = mp3FrameHeader + ByteArray(417 - 4)
            val id3Header = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 4, 0, 0, 0, 0, 0, 0)
            val mp3File = File(tempDir, "test.mp3.bin")
            mp3File.writeBytes(id3Header + singleFrame + singleFrame + singleFrame)
            assertEquals(AudioFileFormat.MP3, detectAudioFileFormat(mp3File))

            // 4. M4A: ftyp box with M4A  brand
            val m4aBytes = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN)
                .putInt(20)
                .put("ftyp".toByteArray())
                .put("M4A ".toByteArray())
                .putInt(0)
                .put("isom".toByteArray())
                .array()
            val m4aFile = File(tempDir, "test.m4a.bin")
            m4aFile.writeBytes(m4aBytes)
            assertEquals(AudioFileFormat.M4A, detectAudioFileFormat(m4aFile))

            // 5. Ogg Opus
            val oggPayload = "OpusHead".toByteArray() + byteArrayOf(1, 2) + ByteArray(10)
            val oggBytes = ByteArrayOutputStream().apply {
                write("OggS".toByteArray())
                write(0) // version
                write(ByteArray(21))
                write(1) // segment count
                write(oggPayload.size)
                write(oggPayload)
            }.toByteArray()
            val oggFile = File(tempDir, "test.ogg.bin")
            oggFile.writeBytes(oggBytes)
            assertEquals(AudioFileFormat.OPUS, detectAudioFileFormat(oggFile))

            // 6. HTML error page masquerading as audio
            val htmlFile = File(tempDir, "error.flac")
            htmlFile.writeText("<html><body>403 Forbidden</body></html>")
            assertNull(detectAudioFileFormat(htmlFile))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun audioTagWriter_formatsLrcAndParsesPngHeader() {
        val parsed = ParsedLyrics(
            lines = listOf(
                LyricLine(
                    words = listOf(LyricWord("Second ", 20_000, 21_000), LyricWord("line", 21_000, 22_000)),
                    translatedLyric = "第二行",
                    startTime = 20_000,
                    endTime = 22_000
                ),
                LyricLine(
                    words = listOf(LyricWord("First line", 1_500, 3_000)),
                    translatedLyric = "First line",
                    startTime = 1_500,
                    endTime = 3_000
                )
            ),
            hasWordTiming = true,
            source = LyricSource.YRC
        )
        val lrc = AudioTagWriter.formatLyricsToLrc(parsed)
        assertEquals(
            "[00:01.500]First line\n[00:20.000]Second line\n[00:20.000]第二行",
            lrc
        )

        val pngBytes = ByteArrayOutputStream().apply {
            write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
            write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(13).array())
            write("IHDR".toByteArray())
            write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(500).array())
            write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(500).array())
            write(8) // bit depth
            write(2) // RGB
            write(ByteArray(7))
        }.toByteArray()
        val info = AudioTagWriter.readCoverImageInfo(pngBytes)
        assertEquals("image/png", info?.mimeType)
        assertEquals(500, info?.width)
        assertEquals(500, info?.height)
        assertEquals(24, info?.bitsPerPixel)
        assertNull(AudioTagWriter.readCoverImageInfo("not-an-image".toByteArray()))
    }

    @Test
    fun searchRepositoryImpl_filtersTrialSourcesAndBuildsQualityLadder() {
        assertEquals(
            listOf("hires", "lossless", "exhigh", "higher", "standard"),
            SearchRepositoryImpl.downloadQualityCandidates("hires")
        )
        assertEquals(
            listOf("exhigh", "higher", "standard"),
            SearchRepositoryImpl.downloadQualityCandidates("EXHIGH")
        )

        val valid = SongUrlItemResponse(
            id = 123L,
            url = "https://example.com/song.flac",
            code = 200,
            freeTrialInfo = null
        )
        val validJsonNull = valid.copy(freeTrialInfo = JsonNull.INSTANCE)
        val trial = valid.copy(freeTrialInfo = JsonObject().apply { addProperty("start", 0) })
        val failedCode = valid.copy(code = 404)

        assertTrue(SearchRepositoryImpl.isFullAudioSource(valid, 123L))
        assertTrue(SearchRepositoryImpl.isFullAudioSource(validJsonNull, 123L))
        assertFalse(SearchRepositoryImpl.isFullAudioSource(trial, 123L))
        assertFalse(SearchRepositoryImpl.isFullAudioSource(failedCode, 123L))
    }

    @Test
    fun playerSettingsAndPathResolver_normalizeAndRouteDirectoriesProperly() {
        assertEquals(
            "/storage/emulated/0/Music/",
            PlayerSettingsManager.normalizeDirectoryPath("  /sdcard//Music  ")
        )
        assertEquals(
            "/storage/emulated/0/Download/netease/cloudmusic/Music/",
            PlayerSettingsManager.normalizeDirectoryPath("/storage/emulated/0/Download/netease/cloudmusic/Music")
        )
        assertEquals("", PlayerSettingsManager.normalizeDirectoryPath("   "))

        assertEquals(
            listOf(
                "/storage/emulated/0/Music/",
                "/storage/emulated/0/Download/netease/cloudmusic/Music/"
            ),
            PlayerSettingsManager.DEFAULT_SCAN_DIRECTORIES
        )

        assertEquals(
            DownloadPublishTarget.MediaStoreAudio("Music"),
            DownloadPathResolver.resolve("/storage/emulated/0/Music/")
        )
        assertEquals(
            DownloadPublishTarget.MediaStoreAudio("Music/Sub"),
            DownloadPathResolver.resolve("/storage/emulated/0/Music/Sub/")
        )
        assertEquals(
            DownloadPublishTarget.MediaStoreDownloads("Download/netease/cloudmusic/Music"),
            DownloadPathResolver.resolve("/storage/emulated/0/Download/netease/cloudmusic/Music/")
        )
        assertEquals(
            DownloadPublishTarget.MediaStoreDownloads("Download/netease/cloudmusic/Music"),
            DownloadPathResolver.resolve("/sdcard/Download/netease/cloudmusic/Music")
        )
        assertEquals(
            DownloadPublishTarget.DirectFile("/storage/emulated/0/CustomFolder"),
            DownloadPathResolver.resolve("/storage/emulated/0/CustomFolder/")
        )
    }
}
