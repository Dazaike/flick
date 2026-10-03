package com.flick.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")
private val GRID_VIEW_KEY = booleanPreferencesKey("grid_view")
private val THEME_KEY = stringPreferencesKey("theme")
private val ACCENT_KEY = intPreferencesKey("accent")
private val HAPTICS_KEY = booleanPreferencesKey("haptics")
private val HAPTIC_STRENGTH_KEY = floatPreferencesKey("haptic_strength")
private val ANIM_SPEED_KEY = floatPreferencesKey("anim_speed")
private val MOTION_INTENSITY_KEY = floatPreferencesKey("motion_intensity")
private val REDUCE_MOTION_KEY = booleanPreferencesKey("reduce_motion")
private val BRIGHTNESS_KEY = floatPreferencesKey("brightness")

private fun Preferences.toUiSettings(): UiSettings {
    val d = UiSettings()
    return UiSettings(
        theme = ThemeMode.entries.firstOrNull { it.name == this[THEME_KEY] } ?: d.theme,
        accent = this[ACCENT_KEY] ?: d.accent,
        haptics = this[HAPTICS_KEY] ?: d.haptics,
        hapticStrength = (this[HAPTIC_STRENGTH_KEY] ?: d.hapticStrength).coerceIn(0.25f, 1f),
        animationSpeed = (this[ANIM_SPEED_KEY] ?: d.animationSpeed).coerceIn(0.5f, 2f),
        motionIntensity = (this[MOTION_INTENSITY_KEY] ?: d.motionIntensity).coerceIn(0f, 1f),
        reduceMotion = this[REDUCE_MOTION_KEY] ?: d.reduceMotion,
        brightness = (this[BRIGHTNESS_KEY] ?: d.brightness).coerceIn(0.5f, 1.5f),
    )
}

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val gridView: Flow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[GRID_VIEW_KEY] ?: false
    }

    suspend fun setGridView(enabled: Boolean) {
        context.themeDataStore.edit { prefs -> prefs[GRID_VIEW_KEY] = enabled }
    }

    val ui: Flow<UiSettings> = context.themeDataStore.data.map { it.toUiSettings() }

    /** Applies [transform] to the current settings and writes only the keys that changed. */
    suspend fun update(transform: (UiSettings) -> UiSettings) {
        context.themeDataStore.edit { prefs ->
            val old = prefs.toUiSettings()
            val new = transform(old)
            if (new.theme != old.theme) prefs[THEME_KEY] = new.theme.name
            if (new.accent != old.accent) prefs[ACCENT_KEY] = new.accent
            if (new.haptics != old.haptics) prefs[HAPTICS_KEY] = new.haptics
            if (new.hapticStrength != old.hapticStrength) {
                prefs[HAPTIC_STRENGTH_KEY] = new.hapticStrength.coerceIn(0.25f, 1f)
            }
            if (new.animationSpeed != old.animationSpeed) {
                prefs[ANIM_SPEED_KEY] = new.animationSpeed.coerceIn(0.5f, 2f)
            }
            if (new.motionIntensity != old.motionIntensity) {
                prefs[MOTION_INTENSITY_KEY] = new.motionIntensity.coerceIn(0f, 1f)
            }
            if (new.reduceMotion != old.reduceMotion) prefs[REDUCE_MOTION_KEY] = new.reduceMotion
            if (new.brightness != old.brightness) {
                prefs[BRIGHTNESS_KEY] = new.brightness.coerceIn(0.5f, 1.5f)
            }
        }
    }
}
