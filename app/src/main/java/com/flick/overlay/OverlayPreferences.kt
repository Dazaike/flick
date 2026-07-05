package com.flick.overlay

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.overlayDataStore by preferencesDataStore(name = "overlay_prefs")
private val SHOW_APP_NAMES_KEY = booleanPreferencesKey("show_app_names")
private val BLUR_INTENSITY_KEY = floatPreferencesKey("blur_intensity")
private val POPUP_OPACITY_KEY = floatPreferencesKey("popup_opacity")
private val RIGHT_POPUP_KEY = booleanPreferencesKey("right_popup")
private val ICON_SPACING_KEY = floatPreferencesKey("icon_spacing")
private val SHOW_ICON_BORDER_KEY = booleanPreferencesKey("show_icon_border")
private val SLIDE_ANIMATION_KEY = booleanPreferencesKey("slide_animation")
private val BOTTOM_BOUNCE_KEY = booleanPreferencesKey("bottom_bounce")
private val BOTTOM_SLIDE_UP_KEY = booleanPreferencesKey("bottom_slide_up")
private val RIGHT_BOUNCE_KEY = booleanPreferencesKey("right_bounce")
private val RIGHT_SLIDE_IN_KEY = booleanPreferencesKey("right_slide_in")
private val RIGHT_POPUP_Y_OFFSET_KEY = floatPreferencesKey("right_popup_y_offset")
private val BOTTOM_ICON_SLIDE_DIRECTION_KEY = stringPreferencesKey("bottom_icon_slide_direction")
private val RIGHT_ICON_SLIDE_DIRECTION_KEY = stringPreferencesKey("right_icon_slide_direction")

/** Single source of truth for default values + Preferences -> [OverlayPrefsData] mapping. */
private fun androidx.datastore.preferences.core.Preferences.toOverlayPrefsData(): OverlayPrefsData =
    OverlayPrefsData(
        showAppNames = this[SHOW_APP_NAMES_KEY] ?: true,
        blurIntensity = this[BLUR_INTENSITY_KEY] ?: 0f,
        popupOpacity = this[POPUP_OPACITY_KEY] ?: 0.92f,
        rightPopup = this[RIGHT_POPUP_KEY] ?: false,
        iconSpacing = this[ICON_SPACING_KEY] ?: 6f,
        showIconBorder = this[SHOW_ICON_BORDER_KEY] ?: false,
        slideAnimation = this[SLIDE_ANIMATION_KEY] ?: false,
        bottomBounce = this[BOTTOM_BOUNCE_KEY] ?: false,
        bottomSlideUp = this[BOTTOM_SLIDE_UP_KEY] ?: false,
        rightBounce = this[RIGHT_BOUNCE_KEY] ?: true,
        rightSlideIn = this[RIGHT_SLIDE_IN_KEY] ?: false,
        rightPopupYOffset = this[RIGHT_POPUP_Y_OFFSET_KEY] ?: 0f,
        bottomIconSlideDirection = this[BOTTOM_ICON_SLIDE_DIRECTION_KEY] ?: "RIGHT",
        rightIconSlideDirection = this[RIGHT_ICON_SLIDE_DIRECTION_KEY] ?: "RIGHT"
    )

