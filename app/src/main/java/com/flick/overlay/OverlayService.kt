package com.flick.overlay

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import com.flick.action.BookmarkActionExecutor
import com.flick.data.model.Bookmark
import com.flick.data.model.BookmarkAction
import com.flick.data.repository.BookmarkRepository
import com.flick.iconpack.IconResolver
import com.flick.overlay.ui.OverlayBookmarkItem
import com.flick.overlay.ui.OverlayRoot
import com.flick.overlay.widgethost.FlickAppWidgetHost
import com.flick.permissions.OverlayPermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class OverlayService : Service() {

    @Inject lateinit var bookmarkRepository: BookmarkRepository
    @Inject lateinit var actionExecutor: BookmarkActionExecutor
    @Inject lateinit var iconResolver: IconResolver
    @Inject lateinit var overlayPreferences: OverlayPreferences
    @Inject lateinit var appWidgetHost: FlickAppWidgetHost

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var overlayController: OverlayWindowController

    override fun onCreate() {
        super.onCreate()
        overlayController = OverlayWindowController(this)
        runCatching { appWidgetHost.startListening() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must happen synchronously as the very first thing in onStartCommand, before any
        // suspending bookmark/icon work, to avoid ForegroundServiceDidNotStartInTimeException
        // on Android 12+.
        startForeground(NOTIFICATION_ID, buildNotification())

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission is required. Enable it in settings.", Toast.LENGTH_LONG).show()
            startActivity(OverlayPermissionHelper.requestOverlayPermissionIntent(this).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        serviceScope.launch {
            val prefs = withContext(Dispatchers.IO) { overlayPreferences.getAllPrefs() }

            // Preload bookmarks, resolved icons, and availability flags before showing the
            // overlay so the popup never renders in a blank-then-populated state.
            val bookmarks = withContext(Dispatchers.IO) { bookmarkRepository.observeAll().first() }
            val availability = withContext(Dispatchers.IO) { computeAvailability(this@OverlayService, bookmarks) }
            val itemsFlow = MutableStateFlow(buildItems(bookmarks))

            overlayController.show(blurIntensity = prefs.blurIntensity) { _ ->
                val currentItems by itemsFlow.collectAsState()
                OverlayRoot(
                    bookmarks = currentItems,
                    showLabels = prefs.showAppNames,
                    blurIntensity = prefs.blurIntensity,
                    popupOpacity = prefs.popupOpacity,
                    rightPopup = prefs.rightPopup,
                    iconSpacing = prefs.iconSpacing,
                    showIconBorder = prefs.showIconBorder,
                    slideAnimation = prefs.slideAnimation,
                    bottomBounce = prefs.bottomBounce,
                    bottomSlideUp = prefs.bottomSlideUp,
                    rightBounce = prefs.rightBounce,
                    rightSlideIn = prefs.rightSlideIn,
                    rightPopupYOffset = prefs.rightPopupYOffset,
                    panelAnimationSpeed = prefs.panelAnimationSpeed,
                    iconAnimationSpeed = prefs.iconAnimationSpeed,
                    availability = availability,
                    onBookmarkClick = { item ->
                        runCatching {
                            actionExecutor.execute(this@OverlayService, item.bookmark.action)
                        }.onFailure {
                            Toast.makeText(this@OverlayService, "Couldn't launch bookmark", Toast.LENGTH_SHORT).show()
                        }
                        stopOverlayService()
                    },
                    onDismiss = { stopOverlayService() },
                    onReorder = { reordered ->
                        serviceScope.launch {
                            withContext(Dispatchers.IO) {
                                reordered.forEachIndexed { index, bookmark ->
                                    bookmarkRepository.updateSortOrder(bookmark.id, index)
                                }
                            }
                            refreshOverlayItems(itemsFlow)
                        }
                    },
                    onMergeIntoFolder = { dragged, target ->
                        serviceScope.launch {
                            withContext(Dispatchers.IO) { bookmarkRepository.mergeIntoFolder(dragged.id, target.id) }
                            refreshOverlayItems(itemsFlow)
                        }
                    }
                )
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun buildItems(bookmarks: List<Bookmark>): List<OverlayBookmarkItem> {
        val icons = iconResolver.resolveBitmaps(this@OverlayService, bookmarks)
        val childrenByFolder = bookmarks
            .filter { it.parentFolderId != null }
            .groupBy { it.parentFolderId }
            .mapValues { (_, children) -> children.sortedBy { it.sortOrder } }
        return bookmarks.map { bookmark ->
            val childPreview = if (bookmark.action is BookmarkAction.Folder) {
                childrenByFolder[bookmark.id].orEmpty().take(4).map { icons[it.id] }
            } else {
                emptyList()
            }
            OverlayBookmarkItem(bookmark, icons[bookmark.id], childPreview = childPreview)
        }
    }

    private suspend fun refreshOverlayItems(itemsFlow: MutableStateFlow<List<OverlayBookmarkItem>>) {
        val refreshed = withContext(Dispatchers.IO) { bookmarkRepository.observeAll().first() }
        itemsFlow.value = buildItems(refreshed)
    }

    /** Availability must be resolved off the main thread; PackageManager calls can block on IPC. */
    private fun computeAvailability(context: Context, bookmarks: List<Bookmark>): Map<Long, Boolean> {
        val pm = context.packageManager
        return bookmarks.associate { bookmark ->
            val available = when (val action = bookmark.action) {
                is BookmarkAction.LaunchApp -> pm.getLaunchIntentForPackage(action.packageName) != null
                is BookmarkAction.AppShortcut -> isAppInstalled(pm, action.packageName)
                is BookmarkAction.LegacyShortcut -> isAppInstalled(pm, action.packageName)
                else -> true
            }
            bookmark.id to available
        }
    }

    @Suppress("DEPRECATION")
    private fun isAppInstalled(pm: PackageManager, packageName: String): Boolean =
        runCatching { pm.getApplicationInfo(packageName, 0) }.isSuccess

    private fun buildNotification(): android.app.Notification {
        val channelId = "flick_overlay"
        val manager = getSystemService(NotificationManager::class.java)
        NotificationChannels.ensureChannel(manager, channelId, "Flick Overlay")
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentTitle("Flick popup is active")
            .setOngoing(true)
            .build()
    }

    private fun stopOverlayService() {
        overlayController.hide()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        overlayController.destroy()
        runCatching { appWidgetHost.stopListening() }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
