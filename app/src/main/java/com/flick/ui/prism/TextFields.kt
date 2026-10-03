package com.flick.ui.prism

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.kyant.backdrop.Backdrop

/** A [TextFieldState] kept two-way in sync with a hoisted [value]. */
@Composable
fun rememberSyncedTextFieldState(value: String, onValueChange: (String) -> Unit): TextFieldState {
    val state = remember { TextFieldState(value) }
    val currentValue = rememberUpdatedState(value)
    val currentChange = rememberUpdatedState(onValueChange)
    LaunchedEffect(value) {
        if (state.text.toString() != value) state.setTextAndPlaceCursorAtEnd(value)
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect {
            if (it != currentValue.value) currentChange.value(it)
        }
    }
    return state
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    supportingText: String? = null,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    onSubmit: (() -> Unit)? = null,
) {
    val state = rememberSyncedTextFieldState(value, onValueChange)
    GlassTextField(
        state = state,
        label = label,
        modifier = modifier,
        placeholder = placeholder,
        supportingText = supportingText,
        error = error,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        onSubmit = onSubmit,
    )
}

@Composable
fun GlassSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    backdrop: Backdrop,
    placeholder: String = "Search",
    modifier: Modifier = Modifier,
) {
    val state = rememberSyncedTextFieldState(value, onValueChange)
    GlassSearchBar(backdrop = backdrop, state = state, modifier = modifier, placeholder = placeholder)
}
