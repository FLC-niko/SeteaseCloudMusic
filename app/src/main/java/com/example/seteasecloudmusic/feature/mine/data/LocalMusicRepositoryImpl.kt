package com.example.seteasecloudmusic.feature.mine.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import com.example.seteasecloudmusic.core.local.LocalMusicScanner
import com.example.seteasecloudmusic.core.local.LocalTrackMatcher
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.local.LocalMusicRepository
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

@Singleton
class LocalMusicRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : LocalMusicRepository {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("local_music_prefs", Context.MODE_PRIVATE)
    }

    private val _localTracksFlow = MutableStateFlow<List<Track>>(emptyList())
    override val localTracksFlow: StateFlow<List<Track>> = _localTracksFlow.asStateFlow()

    @Volatile
    private var hasScanned = false
    private val scanMutex = Mutex()
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    override suspend fun scanDirectory(directoryPath: String): List<Track> = scanMutex.withLock {
        withContext(Dispatchers.IO) {
            setCustomDirectoryPath(directoryPath)
            val combinedTracks = mutableListOf<Track>()

        // 1. 扫描系统 MediaStore
            val mediaStoreTracks = LocalMusicScanner.scanMediaStore(context)
            combinedTracks.addAll(mediaStoreTracks)

        // 2. 扫描指定目录
            val dir = File(directoryPath)
            if (dir.exists() && dir.isDirectory) {
                val dirTracks = LocalMusicScanner.scanDirectory(context, dir, fallbackToMediaStore = false)
                combinedTracks.addAll(dirTracks)
            }

            currentCoroutineContext().ensureActive()
            val deduplicated = deduplicateTracks(combinedTracks)
            _localTracksFlow.value = deduplicated
            hasScanned = true
            Log.d("LocalMusicRepo", "scanDirectory [$directoryPath] completed, total found: ${deduplicated.size}")
            deduplicated
        }
    }

    override suspend fun getLocalTracks(forceRefresh: Boolean): List<Track> = scanMutex.withLock {
        withContext(Dispatchers.IO) {
            if (!forceRefresh && hasScanned) {
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
            Log.d("LocalMusicRepo", "getLocalTracks completed, total found: ${deduplicated.size}")
            deduplicated
        }
    }

    private fun deduplicateTracks(tracks: List<Track>): List<Track> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<Track>()

        for (track in tracks) {
            val normTitle = LocalTrackMatcher.normalizeTitle(track.title)
            val normArtist = track.artists.joinToString("") { LocalTrackMatcher.normalizeToken(it.name) }
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

    companion object {
        private const val KEY_CUSTOM_DIR = "custom_music_dir_path"
    }
}
