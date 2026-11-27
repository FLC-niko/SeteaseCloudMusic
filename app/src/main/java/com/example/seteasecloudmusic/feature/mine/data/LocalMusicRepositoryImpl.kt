package com.example.seteasecloudmusic.feature.mine.data

import android.content.Context
import android.util.Log
import com.example.seteasecloudmusic.core.local.LocalMusicScanner
import com.example.seteasecloudmusic.core.local.LocalTrackMatcher
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.local.LocalMusicRepository
import com.example.seteasecloudmusic.core.settings.PlayerSettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** 本地音乐仓库实现：负责扫描系统 MediaStore 与多扫描目录、曲目去重及在线曲目匹配。 */
@Singleton
class LocalMusicRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val playerSettingsManager: PlayerSettingsManager
) : LocalMusicRepository {

    private val _localTracksFlow = MutableStateFlow<List<Track>>(emptyList())
    override val localTracksFlow: StateFlow<List<Track>> = _localTracksFlow.asStateFlow()

    override val scanDirectoriesFlow: StateFlow<List<String>> = playerSettingsManager.scanDirectories

    @Volatile
    private var hasScanned = false
    private val scanMutex = Mutex()
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getScanDirectories(): List<String> = playerSettingsManager.scanDirectories.value

    override fun setScanDirectories(directories: List<String>) {
        playerSettingsManager.setScanDirectories(directories)
    }

    override fun getCustomDirectoryPath(): String? = getScanDirectories().joinToString(" / ")

    override fun setCustomDirectoryPath(path: String) {
        if (path.isBlank()) {
            playerSettingsManager.resetScanDirectoriesToDefault()
        } else {
            setScanDirectories(listOf(path))
        }
    }

    override suspend fun scanDirectory(directoryPath: String): List<Track> {
        setCustomDirectoryPath(directoryPath)
        return scanConfiguredDirectories()
    }

    override suspend fun scanConfiguredDirectories(): List<Track> = scanMutex.withLock {
        scanAllInternal()
    }

    override suspend fun getLocalTracks(forceRefresh: Boolean): List<Track> = scanMutex.withLock {
        if (!forceRefresh && hasScanned) {
            return@withLock _localTracksFlow.value
        }
        scanAllInternal()
    }

    private suspend fun scanAllInternal(): List<Track> = withContext(Dispatchers.IO) {
        val combinedTracks = mutableListOf<Track>()

        // 1. 扫描系统 MediaStore
        val mediaStoreTracks = LocalMusicScanner.scanMediaStore(context)
        combinedTracks.addAll(mediaStoreTracks)

        // 2. 扫描配置的全部本地目录与当前下载目录
        val targetDirs = (playerSettingsManager.scanDirectories.value + playerSettingsManager.downloadDirectoryPath.value)
            .map { File(it.trimEnd('/')) }
            .distinctBy { it.absolutePath }

        for (dir in targetDirs) {
            currentCoroutineContext().ensureActive()
            if (dir.exists() && dir.isDirectory) {
                val dirTracks = LocalMusicScanner.scanDirectory(context, dir, fallbackToMediaStore = false)
                combinedTracks.addAll(dirTracks)
            }
        }

        currentCoroutineContext().ensureActive()
        val deduplicated = deduplicateTracks(combinedTracks)
        _localTracksFlow.value = deduplicated
        hasScanned = true
        Log.d(
            "LocalMusicRepo",
            "scanAllInternal completed across ${targetDirs.size} dirs, total found: ${deduplicated.size}"
        )
        deduplicated
    }

    private fun deduplicateTracks(tracks: List<Track>): List<Track> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<Track>()

        for (track in tracks) {
            val normTitle = LocalTrackMatcher.normalizeTitle(track.title)
            val normArtist = track.artists.joinToString("") { LocalTrackMatcher.normalizeToken(it.name) }
            val key = if (normTitle.isNotBlank()) {
                // 时长按 3000ms（3 秒）取整分桶，容忍同一曲目不同编码间的微小偏差
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

    override suspend fun findMatchingLocalTrack(onlineTrack: Track): Track? = withContext(Dispatchers.Default) {
        if (!hasScanned && !scanMutex.isLocked) {
            backgroundScope.launch {
                runCatching { getLocalTracks(forceRefresh = false) }
            }
        }
        val localList = _localTracksFlow.value

        if (localList.isEmpty()) {
            Log.d("LocalMusicMatch", "findMatchingLocalTrack: local library is empty, skipping matching for '${onlineTrack.title}'")
            return@withContext null
        }

        val matched = LocalTrackMatcher.findMatchingLocalTrack(onlineTrack, localList)
        if (matched != null) {
            Log.d("LocalMusicMatch", ">>> MATCH SUCCESS! Online '${onlineTrack.title}' matched local '${matched.title}' (Path: ${matched.playableUrl})")
        } else {
            Log.d("LocalMusicMatch", ">>> Match failed for '${onlineTrack.title}'")
        }

        matched
    }

    override fun removeTracks(trackIds: Set<Long>) {
        val updated = _localTracksFlow.value.filterNot { it.id in trackIds }
        _localTracksFlow.value = updated
    }

    override fun updateTrackOrder(tracks: List<Track>) {
        _localTracksFlow.value = tracks
    }
}
