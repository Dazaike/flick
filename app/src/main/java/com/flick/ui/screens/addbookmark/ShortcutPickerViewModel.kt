package com.flick.ui.screens.addbookmark

import android.app.Application
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.os.Process
import androidx.compose.runtime.Immutable
import androidx.core.content.IntentCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flick.data.model.Bookmark
import com.flick.data.model.BookmarkAction
import com.flick.data.repository.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Immutable
data class AppShortcutInfo(
    val packageName: String,
    val shortcutId: String,
    val label: String,
    val icon: Bitmap?
)

@HiltViewModel
class ShortcutPickerViewModel @Inject constructor(
    application: Application,
    private val bookmarkRepository: BookmarkRepository
) : AndroidViewModel(application) {

    private val _shortcuts = MutableStateFlow<List<AppShortcutInfo>>(emptyList())
    val shortcuts: StateFlow<List<AppShortcutInfo>> = _shortcuts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun load(packageName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _shortcuts.value = loadShortcuts(packageName)
            _isLoading.value = false
        }
    }

    private suspend fun loadShortcuts(packageName: String): List<AppShortcutInfo> = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val query = LauncherApps.ShortcutQuery()
            .setPackage(packageName)
            .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC)

        runCatching {
            launcherApps.getShortcuts(query, Process.myUserHandle()).orEmpty()
                .map { shortcut -> shortcut.toAppShortcutInfo(context, launcherApps, packageName) }
                .sortedBy { it.label.lowercase() }
        }.getOrElse { emptyList() }
    }

    private fun ShortcutInfo.toAppShortcutInfo(
        context: Application,
        launcherApps: LauncherApps,
        packageName: String
    ): AppShortcutInfo {
        val label = shortLabel?.toString()
            ?: longLabel?.toString()
            ?: id
        val icon = runCatching {
            launcherApps.getShortcutIconDrawable(this, context.resources.displayMetrics.densityDpi)
                ?.toBitmap(width = 96, height = 96)
        }.getOrNull()
        return AppShortcutInfo(
            packageName = packageName,
            shortcutId = id,
            label = label,
            icon = icon
        )
    }

    fun addBookmark(categoryId: Long, shortcut: AppShortcutInfo, sortOrder: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            // Persist the icon we already loaded for the picker instead of relying on a live
            // LauncherApps re-query later (dynamic shortcut icons/ids can become unreliable to
            // re-fetch, causing a silent fallback to the app's generic launcher icon).
            val iconUri = shortcut.icon?.let {
                withContext(Dispatchers.IO) { saveIconToDisk(it, shortcut.packageName) }
            }
            bookmarkRepository.upsert(
                Bookmark(
                    categoryId = categoryId,
                    label = shortcut.label,
                    sortOrder = sortOrder,
                    customIconUri = iconUri,
                    action = BookmarkAction.AppShortcut(
                        packageName = shortcut.packageName,
                        shortcutId = shortcut.shortcutId,
                        label = shortcut.label
                    )
                )
            )
            onDone()
        }
    }

    fun addLegacyShortcut(
        categoryId: Long,
        packageName: String,
        label: String,
        shortcutIntent: Intent,
        resultData: Intent? = null,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val iconUri = resultData?.let { withContext(Dispatchers.IO) { extractAndSaveIcon(it, packageName) } }
            bookmarkRepository.upsert(
                Bookmark(
                    categoryId = categoryId,
                    label = label,
                    sortOrder = 0,
                    customIconUri = iconUri,
                    action = BookmarkAction.LegacyShortcut(
                        packageName = packageName,
                        label = label,
                        intentUri = shortcutIntent.toUri(Intent.URI_INTENT_SCHEME)
                    )
                )
            )
            onDone()
        }
    }

    private fun extractAndSaveIcon(data: Intent, packageName: String): String? {
        val context = getApplication<Application>()
        val bitmap = run {
            val direct = IntentCompat.getParcelableExtra(data, Intent.EXTRA_SHORTCUT_ICON, Bitmap::class.java)
            if (direct != null) return@run direct
            val iconRes = IntentCompat.getParcelableExtra(
                data,
                Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource::class.java
            )
            if (iconRes != null) {
                runCatching {
                    val res = context.packageManager.getResourcesForApplication(iconRes.packageName)
                    val resId = res.getIdentifier(iconRes.resourceName, null, iconRes.packageName)
                    if (resId != 0) {
                        androidx.core.content.res.ResourcesCompat.getDrawable(res, resId, null)
                            ?.toBitmap(width = 96, height = 96)
                    } else null
                }.getOrNull()
            } else null
        } ?: return null

        return saveIconToDisk(bitmap, packageName)
    }

    private fun saveIconToDisk(bitmap: Bitmap, packageName: String): String? = runCatching {
        val context = getApplication<Application>()
        val dir = java.io.File(context.filesDir, "shortcut_icons").apply { mkdirs() }
        val file = java.io.File(dir, "icon_${System.currentTimeMillis()}_${packageName.hashCode()}.png")
        java.io.FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        android.net.Uri.fromFile(file).toString()
    }.getOrNull()
}
