package com.jellyarr.player.data.tmdb

import com.jellyarr.player.core.Http
import com.jellyarr.player.core.int
import com.jellyarr.player.core.str
import java.util.concurrent.ConcurrentHashMap

/** Minimal TMDB id mapping, needed because Nuvio scrapers take TMDB ids. */
class TmdbClient(private val apiKey: () -> String) {
    private val cache = ConcurrentHashMap<String, String>()

    val isConfigured: Boolean get() = apiKey().isNotBlank()

    suspend fun imdbToTmdb(imdbId: String, type: String): String? {
        val key = apiKey().ifBlank { return null }
        cache["$type:$imdbId"]?.let { return it }
        val o = Http.getJson("https://api.themoviedb.org/3/find/$imdbId?external_source=imdb_id&api_key=$key")
        val arr = if (type == "series") o.optJSONArray("tv_results") else o.optJSONArray("movie_results")
        val id = arr?.optJSONObject(0)?.int("id")?.toString()
            ?: (o.optJSONArray("movie_results")?.optJSONObject(0) ?: o.optJSONArray("tv_results")?.optJSONObject(0))?.int("id")?.toString()
        if (id != null) cache["$type:$imdbId"] = id
        return id
    }

    suspend fun tmdbToImdb(tmdbId: String, type: String): String? {
        val key = apiKey().ifBlank { return null }
        cache["rev:$type:$tmdbId"]?.let { return it }
        val kind = if (type == "series") "tv" else "movie"
        val o = Http.getJson("https://api.themoviedb.org/3/$kind/$tmdbId/external_ids?api_key=$key")
        val id = o.str("imdb_id")
        if (id != null) cache["rev:$type:$tmdbId"] = id
        return id
    }
}
