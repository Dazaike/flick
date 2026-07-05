package com.flick.ui.screens.addbookmark

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ShortcutPickerScreen(
    categoryId: Long,
    packageName: String,
    onAdded: () -> Unit,
    viewModel: ShortcutPickerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val shortcuts by viewModel.shortcuts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val legacyShortcutLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@rememberLauncherForActivityResult
        val shortcutIntent = IntentCompat.getParcelableExtra(data, Intent.EXTRA_SHORTCUT_INTENT, Intent::class.java)
            ?: return@rememberLauncherForActivityResult
        val label = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME) ?: "Shortcut"
        viewModel.addLegacyShortcut(categoryId, packageName, label, shortcutIntent, data, onAdded)
    }

    LaunchedEffect(packageName) {
        viewModel.load(packageName)
    }

    ShortcutPickerContent(
        shortcuts = shortcuts,
        isLoading = isLoading,
        onCreateLegacyShortcut = {
            val intent = Intent(Intent.ACTION_CREATE_SHORTCUT).setPackage(packageName)
            runCatching { legacyShortcutLauncher.launch(intent) }
                .onFailure {
                    Toast.makeText(context, "This app does not expose a shortcut creator", Toast.LENGTH_SHORT).show()
                }
        },
        onShortcutSelected = { shortcut, index ->
            viewModel.addBookmark(categoryId, shortcut, index, onAdded)
        }
    )
}

@Composable
private fun ShortcutPickerContent(
    shortcuts: List<AppShortcutInfo>,
    isLoading: Boolean,
    onCreateLegacyShortcut: () -> Unit,
    onShortcutSelected: (AppShortcutInfo, Int) -> Unit
) {
    PickerScaffold(
        title = "Choose a shortcut",
        isLoading = isLoading,
        isContentEmpty = shortcuts.isEmpty(),
        emptyContent = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No modern shortcuts available")
                Button(
                    onClick = onCreateLegacyShortcut,
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Create app shortcut", modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        headerContent = {
            Button(
                onClick = onCreateLegacyShortcut,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Create app shortcut", modifier = Modifier.padding(start = 8.dp))
            }
        }
    ) {
        itemsIndexed(shortcuts, key = { _, shortcut -> "${shortcut.packageName}:${shortcut.shortcutId}" }) { index, shortcut ->
            val iconBitmap = shortcut.icon?.let { icon -> remember(icon) { icon.asImageBitmap() } }
            ListItem(
                leadingContent = {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Icon(
                            Icons.Filled.Android,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                },
                headlineContent = { Text(shortcut.label) },
                supportingContent = { Text(shortcut.shortcutId) },
                modifier = Modifier.animateItem().clickable { onShortcutSelected(shortcut, index) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShortcutPickerEmptyPreview() {
    ShortcutPickerContent(
        shortcuts = emptyList(),
        isLoading = false,
        onCreateLegacyShortcut = {},
        onShortcutSelected = { _, _ -> }
    )
}
