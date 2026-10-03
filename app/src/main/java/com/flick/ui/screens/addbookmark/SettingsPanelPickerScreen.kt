package com.flick.ui.screens.addbookmark

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.flick.data.model.SettingsPanels
import com.flick.ui.prism.PrismListItem
import com.flick.ui.prism.PrismScreen

@Composable
fun SettingsPanelPickerScreen(
    categoryId: Long,
    onAdded: () -> Unit,
    viewModel: SettingsPanelPickerViewModel = hiltViewModel()
) {
    PrismScreen(title = "Choose a settings panel") { padding, _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        ) {
            items(SettingsPanels.all, key = { option -> option.action }) { option ->
                PrismListItem(
                    headline = option.label,
                    onClick = { viewModel.addBookmark(categoryId, option, onAdded) }
                )
            }
        }
    }
}
