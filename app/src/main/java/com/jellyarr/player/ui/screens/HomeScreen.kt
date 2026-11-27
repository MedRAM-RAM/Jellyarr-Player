package com.jellyarr.player.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jellyarr.player.JellyarrApp
import com.jellyarr.player.core.Format
import com.jellyarr.player.data.HistoryEntry
import com.jellyarr.player.data.stremio.StremioClient
import com.jellyarr.player.ui.CardModel
import com.jellyarr.player.ui.LocalNavigator
import com.jellyarr.player.ui.Target
import com.jellyarr.player.ui.components.CardRow
import com.jellyarr.player.ui.components.GhostButton
import com.jellyarr.player.ui.components.GradientButton
import com.jellyarr.player.ui.components.MessageState
import com.jellyarr.player.ui.components.NetImage
import com.jellyarr.player.ui.components.RowSkeleton
import com.jellyarr.player.ui.components.Shimmer
import com.jellyarr.player.ui.components.Tag
import com.jellyarr.player.ui.theme.JColors
import com.jellyarr.player.ui.toCard
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class HomeRow(
    val key: String,
    val title: String,
    val cards: List<CardModel>,
    val landscape: Boolean = false,
    val order: Int,
    val catalogLink: CatalogLink? = null,
)

data class CatalogLink(val addonUrl: String, val type: String, val id: String, val name: String)

data class HomeState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val hero: List<CardModel> = emptyList(),
    val rows: List<HomeRow> = emptyList(),
    val pendingRows: Int = 0,
    val hasSources: Boolean = true,
)

