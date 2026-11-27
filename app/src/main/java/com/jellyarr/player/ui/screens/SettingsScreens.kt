package com.jellyarr.player.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Javascript
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jellyarr.player.BuildConfig
import com.jellyarr.player.JellyarrApp
import com.jellyarr.player.data.PlaybackPrefs
import com.jellyarr.player.data.stremio.Addon
import com.jellyarr.player.data.stremio.BuiltIn
import com.jellyarr.player.ui.LocalNavigator
import com.jellyarr.player.ui.components.BackButton
import com.jellyarr.player.ui.components.GhostButton
import com.jellyarr.player.ui.components.GradientButton
import com.jellyarr.player.ui.components.NetImage
import com.jellyarr.player.ui.components.SurfaceCard
import com.jellyarr.player.ui.components.Tag
import com.jellyarr.player.ui.theme.JColors
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- shared bits

@Composable
fun SubScreen(title: String, content: @Composable () -> Unit) {
    val nav = LocalNavigator.current
    Column(Modifier.fillMaxSize().background(JColors.Background).statusBarsPadding().imePadding()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton({ nav.back() })
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, color = JColors.Text)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            content()
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingRow(icon: ImageVector, title: String, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(JColors.Primary.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = JColors.PrimarySoft, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = JColors.Text, style = MaterialTheme.typography.titleSmall)
            subtitle?.let { Text(it, color = JColors.TextMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
        when {
            trailing != null -> trailing()
            onClick != null -> Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = JColors.TextDim)
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Text(title, color = JColors.TextMuted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp, top = 18.dp, bottom = 8.dp))
    SurfaceCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(4.dp)) { content() } }
}

@Composable
fun JTextField(value: String, onChange: (String) -> Unit, label: String, password: Boolean = false, ltr: Boolean = true, keyboard: KeyboardType = KeyboardType.Text) {
    val field = @Composable {
        OutlinedTextField(
            value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
            visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JColors.Primary, unfocusedBorderColor = JColors.Outline,
                focusedLabelColor = JColors.PrimarySoft, cursorColor = JColors.Primary,
                focusedContainerColor = JColors.Surface, unfocusedContainerColor = JColors.Surface,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (ltr) CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { field() } else field()
}

@Composable
fun JSwitch(checked: Boolean, onChange: (Boolean) -> Unit) = Switch(
    checked = checked, onCheckedChange = onChange,
    colors = SwitchDefaults.colors(checkedTrackColor = JColors.Primary, checkedThumbColor = Color.White, uncheckedTrackColor = JColors.SurfaceHighest),
)

// ---------------------------------------------------------------- settings tab

@Composable
fun SettingsScreen() {
    val nav = LocalNavigator.current
    val c = JellyarrApp.container
    val settings by c.settings.state.collectAsStateWithLifecycle()
    val addons by c.addons.addons.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Text("الإعدادات", style = MaterialTheme.typography.headlineMedium, color = JColors.Text, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))

        SettingsGroup("المصادر") {
            val jf = settings.jellyfin
            SettingRow(
                Icons.Filled.Dns, "Jellyfin",
                jf?.let { "${it.userName} · ${it.serverName}" } ?: "غير مربوط",
                trailing = {
                    if (jf != null) {
                        IconButton(onClick = {
                            scope.launch {
                                c.jellyfin.logout()
                                c.settings.update { it.copy(jellyfin = null) }
                            }
                        }) { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = JColors.Error) }
                    } else {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = JColors.TextDim)
                    }
                },
                onClick = { nav.to("jflogin") },
            )
            SettingRow(Icons.Filled.Extension, "إضافات Stremio", "${addons.count { it.enabled }} مفعّلة · AIOStreams وغيرها") { nav.to("addons") }
            SettingRow(Icons.Filled.Javascript, "مزوّدات Nuvio", "${settings.nuvioRepos.size} مستودع") { nav.to("nuvio") }
        }

        SettingsGroup("التشغيل") {
            SettingRow(Icons.Filled.SmartDisplay, "التشغيل والترجمة", "المشغّل، اللغة المفضلة، الجودة") { nav.to("playback") }
            SettingRow(
                Icons.Filled.Movie, "المشغّل الخارجي", "استخدم VLC أو MX Player افتراضياً",
                trailing = { JSwitch(settings.playback.useExternalPlayer) { v -> scope.launch { c.settings.update { it.copy(playback = it.playback.copy(useExternalPlayer = v)) } } } },
            )
        }

        SettingsGroup("حول") {
            SettingRow(Icons.Filled.Info, "Jellyarr", "الإصدار ${BuildConfig.VERSION_NAME} · Jellyfin + Stremio + Nuvio")
        }
        Spacer(Modifier.height(BottomBarSpace))
    }
}

