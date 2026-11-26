package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BaseItemDto
import com.example.data.model.UserSettings
import com.example.data.remote.HomeDashboardData
import com.example.ui.components.EmptyStateView
import com.example.ui.components.EpisodeContinueCard
import com.example.ui.components.HeroCarousel
import com.example.ui.components.MediaPosterCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalStrings
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeState: UiState<HomeDashboardData>,
    settings: UserSettings,
    onRefresh: () -> Unit,
    onOpenDetail: (BaseItemDto) -> Unit,
    onPlayDirect: (BaseItemDto) -> Unit,
    onNavigateMovies: () -> Unit,
    onNavigateSeries: () -> Unit
) {
    val strings = LocalStrings.current
    val server = settings.activeServer
    val serverUrl = server?.url.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = strings.appName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (server != null) {
                        Text(
                            text = "${server.name} • ${server.username}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            actions = {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("home_refresh_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = strings.refresh
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        when (homeState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateView(
                        title = strings.errorConnectionFailed,
                        subtitle = homeState.message,
                        actionLabel = strings.retry,
                        onAction = onRefresh
                    )
                }
            }

            is UiState.Success -> {
                val data = homeState.data
                val isEverythingEmpty = data.featuredItems.isEmpty() &&
                    data.resumeItems.isEmpty() &&
                    data.nextUpItems.isEmpty() &&
                    data.latestMovies.isEmpty() &&
                    data.latestSeries.isEmpty()

                if (isEverythingEmpty) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyStateView(
                            title = strings.emptyHomeTitle,
                            subtitle = strings.emptyHomeSubtitle,
                            actionLabel = strings.refresh,
                            onAction = onRefresh
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (data.featuredItems.isNotEmpty()) {
                            item(key = "hero_carousel") {
                                HeroCarousel(
                                    items = data.featuredItems,
                                    serverUrl = serverUrl,
                                    onWatchClick = onPlayDirect,
                                    onDetailsClick = onOpenDetail
                                )
                            }
                        }

                        if (data.libraries.isNotEmpty()) {
                            item(key = "libraries_row") {
                                Column {
                                    SectionHeader(title = strings.myLibraries)
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(data.libraries, key = { it.id }) { lib ->
                                            val isTv = lib.collectionType == "tvshows"
                                            val isMovie = lib.collectionType == "movies"
                                            Card(
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                                modifier = Modifier
                                                    .clickable {
                                                        when {
                                                            isMovie -> onNavigateMovies()
                                                            isTv -> onNavigateSeries()
                                                            else -> onNavigateMovies()
                                                        }
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = when {
                                                                    isTv -> Icons.Filled.LiveTv
                                                                    isMovie -> Icons.Filled.Movie
                                                                    else -> Icons.Filled.VideoLibrary
                                                                },
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = lib.name,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (data.resumeItems.isNotEmpty()) {
                            item(key = "continue_watching") {
                                Column {
                                    SectionHeader(title = strings.continueWatching)
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(data.resumeItems, key = { "resume_${it.id}" }) { item ->
                                            EpisodeContinueCard(
                                                item = item,
                                                serverUrl = serverUrl,
                                                showProgress = settings.showProgressBars,
                                                onClick = { onPlayDirect(item) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (data.nextUpItems.isNotEmpty()) {
                            item(key = "next_up") {
                                Column {
                                    SectionHeader(title = strings.nextUp)
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(data.nextUpItems, key = { "nextup_${it.id}" }) { item ->
                                            EpisodeContinueCard(
                                                item = item,
                                                serverUrl = serverUrl,
                                                showProgress = settings.showProgressBars,
                                                onClick = { onPlayDirect(item) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (data.latestMovies.isNotEmpty()) {
                            item(key = "latest_movies") {
                                Column {
                                    SectionHeader(
                                        title = strings.latestMovies,
                                        actionLabel = strings.allItems,
                                        onActionClick = onNavigateMovies
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(data.latestMovies, key = { "movie_${it.id}" }) { item ->
                                            MediaPosterCard(
                                                item = item,
                                                serverUrl = serverUrl,
                                                showProgress = settings.showProgressBars,
                                                onClick = { onOpenDetail(item) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (data.latestSeries.isNotEmpty()) {
                            item(key = "latest_series") {
                                Column {
                                    SectionHeader(
                                        title = strings.latestSeries,
                                        actionLabel = strings.allItems,
                                        onActionClick = onNavigateSeries
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(data.latestSeries, key = { "series_${it.id}" }) { item ->
                                            MediaPosterCard(
                                                item = item,
                                                serverUrl = serverUrl,
                                                showProgress = settings.showProgressBars,
                                                onClick = { onOpenDetail(item) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
