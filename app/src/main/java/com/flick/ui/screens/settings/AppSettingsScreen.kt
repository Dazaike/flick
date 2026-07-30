package com.flick.ui.screens.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.flick.permissions.OverlayPermissionHelper
import com.flick.trigger.AssistantRoleHelper
import com.flick.trigger.fallback.EdgeGestureOverlayService
import com.flick.ui.theme.ColorMode
import com.flick.ui.theme.DURATION_MEDIUM
import com.flick.ui.theme.DURATION_QUICK
import com.flick.ui.theme.LocalMotion
import com.flick.ui.theme.flickSpring
import com.flick.ui.theme.flickTween

@Composable
private fun SectionHeader(title: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title)
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand"
            )
        }
    }
}

@Composable
private fun BouncySwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val motion = LocalMotion.current
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.08f else 1f,
        animationSpec = motion.flickSpring(),
        label = "switchBounce"
    )
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
    )
}

@Composable
private fun AnimatedPercentLabel(text: String) {
    val motion = LocalMotion.current
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (fadeIn(motion.flickTween(120)) togetherWith fadeOut(motion.flickTween(80)))
        },
        label = "percentLabel"
    ) { value ->
        Text(value)
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val motion = LocalMotion.current
    SectionHeader(title = title, expanded = expanded, onToggle = onToggle)
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(motion.flickTween(DURATION_MEDIUM)) + expandVertically(motion.flickSpring()),
        exit = fadeOut(motion.flickTween(DURATION_QUICK)) + shrinkVertically(motion.flickSpring())
    ) {
        content()
    }
}

@Composable
private fun AssistantTriggerSection(
    roleHeld: Boolean,
    onRoleRequestResult: () -> Unit
) {
    val context = LocalContext.current
    val roleRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { onRoleRequestResult() }

    Text("Primary trigger: Assistant gesture")
    Text(if (roleHeld) "Flick currently holds the Assistant role." else "Assistant role not held.")
    Text("Note: holding this role replaces Google Assistant/Gemini system-wide on this device.")
    Button(
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
        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
    ) {
        Text(if (roleHeld) "Assistant settings" else "Choose default Assistant")
    }
}

