package com.h1son5633.coda.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class Song(
    val id: Long,
    val name: String,
    val artists: String,
    val album: String,
    val picUrl: String,
    val durationMs: Long,
    val fee: Int
) {
    /** NetEase image CDN sizing, same trick the original uses everywhere. */
    fun cover(size: Int = 400): String =
        if (picUrl.isBlank()) picUrl else "$picUrl?param=${size}y${size}"
}

data class LyricRaw(val lrc: String, val tlyric: String, val yrc: String)

data class AccountProfile(val userId: Long, val nickname: String, val avatarUrl: String)

data class Playlist(
    val id: Long,
    val name: String,
    val coverUrl: String,
    val playCount: Long,
    val trackCount: Int,
    val creatorName: String,
    val description: String = "",
    val specialType: Int = 0,
    val userId: Long = 0,
    val updateFrequency: String = ""
) {
    fun cover(size: Int = 400): String =
        if (coverUrl.isBlank()) coverUrl else "$coverUrl?param=${size}y${size}"
    val isLiked: Boolean get() = specialType == 5
}

data class Album(
    val id: Long,
    val name: String,
    val picUrl: String,
    val artistName: String,
    val publishTime: Long = 0,
    val size: Int = 0
) {
    fun cover(size: Int = 400): String =
        if (picUrl.isBlank()) picUrl else "$picUrl?param=${size}y${size}"
}

data class Artist(
    val id: Long,
    val name: String,
    val picUrl: String,
    val albumSize: Int = 0,
    val musicSize: Int = 0
) {
    fun cover(size: Int = 400): String =
        if (picUrl.isBlank()) picUrl else "$picUrl?param=${size}y${size}"
}

/**
 * Direct client for music.163.com — no relay server. Talks eapi for the app
 * catalogue (search / play URL / lyrics) and weapi for QR login, exactly the
 * split the open-source NetEase ecosystem uses.
 *
 * [cookieProvider] supplies the persisted login cookies (empty when logged
 * out); [cookieSink] receives fresh cookies from the QR-login success
 * response so the caller can persist them.
 */
