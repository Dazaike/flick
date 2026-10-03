package com.flick.overlay.ui

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import com.flick.ui.prism.glassDepth
import com.flick.ui.prism.liquidGlass
import com.flick.ui.theme.MotionSpec
import com.flick.ui.theme.Prism
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.shapes.RoundedRectangle

/** Where the bookmark popup panel is anchored on screen. */
enum class OverlayPanelPlacement {
    Bottom,
    Right
}

/** Popup-specific motion flags (combined with global [MotionSpec]). */
@Immutable
data class OverlayPanelMotion(
    val slide: Boolean = false,
    val bounce: Boolean = false
) {
    val hasMotion: Boolean get() = slide || bounce
}

/**
 * Full-screen scrim plus a bookmark panel. The panel is always composed at its final width and
 * height; motion is alpha plus off-screen translation only (never scale).
 */
@Composable
fun OverlayScrimWithPanel(
    shown: Boolean,
    placement: OverlayPanelPlacement,
    motionConfig: MotionSpec,
    panelMotion: OverlayPanelMotion,
    scrimAlpha: Float,
    panelOpacity: Float,
    sceneBackdrop: LayerBackdrop,
    panelSurface: LayerBackdrop,
    rightPanelWidth: Dp = 150.dp,
    rightPanelYOffset: Dp = 0.dp,
    menuScale: Float = 1f,
    cornerRadius: Dp = 32.dp,
    onScrimClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (iconsReady: Boolean, panelSurface: Backdrop) -> Unit
) {
    val fadeSpec = remember(motionConfig) { motionConfig.fade<Float>(220) }
    val scrimSpec = remember(motionConfig) { motionConfig.fade<Float>(350) }
    val slideSpring = remember(motionConfig, panelMotion.bounce) {
        if (panelMotion.bounce) bounceSpring(motionConfig) else motionConfig.settle<Float>()
    }

    val animatedScrim by animateFloatAsState(
        targetValue = if (shown) scrimAlpha else 0f,
        animationSpec = scrimSpec,
        label = "overlayScrim"
    )
    val panelAlpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = fadeSpec,
        label = "overlayPanelAlpha"
    )
    val slideProgress by animateFloatAsState(
        targetValue = if (shown) 0f else 1f,
        animationSpec = if (panelMotion.slide) slideSpring else fadeSpec,
        label = "overlayPanelSlide"
    )

    val density = LocalDensity.current
    val scaledDensity = remember(density, menuScale) {
        Density(density.density * menuScale.coerceIn(0.6f, 1.4f), density.fontScale)
    }
    val navBottomPx = WindowInsets.navigationBars.getBottom(density)
    val insetPx = with(scaledDensity) { PanelInset.toPx() }
    val rightSlidePx = with(scaledDensity) { rightPanelWidth.toPx() } + insetPx
    val rightYOffsetPx = with(density) { rightPanelYOffset.roundToPx() }
    var bottomCardPx by remember { mutableFloatStateOf(0f) }
    val bottomSlidePx = bottomCardPx + insetPx + navBottomPx

    val translationX = when {
        !panelMotion.slide || placement != OverlayPanelPlacement.Right -> 0f
        else -> rightSlidePx * slideProgress
    }
    val translationY = when {
        !panelMotion.slide || placement != OverlayPanelPlacement.Bottom -> 0f
        else -> bottomSlidePx * slideProgress
    }
    // Keep the panel fully opaque while it slides in so icon enter animations are not
    // multiplied away by a parent alpha fade (especially at slower animation speeds).
    val panelContentAlpha = if (panelMotion.slide && shown) 1f else panelAlpha
    val iconsReady = !panelMotion.slide || slideProgress <= 0.2f
    val colors = Prism.colors

    Box(modifier = modifier.fillMaxSize()) {
        // The glass samples this empty layer, not the scrim: the scrim is already drawn under the
        // panel, and sampling it again stacked a second 40% dim that made low opacity look opaque.
        Box(Modifier.fillMaxSize().layerBackdrop(sceneBackdrop))
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = animatedScrim))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onScrimClick
                )
        )

        if (shown || panelAlpha > 0f) {
            val placementModifier = when (placement) {
                OverlayPanelPlacement.Right -> Modifier
                    .align(Alignment.CenterEnd)
                    .offset { IntOffset(0, rightYOffsetPx) }
                    .padding(end = PanelInset)
                OverlayPanelPlacement.Bottom -> Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(PanelInset)
            }

            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                val panelModifier = when (placement) {
                    OverlayPanelPlacement.Right -> placementModifier
                        .width(rightPanelWidth)
                        .wrapContentHeight()
                    OverlayPanelPlacement.Bottom -> placementModifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                }

                Box(
                    modifier = panelModifier
                        .onSizeChanged {
                            if (placement == OverlayPanelPlacement.Bottom) {
                                bottomCardPx = it.height.toFloat()
                            }
                        }
                        .graphicsLayer {
                            alpha = panelContentAlpha
                            clip = false
                            this.translationX = translationX
                            this.translationY = translationY
                        }
                        .liquidGlass(
                            backdrop = sceneBackdrop,
                            shape = { RoundedRectangle(cornerRadius) },
                            depth = glassDepth(elevation = 20.dp),
                            blurRadius = 28.dp,
                            refractionHeight = 16.dp,
                            refractionAmount = 32.dp,
                            surface = colors.background.copy(alpha = panelOpacity),
                            exportedBackdrop = panelSurface,
                        )
                        .clickable(enabled = false) {}
                ) {
                    content(iconsReady, panelSurface)
                }
            }
        }
    }
}

private val PanelInset = 12.dp

private fun bounceSpring(motion: MotionSpec): FiniteAnimationSpec<Float> =
    if (motion.reduced) snap() else spring(dampingRatio = 0.25f, stiffness = 500f)
