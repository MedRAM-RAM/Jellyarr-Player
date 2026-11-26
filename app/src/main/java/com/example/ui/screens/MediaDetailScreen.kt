package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.BaseItemDto
import com.example.data.model.ResumeBehavior
import com.example.data.model.UserSettings
import com.example.data.model.avatarUrl
import com.example.data.model.backdropUrl
import com.example.data.model.durationMinutes
import com.example.data.model.formatDurationMs
import com.example.data.model.posterUrl
import com.example.data.model.progressFraction
import com.example.data.model.resumePositionMs
import com.example.data.model.thumbOrBackdropUrl
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MediaPosterCard
import com.example.ui.components.MetaPill
import com.example.ui.components.PosterPlaceholder
import com.example.ui.components.ResumePromptDialog
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AmberStar
import com.example.ui.theme.EmeraldWatched
import com.example.ui.theme.LocalStrings
import com.example.ui.viewmodel.DetailUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaDetailScreen(
    state: DetailUiState,
    settings: UserSettings,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSelectSeason: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePlayed: () -> Unit,
    onPlayItem: (itemId: String, startFromBeginning: Boolean) -> Unit,
    onOpenSimilar: (BaseItemDto) -> Unit
) {
    val strings = LocalStrings.current
    val serverUrl = settings.activeServer?.url.orEmpty()
    var resumePromptTarget by remember { mutableStateOf<Pair<String, Long>?>(null) }

    fun triggerPlayback(targetItem: BaseItemDto) {
        val posMs = targetItem.resumePositionMs()
        if (posMs > 10_000L) {
            when (settings.resumeBehavior) {
                ResumeBehavior.AUTO_RESUME -> onPlayItem(targetItem.id, false)
                ResumeBehavior.START_OVER -> onPlayItem(targetItem.id, true)
                ResumeBehavior.ASK -> resumePromptTarget = targetItem.id to posMs
            }
        } else {
            onPlayItem(targetItem.id, true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null || state.bundle == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateView(
                        title = strings.errorConnectionFailed,
                        subtitle = state.error.orEmpty(),
                        actionLabel = strings.retry,
                        onAction = onRetry
                    )
                }
            }
            else -> {
                val bundle = state.bundle
                val item = bundle.item

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Backdrop Hero Header
                    item(key = "detail_hero") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        ) {
                            SubcomposeAsyncImage(
                                model = item.backdropUrl(serverUrl),
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                loading = { PosterPlaceholder(item.name) },
                                error = { PosterPlaceholder(item.name) }
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0x55080B13),
                                                Color(0x44080B13),
                                                MaterialTheme.colorScheme.background
                                            )
                                        )
                                    )
                            )

                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Card(
                                    shape = MaterialTheme.shapes.medium,
                                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                                    modifier = Modifier
                                        .width(106.dp)
                                        .aspectRatio(2f / 3f)
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

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!item.taglines.isNullOrEmpty()) {
                                        Text(
                                            text = item.taglines.first(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        item.productionYear?.let { MetaPill(it.toString()) }
                                        item.officialRating?.let { MetaPill(it) }
                                        val mins = item.durationMinutes()
                                        if (mins > 0) MetaPill("$mins ${strings.minutesUnit}")
                                        item.communityRating?.let { rating ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = AmberStar.copy(alpha = 0.2f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Star,
                                                        contentDescription = null,
                                                        tint = AmberStar,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = String.format("%.1f", rating),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = AmberStar
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Primary Actions Row
                    item(key = "detail_actions") {
                        val isSeries = item.type == "Series"
                        val primaryPlayTarget = if (isSeries) state.episodes.firstOrNull() else item
                        val isFav = item.userData?.isFavorite == true
                        val isPlayed = item.userData?.played == true

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (primaryPlayTarget != null) {
                                val resumeMs = primaryPlayTarget.resumePositionMs()
                                Button(
                                    onClick = { triggerPlayback(primaryPlayTarget) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("detail_play_button")
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (resumeMs > 10_000L) {
                                            "${strings.resume} (${formatDurationMs(resumeMs)})"
                                        } else {
                                            strings.play
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            FilledTonalIconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier
                                    .size(50.dp)
                                    .testTag("detail_favorite_button")
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = if (isFav) strings.unfavorite else strings.favorite,
                                    tint = if (isFav) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            FilledTonalIconButton(
                                onClick = onTogglePlayed,
                                modifier = Modifier
                                    .size(50.dp)
                                    .testTag("detail_watched_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlayed) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                                    contentDescription = if (isPlayed) strings.markUnwatched else strings.markWatched,
                                    tint = if (isPlayed) EmeraldWatched else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Genres
                    if (!item.genres.isNullOrEmpty()) {
                        item(key = "detail_genres") {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item.genres.forEach { genre ->
                                    AssistChip(onClick = {}, label = { Text(genre) })
                                }
                            }
                        }
                    }

                    // Overview
                    if (!item.overview.isNullOrBlank()) {
                        item(key = "detail_overview") {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                Text(
                                    text = strings.overview,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.overview,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Seasons & Episodes (for Series)
                    if (item.type == "Series" && bundle.seasons.isNotEmpty()) {
                        item(key = "seasons_selector") {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                SectionHeader(title = strings.seasons)
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(bundle.seasons) { idx, season ->
                                        FilterChip(
                                            selected = idx == state.selectedSeasonIndex,
                                            onClick = { onSelectSeason(idx) },
                                            label = {
                                                Text(
                                                    season.name.ifBlank { "${strings.seasonPrefix} ${idx + 1}" }
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (state.isLoadingEpisodes) {
                            item(key = "episodes_loading") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        } else {
                            items(state.episodes, key = { "ep_${it.id}" }) { episode ->
                                EpisodeRowCard(
                                    episode = episode,
                                    serverUrl = serverUrl,
                                    showProgress = settings.showProgressBars,
                                    onClick = { triggerPlayback(episode) },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Cast & Crew
                    val people = item.people?.filter { !it.name.isBlank() }?.take(15).orEmpty()
                    if (people.isNotEmpty()) {
                        item(key = "cast_row") {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                SectionHeader(title = strings.castAndCrew)
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(people, key = { "${it.id}_${it.name}" }) { person ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(84.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(68.dp)
                                            ) {
                                                SubcomposeAsyncImage(
                                                    model = person.avatarUrl(serverUrl),
                                                    contentDescription = person.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize(),
                                                    error = {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                Icons.Filled.Person,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    },
                                                    loading = {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                Icons.Filled.Person,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = person.name,
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            person.role?.takeIf { it.isNotBlank() }?.let { role ->
                                                Text(
                                                    text = role,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Similar Titles
                    if (bundle.similarItems.isNotEmpty()) {
                        item(key = "similar_titles") {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                SectionHeader(title = strings.similarTitles)
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(bundle.similarItems, key = { "sim_${it.id}" }) { sim ->
                                        MediaPosterCard(
                                            item = sim,
                                            serverUrl = serverUrl,
                                            showProgress = settings.showProgressBars,
                                            onClick = { onOpenSimilar(sim) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Back Button
        FilledTonalIconButton(
            onClick = onBack,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = Color(0x880B0E17),
                contentColor = Color.White
            ),
            modifier = Modifier
                .statusBarsPadding()
                .padding(12.dp)
                .align(Alignment.TopStart)
                .testTag("detail_back_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.play)
        }
    }

    resumePromptTarget?.let { (targetId, posMs) ->
        ResumePromptDialog(
            resumePositionMs = posMs,
            onResume = {
                resumePromptTarget = null
                onPlayItem(targetId, false)
            },
            onStartOver = {
                resumePromptTarget = null
                onPlayItem(targetId, true)
            },
            onDismiss = { resumePromptTarget = null }
        )
    }
}

@Composable
private fun EpisodeRowCard(
    episode: BaseItemDto,
    serverUrl: String,
    showProgress: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val progress = episode.progressFraction()

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("episode_card_${episode.id}")
    ) {
        Column {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(134.dp)
                        .aspectRatio(16f / 9f)
                        .clip(MaterialTheme.shapes.small)
                ) {
                    SubcomposeAsyncImage(
                        model = episode.thumbOrBackdropUrl(serverUrl),
                        contentDescription = episode.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = { PosterPlaceholder(episode.name) },
                        error = { PosterPlaceholder(episode.name) }
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color(0xAA000000),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = strings.play,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (episode.userData?.played == true) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = strings.markWatched,
                            tint = EmeraldWatched,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val epPrefix = episode.indexNumber?.let { "E${it.toString().padStart(2, '0')} • " } ?: ""
                    Text(
                        text = "$epPrefix${episode.name}",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val mins = episode.durationMinutes()
                    if (mins > 0) {
                        Text(
                            text = "$mins ${strings.minutesUnit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    episode.overview?.takeIf { it.isNotBlank() }?.let { ov ->
                        Text(
                            text = ov,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (showProgress && progress > 0.01f && episode.userData?.played != true) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
