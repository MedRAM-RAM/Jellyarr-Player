package com.jellyarr.player.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jellyarr.player.JellyarrApp
import com.jellyarr.player.data.jellyfin.JfItem
import com.jellyarr.player.data.stremio.BuiltIn
import com.jellyarr.player.data.stremio.MetaItem
import com.jellyarr.player.data.stremio.MetaVideo
import com.jellyarr.player.data.stremio.StremioClient
import com.jellyarr.player.data.streams.StreamRequest
import com.jellyarr.player.ui.LocalNavigator
import com.jellyarr.player.ui.Target
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MetaDetailsState(
    val loading: Boolean = true,
    val error: String? = null,
    val meta: MetaItem? = null,
    val addonName: String? = null,
    val seasons: List<SeasonModel> = emptyList(),
    val selectedSeason: String? = null,
    val inLibrary: JfItem? = null,
)

class MetaDetailsViewModel : ViewModel() {
    private val c = JellyarrApp.container
    private val _state = MutableStateFlow(MetaDetailsState())
    val state = _state.asStateFlow()
    private var key: String? = null
    private var type = ""
    private var id = ""
    private var addonUrl: String? = null

    fun bind(type: String, id: String, addonUrl: String?) {
        val k = "$type|$id"
        if (k == key) return
        key = k; this.type = type; this.id = id; this.addonUrl = addonUrl
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val all = c.addons.forResource("meta", type, id)
            val preferred = all.sortedBy { if (it.transportUrl == addonUrl) 0 else 1 }.toMutableList()
            if (id.startsWith("tt") && preferred.none { it.manifest.id == BuiltIn.cinemeta.manifest.id }) preferred += BuiltIn.cinemeta
            var lastError: String? = null
            for (addon in preferred) {
                val meta = runCatching { StremioClient.meta(addon, type, id) }
                    .onFailure { lastError = it.message }.getOrNull() ?: continue
                applyMeta(meta, addon.manifest.name)
                return@launch
            }
            _state.update { it.copy(loading = false, error = lastError ?: "لا توجد إضافة توفّر بيانات لهذا العنصر") }
        }
    }

    private fun applyMeta(meta: MetaItem, addonName: String) {
        val history = c.history.find(type, meta.id)
        val bySeason = meta.videos.groupBy { it.season ?: 1 }
            .toSortedMap(compareBy<Int> { if (it == 0) Int.MAX_VALUE else it })
        val seasons = bySeason.map { (num, vids) ->
            SeasonModel(
                key = "s$num",
                number = num,
                name = if (num == 0) "خاصة" else "الموسم $num",
                episodes = vids.sortedBy { it.episode ?: 0 }.map { v -> episodeModel(v, history?.takeIf { h -> h.videoId == v.id }) },
            )
        }
        val selected = history?.season?.let { "s$it" }?.takeIf { k -> seasons.any { it.key == k } }
            ?: seasons.firstOrNull { it.number != 0 }?.key ?: seasons.firstOrNull()?.key
        _state.update { it.copy(loading = false, meta = meta, addonName = addonName, seasons = seasons, selectedSeason = selected) }
        if (c.settings.state.value.jellyfin != null && meta.imdbId != null) {
            viewModelScope.launch {
                val found = runCatching { c.jellyfin.findByProvider(meta.imdbId, null, type) }.getOrNull()
                _state.update { it.copy(inLibrary = found) }
            }
        }
    }

    private fun episodeModel(v: MetaVideo, h: com.jellyarr.player.data.HistoryEntry?) = EpisodeModel(
        key = v.id.ifBlank { "${v.season}:${v.episode}" },
        number = v.episode,
        season = v.season,
        title = v.title.ifBlank { "الحلقة ${v.episode ?: ""}" },
        overview = v.overview,
        thumb = v.thumbnail,
        released = v.released,
        runtime = null,
        progress = h?.progress?.takeIf { it > 0f },
        played = h != null && h.progress > 0.92f,
        stremioId = v.id,
    )

    fun selectSeason(key: String) = _state.update { it.copy(selectedSeason = key) }

    fun requestFor(ep: EpisodeModel?): StreamRequest? {
        val meta = _state.value.meta ?: return null
        val tmdb = meta.id.removePrefix("tmdb:").takeIf { meta.id.startsWith("tmdb:") }
        return StreamRequest(
            type = type,
            stremioId = ep?.stremioId ?: meta.id,
            imdbId = meta.imdbId,
            tmdbId = tmdb,
            season = ep?.season,
            episode = ep?.number,
            title = meta.name,
            episodeTitle = ep?.title,
            poster = meta.poster,
            backdrop = meta.background ?: ep?.thumb,
            logo = meta.logo,
            metaId = meta.id,
        )
    }

    /** Resume where the user left off, otherwise first episode of the selected season. */
    fun primaryEpisode(): EpisodeModel? {
        val s = _state.value
        val history = s.meta?.let { c.history.find(type, it.id) }
        s.seasons.flatMap { it.episodes.orEmpty() }.firstOrNull { it.stremioId == history?.videoId }?.let { return it }
        return s.seasons.firstOrNull { it.key == s.selectedSeason }?.episodes?.firstOrNull()
    }
}

@Composable
fun MetaDetailsScreen(type: String, id: String, addonUrl: String?, vm: MetaDetailsViewModel = viewModel()) {
    LaunchedEffect(type, id) { vm.bind(type, id, addonUrl) }
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    val meta = state.meta
    val isSeries = meta != null && meta.videos.isNotEmpty() && (type == "series" || meta.videos.size > 1 || meta.videos.any { it.season != null })

    val model = meta?.let { m ->
        DetailsModel(
            title = m.name,
            logo = m.logo,
            backdrop = m.background,
            poster = m.poster,
            year = m.releaseInfo,
            runtime = m.runtime,
            rating = m.imdbRating,
            officialRating = null,
            genres = m.genres,
            overview = m.description,
            tagline = null,
            people = (m.cast.map { PersonModel(it, null, null) } + m.director.map { PersonModel(it, "مخرج", null) }),
            isSeries = isSeries,
            sourceLabel = state.addonName ?: "Stremio",
            badges = listOfNotNull(state.inLibrary?.let { "في مكتبة Jellyfin" }),
            secondaryActionLabel = "Jellyfin",
            primaryActionLabel = if (isSeries) (vm.primaryEpisode()?.let { e -> "شاهد ${e.season?.let { "م$it" } ?: ""} ح${e.number ?: ""}".trim() } ?: "شاهد") else "شاهد",
        )
    }

    DetailsLayout(
        model = model,
        loading = state.loading,
        error = state.error,
        seasons = if (isSeries) state.seasons else emptyList(),
        selectedSeason = state.selectedSeason,
        onSelectSeason = { vm.selectSeason(it.key) },
        onPrimary = {
            val req = if (isSeries) vm.requestFor(vm.primaryEpisode()) else vm.requestFor(null)
            req?.let { nav.streams(it) }
        },
        onSources = state.inLibrary?.let { lib -> { nav.open(Target.Jellyfin(lib.id)) } },
        onEpisode = { ep -> vm.requestFor(ep)?.let { nav.streams(it) } },
        onEpisodeSources = null,
        similar = emptyList(),
        onRetry = vm::load,
    )
}
