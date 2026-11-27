package com.jellyarr.player.data.jellyfin

import com.jellyarr.player.BuildConfig
import com.jellyarr.player.core.Http
import com.jellyarr.player.core.mapObjects
import com.jellyarr.player.core.str
import com.jellyarr.player.core.urlEncode
import com.jellyarr.player.data.JellyfinSession
import org.json.JSONArray
import org.json.JSONObject

/** Thin Jellyfin REST client built on OkHttp + org.json. */
class JellyfinClient(private val deviceId: () -> String, private val session: () -> JellyfinSession?) {

    private fun authHeader(token: String?): String {
        val base = "MediaBrowser Client=\"Jellyarr\", Device=\"Android\", DeviceId=\"${deviceId()}\", Version=\"${BuildConfig.VERSION_NAME}\""
        return if (token.isNullOrBlank()) base else "$base, Token=\"$token\""
    }

    private fun headers(token: String? = session()?.token) = mapOf(
        "Authorization" to authHeader(token),
        "X-Emby-Authorization" to authHeader(token),
    )

    private fun requireSession(): JellyfinSession = session() ?: error("لم يتم ربط خادم Jellyfin")

    private fun url(path: String, query: Map<String, Any?> = emptyMap()): String {
        val s = requireSession()
        return buildUrl(s.serverUrl, path, query)
    }

    private suspend fun getObj(path: String, query: Map<String, Any?> = emptyMap()): JSONObject =
        Http.getJson(url(path, query), headers())

    private fun page(o: JSONObject) = JfPage(
        items = o.optJSONArray("Items").mapObjects { JfItem.parse(it) },
        total = o.optInt("TotalRecordCount", 0),
    )

    // ---- auth ----

    suspend fun publicInfo(serverUrl: String): JSONObject =
        Http.getJson(buildUrl(normalizeServer(serverUrl), "System/Info/Public"))

    suspend fun login(serverUrl: String, username: String, password: String): JellyfinSession {
        val server = normalizeServer(serverUrl)
        val info = runCatching { publicInfo(server) }.getOrNull()
        val body = JSONObject().put("Username", username).put("Pw", password).toString()
        val res = JSONObject(Http.send(buildUrl(server, "Users/AuthenticateByName"), "POST", body, headers(null)))
        val user = res.getJSONObject("User")
        return JellyfinSession(
            serverUrl = server,
            serverName = info?.str("ServerName") ?: server,
            userId = user.getString("Id"),
            userName = user.optString("Name", username),
            token = res.getString("AccessToken"),
        )
    }

    suspend fun logout() {
        runCatching { Http.send(url("Sessions/Logout"), "POST", null, headers()) }
    }

    // ---- browsing ----

    suspend fun views(): List<JfItem> {
        val s = requireSession()
        return page(getObj("Users/${s.userId}/Views")).items
    }

    suspend fun resume(limit: Int = 20): List<JfItem> {
        val s = requireSession()
        return page(getObj("Users/${s.userId}/Items/Resume", mapOf(
            "Limit" to limit, "MediaTypes" to "Video", "Fields" to FIELDS, "EnableImageTypes" to IMAGE_TYPES,
        ))).items
    }

    suspend fun nextUp(limit: Int = 20): List<JfItem> {
        val s = requireSession()
        return page(getObj("Shows/NextUp", mapOf(
            "UserId" to s.userId, "Limit" to limit, "Fields" to FIELDS, "EnableImageTypes" to IMAGE_TYPES,
        ))).items
    }

    suspend fun latest(parentId: String?, limit: Int = 20): List<JfItem> {
        val s = requireSession()
        val text = Http.getText(url("Users/${s.userId}/Items/Latest", mapOf(
            "ParentId" to parentId, "Limit" to limit, "Fields" to FIELDS, "EnableImageTypes" to IMAGE_TYPES,
        )), headers())
        return JSONArray(text).mapObjects { JfItem.parse(it) }
    }

    suspend fun items(
        parentId: String? = null,
        includeTypes: String? = null,
        sortBy: String = "SortName",
        sortOrder: String = "Ascending",
        start: Int = 0,
        limit: Int = 60,
        search: String? = null,
        genres: String? = null,
        filters: String? = null,
        recursive: Boolean = true,
    ): JfPage {
        val s = requireSession()
        return page(getObj("Users/${s.userId}/Items", mapOf(
            "ParentId" to parentId,
            "IncludeItemTypes" to includeTypes,
            "Recursive" to recursive,
            "SortBy" to sortBy,
            "SortOrder" to sortOrder,
            "StartIndex" to start,
            "Limit" to limit,
            "SearchTerm" to search,
            "Genres" to genres,
            "Filters" to filters,
            "Fields" to FIELDS,
            "EnableImageTypes" to IMAGE_TYPES,
            "ImageTypeLimit" to 1,
        )))
    }

