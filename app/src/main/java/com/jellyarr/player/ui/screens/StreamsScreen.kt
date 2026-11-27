package com.jellyarr.player.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Javascript
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jellyarr.player.JellyarrApp
import com.jellyarr.player.core.Format
import com.jellyarr.player.data.streams.Playback
import com.jellyarr.player.data.streams.PlaybackRequest
import com.jellyarr.player.data.streams.ProviderKind
import com.jellyarr.player.data.streams.ProviderState
import com.jellyarr.player.data.streams.ProviderStatus
import com.jellyarr.player.data.streams.StreamRequest
import com.jellyarr.player.data.streams.StreamSource
import com.jellyarr.player.data.streams.StreamsState
import com.jellyarr.player.ui.LocalNavigator
import com.jellyarr.player.ui.components.BackButton
import com.jellyarr.player.ui.components.MessageState
import com.jellyarr.player.ui.components.NetImage
import com.jellyarr.player.ui.components.Shimmer
import com.jellyarr.player.ui.components.Tag
import com.jellyarr.player.ui.theme.JColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortMode(val label: String) { DEFAULT("ترتيب المصادر"), QUALITY("الجودة"), SIZE("الحجم"), SEEDERS("المشاركون") }

data class StreamFilters(
    val resolution: String = "all",
    val provider: String? = null,
    val cachedOnly: Boolean = false,
    val sort: SortMode = SortMode.DEFAULT,
)

class StreamsViewModel : ViewModel() {
    private val c = JellyarrApp.container
    val request: StreamRequest? = c.pendingStreamRequest
    private val raw = MutableStateFlow(StreamsState())
    private val _filters = MutableStateFlow(StreamFilters())
    val filters = _filters.asStateFlow()
    val state = raw.asStateFlow()

    val visible = combine(raw, _filters) { s, f -> apply(s.streams, f) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        request?.let { req -> viewModelScope.launch { c.streams.load(req).collect { raw.value = it } } }
    }

    fun setFilters(f: StreamFilters) { _filters.value = f }

    private fun bucket(res: Int) = when {
        res >= 2160 -> "2160"; res >= 1080 -> "1080"; res >= 720 -> "720"; res > 0 -> "sd"; else -> "unknown"
    }

    private fun apply(list: List<StreamSource>, f: StreamFilters): List<StreamSource> {
        var out = list.asSequence()
        if (f.resolution != "all") out = out.filter { bucket(it.tags.resolution) == f.resolution }
        f.provider?.let { p -> out = out.filter { providerKey(it) == p } }
        if (f.cachedOnly) out = out.filter { it.isPlayable && it.tags.cached != false }
        val sorted = when (f.sort) {
            SortMode.DEFAULT -> out.sortedWith(compareBy<StreamSource> { !it.isPlayable }.thenBy { it.order })
            SortMode.QUALITY -> out.sortedWith(compareBy<StreamSource> { !it.isPlayable }.thenByDescending { it.tags.resolution }.thenByDescending { it.tags.sizeBytes ?: 0 })
            SortMode.SIZE -> out.sortedByDescending { it.tags.sizeBytes ?: 0 }
            SortMode.SEEDERS -> out.sortedByDescending { it.tags.seeders ?: -1 }
        }
        return sorted.toList()
    }

    fun resolutionCounts(list: List<StreamSource>): Map<String, Int> = list.groupingBy { bucket(it.tags.resolution) }.eachCount()

    fun playback(s: StreamSource): PlaybackRequest? = request?.let { Playback.fromStream(c, it, s) }
}

fun providerKey(s: StreamSource): String = s.id.substringBeforeLast('#')

fun openExternal(context: Context, p: PlaybackRequest) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(p.url), "video/*")
        putExtra("title", listOfNotNull(p.title, p.subtitle).joinToString(" - "))
        putExtra("position", p.startPositionMs.coerceAtLeast(0).toInt())
        if (p.headers.isNotEmpty()) {
            putExtra("headers", p.headers.flatMap { listOf(it.key, it.value) }.toTypedArray())
        }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "تشغيل باستخدام").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { Toast.makeText(context, "لا يوجد مشغّل خارجي", Toast.LENGTH_SHORT).show() }
}

