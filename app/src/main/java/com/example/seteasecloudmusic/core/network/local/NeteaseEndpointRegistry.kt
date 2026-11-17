package com.example.seteasecloudmusic.core.network.local

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Implements all 43 Netease Cloud Music API endpoints used across the app's Retrofit services,
 * matching the request mapping and response normalization of `@neteasecloudmusicapienhanced/api`.
 */
internal class NeteaseEndpointRegistry(
    private val client: LocalNeteaseClient,
    private val clockMs: () -> Long = System::currentTimeMillis
) {

    fun isSupported(path: String): Boolean = normalizePath(path) in HANDLERS

    fun dispatch(
        path: String,
        queryParams: Map<String, String>,
        rawCookie: String?
    ): LocalApiResult? {
        val handler = HANDLERS[normalizePath(path)] ?: return null
        return handler(this, queryParams, rawCookie)
    }

    private fun handleAlbum(q: Map<String, String>, cookie: String?): LocalApiResult {
        val id = q["id"].orEmpty()
        return client.request(
            upstreamPath = "/api/v1/album/$id",
            data = JsonObject(),
            queryParams = q,
            rawCookie = cookie,
            defaultCrypto = NeteaseCryptoMode.WEAPI
        )
    }

    private fun handleArtistAlbum(q: Map<String, String>, cookie: String?): LocalApiResult {
        val id = q["id"].orEmpty()
        val data = JsonObject().apply {
            addProperty("limit", q.intOrDefault("limit", 30))
            addProperty("offset", q.intOrDefault("offset", 0))
            addProperty("total", true)
        }
        return client.request("/api/artist/albums/$id", data, q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handleArtistDesc(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply { addProperty("id", q["id"].orEmpty()) }
        return client.request("/api/artist/introduction", data, q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handleArtistDetail(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply { addProperty("id", q["id"].orEmpty()) }
        return client.request("/api/artist/head/info/get", data, q, cookie, NeteaseCryptoMode.EAPI)
    }

    private fun handleArtistNewSong(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply {
            addProperty("limit", q.intOrDefault("limit", 20))
            addProperty("startTimestamp", q.longOrDefault("before", clockMs()))
        }
        return client.request("/api/sub/artist/new/works/song/list", data, q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handleArtistSongs(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply {
            addProperty("id", q["id"].orEmpty())
            addProperty("private_cloud", "true")
            addProperty("work_type", 1)
            addProperty("order", q["order"]?.ifBlank { null } ?: "hot")
            addProperty("offset", q.intOrDefault("offset", 0))
            addProperty("limit", q.intOrDefault("limit", 100))
        }
        return client.request("/api/v1/artist/songs", data, q, cookie, NeteaseCryptoMode.EAPI)
    }

    private fun handleArtistSublist(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply {
            addProperty("limit", q.intOrDefault("limit", 25))
            addProperty("offset", q.intOrDefault("offset", 0))
            addProperty("total", true)
        }
        return client.request("/api/artist/sublist", data, q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handleArtistTopSong(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply { addProperty("id", q["id"].orEmpty()) }
        return client.request("/api/artist/top/song", data, q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handleCaptchaSent(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply {
            addProperty("ctcode", q["ctcode"]?.ifBlank { null } ?: "86")
            addProperty("secrete", "music_middleuser_pclogin")
            addProperty("cellphone", q["phone"].orEmpty())
        }
        return client.request("/api/sms/captcha/sent", data, q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handleCloudsearch(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply {
            addProperty("s", q["keywords"].orEmpty())
            addProperty("type", q.intOrDefault("type", 1))
            addProperty("limit", q.intOrDefault("limit", 30))
            addProperty("offset", q.intOrDefault("offset", 0))
            addProperty("total", true)
        }
        return client.request("/api/cloudsearch/pc", data, q, cookie, NeteaseCryptoMode.EAPI)
    }

    private fun handleHomepageBlockPage(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply {
            addProperty("refresh", q.boolOrDefault("refresh", false))
            q["cursor"]?.let { addProperty("cursor", it) }
        }
        return client.request("/api/homepage/block/page", data, q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handleLike(q: Map<String, String>, cookie: String?): LocalApiResult {
        val like = q["like"] != "false"
        val data = JsonObject().apply {
            addProperty("alg", "itembased")
            addProperty("trackId", q["id"].orEmpty())
            addProperty("like", like)
            addProperty("time", "3")
        }
        return client.request("/api/radio/like", data, q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handleLikelist(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply { addProperty("uid", q["uid"].orEmpty()) }
        return client.request("/api/song/like/get", data, q, cookie, NeteaseCryptoMode.EAPI)
    }

    private fun handleLoginCellphone(q: Map<String, String>, cookie: String?): LocalApiResult {
        val captcha = q["captcha"]?.takeIf { it.isNotBlank() }
        val passwordHash = q["md5_password"]?.takeIf { it.isNotBlank() }
            ?: q["password"]?.let { NeteaseCrypto.md5Hex(it) }.orEmpty()

        val data = JsonObject().apply {
            addProperty("type", "1")
            addProperty("https", "true")
            addProperty("phone", q["phone"].orEmpty())
            addProperty("countrycode", q["countrycode"]?.ifBlank { null } ?: "86")
            if (captcha != null) {
                addProperty("captcha", captcha)
            } else {
                addProperty("password", passwordHash)
            }
            addProperty("remember", "true")
        }

        val result = client.request("/api/w/login/cellphone", data, q, cookie, NeteaseCryptoMode.WEAPI)
        val bodyObj = parseJsonObjectOrNull(result.bodyJson) ?: return result
        val code = bodyObj.get("code")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt
        if (code == 200) {
            val normalizedText = bodyObj.toString().replace("avatarImgId_str", "avatarImgIdStr")
            val finalObj = parseJsonObjectOrNull(normalizedText) ?: JsonObject()
            finalObj.addProperty("cookie", result.setCookies.joinToString(";"))
            return LocalApiResult(
                httpStatus = 200,
                bodyJson = finalObj.toString(),
                setCookies = result.setCookies
            )
        }
        return result
    }

    private fun handleLoginQrCheck(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply {
            addProperty("key", q["key"].orEmpty())
            addProperty("type", 3)
        }
        return try {
            val result = client.request("/api/login/qrcode/client/login", data, q, cookie, NeteaseCryptoMode.EAPI)
            val bodyObj = parseJsonObjectOrNull(result.bodyJson) ?: JsonObject()
            bodyObj.addProperty("cookie", result.setCookies.joinToString(";"))
            LocalApiResult(
                httpStatus = 200,
                bodyJson = bodyObj.toString(),
                setCookies = result.setCookies
            )
        } catch (_: Exception) {
            LocalApiResult(httpStatus = 200, bodyJson = "{}")
        }
    }

    private fun handleLoginQrCreate(q: Map<String, String>, cookie: String?): LocalApiResult {
        val key = q["key"].orEmpty()
        val platform = q["platform"]?.ifBlank { null } ?: "pc"
        val url = buildString {
            append("https://music.163.com/login?codekey=")
            append(key)
            if (platform == "web") {
                append("&chainId=")
                append(NeteaseRequestContextBuilder.generateChainId(cookie, clockMs()))
            }
        }
        val includeImg = q["qrimg"]?.let { it.isNotBlank() && it != "0" && !it.equals("false", ignoreCase = true) } ?: false
        val qrImg = if (includeImg) LocalQrCodeGenerator.toDataUrl(url) else ""
        val dataObj = JsonObject().apply {
            addProperty("qrurl", url)
            addProperty("qrimg", qrImg)
        }
        val root = JsonObject().apply {
            addProperty("code", 200)
            add("data", dataObj)
        }
        return LocalApiResult(httpStatus = 200, bodyJson = root.toString())
    }

    private fun handleLoginQrKey(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply { addProperty("type", 3) }
        val result = client.request("/api/login/qrcode/unikey", data, q, cookie, NeteaseCryptoMode.EAPI)
        val inner = parseJsonObjectOrNull(result.bodyJson) ?: JsonObject()
        val wrapped = JsonObject().apply {
            add("data", inner)
            addProperty("code", 200)
        }
        return LocalApiResult(
            httpStatus = 200,
            bodyJson = wrapped.toString(),
            setCookies = result.setCookies
        )
    }

    private fun handleLogout(q: Map<String, String>, cookie: String?): LocalApiResult {
        return client.request("/api/logout", JsonObject(), q, cookie, NeteaseCryptoMode.EAPI)
    }

    private fun handleLyric(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply {
            addProperty("id", q["id"].orEmpty())
            addProperty("tv", -1)
            addProperty("lv", -1)
            addProperty("rv", -1)
            addProperty("kv", -1)
            addProperty("_nmclfl", 1)
        }
        return client.request("/api/song/lyric", data, q, cookie, NeteaseCryptoMode.EAPI)
    }

    private fun handleLyricNew(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply {
            addProperty("id", q["id"].orEmpty())
            addProperty("cp", false)
            addProperty("tv", 0)
            addProperty("lv", 0)
            addProperty("rv", 0)
            addProperty("kv", 0)
            addProperty("yv", 0)
            addProperty("ytv", 0)
            addProperty("yrv", 0)
        }
        return client.request("/api/song/lyric/v1", data, q, cookie, NeteaseCryptoMode.EAPI)
    }

    private fun handlePersonalized(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply {
            addProperty("limit", q.intOrDefault("limit", 30))
            addProperty("total", true)
            addProperty("n", 1000)
        }
        return client.request("/api/personalized/playlist", data, q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handlePersonalizedNewsong(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply {
            addProperty("type", "recommend")
            addProperty("limit", q.intOrDefault("limit", 10))
            addProperty("areaId", q.intOrDefault("areaId", 0))
        }
        return client.request("/api/personalized/newsong", data, q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handlePlaylistCreate(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply {
            addProperty("name", q["name"].orEmpty())
            addProperty("privacy", q["privacy"]?.ifBlank { null } ?: "0")
            addProperty("type", q["type"]?.ifBlank { null } ?: "NORMAL")
        }
        return client.request("/api/playlist/create", data, q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handlePlaylistDetail(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply {
            addProperty("id", q["id"].orEmpty())
            addProperty("n", 100000)
            addProperty("s", q.intOrDefault("s", 8))
        }
        return client.request("/api/v6/playlist/detail", data, q, cookie, NeteaseCryptoMode.EAPI)
    }

    private fun handlePlaylistTracks(q: Map<String, String>, cookie: String?): LocalApiResult {
        val rawTracks = q["tracks"].orEmpty()
        val trackList = if (rawTracks.isEmpty()) listOf("") else rawTracks.split(",")
        val trackArray = JsonArray().apply { trackList.forEach(::add) }

        val data = JsonObject().apply {
            addProperty("op", q["op"].orEmpty())
            addProperty("pid", q["pid"].orEmpty())
            addProperty("trackIds", trackArray.toString())
            addProperty("imme", "true")
        }

        val firstRes = client.request("/api/playlist/manipulate/tracks", data, q, cookie, NeteaseCryptoMode.EAPI)
        val firstObj = parseJsonObjectOrNull(firstRes.bodyJson)
        val firstCode = firstObj?.get("code")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt

        if (firstRes.httpStatus == 200) {
            val wrapped = JsonObject().apply {
                addProperty("status", 200)
                add("body", firstObj ?: JsonObject())
            }
            return LocalApiResult(200, wrapped.toString(), firstRes.setCookies)
        }

        if (firstCode == 512) {
            val doubledArray = JsonArray().apply {
                trackList.forEach(::add)
                trackList.forEach(::add)
            }
            val retryData = JsonObject().apply {
                addProperty("op", q["op"].orEmpty())
                addProperty("pid", q["pid"].orEmpty())
                addProperty("trackIds", doubledArray.toString())
                addProperty("imme", "true")
            }
            return client.request("/api/playlist/manipulate/tracks", retryData, q, cookie, NeteaseCryptoMode.EAPI)
        }

        val fallback = JsonObject().apply {
            addProperty("status", 200)
            add("body", firstObj ?: JsonObject())
        }
        return LocalApiResult(200, fallback.toString(), firstRes.setCookies)
    }

    private fun handlePlaymodeIntelligenceList(q: Map<String, String>, cookie: String?): LocalApiResult {
        val id = q["id"].orEmpty()
        val data = JsonObject().apply {
            addProperty("songId", id)
            addProperty("type", "fromPlayOne")
            addProperty("playlistId", q["pid"].orEmpty())
            addProperty("startMusicId", q["sid"]?.ifBlank { null } ?: id)
            addProperty("count", q.intOrDefault("count", 1))
        }
        return client.request("/api/playmode/intelligence/list", data, q, cookie, NeteaseCryptoMode.EAPI)
    }

    private fun handleRecommendResource(q: Map<String, String>, cookie: String?): LocalApiResult {
        return client.request("/api/v1/discovery/recommend/resource", JsonObject(), q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handleRecommendSongs(q: Map<String, String>, cookie: String?): LocalApiResult {
        return client.request("/api/v3/discovery/recommend/songs", JsonObject(), q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handleScrobble(q: Map<String, String>, cookie: String?): LocalApiResult {
        val logJson = JsonObject().apply {
            addProperty("download", 0)
            addProperty("end", "playend")
            q["id"]?.let { addProperty("id", it.toLongOrNull() ?: 0L) }
            q["sourceid"]?.takeIf { it.isNotBlank() }?.let { sid ->
                addProperty("sourceId", sid.toLongOrNull() ?: 0L)
            }
            addProperty("time", q.intOrDefault("time", 0))
            addProperty("type", "song")
            addProperty("wifi", 0)
            addProperty("source", "list")
            addProperty("mainsite", 1)
            addProperty("content", "")
        }
        val logEntry = JsonObject().apply {
            addProperty("action", "play")
            add("json", logJson)
        }
        val logsArray = JsonArray().apply { add(logEntry) }
        val data = JsonObject().apply { addProperty("logs", logsArray.toString()) }
        return client.request("/api/feedback/weblog", data, q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handleSearchHot(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply { addProperty("type", 1111) }
        return client.request("/api/search/hot", data, q, cookie, NeteaseCryptoMode.EAPI)
    }

    private fun handleSearchHotDetail(q: Map<String, String>, cookie: String?): LocalApiResult {
        return client.request("/api/hotsearchlist/get", JsonObject(), q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handleSearchSuggest(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply { addProperty("s", q["keywords"].orEmpty()) }
        val typeSegment = if (q["type"] == "mobile") "keyword" else "web"
        return client.request("/api/search/suggest/$typeSegment", data, q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handleSimiArtist(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply { addProperty("artistid", q["id"].orEmpty()) }
        return client.request("/api/discovery/simiArtist", data, q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handleSimiSong(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply {
            addProperty("songid", q["id"].orEmpty())
            addProperty("limit", q.intOrDefault("limit", 50))
            addProperty("offset", q.intOrDefault("offset", 0))
        }
        return client.request("/api/v1/discovery/simiSong", data, q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handleSongOrderUpdate(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply {
            addProperty("pid", q["pid"].orEmpty())
            addProperty("trackIds", q["ids"].orEmpty())
            addProperty("op", "update")
        }
        return client.request("/api/playlist/manipulate/tracks", data, q, cookie, NeteaseCryptoMode.EAPI)
    }

    private fun handleSongUrl(q: Map<String, String>, cookie: String?): LocalApiResult {
        val ids = q["id"].orEmpty().split(",")
        val idsJson = JsonArray().apply { ids.forEach(::add) }.toString()
        val data = JsonObject().apply {
            addProperty("ids", idsJson)
            addProperty("br", q.intOrDefault("br", 999000))
        }
        val res = client.request("/api/song/enhance/player/url", data, q, cookie, NeteaseCryptoMode.EAPI)
        val bodyObj = parseJsonObjectOrNull(res.bodyJson) ?: return res
        val rawArray = bodyObj.getAsJsonArray("data") ?: return res
        val sortedItems = rawArray.sortedWith(compareBy { el ->
            val idStr = el.takeIf { it.isJsonObject }?.asJsonObject?.get("id")?.asString ?: ""
            val idx = ids.indexOf(idStr)
            if (idx >= 0) idx else Int.MAX_VALUE
        })
        val sortedArray = JsonArray().apply { sortedItems.forEach(::add) }
        val out = JsonObject().apply {
            addProperty("code", 200)
            add("data", sortedArray)
        }
        return LocalApiResult(httpStatus = 200, bodyJson = out.toString(), setCookies = res.setCookies)
    }

    private fun handleSongUrlV1(q: Map<String, String>, cookie: String?): LocalApiResult {
        val level = q["level"].orEmpty()
        val data = JsonObject().apply {
            addProperty("ids", "[${q["id"].orEmpty()}]")
            addProperty("level", level)
            addProperty("encodeType", "flac")
            if (level == "sky") {
                addProperty("immerseType", "c51")
            }
        }
        return client.request("/api/song/enhance/player/url/v1", data, q, cookie, NeteaseCryptoMode.EAPI)
    }

    private fun handleTopArtists(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply {
            addProperty("limit", q.intOrDefault("limit", 50))
            addProperty("offset", q.intOrDefault("offset", 0))
            addProperty("total", true)
        }
        return client.request("/api/artist/top", data, q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handleTopPlaylist(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply {
            addProperty("cat", q["cat"]?.ifBlank { null } ?: "全部")
            addProperty("order", q["order"]?.ifBlank { null } ?: "hot")
            addProperty("limit", q.intOrDefault("limit", 50))
            addProperty("offset", q.intOrDefault("offset", 0))
            addProperty("total", true)
        }
        val res = client.request("/api/playlist/list", data, q, cookie, NeteaseCryptoMode.WEAPI)
        val replaced = res.bodyJson.replace("avatarImgId_str", "avatarImgIdStr")
        return res.copy(bodyJson = replaced)
    }

    private fun handleTopSong(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply {
            addProperty("areaId", q.intOrDefault("type", 0))
            addProperty("total", true)
        }
        return client.request("/api/v1/discovery/new/songs", data, q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handleToplistDetail(q: Map<String, String>, cookie: String?): LocalApiResult {
        return client.request("/api/toplist/detail", JsonObject(), q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handleUserAccount(q: Map<String, String>, cookie: String?): LocalApiResult {
        return client.request("/api/nuser/account/get", JsonObject(), q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun handleUserPlaylist(q: Map<String, String>, cookie: String?): LocalApiResult {
        val data = JsonObject().apply {
            addProperty("uid", q["uid"].orEmpty())
            addProperty("limit", q.intOrDefault("limit", 30))
            addProperty("offset", q.intOrDefault("offset", 0))
            addProperty("includeVideo", true)
        }
        return client.request("/api/user/playlist", data, q, cookie, NeteaseCryptoMode.WEAPI)
    }

    private fun parseJsonObjectOrNull(raw: String): JsonObject? = runCatching {
        val el: JsonElement = JsonParser.parseString(raw)
        if (el.isJsonObject) el.asJsonObject else null
    }.getOrNull()

    private fun Map<String, String>.intOrDefault(key: String, default: Int): Int =
        this[key]?.trim()?.toIntOrNull() ?: default

    private fun Map<String, String>.longOrDefault(key: String, default: Long): Long =
        this[key]?.trim()?.toLongOrNull() ?: default

    private fun Map<String, String>.boolOrDefault(key: String, default: Boolean): Boolean =
        when (this[key]?.trim()?.lowercase()) {
            "true", "1" -> true
            "false", "0" -> false
            else -> default
        }

    companion object {
        internal val HANDLERS: Map<String, (NeteaseEndpointRegistry, Map<String, String>, String?) -> LocalApiResult> =
            mapOf(
                "/album" to NeteaseEndpointRegistry::handleAlbum,
                "/artist/album" to NeteaseEndpointRegistry::handleArtistAlbum,
                "/artist/desc" to NeteaseEndpointRegistry::handleArtistDesc,
                "/artist/detail" to NeteaseEndpointRegistry::handleArtistDetail,
                "/artist/new/song" to NeteaseEndpointRegistry::handleArtistNewSong,
                "/artist/songs" to NeteaseEndpointRegistry::handleArtistSongs,
                "/artist/sublist" to NeteaseEndpointRegistry::handleArtistSublist,
                "/artist/top/song" to NeteaseEndpointRegistry::handleArtistTopSong,
                "/captcha/sent" to NeteaseEndpointRegistry::handleCaptchaSent,
                "/cloudsearch" to NeteaseEndpointRegistry::handleCloudsearch,
                "/homepage/block/page" to NeteaseEndpointRegistry::handleHomepageBlockPage,
                "/like" to NeteaseEndpointRegistry::handleLike,
                "/likelist" to NeteaseEndpointRegistry::handleLikelist,
                "/login/cellphone" to NeteaseEndpointRegistry::handleLoginCellphone,
                "/login/qr/check" to NeteaseEndpointRegistry::handleLoginQrCheck,
                "/login/qr/create" to NeteaseEndpointRegistry::handleLoginQrCreate,
                "/login/qr/key" to NeteaseEndpointRegistry::handleLoginQrKey,
                "/logout" to NeteaseEndpointRegistry::handleLogout,
                "/lyric" to NeteaseEndpointRegistry::handleLyric,
                "/lyric/new" to NeteaseEndpointRegistry::handleLyricNew,
                "/personalized" to NeteaseEndpointRegistry::handlePersonalized,
                "/personalized/newsong" to NeteaseEndpointRegistry::handlePersonalizedNewsong,
                "/playlist/create" to NeteaseEndpointRegistry::handlePlaylistCreate,
                "/playlist/detail" to NeteaseEndpointRegistry::handlePlaylistDetail,
                "/playlist/tracks" to NeteaseEndpointRegistry::handlePlaylistTracks,
                "/playmode/intelligence/list" to NeteaseEndpointRegistry::handlePlaymodeIntelligenceList,
                "/recommend/resource" to NeteaseEndpointRegistry::handleRecommendResource,
                "/recommend/songs" to NeteaseEndpointRegistry::handleRecommendSongs,
                "/scrobble" to NeteaseEndpointRegistry::handleScrobble,
                "/search/hot" to NeteaseEndpointRegistry::handleSearchHot,
                "/search/hot/detail" to NeteaseEndpointRegistry::handleSearchHotDetail,
                "/search/suggest" to NeteaseEndpointRegistry::handleSearchSuggest,
                "/simi/artist" to NeteaseEndpointRegistry::handleSimiArtist,
                "/simi/song" to NeteaseEndpointRegistry::handleSimiSong,
                "/song/order/update" to NeteaseEndpointRegistry::handleSongOrderUpdate,
                "/song/url" to NeteaseEndpointRegistry::handleSongUrl,
                "/song/url/v1" to NeteaseEndpointRegistry::handleSongUrlV1,
                "/top/artists" to NeteaseEndpointRegistry::handleTopArtists,
                "/top/playlist" to NeteaseEndpointRegistry::handleTopPlaylist,
                "/top/song" to NeteaseEndpointRegistry::handleTopSong,
                "/toplist/detail" to NeteaseEndpointRegistry::handleToplistDetail,
                "/user/account" to NeteaseEndpointRegistry::handleUserAccount,
                "/user/playlist" to NeteaseEndpointRegistry::handleUserPlaylist
            )

        internal fun normalizePath(rawPath: String): String {
            val trimmed = rawPath.trim()
            if (trimmed.isEmpty() || trimmed == "/") return "/"
            val withLeading = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
            return withLeading.trimEnd('/')
        }
    }
}
