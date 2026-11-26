package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.BaseItemDto
import com.example.data.model.GridDensity
import com.example.data.model.SortOption
import com.example.data.model.UserSettings
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MediaPosterCard
import com.example.ui.theme.LocalStrings
import com.example.ui.viewmodel.LibraryFilterState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryBrowseScreen(
    title: String,
    state: LibraryFilterState,
    settings: UserSettings,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onFilterChange: (SortOption, Boolean, Boolean, String?) -> Unit,
    onItemClick: (BaseItemDto) -> Unit
) {
    val strings = LocalStrings.current
    val serverUrl = settings.activeServer?.url.orEmpty()
    var showSortMenu by remember { mutableStateOf(false) }
    var showGenreSheet by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= (state.items.size - 10).coerceAtLeast(0) &&
                state.items.isNotEmpty() &&
                state.items.size < state.totalCount
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = title, style = MaterialTheme.typography.titleLarge)
                    if (state.totalCount > 0) {
                        Text(
                            text = "(${state.totalCount})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            actions = {
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.testTag("library_sort_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = strings.sortBy)
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortOption.entries.forEach { option ->
                            val label = when (option) {
                                SortOption.LATEST -> strings.sortLatest
                                SortOption.NAME -> strings.sortName
                                SortOption.RATING -> strings.sortRating
                                SortOption.YEAR -> strings.sortYear
                            }
                            DropdownMenuItem(
                                text = { Text(label) },
                                trailingIcon = {
                                    if (state.sortOption == option) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                },
                                onClick = {
                                    showSortMenu = false
                                    onFilterChange(option, state.unwatchedOnly, state.favoritesOnly, state.selectedGenre)
                                }
                            )
                        }
                    }
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = strings.refresh)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        // Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !state.unwatchedOnly && !state.favoritesOnly && state.selectedGenre == null,
                onClick = { onFilterChange(state.sortOption, false, false, null) },
                label = { Text(strings.allItems) }
            )
            FilterChip(
                selected = state.unwatchedOnly,
                onClick = {
                    onFilterChange(state.sortOption, !state.unwatchedOnly, state.favoritesOnly, state.selectedGenre)
                },
                label = { Text(strings.unwatchedOnly) },
                leadingIcon = { Icon(Icons.Filled.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = state.favoritesOnly,
                onClick = {
                    onFilterChange(state.sortOption, state.unwatchedOnly, !state.favoritesOnly, state.selectedGenre)
                },
                label = { Text(strings.favoritesOnly) },
                leadingIcon = { Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            if (state.genres.isNotEmpty()) {
                FilterChip(
                    selected = state.selectedGenre != null,
                    onClick = { showGenreSheet = true },
                    label = { Text(state.selectedGenre ?: strings.genreFilter) },
                    leadingIcon = { Icon(Icons.Filled.Category, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        when {
            state.isLoading && state.items.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null && state.items.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateView(
                        title = strings.errorConnectionFailed,
                        subtitle = state.error,
                        actionLabel = strings.retry,
                        onAction = onRefresh
                    )
                }
            }
            state.items.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateView(
                        title = strings.noItemsFound,
                        subtitle = strings.emptyHomeSubtitle,
                        actionLabel = strings.refresh,
                        onAction = onRefresh
                    )
                }
            }
            else -> {
                val cellMinSize = if (settings.gridDensity == GridDensity.COMPACT) 104.dp else 132.dp
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = cellMinSize),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.items, key = { it.id }) { item ->
                        MediaPosterCard(
                            item = item,
                            serverUrl = serverUrl,
                            showProgress = settings.showProgressBars,
                            onClick = { onItemClick(item) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (state.isLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGenreSheet) {
        ModalBottomSheet(onDismissRequest = { showGenreSheet = false }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = strings.genreFilter,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                LazyColumn(modifier = Modifier.height(380.dp)) {
                    item {
                        ListItem(
                            headlineContent = { Text(strings.allGenres) },
                            trailingContent = {
                                if (state.selectedGenre == null) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier.clickable {
                                showGenreSheet = false
                                onFilterChange(state.sortOption, state.unwatchedOnly, state.favoritesOnly, null)
                            }
                        )
                    }
                    items(state.genres, key = { it.id }) { genre ->
                        ListItem(
                            headlineContent = { Text(genre.name) },
                            trailingContent = {
                                if (state.selectedGenre == genre.name) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier.clickable {
                                showGenreSheet = false
                                onFilterChange(state.sortOption, state.unwatchedOnly, state.favoritesOnly, genre.name)
                            }
                        )
                    }
                }
            }
        }
    }
}
