package com.jellyarr.player.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jellyarr.player.JellyarrApp
import com.jellyarr.player.core.Format
import com.jellyarr.player.data.jellyfin.JfItem
import com.jellyarr.player.data.streams.Playback
import com.jellyarr.player.data.streams.StreamRequest
import com.jellyarr.player.ui.CardModel
import com.jellyarr.player.ui.LocalNavigator
import com.jellyarr.player.ui.toCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class JfDetailsState(
    val loading: Boolean = true,
    val error: String? = null,
    val item: JfItem? = null,
    val seasons: List<SeasonModel> = emptyList(),
    val selectedSeason: String? = null,
    val similar: List<CardModel> = emptyList(),
    val episodeItems: Map<String, JfItem> = emptyMap(),
)

class JellyfinDetailsViewModel : ViewModel() {
    private val c = JellyarrApp.container
    private val jf = c.jellyfin
    private val _state = MutableStateFlow(JfDetailsState())
    val state = _state.asStateFlow()
    private var id: String? = null

    fun bind(itemId: String) {
        if (id == itemId) return
        id = itemId
        load()
    }

    fun load() {
        val itemId = id ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                var item = jf.item(itemId)
                if (item.type == "Season" && item.seriesId != null) item = jf.item(item.seriesId!!)
                item
            }.onSuccess { item ->
                _state.update { it.copy(item = item, loading = false) }
                launch {
                    val sim = if (item.type == "BoxSet" || item.type == "Folder" || item.type == "CollectionFolder")
                        runCatching { jf.items(parentId = item.id, recursive = false, sortBy = "PremiereDate").items }.getOrDefault(emptyList())
                    else runCatching { jf.similar(item.id) }.getOrDefault(emptyList())
                    _state.update { it.copy(similar = sim.map { s -> s.toCard(jf) }) }
                }
                if (item.type == "Series") loadSeasons(item)
            }.onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    private suspend fun loadSeasons(series: JfItem) {
        val seasons = runCatching { jf.seasons(series.id) }.getOrDefault(emptyList())
        val models = seasons.map { SeasonModel(it.id, it.indexNumber, it.name, null) }
        val firstUnwatched = seasons.firstOrNull { it.userData?.played != true && (it.indexNumber ?: 0) > 0 } ?: seasons.firstOrNull()
        _state.update { it.copy(seasons = models, selectedSeason = firstUnwatched?.id) }
        firstUnwatched?.let { selectSeason(it.id) }
    }

    fun selectSeason(seasonId: String) {
        val series = _state.value.item ?: return
        _state.update { it.copy(selectedSeason = seasonId) }
        if (_state.value.seasons.firstOrNull { it.key == seasonId }?.episodes != null) return
        viewModelScope.launch {
            val eps = runCatching { jf.episodes(series.id, seasonId) }.getOrDefault(emptyList())
            val models = eps.map { e ->
                EpisodeModel(
                    key = e.id, number = e.indexNumber, season = e.parentIndexNumber, title = e.name,
                    overview = e.overview, thumb = jf.thumbUrl(e), released = e.premiereDate,
                    runtime = Format.runtime(e.runtimeMinutes), progress = e.progress, played = e.userData?.played == true,
                    jellyfinId = e.id,
                )
            }
            _state.update { s ->
                s.copy(
                    seasons = s.seasons.map { if (it.key == seasonId) it.copy(episodes = models) else it },
                    episodeItems = s.episodeItems + eps.associateBy { it.id },
                )
            }
        }
    }

    fun toggleFavorite() {
        val item = _state.value.item ?: return
        val fav = item.userData?.isFavorite != true
        viewModelScope.launch {
            runCatching { jf.setFavorite(item.id, fav) }.onSuccess {
                _state.update { s -> s.copy(item = s.item?.let { i -> i.copy(userData = i.userData?.copy(isFavorite = fav)) }) }
            }
        }
    }

    fun togglePlayed() {
        val item = _state.value.item ?: return
        val played = item.userData?.played != true
        viewModelScope.launch {
            runCatching { jf.setPlayed(item.id, played) }.onSuccess {
                _state.update { s -> s.copy(item = s.item?.let { i -> i.copy(userData = i.userData?.copy(played = played)) }) }
            }
        }
    }

