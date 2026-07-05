package com.flick.ui.screens.addbookmark

import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flick.data.model.Bookmark
import com.flick.data.model.BookmarkAction
import com.flick.data.repository.BookmarkRepository
import com.flick.overlay.widgethost.FlickAppWidgetHost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Immutable
data class WidgetProviderItem(
    val provider: AppWidgetProviderInfo,
    val label: String,
    val icon: Bitmap?
)

@HiltViewModel
class WidgetPickerViewModel @Inject constructor(
    application: Application,
    val host: FlickAppWidgetHost,
    private val bookmarkRepository: BookmarkRepository
) : AndroidViewModel(application) {

    private val _providerItems = MutableStateFlow<List<WidgetProviderItem>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val providerItems: StateFlow<List<WidgetProviderItem>> = combine(_providerItems, _query) { items, query ->
        if (query.isBlank()) {
            items
        } else {
            items.filter { item ->
                item.label.contains(query, ignoreCase = true) ||
                    item.provider.provider.packageName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _isLoading.value = true
            _providerItems.value = loadProviders()
            _isLoading.value = false
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun allocateId(): Int = host.allocateAppWidgetId()

    fun deleteId(appWidgetId: Int) = host.deleteAppWidgetId(appWidgetId)

    private suspend fun loadProviders(): List<WidgetProviderItem> = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val pm = context.packageManager
        val densityDpi = context.resources.displayMetrics.densityDpi
        AppWidgetManager.getInstance(context).installedProviders
            .map { provider ->
                WidgetProviderItem(
                    provider = provider,
                    label = provider.loadLabel(pm),
                    icon = runCatching {
                        provider.loadIcon(context, densityDpi)?.toBitmap(width = 96, height = 96)
                    }.getOrNull()
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun addBookmark(
        categoryId: Long,
        label: String,
        provider: AppWidgetProviderInfo,
        appWidgetId: Int,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            bookmarkRepository.upsert(
                Bookmark(
                    categoryId = categoryId,
                    label = label,
                    sortOrder = 0,
                    action = BookmarkAction.AppWidget(
                        providerPackageName = provider.provider.packageName,
                        providerClassName = provider.provider.className,
                        appWidgetId = appWidgetId,
                        widthDp = 220,
                        heightDp = 140
                    )
                )
            )
            onDone()
        }
    }
}
