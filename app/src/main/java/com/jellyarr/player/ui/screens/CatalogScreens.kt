package com.jellyarr.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jellyarr.player.JellyarrApp
import com.jellyarr.player.data.stremio.Addon
import com.jellyarr.player.data.stremio.AddonCatalog
import com.jellyarr.player.data.stremio.BuiltIn
import com.jellyarr.player.data.stremio.StremioClient
import com.jellyarr.player.ui.CardModel
import com.jellyarr.player.ui.LocalNavigator
import com.jellyarr.player.ui.components.BackButton
import com.jellyarr.player.ui.components.MessageState
import com.jellyarr.player.ui.components.PosterCard
import com.jellyarr.player.ui.components.ScreenTitle
import com.jellyarr.player.ui.components.Shimmer
import com.jellyarr.player.ui.theme.JColors
import com.jellyarr.player.ui.toCard
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GridState(
    val items: List<CardModel> = emptyList(),
    val loading: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null,
)

/** Paged Stremio catalog loader. */
class CatalogGridViewModel : ViewModel() {
    private val _state = MutableStateFlow(GridState())
    val state = _state.asStateFlow()
    private var key: String? = null
    private var addon: Addon? = null
    private var catalog: AddonCatalog? = null
    private var genre: String? = null
    private var job: Job? = null

    fun bind(addon: Addon, catalog: AddonCatalog, genre: String?) {
        val k = "${addon.transportUrl}|${catalog.type}|${catalog.id}|$genre"
        if (k == key) return
        key = k
        this.addon = addon; this.catalog = catalog; this.genre = genre
        job?.cancel()
        _state.value = GridState()
        loadMore()
    }

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.endReached) return
        val a = addon ?: return
        val c = catalog ?: return
        _state.update { it.copy(loading = true, error = null) }
        job = viewModelScope.launch {
            val extra = buildMap {
                genre?.let { put("genre", it) }
                if (s.items.isNotEmpty()) put("skip", s.items.size.toString())
            }
            runCatching { StremioClient.catalog(a, c.type, c.id, extra) }
                .onSuccess { metas ->
                    val known = _state.value.items.map { it.key }.toSet()
                    val fresh = metas.map { it.toCard(a.transportUrl) }.filter { it.key !in known }
                    _state.update {
                        it.copy(
                            items = it.items + fresh, loading = false,
                            endReached = fresh.isEmpty() || !c.supportsSkip,
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message, endReached = true) } }
        }
    }
}

