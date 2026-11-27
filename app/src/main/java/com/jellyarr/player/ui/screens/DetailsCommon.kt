package com.jellyarr.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jellyarr.player.ui.CardModel
import com.jellyarr.player.ui.LocalNavigator
import com.jellyarr.player.ui.components.BackButton
import com.jellyarr.player.ui.components.CardRow
import com.jellyarr.player.ui.components.CenterLoading
import com.jellyarr.player.ui.components.CircleIconButton
import com.jellyarr.player.ui.components.GhostButton
import com.jellyarr.player.ui.components.GradientButton
import com.jellyarr.player.ui.components.MessageState
import com.jellyarr.player.ui.components.NetImage
import com.jellyarr.player.ui.components.ProgressStrip
import com.jellyarr.player.ui.components.SectionHeader
import com.jellyarr.player.ui.components.Tag
import com.jellyarr.player.ui.theme.JColors

data class EpisodeModel(
    val key: String,
    val number: Int?,
    val season: Int?,
    val title: String,
    val overview: String?,
    val thumb: String?,
    val released: String?,
    val runtime: String?,
    val progress: Float?,
    val played: Boolean,
    val jellyfinId: String? = null,
    val stremioId: String? = null,
)

data class SeasonModel(val key: String, val number: Int?, val name: String, val episodes: List<EpisodeModel>?)

data class PersonModel(val name: String, val role: String?, val image: String?)

