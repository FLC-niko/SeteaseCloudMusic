package com.example.seteasecloudmusic.core.download

import android.util.Log
import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.id3.AbstractID3v2Tag
import org.jaudiotagger.tag.id3.ID3v24Tag
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.util.Locale
import java.util.logging.Level
import java.util.logging.Logger

internal data class CoverImageInfo(
    val mimeType: String,
    val width: Int,
    val height: Int,
    val bitsPerPixel: Int
)

/**
 * 音频元数据写入器：使用 jaudiotagger 为下载歌曲写入标题、歌手、专辑、内嵌封面与 LRC 歌词。
 */
internal object AudioTagWriter {

    private val whitespaceRegex = Regex("\\s+")

    init {
        runCatching {
            Logger.getLogger("org.jaudiotagger").level = Level.OFF
        }
    }

    /** 将 [ParsedLyrics] 格式化为标准逐行 LRC 文本（含可选翻译行），供内嵌到音频标签。 */
    fun formatLyricsToLrc(parsed: ParsedLyrics?): String? {
        if (parsed == null || parsed.lines.isEmpty()) return null
        val records = buildList {
            for (line in parsed.lines) {
                val main = line.words.joinToString("") { it.word }
                    .replace(whitespaceRegex, " ")
                    .trim()
                if (main.isNotBlank()) {
                    add(line.startTime.coerceAtLeast(0) to main)
                }
                val translation = line.translatedLyric
                    .replace(whitespaceRegex, " ")
                    .trim()
                if (translation.isNotBlank() && translation != main) {
                    add(line.startTime.coerceAtLeast(0) to translation)
                }
            }
        }.sortedBy { it.first }

        if (records.isEmpty()) return null
        return records.joinToString("\n") { (timeMs, text) ->
            "[${formatTimestamp(timeMs)}]$text"
        }
    }

    private fun formatTimestamp(timeMs: Int): String {
        val safe = timeMs.coerceAtLeast(0)
        return String.format(
            Locale.ROOT,
            "%02d:%02d.%03d",
            safe / 60_000,
            (safe % 60_000) / 1_000,
            safe % 1_000
        )
    }

    /** 直接解析 PNG / JPEG 头部尺寸信息，避免为写标签而全量解码 Bitmap。 */
    fun readCoverImageInfo(bytes: ByteArray): CoverImageInfo? {
        fun uint16(offset: Int): Int =
            ((bytes[offset].toInt() and 0xff) shl 8) or (bytes[offset + 1].toInt() and 0xff)

        fun uint32(offset: Int): Int = (uint16(offset) shl 16) or uint16(offset + 2)

        val pngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
        if (bytes.size >= 33 &&
            pngSignature.indices.all { bytes[it] == pngSignature[it] } &&
            uint32(8) == 13 &&
            bytes.copyOfRange(12, 16).contentEquals("IHDR".toByteArray(Charsets.US_ASCII))
        ) {
            val channels = when (bytes[25].toInt() and 0xff) {
                0, 3 -> 1
                2 -> 3
                4 -> 2
                6 -> 4
                else -> return null
            }
            val depth = bytes[24].toInt() and 0xff
            if (depth !in setOf(1, 2, 4, 8, 16)) return null
            return CoverImageInfo("image/png", uint32(16), uint32(20), depth * channels)
                .takeIf { it.width > 0 && it.height > 0 }
        }

        if (bytes.size < 4 || bytes[0] != 0xff.toByte() || bytes[1] != 0xd8.toByte()) return null
        var offset = 2
        while (offset + 3 < bytes.size) {
            if (bytes[offset] != 0xff.toByte()) return null
            val marker = bytes[offset + 1].toInt() and 0xff
            if (marker == 0xff) {
                offset++
                continue
            }
            if (marker == 0xd9 || marker == 0xda) return null
            if (marker == 0xd8 || marker == 0x01 || marker in 0xd0..0xd7) {
                offset += 2
                continue
            }
            val length = uint16(offset + 2)
            if (length < 2 || length > bytes.size - offset - 2) return null
            if (marker in setOf(0xc0, 0xc1, 0xc2, 0xc3, 0xc5, 0xc6, 0xc7, 0xc9, 0xca, 0xcb, 0xcd, 0xce, 0xcf)) {
                if (length < 8) return null
                val depth = bytes[offset + 4].toInt() and 0xff
                val components = bytes[offset + 9].toInt() and 0xff
                return CoverImageInfo(
                    mimeType = "image/jpeg",
                    width = uint16(offset + 7),
                    height = uint16(offset + 5),
                    bitsPerPixel = depth * components
                ).takeIf { it.width > 0 && it.height > 0 && it.bitsPerPixel > 0 }
            }
            offset += 2 + length
        }
        return null
    }

    fun writeTags(
        file: File,
        format: AudioFileFormat,
        title: String,
        artist: String,
        album: String,
        coverBytes: ByteArray?,
        lrcLyrics: String?
    ) {
        if (!format.supportsTagWriting || !file.isFile) return
        try {
            val audioFile = AudioFileIO.readAs(file, format.extension)
            var tag = audioFile.tagOrCreateAndSetDefault
            val isFlac = tag is FlacTag

            if (tag is AbstractID3v2Tag && tag !is ID3v24Tag) {
                val v24Tag = ID3v24Tag(tag)
                audioFile.tag = v24Tag
                tag = v24Tag
            }

            tag.setField(FieldKey.TITLE, title)
            tag.setField(FieldKey.ARTIST, artist)
            tag.setField(FieldKey.ALBUM, album)
            tag.setField(FieldKey.ALBUM_ARTIST, artist)

            if (!lrcLyrics.isNullOrBlank()) {
                runCatching { tag.setField(FieldKey.LYRICS, lrcLyrics) }
                    .onFailure { Log.w("AudioTagWriter", "Failed to write lyrics for $title", it) }
            }

            if (coverBytes != null) {
                runCatching {
                    val cover = readCoverImageInfo(coverBytes)
                        ?: error("Unsupported or malformed cover image")
                    tag.deleteArtworkField()
                    if (isFlac) {
                        tag.setField(
                            (tag as FlacTag).createArtworkField(
                                coverBytes,
                                3,
                                cover.mimeType,
                                "Cover",
                                cover.width,
                                cover.height,
                                cover.bitsPerPixel,
                                0
                            )
                        )
                    } else {
                        val artwork = ArtworkFactory.getNew()
                        artwork.mimeType = cover.mimeType
                        artwork.binaryData = coverBytes
                        artwork.pictureType = 3
                        artwork.description = "Cover"
                        tag.setField(artwork)
                    }
                }.onFailure {
                    Log.w("AudioTagWriter", "Failed to write cover for $title", it)
                }
            }

            audioFile.commit()
        } catch (e: Exception) {
            Log.w("AudioTagWriter", "Failed to write tags for ${file.name}", e)
        }
    }
}
