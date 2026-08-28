package com.example.seteasecloudmusic.feature.mine.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
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

        val deduplicated = deduplicateTracks(combinedTracks)
        _localTracksFlow.value = deduplicated
        hasScanned = true
        Log.d("LocalMusicRepo", "scanDirectory [$directoryPath] completed, total found: ${deduplicated.size}")
        deduplicated
    }

    override suspend fun getLocalTracks(forceRefresh: Boolean): List<Track> = withContext(Dispatchers.IO) {
        if (!forceRefresh && _localTracksFlow.value.isNotEmpty()) {
            return@withContext _localTracksFlow.value
        }

        val combinedTracks = mutableListOf<Track>()

        // 1. 优先扫描系统 MediaStore
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
        Log.d("LocalMusicRepo", "getLocalTracks completed, total found: ${deduplicated.size}")
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
     * 智能多维度匹配在线歌曲与本地音频库：
     * 1. 标题纯净度清洗（去除序号、Live、伴奏、无损标记、副标题、全半角括号）
     * 2. 支持文件名倒推匹配（如 "歌手 - 歌名.mp3"）
     * 3. 歌手模糊匹配（支持未知歌手容错）
     * 4. 时长容差扩展至 15 秒（适应不同格式的前后静音差异）
     */
    override suspend fun findMatchingLocalTrack(onlineTrack: Track): Track? = withContext(Dispatchers.Default) {
        val localList = if (_localTracksFlow.value.isEmpty()) {
            getLocalTracks(forceRefresh = false)
        } else {
            _localTracksFlow.value
        }

        if (localList.isEmpty()) {
            Log.d("LocalMusicMatch", "findMatchingLocalTrack: local library is empty, skipping matching for '${onlineTrack.title}'")
            return@withContext null
        }

        val targetTitleNorm = normalizeTitle(onlineTrack.title)
        val targetArtists = onlineTrack.artists.map { normalizeString(it.name) }.filter { it.isNotBlank() }
        val targetDuration = onlineTrack.durationMs ?: 0L

        Log.d("LocalMusicMatch", "Matching online track: title='${onlineTrack.title}' (norm='$targetTitleNorm'), artists=$targetArtists, dur=$targetDuration, localPoolSize=${localList.size}")

        // 1. 最高精度：规范化标题全等 + (歌手匹配 或 歌手未知) + 时长容差(15s)
        var matched = localList.firstOrNull { local ->
            val localTitleNorm = normalizeTitle(local.title)
            val filename = getFilename(local.playableUrl)
            val filenameNorm = normalizeTitle(filename)

            val titleMatches = localTitleNorm.equals(targetTitleNorm, ignoreCase = true) ||
                    filenameNorm.equals(targetTitleNorm, ignoreCase = true) ||
                    (targetTitleNorm.length >= 2 && (filenameNorm.endsWith(targetTitleNorm) || filenameNorm.startsWith(targetTitleNorm)))

            if (!titleMatches) return@firstOrNull false

            val localArtistNorm = local.artists.joinToString(" ") { normalizeString(it.name) }
            val localIsUnknownArtist = localArtistNorm.isBlank() || localArtistNorm.contains("未知") || localArtistNorm.contains("unknown")
            val artistMatches = localIsUnknownArtist || targetArtists.isEmpty() || targetArtists.any {
                localArtistNorm.contains(it, ignoreCase = true) ||
                        it.contains(localArtistNorm, ignoreCase = true) ||
                        filename.contains(it, ignoreCase = true)
            }

            val localDuration = local.durationMs ?: 0L
            val durationMatches = if (targetDuration > 0 && localDuration > 0) {
                abs(targetDuration - localDuration) <= 15000L
            } else {
                true
            }

            titleMatches && artistMatches && durationMatches
        }

        // 2. 次级匹配：规范化标题全等 + 歌手匹配（忽略时长）
        if (matched == null) {
            matched = localList.firstOrNull { local ->
                val localTitleNorm = normalizeTitle(local.title)
                val filename = getFilename(local.playableUrl)
                val filenameNorm = normalizeTitle(filename)

                val titleMatches = localTitleNorm.equals(targetTitleNorm, ignoreCase = true) ||
                        filenameNorm.equals(targetTitleNorm, ignoreCase = true)

                if (!titleMatches) return@firstOrNull false

                val localArtistNorm = local.artists.joinToString(" ") { normalizeString(it.name) }
                val localIsUnknownArtist = localArtistNorm.isBlank() || localArtistNorm.contains("未知") || localArtistNorm.contains("unknown")
                val artistMatches = localIsUnknownArtist || targetArtists.isEmpty() || targetArtists.any {
                    localArtistNorm.contains(it, ignoreCase = true) ||
                            it.contains(localArtistNorm, ignoreCase = true) ||
                            filename.contains(it, ignoreCase = true)
                }

                artistMatches
            }
        }

        // 3. 包含匹配：文件名包含完整歌名与歌手
        if (matched == null && targetTitleNorm.length >= 2) {
            matched = localList.firstOrNull { local ->
                val filename = getFilename(local.playableUrl).lowercase()
                val hasTitle = filename.contains(targetTitleNorm)
                val hasArtist = targetArtists.isEmpty() || targetArtists.any { filename.contains(it) }
                hasTitle && hasArtist
            }
        }

        if (matched != null) {
            Log.d("LocalMusicMatch", ">>> MATCH SUCCESS! Online '${onlineTrack.title}' matched local '${matched.title}' (Path: ${matched.playableUrl})")
        } else {
            Log.d("LocalMusicMatch", ">>> Match failed for '${onlineTrack.title}'")
        }

        matched
    }

    private fun getFilename(url: String?): String {
        if (url.isNullOrBlank()) return ""
        return try {
            if (url.startsWith("/") || url.startsWith("file://")) {
                File(url.removePrefix("file://")).nameWithoutExtension
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("^\\d+[.\\-_\\s]+"), "") // 去除开头的音轨序号 "01. ", "01 - "
            .replace(Regex("\\(.*\\)|\\[.*\\]|\\{.*\\}|【.*】|（.*）"), "") // 去除各种括号内容 (Live), [FLAC] 等
            .replace(Regex("[-_~`!@#\$%^&*+=|:;\"'<>,.?/\\\\]"), "") // 去除标点符号
            .replace(Regex("\\s+"), "") // 去除空格
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
