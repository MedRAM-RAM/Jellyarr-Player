package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.model.BaseItemDto
import com.example.data.model.backdropUrl
import com.example.data.model.durationMinutes
import com.example.data.model.episodeCode
import com.example.data.model.formatDurationMs
import com.example.data.model.posterUrl
import com.example.data.model.progressFraction
import com.example.data.model.resumePositionMs
import com.example.data.model.thumbOrBackdropUrl
import com.example.ui.theme.AmberStar
import com.example.ui.theme.EmeraldWatched
import com.example.ui.theme.LocalStrings

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun MediaPosterCard(
    item: BaseItemDto,
    serverUrl: String,
    showProgress: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 132.dp
) {
    val strings = LocalStrings.current
    val progress = item.progressFraction()

    Column(
        modifier = modifier
            .width(width)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("poster_card_${item.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            SubcomposeAsyncImage(
                model = item.posterUrl(serverUrl),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { PosterPlaceholder(item.name) },
                error = { PosterPlaceholder(item.name) }
            )

            // Bottom subtle gradient for badge legibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC070910))
                        )
                    )
            )

            // Rating Badge
            item.communityRating?.let { rating ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xBB0D111C),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = AmberStar,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = String.format("%.1f", rating),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            // Watched or Unwatched Episode Count Badge
            if (item.userData?.played == true) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = strings.markWatched,
                    tint = EmeraldWatched,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(20.dp)
                        .background(Color(0x99000000), CircleShape)
                )
            } else if (item.type == "Series" && (item.userData?.unplayedItemCount ?: 0) > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "${item.userData?.unplayedItemCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (item.userData?.isFavorite == true) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = strings.favorite,
                    tint = Color(0xFFFF4081),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(16.dp)
                )
            }

            if (showProgress && progress > 0.01f && item.userData?.played != true) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color(0x55FFFFFF)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val meta = buildString {
            item.productionYear?.let { append(it) }
            val mins = item.durationMinutes()
            if (mins > 0 && item.type == "Movie") {
                if (isNotEmpty()) append(" • ")
                append("$mins ${strings.minutesUnit}")
            }
        }
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EpisodeContinueCard(
    item: BaseItemDto,
    serverUrl: String,
    showProgress: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val progress = item.progressFraction()

    Card(
        modifier = modifier
            .width(248.dp)
            .clickable(onClick = onClick)
            .testTag("continue_card_${item.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                SubcomposeAsyncImage(
                    model = item.thumbOrBackdropUrl(serverUrl),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { PosterPlaceholder(item.seriesName ?: item.name) },
                    error = { PosterPlaceholder(item.seriesName ?: item.name) }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xBB070910))
                            )
                        )
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = strings.play,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                item.episodeCode()?.let { code ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xCC0D111C),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = code,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (showProgress && progress > 0.01f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color(0x55FFFFFF)
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    text = item.seriesName ?: item.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = if (!item.seriesName.isNullOrBlank()) item.name else {
                    val pos = item.resumePositionMs()
                    if (pos > 0) "${strings.resumeAt} ${formatDurationMs(pos)}" else ""
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun HeroCarousel(
    items: List<BaseItemDto>,
    serverUrl: String,
    onWatchClick: (BaseItemDto) -> Unit,
    onDetailsClick: (BaseItemDto) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val strings = LocalStrings.current
    val pagerState = rememberPagerState(pageCount = { items.size })

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
        ) { page ->
            val item = items[page]
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onDetailsClick(item) },
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
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
                                        Color(0x22070910),
                                        Color(0x66070910),
                                        Color(0xEE080B13)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item.productionYear?.let { year ->
                                MetaPill(text = year.toString())
                            }
                            item.communityRating?.let { rating ->
                                MetaPill(text = "★ ${String.format("%.1f", rating)}")
                            }
                            item.genres?.take(2)?.forEach { genre ->
                                MetaPill(text = genre)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        item.overview?.takeIf { it.isNotBlank() }?.let { overview ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = overview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD0D8EC),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    if (item.type == "Series") onDetailsClick(item) else onWatchClick(item)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("hero_watch_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (item.type == "Series") strings.details else strings.watchNow,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            FilledTonalButton(
                                onClick = { onDetailsClick(item) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0x55FFFFFF),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = strings.details)
                            }
                        }
                    }
                }
            }
        }

        if (items.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(items.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(if (selected) 20.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun MetaPill(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0x44FFFFFF),
        border = BorderStroke(0.5.dp, Color(0x44FFFFFF)),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun PosterPlaceholder(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Movie,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_library),
            contentDescription = stringResource(R.string.cd_empty_library),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(180.dp)
                .clip(MaterialTheme.shapes.large)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(onClick = onAction) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun ResumePromptDialog(
    resumePositionMs: Long,
    onResume: () -> Unit,
    onStartOver: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.resumePromptTitle) },
        text = {
            Text("${strings.resumeFromPosition} ${formatDurationMs(resumePositionMs)}؟")
        },
        confirmButton = {
            Button(onClick = onResume) {
                Text("${strings.resumeFromPosition} ${formatDurationMs(resumePositionMs)}")
            }
        },
        dismissButton = {
            TextButton(onClick = onStartOver) {
                Text(strings.startFromBeginning)
            }
        }
    )
}
