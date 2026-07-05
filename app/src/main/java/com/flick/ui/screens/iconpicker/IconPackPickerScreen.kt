package com.flick.ui.screens.iconpicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@Composable
fun IconPackPickerScreen(
    onDone: () -> Unit,
    viewModel: IconPackPickerViewModel = hiltViewModel()
) {
    val packs by viewModel.packs.collectAsState()
    val activePack by viewModel.activePack.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scope = rememberCoroutineScope()
    fun selectAndClose(packageName: String?) {
        viewModel.selectPack(packageName)
        scope.launch { onDone() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Icon pack") }) }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                item {
                    ListItem(
                        headlineContent = { Text("Default (no icon pack)") },
                        leadingContent = {
                            RadioButton(selected = activePack == null, onClick = { selectAndClose(null) })
                        },
                        modifier = Modifier.clickable { selectAndClose(null) }
                    )
                }
                items(packs, key = { it.packageName }) { pack ->
                    ListItem(
                        headlineContent = { Text(pack.label) },
                        supportingContent = { Text(pack.packageName) },
                        leadingContent = {
                            RadioButton(
                                selected = activePack == pack.packageName,
                                onClick = { selectAndClose(pack.packageName) }
                            )
                        },
                        modifier = Modifier.clickable { selectAndClose(pack.packageName) }
                    )
                }
                if (packs.isEmpty()) {
                    item { ListItem(headlineContent = { Text("No icon packs found on this device") }) }
                }
            }
        }
    }
}
