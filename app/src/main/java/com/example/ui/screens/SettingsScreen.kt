package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.example.ui.theme.LocalStrings
import kotlinx.coroutines.launch

private enum class SettingPickerType {
    NONE,
    SWITCH_SERVER,
    PLAYBACK_MODE,
    WIFI_QUALITY,
    MOBILE_QUALITY,
    PREFERRED_CODEC,
    DEFAULT_SPEED,
    SEEK_INTERVAL,
    RESUME_BEHAVIOR,
    AUDIO_LANG,
    SUB_LANG,
    SUB_SIZE,
    THEME_MODE,
    APP_LANGUAGE,
    GRID_DENSITY,
    TIMEOUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: UserSettings,
    cacheSizeBytes: Long,
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    onSwitchServer: (SavedServer) -> Unit,
    onSignOut: () -> Unit,
    onAutoLoginChange: (Boolean) -> Unit,
    onPlaybackModeChange: (PlaybackMode) -> Unit,
    onWifiQualityChange: (StreamQuality) -> Unit,
    onMobileQualityChange: (StreamQuality) -> Unit,
    onCodecChange: (PreferredCodec) -> Unit,
    onDefaultSpeedChange: (Float) -> Unit,
    onSeekSecondsChange: (Int) -> Unit,
    onAutoPlayNextChange: (Boolean) -> Unit,
    onResumeBehaviorChange: (ResumeBehavior) -> Unit,
    onAudioLangChange: (String) -> Unit,
    onSubLangChange: (String) -> Unit,
    onSubtitleSizeChange: (SubtitleSize) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onShowProgressBarsChange: (Boolean) -> Unit,
    onGridDensityChange: (GridDensity) -> Unit,
    onTimeoutChange: (Int) -> Unit,
    onClearCache: () -> Unit
) {
    val strings = LocalStrings.current
    val isArabic = settings.language == AppLanguage.ARABIC
    var activePicker by remember { mutableStateOf(SettingPickerType.NONE) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(strings.settingsTitle, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.close)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Server & Account Section
                if (settings.activeServer != null || settings.savedServers.isNotEmpty()) {
                    item {
                        SettingsSectionCard(title = strings.sectionServer) {
                            settings.activeServer?.let { server ->
                                SettingsValueRow(
                                    icon = Icons.Filled.Dns,
                                    title = strings.connectedServer,
                                    subtitle = "${server.name} (${server.url})"
                                )
                                SettingsDivider()
                                SettingsValueRow(
                                    icon = Icons.Filled.Person,
                                    title = strings.activeUser,
                                    subtitle = if (server.serverVersion.isNotBlank()) {
                                        "${server.username} • Jellyfin ${server.serverVersion}"
                                    } else server.username
                                )
                                SettingsDivider()
                            }
                            if (settings.savedServers.isNotEmpty()) {
                                SettingsActionRow(
                                    icon = Icons.Filled.SwapHoriz,
                                    title = strings.switchOrAddServer,
                                    subtitle = "${settings.savedServers.size}",
                                    onClick = { activePicker = SettingPickerType.SWITCH_SERVER }
                                )
                                SettingsDivider()
                            }
                            SettingsSwitchRow(
                                icon = Icons.Filled.PlayCircle,
                                title = strings.autoLogin,
                                checked = settings.autoLogin,
                                onCheckedChange = onAutoLoginChange
                            )
                            if (settings.activeServer != null) {
                                SettingsDivider()
                                SettingsActionRow(
                                    icon = Icons.AutoMirrored.Filled.Logout,
                                    title = strings.signOut,
                                    subtitle = settings.activeServer.username,
                                    isDestructive = true,
                                    onClick = onSignOut,
                                    modifier = Modifier.testTag("settings_sign_out_btn")
                                )
                            }
                        }
                    }
                }

                // 2. Streaming & Quality
                item {
                    SettingsSectionCard(title = strings.sectionPlayback) {
                        val modeLabel = when (settings.playbackMode) {
                            PlaybackMode.DIRECT_PLAY -> strings.modeDirectPlay
                            PlaybackMode.AUTO_TRANSCODE -> strings.modeAuto
                            PlaybackMode.FORCE_HLS -> strings.modeHls
                        }
                        SettingsActionRow(
                            icon = Icons.Filled.VideoSettings,
                            title = strings.playbackMode,
                            subtitle = modeLabel,
                            onClick = { activePicker = SettingPickerType.PLAYBACK_MODE }
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            icon = Icons.Filled.Wifi,
                            title = strings.wifiQuality,
                            subtitle = if (isArabic) settings.wifiQuality.labelAr else settings.wifiQuality.labelEn,
                            onClick = { activePicker = SettingPickerType.WIFI_QUALITY }
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            icon = Icons.Filled.NetworkCell,
                            title = strings.mobileQuality,
                            subtitle = if (isArabic) settings.mobileQuality.labelAr else settings.mobileQuality.labelEn,
                            onClick = { activePicker = SettingPickerType.MOBILE_QUALITY }
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            icon = Icons.Filled.HighQuality,
                            title = strings.preferredCodec,
                            subtitle = settings.preferredCodec.label,
                            onClick = { activePicker = SettingPickerType.PREFERRED_CODEC }
                        )
                    }
                }

                // 3. Player & Subtitles
                item {
                    SettingsSectionCard(title = strings.sectionPlayer) {
                        SettingsActionRow(
                            icon = Icons.Filled.Speed,
                            title = strings.defaultSpeed,
                            subtitle = "${settings.defaultSpeed}x",
                            onClick = { activePicker = SettingPickerType.DEFAULT_SPEED }
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            icon = Icons.Filled.FastForward,
                            title = strings.seekInterval,
                            subtitle = "${settings.seekSeconds} ${strings.secondsUnit}",
                            onClick = { activePicker = SettingPickerType.SEEK_INTERVAL }
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            icon = Icons.Filled.PlayCircle,
                            title = strings.autoPlayNext,
                            checked = settings.autoPlayNextEpisode,
                            onCheckedChange = onAutoPlayNextChange
                        )
                        SettingsDivider()
                        val resumeLabel = when (settings.resumeBehavior) {
                            ResumeBehavior.ASK -> strings.resumeAsk
                            ResumeBehavior.AUTO_RESUME -> strings.resumeAuto
                            ResumeBehavior.START_OVER -> strings.resumeStartOver
                        }
                        SettingsActionRow(
                            icon = Icons.Filled.Restore,
                            title = strings.resumeBehavior,
                            subtitle = resumeLabel,
                            onClick = { activePicker = SettingPickerType.RESUME_BEHAVIOR }
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            icon = Icons.Filled.GraphicEq,
                            title = strings.preferredAudioLang,
                            subtitle = langDisplayName(settings.preferredAudioLang, isArabic),
                            onClick = { activePicker = SettingPickerType.AUDIO_LANG }
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            icon = Icons.Filled.ClosedCaption,
                            title = strings.preferredSubLang,
                            subtitle = langDisplayName(settings.preferredSubLang, isArabic),
                            onClick = { activePicker = SettingPickerType.SUB_LANG }
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            icon = Icons.Filled.FormatSize,
                            title = strings.subtitleSize,
                            subtitle = if (isArabic) settings.subtitleSize.labelAr else settings.subtitleSize.labelEn,
                            onClick = { activePicker = SettingPickerType.SUB_SIZE }
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            icon = Icons.Filled.Brightness6,
                            title = strings.keepScreenOn,
                            checked = settings.keepScreenOn,
                            onCheckedChange = onKeepScreenOnChange
                        )
                    }
                }

                // 4. Appearance & Language
                item {
                    SettingsSectionCard(title = strings.sectionAppearance) {
                        val themeLabel = when (settings.themeMode) {
                            ThemeMode.CINEMA_DARK -> strings.themeCinemaDark
                            ThemeMode.AMOLED -> strings.themeAmoled
                            ThemeMode.LIGHT -> strings.themeLight
                            ThemeMode.SYSTEM -> strings.themeSystem
                        }
                        SettingsActionRow(
                            icon = Icons.Filled.Palette,
                            title = strings.appTheme,
                            subtitle = themeLabel,
                            onClick = { activePicker = SettingPickerType.THEME_MODE },
                            modifier = Modifier.testTag("settings_theme_row")
                        )
                        SettingsDivider()
                        val langLabel = when (settings.language) {
                            AppLanguage.ARABIC -> strings.langArabic
                            AppLanguage.ENGLISH -> strings.langEnglish
                            AppLanguage.SYSTEM -> strings.langSystem
                        }
                        SettingsActionRow(
                            icon = Icons.Filled.Language,
                            title = strings.appLanguage,
                            subtitle = langLabel,
                            onClick = { activePicker = SettingPickerType.APP_LANGUAGE },
                            modifier = Modifier.testTag("settings_language_row")
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            icon = Icons.Filled.PlayCircle,
                            title = strings.showProgressBars,
                            checked = settings.showProgressBars,
                            onCheckedChange = onShowProgressBarsChange
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            icon = Icons.Filled.GridView,
                            title = strings.gridDensity,
                            subtitle = if (settings.gridDensity == GridDensity.COMFORTABLE) {
                                strings.densityComfortable
                            } else strings.densityCompact,
                            onClick = { activePicker = SettingPickerType.GRID_DENSITY }
                        )
                    }
                }

                // 5. Cache & Network
                item {
                    SettingsSectionCard(title = strings.sectionStorage) {
                        SettingsActionRow(
                            icon = Icons.Filled.Timer,
                            title = strings.connectionTimeout,
                            subtitle = "${settings.timeoutSeconds} ${strings.secondsUnit}",
                            onClick = { activePicker = SettingPickerType.TIMEOUT }
                        )
                        SettingsDivider()
                        val mbText = String.format("%.1f MB", cacheSizeBytes / (1024f * 1024f))
                        SettingsActionRow(
                            icon = Icons.Filled.DeleteSweep,
                            title = strings.clearCache,
                            subtitle = "${strings.imageCacheSize}: $mbText",
                            onClick = {
                                onClearCache()
                                scope.launch {
                                    snackbarHostState.showSnackbar(strings.cacheCleared)
                                }
                            },
                            modifier = Modifier.testTag("settings_clear_cache_btn")
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Modal Pickers
    if (activePicker != SettingPickerType.NONE) {
        ModalBottomSheet(onDismissRequest = { activePicker = SettingPickerType.NONE }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                when (activePicker) {
                    SettingPickerType.SWITCH_SERVER -> {
                        SheetTitle(strings.switchOrAddServer)
                        LazyColumn {
                            items(settings.savedServers, key = { it.id }) { srv ->
                                val active = srv.id == settings.activeServer?.id
                                ListItem(
                                    headlineContent = { Text("${srv.name} (${srv.username})") },
                                    supportingContent = { Text(srv.url) },
                                    trailingContent = {
                                        if (active) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    modifier = Modifier.clickable {
                                        activePicker = SettingPickerType.NONE
                                        onSwitchServer(srv)
                                    }
                                )
                            }
                            item {
                                ListItem(
                                    headlineContent = { Text(strings.connectTitle, color = MaterialTheme.colorScheme.primary) },
                                    leadingContent = { Icon(Icons.Filled.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    modifier = Modifier.clickable {
                                        activePicker = SettingPickerType.NONE
                                        onSignOut()
                                    }
                                )
                            }
                        }
                    }

                    SettingPickerType.PLAYBACK_MODE -> {
                        SheetTitle(strings.playbackMode)
                        listOf(
                            Triple(PlaybackMode.DIRECT_PLAY, strings.modeDirectPlay, strings.modeDirectPlayDesc),
                            Triple(PlaybackMode.AUTO_TRANSCODE, strings.modeAuto, strings.modeAutoDesc),
                            Triple(PlaybackMode.FORCE_HLS, strings.modeHls, strings.modeHlsDesc)
                        ).forEach { (mode, label, desc) ->
                            PickerRow(
                                label = label,
                                subtitle = desc,
                                selected = settings.playbackMode == mode,
                                onClick = {
                                    onPlaybackModeChange(mode)
                                    activePicker = SettingPickerType.NONE
                                }
                            )
                        }
                    }

                    SettingPickerType.WIFI_QUALITY -> {
                        SheetTitle(strings.wifiQuality)
                        StreamQuality.entries.forEach { q ->
                            PickerRow(
                                label = if (isArabic) q.labelAr else q.labelEn,
                                selected = settings.wifiQuality == q,
                                onClick = {
                                    onWifiQualityChange(q)
                                    activePicker = SettingPickerType.NONE
                                }
                            )
                        }
                    }

                    SettingPickerType.MOBILE_QUALITY -> {
                        SheetTitle(strings.mobileQuality)
                        StreamQuality.entries.forEach { q ->
                            PickerRow(
                                label = if (isArabic) q.labelAr else q.labelEn,
                                selected = settings.mobileQuality == q,
                                onClick = {
                                    onMobileQualityChange(q)
                                    activePicker = SettingPickerType.NONE
                                }
                            )
                        }
                    }

                    SettingPickerType.PREFERRED_CODEC -> {
                        SheetTitle(strings.preferredCodec)
                        PreferredCodec.entries.forEach { codec ->
                            PickerRow(
                                label = codec.label,
                                selected = settings.preferredCodec == codec,
                                onClick = {
                                    onCodecChange(codec)
                                    activePicker = SettingPickerType.NONE
                                }
                            )
                        }
                    }

                    SettingPickerType.DEFAULT_SPEED -> {
                        SheetTitle(strings.defaultSpeed)
                        listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                            PickerRow(
                                label = "${spd}x",
                                selected = settings.defaultSpeed == spd,
                                onClick = {
                                    onDefaultSpeedChange(spd)
                                    activePicker = SettingPickerType.NONE
                                }
                            )
                        }
                    }

                    SettingPickerType.SEEK_INTERVAL -> {
                        SheetTitle(strings.seekInterval)
                        listOf(5, 10, 15, 30, 60).forEach { sec ->
                            PickerRow(
                                label = "$sec ${strings.secondsUnit}",
                                selected = settings.seekSeconds == sec,
                                onClick = {
                                    onSeekSecondsChange(sec)
                                    activePicker = SettingPickerType.NONE
                                }
                            )
                        }
                    }

                    SettingPickerType.RESUME_BEHAVIOR -> {
                        SheetTitle(strings.resumeBehavior)
                        listOf(
                            ResumeBehavior.ASK to strings.resumeAsk,
                            ResumeBehavior.AUTO_RESUME to strings.resumeAuto,
                            ResumeBehavior.START_OVER to strings.resumeStartOver
                        ).forEach { (beh, label) ->
                            PickerRow(
                                label = label,
                                selected = settings.resumeBehavior == beh,
                                onClick = {
                                    onResumeBehaviorChange(beh)
                                    activePicker = SettingPickerType.NONE
                                }
                            )
                        }
                    }

                    SettingPickerType.AUDIO_LANG -> {
                        SheetTitle(strings.preferredAudioLang)
                        listOf("auto", "ara", "eng", "jpn", "fra").forEach { code ->
                            PickerRow(
                                label = langDisplayName(code, isArabic),
                                selected = settings.preferredAudioLang == code,
                                onClick = {
                                    onAudioLangChange(code)
                                    activePicker = SettingPickerType.NONE
                                }
                            )
                        }
                    }

                    SettingPickerType.SUB_LANG -> {
                        SheetTitle(strings.preferredSubLang)
                        listOf("ara", "eng", "auto", "off").forEach { code ->
                            PickerRow(
                                label = langDisplayName(code, isArabic),
                                selected = settings.preferredSubLang == code,
                                onClick = {
                                    onSubLangChange(code)
                                    activePicker = SettingPickerType.NONE
                                }
                            )
                        }
                    }

                    SettingPickerType.SUB_SIZE -> {
                        SheetTitle(strings.subtitleSize)
                        SubtitleSize.entries.forEach { sz ->
                            PickerRow(
                                label = if (isArabic) sz.labelAr else sz.labelEn,
                                selected = settings.subtitleSize == sz,
                                onClick = {
                                    onSubtitleSizeChange(sz)
                                    activePicker = SettingPickerType.NONE
                                }
                            )
                        }
                    }

                    SettingPickerType.THEME_MODE -> {
                        SheetTitle(strings.appTheme)
                        listOf(
                            ThemeMode.CINEMA_DARK to strings.themeCinemaDark,
                            ThemeMode.AMOLED to strings.themeAmoled,
                            ThemeMode.LIGHT to strings.themeLight,
                            ThemeMode.SYSTEM to strings.themeSystem
                        ).forEach { (mode, label) ->
                            PickerRow(
                                label = label,
                                selected = settings.themeMode == mode,
                                onClick = {
                                    onThemeModeChange(mode)
                                    activePicker = SettingPickerType.NONE
                                }
                            )
                        }
                    }

                    SettingPickerType.APP_LANGUAGE -> {
                        SheetTitle(strings.appLanguage)
                        listOf(
                            AppLanguage.ARABIC to strings.langArabic,
                            AppLanguage.ENGLISH to strings.langEnglish,
                            AppLanguage.SYSTEM to strings.langSystem
                        ).forEach { (lang, label) ->
                            PickerRow(
                                label = label,
                                selected = settings.language == lang,
                                onClick = {
                                    onLanguageChange(lang)
                                    activePicker = SettingPickerType.NONE
                                }
                            )
                        }
                    }

                    SettingPickerType.GRID_DENSITY -> {
                        SheetTitle(strings.gridDensity)
                        listOf(
                            GridDensity.COMFORTABLE to strings.densityComfortable,
                            GridDensity.COMPACT to strings.densityCompact
                        ).forEach { (density, label) ->
                            PickerRow(
                                label = label,
                                selected = settings.gridDensity == density,
                                onClick = {
                                    onGridDensityChange(density)
                                    activePicker = SettingPickerType.NONE
                                }
                            )
                        }
                    }

                    SettingPickerType.TIMEOUT -> {
                        SheetTitle(strings.connectionTimeout)
                        listOf(10, 20, 30, 45).forEach { sec ->
                            PickerRow(
                                label = "$sec ${strings.secondsUnit}",
                                selected = settings.timeoutSeconds == sec,
                                onClick = {
                                    onTimeoutChange(sec)
                                    activePicker = SettingPickerType.NONE
                                }
                            )
                        }
                    }

                    SettingPickerType.NONE -> {}
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
        )
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun SettingsValueRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    val tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val textColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = textColor)
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SheetTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun PickerRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

private fun langDisplayName(code: String, isArabic: Boolean): String {
    return when (code) {
        "ara" -> if (isArabic) "العربية" else "Arabic"
        "eng" -> if (isArabic) "الإنجليزية" else "English"
        "jpn" -> if (isArabic) "اليابانية" else "Japanese"
        "fra" -> if (isArabic) "الفرنسية" else "French"
        "off" -> if (isArabic) "إيقاف" else "Off"
        else -> if (isArabic) "تلقائي (افتراضي الخادم)" else "Auto (Server Default)"
    }
}
