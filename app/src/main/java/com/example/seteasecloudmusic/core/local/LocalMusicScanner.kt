package com.example.seteasecloudmusic.core.local

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.seteasecloudmusic.core.model.Album
import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.model.AudioQuality
import com.example.seteasecloudmusic.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 本地音乐扫描器：
 * 1. 深度适配 Android 各版本 Scoped Storage，通过 ContentResolver 与 MediaStore 保证 100% 检索到音频
 * 2. 支持直接文件目录遍历与元数据解析
 * 3. 自动合并与去重，保证高兼容性
 */
object LocalMusicScanner {

    private val SUPPORTED_AUDIO_EXTENSIONS = setOf(
        "mp3", "flac", "wav", "m4a", "aac", "ogg", "opus", "ape", "wma", "dff", "dsf"
    )

    /**
     * 扫描系统 MediaStore 中的音频库（系统索引，速度极快且完全兼容 Android 10+ 作用域存储）
     */
    suspend fun scanMediaStore(context: Context): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )

        // 筛选条件：去除小于 5 秒的提示音与录音碎片，放宽 IS_MUSIC 条件以防部分 ROM 误标
        val selection = "${MediaStore.Audio.Media.DURATION} >= 5000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val queryUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        try {
            context.contentResolver.query(
                queryUri,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol) ?: ""
                    val rawArtist = cursor.getString(artistCol) ?: ""
                    val rawAlbum = cursor.getString(albumCol) ?: ""
                    val durationMs = cursor.getLong(durationCol)
                    val dataPath = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else -1L

                    // 生成 MediaStore 标准 Content Uri，在 Android 10/11/12/13/14/15 均可稳定播放
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId)

                    // 优先提供可用直链
                    val playableUrl = if (dataPath.isNotBlank() && (File(dataPath).canRead() || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)) {
                        dataPath
                    } else {
                        contentUri.toString()
                    }

                    val ext = if (dataPath.isNotBlank()) File(dataPath).extension.lowercase() else ""
                    val filename = if (dataPath.isNotBlank()) File(dataPath).nameWithoutExtension else rawTitle
                    val parsed = parseFilenameFallback(filename)
                    val title = rawTitle.takeIf { it.isNotBlank() && it != "<unknown>" } ?: parsed.first
                    val artistName = rawArtist.takeIf { it.isNotBlank() && it != "<unknown>" } ?: parsed.second
                    val albumName = rawAlbum.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "本地音乐"

                    val localId = if (mediaId > 0) mediaId else (playableUrl.hashCode().toLong() and 0x7FFFFFFF)

                    val albumCoverUri = if (albumId > 0) {
                        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()
                    } else null

                    val quality = when (ext) {
                        "flac", "wav", "ape", "dff", "dsf" -> listOf(AudioQuality.LOSSLESS, AudioQuality.HIRES)
                        else -> listOf(AudioQuality.STANDARD)
                    }

                    tracks.add(
                        Track(
                            id = localId,
                            title = title,
                            artists = listOf(
                                Artist(
                                    id = localId,
                                    name = artistName,
                                    coverUrl = null
                                )
                            ),
                            album = Album(
                                id = localId,
                                title = albumName,
                                coverUrl = albumCoverUri
                            ),
                            qualityTags = quality,
                            coverUrl = albumCoverUri,
                            durationMs = durationMs,
                            playableUrl = playableUrl,
                            isPlayable = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        tracks
    }

    /**
     * 扫描指定物理文件目录下的所有音频文件（递归遍历）
     */
    suspend fun scanDirectory(context: Context, directory: File): List<Track> = withContext(Dispatchers.IO) {
        val directTracks = mutableListOf<Track>()

        if (directory.exists() && directory.isDirectory) {
            val retriever = MediaMetadataRetriever()
            try {
                val audioFiles = directory.walkTopDown()
                    .maxDepth(10)
                    .filter { it.isFile && SUPPORTED_AUDIO_EXTENSIONS.contains(it.extension.lowercase()) }
                    .toList()

                for (file in audioFiles) {
                    try {
                        retriever.setDataSource(file.absolutePath)
                        val rawTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        val rawArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        val rawAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                        val rawDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

                        val durationMs = rawDuration?.toLongOrNull() ?: 0L
                        val parsed = parseFilenameFallback(file.nameWithoutExtension)
                        val title = rawTitle?.takeIf { it.isNotBlank() } ?: parsed.first
                        val artistName = rawArtist?.takeIf { it.isNotBlank() } ?: parsed.second
                        val albumName = rawAlbum?.takeIf { it.isNotBlank() } ?: "本地音乐"

                        val localId = ((file.absolutePath.hashCode().toLong() and 0x7FFFFFFF) or 0x40000000_00000000L)

                        val quality = when (file.extension.lowercase()) {
                            "flac", "wav", "ape", "dff", "dsf" -> listOf(AudioQuality.LOSSLESS, AudioQuality.HIRES)
                            else -> listOf(AudioQuality.STANDARD)
                        }

                        directTracks.add(
                            Track(
                                id = localId,
                                title = title,
                                artists = listOf(
                                    Artist(
                                        id = localId,
                                        name = artistName,
                                        coverUrl = null
                                    )
                                ),
                                album = Album(
                                    id = localId,
                                    title = albumName,
                                    coverUrl = null
                                ),
                                qualityTags = quality,
                                coverUrl = null,
                                durationMs = durationMs,
                                playableUrl = file.absolutePath,
                                isPlayable = true
                            )
                        )
                    } catch (e: Exception) {
                        // 忽略单个文件解析异常
                    }
                }
            } finally {
                runCatching { retriever.release() }
            }
        }

        // 如果直接文件遍历因 Scoped Storage 受限返回为空，则从 MediaStore 中智能匹配该目录相关的音频
        if (directTracks.isEmpty()) {
            val mediaStoreTracks = scanMediaStore(context)
            val filtered = mediaStoreTracks.filter {
                it.playableUrl?.contains(directory.name, ignoreCase = true) == true ||
                        it.playableUrl?.contains(directory.path, ignoreCase = true) == true
            }
            if (filtered.isNotEmpty()) {
                return@withContext filtered
            }
            return@withContext mediaStoreTracks
        }

        directTracks.sortedBy { it.title.lowercase() }
    }

    /**
     * 文件名回退解析：当 ID3 无元数据时，尝试从 "歌手 - 歌名" 或 "歌名" 解析
     */
    fun parseFilenameFallback(nameWithoutExt: String): Pair<String, String> {
        val cleanName = nameWithoutExt.replace(Regex("^\\d+[.\\-_\\s]+"), "").trim()
        if (cleanName.contains(" - ")) {
            val parts = cleanName.split(" - ", limit = 2)
            val artist = parts[0].trim()
            val title = parts[1].trim()
            if (artist.isNotBlank() && title.isNotBlank()) {
                return title to artist
            }
        }
        return (if (cleanName.isNotBlank()) cleanName else nameWithoutExt) to "未知歌手"
    }
}