fun copyText(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("link", text))
    Toast.makeText(context, "تم النسخ", Toast.LENGTH_SHORT).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamsScreen(vm: StreamsViewModel = viewModel()) {
    val nav = LocalNavigator.current
    val context = LocalContext.current
    val req = vm.request
    val state by vm.state.collectAsStateWithLifecycle()
    val visible by vm.visible.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()
    val settings by JellyarrApp.container.settings.state.collectAsStateWithLifecycle()
    var sheetFor by remember { mutableStateOf<StreamSource?>(null) }
    var sortMenu by remember { mutableStateOf(false) }

    if (req == null) {
        MessageState("لا يوجد طلب", null)
        return
    }

    fun play(s: StreamSource, external: Boolean = settings.playback.useExternalPlayer) {
        if (!s.isPlayable) {
            if (s.externalUrl != null) {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(s.externalUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            } else sheetFor = s
            return
        }
        val p = vm.playback(s) ?: return
        if (external) openExternal(context, p) else nav.play(p)
    }

    Box(Modifier.fillMaxSize().background(JColors.Background)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item(key = "header") {
                Box(Modifier.fillMaxWidth().height(230.dp)) {
                    NetImage(req.backdrop ?: req.poster, Modifier.fillMaxSize(), ContentScale.Crop)
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(0f to Color.Black.copy(.55f), .5f to JColors.Background.copy(.7f), 1f to JColors.Background)
                        )
                    )
                    Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 12.dp)) {
                        if (req.logo != null) NetImage(req.logo, Modifier.height(54.dp).width(200.dp), ContentScale.Fit)
                        else Text(req.title, style = MaterialTheme.typography.headlineMedium, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        val sub = listOfNotNull(Format.episodeCode(req.season, req.episode), req.episodeTitle).joinToString(" · ")
                        if (sub.isNotBlank()) Text(sub, color = JColors.TextMuted, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        val done = state.providers.count { it.status != ProviderStatus.LOADING }
                        Text(
                            if (state.loading) "جاري البحث في ${state.providers.size} مصدر… ($done/${state.providers.size})"
                            else "${state.streams.size} رابط من ${state.providers.size} مصدر",
                            color = JColors.PrimarySoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            item(key = "providers") {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.providers, key = { it.key }) { p ->
                        ProviderChip(p, selected = filters.provider == p.key) {
                            vm.setFilters(filters.copy(provider = if (filters.provider == p.key) null else p.key))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                val counts = vm.resolutionCounts(state.streams)
                Row(Modifier.fillMaxWidth().padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    LazyRow(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val opts = listOf("all" to "الكل", "2160" to "4K", "1080" to "1080p", "720" to "720p", "sd" to "SD", "unknown" to "غير محدد")
                            .filter { it.first == "all" || (counts[it.first] ?: 0) > 0 }
                        items(opts, key = { it.first }) { (k, label) ->
                            val n = if (k == "all") state.streams.size else counts[k] ?: 0
                            Chip("$label  $n", filters.resolution == k, { vm.setFilters(filters.copy(resolution = k)) })
                        }
                        item(key = "cached") {
                            Chip("⚡ جاهز فقط", filters.cachedOnly, { vm.setFilters(filters.copy(cachedOnly = !filters.cachedOnly)) })
                        }
                    }
                    Box {
                        Box(
                            Modifier.size(38.dp).clip(CircleShape).background(JColors.SurfaceHigh).clickable { sortMenu = true },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.AutoMirrored.Filled.Sort, null, tint = JColors.Text, modifier = Modifier.size(20.dp)) }
                        DropdownMenu(sortMenu, { sortMenu = false }) {
                            SortMode.entries.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.label, color = if (m == filters.sort) JColors.PrimarySoft else JColors.Text) },
                                    onClick = { vm.setFilters(filters.copy(sort = m)); sortMenu = false },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            items(visible, key = { it.id }) { s ->
                StreamCard(s, onClick = { play(s) }, onLongClick = { sheetFor = s })
            }
            if (state.loading && visible.isEmpty()) {
                items(5) { Shimmer(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().height(112.dp), RoundedCornerShape(16.dp)) }
            }
            if (!state.loading && visible.isEmpty()) {
                item {
                    if (state.providers.isEmpty()) {
                        MessageState(
                            "لا توجد مصادر مفعّلة",
                            "أضف إضافة Stremio مثل AIOStreams، أو مزوّدات Nuvio، أو اربط Jellyfin.",
                            icon = Icons.Filled.Extension, actionText = "إدارة الإضافات", onAction = { nav.to("addons") },
                        )
                    } else {
                        MessageState("لم يُعثر على روابط", "جرّب تغيير الفلاتر أو تفعيل مصادر أخرى", icon = Icons.Filled.SearchOff)
                    }
                }
            }
        }
        BackButton({ nav.back() }, Modifier.statusBarsPadding().padding(12.dp))
    }

    sheetFor?.let { s ->
        ModalBottomSheet(onDismissRequest = { sheetFor = null }, containerColor = JColors.SurfaceHigh) {
            Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
                Text(s.name.lines().first(), style = MaterialTheme.typography.titleMedium, color = JColors.Text)
                s.filename?.let { Text(it, color = JColors.TextMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                Spacer(Modifier.height(12.dp))
                if (s.isPlayable) {
                    SheetAction(Icons.Filled.PlayArrow, "تشغيل في Jellyarr") { sheetFor = null; play(s, external = false) }
                    SheetAction(Icons.AutoMirrored.Filled.OpenInNew, "تشغيل بمشغّل خارجي") { sheetFor = null; play(s, external = true) }
                    SheetAction(Icons.Filled.ContentCopy, "نسخ الرابط") { copyText(context, s.url!!); sheetFor = null }
                    SheetAction(Icons.Filled.Share, "مشاركة") {
                        sheetFor = null
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, s.url), null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                } else {
                    Text(
                        "هذا رابط تورنت مباشر ولا يمكن تشغيله دون خدمة Debrid. اضبط AIOStreams مع Real-Debrid أو TorBox للحصول على روابط مباشرة، أو افتح الرابط المغناطيسي بتطبيق تورنت.",
                        color = JColors.TextMuted, style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    s.magnet?.let { m ->
                        SheetAction(Icons.AutoMirrored.Filled.OpenInNew, "فتح بتطبيق تورنت") {
                            sheetFor = null
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(m)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                                .onFailure { Toast.makeText(context, "لا يوجد تطبيق تورنت", Toast.LENGTH_SHORT).show() }
                        }
                        SheetAction(Icons.Filled.ContentCopy, "نسخ الرابط المغناطيسي") { copyText(context, m); sheetFor = null }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 14.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = JColors.PrimarySoft)
        Spacer(Modifier.width(14.dp))
        Text(label, color = JColors.Text, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun ProviderChip(p: ProviderState, selected: Boolean, onClick: () -> Unit) {
    val icon = when (p.kind) {
        ProviderKind.JELLYFIN -> Icons.Filled.Dns
        ProviderKind.ADDON -> Icons.Filled.Extension
        ProviderKind.NUVIO -> Icons.Filled.Javascript
    }
    Row(
        Modifier.clip(RoundedCornerShape(12.dp))
            .background(if (selected) JColors.Primary.copy(alpha = .22f) else JColors.Surface)
            .border(1.dp, if (selected) JColors.Primary else JColors.OutlineSoft, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (p.logo != null) NetImage(p.logo, Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)), ContentScale.Fit)
        else Icon(icon, null, tint = JColors.TextMuted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(p.name, color = JColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Spacer(Modifier.width(6.dp))
        when (p.status) {
            ProviderStatus.LOADING -> CircularProgressIndicator(Modifier.size(12.dp), color = JColors.PrimarySoft, strokeWidth = 1.5.dp)
            ProviderStatus.DONE -> Box(
                Modifier.clip(RoundedCornerShape(6.dp)).background(if (p.count > 0) JColors.Success.copy(.2f) else JColors.SurfaceHighest)
                    .padding(horizontal = 5.dp)
            ) { Text("${p.count}", color = if (p.count > 0) JColors.Success else JColors.TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            ProviderStatus.ERROR -> Icon(Icons.Filled.ErrorOutline, p.error, tint = JColors.Error, modifier = Modifier.size(14.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun StreamCard(s: StreamSource, onClick: () -> Unit, onLongClick: () -> Unit) {
    val t = s.tags
    val resColor = JColors.resolution(t.resolution)
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(JColors.Surface)
            .border(1.dp, JColors.OutlineSoft, RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .alpha(if (s.isPlayable) 1f else .6f)
            .padding(12.dp),
    ) {
        // left rail: resolution + provider
        Column(Modifier.width(78.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(resColor.copy(alpha = .32f), resColor.copy(alpha = .12f))))
                    .border(1.dp, resColor.copy(alpha = .5f), RoundedCornerShape(10.dp))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(t.resolutionLabel ?: "—", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                s.provider, color = JColors.TextMuted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center, lineHeight = 13.sp,
            )
            if (t.cached == true || s.providerKind == ProviderKind.JELLYFIN) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Bolt, null, tint = JColors.Warning, modifier = Modifier.size(14.dp))
                    Text(if (s.providerKind == ProviderKind.JELLYFIN) "محلي" else "جاهز", color = JColors.Warning, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                s.name.replace("\n", " · "), color = JColors.Text, style = MaterialTheme.typography.titleSmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (s.description.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    s.description.trim(), color = JColors.TextMuted, fontSize = 12.sp, lineHeight = 17.sp,
                    maxLines = 7, overflow = TextOverflow.Ellipsis,
                )
            }
            val chips = buildList {
                t.hdr.forEach { add(it to JColors.Gold) }
                t.source?.let { add(it to JColors.Info) }
                t.codec?.let { add(it to JColors.TextMuted) }
                t.audio.take(2).forEach { add(it to JColors.PrimarySoft) }
                t.channels?.let { add(it to JColors.PrimarySoft) }
                Format.bytes(t.sizeBytes)?.let { add(it to JColors.Text) }
                t.seeders?.let { add("👤 $it" to JColors.Success) }
                t.debrid?.let { add(it to if (t.cached == true) JColors.Warning else JColors.TextDim) }
                if (!s.isPlayable && s.infoHash != null) add("تورنت" to JColors.Error)
                t.languages.take(4).forEach { add(it to JColors.TextMuted) }
            }
            if (chips.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    chips.forEach { (text, color) -> Tag(text, color) }
                }
            }
        }
    }
}
