package com.example.seteasecloudmusic.core.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.seteasecloudmusic.R
import com.example.seteasecloudmusic.core.common.toUserFriendlyMessage
import com.example.seteasecloudmusic.core.local.LocalMusicRepository
import com.example.seteasecloudmusic.core.settings.PlayerSettingsManager
import com.example.seteasecloudmusic.feature.main.MainActivity
import com.example.seteasecloudmusic.feature.player.domain.repository.ParsedLyricsRepository
import com.example.seteasecloudmusic.feature.search.domain.SearchRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface DownloadWorkerEntryPoint {
    fun downloadTaskStore(): DownloadTaskStore
    fun searchRepository(): SearchRepository
    fun parsedLyricsRepository(): ParsedLyricsRepository
    fun localMusicRepository(): LocalMusicRepository
    fun downloadManager(): TrackDownloadManager
    fun playerSettingsManager(): PlayerSettingsManager
}

/**
 * 后台歌曲下载 Worker：
 * 限制最多 3 路并发下载，实时解析完整非试听音源、流式下载并按魔数校验容器格式、
 * 写入标题/艺人/专辑/封面/LRC 歌词标签，最后通过 MediaStore 原子发布并刷新本地曲库。
 */
class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val entryPoint: DownloadWorkerEntryPoint by lazy {
        EntryPointAccessors.fromApplication(applicationContext, DownloadWorkerEntryPoint::class.java)
    }

    override suspend fun doWork(): Result {
        val trackId = inputData.getLong(KEY_TRACK_ID, -1L)
        if (trackId <= 0L) return Result.failure()

        val taskStore = entryPoint.downloadTaskStore()
        val downloadManager = entryPoint.downloadManager()
        val requestId = id.toString()

        val initialTask = taskStore.getByTrackId(trackId) ?: return Result.success()
        if (initialTask.requestId != requestId || initialTask.status !in DownloadTaskStore.ACTIVE_STATES) {
            return Result.success()
        }

        createNotificationChannel(applicationContext)
        notifications.start(requestId, 1, ::publishNotification)
        return try {
            downloadSlots.withPermit {
                if (isStopped) return@withPermit Result.failure()
                processTrack(
                    trackId = trackId,
                    requestId = requestId,
                    playlistName = inputData.getString(KEY_PLAYLIST_NAME) ?: initialTask.playlistName,
                    taskStore = taskStore,
                    downloadManager = downloadManager
                )
            }
        } finally {
            notifications.finish(requestId, ::publishNotification)
        }
    }

    private suspend fun processTrack(
        trackId: Long,
        requestId: String,
        playlistName: String,
        taskStore: DownloadTaskStore,
        downloadManager: TrackDownloadManager
    ): Result = coroutineScope {
        val task = taskStore.getByTrackId(trackId) ?: return@coroutineScope Result.success()
        if (task.requestId != requestId || task.status !in DownloadTaskStore.ACTIVE_STATES) {
            return@coroutineScope Result.success()
        }

        if (downloadManager.isDownloadedOnDisk(task)) {
            taskStore.markCompleted(
                trackId = trackId,
                requestId = requestId,
                fileName = task.fileName ?: "",
                fileFormat = task.fileFormat ?: "",
                actualQuality = task.actualQuality,
                mediaStoreUri = task.mediaStoreUri!!,
                relativePath = task.relativePath ?: DEFAULT_ROOT_PATH,
                fileSizeBytes = task.fileSizeBytes
            )
            notifications.progress(requestId, 1, ::publishNotification)
            return@coroutineScope Result.success()
        }

        if (!taskStore.updateOwnedProgress(trackId, requestId, DownloadStatus.DOWNLOADING, 0)) {
            return@coroutineScope Result.success()
        }

        val tempDir = File(applicationContext.cacheDir, "download").apply { mkdirs() }
        val tempFile = File(tempDir, "audio-$requestId-$trackId.download")

        try {
            val resolved = entryPoint.searchRepository()
                .resolveDownloadTrackUrl(trackId, task.requestedQuality)
                .getOrElse { throw it }

            currentCoroutineContext().ensureActive()
            if (!taskStore.updateOwnedProgress(
                    trackId = trackId,
                    requestId = requestId,
                    status = DownloadStatus.DOWNLOADING,
                    progress = 1,
                    actualQuality = resolved.actualLevel
                )
            ) {
                throw CancellationException("Download task was retired")
            }

            var lastUpdateMs = 0L
            downloadAudioFile(
                client = getDownloadClient(),
                url = resolved.url,
                file = tempFile
            ) { percent ->
                val now = System.currentTimeMillis()
                val clamped = percent.coerceIn(1, 96)
                if (now - lastUpdateMs >= PROGRESS_UPDATE_INTERVAL_MS || clamped >= 96) {
                    lastUpdateMs = now
                    if (!taskStore.updateOwnedProgress(
                            trackId = trackId,
                            requestId = requestId,
                            status = DownloadStatus.DOWNLOADING,
                            progress = clamped,
                            actualQuality = resolved.actualLevel
                        )
                    ) {
                        throw CancellationException("Download task was retired")
                    }
                }
            }

            currentCoroutineContext().ensureActive()
            taskStore.updateOwnedProgress(trackId, requestId, DownloadStatus.DOWNLOADING, 97, resolved.actualLevel)

            val format = detectAudioFileFormat(tempFile)
                ?: throw IllegalStateException("下载的音频文件损坏或格式不受支持")

            val durationMs = runCatching {
                AudioFileIO.readAs(tempFile, format.extension).audioHeader.trackLength.toLong() * 1000L
            }.getOrDefault(0L).takeIf { it > 0L } ?: task.durationMs

            val coverDeferred = async(Dispatchers.IO) {
                val url = task.coverUrl?.takeIf { it.isNotBlank() } ?: return@async null
                fetchCoverBytes(getDownloadClient(), url)
            }
            val lyricDeferred = async(Dispatchers.IO) {
                withTimeoutOrNull(4_000L) {
                    runCatching {
                        val parsed = entryPoint.parsedLyricsRepository().getLyrics(trackId).getOrNull()
                        AudioTagWriter.formatLyricsToLrc(parsed)
                    }.getOrNull()
                }
            }

            val coverBytes = coverDeferred.await()
            val lrcLyrics = lyricDeferred.await()

            AudioTagWriter.writeTags(
                file = tempFile,
                format = format,
                title = task.title,
                artist = task.artist,
                album = task.album,
                coverBytes = coverBytes,
                lrcLyrics = lrcLyrics
            )

            currentCoroutineContext().ensureActive()
            check(detectAudioFileFormat(tempFile) == format) { "写入标签后音频容器校验失败" }

            taskStore.updateOwnedProgress(trackId, requestId, DownloadStatus.DOWNLOADING, 99, resolved.actualLevel)

            val configuredDirectory = inputData.getString(KEY_DOWNLOAD_DIRECTORY)
                ?.takeIf { it.isNotBlank() }
                ?: task.downloadDirectory.takeIf { it.isNotBlank() }
                ?: entryPoint.playerSettingsManager().downloadDirectoryPath.value
            val normalizedDirectory = PlayerSettingsManager.normalizeDirectoryPath(configuredDirectory)
                .ifBlank { PlayerSettingsManager.DEFAULT_DOWNLOAD_DIRECTORY }
            val publishTarget = DownloadPathResolver.resolve(
                configuredDirectoryPath = normalizedDirectory,
                externalRootPath = Environment.getExternalStorageDirectory().absolutePath
            )
            val fileName = "${DownloadFileNames.songBaseName(task.title, task.artist)}.${format.extension}"

            var publishedUri: Uri? = null
            try {
                publishedUri = publishDownloadedAudio(
                    context = applicationContext,
                    srcFile = tempFile,
                    displayName = fileName,
                    format = format,
                    target = publishTarget,
                    title = task.title,
                    artist = task.artist,
                    album = task.album,
                    durationMs = durationMs
                ) ?: throw IllegalStateException("写入目标目录失败")

                val committed = taskStore.markCompleted(
                    trackId = trackId,
                    requestId = requestId,
                    fileName = fileName,
                    fileFormat = format.extension,
                    actualQuality = resolved.actualLevel,
                    mediaStoreUri = publishedUri.toString(),
                    relativePath = normalizedDirectory,
                    fileSizeBytes = tempFile.length()
                )
                if (!committed) {
                    throw CancellationException("Download task was retired before commit")
                }
                publishedUri = null
            } finally {
                publishedUri?.let { orphanUri ->
                    withContext(NonCancellable) {
                        runCatching {
                            if (orphanUri.scheme == "content") {
                                applicationContext.contentResolver.delete(orphanUri, null, null)
                            } else {
                                orphanUri.path?.let { File(it).delete() }
                            }
                        }
                    }
                }
            }

            notifications.progress(requestId, 1, ::publishNotification)
            downloadManager.notifyEvent(DownloadEvent.Completed(trackId, task.title, normalizedDirectory))
            runCatching {
                entryPoint.localMusicRepository().getLocalTracks(forceRefresh = true)
            }
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            Log.w(TAG, "Download failed for trackId=$trackId (${task.title})", error)
            val friendlyReason = error.toUserFriendlyMessage("下载")
            taskStore.updateOwnedProgress(
                trackId = trackId,
                requestId = requestId,
                status = DownloadStatus.FAILED,
                progress = 0,
                errorMessage = friendlyReason
            )
            downloadManager.notifyEvent(DownloadEvent.Failed(trackId, task.title, friendlyReason))
            Result.failure()
        } finally {
            tempFile.delete()
        }
    }

    private fun publishNotification(progress: DownloadNotificationProgress?) {
        try {
            val manager = NotificationManagerCompat.from(applicationContext)
            if (progress == null) {
                manager.cancel(NOTIFICATION_ID)
                return
            }
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("正在下载 (${progress.done}/${progress.total})")
                .setContentText("SeteaseCloudMusic 歌曲下载")
                .setSmallIcon(R.drawable.ic_action_download)
                .setOngoing(true)
                .setProgress(progress.total, progress.done, false)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted", e)
        }
    }

    companion object {
        private const val TAG = "DownloadWorker"
        const val KEY_TRACK_ID = "track_id"
        const val KEY_PLAYLIST_NAME = "playlist_name"
        const val KEY_DOWNLOAD_DIRECTORY = "download_directory"
        const val DEFAULT_ROOT_PATH = PlayerSettingsManager.DEFAULT_DOWNLOAD_DIRECTORY
        const val CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_ID = 2001
        private const val PROGRESS_UPDATE_INTERVAL_MS = 250L
        private const val MAX_COVER_BYTES = 5 * 1024 * 1024L

        // 全局限制最多 3 个音频并发下载
        private val downloadSlots = Semaphore(3)
        private val notifications = DownloadNotificationState()

        @Volatile
        private var sharedClient: OkHttpClient? = null

        fun getDownloadClient(): OkHttpClient {
            return sharedClient ?: synchronized(this) {
                sharedClient ?: OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .build().also { sharedClient = it }
            }
        }

        fun createNotificationChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "音乐下载",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "歌曲下载进度通知"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}

