package com.flick.ui.screens.addbookmark

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun UrlEntryScreen(
    categoryId: Long,
    onAdded: () -> Unit,
    viewModel: UrlEntryViewModel = hiltViewModel()
) {
    var label by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add a URL bookmark") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label") })
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") })
            Button(
                onClick = { viewModel.addBookmark(categoryId, label.ifBlank { url }, url, onAdded) },
                enabled = url.isNotBlank()
            ) {
                Text("Save")
            }
        }
    }
}
