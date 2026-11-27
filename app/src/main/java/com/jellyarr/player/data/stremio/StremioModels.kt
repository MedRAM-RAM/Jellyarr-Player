package com.jellyarr.player.data.stremio

import com.jellyarr.player.core.arr
import com.jellyarr.player.core.bool
import com.jellyarr.player.core.int
import com.jellyarr.player.core.long
import com.jellyarr.player.core.mapObjects
import com.jellyarr.player.core.obj
import com.jellyarr.player.core.str
import com.jellyarr.player.core.strings
import org.json.JSONArray
import org.json.JSONObject

data class CatalogExtra(val name: String, val isRequired: Boolean, val options: List<String>)

data class AddonCatalog(
    val type: String,
    val id: String,
    val name: String,
    val extra: List<CatalogExtra>,
) {
    val supportsSearch: Boolean get() = extra.any { it.name == "search" }
    val requiresExtra: Boolean get() = extra.any { it.isRequired }
    val genres: List<String> get() = extra.firstOrNull { it.name == "genre" }?.options.orEmpty()
    val supportsSkip: Boolean get() = extra.any { it.name == "skip" }
    /** A catalog suitable for the home/discover feed without user input. */
    val isBrowsable: Boolean get() = !extra.any { it.isRequired && it.name != "genre" } && !(extra.any { it.name == "search" && it.isRequired })
}

data class AddonResource(val name: String, val types: List<String>?, val idPrefixes: List<String>?)

data class AddonManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String?,
    val logo: String?,
    val background: String?,
    val types: List<String>,
    val idPrefixes: List<String>?,
    val catalogs: List<AddonCatalog>,
    val resources: List<AddonResource>,
    val configurable: Boolean,
) {
    fun supports(resource: String, type: String, id: String): Boolean {
        val r = resources.firstOrNull { it.name == resource } ?: return false
        val types = r.types ?: this.types
        if (types.isNotEmpty() && type !in types) return false
        val prefixes = r.idPrefixes ?: idPrefixes
        if (prefixes.isNullOrEmpty()) return true
        return prefixes.any { id.startsWith(it) }
    }

    fun hasResource(resource: String) = resources.any { it.name == resource }

    companion object {
        fun parse(o: JSONObject): AddonManifest {
            val res = mutableListOf<AddonResource>()
            val rawRes = o.optJSONArray("resources") ?: JSONArray()
            for (i in 0 until rawRes.length()) {
                when (val r = rawRes.opt(i)) {
                    is String -> res += AddonResource(r, null, null)
                    is JSONObject -> res += AddonResource(
                        r.optString("name"),
                        r.arr("types")?.strings(),
                        r.arr("idPrefixes")?.strings(),
                    )
                }
            }
            return AddonManifest(
                id = o.optString("id"),
                name = o.str("name") ?: o.optString("id"),
                version = o.optString("version"),
                description = o.str("description"),
                logo = o.str("logo") ?: o.str("icon"),
                background = o.str("background"),
                types = o.arr("types").strings(),
                idPrefixes = o.arr("idPrefixes")?.strings(),
                catalogs = o.arr("catalogs").mapObjects { c ->
                    val extras = mutableListOf<CatalogExtra>()
                    c.arr("extra").mapObjects { e ->
                        CatalogExtra(e.optString("name"), e.bool("isRequired") ?: false, e.arr("options").strings())
                    }.let(extras::addAll)
                    // legacy format
                    c.arr("extraRequired").strings().forEach { n -> if (extras.none { it.name == n }) extras += CatalogExtra(n, true, emptyList()) }
                    c.arr("extraSupported").strings().forEach { n -> if (extras.none { it.name == n }) extras += CatalogExtra(n, false, emptyList()) }
                    AddonCatalog(c.optString("type"), c.optString("id"), c.str("name") ?: c.optString("id"), extras)
                },
                resources = res,
                configurable = o.obj("behaviorHints")?.bool("configurable") ?: false,
            )
        }
    }
}

data class Addon(val transportUrl: String, val manifest: AddonManifest, val enabled: Boolean) {
    /** Base url without the trailing /manifest.json */
    val baseUrl: String get() = transportUrl.substringBefore("?").removeSuffix("/manifest.json").trimEnd('/')
    val configureUrl: String get() = "$baseUrl/configure"
    val key: String get() = transportUrl
}

data class MetaLink(val name: String, val category: String, val url: String)