    /** Episode to play when pressing the main button on a series. */
    fun nextEpisode(): JfItem? {
        val s = _state.value
        val eps = s.seasons.firstOrNull { it.key == s.selectedSeason }?.episodes ?: return null
        val pick = eps.firstOrNull { (it.progress ?: 0f) > 0f } ?: eps.firstOrNull { !it.played } ?: eps.firstOrNull()
        return pick?.jellyfinId?.let { s.episodeItems[it] }
    }

    fun streamRequest(item: JfItem, series: JfItem?): StreamRequest = StreamRequest(
        type = if (item.type == "Episode") "series" else "movie",
        stremioId = null,
        imdbId = if (item.type == "Episode") series?.imdbId else item.imdbId,
        tmdbId = if (item.type == "Episode") series?.tmdbId else item.tmdbId,
        season = item.parentIndexNumber,
        episode = item.indexNumber,
        title = if (item.type == "Episode") item.seriesName ?: series?.name ?: item.name else item.name,
        episodeTitle = if (item.type == "Episode") item.name else null,
        poster = jf.seriesPosterUrl(item),
        backdrop = jf.backdropUrl(series ?: item),
        jellyfinItemId = item.id,
    )
}

@Composable
fun JellyfinDetailsScreen(id: String, vm: JellyfinDetailsViewModel = viewModel()) {
    LaunchedEffect(id) { vm.bind(id) }
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    val c = JellyarrApp.container
    val jf = c.jellyfin
    val item = state.item

    val model = item?.let { i ->
        val video = i.mediaSources.firstOrNull()?.streams?.firstOrNull { it.type == "Video" }
        val badges = buildList {
            video?.height?.let { h -> add(if (h >= 2000) "4K" else if (h >= 1000) "1080p" else if (h >= 700) "720p" else "SD") }
            video?.videoRange?.takeIf { !it.equals("SDR", true) }?.let { add(it) }
            if (i.mediaSources.size > 1) add("${i.mediaSources.size} نسخ")
        }
        DetailsModel(
            title = i.name,
            logo = jf.logoUrl(i),
            backdrop = jf.backdropUrl(i),
            poster = jf.posterUrl(i),
            year = i.year?.toString(),
            runtime = Format.runtime(i.runtimeMinutes),
            rating = i.communityRating?.let { String.format(Locale.US, "%.1f", it) },
            officialRating = i.officialRating,
            genres = i.genres,
            overview = i.overview,
            tagline = i.taglines.firstOrNull(),
            people = i.people.map { PersonModel(it.name, it.role, jf.personImageUrl(it)) },
            isSeries = i.type == "Series",
            sourceLabel = "Jellyfin",
            badges = badges,
            progress = i.progress,
            favorite = i.userData?.isFavorite,
            played = i.userData?.played,
            primaryActionLabel = when {
                i.type == "Series" -> "تشغيل"
                (i.progress ?: 0f) > 0f -> "استئناف"
                i.isFolderLike -> "تصفح"
                else -> "تشغيل"
            },
        )
    }

    DetailsLayout(
        model = model,
        loading = state.loading,
        error = state.error,
        seasons = state.seasons,
        selectedSeason = state.selectedSeason,
        onSelectSeason = { vm.selectSeason(it.key) },
        onPrimary = {
            val i = item ?: return@DetailsLayout
            when {
                i.type == "Series" -> vm.nextEpisode()?.let { nav.play(Playback.fromJellyfin(c, it)) }
                i.isPlayable -> nav.play(Playback.fromJellyfin(c, i))
                else -> nav.library(i.id, i.name)
            }
        },
        onSources = item?.takeIf { it.isPlayable }?.let { i -> { nav.streams(vm.streamRequest(i, null)) } },
        onEpisode = { ep -> state.episodeItems[ep.key]?.let { nav.play(Playback.fromJellyfin(c, it)) } },
        onEpisodeSources = { ep -> state.episodeItems[ep.key]?.let { nav.streams(vm.streamRequest(it, item)) } },
        similar = state.similar,
        onToggleFavorite = vm::toggleFavorite,
        onTogglePlayed = vm::togglePlayed,
        onRetry = vm::load,
    )
}
