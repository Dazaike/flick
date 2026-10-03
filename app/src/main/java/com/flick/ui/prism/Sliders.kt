package com.flick.ui.prism

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** [GlassSlider] over an arbitrary [valueRange] (the underlying slider is 0..1 only). */
@Composable
fun GlassValueSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    stepCount: Int = 0,
    valueLabel: (Float) -> String,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val min = valueRange.start
    val span = valueRange.endInclusive - min
    GlassSlider(
        value = if (span == 0f) 0f else ((value - min) / span).coerceIn(0f, 1f),
        onValueChange = { onValueChange(min + it * span) },
        modifier = modifier,
        enabled = enabled,
        stepCount = stepCount,
        valueLabel = { valueLabel(min + it * span) },
        alwaysShowValue = false,
        onValueChangeFinished = onValueChangeFinished,
    )
}
