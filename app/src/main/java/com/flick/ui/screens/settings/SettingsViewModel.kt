package com.flick.ui.screens.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flick.overlay.OverlayPreferences
import com.flick.ui.theme.ColorMode
import com.flick.ui.theme.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class SettingsUiState(
    val showAppNames: Boolean = true,
    val blurIntensity: Float = 0f,
    val popupOpacity: Float = 0.92f,
    val rightPopup: Boolean = false,
    val iconSpacing: Float = 6f,
    val showIconBorder: Boolean = false,
    val slideAnimation: Boolean = false,
    val bottomBounce: Boolean = false,
    val bottomSlideUp: Boolean = false,
    val rightBounce: Boolean = true,
    val rightSlideIn: Boolean = false,
    val rightPopupYOffset: Float = 0f,
    val bottomIconSlideDirection: String = "RIGHT",
    val rightIconSlideDirection: String = "RIGHT",
    val amoledMode: Boolean = false,
    val gridView: Boolean = false,
    val colorMode: ColorMode = ColorMode.DYNAMIC,
    val animationsEnabled: Boolean = true,
    val animationIntensity: Float = 1f
)

/**
 * Backs [AppSettingsScreen]'s preference-driven UI. Collects every [OverlayPreferences]/
 * [ThemePreferences] flow once here (instead of via ~15 individual `LaunchedEffect` blocks in the
 * composable) and exposes a single [uiState] that section composables read narrow slices of.
 *
 * Slider-backed fields use a "live update, commit on release" pattern: [onBlurIntensityChange] and
 * friends update [uiState] immediately for smooth dragging, while `commit*` persists to DataStore
 * only once the user finishes interacting (mirroring the previous `onValueChangeFinished` behavior).
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val overlayPreferences: OverlayPreferences,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        bind(overlayPreferences.showAppNames) { copy(showAppNames = it) }
        bind(overlayPreferences.blurIntensity) { copy(blurIntensity = it) }
        bind(overlayPreferences.popupOpacity) { copy(popupOpacity = it) }
        bind(overlayPreferences.rightPopup) { copy(rightPopup = it) }
        bind(overlayPreferences.iconSpacing) { copy(iconSpacing = it) }
        bind(overlayPreferences.showIconBorder) { copy(showIconBorder = it) }
        bind(overlayPreferences.slideAnimation) { copy(slideAnimation = it) }
        bind(overlayPreferences.bottomBounce) { copy(bottomBounce = it) }
        bind(overlayPreferences.bottomSlideUp) { copy(bottomSlideUp = it) }
        bind(overlayPreferences.rightBounce) { copy(rightBounce = it) }
        bind(overlayPreferences.rightSlideIn) { copy(rightSlideIn = it) }
        bind(overlayPreferences.rightPopupYOffset) { copy(rightPopupYOffset = it) }
        bind(overlayPreferences.bottomIconSlideDirection) { copy(bottomIconSlideDirection = it) }
        bind(overlayPreferences.rightIconSlideDirection) { copy(rightIconSlideDirection = it) }
        bind(themePreferences.amoledMode) { copy(amoledMode = it) }
        bind(themePreferences.gridView) { copy(gridView = it) }
        bind(themePreferences.colorMode) { copy(colorMode = it) }
        bind(themePreferences.animationsEnabled) { copy(animationsEnabled = it) }
        bind(themePreferences.animationIntensity) { copy(animationIntensity = it) }
    }

    private fun <T> bind(flow: kotlinx.coroutines.flow.Flow<T>, reducer: SettingsUiState.(T) -> SettingsUiState) {
        viewModelScope.launch {
            flow.collect { value -> _uiState.update { state -> state.reducer(value) } }
        }
    }

    fun setShowAppNames(value: Boolean) {
        _uiState.update { it.copy(showAppNames = value) }
        viewModelScope.launch { overlayPreferences.setShowAppNames(value) }
    }

    fun onBlurIntensityChange(value: Float) {
        _uiState.update { it.copy(blurIntensity = value) }
    }

    fun commitBlurIntensity() {
        viewModelScope.launch { overlayPreferences.setBlurIntensity(_uiState.value.blurIntensity) }
    }

    fun onPopupOpacityChange(value: Float) {
        _uiState.update { it.copy(popupOpacity = value) }
    }

    fun commitPopupOpacity() {
        viewModelScope.launch { overlayPreferences.setPopupOpacity(_uiState.value.popupOpacity) }
    }

    fun setRightPopup(value: Boolean) {
        _uiState.update { it.copy(rightPopup = value) }
        viewModelScope.launch { overlayPreferences.setRightPopup(value) }
    }

    fun setRightBounce(value: Boolean) {
        _uiState.update { it.copy(rightBounce = value) }
        viewModelScope.launch { overlayPreferences.setRightBounce(value) }
    }

    fun setRightSlideIn(value: Boolean) {
        _uiState.update { it.copy(rightSlideIn = value) }
        viewModelScope.launch { overlayPreferences.setRightSlideIn(value) }
    }

    fun setRightIconSlideDirection(value: String) {
        _uiState.update { it.copy(rightIconSlideDirection = value) }
        viewModelScope.launch { overlayPreferences.setRightIconSlideDirection(value) }
    }

    fun onRightPopupYOffsetChange(value: Float) {
        _uiState.update { it.copy(rightPopupYOffset = value) }
    }

    fun commitRightPopupYOffset() {
        viewModelScope.launch { overlayPreferences.setRightPopupYOffset(_uiState.value.rightPopupYOffset) }
    }

    fun setBottomBounce(value: Boolean) {
        _uiState.update { it.copy(bottomBounce = value) }
        viewModelScope.launch { overlayPreferences.setBottomBounce(value) }
    }

    fun setBottomSlideUp(value: Boolean) {
        _uiState.update { it.copy(bottomSlideUp = value) }
        viewModelScope.launch { overlayPreferences.setBottomSlideUp(value) }
    }

    fun setBottomIconSlideDirection(value: String) {
        _uiState.update { it.copy(bottomIconSlideDirection = value) }
        viewModelScope.launch { overlayPreferences.setBottomIconSlideDirection(value) }
    }

    fun setShowIconBorder(value: Boolean) {
        _uiState.update { it.copy(showIconBorder = value) }
        viewModelScope.launch { overlayPreferences.setShowIconBorder(value) }
    }

    fun onIconSpacingChange(value: Float) {
        _uiState.update { it.copy(iconSpacing = value) }
    }

    fun commitIconSpacing() {
        viewModelScope.launch { overlayPreferences.setIconSpacing(_uiState.value.iconSpacing) }
    }

    fun setColorMode(value: ColorMode) {
        _uiState.update { it.copy(colorMode = value) }
        viewModelScope.launch { themePreferences.setColorMode(value) }
    }

    fun setAmoledMode(value: Boolean) {
        _uiState.update { it.copy(amoledMode = value) }
        viewModelScope.launch { themePreferences.setAmoledMode(value) }
    }

    fun setGridView(value: Boolean) {
        _uiState.update { it.copy(gridView = value) }
        viewModelScope.launch { themePreferences.setGridView(value) }
    }

    fun setAnimationsEnabled(value: Boolean) {
        _uiState.update { it.copy(animationsEnabled = value) }
        viewModelScope.launch { themePreferences.setAnimationsEnabled(value) }
    }

    fun onAnimationIntensityChange(value: Float) {
        _uiState.update { it.copy(animationIntensity = value) }
    }

    fun commitAnimationIntensity() {
        viewModelScope.launch { themePreferences.setAnimationIntensity(_uiState.value.animationIntensity) }
    }
}
