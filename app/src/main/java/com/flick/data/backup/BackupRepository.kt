package com.flick.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
import com.flick.data.db.BookmarkEntity
import com.flick.data.db.CategoryEntity
import com.flick.data.db.FlickDatabase
import com.flick.iconpack.IconPackPreferences
import com.flick.overlay.OverlayPreferences
import com.flick.ui.theme.ColorMode
import com.flick.ui.theme.ThemePreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: FlickDatabase,
    private val overlayPreferences: OverlayPreferences,
    private val themePreferences: ThemePreferences,
    private val iconPackPreferences: IconPackPreferences
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val bookmarkDao get() = database.bookmarkDao()
    private val categoryDao get() = database.categoryDao()
    private val iconsDir get() = File(context.filesDir, "shortcut_icons").apply { mkdirs() }

    suspend fun exportTo(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val categories = categoryDao.getAll()
        val bookmarks = bookmarkDao.getAll()
        val overlay = overlayPreferences.getAllPrefs()
        val theme = themePreferences.getAllPrefs()
        val iconPack = iconPackPreferences.getActivePack()

        val iconEntries = linkedMapOf<String, ByteArray>()
        val backupBookmarks = bookmarks.map { entity ->
            val iconFileName = copyIconBytes(entity.customIconUri, entity.id, iconEntries)
            BackupBookmark(
                id = entity.id,
                categoryId = entity.categoryId,
                label = entity.label,
                sortOrder = entity.sortOrder,
                actionType = entity.actionType,
                actionPayloadJson = entity.actionPayloadJson,
                customIconUri = entity.customIconUri,
                customIconFile = iconFileName,
                iconPackPackage = entity.iconPackPackage,
                showLabel = entity.showLabel,
                createdAt = entity.createdAt,
                parentFolderId = entity.parentFolderId
            )
        }

        val backup = FlickBackupFile(
            formatVersion = BACKUP_FORMAT_VERSION,
            exportedAtEpochMs = System.currentTimeMillis(),
            appVersionName = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull(),
            categories = categories.map {
                BackupCategory(
                    id = it.id,
                    name = it.name,
                    sortOrder = it.sortOrder,
                    colorOrIconRes = it.colorOrIconRes
                )
            },
            bookmarks = backupBookmarks,
            preferences = BackupPreferences(
                overlay = BackupOverlayPrefs(
                    showAppNames = overlay.showAppNames,
                    blurIntensity = overlay.blurIntensity,
                    popupOpacity = overlay.popupOpacity,
                    rightPopup = overlay.rightPopup,
                    iconSpacing = overlay.iconSpacing,
                    showIconBorder = overlay.showIconBorder,
                    slideAnimation = overlay.slideAnimation,
                    bottomBounce = overlay.bottomBounce,
                    bottomSlideUp = overlay.bottomSlideUp,
                    rightBounce = overlay.rightBounce,
                    rightSlideIn = overlay.rightSlideIn,
                    rightPopupYOffset = overlay.rightPopupYOffset,
                    panelAnimationSpeed = overlay.panelAnimationSpeed,
                    iconAnimationSpeed = overlay.iconAnimationSpeed,
                    panelScale = overlay.panelScale
                ),
                theme = BackupThemePrefs(
                    amoledMode = theme.amoledMode,
                    gridView = theme.gridView,
                    colorMode = theme.colorMode.name,
                    animationsEnabled = theme.animationsEnabled,
                    animationIntensity = theme.animationIntensity
                ),
                iconPack = BackupIconPackPrefs(activePackPackage = iconPack)
            )
        )

        val dataJson = json.encodeToString(FlickBackupFile.serializer(), backup)
        context.contentResolver.openOutputStream(uri)?.use { rawOut ->
            ZipOutputStream(BufferedOutputStream(rawOut)).use { zip ->
                zip.putNextEntry(ZipEntry("data.json"))
                zip.write(dataJson.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                iconEntries.forEach { (path, bytes) ->
                    zip.putNextEntry(ZipEntry(path))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
        } ?: error("Couldn't open export destination")

        BackupResult(
            categoryCount = categories.size,
            bookmarkCount = bookmarks.size,
            iconCount = iconEntries.size
        )
    }

    /**
     * Replaces all bookmarks/categories and overwrites preferences with the backup contents.
     * Remaps IDs so folder parent links stay valid.
     */
    suspend fun importFrom(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val (backup, iconBytes) = readBackupZip(uri)
        if (backup.formatVersion > BACKUP_FORMAT_VERSION) {
            error("This backup requires a newer Flick version (format ${backup.formatVersion})")
        }

        val categoryIdMap = mutableMapOf<Long, Long>()
        val bookmarkIdMap = mutableMapOf<Long, Long>()
        var restoredIcons = 0

        database.withTransaction {
            bookmarkDao.deleteAll()
            categoryDao.deleteAll()

            backup.categories
                .sortedBy { it.sortOrder }
                .forEach { category ->
                    val newId = categoryDao.insert(
                        CategoryEntity(
                            id = 0,
                            name = category.name,
                            sortOrder = category.sortOrder,
                            colorOrIconRes = category.colorOrIconRes
                        )
                    )
                    categoryIdMap[category.id] = newId
                }

            if (categoryIdMap.isEmpty()) {
                val defaultId = categoryDao.insert(
                    CategoryEntity(id = 0, name = "Default", sortOrder = 0)
                )
                // Remap any unknown category ids onto default
                backup.bookmarks.forEach { categoryIdMap.putIfAbsent(it.categoryId, defaultId) }
            }

            val sortedBookmarks = topologicalBookmarks(backup.bookmarks)
            sortedBookmarks.forEach { item ->
                val newCategoryId = categoryIdMap[item.categoryId]
                    ?: categoryIdMap.values.first()
                val newParentId = item.parentFolderId?.let { bookmarkIdMap[it] }
                val newIconUri = restoreIcon(item, iconBytes)?.also { restoredIcons++ }

                val newId = bookmarkDao.insert(
                    BookmarkEntity(
                        id = 0,
                        categoryId = newCategoryId,
                        label = item.label,
                        sortOrder = item.sortOrder,
                        actionType = item.actionType,
                        actionPayloadJson = item.actionPayloadJson,
                        customIconUri = newIconUri ?: item.customIconUri?.takeIf {
                            it.startsWith("content:")
                        },
                        iconPackPackage = item.iconPackPackage,
                        showLabel = item.showLabel,
                        createdAt = item.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis(),
                        parentFolderId = newParentId
                    )
                )
                bookmarkIdMap[item.id] = newId
            }
        }

        applyPreferences(backup.preferences)

        BackupResult(
            categoryCount = categoryIdMap.size,
            bookmarkCount = bookmarkIdMap.size,
            iconCount = restoredIcons
        )
    }

    private fun copyIconBytes(
        customIconUri: String?,
        bookmarkId: Long,
        out: MutableMap<String, ByteArray>
    ): String? {
        if (customIconUri.isNullOrBlank()) return null
        return runCatching {
            val bytes = readUriBytes(Uri.parse(customIconUri)) ?: return null
            val path = "icons/icon_$bookmarkId.png"
            out[path] = bytes
            path
        }.onFailure {
            Log.w(TAG, "Skipping icon for bookmark $bookmarkId", it)
        }.getOrNull()
    }

    private fun readUriBytes(uri: Uri): ByteArray? {
        return when (uri.scheme) {
            "file" -> {
                val path = uri.path ?: return null
                val file = File(path)
                if (!file.exists()) return null
                FileInputStream(file).use { it.readBytes() }
            }
            "content" -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            else -> null
        }
    }

    private fun readBackupZip(uri: Uri): Pair<FlickBackupFile, Map<String, ByteArray>> {
        var backup: FlickBackupFile? = null
        val icons = mutableMapOf<String, ByteArray>()
        context.contentResolver.openInputStream(uri)?.use { rawIn ->
            ZipInputStream(BufferedInputStream(rawIn)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.trimStart('/')
                        val bytes = zip.readBytes()
                        when {
                            name == "data.json" || name.endsWith("/data.json") -> {
                                backup = json.decodeFromString(FlickBackupFile.serializer(), bytes.toString(Charsets.UTF_8))
                            }
                            name.startsWith("icons/") -> icons[name] = bytes
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: error("Couldn't open backup file")

        return (backup ?: error("Backup is missing data.json")) to icons
    }

    private fun restoreIcon(
        item: BackupBookmark,
        iconBytes: Map<String, ByteArray>
    ): String? {
        val relative = item.customIconFile ?: return null
        val bytes = iconBytes[relative] ?: iconBytes[relative.trimStart('/')] ?: return null
        val file = File(iconsDir, "icon_${System.currentTimeMillis()}_${item.id}.png")
        file.writeBytes(bytes)
        return Uri.fromFile(file).toString()
    }

    /** Parents before children so [parentFolderId] remapping works in one pass. */
    private fun topologicalBookmarks(items: List<BackupBookmark>): List<BackupBookmark> {
        val byId = items.associateBy { it.id }
        val visited = linkedSetOf<Long>()
        val result = mutableListOf<BackupBookmark>()

        fun visit(id: Long) {
            if (id in visited) return
            val item = byId[id] ?: return
            item.parentFolderId?.let { visit(it) }
            visited += id
            result += item
        }

        items.forEach { visit(it.id) }
        return result
    }

    private suspend fun applyPreferences(prefs: BackupPreferences) {
        val overlay = prefs.overlay
        overlayPreferences.setShowAppNames(overlay.showAppNames)
        overlayPreferences.setBlurIntensity(overlay.blurIntensity)
        overlayPreferences.setPopupOpacity(overlay.popupOpacity)
        overlayPreferences.setRightPopup(overlay.rightPopup)
        overlayPreferences.setIconSpacing(overlay.iconSpacing)
        overlayPreferences.setShowIconBorder(overlay.showIconBorder)
        overlayPreferences.setSlideAnimation(overlay.slideAnimation)
        overlayPreferences.setBottomBounce(overlay.bottomBounce)
        overlayPreferences.setBottomSlideUp(overlay.bottomSlideUp)
        overlayPreferences.setRightBounce(overlay.rightBounce)
        overlayPreferences.setRightSlideIn(overlay.rightSlideIn)
        overlayPreferences.setRightPopupYOffset(overlay.rightPopupYOffset)
        overlayPreferences.setPanelAnimationSpeed(overlay.panelAnimationSpeed)
        overlayPreferences.setIconAnimationSpeed(overlay.iconAnimationSpeed)
        overlayPreferences.setPanelScale(overlay.panelScale)

        val theme = prefs.theme
        themePreferences.setAmoledMode(theme.amoledMode)
        themePreferences.setGridView(theme.gridView)
        themePreferences.setColorMode(
            runCatching { ColorMode.valueOf(theme.colorMode) }.getOrDefault(ColorMode.DYNAMIC)
        )
        themePreferences.setAnimationsEnabled(theme.animationsEnabled)
        themePreferences.setAnimationIntensity(theme.animationIntensity)

        iconPackPreferences.setActivePack(prefs.iconPack.activePackPackage)
    }

    companion object {
        private const val TAG = "BackupRepository"
    }
}