    suspend fun item(id: String): JfItem {
        val s = requireSession()
        return JfItem.parse(getObj("Users/${s.userId}/Items/$id", mapOf("Fields" to DETAIL_FIELDS)))
    }

    suspend fun seasons(seriesId: String): List<JfItem> {
        val s = requireSession()
        return page(getObj("Shows/$seriesId/Seasons", mapOf("UserId" to s.userId, "Fields" to FIELDS))).items
    }

    suspend fun episodes(seriesId: String, seasonId: String): List<JfItem> {
        val s = requireSession()
        return page(getObj("Shows/$seriesId/Episodes", mapOf(
            "UserId" to s.userId, "SeasonId" to seasonId, "Fields" to "$FIELDS,MediaSources",
        ))).items
    }

    suspend fun similar(id: String, limit: Int = 16): List<JfItem> {
        val s = requireSession()
        return page(getObj("Items/$id/Similar", mapOf("UserId" to s.userId, "Limit" to limit, "Fields" to FIELDS))).items
    }

    suspend fun search(term: String, limit: Int = 40): List<JfItem> =
        items(search = term, includeTypes = "Movie,Series,Episode", sortBy = "SortName", limit = limit).items

    /** Finds a library item matching a provider id (imdb / tmdb). */
    suspend fun findByProvider(imdb: String?, tmdb: String?, type: String): JfItem? {
        val s = requireSession()
        val any = listOfNotNull(imdb?.let { "imdb.$it" }, tmdb?.let { "tmdb.$it" })
        if (any.isEmpty()) return null
        val res = page(getObj("Users/${s.userId}/Items", mapOf(
            "AnyProviderIdEquals" to any.joinToString(","),
            "IncludeItemTypes" to if (type == "series") "Series" else "Movie",
            "Recursive" to true,
            "Limit" to 1,
            "Fields" to FIELDS,
        )))
        return res.items.firstOrNull()
    }

    suspend fun setFavorite(id: String, favorite: Boolean) {
        val s = requireSession()
        Http.send(url("Users/${s.userId}/FavoriteItems/$id"), if (favorite) "POST" else "DELETE", null, headers())
    }

    suspend fun setPlayed(id: String, played: Boolean) {
        val s = requireSession()
        Http.send(url("Users/${s.userId}/PlayedItems/$id"), if (played) "POST" else "DELETE", null, headers())
    }

    // ---- playback ----

    suspend fun playbackInfo(itemId: String, maxBitrate: Long?): JSONObject {
        val s = requireSession()
        val body = JSONObject().apply {
            put("UserId", s.userId)
            put("AutoOpenLiveStream", true)
            put("EnableDirectPlay", true)
            put("EnableDirectStream", true)
            put("EnableTranscoding", true)
            maxBitrate?.let { put("MaxStreamingBitrate", it) }
        }
        return JSONObject(Http.send(url("Items/$itemId/PlaybackInfo", mapOf("UserId" to s.userId)), "POST", body.toString(), headers()))
    }

    fun directStreamUrl(itemId: String, mediaSourceId: String?): String {
        val s = requireSession()
        return buildUrl(s.serverUrl, "Videos/$itemId/stream", mapOf(
            "static" to true,
            "MediaSourceId" to (mediaSourceId ?: itemId),
            "DeviceId" to deviceId(),
            "api_key" to s.token,
        ))
    }

    fun hlsTranscodeUrl(itemId: String, mediaSourceId: String?, maxBitrate: Long?, audioIndex: Int? = null): String {
        val s = requireSession()
        return buildUrl(s.serverUrl, "Videos/$itemId/master.m3u8", mapOf(
            "MediaSourceId" to (mediaSourceId ?: itemId),
            "DeviceId" to deviceId(),
            "api_key" to s.token,
            "VideoCodec" to "h264,hevc",
            "AudioCodec" to "aac,mp3,ac3,eac3",
            "TranscodingMaxAudioChannels" to 6,
            "VideoBitrate" to (maxBitrate ?: 20_000_000L),
            "AudioBitrate" to 384_000,
            "SegmentContainer" to "ts",
            "MinSegments" to 1,
            "BreakOnNonKeyFrames" to true,
            "AudioStreamIndex" to audioIndex,
            "PlaySessionId" to System.currentTimeMillis().toString(),
        ))
    }

    fun subtitleUrl(itemId: String, mediaSourceId: String, index: Int, format: String = "vtt"): String {
        val s = requireSession()
        return buildUrl(s.serverUrl, "Videos/$itemId/$mediaSourceId/Subtitles/$index/0/Stream.$format", mapOf("api_key" to s.token))
    }

    suspend fun reportStart(itemId: String, mediaSourceId: String?, positionMs: Long, playMethod: String) =
        report("Sessions/Playing", itemId, mediaSourceId, positionMs, false, playMethod)

