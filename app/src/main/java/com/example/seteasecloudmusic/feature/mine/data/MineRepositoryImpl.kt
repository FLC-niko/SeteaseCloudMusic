package com.example.seteasecloudmusic.feature.mine.data

import com.example.seteasecloudmusic.core.cache.DataCacheManager
import com.example.seteasecloudmusic.core.model.Album
import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.mine.domain.model.UserPlaylist
import com.example.seteasecloudmusic.feature.mine.domain.model.UserPlaylistsGroup
import com.example.seteasecloudmusic.feature.mine.domain.repository.MineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MineRepositoryImpl @Inject constructor(
    private val mineService: MineService,
    private val dataCacheManager: DataCacheManager
) : MineRepository {

    override fun getCachedUserPlaylists(userId: Long): UserPlaylistsGroup? {
        return dataCacheManager.getUserPlaylists(userId)
    }

    override fun getCachedPlaylistDetailPreview(playlistId: Long): PlaylistDetail? {
        return dataCacheManager.getPlaylistDetailPreview(playlistId)
    }

    override suspend fun getUserPlaylists(userId: Long): Result<UserPlaylistsGroup> = withContext(Dispatchers.IO) {
        runCatching {
            val response = mineService.getUserPlaylists(uid = userId)
            if ((response.code ?: 0) != 200) {
                // 异常时若有缓存则优先返回缓存
                val cached = dataCacheManager.getUserPlaylists(userId)
                if (cached != null) return@runCatching cached
                throw Exception("获取用户歌单失败: code=${response.code}")
            }

            val allPlaylists = response.playlist ?: emptyList()

            // 识别"我喜欢的音乐"：specialType == 5 或名字含"我喜欢"或首个自建歌单
            val likedItem = allPlaylists.firstOrNull { (it.specialType ?: 0) == 5 }
                ?: allPlaylists.firstOrNull { it.subscribed != true && it.name?.contains("我喜欢") == true }
                ?: allPlaylists.firstOrNull { it.subscribed != true }

            val likedPlaylist = likedItem?.let {
                UserPlaylist(
                    id = it.id ?: 0L,
                    name = it.name?.takeIf { n -> n.isNotBlank() } ?: "我喜欢的音乐",
                    coverUrl = it.coverImgUrl,
                    trackCount = it.trackCount ?: 0,
                    playCount = it.playCount ?: 0L,
                    isLikedHero = true,
                    creatorName = it.creator?.nickname,
                    description = it.description
                )
            }

            val createdPlaylists = allPlaylists
                .filter { it.subscribed != true && it.id != likedItem?.id }
                .map {
                    UserPlaylist(
                        id = it.id ?: 0L,
                        name = it.name ?: "未知歌单",
                        coverUrl = it.coverImgUrl,
                        trackCount = it.trackCount ?: 0,
                        playCount = it.playCount ?: 0L,
                        isLikedHero = false,
                        creatorName = it.creator?.nickname,
                        description = it.description
                    )
                }

            val favoritedPlaylists = allPlaylists
                .filter { it.subscribed == true }
                .map {
                    UserPlaylist(
                        id = it.id ?: 0L,
                        name = it.name ?: "未知歌单",
                        coverUrl = it.coverImgUrl,
                        trackCount = it.trackCount ?: 0,
                        playCount = it.playCount ?: 0L,
                        isLikedHero = false,
                        creatorName = it.creator?.nickname,
                        description = it.description
                    )
                }

            val group = UserPlaylistsGroup(
                likedPlaylist = likedPlaylist,
                createdPlaylists = createdPlaylists,
                favoritedPlaylists = favoritedPlaylists
            )

            // 持久化到本地缓存
            dataCacheManager.saveUserPlaylists(userId, group)

            group
        }.recoverCatching { err ->
            val cached = dataCacheManager.getUserPlaylists(userId)
            cached ?: throw err
        }
    }

    override suspend fun getPlaylistDetail(playlistId: Long): Result<PlaylistDetail> = withContext(Dispatchers.IO) {
        runCatching {
            val response = mineService.getPlaylistDetail(id = playlistId)
            if ((response.code ?: 0) != 200) {
                val cached = dataCacheManager.getPlaylistDetailPreview(playlistId)
                if (cached != null) return@runCatching cached
                throw Exception("获取歌单详情失败: code=${response.code}")
            }

            val pl = response.playlist ?: throw Exception("歌单详情为空")

            val tracks = (pl.tracks ?: emptyList()).map { track ->
                Track(
                    id = track.id ?: 0L,
                    title = track.name?.takeIf { it.isNotBlank() } ?: "未知歌曲",
                    artists = (track.ar ?: emptyList()).map { ar ->
                        Artist(
                            id = ar.id ?: 0L,
                            name = ar.name?.takeIf { it.isNotBlank() } ?: "未知歌手",
                            coverUrl = null
                        )
                    },
                    album = Album(
                        id = track.al?.id ?: 0L,
                        title = track.al?.name?.takeIf { it.isNotBlank() } ?: "未知专辑",
                        coverUrl = track.al?.picUrl
                    ),
                    coverUrl = track.al?.picUrl,
                    durationMs = track.dt ?: 0L,
                    playableUrl = null,
                    isPlayable = (track.fee ?: 0) != 4
                )
            }

            val detail = PlaylistDetail(
                id = pl.id ?: 0L,
                name = pl.name ?: "歌单详情",
                coverUrl = pl.coverImgUrl,
                description = pl.description,
                trackCount = pl.trackCount ?: tracks.size,
                playCount = pl.playCount ?: 0L,
                creatorName = pl.creator?.nickname,
                tracks = tracks
            )

            // 轻量持久化歌单首屏曲目（前20首）
            dataCacheManager.savePlaylistDetailPreview(detail)

            detail
        }.recoverCatching { err ->
            val cached = dataCacheManager.getPlaylistDetailPreview(playlistId)
            cached ?: throw err
        }
    }
}
