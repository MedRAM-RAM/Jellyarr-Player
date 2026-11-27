package com.jellyarr.player.data.streams

import com.jellyarr.player.data.stremio.StremioSubtitle

enum class ProviderKind { JELLYFIN, ADDON, NUVIO }

/** What the user wants to watch; enough to query every source. */
data class StreamRequest(
    val type: String,               // "movie" | "series"
    val stremioId: String?,         // exact stremio (video) id, e.g. tt123 or tt123:1:2
    val imdbId: String?,
    val tmdbId: String?,
    val season: Int?,
    val episode: Int?,
    val title: String,
    val episodeTitle: String? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val logo: String? = null,
    val jellyfinItemId: String? = null,
    /** Meta id (series id for episodes) used for history / navigation back to details. */
    val metaId: String? = null,
)

data class ParsedTags(
    val resolution: Int = 0,
    val hdr: List<String> = emptyList(),
    val codec: String? = null,
    val source: String? = null,
    val audio: List<String> = emptyList(),
    val channels: String? = null,
    val sizeBytes: Long? = null,
    val seeders: Int? = null,
    val cached: Boolean? = null,
    val debrid: String? = null,
    val languages: List<String> = emptyList(),
    val is3D: Boolean = false,
) {
    val resolutionLabel: String?
        get() = when {
            resolution >= 2160 -> "4K"
            resolution >= 1440 -> "1440p"
            resolution >= 1080 -> "1080p"
            resolution >= 720 -> "720p"
            resolution >= 576 -> "576p"
            resolution > 0 -> "${resolution}p"
            else -> null
        }
}

data class StreamSource(
    val id: String,
    val provider: String,
    val providerKind: ProviderKind,
    val providerLogo: String?,
    val name: String,
    val description: String,
    val url: String?,
    val headers: Map<String, String>,
    val infoHash: String?,
    val externalUrl: String?,
    val filename: String?,
    val tags: ParsedTags,
    val subtitles: List<StremioSubtitle> = emptyList(),
    val bingeGroup: String? = null,
    val jellyfinItemId: String? = null,
    val jellyfinMediaSourceId: String? = null,
    val order: Int = 0,
) {
    val isPlayable: Boolean get() = !url.isNullOrBlank()
    val magnet: String? get() = infoHash?.let { "magnet:?xt=urn:btih:$it" }
}

enum class ProviderStatus { LOADING, DONE, ERROR }

data class ProviderState(
    val key: String,
    val name: String,
    val kind: ProviderKind,
    val logo: String?,
    val status: ProviderStatus,
    val count: Int = 0,
    val error: String? = null,
)

data class StreamsState(
    val providers: List<ProviderState> = emptyList(),
    val streams: List<StreamSource> = emptyList(),
) {
    val loading: Boolean get() = providers.any { it.status == ProviderStatus.LOADING }
}

/** Everything the player needs. */
data class PlaybackRequest(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val title: String,
    val subtitle: String? = null,
    val artwork: String? = null,
    val startPositionMs: Long = 0,
    val fallbackUrl: String? = null,
    val jellyfinItemId: String? = null,
    val jellyfinMediaSourceId: String? = null,
    val externalSubtitles: List<StremioSubtitle> = emptyList(),
    val stremioType: String? = null,
    val stremioId: String? = null,
    val history: com.jellyarr.player.data.HistoryEntry? = null,
    val filename: String? = null,
)
