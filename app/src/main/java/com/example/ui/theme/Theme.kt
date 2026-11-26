package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.ThemeMode
import java.util.Locale

private val CinemaDarkColorScheme = darkColorScheme(
    primary = CyanGlow,
    onPrimary = Color(0xFF002026),
    primaryContainer = Color(0xFF004F5C),
    onPrimaryContainer = CyanSoft,
    secondary = VioletAccent,
    onSecondary = Color.White,
    secondaryContainer = VioletDeep,
    onSecondaryContainer = VioletSoft,
    tertiary = AmberStar,
    onTertiary = Color(0xFF261A00),
    background = CinemaBackground,
    onBackground = CinemaOnSurface,
    surface = CinemaSurface,
    onSurface = CinemaOnSurface,
    surfaceVariant = CinemaSurfaceVariant,
    onSurfaceVariant = CinemaOnSurfaceVariant,
    surfaceContainerHigh = CinemaSurfaceHigh,
    outline = CinemaOutline,
    error = CoralError,
    onError = Color.White
)

private val AmoledColorScheme = darkColorScheme(
    primary = CyanGlow,
    onPrimary = Color(0xFF002026),
    primaryContainer = Color(0xFF00404B),
    onPrimaryContainer = CyanSoft,
    secondary = VioletAccent,
    onSecondary = Color.White,
    secondaryContainer = VioletDeep,
    onSecondaryContainer = VioletSoft,
    tertiary = AmberStar,
    onTertiary = Color(0xFF261A00),
    background = AmoledBackground,
    onBackground = CinemaOnSurface,
    surface = AmoledSurface,
    onSurface = CinemaOnSurface,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = CinemaOnSurfaceVariant,
    surfaceContainerHigh = AmoledSurfaceHigh,
    outline = CinemaOutline,
    error = CoralError,
    onError = Color.White
)

private val CinemaLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC4F0FB),
    onPrimaryContainer = Color(0xFF002028),
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7DEFF),
    onSecondaryContainer = Color(0xFF1F005E),
    tertiary = AmberStar,
    onTertiary = Color(0xFF261A00),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerHigh = LightSurfaceHigh,
    outline = LightOutline,
    error = CoralError,
    onError = Color.White
)

val CineFinShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CineFinTheme(
    themeMode: ThemeMode = ThemeMode.CINEMA_DARK,
    language: AppLanguage = AppLanguage.ARABIC,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = when (themeMode) {
        ThemeMode.CINEMA_DARK -> CinemaDarkColorScheme
        ThemeMode.AMOLED -> AmoledColorScheme
        ThemeMode.LIGHT -> CinemaLightColorScheme
        ThemeMode.SYSTEM -> if (systemDark) CinemaDarkColorScheme else CinemaLightColorScheme
    }

    val isArabic = when (language) {
        AppLanguage.ARABIC -> true
        AppLanguage.ENGLISH -> false
        AppLanguage.SYSTEM -> Locale.getDefault().language == "ar"
    }
    val strings = if (isArabic) ArabicStrings else EnglishStrings
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalLayoutDirection provides layoutDirection
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CineFinTypography,
            shapes = CineFinShapes,
            content = content
        )
    }
}
