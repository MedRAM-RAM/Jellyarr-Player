package com.jellyarr.player.data.jellyfin

import com.jellyarr.player.core.arr
import com.jellyarr.player.core.bool
import com.jellyarr.player.core.dbl
import com.jellyarr.player.core.int
import com.jellyarr.player.core.long
import com.jellyarr.player.core.mapObjects
import com.jellyarr.player.core.obj
import com.jellyarr.player.core.str
import com.jellyarr.player.core.strings
import org.json.JSONObject

data class JfUserData(
    val played: Boolean,
    val isFavorite: Boolean,
    val playbackPositionTicks: Long,
    val playedPercentage: Double?,
    val unplayedItemCount: Int?,
)

data class JfMediaStream(
    val index: Int,
    val type: String,
    val codec: String?,
    val language: String?,
    val displayTitle: String?,
    val isExternal: Boolean,
    val height: Int?,
    val channels: Int?,
    val videoRange: String?,
)

data class JfMediaSource(
    val id: String,
    val name: String?,
    val container: String?,
    val size: Long?,
    val bitrate: Long?,
    val supportsDirectPlay: Boolean,
    val supportsDirectStream: Boolean,
    val streams: List<JfMediaStream>,
)

data class JfPerson(val id: String, val name: String, val role: String?, val type: String?, val imageTag: String?)

data class JfItem(
    val id: String,
    val name: String,
    val type: String,
    val overview: String?,
    val year: Int?,
    val premiereDate: String?,
    val officialRating: String?,
    val communityRating: Double?,
    val criticRating: Double?,
    val runTimeTicks: Long?,
    val genres: List<String>,
    val studios: List<String>,
    val seriesId: String?,
    val seriesName: String?,
    val seasonId: String?,
    val indexNumber: Int?,
    val parentIndexNumber: Int?,
    val collectionType: String?,
    val primaryTag: String?,
    val backdropTags: List<String>,
    val parentBackdropItemId: String?,
    val parentBackdropTags: List<String>,
    val logoTag: String?,
    val parentLogoItemId: String?,
    val thumbTag: String?,
    val seriesPrimaryTag: String?,
    val primaryAspect: Double?,
    val userData: JfUserData?,
    val providerIds: Map<String, String>,
    val mediaSources: List<JfMediaSource>,
    val people: List<JfPerson>,
    val childCount: Int?,
    val taglines: List<String>,
    val status: String?,
    val endDate: String?,
) {
    val isFolderLike: Boolean get() = type in setOf("Series", "Season", "BoxSet", "Folder", "CollectionFolder", "UserView")
    val isPlayable: Boolean get() = type in setOf("Movie", "Episode", "Video", "MusicVideo")
    val runtimeMinutes: Int? get() = runTimeTicks?.let { (it / 600_000_000L).toInt() }
    val imdbId: String? get() = providerIds.entries.firstOrNull { it.key.equals("Imdb", true) }?.value
    val tmdbId: String? get() = providerIds.entries.firstOrNull { it.key.equals("Tmdb", true) }?.value
    val progress: Float?
        get() {
            val pos = userData?.playbackPositionTicks ?: 0
            val total = runTimeTicks ?: 0
            return if (pos > 0 && total > 0) (pos.toFloat() / total).coerceIn(0f, 1f) else null
        }

    companion object {
        fun parse(o: JSONObject): JfItem {
            val ud = o.obj("UserData")?.let {
                JfUserData(
                    played = it.bool("Played") ?: false,
                    isFavorite = it.bool("IsFavorite") ?: false,
                    playbackPositionTicks = it.long("PlaybackPositionTicks") ?: 0,
                    playedPercentage = it.dbl("PlayedPercentage"),
                    unplayedItemCount = it.int("UnplayedItemCount"),
                )
            }
            val provider = mutableMapOf<String, String>()
            o.obj("ProviderIds")?.let { p -> p.keys().forEach { k -> p.str(k)?.let { provider[k] = it } } }
            val tags = o.obj("ImageTags")
            return JfItem(
                id = o.optString("Id"),
                name = o.str("Name") ?: "",
                type = o.str("Type") ?: "",
                overview = o.str("Overview"),
                year = o.int("ProductionYear"),
                premiereDate = o.str("PremiereDate"),
                officialRating = o.str("OfficialRating"),
                communityRating = o.dbl("CommunityRating"),
                criticRating = o.dbl("CriticRating"),
                runTimeTicks = o.long("RunTimeTicks"),
                genres = o.arr("Genres").strings(),
                studios = o.arr("Studios").strings(),
                seriesId = o.str("SeriesId"),
                seriesName = o.str("SeriesName"),
                seasonId = o.str("SeasonId"),
                indexNumber = o.int("IndexNumber"),
                parentIndexNumber = o.int("ParentIndexNumber"),
                collectionType = o.str("CollectionType"),
                primaryTag = tags?.str("Primary"),
                backdropTags = o.arr("BackdropImageTags").strings(),
                parentBackdropItemId = o.str("ParentBackdropItemId"),
                parentBackdropTags = o.arr("ParentBackdropImageTags").strings(),
                logoTag = tags?.str("Logo") ?: o.str("ParentLogoImageTag"),
                parentLogoItemId = if (tags?.str("Logo") != null) null else o.str("ParentLogoItemId"),
                thumbTag = tags?.str("Thumb"),
                seriesPrimaryTag = o.str("SeriesPrimaryImageTag"),
                primaryAspect = o.dbl("PrimaryImageAspectRatio"),
                userData = ud,
                providerIds = provider,
                mediaSources = o.arr("MediaSources").mapObjects { ms ->
                    JfMediaSource(
                        id = ms.optString("Id"),
                        name = ms.str("Name"),
                        container = ms.str("Container"),
                        size = ms.long("Size"),
                        bitrate = ms.long("Bitrate"),
                        supportsDirectPlay = ms.bool("SupportsDirectPlay") ?: true,
                        supportsDirectStream = ms.bool("SupportsDirectStream") ?: true,
                        streams = ms.arr("MediaStreams").mapObjects { s ->
                            JfMediaStream(
                                index = s.int("Index") ?: 0,
                                type = s.str("Type") ?: "",
                                codec = s.str("Codec"),
                                language = s.str("Language"),
                                displayTitle = s.str("DisplayTitle"),
                                isExternal = s.bool("IsExternal") ?: false,
                                height = s.int("Height"),
                                channels = s.int("Channels"),
                                videoRange = s.str("VideoRangeType") ?: s.str("VideoRange"),
                            )
                        },
                    )
                },
                people = o.arr("People").mapObjects { p ->
                    JfPerson(p.optString("Id"), p.optString("Name"), p.str("Role"), p.str("Type"), p.str("PrimaryImageTag"))
                },
                childCount = o.int("ChildCount"),
                taglines = o.arr("Taglines").strings(),
                status = o.str("Status"),
                endDate = o.str("EndDate"),
            )
        }
    }
}

data class JfPage(val items: List<JfItem>, val total: Int)
