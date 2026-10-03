package com.flick.ui.screens.iconpicker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import com.flick.ui.prism.GlassRadio
import com.flick.ui.prism.PrismListItem
import com.flick.ui.prism.PrismScreen
import com.flick.ui.prism.Spinner
import com.flick.ui.theme.Prism

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

    PrismScreen(title = "Icon pack") { padding, _ ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Spinner(
                    modifier = Modifier.align(Alignment.Center),
                    size = 32.dp,
                    color = Prism.accent
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding
            ) {
                item {
                    PrismListItem(
                        headline = "Default (no icon pack)",
                        leading = {
                            GlassRadio(selected = activePack == null, onClick = { selectAndClose(null) })
                        },
                        onClick = { selectAndClose(null) }
                    )
                }
                items(packs, key = { it.packageName }) { pack ->
                    PrismListItem(
                        headline = pack.label,
                        supporting = pack.packageName,
                        leading = {
                            GlassRadio(
                                selected = activePack == pack.packageName,
                                onClick = { selectAndClose(pack.packageName) }
                            )
                        },
                        onClick = { selectAndClose(pack.packageName) }
                    )
                }
                if (packs.isEmpty()) {
                    item { PrismListItem(headline = "No icon packs found on this device") }
                }
            }
        }
    }
}
