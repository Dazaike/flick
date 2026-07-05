package com.flick.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BookmarkActionSerializationTest {

    private fun <T : BookmarkAction> roundTrip(action: T): BookmarkAction {
        val payload = action.toPayloadJson()
        return decodeBookmarkAction(action.actionType(), payload)
    }

    @Test
    fun launchApp_roundTrips() {
        val action = BookmarkAction.LaunchApp("com.example.app", "com.example.app.MainActivity")
        assertEquals(action, roundTrip(action))
    }

    @Test
    fun appWidget_roundTrips() {
        val action = BookmarkAction.AppWidget("com.example.widget", "com.example.widget.Provider", 42, 100, 80)
        assertEquals(action, roundTrip(action))
    }

    @Test
    fun callContact_roundTrips() {
        val action = BookmarkAction.CallContact("lookup123", "+15551234567", "Jane Doe")
        assertEquals(action, roundTrip(action))
    }

    @Test
    fun messageContact_roundTrips() {
        val action = BookmarkAction.MessageContact("lookup123", "+15551234567", "Jane Doe", "hi")
        assertEquals(action, roundTrip(action))
    }

    @Test
    fun dialNumber_roundTrips() {
        val action = BookmarkAction.DialNumber("+15551234567")
        assertEquals(action, roundTrip(action))
    }

    @Test
    fun directCall_roundTrips() {
        val action = BookmarkAction.DirectCall("+15551234567")
        assertEquals(action, roundTrip(action))
    }

    @Test
    fun sendSms_roundTrips() {
        val action = BookmarkAction.SendSms("+15551234567", "hello")
        assertEquals(action, roundTrip(action))
    }

    @Test
    fun settingsPanel_roundTrips() {
        val action = BookmarkAction.SettingsPanel("android.settings.panel.action.WIFI")
        assertEquals(action, roundTrip(action))
    }

    @Test
    fun appShortcut_roundTrips() {
        val action = BookmarkAction.AppShortcut("com.example.app", "shortcut1", "Compose")
        assertEquals(action, roundTrip(action))
    }

    @Test
    fun legacyShortcut_roundTrips() {
        val action = BookmarkAction.LegacyShortcut("com.example.app", "Macro", "intent:#Intent;action=com.example.RUN;end")
        assertEquals(action, roundTrip(action))
    }

    @Test
    fun webUrl_roundTrips() {
        val action = BookmarkAction.WebUrl("https://example.com")
        assertEquals(action, roundTrip(action))
    }

    @Test
    fun customIntent_roundTrips() {
        val action = BookmarkAction.CustomIntent(
            action = "android.intent.action.VIEW",
            dataUri = "content://example",
            mimeType = "text/plain",
            componentPackage = "com.example.app",
            componentClass = "com.example.app.SomeActivity",
            categories = listOf("android.intent.category.DEFAULT"),
            extras = mapOf("key" to "value"),
            flags = 0x10000000,
            useStartActivity = false
        )
        assertEquals(action, roundTrip(action))
    }
}
