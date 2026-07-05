package com.flick.trigger

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import androidx.core.content.ContextCompat
import com.flick.overlay.OverlayService

class FlickVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        ContextCompat.startForegroundService(context, Intent(context, OverlayService::class.java))
        hide()
    }
}
