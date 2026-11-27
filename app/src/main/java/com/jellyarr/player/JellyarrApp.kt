package com.jellyarr.player

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.jellyarr.player.core.Http
import com.jellyarr.player.data.HistoryStore
import com.jellyarr.player.data.SettingsStore
import com.jellyarr.player.data.jellyfin.JellyfinClient
import com.jellyarr.player.data.nuvio.NuvioEngine
import com.jellyarr.player.data.nuvio.NuvioRepository
import com.jellyarr.player.data.streams.PlaybackRequest
import com.jellyarr.player.data.streams.StreamRequest
import com.jellyarr.player.data.stremio.AddonRepository
import com.jellyarr.player.data.streams.StreamAggregator
import com.jellyarr.player.data.tmdb.TmdbClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settings = SettingsStore(app, scope)
    val history = HistoryStore(app, scope)
    val jellyfin = JellyfinClient(deviceId = { settings.state.value.deviceId }, session = { settings.state.value.jellyfin })
    val addons = AddonRepository(settings, scope)
    val tmdb = TmdbClient { settings.state.value.tmdbApiKey }
    val nuvioEngine = NuvioEngine(app)
    val nuvio = NuvioRepository(settings, nuvioEngine)
    val streams = StreamAggregator(settings, jellyfin, addons, nuvio, tmdb)

    /** Hand-off slots for navigation (objects too rich for route strings). */
    var pendingStreamRequest: StreamRequest? = null
    var pendingPlayback: PlaybackRequest? = null

    init {
        scope.launch {
            // Persist a stable device id on first launch.
            val s = settings.current()
            settings.update { it.copy(deviceId = s.deviceId) }
        }
    }
}

class JellyarrApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient(Http.client)
        .crossfade(true)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("images")).maxSizeBytes(256L * 1024 * 1024).build() }
        .respectCacheHeaders(false)
        .build()

    companion object {
        lateinit var instance: JellyarrApp
            private set
        val container: AppContainer get() = instance.container
    }
}
