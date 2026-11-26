package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.AspectRatioMode
import com.example.data.model.SavedServer
import com.example.data.model.UserSettings
import com.example.data.model.episodeCode
import com.example.data.model.formatDurationMs
import com.example.data.remote.PlaybackSession
import com.example.ui.theme.LocalStrings
import com.example.ui.viewmodel.PlayerUiState
import kotlinx.coroutines.delay

private enum class PlayerSheetType {
    NONE, AUDIO_SUBS, SPEED_ASPECT
}

private data class TrackOption(
    val label: String,
    val groupIndex: Int,
    val trackIndex: Int,
    val isSelected: Boolean,
    val trackGroup: Tracks.Group
)

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    server: SavedServer,
    settings: UserSettings,
    onRetry: () -> Unit,
    onLoadEpisode: (String) -> Unit,
    onReportProgress: (Long, Boolean) -> Unit,
    onReportStopped: (Long) -> Unit,
    onExit: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val activity = context as? Activity

    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(settings.defaultSpeed) }
    var aspectMode by remember { mutableStateOf(AspectRatioMode.FIT) }
    var activeSheet by remember { mutableStateOf(PlayerSheetType.NONE) }
    var currentTracks by remember { mutableStateOf(Tracks.EMPTY) }
    var nextEpisodeCountdown by remember { mutableIntStateOf(-1) }
    var isLandscape by remember { mutableStateOf(true) }

    // Manage Immersive Mode & Orientation
    DisposableEffect(isLandscape) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        activity?.requestedOrientation = if (isLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose { }
    }

    DisposableEffect(Unit) {
        onDispose {
            val window = activity?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val exoPlayer = remember(server.accessToken) {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(
                mapOf("X-Emby-Token" to server.accessToken)
            )
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {
                val trackParamsBuilder = trackSelectionParameters.buildUpon()
                if (settings.preferredAudioLang != "auto") {
                    trackParamsBuilder.setPreferredAudioLanguage(settings.preferredAudioLang)
                }
                if (settings.preferredSubLang == "off") {
                    trackParamsBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                } else if (settings.preferredSubLang != "auto") {
                    trackParamsBuilder.setPreferredTextLanguage(settings.preferredSubLang)
                }
                trackSelectionParameters = trackParamsBuilder.build()
                setPlaybackSpeed(settings.defaultSpeed)
            }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_ENDED) {
                    val nextEp = uiState.session?.nextEpisode
                    if (settings.autoPlayNextEpisode && nextEp != null) {
                        nextEpisodeCountdown = 6
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                currentTracks = tracks
            }

            override fun onPlayerError(error: PlaybackException) {
                playerError = error.localizedMessage ?: strings.playerErrorTitle
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            onReportStopped(exoPlayer.currentPosition.coerceAtLeast(0L))
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    BackHandler {
        onReportStopped(exoPlayer.currentPosition.coerceAtLeast(0L))
        onExit()
    }

    // Prepare media when session changes
    LaunchedEffect(uiState.session?.streamUrl) {
        val session = uiState.session ?: return@LaunchedEffect
        playerError = null
        nextEpisodeCountdown = -1
        val mediaItem = MediaItem.fromUri(session.streamUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        if (session.resumePositionMs > 0L) {
            exoPlayer.seekTo(session.resumePositionMs)
        }
        exoPlayer.playWhenReady = true
    }

    // Poll player progress and periodically sync to Jellyfin
    LaunchedEffect(exoPlayer, uiState.session?.item?.id) {
        var tick = 0
        while (true) {
            if (!isScrubbing) {
                currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            }
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            tick++
            if (tick % 10 == 0 && uiState.session != null) {
                onReportProgress(exoPlayer.currentPosition.coerceAtLeast(0L), !exoPlayer.isPlaying)
            }
            delay(1000L)
        }
    }

    // Auto-hide controls
    LaunchedEffect(controlsVisible, isPlaying, activeSheet, isLocked) {
        if (controlsVisible && isPlaying && activeSheet == PlayerSheetType.NONE && !isLocked) {
            delay(4000L)
            controlsVisible = false
        }
    }

    // Next episode countdown
    LaunchedEffect(nextEpisodeCountdown) {
        if (nextEpisodeCountdown > 0) {
            delay(1000L)
            nextEpisodeCountdown--
        } else if (nextEpisodeCountdown == 0) {
            val nextId = uiState.session?.nextEpisode?.id
            nextEpisodeCountdown = -1
            if (nextId != null) {
                onReportStopped(exoPlayer.currentPosition.coerceAtLeast(0L))
                onLoadEpisode(nextId)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                controlsVisible = !controlsVisible
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = settings.keepScreenOn
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, settings.subtitleSize.spValue)
                }
            },
            update = { pv ->
                pv.keepScreenOn = settings.keepScreenOn
                pv.resizeMode = when (aspectMode) {
                    AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
                pv.subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, settings.subtitleSize.spValue)
            },
            modifier = Modifier.fillMaxSize()
        )

        if (uiState.isLoading || (isBuffering && playerError == null)) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Error Overlay
        val errorText = uiState.error ?: playerError
        if (errorText != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xEE141928)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.playerErrorTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB0BCD6)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onExit) { Text(strings.close) }
                        Button(onClick = {
                            playerError = null
                            onRetry()
                        }) { Text(strings.retry) }
                    }
                }
            }
        }

        // Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(20.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xAA0B0E17),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        IconButton(onClick = { isLocked = false }) {
                            Icon(Icons.Filled.LockOpen, contentDescription = strings.unlockControls, tint = Color.White)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xBB070910), Color(0x22000000), Color(0xCC070910))
                            )
                        )
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = {
                                    onReportStopped(exoPlayer.currentPosition.coerceAtLeast(0L))
                                    onExit()
                                },
                                modifier = Modifier.testTag("player_back_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.close, tint = Color.White)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                val session = uiState.session
                                val title = session?.item?.let { item ->
                                    val code = item.episodeCode()
                                    if (code != null && !item.seriesName.isNullOrBlank()) {
                                        "${item.seriesName} • $code"
                                    } else item.name
                                }.orEmpty()
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                session?.item?.episodeCode()?.let {
                                    Text(
                                        text = session.item.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCAD4EB),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            uiState.session?.let { session ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = if (session.isHls) strings.hlsBadge else strings.directPlayBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            IconButton(onClick = { activeSheet = PlayerSheetType.AUDIO_SUBS }) {
                                Icon(Icons.Filled.ClosedCaption, contentDescription = strings.subtitleTracks, tint = Color.White)
                            }
                            IconButton(onClick = { activeSheet = PlayerSheetType.SPEED_ASPECT }) {
                                Icon(Icons.Filled.Speed, contentDescription = strings.playbackSpeed, tint = Color.White)
                            }
                            IconButton(onClick = { isLandscape = !isLandscape }) {
                                Icon(Icons.Filled.ScreenRotation, contentDescription = null, tint = Color.White)
                            }
                        }
                    }

                    // Center Playback Controls
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        uiState.session?.previousEpisode?.let { prevEp ->
                            IconButton(onClick = {
                                onReportStopped(exoPlayer.currentPosition.coerceAtLeast(0L))
                                onLoadEpisode(prevEp.id)
                            }) {
                                Icon(Icons.Filled.SkipPrevious, contentDescription = strings.prevEpisode, tint = Color.White, modifier = Modifier.size(34.dp))
                            }
                        }

                        IconButton(
                            onClick = {
                                val target = (exoPlayer.currentPosition - settings.seekSeconds * 1000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(target)
                            },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(Icons.Filled.Replay10, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(68.dp)
                                .testTag("player_play_pause_btn")
                        ) {
                            IconButton(onClick = {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) strings.play else strings.play,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                val maxDur = exoPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                                val target = (exoPlayer.currentPosition + settings.seekSeconds * 1000L).coerceAtMost(maxDur)
                                exoPlayer.seekTo(target)
                            },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(Icons.Filled.Forward10, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        uiState.session?.nextEpisode?.let { nextEp ->
                            IconButton(onClick = {
                                onReportStopped(exoPlayer.currentPosition.coerceAtLeast(0L))
                                onLoadEpisode(nextEp.id)
                            }) {
                                Icon(Icons.Filled.SkipNext, contentDescription = strings.nextEpisode, tint = Color.White, modifier = Modifier.size(34.dp))
                            }
                        }
                    }

                    // Bottom Timeline & Utility Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        val sliderMax = durationMs.coerceAtLeast(1L).toFloat()
                        val displayPos = if (isScrubbing) scrubPositionMs else currentPosMs

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${formatDurationMs(displayPos)} / ${formatDurationMs(durationMs)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isLocked = true }) {
                                    Icon(Icons.Filled.Lock, contentDescription = strings.lockControls, tint = Color.White)
                                }
                            }
                        }

                        Slider(
                            value = displayPos.toFloat().coerceIn(0f, sliderMax),
                            onValueChange = { newVal ->
                                isScrubbing = true
                                scrubPositionMs = newVal.toLong()
                            },
                            onValueChangeFinished = {
                                exoPlayer.seekTo(scrubPositionMs)
                                currentPosMs = scrubPositionMs
                                isScrubbing = false
                            },
                            valueRange = 0f..sliderMax,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color(0x55FFFFFF)
                            )
                        )
                    }
                }
            }
        }

        // Auto-play Next Episode Overlay
        if (nextEpisodeCountdown > 0 && uiState.session?.nextEpisode != null) {
            val nextEp = uiState.session.nextEpisode
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = Color(0xEE141928)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${strings.playingNextIn} $nextEpisodeCountdown...",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${nextEp.episodeCode() ?: ""} ${nextEp.name}",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { nextEpisodeCountdown = -1 }) {
                            Text(strings.cancel, color = Color.White)
                        }
                        Button(onClick = {
                            nextEpisodeCountdown = -1
                            onLoadEpisode(nextEp.id)
                        }) {
                            Text(strings.playNow)
                        }
                    }
                }
            }
        }
    }

    // Audio & Subtitles Bottom Sheet
    if (activeSheet == PlayerSheetType.AUDIO_SUBS) {
        val audioTracks = remember(currentTracks) { extractTrackOptions(currentTracks, C.TRACK_TYPE_AUDIO) }
        val textTracks = remember(currentTracks) { extractTrackOptions(currentTracks, C.TRACK_TYPE_TEXT) }
        val isSubDisabled = exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)

        ModalBottomSheet(onDismissRequest = { activeSheet = PlayerSheetType.NONE }) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(strings.audioTracks, style = MaterialTheme.typography.titleMedium)
                    }
                }
                if (audioTracks.isEmpty()) {
                    item {
                        ListItem(headlineContent = { Text("Default") })
                    }
                } else {
                    items(audioTracks) { opt ->
                        ListItem(
                            headlineContent = { Text(opt.label) },
                            trailingContent = {
                                if (opt.isSelected) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier.clickable {
                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                    .buildUpon()
                                    .setOverrideForType(TrackSelectionOverride(opt.trackGroup.mediaTrackGroup, opt.trackIndex))
                                    .build()
                                activeSheet = PlayerSheetType.NONE
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.ClosedCaption, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(strings.subtitleTracks, style = MaterialTheme.typography.titleMedium)
                    }
                }
                item {
                    ListItem(
                        headlineContent = { Text(strings.subtitlesOff) },
                        trailingContent = {
                            if (isSubDisabled || textTracks.none { it.isSelected }) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.clickable {
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                .build()
                            activeSheet = PlayerSheetType.NONE
                        }
                    )
                }
                items(textTracks) { opt ->
                    ListItem(
                        headlineContent = { Text(opt.label) },
                        trailingContent = {
                            if (!isSubDisabled && opt.isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.clickable {
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                .setOverrideForType(TrackSelectionOverride(opt.trackGroup.mediaTrackGroup, opt.trackIndex))
                                .build()
                            activeSheet = PlayerSheetType.NONE
                        }
                    )
                }
            }
        }
    }

    // Speed & Aspect Ratio Bottom Sheet
    if (activeSheet == PlayerSheetType.SPEED_ASPECT) {
        ModalBottomSheet(onDismissRequest = { activeSheet = PlayerSheetType.NONE }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(strings.playbackSpeed, style = MaterialTheme.typography.titleMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                        val selected = playbackSpeed == spd
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    playbackSpeed = spd
                                    exoPlayer.setPlaybackSpeed(spd)
                                    activeSheet = PlayerSheetType.NONE
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${spd}x",
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.AspectRatio, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(strings.aspectRatio, style = MaterialTheme.typography.titleMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        AspectRatioMode.FIT to strings.aspectFit,
                        AspectRatioMode.FILL to strings.aspectFill,
                        AspectRatioMode.ZOOM to strings.aspectZoom
                    ).forEach { (mode, label) ->
                        val selected = aspectMode == mode
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    aspectMode = mode
                                    activeSheet = PlayerSheetType.NONE
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun extractTrackOptions(tracks: Tracks, trackType: Int): List<TrackOption> {
    val result = mutableListOf<TrackOption>()
    tracks.groups.forEachIndexed { groupIdx, group ->
        if (group.type == trackType) {
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val lang = format.language?.uppercase() ?: "UND"
                val label = listOfNotNull(format.label, lang)
                    .distinct()
                    .joinToString(" • ")
                    .ifBlank { "Track ${result.size + 1}" }
                result.add(
                    TrackOption(
                        label = label,
                        groupIndex = groupIdx,
                        trackIndex = i,
                        isSelected = group.isTrackSelected(i),
                        trackGroup = group
                    )
                )
            }
        }
    }
    return result
}
