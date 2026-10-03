package com.flick.ui.screens.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.flick.permissions.OverlayPermissionHelper
import com.flick.trigger.AssistantRoleHelper
import com.flick.trigger.fallback.EdgeGestureOverlayService
import com.flick.ui.prism.AccentPicker
import com.flick.ui.prism.ButtonVariant
import com.flick.ui.prism.GlassButton
import com.flick.ui.prism.GlassIconButton
import com.flick.ui.prism.GlassSegmented
import com.flick.ui.prism.GlassSwitch
import com.flick.ui.prism.GlassValueSlider
import com.flick.ui.prism.PrismIcons
import com.flick.ui.prism.PrismListItem
import com.flick.ui.prism.PrismScreen
import com.flick.ui.theme.DURATION_MEDIUM
import com.flick.ui.theme.DURATION_QUICK
import com.flick.ui.theme.LocalMotion
import com.flick.ui.theme.Prism
import com.flick.ui.theme.PrismText
import com.flick.ui.theme.ThemeMode
import com.flick.ui.theme.UiSettings
import com.kyant.backdrop.Backdrop
import kotlin.math.roundToInt

@Composable
private fun SectionHeader(
    backdrop: Backdrop,
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PrismText(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        GlassIconButton(
            backdrop = backdrop,
            icon = if (expanded) PrismIcons.ChevronUp else PrismIcons.ChevronDown,
            contentDescription = if (expanded) "Collapse" else "Expand",
            onClick = onToggle,
            variant = ButtonVariant.Ghost,
            size = 40.dp
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    supporting: String?,
    control: @Composable () -> Unit
) {
    PrismListItem(headline = title, supporting = supporting, trailing = control)
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
    format: (Float) -> String,
    onChange: (Float) -> Unit,
    onCommit: (() -> Unit)? = null,
    stepCount: Int = 0
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PrismText("$title: ${format(value)}", fontSize = 15.sp)
        GlassValueSlider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            enabled = enabled,
            stepCount = stepCount,
            valueLabel = format,
            onValueChangeFinished = onCommit
        )
    }
}

private fun percent(value: Float): String = "${(value * 100).roundToInt()}%"

private fun dpLabel(value: Float): String = "${value.roundToInt()} dp"

@Composable
private fun ExpandableSection(
    backdrop: Backdrop,
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val motion = LocalMotion.current
    SectionHeader(backdrop = backdrop, title = title, expanded = expanded, onToggle = onToggle)
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(motion.fade(DURATION_MEDIUM)) + expandVertically(motion.glide()),
        exit = fadeOut(motion.fade(DURATION_QUICK)) + shrinkVertically(motion.glide())
    ) {
        content()
    }
}