@Composable
private fun PopupSettingsSection(
    showAppNames: Boolean,
    onShowAppNamesChange: (Boolean) -> Unit,
    blurIntensity: Float,
    onBlurIntensityChange: (Float) -> Unit,
    onBlurIntensityCommit: () -> Unit,
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
    panelScale: Float,
    onPanelScaleChange: (Float) -> Unit,
    onPanelScaleCommit: () -> Unit,
    animationsEnabled: Boolean,
    panelAnimationSpeed: Float,
    onPanelAnimationSpeedChange: (Float) -> Unit,
    onPanelAnimationSpeedCommit: () -> Unit,
    iconAnimationSpeed: Float,
    onIconAnimationSpeedChange: (Float) -> Unit,
    onIconAnimationSpeedCommit: () -> Unit
) {
    Column {
        ListItem(
            headlineContent = { Text("Show app names") },
            supportingContent = { Text("Hide labels under all popup icons") },
            trailingContent = { BouncySwitch(checked = showAppNames, onCheckedChange = onShowAppNamesChange) }
        )
        AnimatedPercentLabel("Background blur: ${(blurIntensity * 100).toInt()}%")
        Slider(
            value = blurIntensity,
            onValueChange = onBlurIntensityChange,
            onValueChangeFinished = onBlurIntensityCommit,
            valueRange = 0f..1f
        )
        AnimatedPercentLabel("Popup opacity: ${(popupOpacity * 100).toInt()}%")
        Slider(
            value = popupOpacity,
            onValueChange = onPopupOpacityChange,
            onValueChangeFinished = onPopupOpacityCommit,
            valueRange = 0.3f..1f
        )
        AnimatedPercentLabel("Panel animation speed: ${(panelAnimationSpeed * 100).toInt()}%")
        Slider(
            value = panelAnimationSpeed,
            onValueChange = onPanelAnimationSpeedChange,
            onValueChangeFinished = onPanelAnimationSpeedCommit,
            enabled = animationsEnabled,
            valueRange = 0.1f..1f
        )
        AnimatedPercentLabel("Icon animation speed: ${(iconAnimationSpeed * 100).toInt()}%")
        Slider(
            value = iconAnimationSpeed,
            onValueChange = onIconAnimationSpeedChange,
            onValueChangeFinished = onIconAnimationSpeedCommit,
            enabled = animationsEnabled,
            valueRange = 0.1f..1f
        )
        if (!animationsEnabled) {
            Text(
                text = "Turn on animations below to adjust speed",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ListItem(
            headlineContent = { Text("Popup on the right") },
            supportingContent = { Text("Smaller grid anchored to the right edge instead of the bottom sheet") },
            trailingContent = { BouncySwitch(checked = rightPopup, onCheckedChange = onRightPopupChange) }
        )
        if (rightPopup) {
            ListItem(
                headlineContent = { Text("Bounce animation") },
                supportingContent = { Text("Bouncy entry scaling/sliding for right popup") },
                trailingContent = { BouncySwitch(checked = rightBounce, onCheckedChange = onRightBounceChange) }
            )
            ListItem(
                headlineContent = { Text("Slide-in animation") },
                supportingContent = { Text("Slide in overlay from the right") },
                trailingContent = { BouncySwitch(checked = rightSlideIn, onCheckedChange = onRightSlideInChange) }
            )
            AnimatedPercentLabel("Vertical Offset (Y-axis): ${rightPopupYOffset.toInt()} dp")
            Slider(
                value = rightPopupYOffset,
                onValueChange = onRightPopupYOffsetChange,
                onValueChangeFinished = onRightPopupYOffsetCommit,
                valueRange = -300f..300f
            )
        } else {
            ListItem(
                headlineContent = { Text("Bounce animation") },
                supportingContent = { Text("Bouncy entry scaling/sliding for bottom popup") },
                trailingContent = { BouncySwitch(checked = bottomBounce, onCheckedChange = onBottomBounceChange) }
            )
            ListItem(
                headlineContent = { Text("Slide up animation") },
                supportingContent = { Text("Slide up overlay from the bottom") },
                trailingContent = { BouncySwitch(checked = bottomSlideUp, onCheckedChange = onBottomSlideUpChange) }
            )
        }
        ListItem(
            headlineContent = { Text("Show icon border") },
            supportingContent = { Text("Add background containers around app icons") },
            trailingContent = { BouncySwitch(checked = showIconBorder, onCheckedChange = onShowIconBorderChange) }
        )
        AnimatedPercentLabel("Icon spacing: ${iconSpacing.toInt()} dp")
        Slider(
            value = iconSpacing,
            onValueChange = onIconSpacingChange,
            onValueChangeFinished = onIconSpacingCommit,
            valueRange = 0f..30f
        )
        AnimatedPercentLabel("Panel scale: ${(panelScale * 100).toInt()}%")
        Slider(
            value = panelScale,
            onValueChange = onPanelScaleChange,
            onValueChangeFinished = onPanelScaleCommit,
            valueRange = 0.7f..1.5f
        )
    }
}

@Composable
private fun AppearanceSettingsSection(
    colorMode: ColorMode,
    onColorModeChange: (ColorMode) -> Unit,
    amoledMode: Boolean,
    onAmoledModeChange: (Boolean) -> Unit,
    gridView: Boolean,
    onGridViewChange: (Boolean) -> Unit
) {
    Column {
        Text("Color style")
        Row(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = colorMode == ColorMode.DYNAMIC,
                onClick = { onColorModeChange(ColorMode.DYNAMIC) },
                label = { Text("Wallpaper colors") }
            )
            FilterChip(
                selected = colorMode == ColorMode.BRAND,
                onClick = { onColorModeChange(ColorMode.BRAND) },
                label = { Text("Flick colors") }
            )
        }
        ListItem(
            headlineContent = { Text("AMOLED mode") },
            supportingContent = { Text("True black backgrounds in dark theme") },
            trailingContent = { BouncySwitch(checked = amoledMode, onCheckedChange = onAmoledModeChange) }
        )
        ListItem(
            headlineContent = { Text("Grid view") },
            supportingContent = { Text("Show bookmarks as a grid in the main menu") },
            trailingContent = { BouncySwitch(checked = gridView, onCheckedChange = onGridViewChange) }
        )
    }
}

@Composable
private fun AnimationSettingsSection(
    animationsEnabled: Boolean,
    onAnimationsEnabledChange: (Boolean) -> Unit,
    animationIntensity: Float,
    onAnimationIntensityChange: (Float) -> Unit,
    onAnimationIntensityCommit: () -> Unit
) {
    val motion = LocalMotion.current
    Text("Animations", modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
    ListItem(
        headlineContent = { Text("Enable animations") },
        supportingContent = { Text("Turn off to make every transition instant") },
        trailingContent = { BouncySwitch(checked = animationsEnabled, onCheckedChange = onAnimationsEnabledChange) }
    )
    AnimatedVisibility(
        visible = animationsEnabled,
        enter = fadeIn(motion.flickTween(DURATION_MEDIUM)) + expandVertically(motion.flickSpring()),
        exit = fadeOut(motion.flickTween(DURATION_QUICK)) + shrinkVertically(motion.flickSpring())
    ) {
        Column {
            AnimatedPercentLabel("Animation speed: ${(animationIntensity * 100).toInt()}%")
            Slider(
                value = animationIntensity,
                onValueChange = onAnimationIntensityChange,
                onValueChangeFinished = onAnimationIntensityCommit,
                valueRange = 0.1f..1f
            )
        }
    }
}

@Composable
private fun FallbackTriggerSection(
    edgeTriggerRunning: Boolean,
    onToggleEdgeTrigger: () -> Unit
) {
    Column {
        Text("An always-on thin strip at the bottom of the screen — swipe up from it to open Flick.")
        Button(
            onClick = onToggleEdgeTrigger,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(if (edgeTriggerRunning) "Disable edge swipe trigger" else "Enable edge swipe trigger")
        }
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Flick settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            AssistantTriggerSection(
                roleHeld = roleHeld,
                onRoleRequestResult = { roleHeld = AssistantRoleHelper.isRoleHeld(context) }
            )

            ExpandableSection(
                title = "Popup",
                expanded = popupSectionExpanded,
                onToggle = { popupSectionExpanded = !popupSectionExpanded }
            ) {
                PopupSettingsSection(
                    showAppNames = uiState.showAppNames,
                    onShowAppNamesChange = viewModel::setShowAppNames,
                    blurIntensity = uiState.blurIntensity,
                    onBlurIntensityChange = viewModel::onBlurIntensityChange,
                    onBlurIntensityCommit = viewModel::commitBlurIntensity,
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
                    panelScale = uiState.panelScale,
                    onPanelScaleChange = viewModel::onPanelScaleChange,
                    onPanelScaleCommit = viewModel::commitPanelScale,
                    animationsEnabled = uiState.animationsEnabled,
                    panelAnimationSpeed = uiState.panelAnimationSpeed,
                    onPanelAnimationSpeedChange = viewModel::onPanelAnimationSpeedChange,
                    onPanelAnimationSpeedCommit = viewModel::commitPanelAnimationSpeed,
                    iconAnimationSpeed = uiState.iconAnimationSpeed,
                    onIconAnimationSpeedChange = viewModel::onIconAnimationSpeedChange,
                    onIconAnimationSpeedCommit = viewModel::commitIconAnimationSpeed
                )
            }

            ExpandableSection(
                title = "Appearance",
                expanded = appearanceSectionExpanded,
                onToggle = { appearanceSectionExpanded = !appearanceSectionExpanded }
            ) {
                AppearanceSettingsSection(
                    colorMode = uiState.colorMode,
                    onColorModeChange = viewModel::setColorMode,
                    amoledMode = uiState.amoledMode,
                    onAmoledModeChange = viewModel::setAmoledMode,
                    gridView = uiState.gridView,
                    onGridViewChange = viewModel::setGridView
                )
            }

            AnimationSettingsSection(
                animationsEnabled = uiState.animationsEnabled,
                onAnimationsEnabledChange = viewModel::setAnimationsEnabled,
                animationIntensity = uiState.animationIntensity,
                onAnimationIntensityChange = viewModel::onAnimationIntensityChange,
                onAnimationIntensityCommit = viewModel::commitAnimationIntensity
            )

            ExpandableSection(
                title = "Fallback trigger: edge swipe",
                expanded = fallbackSectionExpanded,
                onToggle = { fallbackSectionExpanded = !fallbackSectionExpanded }
            ) {
                FallbackTriggerSection(
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
