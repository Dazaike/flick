package com.flick.iconpack

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Process
import androidx.core.graphics.drawable.toBitmap
import com.flick.data.model.Bookmark
import com.flick.data.model.BookmarkAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IconResolver @Inject constructor(
    private val appFilterParser: AppFilterParser,
    private val iconPackPreferences: IconPackPreferences
) {
    private val iconPackLock = Any()
    private var cachedPackPackage: String? = null
    private var cachedMapping: Map<String, String> = emptyMap()
    private val bitmapCache = object : android.util.LruCache<String, Bitmap?>(64) {}

    suspend fun resolveBitmap(context: Context, bookmark: Bookmark): Bitmap? {
        val activePack = resolveActivePack(bookmark, iconPackPreferences.activePackPackage.first())
        return resolveBitmapCached(context, bookmark, activePack)
    }

    /**
     * Resolves bitmaps for a whole overlay's worth of bookmarks in one shot, reading the active
     * icon pack preference a single time instead of once per bookmark, and resolving each
     * bookmark's bitmap concurrently off the calling thread.
     */
    suspend fun resolveBitmaps(context: Context, bookmarks: List<Bookmark>): Map<Long, Bitmap?> {
        val defaultActivePack = iconPackPreferences.activePackPackage.first()
        return coroutineScope {
            bookmarks.map { bookmark ->
                async(Dispatchers.Default) {
                    val activePack = resolveActivePack(bookmark, defaultActivePack)
                    bookmark.id to resolveBitmapCached(context, bookmark, activePack)
                }
            }.awaitAll().toMap()
        }
    }

    private fun resolveActivePack(bookmark: Bookmark, defaultActivePack: String?): String? =
        if (bookmark.action is BookmarkAction.LaunchApp) {
            bookmark.iconPackPackage ?: defaultActivePack
        } else {
            null
        }

    private fun resolveBitmapCached(context: Context, bookmark: Bookmark, activePack: String?): Bitmap? {
        val cacheKey = "${bookmark.id}:${bookmark.customIconUri}:${bookmark.iconPackPackage}:$activePack"
        bitmapCache.get(cacheKey)?.let { return it }

        val resolved = resolveBitmapUncached(context, bookmark, activePack)
        // LruCache.put() throws NPE on a null value (e.g. folder bookmarks, which have no
        // icon of their own), so only cache successfully-resolved bitmaps.
        if (resolved != null) {
            bitmapCache.put(cacheKey, resolved)
        }
        return resolved
    }

    private fun resolveBitmapUncached(context: Context, bookmark: Bookmark, activePack: String?): Bitmap? {
        bookmark.customIconUri?.let { uriString ->
            loadFromUri(context, uriString)?.let { return it }
        }

        val action = bookmark.action
        when (action) {
            is BookmarkAction.LaunchApp -> {
                if (activePack != null) {
                    loadFromIconPack(context, activePack, action)?.let { return it }
                }
                return loadAppIcon(context, action.packageName)
            }
            is BookmarkAction.AppShortcut -> {
                loadShortcutIcon(context, action)?.let { return it }
                return loadAppIcon(context, action.packageName)
            }
            is BookmarkAction.LegacyShortcut -> return loadAppIcon(context, action.packageName)
            else -> return null
        }
    }

    private fun loadFromUri(context: Context, uriString: String): Bitmap? = runCatching {
        val uri = Uri.parse(uriString)
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // decodeStream() with inJustDecodeBounds=true always returns null by design (it only
        // fills in outWidth/outHeight), so we must check stream-openability separately rather
        // than via an elvis on the decode result, or this would always bail out early.
        val streamOpened = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, boundsOptions)
            true
        } ?: false
        if (!streamOpened) return@runCatching null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(boundsOptions, MAX_CUSTOM_ICON_DIMENSION, MAX_CUSTOM_ICON_DIMENSION)
        }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        }
    }.getOrNull()

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            var halfHeight = height / 2
            var halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun loadFromIconPack(context: Context, packPackage: String, action: BookmarkAction.LaunchApp): Bitmap? {
        val mapping = synchronized(iconPackLock) {
            if (cachedPackPackage != packPackage) {
                cachedMapping = appFilterParser.parse(context, packPackage)
                cachedPackPackage = packPackage
            }
            cachedMapping
        }
        val launchActivity = resolveLaunchActivityClass(context, action.packageName) ?: return null
        val componentKey = "ComponentInfo{${action.packageName}/$launchActivity}"
        val drawableName = mapping[componentKey] ?: return null
        return runCatching {
            val packRes = context.packageManager.getResourcesForApplication(packPackage)
            val resId = packRes.getIdentifier(drawableName, "drawable", packPackage)
            if (resId == 0) return null
            androidx.core.content.res.ResourcesCompat.getDrawable(packRes, resId, null)?.toBitmap(width = 96, height = 96)
        }.getOrNull()
    }

    private fun resolveLaunchActivityClass(context: Context, packageName: String): String? =
        context.packageManager.getLaunchIntentForPackage(packageName)?.component?.className

    private fun loadShortcutIcon(context: Context, action: BookmarkAction.AppShortcut): Bitmap? = runCatching {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val query = LauncherApps.ShortcutQuery()
            .setPackage(action.packageName)
            .setShortcutIds(listOf(action.shortcutId))
            .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST)
        val shortcut = launcherApps.getShortcuts(query, Process.myUserHandle()).orEmpty().firstOrNull()
            ?: return null
        launcherApps.getShortcutIconDrawable(shortcut, context.resources.displayMetrics.densityDpi)
            ?.toBitmap(width = 96, height = 96)
    }.getOrNull()

    private fun loadAppIcon(context: Context, packageName: String): Bitmap? = runCatching {
        context.packageManager.getApplicationIcon(packageName).toBitmap(width = 96, height = 96)
    }.getOrNull()

    companion object {
        private const val MAX_CUSTOM_ICON_DIMENSION = 192
    }
}
