package com.jellyarr.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Javascript
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jellyarr.player.JellyarrApp
import com.jellyarr.player.ui.LocalNavigator
import com.jellyarr.player.ui.components.GradientButton
import com.jellyarr.player.ui.components.SurfaceCard
import com.jellyarr.player.ui.theme.JColors
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen() {
    val nav = LocalNavigator.current
    val c = JellyarrApp.container
    val settings by c.settings.state.collectAsStateWithLifecycle()
    val addons by c.addons.addons.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(JColors.Primary.copy(alpha = .35f), JColors.Background), radius = 1400f)
        )
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(36.dp))
            Box(Modifier.size(84.dp).clip(RoundedCornerShape(24.dp)).background(JColors.Brand), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(56.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text("Jellyarr", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "مكتبتك من Jellyfin، وإضافات Stremio مثل AIOStreams، ومزوّدات Nuvio — في تطبيق واحد.",
                color = JColors.TextMuted, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(30.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StepCard(Icons.Filled.Dns, "اربط Jellyfin", "تصفّح مكتبتك وتابع المشاهدة مع مزامنة التقدّم", settings.jellyfin != null) { nav.to("jflogin") }
                StepCard(Icons.Filled.Extension, "أضف إضافات Stremio", "AIOStreams، Torrentio، Cinemeta، الترجمات…", addons.isNotEmpty()) { nav.to("addons") }
                StepCard(Icons.Filled.Javascript, "مزوّدات Nuvio (اختياري)", "سكربتات تجلب روابط مباشرة دون Debrid", settings.nuvioRepos.isNotEmpty()) { nav.to("nuvio") }
            }
            Spacer(Modifier.height(30.dp))
            GradientButton(
                if (settings.jellyfin != null || addons.isNotEmpty()) "ابدأ المشاهدة" else "تخطَّ الآن",
                {
                    scope.launch {
                        c.settings.update { it.copy(onboardingDone = true) }
                        nav.nav.navigate("main") { popUpTo("onboarding") { inclusive = true } }
                    }
                },
                Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StepCard(icon: ImageVector, title: String, subtitle: String, done: Boolean, onClick: () -> Unit) {
    SurfaceCard(
        Modifier.fillMaxWidth().then(if (done) Modifier.border(1.dp, JColors.Success.copy(.5f), RoundedCornerShape(16.dp)) else Modifier),
        onClick = onClick,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(JColors.Primary.copy(.18f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = JColors.PrimarySoft)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = JColors.Text, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = JColors.TextMuted, fontSize = 12.sp)
            }
            if (done) Icon(Icons.Filled.CheckCircle, null, tint = JColors.Success)
        }
    }
}
