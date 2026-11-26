package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.SettingsRepository
import com.example.data.model.AuthenticateRequest
import com.example.data.model.BaseItemDto
import com.example.data.model.GenreDto
import com.example.data.model.MediaSourceInfo
import com.example.data.model.PlaybackInfoRequest
import com.example.data.model.PlaybackMode
import com.example.data.model.PlaybackProgressRequest
import com.example.data.model.PlaybackStartRequest
import com.example.data.model.PlaybackStopRequest
import com.example.data.model.PublicSystemInfo
import com.example.data.model.SavedServer
import com.example.data.model.SortOption
import com.example.data.model.StreamQuality
import com.example.data.model.UserSettings
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.UUID

data class HomeDashboardData(
    val featuredItems: List<BaseItemDto> = emptyList(),
    val resumeItems: List<BaseItemDto> = emptyList(),
    val nextUpItems: List<BaseItemDto> = emptyList(),
    val libraries: List<BaseItemDto> = emptyList(),
    val latestMovies: List<BaseItemDto> = emptyList(),
    val latestSeries: List<BaseItemDto> = emptyList()
)

data class MediaDetailBundle(
    val item: BaseItemDto,
    val seasons: List<BaseItemDto> = emptyList(),
    val firstSeasonEpisodes: List<BaseItemDto> = emptyList(),
    val similarItems: List<BaseItemDto> = emptyList()
)

data class PlaybackSession(
    val item: BaseItemDto,
    val streamUrl: String,
    val playMethod: String,
    val mediaSourceId: String,
    val playSessionId: String?,
    val resumePositionMs: Long,
    val isHls: Boolean,
    val nextEpisode: BaseItemDto? = null,
    val previousEpisode: BaseItemDto? = null
)