@Composable
fun PosterGrid(
    state: GridState,
    onLoadMore: () -> Unit,
    onClick: (CardModel) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    val grid = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val last = grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= grid.layoutInfo.totalItemsCount - 9
        }
    }
    LaunchedEffect(grid) {
        snapshotFlow { nearEnd }.distinctUntilChanged().filter { it }.collect { onLoadMore() }
    }
    LazyVerticalGrid(
        state = grid,
        columns = GridCells.Adaptive(108.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = BottomBarSpace),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        header?.let { h -> item(span = { GridItemSpan(maxLineSpan) }) { h() } }
        items(state.items, key = { it.key }) { c -> PosterCard(c, { onClick(c) }, width = androidx.compose.ui.unit.Dp.Unspecified) }
        if (state.loading && state.items.isEmpty()) {
            items(12) { Shimmer(Modifier.fillMaxWidth().height(170.dp), RoundedCornerShape(12.dp)) }
        } else if (state.loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = JColors.Primary, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
                }
            }
        }
        if (!state.loading && state.items.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                MessageState("لا توجد عناصر", state.error ?: "جرّب تصنيفاً آخر")
            }
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(50))
            .then(
                if (selected) Modifier.background(JColors.BrandHorizontal)
                else Modifier.background(JColors.SurfaceHigh).border(1.dp, JColors.OutlineSoft, RoundedCornerShape(50))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(text, color = if (selected) Color.White else JColors.TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun ChipRow(options: List<Pair<String, String>>, selected: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(modifier, contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options, key = { it.first }) { (k, label) -> Chip(label, k == selected, { onSelect(k) }) }
    }
}

private fun typeLabel(t: String) = when (t) {
    "movie" -> "أفلام"; "series" -> "مسلسلات"; "anime" -> "أنمي"; "tv" -> "قنوات"; "channel" -> "قنوات"; else -> t
}

@Composable
fun DiscoverScreen(vm: CatalogGridViewModel = viewModel(key = "discover")) {
    val nav = LocalNavigator.current
    val installed by JellyarrApp.container.addons.addons.collectAsStateWithLifecycle()
    val addons = remember(installed) {
        installed.filter { it.enabled && it.manifest.catalogs.isNotEmpty() }.ifEmpty { listOf(BuiltIn.cinemeta) }
    }
    val catalogs = remember(addons) {
        addons.flatMap { a -> a.manifest.catalogs.filter { it.isBrowsable }.map { a to it } }
    }
    val types = remember(catalogs) { catalogs.map { it.second.type }.distinct() }
    var type by rememberSaveable { mutableStateOf<String?>(null) }
    val currentType = type?.takeIf { it in types } ?: types.firstOrNull()
    val ofType = remember(catalogs, currentType) { catalogs.filter { it.second.type == currentType } }
    var catKey by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = ofType.firstOrNull { "${it.first.transportUrl}|${it.second.id}" == catKey } ?: ofType.firstOrNull()
    var genre by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(selected, genre) {
        selected?.let { vm.bind(it.first, it.second, genre?.takeIf { g -> g in it.second.genres }) }
    }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTitle("استكشاف")
        if (selected == null) {
            MessageState("لا توجد كتالوجات", "أضف إضافات Stremio تحتوي على كتالوجات", icon = Icons.Filled.Extension,
                actionText = "الإضافات", onAction = { nav.to("addons") })
        } else {
        ChipRow(types.map { it to typeLabel(it) }, currentType, { type = it; catKey = null; genre = null })
        Spacer(Modifier.height(8.dp))
        ChipRow(
            ofType.map { "${it.first.transportUrl}|${it.second.id}" to (if (addons.size > 1) "${it.second.name} · ${it.first.manifest.name}" else it.second.name) },
            "${selected.first.transportUrl}|${selected.second.id}",
            { catKey = it; genre = null },
        )
        val genres = selected.second.genres
        if (genres.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            ChipRow(listOf("" to "الكل") + genres.map { it to it }, genre ?: "", { genre = it.ifBlank { null } })
        }
        Spacer(Modifier.height(12.dp))
        PosterGrid(state, vm::loadMore, { nav.open(it.target) })
        }
    }
}

@Composable
fun CatalogScreen(addonUrl: String, type: String, catalogId: String, name: String, vm: CatalogGridViewModel = viewModel()) {
    val nav = LocalNavigator.current
    val installed by JellyarrApp.container.addons.addons.collectAsStateWithLifecycle()
    val addon = installed.firstOrNull { it.transportUrl == addonUrl } ?: BuiltIn.cinemeta.takeIf { it.transportUrl == addonUrl }
    val catalog = addon?.manifest?.catalogs?.firstOrNull { it.type == type && it.id == catalogId }
    var genre by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(addon, catalog, genre) { if (addon != null && catalog != null) vm.bind(addon, catalog, genre) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton({ nav.back() })
            Spacer(Modifier.width(12.dp))
            Text(name, style = MaterialTheme.typography.headlineSmall, color = JColors.Text, maxLines = 1)
        }
        val genres = catalog?.genres.orEmpty()
        if (genres.isNotEmpty()) {
            ChipRow(listOf("" to "الكل") + genres.map { it to it }, genre ?: "", { genre = it.ifBlank { null } })
            Spacer(Modifier.height(12.dp))
        }
        if (catalog == null) MessageState("الكتالوج غير متاح", icon = Icons.Filled.Explore)
        else PosterGrid(state, vm::loadMore, { nav.open(it.target) })
    }
}
