package com.flick.overlay.ui

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flick.ui.theme.MotionConfig
import com.flick.ui.theme.flickSpring
import com.flick.ui.theme.flickTween
import kotlin.math.roundToInt

/** Where the bookmark popup panel is anchored on screen. */
enum class OverlayPanelPlacement {
    Bottom,
    Right
}

/** Popup-specific motion flags (combined with global [MotionConfig]). */
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
    motionConfig: MotionConfig,
    panelMotion: OverlayPanelMotion,
    scrimAlpha: Float,
    panelOpacity: Float,
    rightPanelWidth: Dp = 150.dp,
    rightPanelYOffset: Dp = 0.dp,
    onScrimClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (iconsReady: Boolean) -> Unit
) {
    val fadeSpec = remember(motionConfig) { motionConfig.flickTween<Float>(220) }
    val scrimSpec = remember(motionConfig) { motionConfig.flickTween<Float>(350) }
    val slideSpring = remember(motionConfig, panelMotion.bounce) {
        if (panelMotion.bounce) {
            motionConfig.flickSpring<Float>(dampingRatio = 0.25f, stiffness = 500f)
        } else {
            motionConfig.flickSpring<Float>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        }
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
    val rightSlidePx = with(density) { rightPanelWidth.toPx() }
    var bottomSlidePx by remember { mutableFloatStateOf(0f) }

    val panelShape = when (placement) {
        OverlayPanelPlacement.Right -> RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
        OverlayPanelPlacement.Bottom -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    }
    val panelColor = MaterialTheme.colorScheme
        .surfaceColorAtElevation(8.dp)
        .copy(alpha = panelOpacity)

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

    Box(modifier = modifier.fillMaxSize()) {
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
            val panelModifier = when (placement) {
                OverlayPanelPlacement.Right -> Modifier
                    .align(Alignment.CenterEnd)
                    .width(rightPanelWidth)
                    .wrapContentHeight()
                    .offset(y = rightPanelYOffset)
                OverlayPanelPlacement.Bottom -> Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .wrapContentHeight()
            }

            Box(
                modifier = panelModifier
                    .onSizeChanged {
                        if (placement == OverlayPanelPlacement.Bottom) {
                            bottomSlidePx = it.height.toFloat()
                        }
                    }
                    .graphicsLayer {
                        alpha = panelContentAlpha
                        clip = false
                        this.translationX = translationX
                        this.translationY = translationY
                    }
                    .clip(panelShape)
                    .background(panelColor)
                    .clickable(enabled = false) {}
            ) {
                content(iconsReady)
            }
        }
    }
}
