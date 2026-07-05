package com.flick.ui.screens.addbookmark

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flick.ui.theme.DURATION_MEDIUM
import com.flick.ui.theme.LocalMotion
import com.flick.ui.theme.flickTween

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
    emptyContent: @Composable () -> Unit = {},
    headerContent: (@Composable () -> Unit)? = null,
    topBarActions: @Composable () -> Unit = {},
    listContent: LazyListScope.() -> Unit
) {
    val motion = LocalMotion.current

    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }, actions = { topBarActions() }) }
    ) { padding ->
        Box(modifier = modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = isLoading,
                transitionSpec = {
                    fadeIn(motion.flickTween(DURATION_MEDIUM)) togetherWith fadeOut(motion.flickTween(120))
                },
                label = "pickerScaffoldLoadingCrossfade"
            ) { loading ->
                if (loading) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (searchQuery != null && onSearchQueryChange != null) {
                            item(key = "__picker_search_field__") {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    placeholder = { Text(searchPlaceholder) },
                                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                                )
                            }
                        }
                        if (isContentEmpty) {
                            item(key = "__picker_empty_content__") { emptyContent() }
                        } else {
                            headerContent?.let { header ->
                                item(key = "__picker_header_content__") { header() }
                            }
                            listContent()
                        }
                    }
                }
            }
        }
    }
}
