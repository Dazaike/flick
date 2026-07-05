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
import androidx.hilt.navigation.compose.hiltViewModel
import com.flick.data.model.SettingsPanels

@Composable
fun SettingsPanelPickerScreen(
    categoryId: Long,
    onAdded: () -> Unit,
    viewModel: SettingsPanelPickerViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Choose a settings panel") }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            itemsIndexed(SettingsPanels.all, key = { _, option -> option.action }) { _, option ->
                ListItem(
                    headlineContent = { Text(option.label) },
                    modifier = Modifier.clickable {
                        viewModel.addBookmark(categoryId, option, onAdded)
                    }
                )
            }
        }
    }
}
