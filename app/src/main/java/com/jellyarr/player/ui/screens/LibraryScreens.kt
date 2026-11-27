package com.jellyarr.player.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jellyarr.player.JellyarrApp
import com.jellyarr.player.data.jellyfin.JfItem
import com.jellyarr.player.ui.LocalNavigator
import com.jellyarr.player.ui.components.BackButton
import com.jellyarr.player.ui.components.CenterLoading
import com.jellyarr.player.ui.components.IconAction
import com.jellyarr.player.ui.components.MessageState
import com.jellyarr.player.ui.components.ScreenTitle
import com.jellyarr.player.ui.theme.JColors
import com.jellyarr.player.ui.toCard
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val SORTS = listOf(
    Triple("SortName", "Ascending", "الاسم"),
    Triple("DateCreated", "Descending", "تاريخ الإضافة"),
    Triple("PremiereDate", "Descending", "تاريخ الإصدار"),
    Triple("CommunityRating", "Descending", "التقييم"),
    Triple("Random", "Ascending", "عشوائي"),
)

class JfGridViewModel : ViewModel() {
    private val c = JellyarrApp.container
    private val _state = MutableStateFlow(GridState())
    val state = _state.asStateFlow()
    private var key: String? = null
    private var parent: String? = null
    private var sort = SORTS[0]
    private var total = Int.MAX_VALUE
    private var job: Job? = null

    fun bind(parentId: String, sortIndex: Int) {
        val k = "$parentId|$sortIndex"
        if (k == key) return
        key = k; parent = parentId; sort = SORTS[sortIndex]; total = Int.MAX_VALUE
        job?.cancel()
        _state.value = GridState()
        loadMore()
    }

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.endReached) return
        val p = parent ?: return
        _state.update { it.copy(loading = true) }
        job = viewModelScope.launch {
            runCatching {
                c.jellyfin.items(
                    parentId = p, includeTypes = "Movie,Series,Video,BoxSet,MusicVideo",
                    sortBy = sort.first, sortOrder = sort.second, start = s.items.size, limit = 60,
                )
            }.onSuccess { page ->
                total = page.total
                _state.update {
                    val items = it.items + page.items.map { i -> i.toCard(c.jellyfin) }
                    it.copy(items = items, loading = false, endReached = items.size >= total || page.items.isEmpty())
                }
            }.onFailure { e -> _state.update { it.copy(loading = false, endReached = true, error = e.message) } }
        }
    }
}

class JfViewsViewModel : ViewModel() {
    private val c = JellyarrApp.container
    val views = MutableStateFlow<List<JfItem>?>(null)
    val error = MutableStateFlow<String?>(null)

    fun load() {
        viewModelScope.launch {
            runCatching { c.jellyfin.views() }
                .onSuccess { v -> views.value = v.filter { it.collectionType !in setOf("music", "playlists", "livetv", "books", "photos") } }
                .onFailure { error.value = it.message; views.value = emptyList() }
        }
    }
}

@Composable
fun LibraryTab(viewsVm: JfViewsViewModel = viewModel(), gridVm: JfGridViewModel = viewModel(key = "libtab")) {
    val nav = LocalNavigator.current
    val settings by JellyarrApp.container.settings.state.collectAsStateWithLifecycle()
    val views by viewsVm.views.collectAsStateWithLifecycle()
    val error by viewsVm.error.collectAsStateWithLifecycle()
    LaunchedEffect(settings.jellyfin?.token) { if (settings.jellyfin != null) viewsVm.load() }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        var sortIndex by rememberSaveable { mutableStateOf(0) }
        var menu by remember { mutableStateOf(false) }
        ScreenTitle("المكتبة") {
            if (settings.jellyfin != null) {
                IconAction(Icons.AutoMirrored.Filled.Sort, { menu = true })
                DropdownMenu(menu, { menu = false }) {
                    SORTS.forEachIndexed { i, s ->
                        DropdownMenuItem(text = { Text(s.third, color = if (i == sortIndex) JColors.PrimarySoft else JColors.Text) }, onClick = { sortIndex = i; menu = false })
                    }
                }
            }
        }
        when {
            settings.jellyfin == null -> MessageState(
                "Jellyfin غير مربوط", "اربط خادمك لتصفح مكتبتك الشخصية ومتابعة المشاهدة.",
                icon = Icons.Filled.Dns, actionText = "ربط الخادم", onAction = { nav.to("jflogin") },
            )
            views == null -> CenterLoading()
            views!!.isEmpty() -> MessageState("لا توجد مكتبات", error)
            else -> {
                val list = views!!
                var selected by rememberSaveable { mutableStateOf<String?>(null) }
                val current = list.firstOrNull { it.id == selected } ?: list.first()
                LaunchedEffect(current.id, sortIndex) { gridVm.bind(current.id, sortIndex) }
                val state by gridVm.state.collectAsStateWithLifecycle()
                ChipRow(list.map { it.id to it.name }, current.id, { selected = it })
                Spacer(Modifier.height(12.dp))
                PosterGrid(state, gridVm::loadMore, { nav.open(it.target) })
            }
        }
    }
}

@Composable
fun JellyfinLibraryScreen(id: String, name: String, vm: JfGridViewModel = viewModel()) {
    val nav = LocalNavigator.current
    LaunchedEffect(id) { vm.bind(id, 0) }
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton({ nav.back() })
            Spacer(Modifier.width(12.dp))
            Text(name, style = MaterialTheme.typography.headlineSmall, color = JColors.Text)
        }
        PosterGrid(state, vm::loadMore, { nav.open(it.target) })
    }
}
