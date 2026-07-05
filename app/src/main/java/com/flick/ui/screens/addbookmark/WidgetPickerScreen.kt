package com.flick.ui.screens.addbookmark

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flick.ui.theme.LocalMotion
import com.flick.ui.theme.flickTween

@Composable
fun WidgetPickerScreen(
    categoryId: Long,
    onAdded: () -> Unit,
    viewModel: WidgetPickerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val providerItems by viewModel.providerItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val query by viewModel.query.collectAsState()

    var pendingProvider by remember { mutableStateOf<AppWidgetProviderInfo?>(null) }
    var pendingId by remember { mutableStateOf<Int?>(null) }
    var pendingLabel by remember { mutableStateOf<String?>(null) }

    fun finishWithSave(provider: AppWidgetProviderInfo, label: String, appWidgetId: Int) {
        viewModel.addBookmark(categoryId, label, provider, appWidgetId, onAdded)
    }

    val configureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val provider = pendingProvider
        val id = pendingId
        val label = pendingLabel
        if (provider != null && id != null && label != null) {
            if (result.resultCode == Activity.RESULT_OK) {
                finishWithSave(provider, label, id)
            } else {
                viewModel.deleteId(id)
            }
        }
        pendingProvider = null
        pendingId = null
        pendingLabel = null
    }

    fun proceedAfterBind(item: WidgetProviderItem, appWidgetId: Int) {
        val provider = item.provider
        if (provider.configure != null) {
            pendingProvider = provider
            pendingId = appWidgetId
            pendingLabel = item.label
            val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = provider.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            configureLauncher.launch(configureIntent)
        } else {
            finishWithSave(provider, item.label, appWidgetId)
        }
    }

    val bindLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val provider = pendingProvider
        val id = pendingId
        val label = pendingLabel
        if (provider != null && id != null && label != null) {
            if (result.resultCode == Activity.RESULT_OK) {
                proceedAfterBind(WidgetProviderItem(provider, label, null), id)
            } else {
                viewModel.deleteId(id)
                pendingProvider = null
                pendingId = null
                pendingLabel = null
            }
        }
    }

    fun selectProvider(item: WidgetProviderItem) {
        val appWidgetId = viewModel.allocateId()
        val bound = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, item.provider.provider)
        if (bound) {
            proceedAfterBind(item, appWidgetId)
        } else {
            pendingProvider = item.provider
            pendingId = appWidgetId
            pendingLabel = item.label
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, item.provider.provider)
            }
            bindLauncher.launch(bindIntent)
        }
    }

    val motion = LocalMotion.current

    PickerScaffold(
        title = "Choose a widget",
        isLoading = isLoading,
        searchQuery = query,
        onSearchQueryChange = viewModel::onQueryChange,
        searchPlaceholder = "Search widgets",
        isContentEmpty = providerItems.isEmpty(),
        emptyContent = {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(motion.flickTween(220))
            ) {
                Text(
                    text = "No widgets found",
                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                )
            }
        }
    ) {
        itemsIndexed(providerItems, key = { _, item -> item.provider.provider.flattenToString() }) { _, item ->
            val iconBitmap = item.icon?.let { icon -> remember(icon) { icon.asImageBitmap() } }
            ListItem(
                leadingContent = {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Icon(Icons.Filled.Widgets, contentDescription = null, modifier = Modifier.size(40.dp))
                    }
                },
                headlineContent = { Text(item.label) },
                supportingContent = { Text(item.provider.provider.packageName) },
                modifier = Modifier.animateItem().clickable { selectProvider(item) }
            )
        }
    }
}
