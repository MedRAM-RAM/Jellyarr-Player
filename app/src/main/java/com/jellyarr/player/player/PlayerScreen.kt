package com.jellyarr.player.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.jellyarr.player.JellyarrApp
import com.jellyarr.player.core.Format
import com.jellyarr.player.core.Http
import com.jellyarr.player.data.stremio.StremioClient
import com.jellyarr.player.data.stremio.StremioSubtitle
import com.jellyarr.player.data.streams.PlaybackRequest
import com.jellyarr.player.ui.LocalNavigator
import com.jellyarr.player.ui.screens.openExternal
import com.jellyarr.player.ui.theme.JColors
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

private data class TrackOption(val label: String, val group: Tracks.Group?, val index: Int, val selected: Boolean)

private enum class Panel { NONE, AUDIO, SUBS, SPEED }

private fun subtitleMime(url: String): String {
    val path = url.substringBefore('?').lowercase(Locale.US)
    return when {
        path.endsWith(".vtt") -> MimeTypes.TEXT_VTT
        path.endsWith(".ass") || path.endsWith(".ssa") -> MimeTypes.TEXT_SSA
        path.endsWith(".ttml") || path.endsWith(".xml") -> MimeTypes.APPLICATION_TTML
        else -> MimeTypes.APPLICATION_SUBRIP
    }
}

private fun langName(code: String?): String {
    if (code.isNullOrBlank() || code == "und") return "غير معروف"
    return runCatching { Locale.forLanguageTag(code).getDisplayLanguage(Locale("ar")).ifBlank { code } }.getOrDefault(code)
}

private fun langMatches(code: String?, pref: String): Boolean {
    if (code.isNullOrBlank() || pref.isBlank()) return false
    val a = runCatching { Locale.forLanguageTag(code).isO3Language }.getOrDefault(code)
    val b = runCatching { Locale.forLanguageTag(pref).isO3Language }.getOrDefault(pref)
    return code.equals(pref, true) || a.equals(b, true) || code.startsWith(pref, true)
}

@Composable
fun PlayerScreen() {
    val c = JellyarrApp.container
    val req = remember { c.pendingPlayback }
    val nav = LocalNavigator.current
    if (req == null) {
        LaunchedEffect(Unit) { nav.back() }
        return
    }
    PlayerContent(req)
}

