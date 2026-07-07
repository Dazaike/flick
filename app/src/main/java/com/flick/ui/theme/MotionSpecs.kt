package com.flick.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.compositionLocalOf

// Central registry for animation durations/specs so every animated composable in the app
// reads from one place. This is the lever for dialing the "excess animation" pass back down
// later: flip MotionConfig.enabled or animationIntensity instead of hunting through files.

// [MotionConfig.intensity] is animation speed: 1.0 = baseline timing, higher = faster,
// lower = slower. Tweens shorten/lengthen duration; springs scale stiffness the same way.

const val DURATION_QUICK = 120
const val DURATION_MEDIUM = 220
const val DURATION_SLOW = 350

data class MotionConfig(
    val enabled: Boolean = true,
    val intensity: Float = 1f
)

val LocalMotion = compositionLocalOf { MotionConfig() }

private fun scaledDuration(durationMs: Int, speed: Float): Int =
    (durationMs / speed.coerceIn(0.1f, 1f)).toInt().coerceAtLeast(1)

fun <T> MotionConfig.flickTween(durationMs: Int, delayMs: Int = 0): FiniteAnimationSpec<T> =
    if (!enabled) {
        snap()
    } else {
        tween(
            durationMillis = scaledDuration(durationMs, intensity),
            delayMillis = scaledDuration(delayMs, intensity)
        )
    }

fun <T> MotionConfig.flickSpring(
    dampingRatio: Float = Spring.DampingRatioMediumBouncy,
    stiffness: Float = Spring.StiffnessMedium
): FiniteAnimationSpec<T> =
    if (!enabled) {
        snap()
    } else {
        spring(
            dampingRatio = dampingRatio,
            stiffness = (stiffness * intensity.coerceIn(0.1f, 1f)).coerceAtLeast(1f)
        )
    }

