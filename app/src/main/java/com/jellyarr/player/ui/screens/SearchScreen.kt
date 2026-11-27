package com.jellyarr.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jellyarr.player.JellyarrApp
import com.jellyarr.player.data.stremio.BuiltIn
import com.jellyarr.player.data.stremio.StremioClient
import com.jellyarr.player.ui.LocalNavigator
import com.jellyarr.player.ui.components.CardRow
import com.jellyarr.player.ui.components.MessageState
import com.jellyarr.player.ui.components.RowSkeleton
import com.jellyarr.player.ui.theme.JColors
import com.jellyarr.player.ui.toCard
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class SearchState(val query: String = "", val rows: List<HomeRow> = emptyList(), val pending: Int = 0)

@OptIn(FlowPreview::class)
class SearchViewModel : ViewModel() {
    private val c = JellyarrApp.container
    private val query = MutableStateFlow("")
    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            query.debounce(450).distinctUntilChanged().collectLatest { q -> search(q.trim()) }
        }
    }

    fun setQuery(q: String) {
        _state.update { it.copy(query = q) }
        query.value = q
    }

    private fun put(row: HomeRow) = _state.update { s ->
        s.copy(rows = (s.rows.filterNot { it.key == row.key } + row).filter { it.cards.isNotEmpty() }.sortedBy { it.order })
    }

    private suspend fun search(q: String) {
        if (q.length < 2) { _state.update { it.copy(rows = emptyList(), pending = 0) }; return }
        val addons = c.addons.enabled.ifEmpty { listOf(BuiltIn.cinemeta) }
        val targets = addons.flatMap { a -> a.manifest.catalogs.filter { it.supportsSearch }.map { a to it } }
        val jfOn = c.settings.state.value.jellyfin != null
        _state.update { it.copy(rows = emptyList(), pending = targets.size + if (jfOn) 1 else 0) }
        supervisorScope {
            if (jfOn) launch {
                val items = runCatching { c.jellyfin.search(q) }.getOrDefault(emptyList())
                put(HomeRow("jf", "في مكتبة Jellyfin", items.map { it.toCard(c.jellyfin) }, order = 0))
                _state.update { it.copy(pending = it.pending - 1) }
            }
            targets.forEachIndexed { i, (addon, cat) ->
                launch {
                    val metas = runCatching { StremioClient.catalog(addon, cat.type, cat.id, mapOf("search" to q)) }.getOrDefault(emptyList())
                    val label = when (cat.type) { "movie" -> "أفلام"; "series" -> "مسلسلات"; else -> cat.type }
                    put(HomeRow("${addon.transportUrl}:${cat.type}:${cat.id}", "$label · ${addon.manifest.name}", metas.map { it.toCard(addon.transportUrl) }, order = 1 + i))
                    _state.update { it.copy(pending = it.pending - 1) }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(vm: SearchViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("بحث", style = MaterialTheme.typography.headlineMedium, color = JColors.Text, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp))
        Row(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp))
                .background(JColors.SurfaceHigh).border(1.dp, JColors.OutlineSoft, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, null, tint = JColors.TextMuted)
            Spacer(Modifier.size(10.dp))
            BasicTextField(
                value = state.query,
                onValueChange = vm::setQuery,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = JColors.Text),
                cursorBrush = SolidColor(JColors.Primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (state.query.isEmpty()) Text("أفلام، مسلسلات، ممثلين…", color = JColors.TextDim, style = MaterialTheme.typography.bodyLarge)
                    inner()
                },
            )
            if (state.query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Filled.Close, null, tint = JColors.TextMuted) }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
            items(state.rows, key = { it.key }) { row -> CardRow(row.title, row.cards, { nav.open(it.target) }) }
            if (state.pending > 0) item { RowSkeleton() }
            if (state.query.trim().length >= 2 && state.pending == 0 && state.rows.isEmpty()) {
                item { MessageState("لا توجد نتائج", "جرّب كلمات أخرى", icon = Icons.Filled.TravelExplore) }
            }
            if (state.query.isBlank()) {
                item { MessageState("ابحث في كل مصادرك", "Jellyfin وكل إضافات Stremio المثبّتة في مكان واحد", icon = Icons.Filled.TravelExplore) }
            }
            item { Spacer(Modifier.height(BottomBarSpace)) }
        }
    }
}
