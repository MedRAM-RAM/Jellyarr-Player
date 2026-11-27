package com.jellyarr.player.data.nuvio

import com.jellyarr.player.core.Http
import com.jellyarr.player.core.arr
import com.jellyarr.player.core.bool
import com.jellyarr.player.core.mapObjects
import com.jellyarr.player.core.str
import com.jellyarr.player.core.strings
import com.jellyarr.player.data.SettingsStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

data class NuvioScraper(
    val id: String,
    val name: String,
    val description: String?,
    val version: String?,
    val author: String?,
    val logo: String?,
    val supportedTypes: List<String>,
    val fileUrl: String,
    val repoUrl: String,
    val repoName: String,
    val defaultEnabled: Boolean,
    val languages: List<String>,
) {
    /** Globally unique key used to store enabled/disabled state. */
    val key: String get() = "$repoUrl#$id"
    fun supports(type: String): Boolean {
        val t = if (type == "series") "tv" else type
        return supportedTypes.isEmpty() || supportedTypes.any { it.equals(t, true) || (t == "tv" && it.equals("series", true)) }
    }
}

data class NuvioRepo(val url: String, val name: String, val scrapers: List<NuvioScraper>, val error: String? = null)

class NuvioRepository(private val settings: SettingsStore, private val engine: NuvioEngine) {

    private val _repos = MutableStateFlow<List<NuvioRepo>>(emptyList())
    val repos: StateFlow<List<NuvioRepo>> = _repos.asStateFlow()
    private val codeCache = ConcurrentHashMap<String, String>()
    private val loadLock = Mutex()
    private var loadedFor: List<String>? = null

    companion object {
        fun manifestUrl(input: String): String {
            var u = input.trim()
            if (!u.startsWith("http://", true) && !u.startsWith("https://", true)) u = "https://$u"
            // github.com/<user>/<repo> → raw manifest on default branch
            Regex("^https?://github\\.com/([^/]+)/([^/]+?)(?:\\.git)?/?$").find(u)?.let {
                return "https://raw.githubusercontent.com/${it.groupValues[1]}/${it.groupValues[2]}/main/manifest.json"
            }
            return if (u.substringBefore("?").endsWith(".json")) u else u.trimEnd('/') + "/manifest.json"
        }
    }

    suspend fun ensureLoaded(force: Boolean = false) = loadLock.withLock {
        val urls = settings.current().nuvioRepos
        if (!force && loadedFor == urls) return@withLock
        _repos.value = coroutineScope {
            urls.map { url -> async { loadRepo(url) } }.map { it.await() }
        }
        loadedFor = urls
    }

    private suspend fun loadRepo(url: String): NuvioRepo = try {
        val manifest = manifestUrl(url)
        val base = manifest.substringBeforeLast('/')
        val o = Http.getJson(manifest)
        val repoName = o.str("name") ?: base.substringAfter("://").substringBefore('/')
        val scrapers = o.arr("scrapers").mapObjects { s ->
            val file = s.str("filename") ?: s.str("file") ?: return@mapObjects null
            NuvioScraper(
                id = s.str("id") ?: file,
                name = s.str("name") ?: s.optString("id"),
                description = s.str("description"),
                version = s.str("version"),
                author = s.str("author"),
                logo = s.str("logo"),
                supportedTypes = s.arr("supportedTypes").strings(),
                fileUrl = if (file.startsWith("http")) file else "$base/${file.trimStart('/')}",
                repoUrl = url,
                repoName = repoName,
                defaultEnabled = s.bool("enabled") ?: true,
                languages = s.arr("contentLanguage").strings(),
            )
        }
        NuvioRepo(url, repoName, scrapers)
    } catch (e: Exception) {
        NuvioRepo(url, url, emptyList(), e.message ?: "خطأ في التحميل")
    }

    suspend fun addRepo(url: String): NuvioRepo {
        val repo = loadRepo(url.trim())
        if (repo.error != null) throw IllegalStateException(repo.error)
        settings.update { it.copy(nuvioRepos = (it.nuvioRepos + url.trim()).distinct()) }
        ensureLoaded(force = true)
        return repo
    }

    suspend fun removeRepo(url: String) {
        settings.update { it.copy(nuvioRepos = it.nuvioRepos - url) }
        ensureLoaded(force = true)
    }

    fun isEnabled(scraper: NuvioScraper, disabled: Set<String>): Boolean =
        if (scraper.defaultEnabled) scraper.key !in disabled else "+${scraper.key}" in disabled

    suspend fun setEnabled(scraper: NuvioScraper, enabled: Boolean) = settings.update { st ->
        val set = st.disabledScrapers.toMutableSet()
        set.remove(scraper.key); set.remove("+${scraper.key}")
        if (scraper.defaultEnabled && !enabled) set += scraper.key
        if (!scraper.defaultEnabled && enabled) set += "+${scraper.key}"
        st.copy(disabledScrapers = set)
    }

    suspend fun activeScrapers(type: String): List<NuvioScraper> {
        ensureLoaded()
        val disabled = settings.current().disabledScrapers
        return _repos.value.flatMap { it.scrapers }.filter { it.supports(type) && isEnabled(it, disabled) }
    }

    suspend fun run(scraper: NuvioScraper, tmdbId: String, type: String, season: Int?, episode: Int?): List<JSONObject> {
        val code = codeCache[scraper.fileUrl] ?: Http.getText(scraper.fileUrl).also { codeCache[scraper.fileUrl] = it }
        val mediaType = if (type == "series") "tv" else "movie"
        val arr = engine.run(code, tmdbId, mediaType, season, episode)
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    }
}
