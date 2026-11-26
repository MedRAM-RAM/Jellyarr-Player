package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguage
import com.example.data.model.GridDensity
import com.example.data.model.PlaybackMode
import com.example.data.model.PreferredCodec
import com.example.data.model.ResumeBehavior
import com.example.data.model.SavedServer
import com.example.data.model.StreamQuality
import com.example.data.model.SubtitleSize
import com.example.data.model.ThemeMode
import com.example.data.model.UserSettings
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cinefin_settings")

class SettingsRepository(private val context: Context) {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val serverListType = Types.newParameterizedType(List::class.java, SavedServer::class.java)
    private val serverListAdapter = moshi.adapter<List<SavedServer>>(serverListType)
    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)

    private object Keys {
        val ACTIVE_SERVER_ID = stringPreferencesKey("active_server_id")
        val SAVED_SERVERS_JSON = stringPreferencesKey("saved_servers_json")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val AUTO_LOGIN = booleanPreferencesKey("auto_login")
        val PLAYBACK_MODE = stringPreferencesKey("playback_mode")
        val WIFI_QUALITY = stringPreferencesKey("wifi_quality")
        val MOBILE_QUALITY = stringPreferencesKey("mobile_quality")
        val PREFERRED_CODEC = stringPreferencesKey("preferred_codec")
        val DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val SEEK_SECONDS = intPreferencesKey("seek_seconds")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val RESUME_BEHAVIOR = stringPreferencesKey("resume_behavior")
        val PREFERRED_AUDIO_LANG = stringPreferencesKey("preferred_audio_lang")
        val PREFERRED_SUB_LANG = stringPreferencesKey("preferred_sub_lang")
        val SUBTITLE_SIZE = stringPreferencesKey("subtitle_size")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val SHOW_PROGRESS_BARS = booleanPreferencesKey("show_progress_bars")
        val GRID_DENSITY = stringPreferencesKey("grid_density")
        val TIMEOUT_SECONDS = intPreferencesKey("timeout_seconds")
        val RECENT_SEARCHES_JSON = stringPreferencesKey("recent_searches_json")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val savedServers = prefs[Keys.SAVED_SERVERS_JSON]?.let {
            runCatching { serverListAdapter.fromJson(it) }.getOrNull()
        } ?: emptyList()
        val activeId = prefs[Keys.ACTIVE_SERVER_ID]
        val activeServer = savedServers.find { it.id == activeId }

        val recentSearches = prefs[Keys.RECENT_SEARCHES_JSON]?.let {
            runCatching { stringListAdapter.fromJson(it) }.getOrNull()
        } ?: emptyList()

        UserSettings(
            activeServer = activeServer,
            savedServers = savedServers,
            autoLogin = prefs[Keys.AUTO_LOGIN] ?: true,
            playbackMode = enumOrDefault(prefs[Keys.PLAYBACK_MODE], PlaybackMode.AUTO_TRANSCODE),
            wifiQuality = enumOrDefault(prefs[Keys.WIFI_QUALITY], StreamQuality.ORIGINAL),
            mobileQuality = enumOrDefault(prefs[Keys.MOBILE_QUALITY], StreamQuality.HD_4),
            preferredCodec = enumOrDefault(prefs[Keys.PREFERRED_CODEC], PreferredCodec.AUTO),
            defaultSpeed = prefs[Keys.DEFAULT_SPEED] ?: 1.0f,
            seekSeconds = prefs[Keys.SEEK_SECONDS] ?: 15,
            autoPlayNextEpisode = prefs[Keys.AUTO_PLAY_NEXT] ?: true,
            resumeBehavior = enumOrDefault(prefs[Keys.RESUME_BEHAVIOR], ResumeBehavior.ASK),
            preferredAudioLang = prefs[Keys.PREFERRED_AUDIO_LANG] ?: "auto",
            preferredSubLang = prefs[Keys.PREFERRED_SUB_LANG] ?: "ara",
            subtitleSize = enumOrDefault(prefs[Keys.SUBTITLE_SIZE], SubtitleSize.MEDIUM),
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
            themeMode = enumOrDefault(prefs[Keys.THEME_MODE], ThemeMode.CINEMA_DARK),
            language = enumOrDefault(prefs[Keys.APP_LANGUAGE], AppLanguage.ARABIC),
            showProgressBars = prefs[Keys.SHOW_PROGRESS_BARS] ?: true,
            gridDensity = enumOrDefault(prefs[Keys.GRID_DENSITY], GridDensity.COMFORTABLE),
            timeoutSeconds = prefs[Keys.TIMEOUT_SECONDS] ?: 20,
            recentSearches = recentSearches
        )
    }

    suspend fun getOrCreateDeviceId(): String {
        var existingId: String? = null
        context.dataStore.edit { prefs ->
            existingId = prefs[Keys.DEVICE_ID]
            if (existingId.isNullOrBlank()) {
                val created = "cinefin-android-${UUID.randomUUID()}"
                prefs[Keys.DEVICE_ID] = created
                existingId = created
            }
        }
        return existingId ?: "cinefin-android-default"
    }

    suspend fun saveActiveServer(server: SavedServer) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SAVED_SERVERS_JSON]?.let {
                runCatching { serverListAdapter.fromJson(it) }.getOrNull()
            } ?: emptyList()
            val updated = current.filterNot {
                it.url.equals(server.url, ignoreCase = true) && it.username.equals(server.username, ignoreCase = true)
            } + server
            prefs[Keys.SAVED_SERVERS_JSON] = serverListAdapter.toJson(updated)
            prefs[Keys.ACTIVE_SERVER_ID] = server.id
        }
    }

    suspend fun setActiveServerId(serverId: String?) {
        context.dataStore.edit { prefs ->
            if (serverId == null) {
                prefs.remove(Keys.ACTIVE_SERVER_ID)
            } else {
                prefs[Keys.ACTIVE_SERVER_ID] = serverId
            }
        }
    }

    suspend fun removeSavedServer(serverId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SAVED_SERVERS_JSON]?.let {
                runCatching { serverListAdapter.fromJson(it) }.getOrNull()
            } ?: emptyList()
            val updated = current.filterNot { it.id == serverId }
            prefs[Keys.SAVED_SERVERS_JSON] = serverListAdapter.toJson(updated)
            if (prefs[Keys.ACTIVE_SERVER_ID] == serverId) {
                prefs.remove(Keys.ACTIVE_SERVER_ID)
            }
        }
    }

    suspend fun updateAutoLogin(value: Boolean) = editPref { it[Keys.AUTO_LOGIN] = value }
    suspend fun updatePlaybackMode(mode: PlaybackMode) = editPref { it[Keys.PLAYBACK_MODE] = mode.name }
    suspend fun updateWifiQuality(quality: StreamQuality) = editPref { it[Keys.WIFI_QUALITY] = quality.name }
    suspend fun updateMobileQuality(quality: StreamQuality) = editPref { it[Keys.MOBILE_QUALITY] = quality.name }
    suspend fun updatePreferredCodec(codec: PreferredCodec) = editPref { it[Keys.PREFERRED_CODEC] = codec.name }
    suspend fun updateDefaultSpeed(speed: Float) = editPref { it[Keys.DEFAULT_SPEED] = speed }
    suspend fun updateSeekSeconds(seconds: Int) = editPref { it[Keys.SEEK_SECONDS] = seconds }
    suspend fun updateAutoPlayNext(enabled: Boolean) = editPref { it[Keys.AUTO_PLAY_NEXT] = enabled }
    suspend fun updateResumeBehavior(behavior: ResumeBehavior) = editPref { it[Keys.RESUME_BEHAVIOR] = behavior.name }
    suspend fun updatePreferredAudioLang(lang: String) = editPref { it[Keys.PREFERRED_AUDIO_LANG] = lang }
    suspend fun updatePreferredSubLang(lang: String) = editPref { it[Keys.PREFERRED_SUB_LANG] = lang }
    suspend fun updateSubtitleSize(size: SubtitleSize) = editPref { it[Keys.SUBTITLE_SIZE] = size.name }
    suspend fun updateKeepScreenOn(enabled: Boolean) = editPref { it[Keys.KEEP_SCREEN_ON] = enabled }
    suspend fun updateThemeMode(mode: ThemeMode) = editPref { it[Keys.THEME_MODE] = mode.name }
    suspend fun updateLanguage(lang: AppLanguage) = editPref { it[Keys.APP_LANGUAGE] = lang.name }
    suspend fun updateShowProgressBars(show: Boolean) = editPref { it[Keys.SHOW_PROGRESS_BARS] = show }
    suspend fun updateGridDensity(density: GridDensity) = editPref { it[Keys.GRID_DENSITY] = density.name }
    suspend fun updateTimeoutSeconds(seconds: Int) = editPref { it[Keys.TIMEOUT_SECONDS] = seconds }

    suspend fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_SEARCHES_JSON]?.let {
                runCatching { stringListAdapter.fromJson(it) }.getOrNull()
            } ?: emptyList()
            val updated = (listOf(clean) + current.filterNot { it.equals(clean, ignoreCase = true) }).take(10)
            prefs[Keys.RECENT_SEARCHES_JSON] = stringListAdapter.toJson(updated)
        }
    }

    suspend fun clearRecentSearches() {
        editPref { it.remove(Keys.RECENT_SEARCHES_JSON) }
    }

    private suspend fun editPref(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
        if (raw.isNullOrBlank()) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }
}
