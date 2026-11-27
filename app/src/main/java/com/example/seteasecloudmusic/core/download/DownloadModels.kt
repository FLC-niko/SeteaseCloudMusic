package com.example.seteasecloudmusic.core.download

import com.example.seteasecloudmusic.core.settings.PlayerSettingsManager

/** 后台下载任务状态。 */
enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

/** 单首歌曲下载任务持久化快照。 */
data class DownloadTask(
    val trackId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String? = null,
    val durationMs: Long = 0L,
    val playlistName: String = "默认下载",
    val downloadDirectory: String = PlayerSettingsManager.DEFAULT_DOWNLOAD_DIRECTORY,
    val requestedQuality: String = "hires",
    val actualQuality: String? = null,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Int = 0,
    val fileName: String? = null,
    val fileFormat: String? = null,
    val mediaStoreUri: String? = null,
    val relativePath: String? = null,
    val fileSizeBytes: Long = 0L,
    val errorMessage: String? = null,
    val requestId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/** 单曲入队结果。 */
enum class DownloadEnqueueResult {
    ENQUEUED,
    ALREADY_DOWNLOADED,
    ALREADY_IN_QUEUE,
    ALREADY_LOCAL
}

/** 批量入队结果统计。 */
data class DownloadBatchEnqueueResult(
    val enqueuedCount: Int,
    val alreadyDownloadedCount: Int,
    val alreadyInQueueCount: Int,
    val localSkippedCount: Int
)

/** 下载任务完成或失败事件，用于向界面发送一次性提示。 */
sealed interface DownloadEvent {
    data class Completed(
        val trackId: Long,
        val title: String,
        val relativePath: String
    ) : DownloadEvent

    data class Failed(
        val trackId: Long,
        val title: String,
        val reason: String
    ) : DownloadEvent
}
