package com.flick.ui.screens.addbookmark

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import com.flick.ui.prism.GlassCheckbox
import com.flick.ui.prism.GlassIconButton
import com.flick.ui.prism.GlassTextField
import com.flick.ui.prism.PrismIcon
import com.flick.ui.prism.PrismIcons
import com.flick.ui.prism.PrismListItem
import com.flick.ui.theme.PrismText
import com.kyant.shapes.RoundedRectangle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
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
        topBarActions = { bd ->
            GlassIconButton(
                backdrop = bd,
                icon = PrismIcons.Check,
                contentDescription = "Create",
                onClick = onConfirm,
                size = 44.dp,
                enabled = canCreate
            )
        },
        emptyContent = {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                PrismText("Add a couple of bookmarks first, then group them into a folder.")
            }
        },
        headerContent = {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                GlassTextField(
                    value = folderName,
                    onValueChange = onNameChange,
                    label = "Folder name",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                PrismText(
                    text = if (canCreate) "Selected ${selectedIds.size} bookmarks" else "Select at least 2 bookmarks",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    ) {
        itemsIndexed(options, key = { _, option -> option.bookmark.id }) { _, option ->
            val isSelected = option.bookmark.id in selectedIds
            val iconBitmap = option.icon?.let { icon -> remember(icon) { icon.asImageBitmap() } }
            PrismListItem(
                leading = {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(RoundedRectangle(8.dp))
                        )
                    } else {
                        PrismIcon(PrismIcons.App, contentDescription = null, size = 40.dp)
                    }
                },
                headline = option.bookmark.label,
                trailing = {
                    GlassCheckbox(checked = isSelected, onCheckedChange = { onToggleSelected(option.bookmark.id) })
                },
                onClick = { onToggleSelected(option.bookmark.id) }
            )
        }
    }
}