class JellyfinRepository(
    private val context: Context,
    private val clientProvider: JellyfinClientProvider,
    private val settingsRepository: SettingsRepository
) {
    private val api: JellyfinApi get() = clientProvider.api

    fun normalizeServerUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isBlank()) return ""
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "http://$trimmed"
        }
    }

    fun syncSessionConfig(settings: UserSettings, deviceId: String) {
        clientProvider.sessionConfig.deviceId.set(deviceId)
        clientProvider.sessionConfig.timeoutSeconds.set(settings.timeoutSeconds)
        clientProvider.sessionConfig.serverUrl.set(settings.activeServer?.url ?: "")
        clientProvider.sessionConfig.accessToken.set(settings.activeServer?.accessToken ?: "")
    }

    suspend fun testServerConnection(rawUrl: String): Result<PublicSystemInfo> = runCatching {
        val baseUrl = normalizeServerUrl(rawUrl)
        require(baseUrl.isNotBlank())
        val deviceId = settingsRepository.getOrCreateDeviceId()
        clientProvider.sessionConfig.deviceId.set(deviceId)
        api.getPublicSystemInfo("$baseUrl/System/Info/Public")
    }

    suspend fun signIn(rawUrl: String, username: String, password: String): Result<SavedServer> = runCatching {
        val baseUrl = normalizeServerUrl(rawUrl)
        require(baseUrl.isNotBlank())
        val deviceId = settingsRepository.getOrCreateDeviceId()
        clientProvider.sessionConfig.deviceId.set(deviceId)
        clientProvider.sessionConfig.serverUrl.set(baseUrl)
        clientProvider.sessionConfig.accessToken.set("")

        val info = runCatching { api.getPublicSystemInfo("$baseUrl/System/Info/Public") }.getOrNull()
        val auth = api.authenticateByName(
            fullUrl = "$baseUrl/Users/AuthenticateByName",
            request = AuthenticateRequest(username = username.trim(), pw = password)
        )

        val server = SavedServer(
            id = "${info?.id ?: UUID.randomUUID()}-${auth.user.id}",
            name = info?.serverName ?: baseUrl.removePrefix("http://").removePrefix("https://"),
            url = baseUrl,
            username = auth.user.name,
            userId = auth.user.id,
            accessToken = auth.accessToken,
            serverVersion = info?.version ?: ""
        )
        clientProvider.sessionConfig.accessToken.set(server.accessToken)
        settingsRepository.saveActiveServer(server)
        server
    }

    suspend fun switchServer(server: SavedServer) {
        clientProvider.sessionConfig.serverUrl.set(server.url)
        clientProvider.sessionConfig.accessToken.set(server.accessToken)
        settingsRepository.setActiveServerId(server.id)
    }

    suspend fun signOut() {
        clientProvider.sessionConfig.accessToken.set("")
        settingsRepository.setActiveServerId(null)
    }

    suspend fun getHomeDashboard(userId: String): Result<HomeDashboardData> = runCatching {
        coroutineScope {
            val viewsDeferred = async { runCatching { api.getUserViews(userId).items }.getOrDefault(emptyList()) }
            val resumeDeferred = async { runCatching { api.getResumeItems(userId, limit = 15).items }.getOrDefault(emptyList()) }
            val nextUpDeferred = async { runCatching { api.getNextUpEpisodes(userId, limit = 15).items }.getOrDefault(emptyList()) }
            val latestMoviesDeferred = async {
                runCatching { api.getLatestItems(userId, limit = 16, includeItemTypes = "Movie") }.getOrDefault(emptyList())
            }
            val latestSeriesDeferred = async {
                runCatching { api.getLatestItems(userId, limit = 16, includeItemTypes = "Series") }.getOrDefault(emptyList())
            }

            val views = viewsDeferred.await().filter {
                it.collectionType in listOf("movies", "tvshows", null, "")
            }
            val latestMovies = latestMoviesDeferred.await()
            val latestSeries = latestSeriesDeferred.await()
            val featured = (latestMovies.take(4) + latestSeries.take(3))
                .filter { !it.backdropImageTags.isNullOrEmpty() || !it.imageTags.isNullOrEmpty() }
                .ifEmpty { latestMovies + latestSeries }
                .take(6)

            HomeDashboardData(
                featuredItems = featured,
                resumeItems = resumeDeferred.await(),
                nextUpItems = nextUpDeferred.await(),
                libraries = views,
                latestMovies = latestMovies,
                latestSeries = latestSeries
            )
        }
    }

    suspend fun getLibraryItems(
        userId: String,
        itemTypes: String,
        parentId: String? = null,
        sortOption: SortOption = SortOption.LATEST,
        unwatchedOnly: Boolean = false,
        favoritesOnly: Boolean = false,
        genre: String? = null,
        startIndex: Int = 0,
        limit: Int = 60
    ): Result<Pair<List<BaseItemDto>, Int>> = runCatching {
        val filters = buildList {
            if (unwatchedOnly) add("IsUnplayed")
            if (favoritesOnly) add("IsFavorite")
        }.takeIf { it.isNotEmpty() }?.joinToString(",")

        val resp = api.getItems(
            userId = userId,
            includeItemTypes = itemTypes,
            parentId = parentId,
            sortBy = sortOption.sortBy,
            sortOrder = sortOption.sortOrder,
            filters = filters,
            genres = genre,
            startIndex = startIndex,
            limit = limit
        )
        resp.items to resp.totalRecordCount
    }

    suspend fun getGenres(userId: String, itemTypes: String): Result<List<GenreDto>> = runCatching {
        api.getGenres(userId = userId, includeItemTypes = itemTypes).items
    }

    suspend fun searchItems(
        userId: String,
        query: String,
        includeItemTypes: String = "Movie,Series,Episode"
    ): Result<List<BaseItemDto>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()
        api.getItems(
            userId = userId,
            includeItemTypes = includeItemTypes,
            searchTerm = query.trim(),
            limit = 45
        ).items
    }

    suspend fun getMediaDetail(userId: String, itemId: String): Result<MediaDetailBundle> = runCatching {
        coroutineScope {
            val itemDeferred = async { api.getItemDetails(userId, itemId) }
            val similarDeferred = async {
                runCatching { api.getSimilarItems(itemId, userId, limit = 12).items }.getOrDefault(emptyList())
            }

            val item = itemDeferred.await()
            var seasons: List<BaseItemDto> = emptyList()
            var episodes: List<BaseItemDto> = emptyList()

            if (item.type == "Series") {
                seasons = runCatching { api.getSeasons(item.id, userId).items }.getOrDefault(emptyList())
                val firstSeason = seasons.firstOrNull()
                if (firstSeason != null) {
                    episodes = runCatching { api.getEpisodes(item.id, firstSeason.id, userId).items }.getOrDefault(emptyList())
                }
            }

            MediaDetailBundle(
                item = item,
                seasons = seasons,
                firstSeasonEpisodes = episodes,
                similarItems = similarDeferred.await()
            )
        }
    }

    suspend fun getSeasonEpisodes(userId: String, seriesId: String, seasonId: String): Result<List<BaseItemDto>> = runCatching {
        api.getEpisodes(seriesId = seriesId, seasonId = seasonId, userId = userId).items
    }

    suspend fun toggleFavorite(userId: String, itemId: String, currentlyFavorite: Boolean): Result<Boolean> = runCatching {
        val result = if (currentlyFavorite) {
            api.unmarkFavorite(userId, itemId)
        } else {
            api.markFavorite(userId, itemId)
        }
        result.isFavorite
    }

    suspend fun togglePlayed(userId: String, itemId: String, currentlyPlayed: Boolean): Result<Boolean> = runCatching {
        val result = if (currentlyPlayed) {
            api.unmarkPlayed(userId, itemId)
        } else {
            api.markPlayed(userId, itemId)
        }
        result.played
    }

    suspend fun buildPlaybackSession(
        userId: String,
        server: SavedServer,
        itemId: String,
        settings: UserSettings,
        startFromBeginning: Boolean = false
    ): Result<PlaybackSession> = runCatching {
        val item = api.getItemDetails(userId, itemId)
        val isWifi = isWifiConnected()
        val targetQuality = if (isWifi) settings.wifiQuality else settings.mobileQuality
        val maxBitrate = targetQuality.bitrateBps

        val playbackInfo = runCatching {
            api.getPlaybackInfo(
                itemId = item.id,
                userId = userId,
                request = PlaybackInfoRequest(
                    userId = userId,
                    maxStreamingBitrate = maxBitrate
                )
            )
        }.getOrNull()

        val source: MediaSourceInfo? = playbackInfo?.mediaSources?.firstOrNull() ?: item.mediaSources?.firstOrNull()
        val mediaSourceId = source?.id?.takeIf { it.isNotBlank() } ?: item.id
        val baseUrl = server.url.trimEnd('/')
        val token = server.accessToken
        val deviceId = settingsRepository.getOrCreateDeviceId()

        val shouldTranscodeHls = when (settings.playbackMode) {
            PlaybackMode.DIRECT_PLAY -> false
            PlaybackMode.FORCE_HLS -> true
            PlaybackMode.AUTO_TRANSCODE -> targetQuality != StreamQuality.ORIGINAL
        }

        val (streamUrl, playMethod, isHls) = if (!shouldTranscodeHls) {
            val url = "$baseUrl/Videos/${item.id}/stream?Static=true&MediaSourceId=$mediaSourceId&DeviceId=$deviceId&api_key=$token"
            Triple(url, "DirectPlay", false)
        } else {
            val bitrateParam = maxBitrate ?: 20_000_000L
            val videoCodec = settings.preferredCodec.codecValue
            val url = "$baseUrl/Videos/${item.id}/master.m3u8?MediaSourceId=$mediaSourceId&DeviceId=$deviceId&VideoCodec=$videoCodec&AudioCodec=aac,mp3&MaxStreamingBitrate=$bitrateParam&TranscodingMaxAudioChannels=2&SegmentContainer=ts&api_key=$token"
            Triple(url, "Transcode", true)
        }

        // Lookup adjacent episodes if playing a TV episode
        var nextEpisode: BaseItemDto? = null
        var prevEpisode: BaseItemDto? = null
        if (item.type == "Episode" && !item.seriesId.isNullOrBlank() && !item.seasonId.isNullOrBlank()) {
            val seasonEpisodes = runCatching {
                api.getEpisodes(item.seriesId, item.seasonId, userId).items
            }.getOrDefault(emptyList())
            val idx = seasonEpisodes.indexOfFirst { it.id == item.id }
            if (idx >= 0) {
                if (idx > 0) prevEpisode = seasonEpisodes[idx - 1]
                if (idx + 1 < seasonEpisodes.size) nextEpisode = seasonEpisodes[idx + 1]
            }
        }

        val resumeMs = if (startFromBeginning) 0L else (item.userData?.playbackPositionTicks ?: 0L) / 10_000L

        PlaybackSession(
            item = item,
            streamUrl = streamUrl,
            playMethod = playMethod,
            mediaSourceId = mediaSourceId,
            playSessionId = playbackInfo?.playSessionId,
            resumePositionMs = resumeMs,
            isHls = isHls,
            nextEpisode = nextEpisode,
            previousEpisode = prevEpisode
        )
    }

    suspend fun reportPlaybackStart(session: PlaybackSession, positionMs: Long) {
        runCatching {
            api.reportPlaybackStart(
                PlaybackStartRequest(
                    itemId = session.item.id,
                    mediaSourceId = session.mediaSourceId,
                    playSessionId = session.playSessionId,
                    positionTicks = positionMs * 10_000L,
                    playMethod = session.playMethod
                )
            )
        }
    }

    suspend fun reportPlaybackProgress(session: PlaybackSession, positionMs: Long, isPaused: Boolean) {
        runCatching {
            api.reportPlaybackProgress(
                PlaybackProgressRequest(
                    itemId = session.item.id,
                    mediaSourceId = session.mediaSourceId,
                    playSessionId = session.playSessionId,
                    positionTicks = positionMs * 10_000L,
                    isPaused = isPaused,
                    playMethod = session.playMethod
                )
            )
        }
    }

    suspend fun reportPlaybackStopped(session: PlaybackSession, positionMs: Long) {
        runCatching {
            api.reportPlaybackStopped(
                PlaybackStopRequest(
                    itemId = session.item.id,
                    mediaSourceId = session.mediaSourceId,
                    playSessionId = session.playSessionId,
                    positionTicks = positionMs * 10_000L
                )
            )
        }
    }

    private fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return true
        val caps = cm.getNetworkCapabilities(network) ?: return true
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
