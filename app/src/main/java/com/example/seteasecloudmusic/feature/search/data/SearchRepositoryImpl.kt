package com.example.seteasecloudmusic.feature.search.data

import com.example.seteasecloudmusic.core.model.Album
import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.model.AudioQuality
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.local.LocalMusicRepository
import com.example.seteasecloudmusic.core.network.ApiHttpException
import com.example.seteasecloudmusic.core.player.TrackPlaybackPreparer
import com.example.seteasecloudmusic.core.settings.PlayerSettingsManager
import com.example.seteasecloudmusic.feature.search.domain.ArtistSuggestion
import com.example.seteasecloudmusic.feature.search.domain.HotSearchItem
import com.example.seteasecloudmusic.feature.search.domain.PlaylistSuggestion
import com.example.seteasecloudmusic.feature.search.domain.SearchRepository
import com.example.seteasecloudmusic.feature.search.domain.SearchSuggestions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.net.ssl.SSLException

/**
 * 搜索与播放预处理仓库实现，实现 [SearchRepository] 与 [TrackPlaybackPreparer]。
 * 负责曲目/歌手/歌单搜索、本地与在线流媒体播放直链解析与缓存，以及听歌打卡上报。
 */
class SearchRepositoryImpl @Inject constructor(
    private val musicService: NeteaseMusicService,
    private val localMusicRepository: LocalMusicRepository,
    private val playerSettingsManager: PlayerSettingsManager,
    private val ncblReporter: com.example.seteasecloudmusic.core.network.ncbl.NcblReporter
) : SearchRepository, TrackPlaybackPreparer {

    private data class CachedTrackUrl(
        val url: String,
        val fetchedAtMs: Long = System.currentTimeMillis(),
        val bitrateKbps: Int? = null,
        val sampleRateHz: Int? = null,
        val fileSizeBytes: Long? = null,
        val format: String? = null,
        val level: String? = null
    )

    // 内存缓存：按 "$trackId-$level" 缓存已解析的播放直链、获取时间与音质元信息，避免重复请求
    private val trackUrlCache = ConcurrentHashMap<String, CachedTrackUrl>()
    // 歌手头像内存缓存：按 artistId 缓存已解析的头像 URL，用于补全缺少头像的曲目与建议项
    private val artistAvatarCache = ConcurrentHashMap<Long, String>()

    private fun resolveCoverUrl(picUrl: String?, img1v1Url: String?): String? {
        // 5639395138885805 为网易云默认空白占位头像 ID，存在真实头像时优先过滤
        val pic = picUrl?.trim()?.takeIf { it.isNotBlank() && !it.contains("5639395138885805") }
        val img = img1v1Url?.trim()?.takeIf { it.isNotBlank() && !it.contains("5639395138885805") }
        val chosen = pic ?: img ?: picUrl?.trim()?.takeIf { it.isNotBlank() } ?: img1v1Url?.trim()?.takeIf { it.isNotBlank() }
        return chosen?.let { if (it.startsWith("http://")) it.replaceFirst("http://", "https://") else it }
    }

    override suspend fun searchTracks(
        query: String,
        limit: Int,
        offset: Int
    ): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val response = musicService.searchSongs(query, limit, offset)
            if (response.code == 200) {
                val tracks = response.result?.songs?.map { song ->
                    mapToDomainTrack(song)
                } ?: emptyList()
                Result.success(tracks)
            } else {
                Result.failure(Exception("API Error with code: ${response.code}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchArtists(
        query: String,
        limit: Int,
        offset: Int
    ): Result<List<Artist>> = withContext(Dispatchers.IO) {
        try {
            val response = musicService.searchArtists(query, limit, offset)
            if (response.code == 200) {
                val artists = response.result?.artists?.mapNotNull { item ->
                    val id = item.id ?: return@mapNotNull null
                    val name = item.name ?: return@mapNotNull null
                    val cover = resolveCoverUrl(item.picUrl, item.img1v1Url)
                    if (cover != null && id > 0L) {
                        artistAvatarCache[id] = cover
                    }
                    Artist(
                        id = id,
                        name = name,
                        coverUrl = cover
                    )
                } ?: emptyList()
                Result.success(artists)
            } else {
                Result.failure(Exception("API Error with code: ${response.code}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchPlaylists(
        query: String,
        limit: Int,
        offset: Int
    ): Result<List<PlaylistSuggestion>> = withContext(Dispatchers.IO) {
        try {
            val response = musicService.searchPlaylists(query, limit, offset)
            if (response.code == 200) {
                val playlists = response.result?.playlists?.mapNotNull { item ->
                    val id = item.id.takeIf { it > 0L } ?: return@mapNotNull null
                    val name = item.name ?: "未知歌单"
                    val rawCover = item.coverImgUrl?.takeIf { it.isNotBlank() }
                    val cover = rawCover?.let { if (it.startsWith("http://")) it.replaceFirst("http://", "https://") else it }
                    PlaylistSuggestion(
                        id = id,
                        name = name,
                        coverUrl = cover,
                        trackCount = item.trackCount
                    )
                } ?: emptyList()
                Result.success(playlists)
            } else {
                Result.failure(Exception("API Error with code: ${response.code}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 网易云直链是短时效签名 URL（通常 20 分钟左右过期）：
     * 1. 若路径中携带 yyyyMMddHHmmss 格式的过期时间戳（北京时间），提前 3 分钟判定过期；
     * 2. 若无路径时间戳，按缓存获取时间超过 15 分钟判定过期。
     */
    private fun isUrlExpired(
        url: String,
        fetchedAtMs: Long? = null,
        safetyMarginMs: Long = 180_000L // 直链过期提前判定的安全余量（ms，默认提前 3 分钟失效）
    ): Boolean {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false
        val now = System.currentTimeMillis()
        // 无路径时间戳时，缓存获取超过 15 分钟即判定过期
        if (fetchedAtMs != null && now - fetchedAtMs >= 15 * 60_000L) {
            return true
        }
        val timestamp = Regex("""/(\d{14})(?:/|$)""").find(url)?.groupValues?.get(1)
        if (timestamp != null) {
            return try {
                val format = SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Shanghai")
                }
                val expiredAt = format.parse(timestamp)?.time ?: return false
                now + safetyMarginMs >= expiredAt
            } catch (_: Exception) {
                false
            }
        }
        return false
    }

    override fun isStreamUrlExpired(url: String): Boolean {
        return isUrlExpired(url)
    }

    override fun invalidateTrackUrl(trackId: Long) {
        val prefix = "$trackId-"
        trackUrlCache.keys.removeIf { it.startsWith(prefix) }
    }

    override suspend fun getTrackUrl(
        trackId: Long,
        level: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cacheKey = "$trackId-$level"
        trackUrlCache[cacheKey]?.let { cached ->
            if (!isUrlExpired(cached.url, cached.fetchedAtMs)) {
                return@withContext Result.success(cached.url)
            }
            // 缓存的直链已过期：立即剔除，走下面的网络请求获取最新地址
            trackUrlCache.remove(cacheKey)
        }

        try {
            val response = resolveSongUrlWithFallback(
                fetchSongUrlV1 = { crypto ->
                    musicService.getSongUrl(id = trackId, level = level, crypto = crypto)
                },
                fetchLegacySongUrl = {
                    musicService.getLegacySongUrl(id = trackId)
                }
            )
            val songData = response.data.firstOrNull()
            val url = songData?.url

            if (response.code == 200 && !url.isNullOrBlank()) {
                val secureUrl = if (url.startsWith("http://")) {
                    url.replaceFirst("http://", "https://")
                } else {
                    url
                }
                val bitrateKbps = songData.br?.let { br ->
                    when {
                        br > 10000 -> (br + 500) / 1000
                        br in 1..10000 -> br
                        else -> null
                    }
                }
                val sampleRateHz = songData.sr?.takeIf { it > 0 }
                val fileSizeBytes = songData.size?.takeIf { it > 0L }
                val resolvedLevel = songData.level?.trim()?.takeIf { it.isNotBlank() } ?: level
                val format = resolveOnlineAudioFormat(
                    url = secureUrl,
                    type = songData.type,
                    encodeType = songData.encodeType,
                    level = resolvedLevel,
                    bitrateKbps = bitrateKbps
                )

                trackUrlCache[cacheKey] = CachedTrackUrl(
                    url = secureUrl,
                    bitrateKbps = bitrateKbps,
                    sampleRateHz = sampleRateHz,
                    fileSizeBytes = fileSizeBytes,
                    format = format,
                    level = resolvedLevel
                )
                Result.success(secureUrl)
            } else {
                Result.failure(Exception("No playable URL available for track $trackId at level $level"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun invoke(track: Track): Result<Track> {
        val currentQualityLevel = playerSettingsManager.audioQuality.value.levelKey

        // 1. 若当前曲目已带有可用播放直链（本地扫描或预先准备），先校验在线直链是否未过期
        val existingUrl = track.playableUrl
        if (!existingUrl.isNullOrBlank() && track.isPlayable && !isUrlExpired(existingUrl)) {
            val isLocalUrl = existingUrl.startsWith("content:") ||
                existingUrl.startsWith("file:") ||
                existingUrl.startsWith("/")
            return if (isLocalUrl) {
                Result.success(
                    track.copy(
                        audioSourceLabel = track.audioSourceLabel ?: "本地音乐 · 设备存储",
                        audioFormat = track.audioFormat ?: extractAudioFormatFromUrl(existingUrl),
                        audioQualityLevel = track.audioQualityLevel ?: "local"
                    )
                )
            } else {
                val cached = trackUrlCache["${track.id}-$currentQualityLevel"]
                    ?: trackUrlCache.values.firstOrNull { it.url == existingUrl }
                val resolvedBitrate = track.audioBitrateKbps ?: cached?.bitrateKbps
                val resolvedLevel = track.audioQualityLevel ?: cached?.level ?: currentQualityLevel
                val resolvedFormat = resolveOnlineAudioFormat(
                    url = existingUrl,
                    type = cached?.format ?: track.audioFormat,
                    encodeType = track.audioFormat,
                    level = resolvedLevel,
                    bitrateKbps = resolvedBitrate
                )
                Result.success(
                    track.copy(
                        audioSourceLabel = track.audioSourceLabel ?: "网易云音乐 · 在线流媒体",
                        audioBitrateKbps = resolvedBitrate,
                        audioSampleRateHz = track.audioSampleRateHz ?: cached?.sampleRateHz,
                        audioFileSizeBytes = track.audioFileSizeBytes ?: cached?.fileSizeBytes,
                        audioFormat = resolvedFormat,
                        audioQualityLevel = resolvedLevel
                    )
                )
            }
        }

        // 2. 优先匹配本地曲库中已存在的音频文件，命中后直接走本地播放以节省流量
        val localMatch = localMusicRepository.findMatchingLocalTrack(track)
        if (localMatch != null && !localMatch.playableUrl.isNullOrBlank()) {
            android.util.Log.d("SearchRepo", "Online track '${track.title}' (ID: ${track.id}) matched LOCAL FILE: ${localMatch.playableUrl}")
            return Result.success(
                track.copy(
                    playableUrl = localMatch.playableUrl,
                    isPlayable = true,
                    audioSourceLabel = "本地音乐 · 智能匹配省流",
                    audioFormat = localMatch.audioFormat ?: extractAudioFormatFromUrl(localMatch.playableUrl),
                    audioBitrateKbps = localMatch.audioBitrateKbps,
                    audioSampleRateHz = localMatch.audioSampleRateHz,
                    audioFileSizeBytes = localMatch.audioFileSizeBytes,
                    audioQualityLevel = "local"
                )
            )
        }
        android.util.Log.d("SearchRepo", "Online track '${track.title}' (ID: ${track.id}) no local match, fetching online stream for quality: $currentQualityLevel")

        // 3. 本地无匹配，根据用户设定的音质等级向云端请求直链
        return getTrackUrl(track.id, level = currentQualityLevel).map { url ->
            val cached = trackUrlCache["${track.id}-$currentQualityLevel"]
            track.copy(
                playableUrl = url,
                isPlayable = url.isNotBlank(),
                audioSourceLabel = "网易云音乐 · 在线流媒体",
                audioBitrateKbps = cached?.bitrateKbps,
                audioSampleRateHz = cached?.sampleRateHz,
                audioFileSizeBytes = cached?.fileSizeBytes,
                audioFormat = cached?.format ?: extractAudioFormatFromUrl(url),
                audioQualityLevel = cached?.level ?: currentQualityLevel
            )
        }
    }

    override suspend fun scrobbleStart(
        trackId: Long,
        sourceId: Long?,
        title: String,
        artist: String,
        totalDurationSeconds: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val sid = sourceId ?: 0L
        ncblReporter.reportPlv(
            songId = trackId,
            totalDurationSeconds = totalDurationSeconds,
            sourceId = sid
        )
    }

    override suspend fun scrobble(
        trackId: Long,
        durationSeconds: Int,
        sourceId: Long?,
        title: String,
        artist: String,
        totalDurationSeconds: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val sid = sourceId ?: 0L

        // 1. 优先通过 NCBL 上报播放结束日志（PLD）到 clientlog3.music.163.com，记录收听时长与足迹
        val ncblResult = ncblReporter.reportPld(
            songId = trackId,
            playDurationSeconds = durationSeconds,
            totalDurationSeconds = totalDurationSeconds,
            sourceId = sid,
            endReason = "playend"
        )

        // 2. 同步调用 /scrobble 接口作为兜底上报
        try {
            val response = musicService.scrobble(id = trackId, sourceId = sid, time = durationSeconds)
            android.util.Log.d("Scrobble", ">>> Server /scrobble response code: ${response.code}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("Scrobble", ">>> Server /scrobble warning: ${e.message}")
        }

        ncblResult
    }

    private val localToOnlineIdCache = java.util.concurrent.ConcurrentHashMap<String, Long>()

    override suspend fun resolveOnlineTrackId(title: String, artist: String): Long? = withContext(Dispatchers.IO) {
        val cacheKey = "$title-$artist".lowercase().trim()
        localToOnlineIdCache[cacheKey]?.let { return@withContext it }

        try {
            val query = if (artist.isNotBlank() && !artist.contains("未知")) "$title $artist" else title
            // 本地曲目反查在线 ID 时最多拉取前 5 条候选进行标题匹配
            val searchRes = searchTracks(query, limit = 5, offset = 0).getOrNull() ?: emptyList()
            val matched = searchRes.firstOrNull { t ->
                val normSearch = t.title.lowercase().replace(" ", "")
                val normLocal = title.lowercase().replace(" ", "")
                normSearch == normLocal || normSearch.contains(normLocal) || normLocal.contains(normSearch)
            } ?: searchRes.firstOrNull()

            val onlineId = matched?.id?.takeIf { it > 0L }
            if (onlineId != null) {
                localToOnlineIdCache[cacheKey] = onlineId
                android.util.Log.d("Scrobble", ">>> Resolved local track '$title - $artist' to online NetEase ID: $onlineId")
            }
            onlineId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getHotSearches(): Result<List<HotSearchItem>> = withContext(Dispatchers.IO) {
        try {
            // 1. 优先尝试获取详细热搜列表
            val detailResponse = musicService.getHotSearchDetail()
            if (detailResponse.code == 200 && !detailResponse.data.isNullOrEmpty()) {
                val list = detailResponse.data.mapNotNull { item ->
                    val word = item.searchWord?.trim() ?: return@mapNotNull null
                    if (word.isEmpty()) return@mapNotNull null
                    HotSearchItem(
                        searchWord = word,
                        score = item.score ?: 0L,
                        content = item.content ?: "",
                        iconUrl = item.iconUrl,
                        iconType = item.iconType ?: 0
                    )
                }
                return@withContext Result.success(list)
            }

            // 2. 兜底获取简略热搜列表
            val briefResponse = musicService.getHotSearch()
            if (briefResponse.code == 200 && !briefResponse.result?.hots.isNullOrEmpty()) {
                val list = briefResponse.result.hots.mapNotNull { item ->
                    val word = item.first?.trim() ?: return@mapNotNull null
                    if (word.isEmpty()) return@mapNotNull null
                    HotSearchItem(
                        searchWord = word,
                        score = item.second ?: 0L,
                        iconType = item.iconType ?: 0
                    )
                }
                return@withContext Result.success(list)
            }

            Result.failure(Exception("热搜数据获取失败: ${detailResponse.code}"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSearchSuggestions(
        query: String,
        type: String?
    ): Result<SearchSuggestions> = withContext(Dispatchers.IO) {
        try {
            val response = musicService.getSearchSuggestions(query, type ?: "mobile")
            if (response.code == 200) {
                val result = response.result
                var allMatch = result?.allMatch?.mapNotNull { it.keyword?.trim() }
                    ?.filter { it.isNotEmpty() } ?: emptyList()
                var songs = result?.songs?.map { mapToDomainTrackFromSuggest(it) } ?: emptyList()
                var artists = result?.artists?.map { mapToArtistSuggestion(it) } ?: emptyList()
                var playlists = result?.playlists?.map { mapToPlaylistSuggestion(it) } ?: emptyList()

                // 如果移动端模式没有返回匹配关键字，但用户可能想要单曲/艺人建议，尝试以默认模式兜底补全
                if (allMatch.isEmpty() && songs.isEmpty() && artists.isEmpty() && playlists.isEmpty() && (type == "mobile" || type == null)) {
                    val fallbackResponse = musicService.getSearchSuggestions(query, null)
                    if (fallbackResponse.code == 200) {
                        val fallbackResult = fallbackResponse.result
                        songs = fallbackResult?.songs?.map { mapToDomainTrackFromSuggest(it) } ?: emptyList()
                        artists = fallbackResult?.artists?.map { mapToArtistSuggestion(it) } ?: emptyList()
                        playlists = fallbackResult?.playlists?.map { mapToPlaylistSuggestion(it) } ?: emptyList()
                    }
                }

                val suggestions = SearchSuggestions(
                    songs = songs,
                    artists = artists,
                    playlists = playlists,
                    allMatch = allMatch
                )
                Result.success(suggestions)
            } else {
                Result.failure(Exception("API Error with code: ${response.code}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 内部映射函数：
     * 把搜索建议中的歌曲响应转换成 domain 层的 Track。
     */
    private fun mapToDomainTrackFromSuggest(song: SearchSuggestSongResponse): Track {
        val albumCover = song.album?.coverUrl?.let { if (it.startsWith("http://")) it.replaceFirst("http://", "https://") else it }
        return Track(
            id = song.id,
            title = song.name ?: "",
            durationMs = song.duration,
            artists = song.artists?.map { artist ->
                val id = artist.id ?: 0L
                val cover = if (id > 0L) artistAvatarCache[id] else null
                Artist(
                    id = id,
                    name = artist.name ?: "未知歌手",
                    coverUrl = cover
                )
            } ?: emptyList(),
            album = Album(
                id = song.album?.id ?: 0L,
                title = song.album?.name ?: "未知专辑",
                coverUrl = albumCover
            ),
            coverUrl = albumCover,
            qualityTags = emptyList(),
            playableUrl = null,
            isPlayable = true
        )
    }

    /**
     * 内部映射函数：
     * 把搜索建议中的歌手响应转换成 domain 层的 ArtistSuggestion。
     */
    private fun mapToArtistSuggestion(artist: SearchSuggestArtistResponse): ArtistSuggestion {
        val cover = artist.coverUrl?.let { if (it.startsWith("http://")) it.replaceFirst("http://", "https://") else it }
        if (cover != null && artist.id > 0L) {
            artistAvatarCache[artist.id] = cover
        }
        return ArtistSuggestion(
            id = artist.id,
            name = artist.name ?: "未知歌手",
            coverUrl = cover
        )
    }

    /**
     * 内部映射函数：
     * 把搜索建议中的歌单响应转换成 domain 层的 PlaylistSuggestion。
     */
    private fun mapToPlaylistSuggestion(playlist: SearchSuggestPlaylistResponse): PlaylistSuggestion {
        val cover = playlist.coverUrl?.let { if (it.startsWith("http://")) it.replaceFirst("http://", "https://") else it }
        return PlaylistSuggestion(
            id = playlist.id,
            name = playlist.name ?: "未知歌单",
            coverUrl = cover,
            trackCount = playlist.trackCount ?: 0
        )
    }

    /**
     * 内部映射函数：
     * 把接口层的 `SearchSongItemResponse` 转换成 domain 层统一使用的 `Track`。
     */
    private fun mapToDomainTrack(song: SearchSongItemResponse): Track {
        val qualityTags = mutableListOf<AudioQuality>()
        if (song.sq != null) qualityTags.add(AudioQuality.LOSSLESS)
        if (song.hr != null) qualityTags.add(AudioQuality.HIRES)
        if (song.h != null) qualityTags.add(AudioQuality.HIGH)
        if (song.l != null || song.m != null) qualityTags.add(AudioQuality.STANDARD)

        val albumCover = song.al?.picUrl?.let { if (it.startsWith("http://")) it.replaceFirst("http://", "https://") else it }

        return Track(
            id = song.id,
            title = song.name,
            durationMs = song.dt,
            artists = song.ar.map { artist ->
                val id = artist.id ?: 0L
                val rawCover = resolveCoverUrl(artist.picUrl, artist.img1v1Url) ?: (if (id > 0L) artistAvatarCache[id] else null)
                val cover = rawCover?.let { if (it.startsWith("http://")) it.replaceFirst("http://", "https://") else it }
                if (cover != null && id > 0L) {
                    artistAvatarCache[id] = cover
                }
                Artist(
                    id = id,
                    name = artist.name ?: "未知歌手",
                    coverUrl = cover
                )
            },
            album = Album(
                id = song.al?.id ?: 0L,
                title = song.al?.name ?: "未知专辑",
                coverUrl = albumCover
            ),
            coverUrl = albumCover,
            qualityTags = qualityTags,
            playableUrl = null,
            isPlayable = song.fee != 4
        )
    }

    companion object {
        // 可识别的音频容器扩展名集合（用于从直链路径提取格式）
        private val SUPPORTED_AUDIO_FORMAT_EXTS = setOf(
            "flac", "mp3", "m4a", "aac", "wav", "ogg", "opus", "ape", "dff", "dsf", "wma", "alac"
        )
        // 无损音频容器格式集合
        private val LOSSLESS_AUDIO_FORMATS = setOf(
            "FLAC", "WAV", "APE", "ALAC", "DFF", "DSF"
        )
        // 视为无损/母带级别的音质等级标识
        private val LOSSLESS_QUALITY_LEVELS = setOf(
            "lossless", "hires", "jymaster"
        )
        // /song/url/v1 加密策略重试顺序：依次尝试 weapi、api、无加密，全部失败后再降级到 /song/url 码率接口
        private val SONG_URL_CRYPTO_FALLBACK_ORDER = listOf("weapi", "api", null)

        internal fun extractAudioFormatFromUrl(url: String?): String? {
            if (url.isNullOrBlank()) return null
            val pathPart = url.substringBefore('?').substringBefore('#')
            val fileName = pathPart.substringAfterLast('/', "")
            val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
            return if (ext in SUPPORTED_AUDIO_FORMAT_EXTS) {
                ext.uppercase(Locale.ROOT)
            } else {
                null
            }
        }

        /**
         * 解析在线流媒体的实际音频格式：
         * 网易云 `/song/url/v1` 返回 Hi-Res / 无损流时，真实容器在 `url` 后缀与 `type` 中（如 `.flac` / `"flac"`），
         * 而接口字段 `encodeType` 经常仍返回 `"mp3"`。因此优先以真实 URL 后缀与 `type` 为准，
         * 仅在二者均缺失时才参考 `encodeType`，并拒绝在无损/高码率流上采信明显矛盾的 `"MP3"`。
         */
        internal fun resolveOnlineAudioFormat(
            url: String?,
            type: String?,
            encodeType: String?,
            level: String?,
            bitrateKbps: Int?
        ): String? {
            val urlFormat = extractAudioFormatFromUrl(url)
            val typeFormat = type?.trim()?.takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT)
            val encodeFormat = encodeType?.trim()?.takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT)
            val normalizedLevel = level?.trim()?.lowercase(Locale.ROOT)
            // 码率超过 320 kbps 视为无损流
            val isLosslessStream = normalizedLevel in LOSSLESS_QUALITY_LEVELS || (bitrateKbps != null && bitrateKbps > 320)

            fun sanitize(candidate: String?): String? {
                if (candidate == null) return null
                if (candidate == "MP3" && isLosslessStream) return null
                return candidate
            }

            if (urlFormat != null && typeFormat != null) {
                return when {
                    urlFormat in LOSSLESS_AUDIO_FORMATS -> urlFormat
                    typeFormat in LOSSLESS_AUDIO_FORMATS -> typeFormat
                    else -> sanitize(urlFormat) ?: sanitize(typeFormat)
                }
            }
            sanitize(urlFormat)?.let { return it }
            sanitize(typeFormat)?.let { return it }
            return sanitize(encodeFormat)
        }

        /**
         * 按 [SONG_URL_CRYPTO_FALLBACK_ORDER] 依次尝试请求 `/song/url/v1`，均失败后降级调用 `/song/url`（最高码率参数）；
         * 若遇底层网络连通性异常（DNS/超时/SSL 等）则直接向上抛出，避免无效重试。
         */
        internal suspend fun resolveSongUrlWithFallback(
            fetchSongUrlV1: suspend (crypto: String?) -> SongResponse,
            fetchLegacySongUrl: suspend () -> SongResponse
        ): SongResponse {
            var firstApiFailure: ApiHttpException? = null
            var lastFailure: Throwable? = null

            for (crypto in SONG_URL_CRYPTO_FALLBACK_ORDER) {
                try {
                    val response = fetchSongUrlV1(crypto)
                    if (response.code == 200) {
                        return response
                    }
                    val apiFailure = ApiHttpException(
                        httpStatusCode = 200,
                        apiErrorCode = response.code,
                        apiMessage = response.data.firstOrNull()?.message,
                        message = "API error code: ${response.code}"
                    )
                    if (firstApiFailure == null) firstApiFailure = apiFailure
                    lastFailure = apiFailure
                } catch (e: CancellationException) {
                    throw e
                } catch (e: ApiHttpException) {
                    if (firstApiFailure == null) firstApiFailure = e
                    lastFailure = e
                } catch (t: Throwable) {
                    if (t.isConnectivityFailure()) {
                        throw t
                    }
                    lastFailure = t
                }
            }

            try {
                val legacy = fetchLegacySongUrl()
                if (legacy.code == 200 && !legacy.data.firstOrNull()?.url.isNullOrBlank()) {
                    return legacy
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                if (lastFailure == null) {
                    lastFailure = t
                }
            }

            throw (firstApiFailure ?: lastFailure ?: IllegalStateException("Failed to resolve song url"))
        }

        private fun Throwable.isConnectivityFailure(): Boolean = when (this) {
            is UnknownHostException,
            is ConnectException,
            is SocketTimeoutException,
            is NoRouteToHostException,
            is SSLException -> true
            else -> false
        }
    }
}
