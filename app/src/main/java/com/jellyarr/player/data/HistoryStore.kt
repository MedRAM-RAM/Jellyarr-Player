package com.jellyarr.player.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.json.JSONArray
import org.json.JSONObject

private val Context.historyStore by preferencesDataStore(name = "jellyarr_history")

/** Local watch progress for addon / Nuvio content (Jellyfin keeps its own server-side). */
data class HistoryEntry(
    val metaId: String,
    val type: String,
    val name: String,
    val poster: String?,
    val backdrop: String?,
    val videoId: String?,
    val season: Int?,
    val episode: Int?,
    val episodeTitle: String?,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val progress: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val key: String get() = "$type:$metaId"
}

class HistoryStore(private val context: Context, scope: CoroutineScope) {
    private val KEY = stringPreferencesKey("entries")

    val entries: StateFlow<List<HistoryEntry>> = context.historyStore.data
        .map { prefs -> decode(prefs[KEY]) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Items in progress (between 2% and 92%), newest first. */
    val continueWatching: List<HistoryEntry>
        get() = entries.value.filter { it.progress in 0.02f..0.92f }.sortedByDescending { it.updatedAt }

    fun find(type: String, metaId: String): HistoryEntry? = entries.value.firstOrNull { it.type == type && it.metaId == metaId }

    suspend fun save(entry: HistoryEntry) {
        context.historyStore.edit { prefs ->
            val list = decode(prefs[KEY]).filterNot { it.key == entry.key }.toMutableList()
            list.add(0, entry.copy(updatedAt = System.currentTimeMillis()))
            prefs[KEY] = encode(list.take(200))
        }
    }

    suspend fun remove(entry: HistoryEntry) {
        context.historyStore.edit { prefs ->
            prefs[KEY] = encode(decode(prefs[KEY]).filterNot { it.key == entry.key })
        }
    }

    private fun decode(raw: String?): List<HistoryEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                HistoryEntry(
                    metaId = o.optString("metaId"),
                    type = o.optString("type"),
                    name = o.optString("name"),
                    poster = o.optString("poster").ifBlank { null },
                    backdrop = o.optString("backdrop").ifBlank { null },
                    videoId = o.optString("videoId").ifBlank { null },
                    season = if (o.has("season")) o.optInt("season") else null,
                    episode = if (o.has("episode")) o.optInt("episode") else null,
                    episodeTitle = o.optString("episodeTitle").ifBlank { null },
                    positionMs = o.optLong("position"),
                    durationMs = o.optLong("duration"),
                    updatedAt = o.optLong("updated"),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encode(list: List<HistoryEntry>): String = JSONArray().apply {
        list.forEach { e ->
            put(JSONObject().apply {
                put("metaId", e.metaId); put("type", e.type); put("name", e.name)
                e.poster?.let { put("poster", it) }; e.backdrop?.let { put("backdrop", it) }
                e.videoId?.let { put("videoId", it) }
                e.season?.let { put("season", it) }; e.episode?.let { put("episode", it) }
                e.episodeTitle?.let { put("episodeTitle", it) }
                put("position", e.positionMs); put("duration", e.durationMs); put("updated", e.updatedAt)
            })
        }
    }.toString()
}
