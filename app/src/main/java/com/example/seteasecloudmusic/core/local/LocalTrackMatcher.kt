package com.example.seteasecloudmusic.core.local

import com.example.seteasecloudmusic.core.model.Track
import java.io.File
import java.util.Locale
import kotlin.math.abs

/**
 * 在线曲目与本地扫描音频文件的匹配器。
 */
internal object LocalTrackMatcher {
    private val LEADING_TRACK_INDEX_REGEX = Regex("^\\d+[.\\-_\\s]+")                                      // 曲目前导序号（如 "01 - "）
    private val NON_GREEDY_BRACKETS_REGEX = Regex("\\([^)]*\\)|\\[[^\\]]*\\]|\\{[^}]*\\}|【[^】]*】|（[^）]*）") // 标题中常见括号附注（如 Live/Feat 版本说明）
    private val PUNCTUATION_REGEX = Regex("[-_~`!@#\$%^&*+=|:;\"'<>,.?/\\\\()\\[\\]{}【】（）\\s]+")           // 用于文本归一化的标点与空白分隔符
    private val UNKNOWN_ARTIST_TOKENS = setOf("unknown", "未知", "未知歌手", "variousartists", "群星", "va")    // 无效或泛化歌手名集合，不作为歌手确认依据

    private const val STRICT_DURATION_TOLERANCE_MS = 5_000L   // 严格时长误差容限（ms）：本地歌手未知时用于防止同名异曲误匹配
    private const val RELAXED_DURATION_TOLERANCE_MS = 15_000L // 宽松时长误差容限（ms）：允许版本剪辑或前奏空白造成的轻微差异

    /**
     * 为在线曲目 [onlineTrack] 查找可播放的本地匹配曲目。
     *
     * 过滤空白归一化标题、未知歌手冲突与过大时长偏差，避免误命中。
     */
    fun findMatchingLocalTrack(onlineTrack: Track, localTracks: List<Track>): Track? {
        if (localTracks.isEmpty()) return null

        val targetTitleNorm = normalizeTitle(onlineTrack.title)
        if (targetTitleNorm.isBlank()) return null

        val targetArtists = extractKnownArtists(onlineTrack)
        val targetDurationMs = onlineTrack.durationMs ?: 0L

        // 第一层匹配：归一化歌名或文件名完全一致，并校对已知歌手与时长
        localTracks.firstOrNull { local ->
            if (local.playableUrl.isNullOrBlank()) return@firstOrNull false
            val localTitleNorm = normalizeTitle(local.title)
            val rawFilename = extractFilename(local.playableUrl)
            val filenameNorm = normalizeTitle(rawFilename)

            val exactTitleMatch = (localTitleNorm.isNotBlank() && localTitleNorm == targetTitleNorm) ||
                (filenameNorm.isNotBlank() && filenameNorm == targetTitleNorm)
            if (!exactTitleMatch) return@firstOrNull false

            val localDurationMs = local.durationMs ?: 0L
            if (!isDurationCompatible(targetDurationMs, localDurationMs, RELAXED_DURATION_TOLERANCE_MS)) {
                return@firstOrNull false
            }

            val localArtists = extractKnownArtists(local)
            val rawFilenameNorm = normalizeToken(rawFilename)
            if (localArtists.isNotEmpty() && targetArtists.isNotEmpty()) {
                return@firstOrNull artistsOverlap(localArtists, targetArtists) ||
                    filenameMentionsArtist(rawFilenameNorm, targetArtists)
            }
            if (localArtists.isEmpty() && targetArtists.isNotEmpty()) {
                // 本地缺少有效歌手标签时，优先通过文件名或更严格的时长容限确认
                if (filenameMentionsArtist(rawFilenameNorm, targetArtists)) return@firstOrNull true
                return@firstOrNull targetDurationMs > 0L &&
                    localDurationMs > 0L &&
                    abs(targetDurationMs - localDurationMs) <= STRICT_DURATION_TOLERANCE_MS
            }
            true
        }?.let { return it }

        // 第二层匹配：文件名同时包含归一化歌手名与歌名（例如 "Artist - Song.flac"）
        if (targetTitleNorm.length < 2 || targetArtists.isEmpty()) return null
        return localTracks.firstOrNull { local ->
            if (local.playableUrl.isNullOrBlank()) return@firstOrNull false
            val rawFilename = extractFilename(local.playableUrl)
            if (rawFilename.isBlank()) return@firstOrNull false
            val filenameNorm = normalizeTitle(rawFilename)
            val rawFilenameNorm = normalizeToken(rawFilename)

            val titleInFilename = (filenameNorm.isNotBlank() &&
                (filenameNorm.endsWith(targetTitleNorm) || filenameNorm.startsWith(targetTitleNorm))) ||
                (targetTitleNorm.length >= 3 && rawFilenameNorm.contains(targetTitleNorm))
            if (!titleInFilename) return@firstOrNull false

            val localArtists = extractKnownArtists(local)
            val artistConfirmed = artistsOverlap(localArtists, targetArtists) ||
                filenameMentionsArtist(rawFilenameNorm, targetArtists)
            if (!artistConfirmed) return@firstOrNull false

            isDurationCompatible(targetDurationMs, local.durationMs ?: 0L, RELAXED_DURATION_TOLERANCE_MS)
        }
    }

    fun normalizeTitle(title: String): String {
        val lower = title.lowercase(Locale.ROOT)
            .replace(LEADING_TRACK_INDEX_REGEX, "")
        val withoutBrackets = lower.replace(NON_GREEDY_BRACKETS_REGEX, "")
        val stripped = normalizeToken(withoutBrackets)
        return stripped.ifBlank { normalizeToken(lower) }
    }

    fun normalizeToken(text: String): String {
        return text.lowercase(Locale.ROOT)
            .replace(PUNCTUATION_REGEX, "")
            .trim()
    }

    private fun extractFilename(url: String?): String {
        if (url.isNullOrBlank()) return ""
        return runCatching {
            when {
                url.startsWith("/") -> File(url).nameWithoutExtension
                url.startsWith("file://") -> File(url.removePrefix("file://")).nameWithoutExtension
                else -> ""
            }
        }.getOrDefault("")
    }

    private fun extractKnownArtists(track: Track): List<String> {
        return track.artists
            .map { normalizeToken(it.name) }
            .filter { it.isNotBlank() && it !in UNKNOWN_ARTIST_TOKENS && !it.contains("unknown") && !it.contains("未知") }
    }

    private fun artistsOverlap(localArtists: List<String>, targetArtists: List<String>): Boolean {
        if (localArtists.isEmpty() || targetArtists.isEmpty()) return false
        return localArtists.any { localArtist ->
            targetArtists.any { targetArtist ->
                localArtist.contains(targetArtist) || targetArtist.contains(localArtist)
            }
        }
    }

    private fun filenameMentionsArtist(rawFilenameNorm: String, targetArtists: List<String>): Boolean {
        if (rawFilenameNorm.isBlank()) return false
        return targetArtists.any { artist -> artist.length >= 2 && rawFilenameNorm.contains(artist) }
    }

    private fun isDurationCompatible(firstMs: Long, secondMs: Long, maxDeltaMs: Long): Boolean {
        if (firstMs <= 0L || secondMs <= 0L) return true
        return abs(firstMs - secondMs) <= maxDeltaMs
    }
}