private data class DownloadNotificationProgress(val done: Int, val total: Int)

private class DownloadNotificationState {
    private val requests = mutableMapOf<String, DownloadNotificationProgress>()

    @Synchronized
    fun start(requestId: String, total: Int, publish: (DownloadNotificationProgress?) -> Unit) {
        if (total <= 0) return
        requests[requestId] = DownloadNotificationProgress(0, total)
        publish(aggregate())
    }

    @Synchronized
    fun progress(requestId: String, done: Int, publish: (DownloadNotificationProgress?) -> Unit) {
        val current = requests[requestId] ?: return
        requests[requestId] = current.copy(done = done.coerceIn(current.done, current.total))
        publish(aggregate())
    }

    @Synchronized
    fun finish(requestId: String, publish: (DownloadNotificationProgress?) -> Unit) {
        if (requests.remove(requestId) == null) return
        publish(aggregate())
    }

    private fun aggregate(): DownloadNotificationProgress? =
        requests.values.takeIf { it.isNotEmpty() }?.let { active ->
            DownloadNotificationProgress(active.sumOf { it.done }, active.sumOf { it.total })
        }
}

private suspend fun <T> executeCancellableCall(
    client: OkHttpClient,
    request: Request,
    consume: (Response) -> T
): T = withContext(Dispatchers.IO) {
    suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        try {
            if (!continuation.isActive) throw CancellationException("Download canceled")
            val value = call.execute().use(consume)
            continuation.resume(value)
        } catch (error: Throwable) {
            if (continuation.isActive) {
                continuation.resumeWithException(error)
            }
        }
    }
}

