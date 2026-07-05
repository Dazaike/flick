package com.flick.overlay.widgethost

import android.appwidget.AppWidgetHost
import android.content.Context

class FlickAppWidgetHost(context: Context) : AppWidgetHost(context.applicationContext, HOST_ID) {
    companion object {
        const val HOST_ID = 1024
    }
}
