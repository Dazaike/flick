package com.flick.ui.screens.addbookmark

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flick.data.model.Bookmark
import com.flick.data.model.BookmarkAction
import com.flick.data.repository.BookmarkRepository
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
data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: Bitmap?
)

private const val ICON_BATCH_SIZE = 24

@HiltViewModel
class AppPickerViewModel @Inject constructor(
    application: Application,
    private val bookmarkRepository: BookmarkRepository
) : AndroidViewModel(application) {

    private val _apps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val apps: StateFlow<List<InstalledAppInfo>> = combine(_apps, _query) { apps, query ->
        if (query.isBlank()) {
            apps
        } else {
            apps.filter { it.label.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _isLoading.value = true
            val installed = loadInstalledApps()
            _apps.value = installed
            _isLoading.value = false
            resolveIconsInBatches(installed)
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    /** Loads package/label info only; icons are resolved lazily afterwards via [resolveIconsInBatches]. */
    private suspend fun loadInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = getApplication<Application>().packageManager
        val launchableIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(launchableIntent, 0)
            .map { resolveInfo ->
                InstalledAppInfo(
                    packageName = resolveInfo.activityInfo.packageName,
                    label = resolveInfo.loadLabel(pm).toString(),
                    icon = null
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * Resolves and decodes launcher icons in small batches on [Dispatchers.IO] so the list becomes
     * interactive immediately with labels, instead of blocking on bitmapping every installed app
     * up front.
     */
    private suspend fun resolveIconsInBatches(apps: List<InstalledAppInfo>) {
        val pm = getApplication<Application>().packageManager
        apps.chunked(ICON_BATCH_SIZE).forEach { batch ->
            val resolvedIcons = withContext(Dispatchers.IO) {
                batch.associate { app ->
                    app.packageName to runCatching {
                        pm.getApplicationIcon(app.packageName).toBitmap(width = 96, height = 96)
                    }.getOrNull()
                }
            }
            _apps.value = _apps.value.map { app ->
                resolvedIcons[app.packageName]?.let { icon -> app.copy(icon = icon) } ?: app
            }
        }
    }

    fun addBookmark(categoryId: Long, app: InstalledAppInfo, sortOrder: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            bookmarkRepository.upsert(
                Bookmark(
                    categoryId = categoryId,
                    label = app.label,
                    sortOrder = sortOrder,
                    action = BookmarkAction.LaunchApp(packageName = app.packageName)
                )
            )
            onDone()
        }
    }
}
