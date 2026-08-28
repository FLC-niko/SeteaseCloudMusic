package com.example.seteasecloudmusic.feature.mine.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import com.example.seteasecloudmusic.core.local.LocalMusicScanner
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.mine.domain.repository.LocalMusicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class LocalMusicRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : LocalMusicRepository {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("local_music_prefs", Context.MODE_PRIVATE)
    }

    private val _localTracksFlow = MutableStateFlow<List<Track>>(emptyList())
    override val localTracksFlow: StateFlow<List<Track>> = _localTracksFlow.asStateFlow()

    private var hasScanned = false

    override fun getCustomDirectoryPath(): String? {
        val saved = prefs.getString(KEY_CUSTOM_DIR, null)
        if (!saved.isNullOrBlank()) {
            return saved
        }
        val defaultMusicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        return if (defaultMusicDir.exists()) defaultMusicDir.absolutePath else "/storage/emulated/0/Music"
    }

    override fun setCustomDirectoryPath(path: String) {
        prefs.edit().putString(KEY_CUSTOM_DIR, path).apply()
    }

    override suspend fun scanDirectory(directoryPath: String): List<Track> = withContext(Dispatchers.IO) {
        setCustomDirectoryPath(directoryPath)
        val combinedTracks = mutableListOf<Track>()

        // 1. 扫描系统 MediaStore
        val mediaStoreTracks = LocalMusicScanner.scanMediaStore(context)
        combinedTracks.addAll(mediaStoreTracks)

        // 2. 扫描指定目录
        val dir = File(directoryPath)
        if (dir.exists() && dir.isDirectory) {
            val dirTracks = LocalMusicScanner.scanDirectory(context, dir)
            combinedTracks.addAll(dirTracks)
        }

        // 3. 去重（按标题+歌手+时长或文件路径）
        val deduplicated = deduplicateTracks(combinedTracks)
        _localTracksFlow.value = deduplicated
        hasScanned = true
        deduplicated
    }

    override suspend fun getLocalTracks(forceRefresh: Boolean): List<Track> = withContext(Dispatchers.IO) {
        if (!forceRefresh && hasScanned && _localTracksFlow.value.isNotEmpty()) {
            return@withContext _localTracksFlow.value
        }

        val combinedTracks = mutableListOf<Track>()

        // 1. 优先扫描系统 MediaStore（快速提取系统索引的所有音频）
        val mediaStoreTracks = LocalMusicScanner.scanMediaStore(context)
        combinedTracks.addAll(mediaStoreTracks)

        // 2. 扫描常用音频目录
        val targetDirs = mutableListOf<File>()
        val customPath = getCustomDirectoryPath()
        if (!customPath.isNullOrBlank()) {
            targetDirs.add(File(customPath))
        }
        targetDirs.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC))
        targetDirs.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
        targetDirs.add(File("/storage/emulated/0/Music"))
        targetDirs.add(File("/storage/emulated/0/Download"))
        targetDirs.add(File("/storage/emulated/0/netease/cloudmusic/Music"))

        for (dir in targetDirs.distinctBy { it.absolutePath }) {
            if (dir.exists() && dir.isDirectory) {
                val dirTracks = LocalMusicScanner.scanDirectory(context, dir)
                combinedTracks.addAll(dirTracks)
            }
        }

        val deduplicated = deduplicateTracks(combinedTracks)
        _localTracksFlow.value = deduplicated
        hasScanned = true
        deduplicated
    }

    private fun deduplicateTracks(tracks: List<Track>): List<Track> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<Track>()

        for (track in tracks) {
            val normTitle = normalizeTitle(track.title)
            val normArtist = track.artists.joinToString("") { normalizeString(it.name) }
            val key = if (normTitle.isNotBlank()) {
                "$normTitle-$normArtist-${(track.durationMs ?: 0L) / 3000L}"
            } else {
                track.playableUrl ?: track.id.toString()
            }

            if (seen.add(key)) {
                result.add(track)
            }
        }

        return result.sortedBy { it.title.lowercase() }
    }

    /**
     * 智能匹配在线歌曲与本地音频：
     * 1. 歌名归一化（去除音轨编号、版本后缀如 (Live)、[FLAC] 等）
     * 2. 歌手归一化并模糊匹配
     * 3. 时长容差判断（8秒内误差）
     */
    override suspend fun findMatchingLocalTrack(onlineTrack: Track): Track? = withContext(Dispatchers.Default) {
        val localList = if (_localTracksFlow.value.isEmpty()) {
            getLocalTracks(forceRefresh = false)
        } else {
            _localTracksFlow.value
        }

        if (localList.isEmpty()) return@withContext null

        val targetTitleNorm = normalizeTitle(onlineTrack.title)
        val targetArtists = onlineTrack.artists.map { normalizeString(it.name) }.filter { it.isNotBlank() }
        val targetDuration = onlineTrack.durationMs ?: 0L

        // 1. 尝试全匹配：歌名 + 歌手 + 时长容差
        val fullMatch = localList.firstOrNull { local ->
            val localTitleNorm = normalizeTitle(local.title)
            val titleMatches = localTitleNorm.equals(targetTitleNorm, ignoreCase = true) ||
                    localTitleNorm.contains(targetTitleNorm, ignoreCase = true) ||
                    targetTitleNorm.contains(localTitleNorm, ignoreCase = true)

            if (!titleMatches) return@firstOrNull false

            val localArtistNorm = local.artists.joinToString(" ") { normalizeString(it.name) }
            val artistMatches = targetArtists.isEmpty() || targetArtists.any {
                localArtistNorm.contains(it, ignoreCase = true) || it.contains(localArtistNorm, ignoreCase = true)
            }

            val localDuration = local.durationMs ?: 0L
            val durationMatches = if (targetDuration > 0 && localDuration > 0) {
                abs(targetDuration - localDuration) <= 8000L
            } else {
                true
            }

            artistMatches && durationMatches
        }

        if (fullMatch != null) return@withContext fullMatch

        // 2. 次级匹配：精确歌名 + 歌手匹配（忽略时长）
        val titleArtistMatch = localList.firstOrNull { local ->
            val localTitleNorm = normalizeTitle(local.title)
            if (!localTitleNorm.equals(targetTitleNorm, ignoreCase = true)) return@firstOrNull false

            val localArtistNorm = local.artists.joinToString(" ") { normalizeString(it.name) }
            targetArtists.any {
                localArtistNorm.contains(it, ignoreCase = true) || it.contains(localArtistNorm, ignoreCase = true)
            }
        }

        titleArtistMatch
    }

    private fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("\\(.*\\)|\\[.*\\]|\\{.*\\}"), "")
            .replace(Regex("[-_~`!@#\$%^&*+=|:;\"'<>,.?/\\\\]"), "")
            .replace(Regex("\\s+"), "")
            .trim()
    }

    private fun normalizeString(text: String): String {
        return text.lowercase()
            .replace(Regex("[-_~`!@#\$%^&*+=|:;\"'<>,.?/\\\\\\s]"), "")
            .trim()
    }

    companion object {
        private const val KEY_CUSTOM_DIR = "custom_music_dir_path"
    }
}
