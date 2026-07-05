package com.flick.overlay

import android.app.NotificationChannel
import android.app.NotificationManager

/** Shared helper so overlay-related foreground services don't duplicate channel-creation code. */
internal object NotificationChannels {

    fun ensureChannel(manager: NotificationManager, channelId: String, channelName: String) {
        if (manager.getNotificationChannel(channelId) == null) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_MIN)
            )
        }
    }
}
