package com.example.seteasecloudmusic.core.download

import java.util.Locale

/**
 * 规范化下载目录名与文件名，过滤非法路径字符与控制字符并按 UTF-8 字节长度截断。
 */
object DownloadFileNames {
    private val illegalCharsRegex = Regex("""[\\/:*?"<>|]""")
    private val reservedNames = setOf("CON", "PRN", "AUX", "NUL") +
        (1..9).flatMap { listOf("COM$it", "LPT$it") }

    fun playlistName(raw: String): String = safeSegment(raw, fallback = "默认下载", maxBytes = 120)

    fun songBaseName(title: String, artist: String): String {
        val safeTitle = safeSegment(title, fallback = "未命名歌曲", maxBytes = 100)
        val safeArtist = safeSegment(artist, fallback = "未知歌手", maxBytes = 70)
        return safeSegment("$safeTitle - $safeArtist", fallback = "未命名歌曲", maxBytes = 180)
    }

    private fun safeSegment(raw: String, fallback: String, maxBytes: Int): String {
        val replaced = raw.replace(illegalCharsRegex, "_")
        val cleaned = buildString(replaced.length) {
            var index = 0
            while (index < replaced.length) {
                val point = replaced.codePointAt(index)
                if (!Character.isISOControl(point) && point !in 0xd800..0xdfff) {
                    appendCodePoint(point)
                }
                index += Character.charCount(point)
            }
        }.trim().trimEnd('.', ' ')
        val normalized = cleaned.takeUnless { it.isBlank() || it == "." || it == ".." } ?: fallback
        val limited = truncateUtf8(normalized, maxBytes).trimEnd('.', ' ').ifBlank { fallback }
        return if (limited.substringBefore('.').uppercase(Locale.ROOT) in reservedNames) {
            "_${truncateUtf8(limited, (maxBytes - 1).coerceAtLeast(1)).trimEnd('.', ' ')}"
        } else {
            limited
        }
    }

    private fun truncateUtf8(value: String, maxBytes: Int): String = buildString {
        var bytes = 0
        var index = 0
        while (index < value.length) {
            val point = value.codePointAt(index)
            val text = String(Character.toChars(point))
            val size = text.toByteArray(Charsets.UTF_8).size
            if (bytes + size > maxBytes) break
            append(text)
            bytes += size
            index += Character.charCount(point)
        }
    }
}
