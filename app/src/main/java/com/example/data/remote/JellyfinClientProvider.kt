package com.example.data.remote

import android.os.Build
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class JellyfinSessionConfig {
    val serverUrl = AtomicReference("")
    val accessToken = AtomicReference("")
    val deviceId = AtomicReference("cinefin-android-device")
    val timeoutSeconds = AtomicInteger(20)

    fun authHeaderValue(): String {
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".replace("\"", "")
        val token = accessToken.get()
        val tokenPart = if (token.isNotBlank()) ", Token=\"$token\"" else ""
        return "MediaBrowser Client=\"CineFin\", Device=\"$deviceName\", DeviceId=\"${deviceId.get()}\", Version=\"1.0.0\"$tokenPart"
    }
}

class JellyfinClientProvider(val sessionConfig: JellyfinSessionConfig) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val dynamicInterceptor = Interceptor { chain ->
        val original = chain.request()
        val configuredUrl = sessionConfig.serverUrl.get().trimEnd('/')
        val currentHttpUrl = original.url

        // If the request used the placeholder host, rewrite scheme/host/port/basePath to active server URL
        val rebuiltUrl = if (currentHttpUrl.host == PLACEHOLDER_HOST && configuredUrl.isNotBlank()) {
            val base = configuredUrl.toHttpUrlOrNull()
            if (base != null) {
                val baseSegments = base.encodedPathSegments.filter { it.isNotEmpty() }
                val requestSegments = currentHttpUrl.encodedPathSegments.filter { it.isNotEmpty() }
                val builder = currentHttpUrl.newBuilder()
                    .scheme(base.scheme)
                    .host(base.host)
                    .port(base.port)
                    .encodedPath("/")
                (baseSegments + requestSegments).forEach { segment ->
                    builder.addEncodedPathSegment(segment)
                }
                builder.build()
            } else {
                currentHttpUrl
            }
        } else {
            currentHttpUrl
        }

        val reqBuilder = original.newBuilder()
            .url(rebuiltUrl)
            .header("X-Emby-Authorization", sessionConfig.authHeaderValue())
            .header("Accept", "application/json")

        val token = sessionConfig.accessToken.get()
        if (token.isNotBlank()) {
            reqBuilder.header("X-Emby-Token", token)
        }

        val timeout = sessionConfig.timeoutSeconds.get().coerceIn(5, 60)
        chain
            .withConnectTimeout(timeout, TimeUnit.SECONDS)
            .withReadTimeout(timeout, TimeUnit.SECONDS)
            .withWriteTimeout(timeout, TimeUnit.SECONDS)
            .proceed(reqBuilder.build())
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(dynamicInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    val api: JellyfinApi = Retrofit.Builder()
        .baseUrl("https://$PLACEHOLDER_HOST/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(JellyfinApi::class.java)

    companion object {
        private const val PLACEHOLDER_HOST = "jellyfin.local.placeholder"
    }
}