@Singleton
class OverlayPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val showAppNames: Flow<Boolean> = context.overlayDataStore.data.map { it.toOverlayPrefsData().showAppNames }

    suspend fun setShowAppNames(show: Boolean) {
        context.overlayDataStore.edit { prefs -> prefs[SHOW_APP_NAMES_KEY] = show }
    }

    /** 0f = no blur behind the popup, 1f = maximum blur radius. */
    val blurIntensity: Flow<Float> = context.overlayDataStore.data.map { it.toOverlayPrefsData().blurIntensity }

    suspend fun setBlurIntensity(intensity: Float) {
        context.overlayDataStore.edit { prefs -> prefs[BLUR_INTENSITY_KEY] = intensity }
    }

    val popupOpacity: Flow<Float> = context.overlayDataStore.data.map { it.toOverlayPrefsData().popupOpacity }

    suspend fun setPopupOpacity(opacity: Float) {
        context.overlayDataStore.edit { prefs -> prefs[POPUP_OPACITY_KEY] = opacity }
    }

    val rightPopup: Flow<Boolean> = context.overlayDataStore.data.map { it.toOverlayPrefsData().rightPopup }

    suspend fun setRightPopup(rightPopup: Boolean) {
        context.overlayDataStore.edit { prefs -> prefs[RIGHT_POPUP_KEY] = rightPopup }
    }

    val iconSpacing: Flow<Float> = context.overlayDataStore.data.map { it.toOverlayPrefsData().iconSpacing }

    suspend fun setIconSpacing(spacing: Float) {
        context.overlayDataStore.edit { prefs -> prefs[ICON_SPACING_KEY] = spacing }
    }

    val showIconBorder: Flow<Boolean> = context.overlayDataStore.data.map { it.toOverlayPrefsData().showIconBorder }
    suspend fun setShowIconBorder(show: Boolean) {
        context.overlayDataStore.edit { prefs -> prefs[SHOW_ICON_BORDER_KEY] = show }
    }

    val slideAnimation: Flow<Boolean> = context.overlayDataStore.data.map { it.toOverlayPrefsData().slideAnimation }

    suspend fun setSlideAnimation(slide: Boolean) {
        context.overlayDataStore.edit { prefs -> prefs[SLIDE_ANIMATION_KEY] = slide }
    }

    val bottomBounce: Flow<Boolean> = context.overlayDataStore.data.map { it.toOverlayPrefsData().bottomBounce }

    suspend fun setBottomBounce(bounce: Boolean) {
        context.overlayDataStore.edit { prefs -> prefs[BOTTOM_BOUNCE_KEY] = bounce }
    }

    val bottomSlideUp: Flow<Boolean> = context.overlayDataStore.data.map { it.toOverlayPrefsData().bottomSlideUp }

    suspend fun setBottomSlideUp(slideUp: Boolean) {
        context.overlayDataStore.edit { prefs -> prefs[BOTTOM_SLIDE_UP_KEY] = slideUp }
    }

    val rightBounce: Flow<Boolean> = context.overlayDataStore.data.map { it.toOverlayPrefsData().rightBounce }

    suspend fun setRightBounce(bounce: Boolean) {
        context.overlayDataStore.edit { prefs -> prefs[RIGHT_BOUNCE_KEY] = bounce }
    }

    val rightSlideIn: Flow<Boolean> = context.overlayDataStore.data.map { it.toOverlayPrefsData().rightSlideIn }

    suspend fun setRightSlideIn(slideIn: Boolean) {
        context.overlayDataStore.edit { prefs -> prefs[RIGHT_SLIDE_IN_KEY] = slideIn }
    }

    val rightPopupYOffset: Flow<Float> = context.overlayDataStore.data.map { it.toOverlayPrefsData().rightPopupYOffset }

    suspend fun setRightPopupYOffset(offset: Float) {
        context.overlayDataStore.edit { prefs -> prefs[RIGHT_POPUP_Y_OFFSET_KEY] = offset }
    }
    val bottomIconSlideDirection: Flow<String> = context.overlayDataStore.data.map { it.toOverlayPrefsData().bottomIconSlideDirection }

    suspend fun setBottomIconSlideDirection(direction: String) {
        context.overlayDataStore.edit { prefs -> prefs[BOTTOM_ICON_SLIDE_DIRECTION_KEY] = direction }
    }

    val rightIconSlideDirection: Flow<String> = context.overlayDataStore.data.map { it.toOverlayPrefsData().rightIconSlideDirection }

    suspend fun setRightIconSlideDirection(direction: String) {
        context.overlayDataStore.edit { prefs -> prefs[RIGHT_ICON_SLIDE_DIRECTION_KEY] = direction }
    }

    suspend fun getAllPrefs(): OverlayPrefsData = context.overlayDataStore.data.first().toOverlayPrefsData()
}

data class OverlayPrefsData(
    val showAppNames: Boolean,
    val blurIntensity: Float,
    val popupOpacity: Float,
    val rightPopup: Boolean,
    val iconSpacing: Float,
    val showIconBorder: Boolean,
    val slideAnimation: Boolean,
    val bottomBounce: Boolean,
    val bottomSlideUp: Boolean,
    val rightBounce: Boolean,
    val rightSlideIn: Boolean,
    val rightPopupYOffset: Float,
    val bottomIconSlideDirection: String,
    val rightIconSlideDirection: String
)


