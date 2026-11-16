package com.example.seteasecloudmusic.core.local

import com.example.seteasecloudmusic.core.model.Track
import java.io.File
import java.util.Locale
import kotlin.math.abs

/**
 * Pure matching logic between online catalog tracks and scanned local audio files.
 */
internal object LocalTrackMatcher {
    private val LEADING_TRACK_INDEX_REGEX = Regex("^\\d+[.\\-_\\s]+")
    private val NON_GREEDY_BRACKETS_REGEX = Regex("\\([^)]*\\)|\\[[^\\]]*\\]|\\{[^}]*\\}|【[^】]*】|（[^）]*）")
    private val PUNCTUATION_REGEX = Regex("[-_~`!@#\$%^&*+=|:;\"'<>,.?/\\\\()\\[\\]{}【】（）\\s]+")
    private val UNKNOWN_ARTIST_TOKENS = setOf("unknown", "未知", "未知歌手", "variousartists", "群星", "va")

    private const val STRICT_DURATION_TOLERANCE_MS = 5_000L
    private const val RELAXED_DURATION_TOLERANCE_MS = 15_000L

    /**
     * Finds a matching playable local audio track for [onlineTrack], guarding against empty
     * normalized strings, unknown-artist collisions, and large duration mismatches.
     */
    fun findMatchingLocalTrack(onlineTrack: Track, localTracks: List<Track>): Track? {
        if (localTracks.isEmpty()) return null

        val targetTitleNorm = normalizeTitle(onlineTrack.title)
        if (targetTitleNorm.isBlank()) return null

        val targetArtists = extractKnownArtists(onlineTrack)
        val targetDurationMs = onlineTrack.durationMs ?: 0L

        // Tier 1: exact normalized title (or exact normalized filename), checking known artist + duration
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
                // Unknown local artist: avoid matching unrelated songs sharing the same title
                if (filenameMentionsArtist(rawFilenameNorm, targetArtists)) return@firstOrNull true
                return@firstOrNull targetDurationMs > 0L &&
                    localDurationMs > 0L &&
                    abs(targetDurationMs - localDurationMs) <= STRICT_DURATION_TOLERANCE_MS
            }
            true
        }?.let { return it }

        // Tier 2: filename contains both normalized artist and title (e.g. "Artist - Song.flac")
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