private suspend fun downloadAudioFile(
    client: OkHttpClient,
    url: String,
    file: File,
    onProgress: (Int) -> Unit
) = withContext(Dispatchers.IO) {
    val owner = currentCoroutineContext()
    try {
        executeCancellableCall(client, Request.Builder().url(url).build()) { response ->
            check(response.isSuccessful) { "音频下载失败 (HTTP ${response.code})" }
            val body = response.body ?: error("音频响应体为空")
            val total = body.contentLength()
            var received = 0L
            var lastProgress = -1
            file.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        owner.ensureActive()
                        val size = input.read(buffer)
                        if (size < 0) break
                        output.write(buffer, 0, size)
                        received += size
                        if (total > 0) {
                            val progress = (received * 100 / total).coerceAtMost(96).toInt()
                            if (progress != lastProgress) {
                                lastProgress = progress
                                onProgress(progress)
                            }
                        }
                    }
                }
            }
            check(received > 0L && (total < 0L || received == total)) { "音频下载不完整" }
        }
    } catch (error: Throwable) {
        file.delete()
        throw error
    }
}

private suspend fun fetchCoverBytes(
    client: OkHttpClient,
    rawUrl: String
): ByteArray? = withTimeoutOrNull(15_000L) {
    runCatching {
        val secureUrl = if (rawUrl.startsWith("http://")) {
            rawUrl.replaceFirst("http://", "https://")
        } else {
            rawUrl
        }
        executeCancellableCall(client, Request.Builder().url(secureUrl).build()) { response ->
            if (!response.isSuccessful) return@executeCancellableCall null
            val body = response.body ?: return@executeCancellableCall null
            val length = body.contentLength()
            if (length > 5 * 1024 * 1024L) return@executeCancellableCall null
            val bytes = body.bytes()
            bytes.takeIf { it.isNotEmpty() && it.size <= 5 * 1024 * 1024 }
        }
    }.getOrNull()
}