@Composable
fun PlaybackSettingsScreen() {
    val c = JellyarrApp.container
    val settings by c.settings.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val p = settings.playback
    fun set(f: (PlaybackPrefs) -> PlaybackPrefs) { scope.launch { c.settings.update { it.copy(playback = f(it.playback)) } } }
    var audio by remember { mutableStateOf(p.preferredAudioLang) }
    var sub by remember { mutableStateOf(p.preferredSubtitleLang) }
    var tmdb by remember { mutableStateOf(settings.tmdbApiKey) }

    SubScreen("التشغيل والترجمة") {
        SettingsGroup("المشغّل") {
            SettingRow(Icons.Filled.Movie, "مشغّل خارجي افتراضياً", trailing = { JSwitch(p.useExternalPlayer) { v -> set { it.copy(useExternalPlayer = v) } } })
            SettingRow(Icons.Filled.Tune, "تحويل Jellyfin دائماً (Transcode)", "مفيد للأجهزة الضعيفة أو الشبكات البطيئة",
                trailing = { JSwitch(p.jellyfinTranscode) { v -> set { it.copy(jellyfinTranscode = v) } } })
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "أقصى جودة", 40 to "40", 20 to "20", 10 to "10", 4 to "4").forEach { (v, label) ->
                    Chip(if (v == 0) label else "$label Mbps", p.maxBitrateMbps == v, { set { it.copy(maxBitrateMbps = v) } })
                }
            }
        }
        SettingsGroup("اللغة") {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                JTextField(audio, { audio = it.trim(); set { s -> s.copy(preferredAudioLang = audio) } }, "لغة الصوت المفضلة (مثال: ar, en, ja)")
                JTextField(sub, { sub = it.trim(); set { s -> s.copy(preferredSubtitleLang = sub) } }, "لغة الترجمة المفضلة (مثال: ar)")
            }
            SettingRow(Icons.Filled.Settings, "تحميل الترجمات من الإضافات تلقائياً",
                trailing = { JSwitch(p.autoLoadSubtitles) { v -> set { it.copy(autoLoadSubtitles = v) } } })
        }
        SettingsGroup("TMDB (لمزوّدات Nuvio)") {
            Column(Modifier.padding(12.dp)) {
                Text("مزوّدات Nuvio تعمل بمعرّفات TMDB. أضف مفتاح API مجاني من themoviedb.org لتحويل معرّفات IMDb.", color = JColors.TextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                JTextField(tmdb, { tmdb = it.trim(); scope.launch { c.settings.update { s -> s.copy(tmdbApiKey = tmdb) } } }, "TMDB API Key (v3)")
            }
        }
    }
}

// ---------------------------------------------------------------- Jellyfin login

@Composable
fun JellyfinLoginScreen() {
    val nav = LocalNavigator.current
    val c = JellyarrApp.container
    val settings by c.settings.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var server by remember { mutableStateOf(settings.jellyfin?.serverUrl ?: "") }
    var user by remember { mutableStateOf(settings.jellyfin?.userName ?: "") }
    var pass by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var serverName by remember { mutableStateOf<String?>(null) }

    SubScreen("ربط Jellyfin") {
        SurfaceCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(JColors.Brand), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Dns, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("خادم Jellyfin", color = JColors.Text, style = MaterialTheme.typography.titleMedium)
                        Text(serverName ?: "أدخل عنوان الخادم وبيانات الدخول", color = JColors.TextMuted, fontSize = 12.sp)
                    }
                }
                JTextField(server, { server = it; serverName = null }, "عنوان الخادم (http://192.168.1.10:8096)", keyboard = KeyboardType.Uri)
                JTextField(user, { user = it }, "اسم المستخدم")
                JTextField(pass, { pass = it }, "كلمة المرور", password = true)
                error?.let { Text(it, color = JColors.Error, fontSize = 13.sp) }
                GradientButton("دخول", {
                    loading = true; error = null
                    scope.launch {
                        runCatching { c.jellyfin.login(server, user.trim(), pass) }
                            .onSuccess { s ->
                                c.settings.update { it.copy(jellyfin = s) }
                                loading = false
                                nav.back()
                            }
                            .onFailure { e ->
                                loading = false
                                error = when {
                                    e.message?.contains("401") == true -> "اسم المستخدم أو كلمة المرور غير صحيحة"
                                    else -> "تعذّر الاتصال: ${e.message}"
                                }
                            }
                    }
                }, Modifier.fillMaxWidth(), enabled = server.isNotBlank() && user.isNotBlank(), loading = loading)
                GhostButton("اختبار الاتصال", {
                    scope.launch {
                        runCatching { c.jellyfin.publicInfo(server) }
                            .onSuccess { serverName = "✓ ${it.optString("ServerName")} · ${it.optString("Version")}"; error = null }
                            .onFailure { error = "تعذّر الوصول للخادم: ${it.message}" }
                    }
                }, Modifier.fillMaxWidth())
            }
        }
    }
}