@Composable
private fun AssistantTriggerSection(
    backdrop: Backdrop,
    roleHeld: Boolean,
    onRoleRequestResult: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val roleRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val updatedRoleHeld = AssistantRoleHelper.isRoleHeld(context)
        onRoleRequestResult(updatedRoleHeld)
        if (!updatedRoleHeld) {
            runCatching { context.startActivity(AssistantRoleHelper.createAssistantSettingsIntent(context)) }
                .onFailure {
                    Toast.makeText(context, "Couldn't open Assistant settings", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PrismText("Primary trigger: Assistant gesture", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        PrismText(
            if (roleHeld) "Flick currently holds the Assistant role." else "Assistant role not held.",
            fontSize = 14.sp,
            color = Prism.subText
        )
        PrismText(
            "Note: holding this role replaces Google Assistant/Gemini system-wide on this device.",
            fontSize = 14.sp,
            color = Prism.subText
        )
        GlassButton(
            backdrop = backdrop,
            text = if (roleHeld) "Assistant settings" else "Choose default Assistant",
            onClick = {
                val intent = if (!roleHeld && AssistantRoleHelper.isRoleAvailable(context)) {
                    AssistantRoleHelper.createRequestRoleIntent(context)
                } else {
                    AssistantRoleHelper.createAssistantSettingsIntent(context)
                }
                runCatching { roleRequestLauncher.launch(intent) }
                    .onFailure {
                        runCatching { context.startActivity(AssistantRoleHelper.createAssistantSettingsIntent(context)) }
                            .onFailure { Toast.makeText(context, "Couldn't open Assistant settings", Toast.LENGTH_SHORT).show() }
                    }
            },
            variant = ButtonVariant.Primary
        )
    }
}

@Composable
private fun PopupSettingsSection(
    showAppNames: Boolean,
    onShowAppNamesChange: (Boolean) -> Unit,
    popupOpacity: Float,
    onPopupOpacityChange: (Float) -> Unit,
    onPopupOpacityCommit: () -> Unit,
    rightPopup: Boolean,
    onRightPopupChange: (Boolean) -> Unit,
    rightBounce: Boolean,
    onRightBounceChange: (Boolean) -> Unit,
    rightSlideIn: Boolean,
    onRightSlideInChange: (Boolean) -> Unit,
    rightPopupYOffset: Float,
    onRightPopupYOffsetChange: (Float) -> Unit,
    onRightPopupYOffsetCommit: () -> Unit,
    bottomBounce: Boolean,
    onBottomBounceChange: (Boolean) -> Unit,
    bottomSlideUp: Boolean,
    onBottomSlideUpChange: (Boolean) -> Unit,
    showIconBorder: Boolean,
    onShowIconBorderChange: (Boolean) -> Unit,
    iconSpacing: Float,
    onIconSpacingChange: (Float) -> Unit,
    onIconSpacingCommit: () -> Unit,
    reduceMotion: Boolean,
    panelAnimationSpeed: Float,
    onPanelAnimationSpeedChange: (Float) -> Unit,
    onPanelAnimationSpeedCommit: () -> Unit,
    iconAnimationSpeed: Float,
    onIconAnimationSpeedChange: (Float) -> Unit,
    onIconAnimationSpeedCommit: () -> Unit,
    menuScale: Float,
    onMenuScaleChange: (Float) -> Unit,
    onMenuScaleCommit: () -> Unit,
    cornerRadius: Float,
    onCornerRadiusChange: (Float) -> Unit,
    onCornerRadiusCommit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingRow("Show app names", "Hide labels under all popup icons") {
            GlassSwitch(checked = showAppNames, onCheckedChange = onShowAppNamesChange)
        }
        SliderRow(
            title = "Popup opacity",
            value = popupOpacity,
            range = 0.1f..1f,
            format = ::percent,
            onChange = onPopupOpacityChange,
            onCommit = onPopupOpacityCommit
        )
        SliderRow(
            title = "Popup corner radius",
            value = cornerRadius,
            range = 8f..48f,
            format = ::dpLabel,
            onChange = onCornerRadiusChange,
            onCommit = onCornerRadiusCommit
        )
        SliderRow(
            title = "Panel animation speed",
            value = panelAnimationSpeed,
            range = 0.1f..4f,
            enabled = !reduceMotion,
            format = ::percent,
            onChange = onPanelAnimationSpeedChange,
            onCommit = onPanelAnimationSpeedCommit
        )
        SliderRow(
            title = "Icon animation speed",
            value = iconAnimationSpeed,
            range = 0.1f..4f,
            enabled = !reduceMotion,
            format = ::percent,
            onChange = onIconAnimationSpeedChange,
            onCommit = onIconAnimationSpeedCommit
        )
        if (reduceMotion) {
            PrismText(
                text = "Reduce motion is on — turn it off under Motion & haptics to adjust speed",
                fontSize = 14.sp,
                color = Prism.subText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        SettingRow(
            "Popup on the right",
            "Smaller grid anchored to the right edge instead of the bottom sheet"
        ) {
            GlassSwitch(checked = rightPopup, onCheckedChange = onRightPopupChange)
        }
        if (rightPopup) {
            SettingRow("Bounce animation", "Bouncy entry scaling/sliding for right popup") {
                GlassSwitch(checked = rightBounce, onCheckedChange = onRightBounceChange)
            }
            SettingRow("Slide-in animation", "Slide in overlay from the right") {
                GlassSwitch(checked = rightSlideIn, onCheckedChange = onRightSlideInChange)
            }
            SliderRow(
                title = "Vertical Offset (Y-axis)",
                value = rightPopupYOffset,
                range = -300f..300f,
                format = ::dpLabel,
                onChange = onRightPopupYOffsetChange,
                onCommit = onRightPopupYOffsetCommit
            )
        } else {
            SettingRow("Bounce animation", "Bouncy entry scaling/sliding for bottom popup") {
                GlassSwitch(checked = bottomBounce, onCheckedChange = onBottomBounceChange)
            }
            SettingRow("Slide up animation", "Slide up overlay from the bottom") {
                GlassSwitch(checked = bottomSlideUp, onCheckedChange = onBottomSlideUpChange)
            }
        }
        SettingRow("Show icon border", "Add background containers around app icons") {
            GlassSwitch(checked = showIconBorder, onCheckedChange = onShowIconBorderChange)
        }
        SliderRow(
            title = "Icon spacing",
            value = iconSpacing,
            range = 0f..30f,
            format = ::dpLabel,
            onChange = onIconSpacingChange,
            onCommit = onIconSpacingCommit
        )
        SliderRow(
            title = "Menu scale",
            value = menuScale,
            range = 0.6f..1.4f,
            format = ::percent,
            onChange = onMenuScaleChange,
            onCommit = onMenuScaleCommit
        )
    }
}

@Composable
private fun AppearanceSettingsSection(
    backdrop: Backdrop,
    ui: UiSettings,
    onUiChange: ((UiSettings) -> UiSettings) -> Unit,
    gridView: Boolean,
    onGridViewChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrismText("Theme", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            GlassSegmented(
                options = listOf("System", "Light", "Dark"),
                selectedIndex = ui.theme.ordinal,
                onSelect = { index -> onUiChange { s -> s.copy(theme = ThemeMode.entries[index]) } },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrismText("Accent colour", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            AccentPicker(
                backdrop = backdrop,
                accent = Color(ui.accent),
                onAccent = { c -> onUiChange { it.copy(accent = c.toArgb()) } }
            )
        }
        SliderRow(
            title = "Surface brightness",
            value = ui.brightness,
            range = 0.5f..1.5f,
            format = ::percent,
            onChange = { v -> onUiChange { it.copy(brightness = v) } },
            stepCount = 10
        )
        SettingRow("Grid view", "Show bookmarks as a grid in the main menu") {
            GlassSwitch(checked = gridView, onCheckedChange = onGridViewChange)
        }
    }
}

@Composable
private fun MotionSettingsSection(
    backdrop: Backdrop,
    ui: UiSettings,
    onUiChange: ((UiSettings) -> UiSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PrismText(
            "Motion & haptics",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
        SettingRow("Reduce motion", "Turn on to make every transition instant") {
            GlassSwitch(
                checked = ui.reduceMotion,
                onCheckedChange = { v -> onUiChange { it.copy(reduceMotion = v) } }
            )
        }
        SliderRow(
            title = "Animation speed",
            value = ui.animationSpeed,
            range = 0.5f..2f,
            format = ::percent,
            onChange = { v -> onUiChange { it.copy(animationSpeed = v) } },
            stepCount = 6
        )
        SliderRow(
            title = "Motion intensity",
            value = ui.motionIntensity,
            range = 0f..1f,
            format = ::percent,
            onChange = { v -> onUiChange { it.copy(motionIntensity = v) } }
        )
        SettingRow("Haptics", "Vibrate on taps and interactions") {
            GlassSwitch(
                checked = ui.haptics,
                onCheckedChange = { v -> onUiChange { it.copy(haptics = v) } }
            )
        }
        SliderRow(
            title = "Haptic strength",
            value = ui.hapticStrength,
            range = 0.25f..1f,
            enabled = ui.haptics,
            format = ::percent,
            onChange = { v -> onUiChange { it.copy(hapticStrength = v) } }
        )
        GlassButton(
            backdrop = backdrop,
            text = "Reset to defaults",
            onClick = { onUiChange { UiSettings().copy(theme = it.theme, accent = it.accent) } },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun FallbackTriggerSection(
    backdrop: Backdrop,
    edgeTriggerRunning: Boolean,
    onToggleEdgeTrigger: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PrismText(
            "An always-on thin strip at the bottom of the screen — swipe up from it to open Flick.",
            fontSize = 14.sp,
            color = Prism.subText
        )
        GlassButton(
            backdrop = backdrop,
            text = if (edgeTriggerRunning) "Disable edge swipe trigger" else "Enable edge swipe trigger",
            onClick = onToggleEdgeTrigger,
            variant = ButtonVariant.Primary
        )
    }
}

@Composable
fun AppSettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var edgeTriggerRunning by remember { mutableStateOf(false) }
    var roleHeld by remember { mutableStateOf(AssistantRoleHelper.isRoleHeld(context)) }

    var popupSectionExpanded by remember { mutableStateOf(true) }
    var appearanceSectionExpanded by remember { mutableStateOf(true) }
    var fallbackSectionExpanded by remember { mutableStateOf(true) }

    PrismScreen(title = "Flick settings") { padding, contentBackdrop ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AssistantTriggerSection(
                backdrop = contentBackdrop,
                roleHeld = roleHeld,
                onRoleRequestResult = { roleHeld = it }
            )

            ExpandableSection(
                backdrop = contentBackdrop,
                title = "Popup",
                expanded = popupSectionExpanded,
                onToggle = { popupSectionExpanded = !popupSectionExpanded }
            ) {
                PopupSettingsSection(
                    showAppNames = uiState.showAppNames,
                    onShowAppNamesChange = viewModel::setShowAppNames,
                    popupOpacity = uiState.popupOpacity,
                    onPopupOpacityChange = viewModel::onPopupOpacityChange,
                    onPopupOpacityCommit = viewModel::commitPopupOpacity,
                    rightPopup = uiState.rightPopup,
                    onRightPopupChange = viewModel::setRightPopup,
                    rightBounce = uiState.rightBounce,
                    onRightBounceChange = viewModel::setRightBounce,
                    rightSlideIn = uiState.rightSlideIn,
                    onRightSlideInChange = viewModel::setRightSlideIn,
                    rightPopupYOffset = uiState.rightPopupYOffset,
                    onRightPopupYOffsetChange = viewModel::onRightPopupYOffsetChange,
                    onRightPopupYOffsetCommit = viewModel::commitRightPopupYOffset,
                    bottomBounce = uiState.bottomBounce,
                    onBottomBounceChange = viewModel::setBottomBounce,
                    bottomSlideUp = uiState.bottomSlideUp,
                    onBottomSlideUpChange = viewModel::setBottomSlideUp,
                    showIconBorder = uiState.showIconBorder,
                    onShowIconBorderChange = viewModel::setShowIconBorder,
                    iconSpacing = uiState.iconSpacing,
                    onIconSpacingChange = viewModel::onIconSpacingChange,
                    onIconSpacingCommit = viewModel::commitIconSpacing,
                    reduceMotion = uiState.ui.reduceMotion,
                    panelAnimationSpeed = uiState.panelAnimationSpeed,
                    onPanelAnimationSpeedChange = viewModel::onPanelAnimationSpeedChange,
                    onPanelAnimationSpeedCommit = viewModel::commitPanelAnimationSpeed,
                    iconAnimationSpeed = uiState.iconAnimationSpeed,
                    onIconAnimationSpeedChange = viewModel::onIconAnimationSpeedChange,
                    onIconAnimationSpeedCommit = viewModel::commitIconAnimationSpeed,
                    menuScale = uiState.menuScale,
                    onMenuScaleChange = viewModel::onMenuScaleChange,
                    onMenuScaleCommit = viewModel::commitMenuScale,
                    cornerRadius = uiState.cornerRadius,
                    onCornerRadiusChange = viewModel::onCornerRadiusChange,
                    onCornerRadiusCommit = viewModel::commitCornerRadius
                )
            }

            ExpandableSection(
                backdrop = contentBackdrop,
                title = "Appearance",
                expanded = appearanceSectionExpanded,
                onToggle = { appearanceSectionExpanded = !appearanceSectionExpanded }
            ) {
                AppearanceSettingsSection(
                    backdrop = contentBackdrop,
                    ui = uiState.ui,
                    onUiChange = viewModel::updateUi,
                    gridView = uiState.gridView,
                    onGridViewChange = viewModel::setGridView
                )
            }

            MotionSettingsSection(
                backdrop = contentBackdrop,
                ui = uiState.ui,
                onUiChange = viewModel::updateUi
            )

            ExpandableSection(
                backdrop = contentBackdrop,
                title = "Fallback trigger: edge swipe",
                expanded = fallbackSectionExpanded,
                onToggle = { fallbackSectionExpanded = !fallbackSectionExpanded }
            ) {
                FallbackTriggerSection(
                    backdrop = contentBackdrop,
                    edgeTriggerRunning = edgeTriggerRunning,
                    onToggleEdgeTrigger = {
                        if (!OverlayPermissionHelper.canDrawOverlays(context)) {
                            context.startActivity(OverlayPermissionHelper.requestOverlayPermissionIntent(context))
                        } else {
                            if (edgeTriggerRunning) {
                                context.stopService(Intent(context, EdgeGestureOverlayService::class.java))
                            } else {
                                ContextCompat.startForegroundService(
                                    context,
                                    Intent(context, EdgeGestureOverlayService::class.java)
                                )
                            }
                            edgeTriggerRunning = !edgeTriggerRunning
                        }
                    }
                )
            }
        }
    }
}
