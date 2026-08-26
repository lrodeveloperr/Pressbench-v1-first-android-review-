package com.goodusestudios.pressbench.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightScheme = lightColorScheme(
    primary = Color(0xFF3F778A), onPrimary = Color.White,
    primaryContainer = Color(0xFFE7F3F7), onPrimaryContainer = Color(0xFF17252D),
    secondary = Color(0xFFD97941), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF1E8), onSecondaryContainer = Color(0xFF482413),
    tertiary = Color(0xFF2D8667), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8F6EF), onTertiaryContainer = Color(0xFF123A2C),
    background = Color(0xFFEEF5F7), onBackground = Color(0xFF1B272E),
    surface = Color.White, onSurface = Color(0xFF1B272E),
    surfaceVariant = Color(0xFFF7FAFB), onSurfaceVariant = Color(0xFF53666F),
    outline = Color(0xFFD9E5E9), outlineVariant = Color(0xFFCBD9DE),
    error = Color(0xFFB84B4F), onError = Color.White, errorContainer = Color(0xFFFFF0F1),
)

@Immutable
data class PressBenchPalette(
    val backgroundTop: Color,
    val surface2: Color,
    val surface3: Color,
    val ink2: Color,
    val ink3: Color,
    val brand2: Color,
    val brand3: Color,
    val dark: Color,
    val cream: Color,
    val warm: Color,
    val warmSoft: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
)

private val LightPalette = PressBenchPalette(
    backgroundTop = Color(0xFFF9FBFC), surface2 = Color(0xFFF7FAFB), surface3 = Color(0xFFEDF4F6),
    ink2 = Color(0xFF53666F), ink3 = Color(0xFF80919A), brand2 = Color(0xFF5597AF),
    brand3 = Color(0xFF65AFC8), dark = Color(0xFF17252D), cream = Color(0xFFF3E9DA),
    warm = Color(0xFFD97941), warmSoft = Color(0xFFFFF1E8), success = Color(0xFF2D8667),
    successSoft = Color(0xFFE8F6EF), warning = Color(0xFFA86B28), warningSoft = Color(0xFFFFF4DF),
    danger = Color(0xFFB84B4F), dangerSoft = Color(0xFFFFF0F1),
)

val LocalPressBenchPalette = staticCompositionLocalOf { LightPalette }

val PressBenchTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 32.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 33.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp),
)

@Composable
fun PressBenchTheme(content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalPressBenchPalette provides LightPalette,
    ) {
        MaterialTheme(
            colorScheme = LightScheme,
            typography = PressBenchTypography,
            content = content,
        )
    }
}
