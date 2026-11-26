package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.BaseItemDto
import com.example.data.model.UserSettings
import com.example.data.model.durationMinutes
import com.example.data.model.episodeCode
import com.example.data.model.posterUrl
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PosterPlaceholder
import com.example.ui.theme.AmberStar
import com.example.ui.theme.LocalStrings
import com.example.ui.viewmodel.SearchUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    settings: UserSettings,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onClearHistory: () -> Unit,
    onItemClick: (BaseItemDto) -> Unit
) {
    val strings = LocalStrings.current
    val serverUrl = settings.activeServer?.url.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Search Box
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = { Text(strings.searchPlaceholder) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = strings.navSearch) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = strings.close)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmitSearch(state.query) }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("search_input")
        )

        // Filter Chips
        val filters = listOf(
            "Movie,Series,Episode" to strings.filterAll,
            "Movie" to strings.filterMovies,
            "Series" to strings.filterSeries,
            "Episode" to strings.filterEpisodes
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { (typeKey, label) ->
                FilterChip(
                    selected = state.selectedFilter == typeKey,
                    onClick = { onFilterChange(typeKey) },
                    label = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.query.isBlank() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (settings.recentSearches.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.recentSearches,
                                style = MaterialTheme.typography.titleMedium
                            )
                            TextButton(onClick = onClearHistory) {
                                Text(strings.clearHistory)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            settings.recentSearches.forEach { recent ->
                                AssistChip(
                                    onClick = { onSubmitSearch(recent) },
                                    label = { Text(recent) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.History,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyStateView(
                                title = strings.navSearch,
                                subtitle = strings.searchEmptyHint
                            )
                        }
                    }
                }
            }
            state.hasSearched && state.results.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateView(
                        title = strings.noResults,
                        subtitle = state.query
                    )
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.results, key = { it.id }) { item ->
                        SearchResultRow(
                            item = item,
                            serverUrl = serverUrl,
                            onClick = { onItemClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    item: BaseItemDto,
    serverUrl: String,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .aspectRatio(2f / 3f)
                    .clip(MaterialTheme.shapes.small)
            ) {
                SubcomposeAsyncImage(
                    model = item.posterUrl(serverUrl),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { PosterPlaceholder(item.name) },
                    error = { PosterPlaceholder(item.name) }
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val typeLabel = when (item.type) {
                    "Movie" -> strings.filterMovies
                    "Series" -> strings.filterSeries
                    "Episode" -> item.episodeCode() ?: strings.filterEpisodes
                    else -> item.type
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = if (item.type == "Episode" && !item.seriesName.isNullOrBlank()) {
                        "${item.seriesName} — ${item.name}"
                    } else item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.productionYear?.let {
                        Text(
                            text = it.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val mins = item.durationMinutes()
                    if (mins > 0) {
                        Text(
                            text = "$mins ${strings.minutesUnit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item.communityRating?.let { rating ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = AmberStar, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format("%.1f", rating),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
