package com.flick.data.model

import kotlinx.serialization.Serializable

@Serializable
sealed class BookmarkAction {

    @Serializable
    data class LaunchApp(
        val packageName: String,
        val activityClassName: String? = null
    ) : BookmarkAction()

    @Serializable
    data class AppWidget(
        val providerPackageName: String,
        val providerClassName: String,
        val appWidgetId: Int,
        val widthDp: Int,
        val heightDp: Int
    ) : BookmarkAction()

    @Serializable
    data class CallContact(
        val lookupKey: String,
        val phoneNumber: String,
        val displayName: String
    ) : BookmarkAction()

    @Serializable
    data class MessageContact(
        val lookupKey: String?,
        val phoneNumber: String,
        val displayName: String,
        val prefilledBody: String? = null
    ) : BookmarkAction()

    @Serializable
    data class DialNumber(
        val phoneNumber: String
    ) : BookmarkAction()

    @Serializable
    data class DirectCall(
        val phoneNumber: String
    ) : BookmarkAction()

    @Serializable
    data class SendSms(
        val phoneNumber: String,
        val body: String
    ) : BookmarkAction()

    @Serializable
    data class SettingsPanel(
        val panelAction: String
    ) : BookmarkAction()

    @Serializable
    data class AppShortcut(
        val packageName: String,
        val shortcutId: String,
        val label: String
    ) : BookmarkAction()

    @Serializable
    data class LegacyShortcut(
        val packageName: String,
        val label: String,
        val intentUri: String
    ) : BookmarkAction()

    @Serializable
    data class WebUrl(
        val url: String
    ) : BookmarkAction()

    @Serializable
    data class CustomIntent(
        val action: String? = null,
        val dataUri: String? = null,
        val mimeType: String? = null,
        val componentPackage: String? = null,
        val componentClass: String? = null,
        val categories: List<String> = emptyList(),
        val extras: Map<String, String> = emptyMap(),
        val flags: Int = 0,
        val useStartActivity: Boolean = true
    ) : BookmarkAction()

    /**
     * Marks a bookmark as a folder container. Folder bookmarks are never passed to
     * `BookmarkActionExecutor`; tapping one expands its children in place instead.
     */
    @Serializable
    data object Folder : BookmarkAction()
}

/** Discriminator stored alongside the serialized [BookmarkAction] payload in Room. */
enum class BookmarkActionType {
    LAUNCH_APP,
    APP_WIDGET,
    CALL_CONTACT,
    MESSAGE_CONTACT,
    DIAL_NUMBER,
    DIRECT_CALL,
    SEND_SMS,
    SETTINGS_PANEL,
    APP_SHORTCUT,
    LEGACY_SHORTCUT,
    WEB_URL,
    CUSTOM_INTENT,
    FOLDER
}

fun BookmarkAction.actionType(): BookmarkActionType = when (this) {
    is BookmarkAction.LaunchApp -> BookmarkActionType.LAUNCH_APP
    is BookmarkAction.AppWidget -> BookmarkActionType.APP_WIDGET
    is BookmarkAction.CallContact -> BookmarkActionType.CALL_CONTACT
    is BookmarkAction.MessageContact -> BookmarkActionType.MESSAGE_CONTACT
    is BookmarkAction.DialNumber -> BookmarkActionType.DIAL_NUMBER
    is BookmarkAction.DirectCall -> BookmarkActionType.DIRECT_CALL
    is BookmarkAction.SendSms -> BookmarkActionType.SEND_SMS
    is BookmarkAction.SettingsPanel -> BookmarkActionType.SETTINGS_PANEL
    is BookmarkAction.AppShortcut -> BookmarkActionType.APP_SHORTCUT
    is BookmarkAction.LegacyShortcut -> BookmarkActionType.LEGACY_SHORTCUT
    is BookmarkAction.WebUrl -> BookmarkActionType.WEB_URL
    is BookmarkAction.CustomIntent -> BookmarkActionType.CUSTOM_INTENT
    is BookmarkAction.Folder -> BookmarkActionType.FOLDER
}
