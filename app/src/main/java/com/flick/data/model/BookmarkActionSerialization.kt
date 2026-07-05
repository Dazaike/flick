package com.flick.data.model

import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun BookmarkAction.toPayloadJson(): String = when (this) {
    is BookmarkAction.LaunchApp -> json.encodeToString(BookmarkAction.LaunchApp.serializer(), this)
    is BookmarkAction.AppWidget -> json.encodeToString(BookmarkAction.AppWidget.serializer(), this)
    is BookmarkAction.CallContact -> json.encodeToString(BookmarkAction.CallContact.serializer(), this)
    is BookmarkAction.MessageContact -> json.encodeToString(BookmarkAction.MessageContact.serializer(), this)
    is BookmarkAction.DialNumber -> json.encodeToString(BookmarkAction.DialNumber.serializer(), this)
    is BookmarkAction.DirectCall -> json.encodeToString(BookmarkAction.DirectCall.serializer(), this)
    is BookmarkAction.SendSms -> json.encodeToString(BookmarkAction.SendSms.serializer(), this)
    is BookmarkAction.SettingsPanel -> json.encodeToString(BookmarkAction.SettingsPanel.serializer(), this)
    is BookmarkAction.AppShortcut -> json.encodeToString(BookmarkAction.AppShortcut.serializer(), this)
    is BookmarkAction.LegacyShortcut -> json.encodeToString(BookmarkAction.LegacyShortcut.serializer(), this)
    is BookmarkAction.WebUrl -> json.encodeToString(BookmarkAction.WebUrl.serializer(), this)
    is BookmarkAction.CustomIntent -> json.encodeToString(BookmarkAction.CustomIntent.serializer(), this)
    is BookmarkAction.Folder -> json.encodeToString(BookmarkAction.Folder.serializer(), this)
}

fun decodeBookmarkAction(actionType: BookmarkActionType, payloadJson: String): BookmarkAction =
    when (actionType) {
        BookmarkActionType.LAUNCH_APP -> json.decodeFromString<BookmarkAction.LaunchApp>(payloadJson)
        BookmarkActionType.APP_WIDGET -> json.decodeFromString<BookmarkAction.AppWidget>(payloadJson)
        BookmarkActionType.CALL_CONTACT -> json.decodeFromString<BookmarkAction.CallContact>(payloadJson)
        BookmarkActionType.MESSAGE_CONTACT -> json.decodeFromString<BookmarkAction.MessageContact>(payloadJson)
        BookmarkActionType.DIAL_NUMBER -> json.decodeFromString<BookmarkAction.DialNumber>(payloadJson)
        BookmarkActionType.DIRECT_CALL -> json.decodeFromString<BookmarkAction.DirectCall>(payloadJson)
        BookmarkActionType.SEND_SMS -> json.decodeFromString<BookmarkAction.SendSms>(payloadJson)
        BookmarkActionType.SETTINGS_PANEL -> json.decodeFromString<BookmarkAction.SettingsPanel>(payloadJson)
        BookmarkActionType.APP_SHORTCUT -> json.decodeFromString<BookmarkAction.AppShortcut>(payloadJson)
        BookmarkActionType.LEGACY_SHORTCUT -> json.decodeFromString<BookmarkAction.LegacyShortcut>(payloadJson)
        BookmarkActionType.WEB_URL -> json.decodeFromString<BookmarkAction.WebUrl>(payloadJson)
        BookmarkActionType.CUSTOM_INTENT -> json.decodeFromString<BookmarkAction.CustomIntent>(payloadJson)
        BookmarkActionType.FOLDER -> BookmarkAction.Folder
    }
