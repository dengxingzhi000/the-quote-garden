package com.dailymind.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// MASTER.md Palette: Education teal + course amber
private val LightColors = lightColorScheme(
    primary = Color(0xFF0D9488),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8F1F4),
    onPrimaryContainer = Color(0xFF134E4A),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF134E4A),
    secondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEDD5),
    background = Color(0xFFF0FDFA),
    onBackground = Color(0xFF134E4A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF134E4A),
    surfaceVariant = Color(0xFFE8F1F4),
    onSurfaceVariant = Color(0xFF5A6B6B),
    outline = Color(0xFF5EEAD4),
    outlineVariant = Color(0xFFCCFBF1),
    error = Color(0xFFDC2626),
    onError = Color.White,
    scrim = Color(0xFF000000).copy(alpha = 0.5f)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF0F2F2B),
    secondary = Color(0xFF5EEAD4),
    tertiary = Color(0xFFFB923C),
    background = Color(0xFF0F1F1E),
    surface = Color(0xFF1A2E2C),
    onSurface = Color(0xFFE8F1F4),
    surfaceVariant = Color(0xFF23403D),
    outline = Color(0xFF2A5552)
)

// MASTER: Baloo 2 (heading) / Comic Neue (body) -> Compose fallback to rounded sans + serif for reading
private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontSize = 11.sp, letterSpacing = 0.3.sp)
)

@Composable
fun DailyMindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // MASTER anti-pattern: avoid dark modes -> prefer light, but keep dark for accessibility
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