// ---------------------------------------------------------------- addons

@Composable
fun AddonsScreen(initialUrl: String?) {
    val c = JellyarrApp.container
    val context = LocalContext.current
    val addons by c.addons.addons.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(initialUrl ?: "") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun install(u: String) {
        loading = true; error = null
        scope.launch {
            runCatching { c.addons.install(u) }
                .onSuccess { a -> loading = false; url = ""; Toast.makeText(context, "تم تثبيت ${a.manifest.name}", Toast.LENGTH_SHORT).show() }
                .onFailure { e -> loading = false; error = "فشل التثبيت: ${e.message}" }
        }
    }

    LaunchedEffect(initialUrl) { if (!initialUrl.isNullOrBlank()) install(initialUrl) }

    SubScreen("إضافات Stremio") {
        SurfaceCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("أضف رابط manifest لأي إضافة Stremio. لـ AIOStreams: اضبط الإعدادات في صفحة التهيئة ثم انسخ رابط التثبيت.", color = JColors.TextMuted, fontSize = 13.sp)
                JTextField(url, { url = it }, "https://…/manifest.json", keyboard = KeyboardType.Uri)
                error?.let { Text(it, color = JColors.Error, fontSize = 12.sp) }
                GradientButton("تثبيت", { install(url) }, Modifier.fillMaxWidth(), icon = Icons.Filled.Add, enabled = url.isNotBlank(), loading = loading)
            }
        }

        Text("المثبّتة (${addons.size})", color = JColors.TextMuted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp, top = 18.dp, bottom = 8.dp))
        if (addons.isEmpty()) {
            Text("لا توجد إضافات بعد.", color = JColors.TextDim, modifier = Modifier.padding(6.dp))
        }
        addons.forEachIndexed { i, a -> AddonRow(a, i, addons.size); Spacer(Modifier.height(10.dp)) }

        Text("مقترحة", color = JColors.TextMuted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 8.dp))
        BuiltIn.suggestedAddons.forEach { s ->
            val installed = s.url != null && addons.any { it.transportUrl == s.url }
            SurfaceCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(s.name, color = JColors.Text, style = MaterialTheme.typography.titleSmall)
                        Text(s.description, color = JColors.TextMuted, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    when {
                        installed -> Tag("مثبّتة", JColors.Success, filled = true)
                        s.url != null -> Chip("تثبيت", true, { install(s.url) })
                        s.configureUrl != null -> Chip("تهيئة", true, {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(s.configureUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun AddonRow(a: Addon, index: Int, total: Int) {
    val c = JellyarrApp.container
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val m = a.manifest
    SurfaceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(JColors.SurfaceHighest), contentAlignment = Alignment.Center) {
                    if (m.logo != null) NetImage(m.logo, Modifier.size(46.dp), ContentScale.Fit, fallbackText = m.name.take(1))
                    else Text(m.name.take(1).uppercase(), color = JColors.PrimarySoft, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(m.name, color = JColors.Text, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("v${m.version}", color = JColors.TextDim, fontSize = 11.sp)
                }
                JSwitch(a.enabled) { v -> scope.launch { c.addons.setEnabled(a, v) } }
            }
            m.description?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = JColors.TextMuted, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                m.resources.map { it.name }.distinct().forEach { r ->
                    val label = when (r) { "stream" -> "روابط"; "catalog" -> "كتالوج"; "meta" -> "بيانات"; "subtitles" -> "ترجمة"; else -> r }
                    Tag(label, if (r == "stream") JColors.Success else JColors.Info)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (m.configurable) {
                    Chip("تهيئة", false, {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.configureUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    })
                }
                Spacer(Modifier.weight(1f))
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = JColors.Primary)
                IconButton(onClick = { busy = true; scope.launch { runCatching { c.addons.refresh(a) }; busy = false } }) { Icon(Icons.Filled.Refresh, null, tint = JColors.TextMuted) }
                IconButton(onClick = { scope.launch { c.addons.move(a, -1) } }, enabled = index > 0) { Icon(Icons.Filled.ArrowUpward, null, tint = JColors.TextMuted) }
                IconButton(onClick = { scope.launch { c.addons.move(a, 1) } }, enabled = index < total - 1) { Icon(Icons.Filled.ArrowDownward, null, tint = JColors.TextMuted) }
                IconButton(onClick = { scope.launch { c.addons.remove(a) } }) { Icon(Icons.Filled.Delete, null, tint = JColors.Error) }
            }
        }
    }
}

// ---------------------------------------------------------------- Nuvio

@Composable
fun NuvioScreen() {
    val c = JellyarrApp.container
    val settings by c.settings.state.collectAsStateWithLifecycle()
    val repos by c.nuvio.repos.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { c.nuvio.ensureLoaded() }

    fun add(u: String) {
        loading = true; error = null
        scope.launch {
            runCatching { c.nuvio.addRepo(u) }
                .onSuccess { loading = false; url = "" }
                .onFailure { e -> loading = false; error = "تعذّر إضافة المستودع: ${e.message}" }
        }
    }

    SubScreen("مزوّدات Nuvio") {
        SurfaceCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "مزوّدات Nuvio هي سكربتات JavaScript تجلب الروابط مباشرة من المواقع. أضف رابط مستودع (manifest.json أو رابط GitHub).",
                    color = JColors.TextMuted, fontSize = 13.sp,
                )
                if (settings.tmdbApiKey.isBlank()) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(JColors.Warning.copy(.12f)).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Key, null, tint = JColors.Warning, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("أضف مفتاح TMDB من إعدادات التشغيل لتعمل المزوّدات مع محتوى IMDb.", color = JColors.Warning, fontSize = 12.sp)
                    }
                }
                JTextField(url, { url = it }, "https://raw.githubusercontent.com/…/manifest.json", keyboard = KeyboardType.Uri)
                error?.let { Text(it, color = JColors.Error, fontSize = 12.sp) }
                GradientButton("إضافة مستودع", { add(url) }, Modifier.fillMaxWidth(), icon = Icons.Filled.Add, enabled = url.isNotBlank(), loading = loading)
                if (BuiltIn.NUVIO_DEFAULT_REPO !in settings.nuvioRepos) {
                    GhostButton("إضافة المستودع الرسمي (tapframe)", { add(BuiltIn.NUVIO_DEFAULT_REPO) }, Modifier.fillMaxWidth())
                }
            }
        }
        repos.forEach { repo ->
            Row(Modifier.padding(start = 6.dp, top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(repo.name, color = JColors.Text, style = MaterialTheme.typography.titleSmall)
                    Text(repo.error ?: "${repo.scrapers.size} مزوّد", color = if (repo.error != null) JColors.Error else JColors.TextMuted, fontSize = 12.sp)
                }
                IconButton(onClick = { scope.launch { c.nuvio.ensureLoaded(force = true) } }) { Icon(Icons.Filled.Refresh, null, tint = JColors.TextMuted) }
                IconButton(onClick = { scope.launch { c.nuvio.removeRepo(repo.url) } }) { Icon(Icons.Filled.Delete, null, tint = JColors.Error) }
            }
            SurfaceCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(4.dp)) {
                    repo.scrapers.forEach { s ->
                        val enabled = c.nuvio.isEnabled(s, settings.disabledScrapers)
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(36.dp).clip(CircleShape).background(JColors.SurfaceHighest), contentAlignment = Alignment.Center) {
                                if (s.logo != null) NetImage(s.logo, Modifier.size(36.dp), ContentScale.Crop, fallbackText = s.name.take(1))
                                else Text(s.name.take(1), color = JColors.PrimarySoft, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.name, color = JColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                val meta = listOfNotNull(
                                    s.supportedTypes.joinToString("/").takeIf { it.isNotBlank() },
                                    s.languages.joinToString(",").takeIf { it.isNotBlank() },
                                    s.version?.let { "v$it" },
                                ).joinToString(" · ")
                                if (meta.isNotBlank()) Text(meta, color = JColors.TextDim, fontSize = 11.sp)
                            }
                            JSwitch(enabled) { v -> scope.launch { c.nuvio.setEnabled(s, v) } }
                        }
                    }
                }
            }
        }
    }
}
