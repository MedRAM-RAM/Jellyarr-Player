package com.jellyarr.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jellyarr.player.R

object JColors {
    val Background = Color(0xFF07070B)
    val Surface = Color(0xFF0F0F16)
    val SurfaceHigh = Color(0xFF171721)
    val SurfaceHighest = Color(0xFF20202C)
    val Outline = Color(0xFF2A2A3A)
    val OutlineSoft = Color(0x1FFFFFFF)
    val Primary = Color(0xFF8B5CF6)
    val PrimarySoft = Color(0xFFA78BFA)
    val Secondary = Color(0xFFEC4899)
    val Text = Color(0xFFF4F4F8)
    val TextMuted = Color(0xFFA1A1B5)
    val TextDim = Color(0xFF6E6E85)
    val Success = Color(0xFF22C55E)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    val Info = Color(0xFF38BDF8)
    val Gold = Color(0xFFF5C518)

    val Brand = Brush.linearGradient(listOf(Primary, Secondary))
    val BrandHorizontal = Brush.horizontalGradient(listOf(Primary, Secondary))

    fun resolution(res: Int): Color = when {
        res >= 2160 -> Color(0xFFF59E0B)
        res >= 1080 -> Color(0xFF8B5CF6)
        res >= 720 -> Color(0xFF38BDF8)
        res > 0 -> Color(0xFF64748B)
        else -> Color(0xFF475569)
    }
}

val Cairo = FontFamily(
    Font(R.font.cairo, FontWeight.Normal),
    Font(R.font.cairo, FontWeight.Medium),
    Font(R.font.cairo, FontWeight.SemiBold),
    Font(R.font.cairo, FontWeight.Bold),
    Font(R.font.cairo, FontWeight.ExtraBold),
)

private fun style(size: Int, weight: FontWeight, line: Int = (size * 1.4).toInt()) =
    TextStyle(fontFamily = Cairo, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

private val JTypography = Typography(
    displayLarge = style(40, FontWeight.ExtraBold, 48),
    displayMedium = style(34, FontWeight.ExtraBold, 42),
    displaySmall = style(28, FontWeight.Bold, 36),
    headlineLarge = style(26, FontWeight.Bold, 34),
    headlineMedium = style(22, FontWeight.Bold, 30),
    headlineSmall = style(20, FontWeight.Bold, 28),
    titleLarge = style(18, FontWeight.Bold, 26),
    titleMedium = style(16, FontWeight.SemiBold, 22),
    titleSmall = style(14, FontWeight.SemiBold, 20),
    bodyLarge = style(15, FontWeight.Normal, 23),
    bodyMedium = style(14, FontWeight.Normal, 21),
    bodySmall = style(12, FontWeight.Normal, 18),
    labelLarge = style(14, FontWeight.SemiBold, 20),
    labelMedium = style(12, FontWeight.SemiBold, 16),
    labelSmall = style(11, FontWeight.Medium, 14),
)

private val JScheme = darkColorScheme(
    primary = JColors.Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2E1F5E),
    onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = JColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3B1530),
    onSecondaryContainer = Color(0xFFFFD8EC),
    tertiary = JColors.Info,
    background = JColors.Background,
    onBackground = JColors.Text,
    surface = JColors.Surface,
    onSurface = JColors.Text,
    surfaceVariant = JColors.SurfaceHigh,
    onSurfaceVariant = JColors.TextMuted,
    surfaceContainerLowest = JColors.Background,
    surfaceContainerLow = JColors.Surface,
    surfaceContainer = JColors.SurfaceHigh,
    surfaceContainerHigh = JColors.SurfaceHigh,
    surfaceContainerHighest = JColors.SurfaceHighest,
    outline = JColors.Outline,
    outlineVariant = JColors.OutlineSoft,
    error = JColors.Error,
    onError = Color.White,
)

@Composable
fun JellyarrTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = JScheme, typography = JTypography, content = content)
}
