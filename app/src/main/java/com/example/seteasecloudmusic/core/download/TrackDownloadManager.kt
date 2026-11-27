package com.example.seteasecloudmusic.core.download

import android.content.Context
import android.net.Uri
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.settings.PlayerSettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局歌曲下载调度器：负责单曲/批量入队、去重校验、暂停/重试/删除以及下载结果事件广播。
 */
@Singleton
class TrackDownloadManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val taskStore: DownloadTaskStore,
    private val playerSettingsManager: PlayerSettingsManager
) {
    val tasksFlow: StateFlow<List<DownloadTask>> = taskStore.tasksFlow

    private val _eventsFlow = MutableSharedFlow<DownloadEvent>(extraBufferCapacity = 16)
    val eventsFlow: SharedFlow<DownloadEvent> = _eventsFlow.asSharedFlow()

    internal fun notifyEvent(event: DownloadEvent) {
        _eventsFlow.tryEmit(event)
    }

    fun isLocalTrack(track: Track): Boolean {
        if (track.id <= 0L) return true
        if (track.audioQualityLevel == "local") return true
        val url = track.playableUrl ?: return false
        return url.startsWith("content:") || url.startsWith("file:") || url.startsWith("/")
    }

    fun isDownloadedOnDisk(task: DownloadTask): Boolean {
        if (task.status != DownloadStatus.COMPLETED) return false
        val uriString = task.mediaStoreUri?.takeIf { it.isNotBlank() } ?: return false
        return if (uriString.startsWith("content://")) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uriString))?.use { true } == true
            }.getOrDefault(false)
        } else {
            val filePath = if (uriString.startsWith("file://")) {
                Uri.parse(uriString).path
            } else {
                uriString
            }
            !filePath.isNullOrBlank() && File(filePath).isFile
        }
    }

    fun enqueue(
        track: Track,
        playlistName: String = DEFAULT_PLAYLIST_NAME
    ): DownloadEnqueueResult {
        if (isLocalTrack(track)) {
            return DownloadEnqueueResult.ALREADY_LOCAL
        }
        val existing = taskStore.getByTrackId(track.id)
        if (existing != null) {
            if (isDownloadedOnDisk(existing)) {
                return DownloadEnqueueResult.ALREADY_DOWNLOADED
            }
            if (existing.status in DownloadTaskStore.ACTIVE_STATES) {
                return DownloadEnqueueResult.ALREADY_IN_QUEUE
            }
        }

        val requestId = UUID.randomUUID()
        val task = buildTask(
            track = track,
            playlistName = playlistName,
            requestedQuality = playerSettingsManager.audioQuality.value.levelKey,
            requestId = requestId.toString(),
            createdAt = existing?.createdAt ?: System.currentTimeMillis()
        )
        taskStore.upsertAll(listOf(task))
        enqueueWork(track.id, playlistName, task.downloadDirectory, requestId)
        return DownloadEnqueueResult.ENQUEUED
    }

    fun enqueueBatch(
        tracks: List<Track>,
        playlistName: String = DEFAULT_PLAYLIST_NAME
    ): DownloadBatchEnqueueResult {
        val uniqueTracks = tracks.distinctBy { it.id }
        var alreadyDownloaded = 0
        var alreadyInQueue = 0
        var localSkipped = 0
        val toEnqueue = mutableListOf<Pair<DownloadTask, UUID>>()
        val preferredQuality = playerSettingsManager.audioQuality.value.levelKey

        for (track in uniqueTracks) {
            if (isLocalTrack(track)) {
                localSkipped++
                continue
            }
            val existing = taskStore.getByTrackId(track.id)
            if (existing != null) {
                if (isDownloadedOnDisk(existing)) {
                    alreadyDownloaded++
                    continue
                }
                if (existing.status in DownloadTaskStore.ACTIVE_STATES) {
                    alreadyInQueue++
                    continue
                }
            }
            val requestId = UUID.randomUUID()
            val task = buildTask(
                track = track,
                playlistName = playlistName,
                requestedQuality = preferredQuality,
                requestId = requestId.toString(),
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
            toEnqueue += task to requestId
        }

        if (toEnqueue.isNotEmpty()) {
            DownloadWorker.createNotificationChannel(context)
            taskStore.upsertAll(toEnqueue.map { it.first })
            toEnqueue.forEach { (task, requestId) ->
                enqueueWork(task.trackId, playlistName, task.downloadDirectory, requestId)
            }
        }

        return DownloadBatchEnqueueResult(
            enqueuedCount = toEnqueue.size,
            alreadyDownloadedCount = alreadyDownloaded,
            alreadyInQueueCount = alreadyInQueue,
            localSkippedCount = localSkipped
        )
    }

    fun pause(trackId: Long) {
        val task = taskStore.getByTrackId(trackId) ?: return
        taskStore.updateStatus(trackId, DownloadStatus.PAUSED)
        cancelWork(trackId, task.requestId)
    }

    fun resume(trackId: Long): Boolean {
        val task = taskStore.getByTrackId(trackId) ?: return false
        if (isDownloadedOnDisk(task)) return false
        val requestId = UUID.randomUUID()
        val downloadDir = playerSettingsManager.downloadDirectoryPath.value
        val refreshed = task.copy(
            status = DownloadStatus.PENDING,
            progress = 0,
            downloadDirectory = downloadDir,
            requestedQuality = playerSettingsManager.audioQuality.value.levelKey,
            errorMessage = null,
            requestId = requestId.toString(),
            updatedAt = System.currentTimeMillis()
        )
        taskStore.upsertAll(listOf(refreshed))
        enqueueWork(trackId, refreshed.playlistName, downloadDir, requestId)
        return true
    }

    fun deleteTask(trackId: Long, deleteFile: Boolean = false) {
        val task = taskStore.getByTrackId(trackId) ?: return
        taskStore.updateStatus(trackId, DownloadStatus.PAUSED)
        cancelWork(trackId, task.requestId)
        if (deleteFile) {
            task.mediaStoreUri?.takeIf { it.isNotBlank() }?.let { uriString ->
                runCatching {
                    if (uriString.startsWith("content://")) {
                        context.contentResolver.delete(Uri.parse(uriString), null, null)
                    } else {
                        val filePath = if (uriString.startsWith("file://")) Uri.parse(uriString).path else uriString
                        if (!filePath.isNullOrBlank()) File(filePath).delete()
                    }
                }
            }
        }
        taskStore.remove(trackId)
    }

    fun clearFinished() {
        taskStore.clearFinished()
    }

    private fun buildTask(
        track: Track,
        playlistName: String,
        requestedQuality: String,
        requestId: String,
        createdAt: Long
    ): DownloadTask {
        val artistText = track.artists.joinToString("/") { it.name }.ifBlank { "未知歌手" }
        val albumText = track.album.title.ifBlank { "未知专辑" }
        val cover = (track.coverUrl ?: track.album.coverUrl)?.takeIf { it.isNotBlank() }
        val downloadDir = playerSettingsManager.downloadDirectoryPath.value
        return DownloadTask(
            trackId = track.id,
            title = track.title.ifBlank { "未命名歌曲" },
            artist = artistText,
            album = albumText,
            coverUrl = cover,
            durationMs = track.durationMs ?: 0L,
            playlistName = playlistName.ifBlank { DEFAULT_PLAYLIST_NAME },
            downloadDirectory = downloadDir,
            requestedQuality = requestedQuality,
            status = DownloadStatus.PENDING,
            progress = 0,
            requestId = requestId,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun enqueueWork(
        trackId: Long,
        playlistName: String,
        downloadDirectory: String,
        requestId: UUID
    ) {
        DownloadWorker.createNotificationChannel(context)
        val uniqueWorkName = WORK_NAME_PREFIX + trackId
        val inputData = Data.Builder()
            .putLong(DownloadWorker.KEY_TRACK_ID, trackId)
            .putString(DownloadWorker.KEY_PLAYLIST_NAME, playlistName)
            .putString(DownloadWorker.KEY_DOWNLOAD_DIRECTORY, downloadDirectory)
            .build()
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setId(requestId)
            .addTag(WORK_TAG)
            .addTag(uniqueWorkName)
            .setInputData(inputData)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, request)
    }

    private fun cancelWork(trackId: Long, requestId: String) {
        val manager = WorkManager.getInstance(context)
        val uuid = runCatching { UUID.fromString(requestId) }.getOrNull()
        if (uuid != null) {
            manager.cancelWorkById(uuid)
        }
        manager.cancelUniqueWork(WORK_NAME_PREFIX + trackId)
    }

    companion object {
        private const val WORK_NAME_PREFIX = "download_track_"
        private const val WORK_TAG = "track_download"
        const val DEFAULT_PLAYLIST_NAME = "默认下载"
    }
}
