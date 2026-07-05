package com.flick.ui.screens.addbookmark

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

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

@Composable
fun AddBookmarkScreen(
    onTypeSelected: (BookmarkTypeOption) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Add bookmark") }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            itemsIndexed(BookmarkTypeOption.entries, key = { _, type -> type.name }) { _, type ->
                ListItem(
                    headlineContent = { Text(type.label) },
                    modifier = Modifier.clickable { onTypeSelected(type) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddBookmarkScreenPreview() {
    AddBookmarkScreen(onTypeSelected = {})
}
