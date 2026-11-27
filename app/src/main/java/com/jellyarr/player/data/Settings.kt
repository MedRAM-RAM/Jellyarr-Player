package com.jellyarr.player.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "jellyarr_settings")

data class JellyfinSession(
    val serverUrl: String,
    val serverName: String,
    val userId: String,
    val userName: String,
    val token: String,
)

data class PlaybackPrefs(
    val useExternalPlayer: Boolean = false,
    val preferredAudioLang: String = "",
    val preferredSubtitleLang: String = "ar",
    val autoLoadSubtitles: Boolean = true,
    val jellyfinTranscode: Boolean = false,
    val maxBitrateMbps: Int = 0,
)

data class AppSettings(
    val loaded: Boolean = false,
    val deviceId: String = "",
    val jellyfin: JellyfinSession? = null,
    /** Raw stored addon entries (transport url + cached manifest json), in user order. */
    val addons: List<StoredAddon> = emptyList(),
    val nuvioRepos: List<String> = emptyList(),
    val disabledScrapers: Set<String> = emptySet(),
    val tmdbApiKey: String = "",
    val playback: PlaybackPrefs = PlaybackPrefs(),
    val onboardingDone: Boolean = false,
)

data class StoredAddon(val transportUrl: String, val manifestJson: String, val enabled: Boolean = true)

class SettingsStore(private val context: Context, scope: CoroutineScope) {

    private object Keys {
        val DATA = stringPreferencesKey("data_v1")
    }

    val state: StateFlow<AppSettings> = context.dataStore.data
        .map { decode(it) }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    suspend fun current(): AppSettings = state.value.takeIf { it.loaded } ?: context.dataStore.data.map { decode(it) }.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val now = decode(prefs)
            prefs[Keys.DATA] = encode(transform(now))
        }
    }

    private fun decode(prefs: Preferences): AppSettings {
        val raw = prefs[Keys.DATA]
        if (raw.isNullOrBlank()) return AppSettings(loaded = true, deviceId = newDeviceId())
        return runCatching {
            val o = JSONObject(raw)
            val jf = o.optJSONObject("jellyfin")?.let {
                JellyfinSession(
                    serverUrl = it.optString("serverUrl"),
                    serverName = it.optString("serverName"),
                    userId = it.optString("userId"),
                    userName = it.optString("userName"),
                    token = it.optString("token"),
                )
            }?.takeIf { it.token.isNotBlank() }
            val addons = mutableListOf<StoredAddon>()
            o.optJSONArray("addons")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val a = arr.optJSONObject(i) ?: continue
                    addons += StoredAddon(
                        a.optString("url"),
                        a.optString("manifest"),
                        a.optBoolean("enabled", true),
                    )
                }
            }
            val repos = mutableListOf<String>()
            o.optJSONArray("nuvioRepos")?.let { for (i in 0 until it.length()) repos += it.optString(i) }
            val disabled = mutableSetOf<String>()
            o.optJSONArray("disabledScrapers")?.let { for (i in 0 until it.length()) disabled += it.optString(i) }
            val pb = o.optJSONObject("playback")
            AppSettings(
                loaded = true,
                deviceId = o.optString("deviceId").ifBlank { newDeviceId() },
                jellyfin = jf,
                addons = addons,
                nuvioRepos = repos.filter { it.isNotBlank() },
                disabledScrapers = disabled,
                tmdbApiKey = o.optString("tmdbApiKey"),
                onboardingDone = o.optBoolean("onboardingDone", false),
                playback = if (pb == null) PlaybackPrefs() else PlaybackPrefs(
                    useExternalPlayer = pb.optBoolean("external", false),
                    preferredAudioLang = pb.optString("audioLang"),
                    preferredSubtitleLang = pb.optString("subLang", "ar"),
                    autoLoadSubtitles = pb.optBoolean("autoSubs", true),
                    jellyfinTranscode = pb.optBoolean("transcode", false),
                    maxBitrateMbps = pb.optInt("maxBitrate", 0),
                ),
            )
        }.getOrElse { AppSettings(loaded = true, deviceId = newDeviceId()) }
    }

    private fun encode(s: AppSettings): String {
        val o = JSONObject()
        o.put("deviceId", s.deviceId.ifBlank { newDeviceId() })
        s.jellyfin?.let {
            o.put("jellyfin", JSONObject().apply {
                put("serverUrl", it.serverUrl)
                put("serverName", it.serverName)
                put("userId", it.userId)
                put("userName", it.userName)
                put("token", it.token)
            })
        }
        o.put("addons", JSONArray().apply {
            s.addons.forEach {
                put(JSONObject().apply {
                    put("url", it.transportUrl)
                    put("manifest", it.manifestJson)
                    put("enabled", it.enabled)
                })
            }
        })
        o.put("nuvioRepos", JSONArray(s.nuvioRepos))
        o.put("disabledScrapers", JSONArray(s.disabledScrapers.toList()))
        o.put("tmdbApiKey", s.tmdbApiKey)
        o.put("onboardingDone", s.onboardingDone)
        o.put("playback", JSONObject().apply {
            put("external", s.playback.useExternalPlayer)
            put("audioLang", s.playback.preferredAudioLang)
            put("subLang", s.playback.preferredSubtitleLang)
            put("autoSubs", s.playback.autoLoadSubtitles)
            put("transcode", s.playback.jellyfinTranscode)
            put("maxBitrate", s.playback.maxBitrateMbps)
        })
        return o.toString()
    }

    private fun newDeviceId() = UUID.randomUUID().toString().replace("-", "")
}
