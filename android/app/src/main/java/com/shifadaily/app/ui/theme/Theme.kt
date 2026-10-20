package com.shifadaily.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shifadaily.app.data.prefs.ThemeMode

// Brand palette (contrast-checked: primary on white 4.9:1, secondary text on background 4.6:1).
val Teal = Color(0xFF087F70)
val TealDark = Color(0xFF075E54)
val Mint = Color(0xFFE8F5F1)
val Background = Color(0xFFF8FAFC)
val Ink = Color(0xFF172B2A)
val InkSecondary = Color(0xFF55657A)
val ErrorRed = Color(0xFFB3261E)
val Warning = Color(0xFF8A5300)
val WarningContainer = Color(0xFFFFF1DC)

/** Leaflet tier colours, always paired with a text label (never colour alone). */
@Immutable
data class TierColors(val freely: Color, val limited: Color, val occasional: Color, val neutral: Color)

private val LightTiers = TierColors(Color(0xFF2E7D32), Color(0xFF9A6700), Color(0xFFC62828), Color(0xFF55657A))
private val DarkTiers = TierColors(Color(0xFF81C995), Color(0xFFF2C14E), Color(0xFFF28B82), Color(0xFFB0BEC5))

@Immutable
data class ExtraColors(val warning: Color, val warningContainer: Color, val onWarningContainer: Color, val tiers: TierColors)

val LocalExtraColors = staticCompositionLocalOf {
    ExtraColors(Warning, WarningContainer, Color(0xFF4A2C00), LightTiers)
}

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = TealDark,
    secondary = TealDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6EFE8),
    onSecondaryContainer = Color(0xFF00382F),
    background = Background,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEFF3F6),
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4F7F9),
    surfaceContainer = Color(0xFFEFF3F6),
    outline = Color(0xFF8A99A8),
    outlineVariant = Color(0xFFDCE3E8),
    error = ErrorRed,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FD6C3),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF0B4F46),
    onPrimaryContainer = Color(0xFFC9F1E7),
    secondary = Color(0xFF9FD3C7),
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF1E4A43),
    onSecondaryContainer = Color(0xFFCFEDE6),
    background = Color(0xFF0F1716),
    onBackground = Color(0xFFE2ECEA),
    surface = Color(0xFF151F1E),
    onSurface = Color(0xFFE2ECEA),
    surfaceVariant = Color(0xFF243130),
    onSurfaceVariant = Color(0xFFB8C7C4),
    surfaceContainerLowest = Color(0xFF0B1211),
    surfaceContainerLow = Color(0xFF172221),
    surfaceContainer = Color(0xFF1B2726),
    outline = Color(0xFF85928F),
    outlineVariant = Color(0xFF34413F),
    error = Color(0xFFF2B8B5),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

// System fonts are used deliberately: Roboto for Latin and Noto Naskh / Noto Sans Arabic for Arabic
// are present on every supported Android version, render Arabic correctly and add no APK weight.
// Line heights are a little generous so Arabic diacritics are never clipped.
private val base = Typography()
private val AppTypography = Typography(
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 38.sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold, lineHeight = 34.sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, lineHeight = 30.sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyLarge = base.bodyLarge.copy(lineHeight = 26.sp),
    bodyMedium = base.bodyMedium.copy(lineHeight = 22.sp),
    bodySmall = base.bodySmall.copy(lineHeight = 18.sp),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun ShifaTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val extra = if (dark) {
        ExtraColors(Color(0xFFF2C14E), Color(0xFF3D2E0B), Color(0xFFFFE3A8), DarkTiers)
    } else {
        ExtraColors(Warning, WarningContainer, Color(0xFF4A2C00), LightTiers)
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalExtraColors provides extra) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