data class MetaVideo(
    val id: String,
    val title: String,
    val season: Int?,
    val episode: Int?,
    val released: String?,
    val overview: String?,
    val thumbnail: String?,
)

data class MetaItem(
    val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val posterShape: String?,
    val background: String?,
    val logo: String?,
    val description: String?,
    val releaseInfo: String?,
    val imdbRating: String?,
    val runtime: String?,
    val genres: List<String>,
    val cast: List<String>,
    val director: List<String>,
    val videos: List<MetaVideo>,
    val trailerYt: String?,
    val imdbId: String?,
    val links: List<MetaLink>,
) {
    val year: String? get() = releaseInfo?.take(4)?.takeIf { it.all(Char::isDigit) }

    companion object {
        fun parse(o: JSONObject): MetaItem {
            val links = o.arr("links").mapObjects { l ->
                MetaLink(l.optString("name"), l.optString("category"), l.optString("url"))
            }
            val genres = o.arr("genres").strings().ifEmpty { links.filter { it.category == "Genres" }.map { it.name } }
            val cast = o.arr("cast").strings().ifEmpty { links.filter { it.category == "Cast" }.map { it.name } }
            val director = o.arr("director").strings().ifEmpty { links.filter { it.category == "Directors" }.map { it.name } }
            val id = o.optString("id")
            return MetaItem(
                id = id,
                type = o.optString("type"),
                name = o.str("name") ?: "",
                poster = o.str("poster"),
                posterShape = o.str("posterShape"),
                background = o.str("background"),
                logo = o.str("logo"),
                description = o.str("description"),
                releaseInfo = o.str("releaseInfo") ?: o.str("year") ?: o.str("released")?.take(4),
                imdbRating = o.str("imdbRating"),
                runtime = o.str("runtime"),
                genres = genres,
                cast = cast,
                director = director,
                videos = o.arr("videos").mapObjects { v ->
                    MetaVideo(
                        id = v.optString("id"),
                        title = v.str("title") ?: v.str("name") ?: "",
                        season = v.int("season"),
                        episode = v.int("episode") ?: v.int("number"),
                        released = v.str("released") ?: v.str("firstAired"),
                        overview = v.str("overview") ?: v.str("description"),
                        thumbnail = v.str("thumbnail"),
                    )
                },
                trailerYt = o.arr("trailerStreams").mapObjects { it.str("ytId") }.firstOrNull()
                    ?: o.arr("trailers").mapObjects { it.str("source") }.firstOrNull(),
                imdbId = o.str("imdb_id") ?: id.takeIf { it.startsWith("tt") },
                links = links,
            )
        }
    }
}

data class StremioSubtitle(val id: String, val url: String, val lang: String)

data class StremioStream(
    val name: String?,
    val title: String?,
    val description: String?,
    val url: String?,
    val infoHash: String?,
    val fileIdx: Int?,
    val externalUrl: String?,
    val ytId: String?,
    val filename: String?,
    val videoSize: Long?,
    val bingeGroup: String?,
    val notWebReady: Boolean,
    val requestHeaders: Map<String, String>,
    val subtitles: List<StremioSubtitle>,
) {
    companion object {
        fun parse(o: JSONObject): StremioStream {
            val hints = o.obj("behaviorHints")
            val headers = mutableMapOf<String, String>()
            hints?.obj("proxyHeaders")?.obj("request")?.let { r -> r.keys().forEach { k -> r.str(k)?.let { headers[k] = it } } }
            o.obj("headers")?.let { r -> r.keys().forEach { k -> r.str(k)?.let { headers[k] = it } } }
            return StremioStream(
                name = o.str("name"),
                title = o.str("title"),
                description = o.str("description"),
                url = o.str("url"),
                infoHash = o.str("infoHash"),
                fileIdx = o.int("fileIdx"),
                externalUrl = o.str("externalUrl"),
                ytId = o.str("ytId"),
                filename = hints?.str("filename"),
                videoSize = hints?.long("videoSize"),
                bingeGroup = hints?.str("bingeGroup"),
                notWebReady = hints?.bool("notWebReady") ?: false,
                requestHeaders = headers,
                subtitles = o.arr("subtitles").mapObjects { s ->
                    val u = s.str("url") ?: return@mapObjects null
                    StremioSubtitle(s.optString("id", u), u, s.optString("lang"))
                },
            )
        }
    }
}
