package com.jellyarr.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.jellyarr.player.ui.CardModel
import com.jellyarr.player.ui.theme.JColors

@Composable
fun NetImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackText: String? = null,
) {
    if (url.isNullOrBlank()) {
        Placeholder(modifier, fallbackText)
        return
    }
    SubcomposeAsyncImage(
        model = url,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier,
        loading = { Shimmer(Modifier.fillMaxSize()) },
        error = { Placeholder(Modifier.fillMaxSize(), fallbackText) },
    )
}

@Composable
private fun Placeholder(modifier: Modifier, text: String?) {
    Box(
        modifier.background(Brush.linearGradient(listOf(JColors.SurfaceHighest, JColors.SurfaceHigh))),
        contentAlignment = Alignment.Center,
    ) {
        if (text != null) {
            Text(
                text, color = JColors.TextMuted, style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp),
            )
        } else {
            Icon(Icons.Filled.Movie, null, tint = JColors.TextDim, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun Shimmer(modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(0.dp)) {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue = -400f, targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart), label = "x",
    )
    Box(
        modifier.clip(shape).background(
            Brush.linearGradient(
                listOf(JColors.SurfaceHigh, JColors.SurfaceHighest, JColors.SurfaceHigh),
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x + 400f, 400f),
            )
        )
    )
}

@Composable
fun PosterCard(
    card: CardModel,
    onClick: () -> Unit,
    width: Dp = 120.dp,
    modifier: Modifier = Modifier,
) {
    Column(modifier.then(if (width == Dp.Unspecified) Modifier.fillMaxWidth() else Modifier.width(width)).clickable(onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(12.dp))
                .border(1.dp, JColors.OutlineSoft, RoundedCornerShape(12.dp))
        ) {
            NetImage(card.poster, Modifier.fillMaxSize(), fallbackText = card.title)
            if (card.badge != null) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(6.dp))
                        .background(JColors.Brand).padding(horizontal = 6.dp, vertical = 1.dp)
                ) { Text(card.badge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
            if (card.played) {
                Icon(
                    Icons.Filled.CheckCircle, null, tint = JColors.Success,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp).background(Color.Black.copy(.5f), CircleShape),
                )
            }
            card.rating?.let {
                Row(
                    Modifier.align(Alignment.BottomStart).padding(6.dp).clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = .65f)).padding(horizontal = 5.dp, vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Star, null, tint = JColors.Gold, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(it, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            card.progress?.let { ProgressStrip(it, Modifier.align(Alignment.BottomCenter)) }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            card.title, style = MaterialTheme.typography.labelLarge, color = JColors.Text,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        card.subtitle?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = JColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun LandscapeCard(card: CardModel, onClick: () -> Unit, width: Dp = 260.dp, modifier: Modifier = Modifier) {
    Column(modifier.width(width).clickable(onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp))
                .border(1.dp, JColors.OutlineSoft, RoundedCornerShape(14.dp))
        ) {
            NetImage(card.backdrop?.takeIf { card.poster == null } ?: card.poster ?: card.backdrop, Modifier.fillMaxSize(), fallbackText = card.title)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.75f)))))
            Box(
                Modifier.align(Alignment.Center).size(42.dp).clip(CircleShape).background(Color.Black.copy(.45f))
                    .border(1.dp, Color.White.copy(.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            card.progress?.let { ProgressStrip(it, Modifier.align(Alignment.BottomCenter)) }
        }
        Spacer(Modifier.height(6.dp))
        Text(card.title, style = MaterialTheme.typography.labelLarge, color = JColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        card.subtitle?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = JColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ProgressStrip(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(4.dp).background(Color.Black.copy(.6f))) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).background(JColors.BrandHorizontal))
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, accent: Boolean = true, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (accent) {
            Box(Modifier.width(4.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(JColors.Brand))
            Spacer(Modifier.width(8.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = JColors.Text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        action?.invoke()
    }
}

@Composable
fun CardRow(
    title: String,
    cards: List<CardModel>,
    onClick: (CardModel) -> Unit,
    landscape: Boolean = false,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    if (cards.isEmpty()) return
    Column(modifier.padding(vertical = 10.dp)) {
        SectionHeader(title, action = action)
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(cards, key = { it.key }) { c ->
                if (landscape) LandscapeCard(c, { onClick(c) }) else PosterCard(c, { onClick(c) })
            }
        }
    }
}

@Composable
fun RowSkeleton(landscape: Boolean = false) {
    Column(Modifier.padding(vertical = 10.dp)) {
        Shimmer(Modifier.padding(horizontal = 16.dp).width(140.dp).height(18.dp), RoundedCornerShape(6.dp))
        Spacer(Modifier.height(12.dp))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(4) {
                if (landscape) Shimmer(Modifier.width(260.dp).aspectRatio(16f / 9f), RoundedCornerShape(14.dp))
                else Shimmer(Modifier.width(120.dp).aspectRatio(2f / 3f), RoundedCornerShape(12.dp))
            }
        }
    }
}

@Composable
fun Tag(text: String, color: Color = JColors.TextMuted, filled: Boolean = false, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(6.dp))
            .then(if (filled) Modifier.background(color.copy(alpha = .18f)) else Modifier.border(1.dp, color.copy(alpha = .45f), RoundedCornerShape(6.dp)))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Box(
        modifier.height(50.dp).clip(RoundedCornerShape(14.dp))
            .background(if (enabled) JColors.BrandHorizontal else Brush.horizontalGradient(listOf(JColors.SurfaceHighest, JColors.SurfaceHighest)))
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        else Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let { Icon(it, null, tint = Color.White, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Box(
        modifier.height(50.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .1f))
            .border(1.dp, Color.White.copy(alpha = .14f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let { Icon(it, null, tint = Color.White, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun CircleIconButton(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Color.White) {
    Box(
        modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = .4f))
            .border(1.dp, Color.White.copy(alpha = .12f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp)) }
}

@Composable
fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) =
    CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, onBack, modifier)

@Composable
fun CenterLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = JColors.Primary, strokeWidth = 3.dp)
    }
}

@Composable
fun MessageState(
    title: String,
    message: String? = null,
    icon: ImageVector = Icons.Filled.ErrorOutline,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(JColors.Primary.copy(alpha = .14f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = JColors.PrimarySoft, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = JColors.Text, textAlign = TextAlign.Center)
        message?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = JColors.TextMuted, textAlign = TextAlign.Center)
        }
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            GradientButton(actionText, onAction)
        }
    }
}

@Composable
fun SurfaceCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(16.dp)).background(JColors.Surface)
            .border(1.dp, JColors.OutlineSoft, RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content,
    )
}

@Composable
fun ScreenTitle(title: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = JColors.Text, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun IconAction(icon: ImageVector, onClick: () -> Unit, tint: Color = JColors.Text) {
    IconButton(onClick = onClick) { Icon(icon, null, tint = tint) }
}
