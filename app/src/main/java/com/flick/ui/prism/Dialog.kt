package com.flick.ui.prism

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flick.ui.theme.LocalMotion
import com.flick.ui.theme.Prism
import com.flick.ui.theme.PrismText
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle

/**
 * Glass card dialog rendered in the [OverlayHost] above the whole app. [content] fills the body
 * between the title and the buttons and receives the card's own backdrop for nested glass controls.
 */
@Composable
fun GlassDialog(
    visible: Boolean,
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = "Cancel",
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    confirmLoading: Boolean = false,
    content: @Composable ColumnScope.(cardBackdrop: Backdrop) -> Unit,
) {
    val vis = rememberOverlayVisibility(visible)
    if (!vis.isComposed) return
    Portal { backdrop ->
        val colors = Prism.colors
        val motion = LocalMotion.current
        val m = motion.magnitude
        BackHandler(enabled = visible && !confirmLoading, onBack = onDismiss)
        AnimatedVisibility(
            visibleState = vis,
            modifier = Modifier.fillMaxSize(),
            enter = EnterTransition.None,
            exit = ExitTransition.None,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .animateEnterExit(enter = fadeIn(motion.fade(200)), exit = fadeOut(motion.fade(200)))
                        .fillMaxSize()
                        .background(colors.scrim)
                        .clickable(interactionSource = null, indication = null) {
                            if (!confirmLoading) onDismiss()
                        },
                )
                val cardBackdrop = rememberLayerBackdrop()
                Column(
                    modifier
                        .animateEnterExit(
                            enter = fadeIn(motion.enter(240)) + scaleIn(motion.settle(), initialScale = 1f - 0.06f * m),
                            exit = fadeOut(motion.exit(150)) + scaleOut(motion.exit(150), targetScale = 1f - 0.03f * m),
                        )
                        .padding(24.dp)
                        .widthIn(max = 340.dp)
                        .fillMaxWidth()
                        .liquidGlass(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(28.dp) },
                            depth = glassDepth(elevation = 20.dp),
                            blurRadius = 28.dp,
                            refractionHeight = 16.dp,
                            refractionAmount = 32.dp,
                            surface = colors.sheet,
                            exportedBackdrop = cardBackdrop,
                        )
                        .clickable(interactionSource = null, indication = null) {}
                        .semantics { paneTitle = title }
                        .padding(24.dp),
                ) {
                    PrismText(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                    ) { content(cardBackdrop) }
                    Spacer(Modifier.height(24.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    ) {
                        GlassButton(cardBackdrop, dismissLabel, onDismiss, enabled = !confirmLoading)
                        GlassButton(
                            cardBackdrop,
                            confirmLabel,
                            onConfirm,
                            enabled = confirmEnabled,
                            variant = if (destructive) ButtonVariant.Destructive else ButtonVariant.Primary,
                            loading = confirmLoading,
                        )
                    }
                }
            }
        }
    }
}
