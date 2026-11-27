package com.jellyarr.player.data.stremio

import org.json.JSONObject

object BuiltIn {
    const val CINEMETA_URL = "https://v3-cinemeta.strem.io/manifest.json"

    /** Cinemeta is used as a metadata fallback for IMDb ids even when not installed. */
    val cinemeta: Addon by lazy {
        val json = """
            {"id":"com.linvo.cinemeta","version":"3.0.0","name":"Cinemeta",
             "description":"The official addon for movie and series catalogs",
             "resources":["catalog","meta","addon_catalog"],"types":["movie","series"],"idPrefixes":["tt"],
             "catalogs":[
               {"type":"movie","id":"top","name":"Popular","extra":[{"name":"genre"},{"name":"search"},{"name":"skip"}]},
               {"type":"series","id":"top","name":"Popular","extra":[{"name":"genre"},{"name":"search"},{"name":"skip"}]}
             ]}
        """.trimIndent()
        Addon(CINEMETA_URL, AddonManifest.parse(JSONObject(json)), true)
    }

    data class Suggested(val name: String, val description: String, val url: String?, val configureUrl: String?)

    val suggestedAddons = listOf(
        Suggested("Cinemeta", "كتالوجات وبيانات الأفلام والمسلسلات الرسمية", CINEMETA_URL, null),
        Suggested(
            "AIOStreams",
            "يجمع كل إضافات Stremio وخدمات Debrid في إضافة واحدة. اضبطه ثم انسخ رابط manifest",
            null,
            "https://aiostreams.elfhosted.com/stremio/configure",
        ),
        Suggested("OpenSubtitles v3", "ترجمات لكل اللغات", "https://opensubtitles-v3.strem.io/manifest.json", null),
    )

    const val NUVIO_DEFAULT_REPO = "https://raw.githubusercontent.com/tapframe/nuvio-providers/refs/heads/main/manifest.json"
}