private suspend fun publishDownloadedAudio(
    context: Context,
    srcFile: File,
    displayName: String,
    format: AudioFileFormat,
    target: DownloadPublishTarget,
    title: String,
    artist: String,
    album: String,
    durationMs: Long
): Uri? {
    val resolver = context.contentResolver
    return when (target) {
        is DownloadPublishTarget.MediaStoreAudio -> {
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${target.relativePath}/")
                put(MediaStore.Audio.Media.MIME_TYPE, format.mimeType)
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.ARTIST, artist)
                put(MediaStore.Audio.Media.ALBUM, album)
                if (durationMs > 0L) {
                    put(MediaStore.Audio.Media.DURATION, durationMs)
                }
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return null
            copyAndFinalizeMediaStoreUri(context, srcFile, uri, contentValues)
        }

        is DownloadPublishTarget.MediaStoreDownloads -> {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.RELATIVE_PATH, "${target.relativePath}/")
                put(MediaStore.Downloads.MIME_TYPE, format.mimeType)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return null
            val finalized = copyAndFinalizeMediaStoreUri(context, srcFile, uri, contentValues) ?: return null
            val physicalPath = File(
                Environment.getExternalStorageDirectory(),
                "${target.relativePath}/$displayName"
            ).absolutePath
            runCatching {
                MediaScannerConnection.scanFile(context, arrayOf(physicalPath), arrayOf(format.mimeType), null)
            }
            finalized
        }

        is DownloadPublishTarget.DirectFile -> {
            val dir = File(target.directoryPath)
            try {
                if (!dir.exists() && !dir.mkdirs()) {
                    throw IllegalStateException("无权写入该目录，请选择 Music/ 或 Download/ 下的目录")
                }
                val destFile = File(dir, displayName)
                srcFile.inputStream().use { input ->
                    destFile.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val size = input.read(buffer)
                            if (size < 0) break
                            output.write(buffer, 0, size)
                        }
                    }
                }
                runCatching {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(destFile.absolutePath),
                        arrayOf(format.mimeType),
                        null
                    )
                }
                Uri.fromFile(destFile)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: SecurityException) {
                throw IllegalStateException("无权写入该目录，请选择 Music/ 或 Download/ 下的目录", e)
            } catch (e: Exception) {
                Log.e("DownloadWorker", "Failed to write direct file to ${target.directoryPath}", e)
                throw IllegalStateException("无权写入该目录，请选择 Music/ 或 Download/ 下的目录", e)
            }
        }
    }
}

private suspend fun copyAndFinalizeMediaStoreUri(
    context: Context,
    srcFile: File,
    uri: Uri,
    contentValues: ContentValues
): Uri? {
    val resolver = context.contentResolver
    try {
        val os = checkNotNull(resolver.openOutputStream(uri)) { "Unable to open MediaStore output stream" }
        os.use { out ->
            srcFile.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val size = input.read(buffer)
                    if (size < 0) break
                    out.write(buffer, 0, size)
                }
            }
        }
        contentValues.clear()
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
        currentCoroutineContext().ensureActive()
        check(resolver.update(uri, contentValues, null, null) == 1) {
            "MediaStore rejected audio publication"
        }
        return uri
    } catch (cancelled: CancellationException) {
        withContext(NonCancellable) {
            runCatching { resolver.delete(uri, null, null) }
        }
        throw cancelled
    } catch (e: Exception) {
        Log.e("DownloadWorker", "Failed to publish audio to MediaStore", e)
        withContext(NonCancellable) {
            runCatching { resolver.delete(uri, null, null) }
        }
        return null
    }
}
