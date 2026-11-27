package com.jellyarr.player.ui

import com.jellyarr.player.data.jellyfin.JellyfinClient
import com.jellyarr.player.data.jellyfin.JfItem
import com.jellyarr.player.data.stremio.MetaItem

/** Where a card leads. */
sealed interface Target {
    data class Jellyfin(val id: String) : Target
    data class Meta(val type: String, val id: String, val addonUrl: String? = null) : Target
}

data class CardModel(
    val key: String,
    val title: String,
    val subtitle: String? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val logo: String? = null,
    val overview: String? = null,
    val progress: Float? = null,
    val badge: String? = null,
    val rating: String? = null,
    val year: String? = null,
    val genres: List<String> = emptyList(),
    val target: Target,
    val played: Boolean = false,
)

fun JfItem.toCard(jf: JellyfinClient, landscape: Boolean = false): CardModel {
    val isEp = type == "Episode"
    val title = if (isEp) seriesName ?: name else name
    val sub = when {
        isEp -> listOfNotNull(
            com.jellyarr.player.core.Format.episodeCode(parentIndexNumber, indexNumber),
            name.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
        else -> year?.toString()
    }
    return CardModel(
        key = "jf:$id",
        title = title,
        subtitle = sub,
        poster = if (landscape) jf.thumbUrl(this) else jf.seriesPosterUrl(this),
        backdrop = jf.backdropUrl(this),
        logo = jf.logoUrl(this),
        overview = overview,
        progress = progress,
        badge = userData?.unplayedItemCount?.takeIf { it > 0 && type == "Series" }?.toString(),
        rating = communityRating?.let { String.format(java.util.Locale.US, "%.1f", it) },
        year = year?.toString(),
        genres = genres,
        target = Target.Jellyfin(id),
        played = userData?.played == true,
    )
}

fun MetaItem.toCard(addonUrl: String? = null): CardModel = CardModel(
    key = "meta:$type:$id",
    title = name,
    subtitle = releaseInfo,
    poster = poster,
    backdrop = background,
    logo = logo,
    overview = description,
    rating = imdbRating,
    year = year,
    genres = genres,
    target = Target.Meta(type, id, addonUrl),
)
