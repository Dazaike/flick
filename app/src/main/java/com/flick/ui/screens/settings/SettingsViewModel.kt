package com.flick.ui.screens.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flick.overlay.OverlayPreferences
import com.flick.ui.theme.ThemePreferences
import com.flick.ui.theme.UiSettings
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
    val panelAnimationSpeed: Float = 1f,
    val iconAnimationSpeed: Float = 1f,
    val menuScale: Float = 1f,
    val cornerRadius: Float = 32f,
    val gridView: Boolean = false,
    val ui: UiSettings = UiSettings()
)

/**
 * Backs [AppSettingsScreen]'s preference-driven UI. Collects every [OverlayPreferences]/
 * [ThemePreferences] flow once here (instead of via ~15 individual `LaunchedEffect` blocks in the
 * composable) and exposes a single [uiState] that section composables read narrow slices of.
 *
 * Slider-backed fields use a "live update, commit on release" pattern: [onPopupOpacityChange] and
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
        bind(overlayPreferences.panelAnimationSpeed) { copy(panelAnimationSpeed = it) }
        bind(overlayPreferences.iconAnimationSpeed) { copy(iconAnimationSpeed = it) }
        bind(overlayPreferences.menuScale) { copy(menuScale = it) }
        bind(overlayPreferences.cornerRadius) { copy(cornerRadius = it) }
        bind(themePreferences.gridView) { copy(gridView = it) }
        bind(themePreferences.ui) { copy(ui = it) }
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

    fun onPanelAnimationSpeedChange(value: Float) {
        _uiState.update { it.copy(panelAnimationSpeed = value) }
    }

    fun commitPanelAnimationSpeed() {
        viewModelScope.launch { overlayPreferences.setPanelAnimationSpeed(_uiState.value.panelAnimationSpeed) }
    }

    fun onIconAnimationSpeedChange(value: Float) {
        _uiState.update { it.copy(iconAnimationSpeed = value) }
    }

    fun commitIconAnimationSpeed() {
        viewModelScope.launch { overlayPreferences.setIconAnimationSpeed(_uiState.value.iconAnimationSpeed) }
    }

    fun onMenuScaleChange(value: Float) {
        _uiState.update { it.copy(menuScale = value) }
    }

    fun commitMenuScale() {
        viewModelScope.launch { overlayPreferences.setMenuScale(_uiState.value.menuScale) }
    }

    fun onCornerRadiusChange(value: Float) {
        _uiState.update { it.copy(cornerRadius = value) }
    }

    fun commitCornerRadius() {
        viewModelScope.launch { overlayPreferences.setCornerRadius(_uiState.value.cornerRadius) }
    }

    fun updateUi(transform: (UiSettings) -> UiSettings) {
        _uiState.update { it.copy(ui = transform(it.ui)) }
        viewModelScope.launch { themePreferences.update(transform) }
    }

    fun setGridView(value: Boolean) {
        _uiState.update { it.copy(gridView = value) }
        viewModelScope.launch { themePreferences.setGridView(value) }
    }
}
