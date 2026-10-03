package com.flick.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.flick.overlay.OverlayService

/** Launcher-shortcut trampoline: starts the overlay popup and finishes immediately. */
class LaunchOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.startForegroundService(this, Intent(this, OverlayService::class.java))
        finish()
    }
}