class NeteaseApi(
    private val deviceId: String,
    private val cookieProvider: () -> Map<String, String>,
    private val cookieSink: (Map<String, String>) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun musicU(): String = cookieProvider()["MUSIC_U"] ?: ""

    private fun cookieHeader(): String {
        val base = linkedMapOf(
            "os" to "android",
            "appver" to "9.0.65",
            "osver" to "14",
            "deviceId" to deviceId,
            "__remember_me" to "true"
        )
        base.putAll(cookieProvider())
        return base.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    private fun eapiHeader(): JSONObject = JSONObject().apply {
        put("os", "android")
        put("appver", "9.0.65")
        put("deviceId", deviceId)
        put("osver", "14")
        put("brand", "OnePlus")
        put("model", "PJZ110")
        put("requestId", (10000000..99999999).random().toString())
        put("MUSIC_U", musicU())
    }

    suspend fun eapi(apiPath: String, data: JSONObject): JSONObject =
        withContext(Dispatchers.IO) {
            data.put("header", eapiHeader())
            val params = NeteaseCrypto.eapiParams(apiPath, data.toString())
            val url = "https://music.163.com/eapi/" + apiPath.removePrefix("/api/")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 NeteaseMusic/9.0.65")
                .header("Cookie", cookieHeader())
                .post(FormBody.Builder().add("params", params).build())
                .build()
            client.newCall(request).execute().use { response ->
                JSONObject(response.body?.string() ?: "{}")
            }
        }

    suspend fun weapi(path: String, data: JSONObject): Pair<JSONObject, Map<String, String>> =
        withContext(Dispatchers.IO) {
            if (!data.has("csrf_token")) {
                data.put("csrf_token", cookieProvider()["__csrf"] ?: "")
            }
            val (params, encSecKey) = NeteaseCrypto.weapiParams(data.toString())
            val request = Request.Builder()
                .url("https://music.163.com/weapi/$path")
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"
                )
                .header("Referer", "https://music.163.com/")
                .header("Cookie", cookieHeader())
                .post(
                    FormBody.Builder()
                        .add("params", params)
                        .add("encSecKey", encSecKey)
                        .build()
                )
                .build()
            client.newCall(request).execute().use { response ->
                val cookies = mutableMapOf<String, String>()
                for (header in response.headers("Set-Cookie")) {
                    val pair = header.substringBefore(';')
                    val idx = pair.indexOf('=')
                    if (idx > 0) cookies[pair.substring(0, idx).trim()] = pair.substring(idx + 1).trim()
                }
                JSONObject(response.body?.string() ?: "{}") to cookies
            }
        }

    // ---- Catalogue -------------------------------------------------------

    private fun parseSong(s: JSONObject): Song {
        val artists = s.optJSONArray("ar")?.let { ar ->
            (0 until ar.length()).joinToString("/") { ar.getJSONObject(it).optString("name") }
        } ?: ""
        val al = s.optJSONObject("al")
        return Song(
            id = s.optLong("id"),
            name = s.optString("name"),
            artists = artists,
            album = al?.optString("name") ?: "",
            picUrl = al?.optString("picUrl") ?: "",
            durationMs = s.optLong("dt"),
            fee = s.optInt("fee")
        )
    }

    private fun parsePlaylist(p: JSONObject): Playlist = Playlist(
        id = p.optLong("id"),
        name = p.optString("name"),
        coverUrl = p.optString("coverImgUrl").ifBlank { p.optString("picUrl") },
        playCount = p.optLong("playCount"),
        trackCount = p.optInt("trackCount"),
        creatorName = p.optJSONObject("creator")?.optString("nickname") ?: "",
        description = p.optString("description"),
        specialType = p.optInt("specialType"),
        userId = p.optLong("userId"),
        updateFrequency = p.optString("updateFrequency")
    )

    private suspend fun cloudsearch(
        keywords: String, type: Int, limit: Int, offset: Int
    ): JSONObject {
        val res = eapi(
            "/api/cloudsearch/pc",
            JSONObject().apply {
                put("s", keywords)
                put("type", type)
                put("limit", limit)
                put("offset", offset)
                put("total", true)
            }
        )
        return res.optJSONObject("result") ?: JSONObject()
    }

    suspend fun searchSongs(keywords: String, limit: Int = 30, offset: Int = 0): List<Song> {
        val songs = cloudsearch(keywords, 1, limit, offset).optJSONArray("songs")
            ?: return emptyList()
        return (0 until songs.length()).map { parseSong(songs.getJSONObject(it)) }
    }

    suspend fun searchArtists(keywords: String, limit: Int = 20): List<Artist> {
        val arr = cloudsearch(keywords, 100, limit, 0).optJSONArray("artists")
            ?: return emptyList()
        return (0 until arr.length()).map {
            val a = arr.getJSONObject(it)
            Artist(
                id = a.optLong("id"),
                name = a.optString("name"),
                picUrl = a.optString("picUrl").ifBlank { a.optString("img1v1Url") },
                albumSize = a.optInt("albumSize"),
                musicSize = a.optInt("musicSize")
            )
        }
    }

    suspend fun searchAlbums(keywords: String, limit: Int = 20): List<Album> {
        val arr = cloudsearch(keywords, 10, limit, 0).optJSONArray("albums")
            ?: return emptyList()
        return (0 until arr.length()).map {
            val a = arr.getJSONObject(it)
            Album(
                id = a.optLong("id"),
                name = a.optString("name"),
                picUrl = a.optString("picUrl"),
                artistName = a.optJSONObject("artist")?.optString("name") ?: "",
                publishTime = a.optLong("publishTime"),
                size = a.optInt("size")
            )
        }
    }

    suspend fun searchPlaylists(keywords: String, limit: Int = 20): List<Playlist> {
        val arr = cloudsearch(keywords, 1000, limit, 0).optJSONArray("playlists")
            ?: return emptyList()
        return (0 until arr.length()).map { parsePlaylist(arr.getJSONObject(it)) }
    }

    // ---- Discovery / detail ----------------------------------------------

    suspend fun personalizedPlaylists(limit: Int = 12): List<Playlist> {
        val res = eapi(
            "/api/personalized/playlist",
            JSONObject().apply {
                put("limit", limit)
                put("total", true)
                put("n", limit)
            }
        )
        val arr = res.optJSONArray("result") ?: return emptyList()
        return (0 until arr.length()).map { parsePlaylist(arr.getJSONObject(it)) }
    }

    /** Returns the playlist header plus its embedded tracks. */
    suspend fun playlistDetail(id: Long): Pair<Playlist, List<Song>>? {
        val res = eapi(
            "/api/v6/playlist/detail",
            JSONObject().apply {
                put("id", id)
                put("n", 1000)
                put("s", 8)
            }
        )
        val pl = res.optJSONObject("playlist") ?: return null
        val tracks = pl.optJSONArray("tracks") ?: return parsePlaylist(pl) to emptyList()
        return parsePlaylist(pl) to (0 until tracks.length()).map {
            parseSong(tracks.getJSONObject(it))
        }
    }

    suspend fun toplists(): List<Playlist> {
        val res = eapi("/api/toplist", JSONObject().put("n", 100))
        val arr = res.optJSONArray("list") ?: return emptyList()
        return (0 until arr.length()).map { parsePlaylist(arr.getJSONObject(it)) }
    }

    suspend fun newAlbums(limit: Int = 12): List<Album> {
        val res = eapi(
            "/api/album/new",
            JSONObject().apply {
                put("limit", limit)
                put("offset", 0)
                put("total", true)
                put("area", "ALL")
            }
        )
        val arr = res.optJSONArray("albums") ?: return emptyList()
        return (0 until arr.length()).map {
            val a = arr.getJSONObject(it)
            Album(
                id = a.optLong("id"),
                name = a.optString("name"),
                picUrl = a.optString("picUrl"),
                artistName = a.optJSONObject("artist")?.optString("name") ?: "",
                publishTime = a.optLong("publishTime"),
                size = a.optInt("size")
            )
        }
    }

    suspend fun albumDetail(id: Long): Pair<Album, List<Song>>? {
        val res = eapi("/api/v1/album/$id", JSONObject())
        val a = res.optJSONObject("album") ?: return null
        val album = Album(
            id = a.optLong("id"),
            name = a.optString("name"),
            picUrl = a.optString("picUrl"),
            artistName = a.optJSONObject("artist")?.optString("name") ?: "",
            publishTime = a.optLong("publishTime"),
            size = a.optInt("size")
        )
        val songs = res.optJSONArray("songs") ?: return album to emptyList()
        return album to (0 until songs.length()).map { parseSong(songs.getJSONObject(it)) }
    }

    /** Hot songs for an artist page. */
    suspend fun artistHotSongs(id: Long): Pair<Artist, List<Song>>? {
        val res = eapi("/api/v1/artist/$id", JSONObject())
        val a = res.optJSONObject("artist") ?: return null
        val artist = Artist(
            id = a.optLong("id"),
            name = a.optString("name"),
            picUrl = a.optString("picUrl").ifBlank { a.optString("img1v1Url") },
            albumSize = a.optInt("albumSize"),
            musicSize = a.optInt("musicSize")
        )
        val songs = res.optJSONArray("hotSongs") ?: return artist to emptyList()
        return artist to (0 until songs.length()).map { parseSong(songs.getJSONObject(it)) }
    }

    // ---- Logged-in --------------------------------------------------------

    suspend fun dailySongs(): List<Song> {
        val res = eapi("/api/v3/discovery/recommend/songs", JSONObject())
        val arr = res.optJSONObject("data")?.optJSONArray("dailySongs") ?: return emptyList()
        return (0 until arr.length()).map { parseSong(arr.getJSONObject(it)) }
    }

    suspend fun userPlaylists(uid: Long): List<Playlist> {
        val res = eapi(
            "/api/user/playlist",
            JSONObject().apply {
                put("uid", uid)
                put("limit", 100)
                put("offset", 0)
                put("includeVideo", true)
            }
        )
        val arr = res.optJSONArray("playlist") ?: return emptyList()
        return (0 until arr.length()).map { parsePlaylist(arr.getJSONObject(it)) }
    }

    /** Play URL for one song, or null when NetEase gates it (login/VIP/copyright). */
    suspend fun songUrl(id: Long, level: String = "exhigh"): String? {
        val res = eapi(
            "/api/song/enhance/player/url/v1",
            JSONObject().apply {
                put("ids", "[$id]")
                put("level", level)
                put("encodeType", "flac")
            }
        )
        val first = res.optJSONArray("data")?.optJSONObject(0) ?: return null
        val url = first.optString("url")
        return url.takeIf { it.isNotBlank() && first.optInt("code") == 200 }
    }

    suspend fun lyric(id: Long): LyricRaw {
        val res = eapi(
            "/api/song/lyric/v1",
            JSONObject().apply {
                put("id", id)
                put("cp", false)
                put("lv", 0)
                put("kv", 0)
                put("tv", 0)
                put("rv", 0)
                put("yv", 0)
                put("ytv", 0)
                put("yrv", 0)
            }
        )
        return LyricRaw(
            lrc = res.optJSONObject("lrc")?.optString("lyric") ?: "",
            tlyric = res.optJSONObject("tlyric")?.optString("lyric") ?: "",
            yrc = res.optJSONObject("yrc")?.optString("lyric") ?: ""
        )
    }

    /** Hot search keywords for the Explore page's empty state. */
    suspend fun hotKeywords(): List<String> {
        val res = eapi("/api/search/hot/detail", JSONObject())
        val arr = res.optJSONArray("data") ?: return emptyList()
        return (0 until arr.length()).map { arr.getJSONObject(it).optString("searchWord") }
            .filter { it.isNotBlank() }
    }

    // ---- QR login --------------------------------------------------------

    suspend fun qrUnikey(): String? {
        val (res, _) = weapi("login/qrcode/unikey", JSONObject().put("type", 1))
        return res.optString("unikey").takeIf { it.isNotBlank() }
    }

    /**
     * Poll QR login state. Codes: 800 expired, 801 waiting, 802 scanned
     * (awaiting confirm), 803 success — on 803 the login cookies are handed
     * to the cookie sink.
     */
    suspend fun qrPoll(unikey: String): Int {
        val (res, cookies) = weapi(
            "login/qrcode/client/login",
            JSONObject().apply {
                put("key", unikey)
                put("type", 1)
            }
        )
        val code = res.optInt("code")
        if (code == 803 && cookies.isNotEmpty()) cookieSink(cookies)
        return code
    }

    suspend fun accountProfile(): AccountProfile? {
        if (musicU().isBlank()) return null
        val res = eapi("/api/nuser/account/get", JSONObject())
        val profile = res.optJSONObject("profile") ?: return null
        return AccountProfile(
            userId = profile.optLong("userId"),
            nickname = profile.optString("nickname"),
            avatarUrl = profile.optString("avatarUrl")
        )
    }
}
