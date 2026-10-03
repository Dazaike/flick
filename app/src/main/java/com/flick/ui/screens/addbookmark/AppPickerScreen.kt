package com.flick.ui.screens.addbookmark

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import com.flick.ui.prism.PrismListItem
import com.kyant.shapes.RoundedRectangle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AppPickerScreen(
    categoryId: Long,
    onAdded: () -> Unit,
    onAppSelected: ((InstalledAppInfo) -> Unit)? = null,
    viewModel: AppPickerViewModel = hiltViewModel()
) {
    val apps by viewModel.apps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val query by viewModel.query.collectAsState()

    PickerScaffold(
        title = "Choose an app",
        isLoading = isLoading,
        searchQuery = query,
        onSearchQueryChange = viewModel::onQueryChange,
        searchPlaceholder = "Search apps"
    ) {
        itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
            val iconBitmap = app.icon?.let { icon -> remember(icon) { icon.asImageBitmap() } }
            PrismListItem(
                leading = iconBitmap?.let { bitmap ->
                    {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(RoundedRectangle(8.dp))
                        )
                    }
                },
                headline = app.label,
                supporting = app.packageName,
                modifier = Modifier.animateItem(),
                onClick = {
                    if (onAppSelected != null) {
                        onAppSelected(app)
                    } else {
                        viewModel.addBookmark(categoryId, app, sortOrder = index, onDone = onAdded)
                    }
                }
            )
        }
    }
}
