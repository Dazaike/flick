package com.flick.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.flick.R

val FlickFontFamily = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold)
)

private val DefaultTypography = Typography()

val FlickTypography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = FlickFontFamily),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = FlickFontFamily),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = FlickFontFamily),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = FlickFontFamily),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = FlickFontFamily),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = FlickFontFamily),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = FlickFontFamily, fontWeight = FontWeight.SemiBold),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = FlickFontFamily, fontWeight = FontWeight.SemiBold),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = FlickFontFamily, fontWeight = FontWeight.Medium),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = FlickFontFamily),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = FlickFontFamily),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = FlickFontFamily),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = FlickFontFamily, fontWeight = FontWeight.Medium),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = FlickFontFamily, fontWeight = FontWeight.Medium),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = FlickFontFamily, fontWeight = FontWeight.Medium)
)