@Composable
private fun PlayerContent(req: PlaybackRequest) {
    val context = LocalContext.current
    val activity = remember { context.findActivity() }
    val c = JellyarrApp.container
    val prefs = remember { c.settings.state.value.playback }
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()

    var controlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(true) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var scrubbing by remember { mutableStateOf<Float?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var panel by remember { mutableStateOf(Panel.NONE) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var speed by remember { mutableFloatStateOf(1f) }
    var tracksVersion by remember { mutableIntStateOf(0) }
    var usedFallback by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    var subtitleCount by remember { mutableIntStateOf(0) }
    var interactionTick by remember { mutableIntStateOf(0) }
    val playMethod = remember { if (req.url.contains("master.m3u8")) "Transcode" else "DirectPlay" }

    val player = remember {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(Http.USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(30_000)
            .setDefaultRequestProperties(req.headers)
        val renderers = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        ExoPlayer.Builder(context, renderers)
            .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(context, http)))
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                trackSelectionParameters = trackSelectionParameters.buildUpon().apply {
                    if (prefs.preferredAudioLang.isNotBlank()) setPreferredAudioLanguage(prefs.preferredAudioLang)
                    if (prefs.preferredSubtitleLang.isNotBlank()) setPreferredTextLanguage(prefs.preferredSubtitleLang)
                }.build()
            }
    }

    fun buildItem(url: String, subs: List<StremioSubtitle>): MediaItem {
        val b = MediaItem.Builder().setUri(Uri.parse(url))
        if (url.contains(".m3u8", true)) b.setMimeType(MimeTypes.APPLICATION_M3U8)
        else if (url.contains(".mpd", true)) b.setMimeType(MimeTypes.APPLICATION_MPD)
        val configs = subs.distinctBy { it.url }.take(30).map { s ->
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(s.url))
                .setMimeType(subtitleMime(s.url))
                .setLanguage(s.lang.ifBlank { null })
                .setLabel(langName(s.lang) + (s.id.takeIf { it.length < 24 }?.let { " · $it" } ?: ""))
                .setSelectionFlags(0)
                .build()
        }
        return b.setSubtitleConfigurations(configs).build()
    }

    // ---- load: resolve resume position + external subtitles, then prepare ----
    LaunchedEffect(Unit) {
        var start = req.startPositionMs
        val subs = mutableListOf<StremioSubtitle>()
        subs += req.externalSubtitles
        withTimeoutOrNull(4_000) {
            val jobs = mutableListOf<kotlinx.coroutines.Deferred<List<StremioSubtitle>>>()
            if (req.jellyfinItemId != null) {
                jobs += async {
                    val item = runCatching { c.jellyfin.item(req.jellyfinItemId) }.getOrNull() ?: return@async emptyList()
                    if (start < 0) start = (item.userData?.playbackPositionTicks ?: 0) / 10_000
                    val ms = item.mediaSources.firstOrNull { it.id == req.jellyfinMediaSourceId } ?: item.mediaSources.firstOrNull()
                    ms?.streams?.filter { it.type == "Subtitle" && it.isExternal }?.map {
                        StremioSubtitle("jf-${it.index}", c.jellyfin.subtitleUrl(item.id, ms.id, it.index), it.language.orEmpty())
                    }.orEmpty()
                }
            }
            val type = req.stremioType
            val id = req.stremioId
            if (prefs.autoLoadSubtitles && type != null && id != null) {
                c.addons.forResource("subtitles", type, id).forEach { addon ->
                    jobs += async { runCatching { StremioClient.subtitles(addon, type, id) }.getOrDefault(emptyList()) }
                }
            }
            jobs.awaitAll().forEach { subs += it }
        }
        if (start < 0) start = 0
        // preferred language first
        val sorted = subs.sortedByDescending { langMatches(it.lang, prefs.preferredSubtitleLang) }
        subtitleCount = sorted.size
        player.setMediaItem(buildItem(req.url, sorted), start)
        player.prepare()
        player.playWhenReady = true
        ready = true
    }

    // ---- listeners ----
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) duration = player.duration.coerceAtLeast(0)
                if (state == Player.STATE_ENDED) controlsVisible = true
            }
            override fun onTracksChanged(tracks: Tracks) { tracksVersion++ }
            override fun onPlayerError(e: PlaybackException) {
                val fb = req.fallbackUrl
                if (fb != null && !usedFallback) {
                    usedFallback = true
                    val pos = player.currentPosition
                    player.setMediaItem(buildItem(fb, emptyList()), pos)
                    player.prepare()
                    player.playWhenReady = true
                } else {
                    error = e.errorCodeName + (e.message?.let { "\n$it" } ?: "")
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // ---- window: landscape + immersive + keep screen on ----
    DisposableEffect(Unit) {
        val window = activity?.window
        val prevOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = prevOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // ---- progress ticker + reporting ----
    LaunchedEffect(ready) {
        if (!ready) return@LaunchedEffect
        req.jellyfinItemId?.let { id -> launch { c.jellyfin.reportStart(id, req.jellyfinMediaSourceId, player.currentPosition, playMethod) } }
        var ticks = 0
        while (true) {
            position = player.currentPosition.coerceAtLeast(0)
            if (player.duration > 0) duration = player.duration
            ticks++
            if (ticks % 20 == 0) { // every ~10s
                val pos = position
                req.jellyfinItemId?.let { id -> launch { c.jellyfin.reportProgress(id, req.jellyfinMediaSourceId, pos, !player.isPlaying, playMethod) } }
                req.history?.let { h -> if (duration > 0) launch { c.history.save(h.copy(positionMs = pos, durationMs = duration)) } }
            }
            delay(500)
        }
    }

    // ---- auto-hide controls ----
    LaunchedEffect(controlsVisible, isPlaying, panel, interactionTick) {
        if (controlsVisible && isPlaying && panel == Panel.NONE && scrubbing == null) {
            delay(4000)
            controlsVisible = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val pos = player.currentPosition
            val dur = player.duration
            scope.launch(NonCancellable) {
                req.jellyfinItemId?.let { c.jellyfin.reportStopped(it, req.jellyfinMediaSourceId, pos) }
                req.history?.let { h -> if (dur > 0) c.history.save(h.copy(positionMs = pos, durationMs = dur)) }
            }
            player.release()
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { if (panel != Panel.NONE) panel = Panel.NONE else controlsVisible = !controlsVisible },
                    onDoubleTap = { o ->
                        val forward = o.x > size.width / 2
                        player.seekTo((player.currentPosition + if (forward) 10_000 else -10_000).coerceAtLeast(0))
                        interactionTick++
                    },
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    this.player = player
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    subtitleView?.setStyle(
                        CaptionStyleCompat(
                            android.graphics.Color.WHITE, android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT,
                            CaptionStyleCompat.EDGE_TYPE_OUTLINE, android.graphics.Color.BLACK, null,
                        )
                    )
                    subtitleView?.setFractionalTextSize(0.0533f * 1.15f)
                }
            },
            update = { it.resizeMode = resizeMode },
            modifier = Modifier.fillMaxSize(),
        )

        if (buffering && error == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center).size(54.dp), color = JColors.Primary, strokeWidth = 3.dp)
        }

        AnimatedVisibility(controlsVisible && error == null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0f to Color.Black.copy(.75f), .3f to Color.Transparent, .65f to Color.Transparent, 1f to Color.Black.copy(.85f))
                )
            ) {
                // top bar
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundIcon(Icons.AutoMirrored.Filled.ArrowBack) { nav.back() }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(req.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val sub = listOfNotNull(req.subtitle, req.filename).joinToString(" · ")
                        if (sub.isNotBlank()) Text(sub, color = Color.White.copy(.7f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    RoundIcon(Icons.Filled.Speed) { panel = Panel.SPEED }
                    Spacer(Modifier.width(8.dp))
                    RoundIcon(Icons.Filled.Audiotrack) { panel = Panel.AUDIO }
                    Spacer(Modifier.width(8.dp))
                    RoundIcon(Icons.Filled.Subtitles) { panel = Panel.SUBS }
                    Spacer(Modifier.width(8.dp))
                    RoundIcon(Icons.Filled.AspectRatio) {
                        resizeMode = when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                        interactionTick++
                    }
                }
                // center controls
                Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundIcon(Icons.Filled.Replay10, size = 56) { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)); interactionTick++ }
                    Box(
                        Modifier.size(78.dp).clip(CircleShape).background(JColors.Brand).clickable {
                            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
                            if (player.isPlaying) player.pause() else player.play()
                            interactionTick++
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(42.dp))
                    }
                    RoundIcon(Icons.Filled.Forward10, size = 56) { player.seekTo(player.currentPosition + 10_000); interactionTick++ }
                }
                // bottom seek bar
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp)) {
                    val shown = scrubbing?.let { (it * duration).toLong() } ?: position
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Format.duration(shown), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text("-" + Format.duration((duration - shown).coerceAtLeast(0)), color = Color.White.copy(.75f), fontSize = 13.sp)
                    }
                    Slider(
                        value = scrubbing ?: if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                        onValueChange = { scrubbing = it; interactionTick++ },
                        onValueChangeFinished = {
                            scrubbing?.let { player.seekTo((it * duration).toLong()) }
                            scrubbing = null
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = JColors.Primary,
                            inactiveTrackColor = Color.White.copy(alpha = .25f),
                        ),
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                    )
                }
            }
        }

        // ---- error overlay ----
        error?.let { msg ->
            Column(
                Modifier.align(Alignment.Center).widthIn(max = 480.dp).clip(RoundedCornerShape(20.dp))
                    .background(JColors.SurfaceHigh).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("تعذّر تشغيل الفيديو", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(msg, color = JColors.TextMuted, fontSize = 12.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("مشغّل خارجي", true) { openExternal(context, req.copy(startPositionMs = player.currentPosition)) }
                    PillButton("إعادة المحاولة", false) {
                        error = null
                        player.prepare(); player.play()
                    }
                    PillButton("رجوع", false) { nav.back() }
                }
            }
        }

        // ---- side panels ----
        if (panel != Panel.NONE) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(.35f)).clickable { panel = Panel.NONE })
            Column(
                Modifier.align(Alignment.CenterEnd).padding(16.dp).width(320.dp).clip(RoundedCornerShape(20.dp))
                    .background(JColors.SurfaceHigh.copy(alpha = .97f)).padding(vertical = 12.dp)
            ) {
                val title = when (panel) { Panel.AUDIO -> "الصوت"; Panel.SUBS -> "الترجمة"; Panel.SPEED -> "السرعة"; else -> "" }
                Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp))
                val options: List<TrackOption> = remember(panel, tracksVersion) {
                    when (panel) {
                        Panel.AUDIO -> trackOptions(player, C.TRACK_TYPE_AUDIO, allowOff = false)
                        Panel.SUBS -> trackOptions(player, C.TRACK_TYPE_TEXT, allowOff = true)
                        else -> emptyList()
                    }
                }
                if (panel == Panel.SPEED) {
                    LazyColumn {
                        items(listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)) { rate ->
                            PanelRow("${rate}x", rate == speed) { speed = rate; player.setPlaybackSpeed(rate); panel = Panel.NONE }
                        }
                    }
                } else if (options.isEmpty()) {
                    Text(
                        if (panel == Panel.SUBS && subtitleCount == 0) "لا توجد ترجمات. أضف إضافة ترجمة مثل OpenSubtitles." else "لا توجد مسارات",
                        color = JColors.TextMuted, fontSize = 13.sp, modifier = Modifier.padding(18.dp),
                    )
                } else {
                    LazyColumn {
                        items(options) { o ->
                            PanelRow(o.label, o.selected) {
                                val type = if (panel == Panel.AUDIO) C.TRACK_TYPE_AUDIO else C.TRACK_TYPE_TEXT
                                val builder = player.trackSelectionParameters.buildUpon()
                                if (o.group == null) {
                                    builder.setTrackTypeDisabled(type, true)
                                } else {
                                    builder.setTrackTypeDisabled(type, false)
                                    builder.setOverrideForType(TrackSelectionOverride(o.group.mediaTrackGroup, o.index))
                                }
                                player.trackSelectionParameters = builder.build()
                                panel = Panel.NONE
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun trackOptions(player: ExoPlayer, type: Int, allowOff: Boolean): List<TrackOption> {
    val out = mutableListOf<TrackOption>()
    val disabled = player.trackSelectionParameters.disabledTrackTypes.contains(type)
    var anySelected = false
    player.currentTracks.groups.filter { it.type == type }.forEach { g ->
        for (i in 0 until g.length) {
            if (!g.isTrackSupported(i, true)) continue
            val f = g.getTrackFormat(i)
            val parts = listOfNotNull(
                f.label ?: langName(f.language),
                when (f.channelCount) { 8 -> "7.1"; 6 -> "5.1"; 2 -> "ستيريو"; else -> null }.takeIf { type == C.TRACK_TYPE_AUDIO },
                f.codecs?.substringBefore('.')?.uppercase(Locale.US)?.takeIf { type == C.TRACK_TYPE_AUDIO },
            )
            val sel = !disabled && g.isTrackSelected(i)
            if (sel) anySelected = true
            out += TrackOption(parts.joinToString(" · "), g, i, sel)
        }
    }
    if (allowOff) out.add(0, TrackOption("إيقاف", null, -1, disabled || !anySelected))
    return out
}

@Composable
private fun PanelRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = if (selected) JColors.PrimarySoft else Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (selected) Icon(Icons.Filled.Check, null, tint = JColors.PrimarySoft, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, size: Int = 42, onClick: () -> Unit) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(Color.White.copy(alpha = .12f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size((size * 0.55f).dp)) }
}

@Composable
private fun PillButton(text: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(12.dp))
            .background(if (primary) JColors.BrandHorizontal else Brush.horizontalGradient(listOf(Color.White.copy(.12f), Color.White.copy(.12f))))
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp)
    ) { Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
}
