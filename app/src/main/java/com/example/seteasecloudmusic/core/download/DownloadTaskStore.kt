package com.example.seteasecloudmusic.core.download

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 基于本地 JSON 原子写入的下载任务持久化仓库，对外暴露按创建时间倒序排列的任务流。
 */
@Singleton
class DownloadTaskStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val lock = Any()
    private val storageFile = File(context.filesDir, FILE_NAME)
    private val tasksMap = LinkedHashMap<Long, DownloadTask>()
    private val _tasksFlow = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasksFlow: StateFlow<List<DownloadTask>> = _tasksFlow.asStateFlow()

    init {
        synchronized(lock) {
            loadFromDisk()
            publishLocked()
        }
    }

    fun getByTrackId(trackId: Long): DownloadTask? = synchronized(lock) {
        tasksMap[trackId]
    }

    fun upsertAll(tasks: List<DownloadTask>) {
        if (tasks.isEmpty()) return
        synchronized(lock) {
            tasks.forEach { task ->
                tasksMap[task.trackId] = task
            }
            persistAndPublishLocked()
        }
    }

    /**
     * 仅当当前任务仍归属 [requestId] 且处于活跃下载态时更新进度或失败状态，防止旧 Worker 覆盖已暂停/重建的新任务。
     */
    fun updateOwnedProgress(
        trackId: Long,
        requestId: String,
        status: DownloadStatus,
        progress: Int,
        actualQuality: String? = null,
        errorMessage: String? = null
    ): Boolean = synchronized(lock) {
        val current = tasksMap[trackId] ?: return false
        if (current.requestId != requestId || current.status !in ACTIVE_STATES) {
            return false
        }
        tasksMap[trackId] = current.copy(
            status = status,
            progress = progress.coerceIn(0, 100),
            actualQuality = actualQuality ?: current.actualQuality,
            errorMessage = errorMessage,
            updatedAt = System.currentTimeMillis()
        )
        persistAndPublishLocked()
        true
    }

    fun markCompleted(
        trackId: Long,
        requestId: String,
        fileName: String,
        fileFormat: String,
        actualQuality: String?,
        mediaStoreUri: String,
        relativePath: String,
        fileSizeBytes: Long
    ): Boolean = synchronized(lock) {
        val current = tasksMap[trackId] ?: return false
        if (current.requestId != requestId || current.status !in ACTIVE_STATES) {
            return false
        }
        tasksMap[trackId] = current.copy(
            status = DownloadStatus.COMPLETED,
            progress = 100,
            fileName = fileName,
            fileFormat = fileFormat,
            actualQuality = actualQuality ?: current.actualQuality,
            mediaStoreUri = mediaStoreUri,
            relativePath = relativePath,
            fileSizeBytes = fileSizeBytes,
            errorMessage = null,
            updatedAt = System.currentTimeMillis()
        )
        persistAndPublishLocked()
        true
    }

    fun updateStatus(trackId: Long, status: DownloadStatus, errorMessage: String? = null) {
        synchronized(lock) {
            val current = tasksMap[trackId] ?: return
            tasksMap[trackId] = current.copy(
                status = status,
                errorMessage = errorMessage,
                updatedAt = System.currentTimeMillis()
            )
            persistAndPublishLocked()
        }
    }

    fun remove(trackId: Long) {
        synchronized(lock) {
            if (tasksMap.remove(trackId) != null) {
                persistAndPublishLocked()
            }
        }
    }

    fun clearFinished() {
        synchronized(lock) {
            val iterator = tasksMap.entries.iterator()
            var removed = false
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value.status == DownloadStatus.COMPLETED || entry.value.status == DownloadStatus.FAILED) {
                    iterator.remove()
                    removed = true
                }
            }
            if (removed) {
                persistAndPublishLocked()
            }
        }
    }

    private fun loadFromDisk() {
        if (!storageFile.isFile) return
        runCatching {
            val json = storageFile.readText(Charsets.UTF_8)
            val listType = object : TypeToken<List<DownloadTask>>() {}.type
            val loaded: List<DownloadTask>? = gson.fromJson(json, listType)
            loaded?.forEach { task ->
                tasksMap[task.trackId] = task
            }
        }
    }

    private fun persistAndPublishLocked() {
        val snapshot = tasksMap.values.sortedByDescending { it.createdAt }
        _tasksFlow.value = snapshot
        runCatching {
            val tempFile = File(storageFile.parentFile, "$FILE_NAME.tmp")
            tempFile.writeText(gson.toJson(snapshot), Charsets.UTF_8)
            if (!tempFile.renameTo(storageFile)) {
                tempFile.copyTo(storageFile, overwrite = true)
                tempFile.delete()
            }
        }
    }

    private fun publishLocked() {
        _tasksFlow.value = tasksMap.values.sortedByDescending { it.createdAt }
    }

    companion object {
        private const val FILE_NAME = "download_tasks.json"
        val ACTIVE_STATES = setOf(DownloadStatus.PENDING, DownloadStatus.DOWNLOADING)
    }
}
