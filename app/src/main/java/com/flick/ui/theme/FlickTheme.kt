package com.flick.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun FlickTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context.applicationContext) }
    val amoledMode by themePreferences.amoledMode.collectAsState(initial = false)
    val colorMode by themePreferences.colorMode.collectAsState(initial = ColorMode.DYNAMIC)
    val animationsEnabled by themePreferences.animationsEnabled.collectAsState(initial = true)
    val animationIntensity by themePreferences.animationIntensity.collectAsState(initial = 1f)

    val baseScheme = when (colorMode) {
        ColorMode.DYNAMIC -> if (darkTheme) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
        ColorMode.BRAND -> if (darkTheme) {
            FlickBrandDarkColors
        } else {
            FlickBrandLightColors
        }
    }

    val colorScheme = if (darkTheme && amoledMode) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF050505),
            surfaceContainer = Color(0xFF0A0A0A),
            surfaceContainerHigh = Color(0xFF0F0F0F),
            surfaceContainerHighest = Color(0xFF141414)
        )
    } else {
        baseScheme
    }

    CompositionLocalProvider(
        LocalMotion provides MotionConfig(enabled = animationsEnabled, intensity = animationIntensity)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FlickTypography,
            content = content
        )
    }
}
