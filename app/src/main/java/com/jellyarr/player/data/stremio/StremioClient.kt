package com.jellyarr.player.data.stremio

import com.jellyarr.player.core.Http
import com.jellyarr.player.core.mapObjects
import com.jellyarr.player.core.urlEncode
import org.json.JSONObject

object StremioClient {

    /** Accepts stremio://, http(s) urls, with or without /manifest.json */
    fun normalizeManifestUrl(input: String): String {
        var u = input.trim()
        if (u.startsWith("stremio://", ignoreCase = true)) u = "https://" + u.substring("stremio://".length)
        if (!u.startsWith("http://", true) && !u.startsWith("https://", true)) u = "https://$u"
        val path = u.substringBefore("?")
        if (!path.endsWith("/manifest.json")) {
            u = path.trimEnd('/') + "/manifest.json" + (u.substringAfter("?", "").let { if (it.isEmpty()) "" else "?$it" })
        }
        return u
    }

    suspend fun fetchManifest(url: String): Pair<String, AddonManifest> {
        val normalized = normalizeManifestUrl(url)
        val text = Http.getText(normalized)
        val manifest = AddonManifest.parse(JSONObject(text))
        require(manifest.id.isNotBlank()) { "ملف manifest غير صالح" }
        return text to manifest
    }

    private fun extraPath(extra: Map<String, String>): String =
        if (extra.isEmpty()) "" else "/" + extra.entries.joinToString("&") { "${it.key}=${it.value.urlEncode()}" }

    suspend fun catalog(addon: Addon, type: String, id: String, extra: Map<String, String> = emptyMap()): List<MetaItem> {
        val url = "${addon.baseUrl}/catalog/$type/${id.urlEncode()}${extraPath(extra)}.json"
        return Http.getJson(url).optJSONArray("metas").mapObjects { m ->
            runCatching { MetaItem.parse(m) }.getOrNull()?.let { if (it.type.isBlank()) it.copy(type = type) else it }
        }
    }

    suspend fun meta(addon: Addon, type: String, id: String): MetaItem? {
        val url = "${addon.baseUrl}/meta/$type/${id.urlEncode()}.json"
        val o = Http.getJson(url).optJSONObject("meta") ?: return null
        return MetaItem.parse(o)
    }

    suspend fun streams(addon: Addon, type: String, id: String): List<StremioStream> {
        val url = "${addon.baseUrl}/stream/$type/${id.urlEncode()}.json"
        return Http.getJson(url).optJSONArray("streams").mapObjects { runCatching { StremioStream.parse(it) }.getOrNull() }
    }

    suspend fun subtitles(addon: Addon, type: String, id: String): List<StremioSubtitle> {
        val url = "${addon.baseUrl}/subtitles/$type/${id.urlEncode()}.json"
        return Http.getJson(url).optJSONArray("subtitles").mapObjects { s ->
            val u = s.optString("url").takeIf { it.isNotBlank() } ?: return@mapObjects null
            StremioSubtitle(s.optString("id", u), u, s.optString("lang"))
        }
    }
}
