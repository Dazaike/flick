package com.flick.ui.screens.addbookmark

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flick.ui.prism.GlassSearchField
import com.flick.ui.prism.PrismScreen
import com.flick.ui.prism.Spinner
import com.flick.ui.theme.DURATION_MEDIUM
import com.flick.ui.theme.DURATION_QUICK
import com.flick.ui.theme.LocalMotion
import com.flick.ui.theme.Prism
import com.kyant.backdrop.Backdrop

/**
 * Shared scaffold for the app/shortcut/widget picker screens: a top bar, an optional search
 * field, a one-time crossfade between a loading spinner and the resolved content, and a
 * [LazyColumn] hosting either an empty-state or the caller-provided [listContent].
 *
 * Wrapping the loading -> content swap in a single [AnimatedContent] (rather than animating each
 * row individually) keeps the number of concurrently running animation nodes small even for long
 * lists.
 */
@Composable
fun PickerScaffold(
    title: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    searchQuery: String? = null,
    onSearchQueryChange: ((String) -> Unit)? = null,
    searchPlaceholder: String = "Search",
    isContentEmpty: Boolean = false,
    emptyContent: @Composable (Backdrop) -> Unit = {},
    headerContent: (@Composable (Backdrop) -> Unit)? = null,
    topBarActions: @Composable RowScope.(Backdrop) -> Unit = {},
    listContent: LazyListScope.() -> Unit
) {
    val motion = LocalMotion.current

    PrismScreen(
        title = title,
        modifier = modifier,
        actions = { bd -> topBarActions(bd) }
    ) { padding, contentBackdrop ->
        AnimatedContent(
            targetState = isLoading,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                fadeIn(motion.fade(DURATION_MEDIUM)) togetherWith fadeOut(motion.fade(DURATION_QUICK))
            },
            label = "pickerScaffoldLoadingCrossfade"
        ) { loading ->
            if (loading) {
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Spinner(
                        modifier = Modifier.align(Alignment.Center),
                        size = 32.dp,
                        color = Prism.accent
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = padding
                ) {
                    if (searchQuery != null && onSearchQueryChange != null) {
                        item(key = "__picker_search_field__") {
                            GlassSearchField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                backdrop = contentBackdrop,
                                placeholder = searchPlaceholder,
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            )
                        }
                    }
                    if (isContentEmpty) {
                        item(key = "__picker_empty_content__") { emptyContent(contentBackdrop) }
                    } else {
                        headerContent?.let { header ->
                            item(key = "__picker_header_content__") { header(contentBackdrop) }
                        }
                        listContent()
                    }
                }
            }
        }
    }
}
