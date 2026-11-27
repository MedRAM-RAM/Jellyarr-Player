package com.jellyarr.player.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HttpException(val code: Int, message: String) : IOException(message)

object Http {
    const val USER_AGENT = "Jellyarr/2.0 (Android)"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    suspend fun await(call: Call): Response = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { runCatching { call.cancel() } }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (cont.isActive) cont.resume(response) else response.close()
            }
        })
    }

    suspend fun getText(
        url: String,
        headers: Map<String, String> = emptyMap(),
        client: OkHttpClient = this.client,
    ): String {
        val req = Request.Builder().url(url).get().apply {
            header("User-Agent", USER_AGENT)
            header("Accept", "application/json, text/plain, */*")
            headers.forEach { (k, v) -> header(k, v) }
        }.build()
        val resp = await(client.newCall(req))
        return withContext(Dispatchers.IO) {
            resp.use {
                val body = it.body?.string().orEmpty()
                if (!it.isSuccessful) throw HttpException(it.code, "HTTP ${it.code}: ${body.take(200)}")
                body
            }
        }
    }

    suspend fun getJson(url: String, headers: Map<String, String> = emptyMap()): JSONObject =
        JSONObject(getText(url, headers))

    suspend fun getJsonArray(url: String, headers: Map<String, String> = emptyMap()): JSONArray =
        JSONArray(getText(url, headers))

    /** Sends a request with an optional JSON body and returns the raw response body. */
    suspend fun send(
        url: String,
        method: String,
        json: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): String {
        val body = when {
            json != null -> json.toRequestBody(JSON)
            method == "GET" || method == "HEAD" -> null
            else -> "".toRequestBody(JSON)
        }
        val req = Request.Builder().url(url).method(method, body).apply {
            header("User-Agent", USER_AGENT)
            header("Accept", "application/json")
            headers.forEach { (k, v) -> header(k, v) }
        }.build()
        val resp = await(client.newCall(req))
        return withContext(Dispatchers.IO) {
            resp.use {
                val text = it.body?.string().orEmpty()
                if (!it.isSuccessful) throw HttpException(it.code, "HTTP ${it.code}: ${text.take(200)}")
                text
            }
        }
    }
}

// ---- org.json helpers (null-safe, tolerant of schema variance) ----

fun JSONObject.str(key: String): String? =
    if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null

fun JSONObject.int(key: String): Int? =
    if (has(key) && !isNull(key)) optString(key).toDoubleOrNull()?.toInt() else null

fun JSONObject.long(key: String): Long? =
    if (has(key) && !isNull(key)) optString(key).toDoubleOrNull()?.toLong() else null

fun JSONObject.dbl(key: String): Double? =
    if (has(key) && !isNull(key)) optString(key).toDoubleOrNull() else null

fun JSONObject.bool(key: String): Boolean? =
    if (has(key) && !isNull(key)) optBoolean(key) else null

fun JSONObject.obj(key: String): JSONObject? = optJSONObject(key)

fun JSONObject.arr(key: String): JSONArray? = optJSONArray(key)

inline fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T?): List<T> {
    if (this == null) return emptyList()
    val out = ArrayList<T>(length())
    for (i in 0 until length()) {
        val o = optJSONObject(i) ?: continue
        transform(o)?.let(out::add)
    }
    return out
}

fun JSONArray?.strings(): List<String> {
    if (this == null) return emptyList()
    val out = ArrayList<String>(length())
    for (i in 0 until length()) {
        val v = opt(i)
        when (v) {
            is String -> if (v.isNotBlank()) out += v
            is JSONObject -> v.str("name")?.let(out::add)
            null -> Unit
            else -> out += v.toString()
        }
    }
    return out
}

fun String.urlEncode(): String = java.net.URLEncoder.encode(this, "UTF-8").replace("+", "%20")
