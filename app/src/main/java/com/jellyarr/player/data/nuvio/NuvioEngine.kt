package com.jellyarr.player.data.nuvio

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.jellyarr.player.core.Http
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Executes Nuvio-format JavaScript providers inside a hidden WebView.
 * Network requests are bridged to OkHttp, so scrapers are not limited by CORS.
 */
class NuvioEngine(private val context: Context) {

    private val main = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initLock = Mutex()
    private var webView: WebView? = null
    private var ready: CompletableDeferred<Unit>? = null

    private val codes = ConcurrentHashMap<String, String>()
    private val calls = ConcurrentHashMap<String, CompletableDeferred<String>>()
    private val callSeq = AtomicLong(0)

    private val noRedirectClient by lazy { Http.client.newBuilder().followRedirects(false).followSslRedirects(false).build() }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private suspend fun ensureReady() {
        initLock.withLock {
            val existing = ready
            if (existing != null && webView != null) {
                existing.await()
                return
            }
            val deferred = CompletableDeferred<Unit>()
            ready = deferred
            withContext(Dispatchers.Main) {
                val wv = WebView(context.applicationContext)
                wv.settings.javaScriptEnabled = true
                wv.settings.domStorageEnabled = true
                wv.settings.blockNetworkLoads = false
                wv.addJavascriptInterface(Bridge(), "NativeBridge")
                webView = wv
                val runtime = context.assets.open("nuvio_runtime.js").bufferedReader().use { it.readText() }
                val html = "<!doctype html><html><head><meta charset=\"utf-8\"></head><body><script>$runtime</script></body></html>"
                wv.loadDataWithBaseURL("https://nuvio.local/", html, "text/html", "utf-8", null)
            }
            withTimeout(15_000) { deferred.await() }
        }
    }

    suspend fun run(
        code: String,
        tmdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?,
        timeoutMs: Long = 30_000,
    ): JSONArray {
        ensureReady()
        val id = "c" + callSeq.incrementAndGet()
        val deferred = CompletableDeferred<String>()
        calls[id] = deferred
        codes[id] = code + "\n;if(typeof getStreams==='function'&&!(module.exports&&module.exports.getStreams)){module.exports={getStreams:getStreams};}"
        try {
            val js = "window.__runScraper(${JSONObject.quote(id)}, ${JSONObject.quote(tmdbId)}, ${JSONObject.quote(mediaType)}, ${season ?: "null"}, ${episode ?: "null"})"
            withContext(Dispatchers.Main) { webView?.evaluateJavascript(js, null) }
            val text = withTimeout(timeoutMs) { deferred.await() }
            return JSONArray(text)
        } finally {
            calls.remove(id)
            codes.remove(id)
        }
    }

    private fun deliver(reqId: String, payload: JSONObject) {
        val js = "window.__nativeResponse(${JSONObject.quote(reqId)}, ${payload})"
        main.post { webView?.evaluateJavascript(js, null) }
    }

    private inner class Bridge {
        @JavascriptInterface
        fun ready() {
            ready?.complete(Unit)
        }

        @JavascriptInterface
        fun getCode(callId: String): String = codes[callId].orEmpty()

        @JavascriptInterface
        fun result(callId: String, json: String) {
            calls[callId]?.complete(json)
        }

        @JavascriptInterface
        fun error(callId: String, message: String) {
            calls[callId]?.completeExceptionally(IllegalStateException(message))
        }

        @JavascriptInterface
        fun log(message: String) {
            Log.d("NuvioJS", message.take(2000))
        }

        @JavascriptInterface
        fun request(reqId: String, url: String, method: String, headersJson: String, body: String, redirect: String) {
            scope.launch {
                val payload = JSONObject()
                try {
                    val headers = runCatching { JSONObject(headersJson) }.getOrElse { JSONObject() }
                    val contentType = headers.keys().asSequence()
                        .firstOrNull { it.equals("content-type", true) }?.let { headers.optString(it) }
                    val reqBody = when {
                        method == "GET" || method == "HEAD" -> null
                        else -> body.toRequestBody(contentType?.toMediaTypeOrNull())
                    }
                    val builder = Request.Builder().url(url).method(method, reqBody)
                    var hasUa = false
                    headers.keys().forEach { k ->
                        if (k.equals("user-agent", true)) hasUa = true
                        runCatching { builder.header(k, headers.optString(k)) }
                    }
                    if (!hasUa) builder.header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36",
                    )
                    val client = if (redirect == "manual") noRedirectClient else Http.client
                    val resp = Http.await(client.newCall(builder.build()))
                    resp.use {
                        val h = JSONObject()
                        it.headers.names().forEach { name -> h.put(name.lowercase(), it.headers(name).joinToString(", ")) }
                        payload.put("status", it.code)
                        payload.put("statusText", it.message)
                        payload.put("url", it.request.url.toString())
                        payload.put("redirected", it.priorResponse != null)
                        payload.put("headers", h)
                        payload.put("body", it.body?.string().orEmpty())
                    }
                } catch (e: Exception) {
                    payload.put("error", e.message ?: e.javaClass.simpleName)
                }
                deliver(reqId, payload)
            }
        }
    }
}