data class DetailsModel(
    val title: String,
    val logo: String?,
    val backdrop: String?,
    val poster: String?,
    val year: String?,
    val runtime: String?,
    val rating: String?,
    val officialRating: String?,
    val genres: List<String>,
    val overview: String?,
    val tagline: String?,
    val people: List<PersonModel>,
    val isSeries: Boolean,
    val sourceLabel: String,
    val badges: List<String> = emptyList(),
    val progress: Float? = null,
    val favorite: Boolean? = null,
    val played: Boolean? = null,
    val primaryActionLabel: String = "شاهد",
    val secondaryActionLabel: String = "المصادر",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailsLayout(
    model: DetailsModel?,
    loading: Boolean,
    error: String?,
    seasons: List<SeasonModel>,
    selectedSeason: String?,
    onSelectSeason: (SeasonModel) -> Unit,
    onPrimary: () -> Unit,
    onSources: (() -> Unit)?,
    onEpisode: (EpisodeModel) -> Unit,
    onEpisodeSources: ((EpisodeModel) -> Unit)?,
    similar: List<CardModel>,
    onToggleFavorite: (() -> Unit)? = null,
    onTogglePlayed: (() -> Unit)? = null,
    onRetry: () -> Unit = {},
) {
    val nav = LocalNavigator.current
    Box(Modifier.fillMaxSize().background(JColors.Background)) {
        when {
            model == null && loading -> CenterLoading()
            model == null -> MessageState("تعذّر تحميل التفاصيل", error, actionText = "إعادة المحاولة", onAction = onRetry, modifier = Modifier.align(Alignment.Center))
            else -> LazyColumn(Modifier.fillMaxSize()) {
                item(key = "header") { Header(model, onPrimary, onSources, onToggleFavorite, onTogglePlayed) }
                if (model.isSeries && seasons.isNotEmpty()) {
                    item(key = "seasons") {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(seasons, key = { it.key }) { s -> Chip(s.name, s.key == selectedSeason, { onSelectSeason(s) }) }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    val season = seasons.firstOrNull { it.key == selectedSeason }
                    val eps = season?.episodes
                    if (eps == null) item { Box(Modifier.fillMaxWidth().height(120.dp)) { CenterLoading() } }
                    else if (eps.isEmpty()) item { MessageState("لا توجد حلقات", null) }
                    else items(eps, key = { it.key }) { ep -> EpisodeRow(ep, { onEpisode(ep) }, onEpisodeSources?.let { f -> { f(ep) } }) }
                }
                if (model.people.isNotEmpty()) {
                    item(key = "cast") {
                        Spacer(Modifier.height(14.dp))
                        SectionHeader("طاقم العمل")
                        Spacer(Modifier.height(10.dp))
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(model.people.take(30)) { p ->
                                Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    NetImage(p.image, Modifier.size(68.dp).clip(CircleShape).border(1.dp, JColors.OutlineSoft, CircleShape), fallbackText = p.name.take(1))
                                    Spacer(Modifier.height(6.dp))
                                    Text(p.name, color = JColors.Text, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                    p.role?.let { Text(it, color = JColors.TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                }
                            }
                        }
                    }
                }
                if (similar.isNotEmpty()) {
                    item(key = "similar") { CardRow("أعمال مشابهة", similar, { nav.open(it.target) }) }
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
        BackButton({ nav.back() }, Modifier.statusBarsPadding().padding(12.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(
    model: DetailsModel,
    onPrimary: () -> Unit,
    onSources: (() -> Unit)?,
    onToggleFavorite: (() -> Unit)?,
    onTogglePlayed: (() -> Unit)?,
) {
    Column {
        Box(Modifier.fillMaxWidth().height(460.dp)) {
            NetImage(model.backdrop ?: model.poster, Modifier.fillMaxSize(), ContentScale.Crop)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = .5f),
                        0.3f to Color.Transparent,
                        0.7f to JColors.Background.copy(alpha = .75f),
                        1f to JColors.Background,
                    )
                )
            )
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (model.logo != null) {
                    NetImage(model.logo, Modifier.fillMaxWidth(.75f).heightIn(max = 120.dp).height(110.dp), ContentScale.Fit)
                } else {
                    Text(model.title, style = MaterialTheme.typography.displaySmall, color = Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(10.dp))
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                model.rating?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, null, tint = JColors.Gold, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(it, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                model.year?.let { Text(it, color = JColors.TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                model.runtime?.let { Text(it, color = JColors.TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                model.officialRating?.let { Tag(it) }
                Tag(model.sourceLabel, JColors.PrimarySoft, filled = true)
                model.badges.forEach { Tag(it, JColors.Info) }
            }
            if (model.genres.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    model.genres.take(4).joinToString("  •  "), color = JColors.TextMuted, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                GradientButton(model.primaryActionLabel, onPrimary, Modifier.weight(1f), icon = Icons.Filled.PlayArrow)
                if (onSources != null) GhostButton(model.secondaryActionLabel, onSources, icon = Icons.Filled.Stream)
            }
            model.progress?.let {
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp))) { ProgressStrip(it) }
            }
            if (onToggleFavorite != null || onTogglePlayed != null) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    onToggleFavorite?.let {
                        CircleIconButton(
                            if (model.favorite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, it,
                            tint = if (model.favorite == true) JColors.Secondary else Color.White,
                        )
                    }
                    onTogglePlayed?.let {
                        CircleIconButton(
                            if (model.played == true) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle, it,
                            tint = if (model.played == true) JColors.Success else Color.White,
                        )
                    }
                }
            }
            model.tagline?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, color = JColors.PrimarySoft, style = MaterialTheme.typography.titleSmall)
            }
            model.overview?.let { ExpandableText(it) }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ExpandableText(text: String) {
    var expanded by remember { mutableStateOf(false) }
    Spacer(Modifier.height(8.dp))
    Text(
        text, color = JColors.Text.copy(alpha = .86f), style = MaterialTheme.typography.bodyMedium,
        maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.clickable { expanded = !expanded },
    )
}

@Composable
private fun EpisodeRow(ep: EpisodeModel, onClick: () -> Unit, onSources: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(150.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)).border(1.dp, JColors.OutlineSoft, RoundedCornerShape(10.dp))) {
            NetImage(ep.thumb, Modifier.fillMaxSize(), fallbackText = ep.number?.let { "E$it" })
            Box(
                Modifier.align(Alignment.Center).size(34.dp).clip(CircleShape).background(Color.Black.copy(.5f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
            if (ep.played) {
                Icon(Icons.Filled.CheckCircle, null, tint = JColors.Success, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp))
            }
            ep.progress?.let { ProgressStrip(it, Modifier.align(Alignment.BottomCenter)) }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                listOfNotNull(ep.number?.let { "$it." }, ep.title).joinToString(" "),
                color = JColors.Text, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            val meta = listOfNotNull(ep.released?.take(10), ep.runtime).joinToString(" · ")
            if (meta.isNotEmpty()) Text(meta, color = JColors.TextMuted, fontSize = 11.sp)
            ep.overview?.let {
                Text(it, color = JColors.TextMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
            }
        }
        if (onSources != null) {
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(JColors.SurfaceHigh).clickable(onClick = onSources),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Stream, null, tint = JColors.PrimarySoft, modifier = Modifier.size(18.dp)) }
        }
    }
}
