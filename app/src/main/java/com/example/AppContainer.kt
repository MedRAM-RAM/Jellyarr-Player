package com.example

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.local.SettingsRepository
import com.example.data.remote.JellyfinClientProvider
import com.example.data.remote.JellyfinRepository
import com.example.data.remote.JellyfinSessionConfig

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val settingsRepository = SettingsRepository(appContext)
    val sessionConfig = JellyfinSessionConfig()
    val clientProvider = JellyfinClientProvider(sessionConfig)
    val jellyfinRepository = JellyfinRepository(
        context = appContext,
        clientProvider = clientProvider,
        settingsRepository = settingsRepository
    )

    val imageLoader: ImageLoader = ImageLoader.Builder(appContext)
        .okHttpClient(clientProvider.okHttpClient)
        .memoryCache {
            MemoryCache.Builder(appContext)
                .maxSizePercent(0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(appContext.cacheDir.resolve("cinefin_image_cache"))
                .maxSizeBytes(150L * 1024L * 1024L)
                .build()
        }
        .crossfade(true)
        .build()

    fun getImageCacheSizeBytes(): Long {
        return imageLoader.diskCache?.size ?: 0L
    }

    fun clearImageCache() {
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }
}
