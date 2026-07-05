package com.flick.ui.screens.addbookmark

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun FolderCreateScreen(
    categoryId: Long,
    onAdded: () -> Unit,
    viewModel: FolderCreateViewModel = hiltViewModel()
) {
    val options by viewModel.options.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val folderName by viewModel.folderName.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()

    LaunchedEffect(categoryId) {
        viewModel.load(categoryId)
    }

    FolderCreateContent(
        options = options,
        isLoading = isLoading,
        folderName = folderName,
        selectedIds = selectedIds,
        onNameChange = viewModel::onNameChange,
        onToggleSelected = viewModel::toggleSelected,
        onConfirm = {
            viewModel.createFolder(categoryId, sortOrder = 0, onDone = onAdded)
        }
    )
}

@Composable
private fun FolderCreateContent(
    options: List<FolderMemberOption>,
    isLoading: Boolean,
    folderName: String,
    selectedIds: Set<Long>,
    onNameChange: (String) -> Unit,
    onToggleSelected: (Long) -> Unit,
    onConfirm: () -> Unit
) {
    val canCreate = selectedIds.size >= 2

    PickerScaffold(
        title = "Create folder",
        isLoading = isLoading,
        isContentEmpty = options.size < 2,
        topBarActions = {
            IconButton(onClick = onConfirm, enabled = canCreate) {
                Icon(Icons.Filled.Check, contentDescription = "Create folder")
            }
        },
        emptyContent = {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text("Add a couple of bookmarks first, then group them into a folder.")
            }
        },
        headerContent = {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = onNameChange,
                    label = { Text("Folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (canCreate) "Selected ${selectedIds.size} bookmarks" else "Select at least 2 bookmarks",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    ) {
        itemsIndexed(options, key = { _, option -> option.bookmark.id }) { _, option ->
            val isSelected = option.bookmark.id in selectedIds
            val iconBitmap = option.icon?.let { icon -> remember(icon) { icon.asImageBitmap() } }
            ListItem(
                leadingContent = {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Icon(Icons.Filled.Android, contentDescription = null, modifier = Modifier.size(40.dp))
                    }
                },
                headlineContent = { Text(option.bookmark.label) },
                trailingContent = {
                    Checkbox(checked = isSelected, onCheckedChange = { onToggleSelected(option.bookmark.id) })
                },
                modifier = Modifier.clickable { onToggleSelected(option.bookmark.id) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FolderCreateScreenPreview() {
    FolderCreateContent(
        options = emptyList(),
        isLoading = false,
        folderName = "",
        selectedIds = emptySet(),
        onNameChange = {},
        onToggleSelected = {},
        onConfirm = {}
    )
}