    suspend fun reportProgress(itemId: String, mediaSourceId: String?, positionMs: Long, paused: Boolean, playMethod: String) =
        report("Sessions/Playing/Progress", itemId, mediaSourceId, positionMs, paused, playMethod)

    suspend fun reportStopped(itemId: String, mediaSourceId: String?, positionMs: Long) =
        report("Sessions/Playing/Stopped", itemId, mediaSourceId, positionMs, false, null)

    private suspend fun report(path: String, itemId: String, mediaSourceId: String?, positionMs: Long, paused: Boolean, method: String?) {
        if (session() == null) return
        val body = JSONObject().apply {
            put("ItemId", itemId)
            mediaSourceId?.let { put("MediaSourceId", it) }
            put("PositionTicks", positionMs * 10_000L)
            put("IsPaused", paused)
            put("CanSeek", true)
            method?.let { put("PlayMethod", it) }
        }
        runCatching { Http.send(url(path), "POST", body.toString(), headers()) }
    }

    // ---- images ----

    fun imageUrl(itemId: String, type: String = "Primary", tag: String? = null, maxWidth: Int = 480, index: Int? = null): String? {
        val s = session() ?: return null
        val path = if (index != null) "Items/$itemId/Images/$type/$index" else "Items/$itemId/Images/$type"
        return buildUrl(s.serverUrl, path, mapOf("tag" to tag, "maxWidth" to maxWidth, "quality" to 90))
    }

    fun posterUrl(item: JfItem, maxWidth: Int = 400): String? = when {
        item.primaryTag != null -> imageUrl(item.id, "Primary", item.primaryTag, maxWidth)
        item.type == "Episode" && item.seriesId != null -> imageUrl(item.seriesId, "Primary", item.seriesPrimaryTag, maxWidth)
        item.seriesId != null -> imageUrl(item.seriesId, "Primary", item.seriesPrimaryTag, maxWidth)
        else -> null
    }

    /** Series poster for episodes, item poster otherwise. */
    fun seriesPosterUrl(item: JfItem, maxWidth: Int = 400): String? =
        if (item.type == "Episode" && item.seriesId != null) imageUrl(item.seriesId, "Primary", item.seriesPrimaryTag, maxWidth)
        else posterUrl(item, maxWidth)

    fun backdropUrl(item: JfItem, maxWidth: Int = 1280): String? = when {
        item.backdropTags.isNotEmpty() -> imageUrl(item.id, "Backdrop", item.backdropTags.first(), maxWidth, 0)
        item.parentBackdropItemId != null && item.parentBackdropTags.isNotEmpty() ->
            imageUrl(item.parentBackdropItemId, "Backdrop", item.parentBackdropTags.first(), maxWidth, 0)
        item.seriesId != null -> imageUrl(item.seriesId, "Backdrop", null, maxWidth, 0)
        else -> null
    }

    fun thumbUrl(item: JfItem, maxWidth: Int = 640): String? = when {
        item.type == "Episode" && item.primaryTag != null -> imageUrl(item.id, "Primary", item.primaryTag, maxWidth)
        item.thumbTag != null -> imageUrl(item.id, "Thumb", item.thumbTag, maxWidth)
        else -> backdropUrl(item, maxWidth) ?: posterUrl(item, maxWidth)
    }

    fun logoUrl(item: JfItem, maxWidth: Int = 600): String? = when {
        item.logoTag != null -> imageUrl(item.parentLogoItemId ?: item.id, "Logo", item.logoTag, maxWidth)
        else -> null
    }

    fun personImageUrl(p: JfPerson, maxWidth: Int = 200): String? =
        p.imageTag?.let { imageUrl(p.id, "Primary", it, maxWidth) }

    companion object {
        const val FIELDS = "PrimaryImageAspectRatio,Overview,Genres,ProviderIds,ParentId,ChildCount"
        const val DETAIL_FIELDS = "Overview,Genres,Studios,People,ProviderIds,MediaSources,MediaStreams,Taglines,ChildCount,ParentId"
        const val IMAGE_TYPES = "Primary,Backdrop,Thumb,Logo"

        fun normalizeServer(input: String): String {
            var s = input.trim().trimEnd('/')
            if (!s.startsWith("http://", true) && !s.startsWith("https://", true)) s = "http://$s"
            return s
        }

        fun buildUrl(server: String, path: String, query: Map<String, Any?> = emptyMap()): String {
            val q = query.entries.filter { it.value != null }
                .joinToString("&") { "${it.key}=${it.value.toString().urlEncode()}" }
            val base = "${server.trimEnd('/')}/${path.trimStart('/')}"
            return if (q.isEmpty()) base else "$base?$q"
        }
    }
}
