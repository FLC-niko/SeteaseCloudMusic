package com.example.seteasecloudmusic.core.download

import com.example.seteasecloudmusic.core.settings.PlayerSettingsManager

/** 下载目录对应的存储发布目标。 */
sealed interface DownloadPublishTarget {
    /** 写入 `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`（适用于 `Music/` 等系统音频目录）。 */
    data class MediaStoreAudio(val relativePath: String) : DownloadPublishTarget

    /** 写入 `MediaStore.Downloads.EXTERNAL_CONTENT_URI`（适用于 `Download/` 下目录）。 */
    data class MediaStoreDownloads(val relativePath: String) : DownloadPublishTarget

    /** 直接写入文件系统（适用于非标准媒体库目录）。 */
    data class DirectFile(val directoryPath: String) : DownloadPublishTarget
}

/**
 * 将用户配置的下载目录路径解析为 Android 10+ 作用域存储兼容的写入目标。
 */
object DownloadPathResolver {

    private val AUDIO_MEDIA_ROOTS = mapOf(
        "music" to "Music",
        "alarms" to "Alarms",
        "audiobooks" to "Audiobooks",
        "notifications" to "Notifications",
        "podcasts" to "Podcasts",
        "ringtones" to "Ringtones",
        "recordings" to "Recordings"
    )

    fun resolve(
        configuredDirectoryPath: String,
        externalRootPath: String = "/storage/emulated/0"
    ): DownloadPublishTarget {
        val normalized = PlayerSettingsManager.normalizeDirectoryPath(configuredDirectoryPath)
            .ifBlank { PlayerSettingsManager.DEFAULT_DOWNLOAD_DIRECTORY }
        val normalizedExternalRoot = externalRootPath.trimEnd('/').ifBlank { "/storage/emulated/0" }

        val relative = when {
            normalized.startsWith("$normalizedExternalRoot/") ->
                normalized.removePrefix("$normalizedExternalRoot/")
            normalized.startsWith("/storage/emulated/0/") ->
                normalized.removePrefix("/storage/emulated/0/")
            else -> null
        }?.trim('/')

        if (!relative.isNullOrBlank()) {
            val segments = relative.split('/').filter { it.isNotBlank() }
            if (segments.isNotEmpty()) {
                val headLower = segments.first().lowercase()
                val canonicalAudioRoot = AUDIO_MEDIA_ROOTS[headLower]
                if (canonicalAudioRoot != null) {
                    val rest = segments.drop(1)
                    val canonicalPath = (listOf(canonicalAudioRoot) + rest).joinToString("/")
                    return DownloadPublishTarget.MediaStoreAudio(canonicalPath)
                }
                if (headLower == "download" || headLower == "downloads") {
                    val rest = segments.drop(1)
                    val canonicalPath = (listOf("Download") + rest).joinToString("/")
                    return DownloadPublishTarget.MediaStoreDownloads(canonicalPath)
                }
            }
        }

        return DownloadPublishTarget.DirectFile(normalized.trimEnd('/'))
    }
}