class HomeViewModel : ViewModel() {
    private val c = JellyarrApp.container
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Reload whenever the set of sources changes.
            c.settings.state.map { s -> Triple(s.jellyfin?.token, s.addons.map { it.transportUrl to it.enabled }, s.loaded) }
                .distinctUntilChanged()
                .collect { load() }
        }
        viewModelScope.launch {
            c.history.entries.collect { refreshHistoryRow() }
        }
    }

    fun refresh() {
        _state.update { it.copy(refreshing = true) }
        viewModelScope.launch { load(); _state.update { it.copy(refreshing = false) } }
    }

    private fun putRow(row: HomeRow) = _state.update { s ->
        val rows = (s.rows.filterNot { it.key == row.key } + row).filter { it.cards.isNotEmpty() }.sortedBy { it.order }
        s.copy(rows = rows)
    }

    private fun historyCard(e: HistoryEntry) = CardModel(
        key = "hist:${e.key}",
        title = e.name,
        subtitle = Format.episodeCode(e.season, e.episode)?.let { code -> listOfNotNull(code, e.episodeTitle).joinToString(" · ") }
            ?: "${Format.duration(e.durationMs - e.positionMs)} متبقية",
        poster = e.backdrop ?: e.poster,
        backdrop = e.backdrop,
        progress = e.progress,
        target = Target.Meta(e.type, e.metaId),
    )

    private fun refreshHistoryRow() {
        val cards = c.history.continueWatching.take(20).map(::historyCard)
        putRow(HomeRow("history", "متابعة المشاهدة · الإضافات", cards, landscape = true, order = 1))
    }

    private suspend fun load() {
        val s = c.settings.current()
        val addons = c.addons.enabled
        _state.update { it.copy(loading = true, rows = emptyList(), hero = emptyList(), hasSources = s.jellyfin != null || addons.isNotEmpty()) }
        refreshHistoryRow()
        val jf = c.jellyfin
        supervisorScope {
            val jobs = mutableListOf<kotlinx.coroutines.Job>()
            if (s.jellyfin != null) {
                jobs += launch {
                    runCatching { jf.resume() }.getOrNull()?.let { items ->
                        putRow(HomeRow("jf-resume", "متابعة المشاهدة", items.map { it.toCard(jf, landscape = true) }, true, 0))
                    }
                }
                jobs += launch {
                    runCatching { jf.nextUp() }.getOrNull()?.let { items ->
                        putRow(HomeRow("jf-next", "الحلقات التالية", items.map { it.toCard(jf, landscape = true) }, true, 2))
                    }
                }
                jobs += launch {
                    val views = runCatching { jf.views() }.getOrDefault(emptyList())
                        .filter { it.collectionType in setOf("movies", "tvshows", "homevideos", "mixed", null) }
                    views.mapIndexed { i, v ->
                        launch {
                            val latest = runCatching { jf.latest(v.id, 20) }.getOrDefault(emptyList())
                            putRow(HomeRow("jf-latest-${v.id}", "أُضيف حديثاً · ${v.name}", latest.map { it.toCard(jf) }, false, 10 + i))
                            if (_state.value.hero.isEmpty()) {
                                val hero = latest.filter { it.backdropTags.isNotEmpty() || it.parentBackdropTags.isNotEmpty() }
                                    .take(6).map { it.toCard(jf) }
                                if (hero.isNotEmpty()) _state.update { st -> if (st.hero.isEmpty()) st.copy(hero = hero) else st }
                            }
                        }
                    }.joinAll()
                }
            }
            var order = 100
            addons.forEach { addon ->
                addon.manifest.catalogs.filter { it.isBrowsable && !it.requiresExtra }.take(10).forEach { cat ->
                    val myOrder = order++
                    jobs += launch {
                        val metas = runCatching { StremioClient.catalog(addon, cat.type, cat.id) }.getOrDefault(emptyList())
                        val typeLabel = when (cat.type) { "movie" -> "أفلام"; "series" -> "مسلسلات"; "anime" -> "أنمي"; "tv" -> "قنوات"; else -> cat.type }
                        putRow(
                            HomeRow(
                                "cat:${addon.transportUrl}:${cat.type}:${cat.id}",
                                "${cat.name} · $typeLabel",
                                metas.take(30).map { it.toCard(addon.transportUrl) },
                                false, myOrder,
                                CatalogLink(addon.transportUrl, cat.type, cat.id, "${cat.name} · $typeLabel"),
                            )
                        )
                        if (_state.value.hero.isEmpty()) {
                            val hero = metas.filter { it.background != null }.take(6).map { it.toCard(addon.transportUrl) }
                            if (hero.isNotEmpty()) _state.update { st -> if (st.hero.isEmpty()) st.copy(hero = hero) else st }
                        }
                    }
                }
            }
            _state.update { it.copy(pendingRows = jobs.size) }
            jobs.joinAll()
        }
        _state.update { it.copy(loading = false, pendingRows = 0) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current

    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            item(key = "hero") {
                when {
                    state.hero.isNotEmpty() -> HeroCarousel(state.hero) { nav.open(it.target) }
                    state.loading && state.hasSources -> Shimmer(Modifier.fillMaxWidth().height(520.dp))
                    else -> Spacer(Modifier.statusBarsPadding().height(16.dp))
                }
            }
            if (!state.hasSources) {
                item(key = "empty") {
                    MessageState(
                        title = "مرحباً بك في Jellyarr",
                        message = "اربط خادم Jellyfin أو أضف إضافات Stremio (مثل AIOStreams) لبدء المشاهدة.",
                        icon = Icons.Filled.Extension,
                        actionText = "إضافة مصادر",
                        onAction = { nav.to("addons") },
                    )
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        GhostButton("ربط Jellyfin", { nav.to("jflogin") })
                    }
                }
            }
            items(state.rows, key = { it.key }) { row ->
                CardRow(
                    title = row.title,
                    cards = row.cards,
                    landscape = row.landscape,
                    onClick = { nav.open(it.target) },
                    action = row.catalogLink?.let { link ->
                        @Composable {
                            Row(
                                Modifier.clip(RoundedCornerShape(8.dp)).clickable { nav.catalog(link.addonUrl, link.type, link.id, link.name) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("الكل", color = JColors.PrimarySoft, style = MaterialTheme.typography.labelLarge)
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = JColors.PrimarySoft, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                )
            }
            if (state.loading && state.hasSources) {
                items(if (state.rows.isEmpty()) 3 else 1) { i -> RowSkeleton(landscape = i == 0 && state.rows.isEmpty()) }
            }
            item { Spacer(Modifier.height(BottomBarSpace)) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(cards: List<CardModel>, onOpen: (CardModel) -> Unit) {
    val pager = rememberPagerState { cards.size }
    LaunchedEffect(cards.size) {
        while (cards.size > 1) {
            delay(7000)
            pager.animateScrollToPage((pager.currentPage + 1) % cards.size)
        }
    }
    Box(Modifier.fillMaxWidth().height(540.dp)) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            val card = cards[page]
            Box(Modifier.fillMaxSize().clickable { onOpen(card) }) {
                NetImage(card.backdrop ?: card.poster, Modifier.fillMaxSize(), ContentScale.Crop)
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = .55f),
                            0.25f to Color.Transparent,
                            0.55f to JColors.Background.copy(alpha = .35f),
                            0.85f to JColors.Background.copy(alpha = .92f),
                            1f to JColors.Background,
                        )
                    )
                )
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (card.logo != null) {
                        NetImage(card.logo, Modifier.fillMaxWidth(.7f).heightIn(max = 110.dp).height(100.dp), ContentScale.Fit)
                    } else {
                        Text(
                            card.title, style = MaterialTheme.typography.displaySmall, color = Color.White,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        card.rating?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, null, tint = JColors.Gold, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(it, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        card.year?.let { Text(it, color = JColors.TextMuted, fontSize = 13.sp) }
                        card.genres.take(3).forEach { Tag(it, Color.White.copy(alpha = .85f)) }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GradientButton("شاهد الآن", { onOpen(card) }, icon = Icons.Filled.PlayArrow, modifier = Modifier.width(170.dp))
                        GhostButton("التفاصيل", { onOpen(card) }, icon = Icons.Filled.Info)
                    }
                }
            }
        }
        // top brand bar
        Row(
            Modifier.statusBarsPadding().padding(horizontal = 18.dp, vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(JColors.Brand), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text("Jellyarr", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
        // page indicators
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(cards.size) { i ->
                val active = i == pager.currentPage
                Box(
                    Modifier.height(5.dp).width(if (active) 18.dp else 5.dp).clip(CircleShape)
                        .background(if (active) JColors.Primary else Color.White.copy(alpha = .35f))
                )
            }
        }
    }
}
