package com.example.data.remote

import com.example.data.model.AuthenticateRequest
import com.example.data.model.AuthenticateResponse
import com.example.data.model.BaseItemDto
import com.example.data.model.GenresResponse
import com.example.data.model.ItemsResponse
import com.example.data.model.PlaybackInfoRequest
import com.example.data.model.PlaybackInfoResponse
import com.example.data.model.PlaybackProgressRequest
import com.example.data.model.PlaybackStartRequest
import com.example.data.model.PlaybackStopRequest
import com.example.data.model.PublicSystemInfo
import com.example.data.model.UserItemDataDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface JellyfinApi {

    @GET
    suspend fun getPublicSystemInfo(@Url fullUrl: String): PublicSystemInfo

    @POST
    suspend fun authenticateByName(
        @Url fullUrl: String,
        @Body request: AuthenticateRequest
    ): AuthenticateResponse

    @GET("Users/{userId}/Views")
    suspend fun getUserViews(
        @Path("userId") userId: String
    ): ItemsResponse

    @GET("Users/{userId}/Items/Resume")
    suspend fun getResumeItems(
        @Path("userId") userId: String,
        @Query("Limit") limit: Int = 15,
        @Query("MediaTypes") mediaTypes: String = "Video",
        @Query("Fields") fields: String = DEFAULT_ITEM_FIELDS
    ): ItemsResponse

    @GET("Shows/NextUp")
    suspend fun getNextUpEpisodes(
        @Query("UserId") userId: String,
        @Query("Limit") limit: Int = 15,
        @Query("Fields") fields: String = DEFAULT_ITEM_FIELDS
    ): ItemsResponse

    @GET("Users/{userId}/Items/Latest")
    suspend fun getLatestItems(
        @Path("userId") userId: String,
        @Query("Limit") limit: Int = 16,
        @Query("IncludeItemTypes") includeItemTypes: String,
        @Query("Fields") fields: String = DEFAULT_ITEM_FIELDS
    ): List<BaseItemDto>

    @GET("Users/{userId}/Items")
    suspend fun getItems(
        @Path("userId") userId: String,
        @Query("IncludeItemTypes") includeItemTypes: String? = null,
        @Query("ParentId") parentId: String? = null,
        @Query("Recursive") recursive: Boolean = true,
        @Query("SortBy") sortBy: String = "DateCreated",
        @Query("SortOrder") sortOrder: String = "Descending",
        @Query("Filters") filters: String? = null,
        @Query("Genres") genres: String? = null,
        @Query("SearchTerm") searchTerm: String? = null,
        @Query("StartIndex") startIndex: Int = 0,
        @Query("Limit") limit: Int = 50,
        @Query("Fields") fields: String = DEFAULT_ITEM_FIELDS
    ): ItemsResponse

    @GET("Users/{userId}/Items/{itemId}")
    suspend fun getItemDetails(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String
    ): BaseItemDto

    @GET("Shows/{seriesId}/Seasons")
    suspend fun getSeasons(
        @Path("seriesId") seriesId: String,
        @Query("UserId") userId: String,
        @Query("Fields") fields: String = "ItemCounts,Overview,UserData"
    ): ItemsResponse

    @GET("Shows/{seriesId}/Episodes")
    suspend fun getEpisodes(
        @Path("seriesId") seriesId: String,
        @Query("SeasonId") seasonId: String,
        @Query("UserId") userId: String,
        @Query("Fields") fields: String = DEFAULT_ITEM_FIELDS
    ): ItemsResponse

    @GET("Items/{itemId}/Similar")
    suspend fun getSimilarItems(
        @Path("itemId") itemId: String,
        @Query("UserId") userId: String,
        @Query("Limit") limit: Int = 12,
        @Query("Fields") fields: String = "ProductionYear,CommunityRating,UserData,ImageTags"
    ): ItemsResponse

    @GET("Genres")
    suspend fun getGenres(
        @Query("UserId") userId: String,
        @Query("IncludeItemTypes") includeItemTypes: String
    ): GenresResponse

    @POST("Users/{userId}/FavoriteItems/{itemId}")
    suspend fun markFavorite(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String
    ): UserItemDataDto

    @DELETE("Users/{userId}/FavoriteItems/{itemId}")
    suspend fun unmarkFavorite(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String
    ): UserItemDataDto

    @POST("Users/{userId}/PlayedItems/{itemId}")
    suspend fun markPlayed(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String
    ): UserItemDataDto

    @DELETE("Users/{userId}/PlayedItems/{itemId}")
    suspend fun unmarkPlayed(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String
    ): UserItemDataDto

    @POST("Items/{itemId}/PlaybackInfo")
    suspend fun getPlaybackInfo(
        @Path("itemId") itemId: String,
        @Query("UserId") userId: String,
        @Body request: PlaybackInfoRequest
    ): PlaybackInfoResponse

    @POST("Sessions/Playing")
    suspend fun reportPlaybackStart(@Body request: PlaybackStartRequest)

    @POST("Sessions/Playing/Progress")
    suspend fun reportPlaybackProgress(@Body request: PlaybackProgressRequest)

    @POST("Sessions/Playing/Stopped")
    suspend fun reportPlaybackStopped(@Body request: PlaybackStopRequest)

    companion object {
        const val DEFAULT_ITEM_FIELDS =
            "Overview,Genres,CommunityRating,OfficialRating,ProductionYear,RunTimeTicks,UserData,MediaSources,People,Taglines,SeriesName,SeriesId,SeasonId,ParentIndexNumber,IndexNumber,ParentBackdropItemId,ParentBackdropImageTags,SeriesPrimaryImageTag"
    }
}
