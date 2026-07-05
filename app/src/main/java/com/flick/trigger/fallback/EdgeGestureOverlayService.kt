package com.flick.trigger.fallback

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.flick.overlay.NotificationChannels
import com.flick.overlay.OverlayService

/**
 * Always-on transparent strip pinned to the bottom edge. A short upward swipe from
 * this strip is the reliable fallback trigger when the Assistant-role gesture isn't held.
 */
class EdgeGestureOverlayService : Service() {

    private val windowManager by lazy { getSystemService(WindowManager::class.java) }
    private var stripView: View? = null
    private var lastTriggerElapsedMs = 0L

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        addStripView()
    }

    private fun addStripView() {
        if (stripView != null) return

        var downY = 0f
        val triggerDistancePx = (56 * resources.displayMetrics.density)

        val view = View(this).apply {
            setBackgroundColor(Color.argb(18, 255, 255, 255))
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (downY - event.rawY > triggerDistancePx) {
                            downY = event.rawY
                            triggerOverlay()
                        }
                        true
                    }
                    else -> true
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            (18 * resources.displayMetrics.density).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
        }

        windowManager.addView(view, params)
        stripView = view
    }

    private fun triggerOverlay() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastTriggerElapsedMs < TRIGGER_DEBOUNCE_MS) return
        if (!Settings.canDrawOverlays(this)) return
        lastTriggerElapsedMs = now

        ContextCompat.startForegroundService(this, Intent(this, OverlayService::class.java))
    }

    private fun buildNotification(): android.app.Notification {
        val channelId = "flick_edge_trigger"
        val manager = getSystemService(NotificationManager::class.java)
        NotificationChannels.ensureChannel(manager, channelId, "Flick Edge Trigger")
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentTitle("Flick edge gesture is active")
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stripView?.let { runCatching { windowManager.removeView(it) } }
        stripView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val TRIGGER_DEBOUNCE_MS = 800L
    }
}
