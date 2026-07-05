package com.flick.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Flick brand palette: electric violet primary (motion/speed identity) with a warm
// coral/amber tertiary accent. Used when the user opts out of Material You dynamic
// color via the "Color style" setting.

private val BrandVioletLight = Color(0xFF6E56CF)
private val BrandVioletOnLight = Color(0xFFFFFFFF)
private val BrandVioletContainerLight = Color(0xFFE4DDFF)
private val BrandVioletOnContainerLight = Color(0xFF22005D)

private val BrandSecondaryLight = Color(0xFF625B71)
private val BrandSecondaryOnLight = Color(0xFFFFFFFF)
private val BrandSecondaryContainerLight = Color(0xFFE8DEF8)
private val BrandSecondaryOnContainerLight = Color(0xFF1E192B)

private val BrandCoralLight = Color(0xFFFF6F61)
private val BrandCoralOnLight = Color(0xFFFFFFFF)
private val BrandCoralContainerLight = Color(0xFFFFDBD2)
private val BrandCoralOnContainerLight = Color(0xFF3A0907)

val FlickBrandLightColors = lightColorScheme(
    primary = BrandVioletLight,
    onPrimary = BrandVioletOnLight,
    primaryContainer = BrandVioletContainerLight,
    onPrimaryContainer = BrandVioletOnContainerLight,
    secondary = BrandSecondaryLight,
    onSecondary = BrandSecondaryOnLight,
    secondaryContainer = BrandSecondaryContainerLight,
    onSecondaryContainer = BrandSecondaryOnContainerLight,
    tertiary = BrandCoralLight,
    onTertiary = BrandCoralOnLight,
    tertiaryContainer = BrandCoralContainerLight,
    onTertiaryContainer = BrandCoralOnContainerLight,
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFC9C5D0),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFFCFBCFF),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFFDED8E1),
    surfaceBright = Color(0xFFFFFBFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainer = Color(0xFFF2ECF4),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9)
)

private val BrandVioletDark = Color(0xFFCFBCFF)
private val BrandVioletOnDark = Color(0xFF341F77)
private val BrandVioletContainerDark = Color(0xFF4D3A91)
private val BrandVioletOnContainerDark = Color(0xFFE4DDFF)

private val BrandSecondaryDark = Color(0xFFCBC2DB)
private val BrandSecondaryOnDark = Color(0xFF332D41)
private val BrandSecondaryContainerDark = Color(0xFF4A4458)
private val BrandSecondaryOnContainerDark = Color(0xFFE8DEF8)

private val BrandCoralDark = Color(0xFFFFB4A6)
private val BrandCoralOnDark = Color(0xFF5C1A11)
private val BrandCoralContainerDark = Color(0xFF7D2D1F)
private val BrandCoralOnContainerDark = Color(0xFFFFDBD2)

val FlickBrandDarkColors = darkColorScheme(
    primary = BrandVioletDark,
    onPrimary = BrandVioletOnDark,
    primaryContainer = BrandVioletContainerDark,
    onPrimaryContainer = BrandVioletOnContainerDark,
    secondary = BrandSecondaryDark,
    onSecondary = BrandSecondaryOnDark,
    secondaryContainer = BrandSecondaryContainerDark,
    onSecondaryContainer = BrandSecondaryOnContainerDark,
    tertiary = BrandCoralDark,
    onTertiary = BrandCoralOnDark,
    tertiaryContainer = BrandCoralContainerDark,
    onTertiaryContainer = BrandCoralOnContainerDark,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF151218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF151218),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF948F99),
    outlineVariant = Color(0xFF49454F),
    inverseSurface = Color(0xFFE6E0E9),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = Color(0xFF6750A4),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFF151218),
    surfaceBright = Color(0xFF3B383E),
    surfaceContainerLowest = Color(0xFF0F0C13),
    surfaceContainerLow = Color(0xFF1D1A20),
    surfaceContainer = Color(0xFF211E24),
    surfaceContainerHigh = Color(0xFF2B282F),
    surfaceContainerHighest = Color(0xFF36333A)
)
