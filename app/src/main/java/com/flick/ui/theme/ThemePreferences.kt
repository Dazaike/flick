package com.flick.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")
private val AMOLED_MODE_KEY = booleanPreferencesKey("amoled_mode")
private val GRID_VIEW_KEY = booleanPreferencesKey("grid_view")
private val COLOR_MODE_KEY = stringPreferencesKey("color_mode")
private val ANIMATIONS_ENABLED_KEY = booleanPreferencesKey("animations_enabled")
private val ANIMATION_INTENSITY_KEY = floatPreferencesKey("animation_intensity")

enum class ColorMode { DYNAMIC, BRAND }

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val amoledMode: Flow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[AMOLED_MODE_KEY] ?: false
    }

    suspend fun setAmoledMode(enabled: Boolean) {
        context.themeDataStore.edit { prefs -> prefs[AMOLED_MODE_KEY] = enabled }
    }

    val gridView: Flow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[GRID_VIEW_KEY] ?: false
    }

    suspend fun setGridView(enabled: Boolean) {
        context.themeDataStore.edit { prefs -> prefs[GRID_VIEW_KEY] = enabled }
    }

    val colorMode: Flow<ColorMode> = context.themeDataStore.data.map { prefs ->
        prefs[COLOR_MODE_KEY]?.let { runCatching { ColorMode.valueOf(it) }.getOrNull() } ?: ColorMode.DYNAMIC
    }

    suspend fun setColorMode(mode: ColorMode) {
        context.themeDataStore.edit { prefs -> prefs[COLOR_MODE_KEY] = mode.name }
    }

    val animationsEnabled: Flow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[ANIMATIONS_ENABLED_KEY] ?: true
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        context.themeDataStore.edit { prefs -> prefs[ANIMATIONS_ENABLED_KEY] = enabled }
    }

    val animationIntensity: Flow<Float> = context.themeDataStore.data.map { prefs ->
        prefs[ANIMATION_INTENSITY_KEY] ?: 1f
    }

    suspend fun setAnimationIntensity(intensity: Float) {
        context.themeDataStore.edit { prefs ->
            prefs[ANIMATION_INTENSITY_KEY] = intensity.coerceIn(0.1f, 1f)
        }
    }
}
