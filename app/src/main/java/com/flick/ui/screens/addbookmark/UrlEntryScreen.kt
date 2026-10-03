package com.flick.ui.screens.addbookmark

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flick.ui.prism.ButtonVariant
import com.flick.ui.prism.GlassButton
import com.flick.ui.prism.GlassTextField
import com.flick.ui.prism.PrismScreen

@Composable
fun UrlEntryScreen(
    categoryId: Long,
    onAdded: () -> Unit,
    viewModel: UrlEntryViewModel = hiltViewModel()
) {
    var label by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    PrismScreen(title = "Add a URL bookmark") { padding, contentBackdrop ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassTextField(
                value = label,
                onValueChange = { label = it },
                label = "Label",
                modifier = Modifier.fillMaxWidth()
            )
            GlassTextField(
                value = url,
                onValueChange = { url = it },
                label = "URL",
                modifier = Modifier.fillMaxWidth()
            )
            GlassButton(
                backdrop = contentBackdrop,
                text = "Save",
                onClick = { viewModel.addBookmark(categoryId, label.ifBlank { url }, url, onAdded) },
                variant = ButtonVariant.Primary,
                enabled = url.isNotBlank()
            )
        }
    }
}
