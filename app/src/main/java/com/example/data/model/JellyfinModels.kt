package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PublicSystemInfo(
    @Json(name = "LocalAddress") val localAddress: String? = null,
    @Json(name = "ServerName") val serverName: String? = null,
    @Json(name = "Version") val version: String? = null,
    @Json(name = "ProductName") val productName: String? = null,
    @Json(name = "OperatingSystem") val operatingSystem: String? = null,
    @Json(name = "Id") val id: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthenticateRequest(
    @Json(name = "Username") val username: String,
    @Json(name = "Pw") val pw: String
)

@JsonClass(generateAdapter = true)
data class AuthenticateResponse(
    @Json(name = "User") val user: JellyfinUser,
    @Json(name = "AccessToken") val accessToken: String,
    @Json(name = "ServerId") val serverId: String? = null
)

@JsonClass(generateAdapter = true)
data class JellyfinUser(
    @Json(name = "Name") val name: String,
    @Json(name = "Id") val id: String,
    @Json(name = "PrimaryImageTag") val primaryImageTag: String? = null
)

@JsonClass(generateAdapter = true)
data class ItemsResponse(
    @Json(name = "Items") val items: List<BaseItemDto> = emptyList(),
    @Json(name = "TotalRecordCount") val totalRecordCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class BaseItemDto(
    @Json(name = "Name") val name: String = "",
    @Json(name = "OriginalTitle") val originalTitle: String? = null,
    @Json(name = "Id") val id: String = "",
    @Json(name = "Type") val type: String = "",
    @Json(name = "CollectionType") val collectionType: String? = null,
    @Json(name = "Overview") val overview: String? = null,
    @Json(name = "Taglines") val taglines: List<String>? = null,
    @Json(name = "ProductionYear") val productionYear: Int? = null,
    @Json(name = "CommunityRating") val communityRating: Double? = null,
    @Json(name = "OfficialRating") val officialRating: String? = null,
    @Json(name = "RunTimeTicks") val runTimeTicks: Long? = null,
    @Json(name = "Genres") val genres: List<String>? = null,
    @Json(name = "SeriesName") val seriesName: String? = null,
    @Json(name = "SeriesId") val seriesId: String? = null,
    @Json(name = "SeasonName") val seasonName: String? = null,
    @Json(name = "SeasonId") val seasonId: String? = null,
    @Json(name = "ParentIndexNumber") val parentIndexNumber: Int? = null,
    @Json(name = "IndexNumber") val indexNumber: Int? = null,
    @Json(name = "ChildCount") val childCount: Int? = null,
    @Json(name = "ImageTags") val imageTags: Map<String, String>? = null,
    @Json(name = "BackdropImageTags") val backdropImageTags: List<String>? = null,
    @Json(name = "ParentBackdropImageTags") val parentBackdropImageTags: List<String>? = null,
    @Json(name = "ParentBackdropItemId") val parentBackdropItemId: String? = null,
    @Json(name = "SeriesPrimaryImageTag") val seriesPrimaryImageTag: String? = null,
    @Json(name = "UserData") val userData: UserItemDataDto? = null,
    @Json(name = "MediaSources") val mediaSources: List<MediaSourceInfo>? = null,
    @Json(name = "People") val people: List<PersonDto>? = null,
    @Json(name = "Studios") val studios: List<StudioDto>? = null
)

@JsonClass(generateAdapter = true)
data class UserItemDataDto(
    @Json(name = "PlaybackPositionTicks") val playbackPositionTicks: Long = 0L,
    @Json(name = "PlayedPercentage") val playedPercentage: Double? = null,
    @Json(name = "Played") val played: Boolean = false,
    @Json(name = "IsFavorite") val isFavorite: Boolean = false,
    @Json(name = "UnplayedItemCount") val unplayedItemCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class MediaSourceInfo(
    @Json(name = "Id") val id: String = "",
    @Json(name = "Name") val name: String? = null,
    @Json(name = "Container") val container: String? = null,
    @Json(name = "Bitrate") val bitrate: Long? = null,
    @Json(name = "Size") val size: Long? = null,
    @Json(name = "RunTimeTicks") val runTimeTicks: Long? = null,
    @Json(name = "SupportsDirectPlay") val supportsDirectPlay: Boolean = true,
    @Json(name = "SupportsDirectStream") val supportsDirectStream: Boolean = true,
    @Json(name = "SupportsTranscoding") val supportsTranscoding: Boolean = true,
    @Json(name = "TranscodingUrl") val transcodingUrl: String? = null,
    @Json(name = "MediaStreams") val mediaStreams: List<MediaStream>? = null
)

@JsonClass(generateAdapter = true)
data class MediaStream(
    @Json(name = "Type") val type: String = "",
    @Json(name = "Index") val index: Int = 0,
    @Json(name = "Codec") val codec: String? = null,
    @Json(name = "Language") val language: String? = null,
    @Json(name = "DisplayTitle") val displayTitle: String? = null,
    @Json(name = "Title") val title: String? = null,
    @Json(name = "IsDefault") val isDefault: Boolean = false,
    @Json(name = "IsExternal") val isExternal: Boolean = false,
    @Json(name = "DeliveryUrl") val deliveryUrl: String? = null,
    @Json(name = "Width") val width: Int? = null,
    @Json(name = "Height") val height: Int? = null
)

@JsonClass(generateAdapter = true)
data class PersonDto(
    @Json(name = "Name") val name: String = "",
    @Json(name = "Id") val id: String = "",
    @Json(name = "Role") val role: String? = null,
    @Json(name = "Type") val type: String? = null,
    @Json(name = "PrimaryImageTag") val primaryImageTag: String? = null
)

@JsonClass(generateAdapter = true)
data class StudioDto(
    @Json(name = "Name") val name: String = "",
    @Json(name = "Id") val id: String = ""
)

@JsonClass(generateAdapter = true)
data class GenresResponse(
    @Json(name = "Items") val items: List<GenreDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GenreDto(
    @Json(name = "Name") val name: String = "",
    @Json(name = "Id") val id: String = ""
)

@JsonClass(generateAdapter = true)
data class PlaybackInfoRequest(
    @Json(name = "UserId") val userId: String,
    @Json(name = "MaxStreamingBitrate") val maxStreamingBitrate: Long? = null,
    @Json(name = "AutoOpenLiveStream") val autoOpenLiveStream: Boolean = true
)

@JsonClass(generateAdapter = true)
data class PlaybackInfoResponse(
    @Json(name = "MediaSources") val mediaSources: List<MediaSourceInfo> = emptyList(),
    @Json(name = "PlaySessionId") val playSessionId: String? = null
)

@JsonClass(generateAdapter = true)
data class PlaybackStartRequest(
    @Json(name = "ItemId") val itemId: String,
    @Json(name = "MediaSourceId") val mediaSourceId: String? = null,
    @Json(name = "PlaySessionId") val playSessionId: String? = null,
    @Json(name = "PositionTicks") val positionTicks: Long = 0L,
    @Json(name = "IsPaused") val isPaused: Boolean = false,
    @Json(name = "PlayMethod") val playMethod: String = "DirectPlay"
)

@JsonClass(generateAdapter = true)
data class PlaybackProgressRequest(
    @Json(name = "ItemId") val itemId: String,
    @Json(name = "MediaSourceId") val mediaSourceId: String? = null,
    @Json(name = "PlaySessionId") val playSessionId: String? = null,
    @Json(name = "PositionTicks") val positionTicks: Long = 0L,
    @Json(name = "IsPaused") val isPaused: Boolean = false,
    @Json(name = "PlayMethod") val playMethod: String = "DirectPlay"
)

@JsonClass(generateAdapter = true)
data class PlaybackStopRequest(
    @Json(name = "ItemId") val itemId: String,
    @Json(name = "MediaSourceId") val mediaSourceId: String? = null,
    @Json(name = "PlaySessionId") val playSessionId: String? = null,
    @Json(name = "PositionTicks") val positionTicks: Long = 0L
)

// Domain Helper Extensions
fun BaseItemDto.posterUrl(serverUrl: String, maxWidth: Int = 480): String? {
    val cleanBase = serverUrl.trimEnd('/')
    if (cleanBase.isBlank() || id.isBlank()) return null
    val hasPrimary = imageTags?.containsKey("Primary") == true
    return when {
        hasPrimary -> "$cleanBase/Items/$id/Images/Primary?fillWidth=$maxWidth&quality=90&tag=${imageTags?.get("Primary")}"
        type == "Episode" && !seriesId.isNullOrBlank() && !seriesPrimaryImageTag.isNullOrBlank() ->
            "$cleanBase/Items/$seriesId/Images/Primary?fillWidth=$maxWidth&quality=90&tag=$seriesPrimaryImageTag"
        else -> "$cleanBase/Items/$id/Images/Primary?fillWidth=$maxWidth&quality=90"
    }
}

fun BaseItemDto.backdropUrl(serverUrl: String, maxWidth: Int = 1280): String? {
    val cleanBase = serverUrl.trimEnd('/')
    if (cleanBase.isBlank() || id.isBlank()) return null
    return when {
        !backdropImageTags.isNullOrEmpty() ->
            "$cleanBase/Items/$id/Images/Backdrop/0?fillWidth=$maxWidth&quality=85&tag=${backdropImageTags.first()}"
        !parentBackdropItemId.isNullOrBlank() && !parentBackdropImageTags.isNullOrEmpty() ->
            "$cleanBase/Items/$parentBackdropItemId/Images/Backdrop/0?fillWidth=$maxWidth&quality=85&tag=${parentBackdropImageTags.first()}"
        imageTags?.containsKey("Thumb") == true ->
            "$cleanBase/Items/$id/Images/Thumb?fillWidth=$maxWidth&quality=85&tag=${imageTags["Thumb"]}"
        else -> posterUrl(serverUrl, maxWidth)
    }
}

fun BaseItemDto.thumbOrBackdropUrl(serverUrl: String, maxWidth: Int = 720): String? {
    val cleanBase = serverUrl.trimEnd('/')
    if (cleanBase.isBlank() || id.isBlank()) return null
    return when {
        imageTags?.containsKey("Primary") == true && type == "Episode" ->
            "$cleanBase/Items/$id/Images/Primary?fillWidth=$maxWidth&quality=85&tag=${imageTags["Primary"]}"
        imageTags?.containsKey("Thumb") == true ->
            "$cleanBase/Items/$id/Images/Thumb?fillWidth=$maxWidth&quality=85&tag=${imageTags["Thumb"]}"
        else -> backdropUrl(serverUrl, maxWidth)
    }
}

fun PersonDto.avatarUrl(serverUrl: String): String? {
    val cleanBase = serverUrl.trimEnd('/')
    if (cleanBase.isBlank() || id.isBlank() || primaryImageTag.isNullOrBlank()) return null
    return "$cleanBase/Items/$id/Images/Primary?fillWidth=240&quality=85&tag=$primaryImageTag"
}

fun BaseItemDto.durationMinutes(): Int {
    val ticks = runTimeTicks ?: return 0
    return (ticks / 600_000_000L).toInt()
}

fun BaseItemDto.resumePositionMs(): Long {
    val ticks = userData?.playbackPositionTicks ?: 0L
    return ticks / 10_000L
}

fun BaseItemDto.progressFraction(): Float {
    val pct = userData?.playedPercentage
    if (pct != null && pct > 0.0) {
        return (pct / 100.0).toFloat().coerceIn(0f, 1f)
    }
    val total = runTimeTicks ?: return 0f
    val pos = userData?.playbackPositionTicks ?: 0L
    if (total <= 0L || pos <= 0L) return 0f
    return (pos.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
}

fun BaseItemDto.episodeCode(): String? {
    val s = parentIndexNumber ?: return null
    val e = indexNumber ?: return null
    return "S${s.toString().padStart(2, '0')}E${e.toString().padStart(2, '0')}"
}

fun formatDurationMs(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000L
    val hours = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}
