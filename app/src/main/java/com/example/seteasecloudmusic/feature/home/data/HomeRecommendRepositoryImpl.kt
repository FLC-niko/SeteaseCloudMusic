package com.example.seteasecloudmusic.feature.home.data

import android.content.Context
import com.example.seteasecloudmusic.core.model.Album
import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.model.AudioQuality
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.home.domain.repository.HomeRecommendRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class HomeRecommendRepositoryImpl @Inject constructor(
    private val dailyRecommendService: DailyRecommendService,
    private val homeRecommendCache: HomeRecommendCache,
    @param:ApplicationContext private val context: Context
) : HomeRecommendRepository {

    override fun getCachedDailyRecommendSongs(): List<Track>? {
        return homeRecommendCache.getDailyRecommend()
    }

    override suspend fun getDailyRecommendSongs(afresh: Boolean): Result<List<Track>> =
        withContext(Dispatchers.IO) {
            try {
                if (!hasLoginCookie()) {
                    // 若未登录但本地有历史缓存，先降级返回历史缓存
                    val cached = homeRecommendCache.getDailyRecommend()
                    if (!cached.isNullOrEmpty()) {
                        return@withContext Result.success(cached)
                    }
                    return@withContext Result.failure(Exception("请先登录后再获取每日推荐"))
                }

                val response = dailyRecommendService.getDailyRecommendSongs(afresh)
                if (response.code != 200) {
                    val cached = homeRecommendCache.getDailyRecommend()
                    if (!cached.isNullOrEmpty()) {
                        return@withContext Result.success(cached)
                    }
                    return@withContext Result.failure(Exception("获取每日推荐失败: ${response.code}"))
                }

                val songs = response.data?.dailySongs
                    ?.takeIf { it.isNotEmpty() }
                    ?: response.dailySongs

                val tracks = songs
                    .map(::mapToTrack)
                    .distinctBy { it.id }

                if (tracks.isNotEmpty()) {
                    homeRecommendCache.saveDailyRecommend(tracks)
                }

                Result.success(tracks)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val cached = homeRecommendCache.getDailyRecommend()
                if (!cached.isNullOrEmpty()) {
                    Result.success(cached)
                } else {
                    Result.failure(e)
                }
            }
        }

    private fun mapToTrack(song: DailyRecommendSongItemResponse): Track {
        val qualityTags = mutableListOf<AudioQuality>()
        if (song.sq != null) qualityTags.add(AudioQuality.LOSSLESS)
        if (song.hr != null) qualityTags.add(AudioQuality.HIRES)
        if (song.h != null) qualityTags.add(AudioQuality.HIGH)
        if (song.l != null || song.m != null) qualityTags.add(AudioQuality.STANDARD)

        val artists = song.artists
            .map {
                Artist(
                    id = it.id ?: 0L,
                    name = it.name?.takeIf { value -> value.isNotBlank() } ?: "未知歌手",
                    coverUrl = null
                )
            }
            .ifEmpty { listOf(Artist(id = 0L, name = "未知歌手")) }

        val album = Album(
            id = song.album?.id ?: 0L,
            title = song.album?.name?.takeIf { it.isNotBlank() } ?: "未知专辑",
            coverUrl = song.album?.picUrl
        )

        val playableByPrivilege = song.privilege?.let { privilege ->
            val blockedStatus = (privilege.st ?: 0) < 0
            val hasPlayableLevel = (privilege.pl ?: 0L) > 0L
            !blockedStatus && hasPlayableLevel
        }

        val isPlayable = when (playableByPrivilege) {
            true -> true
            false -> false
            null -> song.fee != 4
        }

        return Track(
            id = song.id,
            title = song.name.ifBlank { "未知歌曲" },
            artists = artists,
            album = album,
            qualityTags = qualityTags,
            coverUrl = song.album?.picUrl,
            durationMs = song.dt,
            playableUrl = null,
            isPlayable = isPlayable
        )
    }

    private fun hasLoginCookie(): Boolean {
        val cookie = context.getSharedPreferences(COOKIE_PREF_NAME, Context.MODE_PRIVATE)
            .getString(COOKIE_KEY, null)
        if (!cookie.isNullOrBlank()) return true
        return !context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .getString("cookie", null).isNullOrBlank()
    }

    companion object {
        private const val COOKIE_PREF_NAME = "auth_cookies"
        private const val COOKIE_KEY = "cookie_string"
    }
}
