package com.flick.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.util.concurrent.atomic.AtomicBoolean

class OverlayWindowController(private val context: Context) {

    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val lifecycleOwner = OverlayLifecycleOwner()

    private var composeView: ComposeView? = null

    // Guards against concurrent show()/hide() invocations racing to add/remove the same
    // window (which would otherwise be able to produce duplicate overlay windows).
    private val isShowing = AtomicBoolean(false)

    @SuppressLint("InflateParams")
    fun show(blurIntensity: Float = 0f, content: @Composable (dismiss: () -> Unit) -> Unit) {
        if (!isShowing.compareAndSet(false, true)) return

        lifecycleOwner.performRestore()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)

        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeOnBackPressedDispatcherOwner(lifecycleOwner)
            setContent { content { hide() } }
        }
        composeView = view

        var flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        val radius = (blurIntensity.coerceIn(0f, 1f) * 100).toInt()
        if (radius > 0) {
            flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            setFitInsetsTypes(0)
            if (radius > 0) {
                blurBehindRadius = radius
            }
        }

        windowManager.addView(view, params)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun hide() {
        if (!isShowing.compareAndSet(true, false)) return
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        composeView?.let { windowManager.removeView(it) }
        composeView = null
    }

    fun destroy() {
        hide()
        lifecycleOwner.onDestroy()
    }
}
