package com.quotegarden.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.quotegarden.R

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val serifDownloadable = FontFamily(
    Font(googleFont = GoogleFont("Noto Serif SC"), fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Noto Serif SC"), fontProvider = fontProvider, weight = FontWeight.Normal)
)

private val sansDownloadable = FontFamily(
    Font(googleFont = GoogleFont("Inter"), fontProvider = fontProvider, weight = FontWeight.Normal)
)

val EditorialSerif: FontFamily = serifDownloadable
val EditorialSerifFallback: FontFamily = FontFamily.Serif
val EditorialSansFallback: FontFamily = FontFamily.SansSerif

val EditorialTypography = Typography(
    displayLarge = TextStyle(fontFamily = EditorialSerif, fontWeight = FontWeight.Medium, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = EditorialSerif, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 36.sp),
    bodyLarge = TextStyle(fontFamily = EditorialSansFallback, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = EditorialSansFallback, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = EditorialSansFallback, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = EditorialSansFallback, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = EditorialSansFallback, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = EditorialSansFallback, fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 0.3.sp)
)
