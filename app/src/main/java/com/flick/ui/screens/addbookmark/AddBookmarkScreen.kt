package com.flick.ui.screens.addbookmark

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.flick.ui.prism.PrismIcon
import com.flick.ui.prism.PrismIcons
import com.flick.ui.prism.PrismListItem
import com.flick.ui.prism.PrismScreen

enum class BookmarkTypeOption(val label: String) {
    APP("App"),
    APP_SHORTCUT("App shortcut"),
    WIDGET("App widget"),
    URL("Web URL"),
    SETTINGS_PANEL("Settings panel"),
    CALL_CONTACT("Call a contact"),
    MESSAGE_CONTACT("Message a contact"),
    DIAL_NUMBER("Dial a number"),
    DIRECT_CALL("Call a number directly"),
    SEND_SMS("Text a number"),
    FOLDER("Folder")
}

private fun BookmarkTypeOption.icon(): Int = when (this) {
    BookmarkTypeOption.APP -> PrismIcons.App
    BookmarkTypeOption.APP_SHORTCUT -> PrismIcons.Widgets
    BookmarkTypeOption.WIDGET -> PrismIcons.Widgets
    BookmarkTypeOption.URL -> PrismIcons.Globe
    BookmarkTypeOption.SETTINGS_PANEL -> PrismIcons.Settings
    BookmarkTypeOption.CALL_CONTACT -> PrismIcons.Call
    BookmarkTypeOption.MESSAGE_CONTACT -> PrismIcons.Sms
    BookmarkTypeOption.DIAL_NUMBER -> PrismIcons.Dialpad
    BookmarkTypeOption.DIRECT_CALL -> PrismIcons.Call
    BookmarkTypeOption.SEND_SMS -> PrismIcons.Sms
    BookmarkTypeOption.FOLDER -> PrismIcons.Folder
}

@Composable
fun AddBookmarkScreen(
    onTypeSelected: (BookmarkTypeOption) -> Unit
) {
    PrismScreen(title = "Add bookmark") { padding, _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        ) {
            items(BookmarkTypeOption.entries, key = { it.name }) { type ->
                PrismListItem(
                    headline = type.label,
                    leading = { PrismIcon(type.icon(), contentDescription = null) },
                    onClick = { onTypeSelected(type) }
                )
            }
        }
    }
}
