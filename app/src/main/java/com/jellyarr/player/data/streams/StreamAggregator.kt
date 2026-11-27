package com.jellyarr.player.data.streams

import com.jellyarr.player.core.Format
import com.jellyarr.player.core.str
import com.jellyarr.player.data.SettingsStore
import com.jellyarr.player.data.jellyfin.JellyfinClient
import com.jellyarr.player.data.jellyfin.JfItem
import com.jellyarr.player.data.nuvio.NuvioRepository
import com.jellyarr.player.data.stremio.AddonRepository
import com.jellyarr.player.data.stremio.StremioClient
import com.jellyarr.player.data.tmdb.TmdbClient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

/** Queries Jellyfin, every enabled Stremio addon and every enabled Nuvio scraper in parallel. */
class StreamAggregator(
    private val settings: SettingsStore,
    private val jellyfin: JellyfinClient,
    private val addons: AddonRepository,
    private val nuvio: NuvioRepository,
    private val tmdb: TmdbClient,
) {

    fun load(req: StreamRequest): Flow<StreamsState> = channelFlow {
        val lock = Mutex()
        var state = StreamsState()

        suspend fun publish(transform: (StreamsState) -> StreamsState) {
            lock.withLock {
                state = transform(state)
                send(state)
            }
        }

        suspend fun register(p: ProviderState) = publish { it.copy(providers = it.providers + p) }

        suspend fun finish(key: String, streams: List<StreamSource>, error: String? = null) = publish { s ->
            s.copy(
                providers = s.providers.map {
                    if (it.key == key) it.copy(
                        status = if (error != null) ProviderStatus.ERROR else ProviderStatus.DONE,
                        count = streams.size,
                        error = error,
                    ) else it
                },
                streams = s.streams + streams,
            )
        }

        val st = settings.current()

        // ---- Jellyfin ----
        if (st.jellyfin != null) {
            val key = "jellyfin"
            register(ProviderState(key, "Jellyfin", ProviderKind.JELLYFIN, null, ProviderStatus.LOADING))
            launch {
                try {
                    val item = resolveJellyfinItem(req)
                    val list = item?.let { jellyfinStreams(it) }.orEmpty()
                    finish(key, list)
                } catch (e: Exception) {
                    finish(key, emptyList(), e.message ?: "خطأ")
                }
            }
        }

        // ---- Stremio addons ----
        val stremioId = req.stremioId ?: req.imdbId?.let { imdb ->
            if (req.type == "series" && req.season != null && req.episode != null) "$imdb:${req.season}:${req.episode}" else imdb
        }
        if (stremioId != null) {
            addons.forResource("stream", req.type, stremioId).forEachIndexed { addonIndex, addon ->
                val key = "addon:${addon.transportUrl}"
                register(ProviderState(key, addon.manifest.name, ProviderKind.ADDON, addon.manifest.logo, ProviderStatus.LOADING))
                launch {
                    try {
                        val streams = StremioClient.streams(addon, req.type, stremioId)
                        val mapped = streams.mapIndexed { i, s ->
                            val name = s.name ?: addon.manifest.name
                            val desc = s.description ?: s.title ?: ""
                            StreamSource(
                                id = "$key#$i",
                                provider = addon.manifest.name,
                                providerKind = ProviderKind.ADDON,
                                providerLogo = addon.manifest.logo,
                                name = name,
                                description = desc,
                                url = s.url,
                                headers = s.requestHeaders,
                                infoHash = s.infoHash,
                                externalUrl = s.externalUrl ?: s.ytId?.let { "https://www.youtube.com/watch?v=$it" },
                                filename = s.filename,
                                tags = StreamParser.parse(name, desc, s.filename, sizeHint = s.videoSize),
                                subtitles = s.subtitles,
                                bingeGroup = s.bingeGroup,
                                order = addonIndex * 10_000 + i,
                            )
                        }
                        finish(key, mapped)
                    } catch (e: Exception) {
                        finish(key, emptyList(), e.message ?: "خطأ")
                    }
                }
            }
        }

        // ---- Nuvio scrapers ----
        val scrapers = runCatching { nuvio.activeScrapers(req.type) }.getOrDefault(emptyList())
        if (scrapers.isNotEmpty()) {
            val tmdbId = req.tmdbId ?: req.imdbId?.let { runCatching { tmdb.imdbToTmdb(it, req.type) }.getOrNull() }
            scrapers.forEachIndexed { sIndex, scraper ->
                val key = "nuvio:${scraper.key}"
                register(ProviderState(key, scraper.name, ProviderKind.NUVIO, scraper.logo, ProviderStatus.LOADING))
                if (tmdbId == null) {
                    finish(key, emptyList(), if (tmdb.isConfigured) "لم يُعثر على معرف TMDB" else "أضف مفتاح TMDB من الإعدادات")
                    return@forEachIndexed
                }
                launch {
                    try {
                        val results = nuvio.run(scraper, tmdbId, req.type, req.season, req.episode)
                        finish(key, results.mapIndexedNotNull { i, o -> nuvioStream(o, key, i, scraper.name, scraper.logo, 1_000_000 + sIndex * 10_000 + i) })
                    } catch (e: Exception) {
                        finish(key, emptyList(), e.message ?: "خطأ")
                    }
                }
            }
        }

        if (state.providers.isEmpty()) send(state)
        awaitClose { }
    }

    private fun nuvioStream(o: JSONObject, key: String, i: Int, scraperName: String, logo: String?, order: Int): StreamSource? {
        val url = o.str("url") ?: return null
        val headers = mutableMapOf<String, String>()
        o.optJSONObject("headers")?.let { h -> h.keys().forEach { k -> h.str(k)?.let { headers[k] = it } } }
        val name = o.str("name") ?: scraperName
        val title = o.str("title") ?: ""
        val quality = o.str("quality")
        val size = o.str("size")
        val tags = StreamParser.parse(name, title, quality, size).let { t ->
            if (t.resolution == 0) t.copy(resolution = StreamParser.qualityToResolution(quality)) else t
        }
        val desc = listOfNotNull(title.takeIf { it.isNotBlank() }, quality?.let { "📺 $it" }, size?.let { "💾 $it" }).joinToString("\n")
        return StreamSource(
            id = "$key#$i",
            provider = scraperName,
            providerKind = ProviderKind.NUVIO,
            providerLogo = logo,
            name = name,
            description = desc,
            url = url,
            headers = headers,
            infoHash = null,
            externalUrl = null,
            filename = null,
            tags = tags,
            order = order,
        )
    }

    private suspend fun resolveJellyfinItem(req: StreamRequest): JfItem? {
        req.jellyfinItemId?.let { return jellyfin.item(it) }
        if (req.imdbId == null && req.tmdbId == null) return null
        val found = jellyfin.findByProvider(req.imdbId, req.tmdbId, req.type) ?: return null
        if (req.type != "series") return jellyfin.item(found.id)
        val season = req.season ?: return null
        val episode = req.episode ?: return null
        return coroutineScope {
            val seasonItem = jellyfin.seasons(found.id).firstOrNull { it.indexNumber == season } ?: return@coroutineScope null
            val ep = jellyfin.episodes(found.id, seasonItem.id).firstOrNull { it.indexNumber == episode } ?: return@coroutineScope null
            jellyfin.item(ep.id)
        }
    }

    private fun jellyfinStreams(item: JfItem): List<StreamSource> {
        val sources = item.mediaSources.ifEmpty { return emptyList() }
        return sources.mapIndexed { i, ms ->
            val video = ms.streams.firstOrNull { it.type == "Video" }
            val audio = ms.streams.filter { it.type == "Audio" }
            val subs = ms.streams.count { it.type == "Subtitle" }
            val tags = ParsedTags(
                resolution = video?.height?.let { h -> if (h >= 2000) 2160 else if (h >= 1300) 1440 else if (h >= 1000) 1080 else if (h >= 700) 720 else h } ?: 0,
                hdr = video?.videoRange?.takeIf { !it.equals("SDR", true) }?.let { listOf(it.replace("DOVI", "DV", true)) }.orEmpty(),
                codec = video?.codec?.uppercase()?.let { if (it == "H264") "H.264" else it },
                audio = audio.mapNotNull { it.codec?.uppercase() }.distinct().take(3),
                channels = audio.firstOrNull()?.channels?.let { c -> when (c) { 8 -> "7.1"; 6 -> "5.1"; 2 -> "2.0"; else -> "${c}ch" } },
                sizeBytes = ms.size,
                cached = true,
            )
            val lines = buildList {
                add("📁 ${ms.name ?: item.name}")
                listOfNotNull(ms.container?.uppercase(), Format.bytes(ms.size), ms.bitrate?.let { "${it / 1_000_000} Mbps" })
                    .takeIf { it.isNotEmpty() }?.let { add("💾 " + it.joinToString(" · ")) }
                if (audio.isNotEmpty()) add("🔊 " + audio.take(4).joinToString(" / ") { it.displayTitle ?: it.language ?: it.codec.orEmpty() })
                if (subs > 0) add("💬 $subs ترجمة")
            }
            StreamSource(
                id = "jellyfin#${ms.id}",
                provider = "Jellyfin",
                providerKind = ProviderKind.JELLYFIN,
                providerLogo = null,
                name = "Jellyfin\n${tags.resolutionLabel ?: "Direct"}",
                description = lines.joinToString("\n"),
                url = jellyfin.directStreamUrl(item.id, ms.id),
                headers = emptyMap(),
                infoHash = null,
                externalUrl = null,
                filename = ms.name,
                tags = tags,
                jellyfinItemId = item.id,
                jellyfinMediaSourceId = ms.id,
                order = -1000 + i,
            )
        }
    }
}
