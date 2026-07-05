package com.flick.ui.screens.bookmarklist

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.flick.action.BookmarkActionExecutor
import com.flick.data.model.Bookmark
import com.flick.data.model.BookmarkAction
import com.flick.overlay.OverlayService
import com.flick.permissions.OverlayPermissionHelper
import com.flick.ui.theme.DURATION_MEDIUM
import com.flick.ui.theme.LocalMotion
import com.flick.ui.theme.ThemePreferences
import com.flick.ui.theme.flickSpring
import com.flick.ui.theme.flickTween
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** How long a dragged grid card must dwell over another's slot to trigger a folder merge. */
private const val MERGE_DWELL_MS = 450L

@Composable
fun BookmarkListScreen(
    modifier: Modifier = Modifier,
    onAddBookmark: (categoryId: Long) -> Unit,
    onOpenIconPacks: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: BookmarkListViewModel = hiltViewModel()
) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val icons by viewModel.icons.collectAsState()
    val defaultCategoryId by viewModel.defaultCategoryId.collectAsState()
    val context = LocalContext.current
    val executor = remember { BookmarkActionExecutor() }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val themePreferences = remember { ThemePreferences(context.applicationContext) }
    val motion = LocalMotion.current
    var gridView by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        themePreferences.gridView.collectLatest { gridView = it }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Flick") },
                actions = {
                    IconButton(
                        onClick = {
                            val newValue = !gridView
                            gridView = newValue
                            snackbarScope.launch { themePreferences.setGridView(newValue) }
                        },
                        modifier = Modifier
                            .padding(2.dp)
                    ) {
                        AnimatedContent(
                            targetState = gridView,
                            transitionSpec = {
                                (scaleIn(motion.flickSpring()) + fadeIn(motion.flickTween(DURATION_MEDIUM))) togetherWith
                                    (scaleOut(motion.flickTween(120)) + fadeOut(motion.flickTween(120)))
                            },
                            label = "viewToggleIcon"
                        ) { isGrid ->
                            Icon(
                                imageVector = if (isGrid) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                                contentDescription = if (isGrid) "Switch to list view" else "Switch to grid view"
                            )
                        }
                    }
                    BorderedIconButton(
                        icon = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        onClick = onOpenSettings
                    )
                    BorderedIconButton(
                        icon = Icons.Filled.Palette,
                        contentDescription = "Icon packs",
                        onClick = onOpenIconPacks
                    )
                    BorderedIconButton(
                        icon = Icons.Filled.PlayArrow,
                        contentDescription = "Show overlay (debug)",
                        onClick = {
                            if (OverlayPermissionHelper.canDrawOverlays(context)) {
                                runCatching {
                                    ContextCompat.startForegroundService(
                                        context,
                                        android.content.Intent(context, OverlayService::class.java)
                                    )
                                }.onFailure {
                                    snackbarScope.launch {
                                        snackbarHostState.showSnackbar("Couldn't show overlay")
                                    }
                                }
                            } else {
                                context.startActivity(
                                    OverlayPermissionHelper.requestOverlayPermissionIntent(context)
                                )
                            }
                        }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { defaultCategoryId?.let { onAddBookmark(it) } },
                modifier = Modifier
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add bookmark")
            }
        }
    ) { padding ->
        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(motion.flickTween(DURATION_MEDIUM)) + scaleIn(motion.flickSpring(), initialScale = 0.8f)
                ) {
                    Text("No bookmarks yet — tap + to add one")
                }
            }
        } else {
            var editingBookmark by remember { mutableStateOf<Bookmark?>(null) }
            var localBookmarks by remember(bookmarks) { mutableStateOf(bookmarks) }
            var draggedIndex by remember { mutableStateOf<Int?>(null) }
            var dragOffset by remember { mutableStateOf(Offset.Zero) }
            var expandedFolders by remember { mutableStateOf(setOf<Long>()) }
            var folderPendingDelete by remember { mutableStateOf<Bookmark?>(null) }
            var folderAddingTo by remember { mutableStateOf<Bookmark?>(null) }
            var mergeCandidateId by remember { mutableStateOf<Long?>(null) }
            var settledSinceMs by remember { mutableStateOf(0L) }
            var mergeHighlightId by remember { mutableStateOf<Long?>(null) }
            var readyToMerge by remember { mutableStateOf(false) }
            val gridState = rememberLazyGridState()
            val idToIndex by remember { derivedStateOf { localBookmarks.withIndex().associate { (i, b) -> b.id to i } } }

            val requestDelete: (Bookmark) -> Unit = { bookmark ->
                if (bookmark.action is BookmarkAction.Folder) {
                    folderPendingDelete = bookmark
                } else {
                    viewModel.delete(bookmark)
                }
            }
            val toggleFolderExpanded: (Long) -> Unit = { folderId ->
                expandedFolders = if (folderId in expandedFolders) expandedFolders - folderId else expandedFolders + folderId
            }

            if (gridView) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    localBookmarks.forEachIndexed { index, bookmark ->
                        item(key = bookmark.id) {
                            val isDragging = draggedIndex == index
                            val itemModifier = if (isDragging) {
                                Modifier
                                    .zIndex(1f)
                                    .graphicsLayer {
                                        translationX = dragOffset.x
                                        translationY = dragOffset.y
                                        scaleX = 1.08f
                                        scaleY = 1.08f
                                        alpha = 0.9f
                                    }
                            } else {
                                Modifier.animateItem(placementSpec = motion.flickSpring())
                            }

                            val dragModifier = itemModifier
                                .pointerInput(bookmark.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { offset ->
                                            draggedIndex = idToIndex[bookmark.id]
                                            dragOffset = Offset.Zero
                                            mergeCandidateId = null
                                            mergeHighlightId = null
                                            readyToMerge = false
                                            settledSinceMs = System.currentTimeMillis()
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount

                                            val draggedIdx = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                            val layoutInfo = gridState.layoutInfo
                                            val draggedItemInfo = layoutInfo.visibleItemsInfo.find { it.key == bookmark.id } ?: return@detectDragGesturesAfterLongPress

                                            val currentCenterX = draggedItemInfo.offset.x + draggedItemInfo.size.width / 2 + dragOffset.x
                                            val currentCenterY = draggedItemInfo.offset.y + draggedItemInfo.size.height / 2 + dragOffset.y

                                            val targetItem = layoutInfo.visibleItemsInfo.find { info ->
                                                val left = info.offset.x
                                                val right = info.offset.x + info.size.width
                                                val top = info.offset.y
                                                val bottom = info.offset.y + info.size.height
                                                currentCenterX >= left && currentCenterX <= right && currentCenterY >= top && currentCenterY <= bottom
                                            }

                                            if (targetItem == null) {
                                                mergeCandidateId = null
                                                mergeHighlightId = null
                                                readyToMerge = false
                                            } else if (targetItem.key != bookmark.id) {
                                                val targetIdx = (targetItem.key as? Long)?.let { idToIndex[it] } ?: -1
                                                if (targetIdx != -1) {
                                                    mergeCandidateId = targetItem.key as? Long
                                                    settledSinceMs = System.currentTimeMillis()
                                                    readyToMerge = false
                                                    mergeHighlightId = null
                                                    localBookmarks = localBookmarks.toMutableList().apply {
                                                        add(targetIdx, removeAt(draggedIdx))
                                                    }
                                                    draggedIndex = targetIdx
                                                    val targetItemInfo = layoutInfo.visibleItemsInfo.find { it.key == targetItem.key }
                                                    if (targetItemInfo != null) {
                                                        dragOffset += Offset(
                                                            (draggedItemInfo.offset.x - targetItemInfo.offset.x).toFloat(),
                                                            (draggedItemInfo.offset.y - targetItemInfo.offset.y).toFloat()
                                                        )
                                                    }
                                                }
                                            } else if (mergeCandidateId != null) {
                                                val elapsed = System.currentTimeMillis() - settledSinceMs
                                                if (elapsed >= MERGE_DWELL_MS) {
                                                    readyToMerge = true
                                                    mergeHighlightId = mergeCandidateId
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggedIndex = null
                                            dragOffset = Offset.Zero
                                            val targetId = mergeCandidateId
                                            val shouldMerge = readyToMerge
                                            mergeCandidateId = null
                                            mergeHighlightId = null
                                            readyToMerge = false
                                            if (shouldMerge && targetId != null) {
                                                viewModel.mergeIntoFolder(bookmark.id, targetId)
                                            } else {
                                                viewModel.updateAllSortOrders(localBookmarks)
                                            }
                                        },
                                        onDragCancel = {
                                            draggedIndex = null
                                            dragOffset = Offset.Zero
                                            mergeCandidateId = null
                                            mergeHighlightId = null
                                            readyToMerge = false
                                        }
                                    )
                                }

                            if (bookmark.action is BookmarkAction.Folder) {
                                FolderGridCard(
                                    bookmark = bookmark,
                                    isExpanded = bookmark.id in expandedFolders,
                                    mergeHighlighted = mergeHighlightId == bookmark.id,
                                    onClick = { toggleFolderExpanded(bookmark.id) },
                                    onAddBookmarks = { folderAddingTo = bookmark },
                                    onDelete = { requestDelete(bookmark) },
                                    modifier = dragModifier
                                )
                            } else {
                                BookmarkGridCard(
                                    bookmark = bookmark,
                                    icon = icons[bookmark.id],
                                    mergeHighlighted = mergeHighlightId == bookmark.id,
                                    onClick = {
                                        if (!executor.execute(context, bookmark.action)) {
                                            snackbarScope.launch {
                                                snackbarHostState.showSnackbar("Couldn't launch bookmark")
                                            }
                                        }
                                    },
                                    onEdit = { editingBookmark = bookmark },
                                    onDelete = { requestDelete(bookmark) },
                                    modifier = dragModifier
                                )
                            }
                        }

                        if (bookmark.action is BookmarkAction.Folder && bookmark.id in expandedFolders) {
                            item(key = "folder_children_grid_${bookmark.id}", span = { GridItemSpan(maxLineSpan) }) {
                                FolderChildrenGridRow(
                                    folderId = bookmark.id,
                                    viewModel = viewModel,
                                    onClick = { child ->
                                        if (!executor.execute(context, child.action)) {
                                            snackbarScope.launch {
                                                snackbarHostState.showSnackbar("Couldn't launch bookmark")
                                            }
                                        }
                                    },
                                    onEdit = { child -> editingBookmark = child },
                                    onDelete = { child -> requestDelete(child) },
                                    onRemoveFromFolder = { child -> viewModel.removeFromFolder(child) }
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    bookmarks.forEach { bookmark ->
                        item(key = bookmark.id) {
                            if (bookmark.action is BookmarkAction.Folder) {
                                FolderListRow(
                                    bookmark = bookmark,
                                    isExpanded = bookmark.id in expandedFolders,
                                    onClick = { toggleFolderExpanded(bookmark.id) },
                                    onAddBookmarks = { folderAddingTo = bookmark },
                                    onDelete = { requestDelete(bookmark) },
                                    modifier = Modifier.animateItem(placementSpec = motion.flickSpring())
                                )
                            } else {
                                BookmarkRow(
                                    bookmark = bookmark,
                                    icon = icons[bookmark.id],
                                    onClick = {
                                        if (!executor.execute(context, bookmark.action)) {
                                            snackbarScope.launch {
                                                snackbarHostState.showSnackbar("Couldn't launch bookmark")
                                            }
                                        }
                                    },
                                    onEdit = { editingBookmark = bookmark },
                                    onDelete = { requestDelete(bookmark) },
                                    onMoveUp = { viewModel.moveUp(bookmark) },
                                    onMoveDown = { viewModel.moveDown(bookmark) },
                                    modifier = Modifier.animateItem(placementSpec = motion.flickSpring())
                                )
                            }
                        }

                        if (bookmark.action is BookmarkAction.Folder && bookmark.id in expandedFolders) {
                            item(key = "folder_children_${bookmark.id}") {
                                FolderChildrenList(
                                    folderId = bookmark.id,
                                    viewModel = viewModel,
                                    onClick = { child ->
                                        if (!executor.execute(context, child.action)) {
                                            snackbarScope.launch {
                                                snackbarHostState.showSnackbar("Couldn't launch bookmark")
                                            }
                                        }
                                    },
                                    onEdit = { child -> editingBookmark = child },
                                    onDelete = { child -> requestDelete(child) },
                                    onRemoveFromFolder = { child -> viewModel.removeFromFolder(child) }
                                )
                            }
                        }
                    }
                }
            }

            folderPendingDelete?.let { folder ->
                AlertDialog(
                    onDismissRequest = { folderPendingDelete = null },
                    title = { Text("Delete \"${folder.label}\"?") },
                    text = { Text("This will also delete everything inside this folder. This can't be undone.") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.delete(folder)
                            folderPendingDelete = null
                        }) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { folderPendingDelete = null }) { Text("Cancel") }
                    }
                )
            }

            folderAddingTo?.let { folder ->
                AddToFolderDialog(
                    folder = folder,
                    candidates = bookmarks.filter { it.id != folder.id && it.action !is BookmarkAction.Folder },
                    icons = icons,
                    onDismiss = { folderAddingTo = null },
                    onConfirm = { selectedIds ->
                        viewModel.addToFolder(folder.id, selectedIds)
                        expandedFolders = expandedFolders + folder.id
                        folderAddingTo = null
                    }
                )
            }

            editingBookmark?.let { bookmark ->
                EditBookmarkDialog(
                    bookmark = bookmark,
                    onDismiss = { editingBookmark = null },
                    onSave = { updated ->
                        viewModel.update(updated)
                        editingBookmark = null
                    }
                )
            }
        }
    }
}

@Composable
private fun BorderedIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(2.dp)
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
private fun BookmarkIcon(icon: Bitmap?, modifier: Modifier = Modifier) {
    if (icon != null) {
        Image(
            bitmap = icon.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Android, contentDescription = null)
        }
    }
}

@Composable
private fun BookmarkRow(
    bookmark: Bookmark,
    icon: Bitmap?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onRemoveFromFolder: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ListItem(
        leadingContent = { BookmarkIcon(icon = icon, modifier = Modifier.size(40.dp)) },
        headlineContent = { Text(bookmark.label) },
        supportingContent = { Text(bookmark.action::class.simpleName ?: "") },
        trailingContent = {
            Row {
                if (onRemoveFromFolder != null) {
                    BorderedIconButton(Icons.Filled.Close, "Remove from folder", onRemoveFromFolder)
                } else {
                    BorderedIconButton(Icons.Filled.KeyboardArrowUp, "Move up", onMoveUp)
                    BorderedIconButton(Icons.Filled.KeyboardArrowDown, "Move down", onMoveDown)
                }
                BorderedIconButton(Icons.Filled.Edit, "Edit", onEdit)
                BorderedIconButton(Icons.Filled.Delete, "Delete", onDelete)
            }
        },
        modifier = modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun BookmarkGridCard(
    bookmark: Bookmark,
    icon: Bitmap?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRemoveFromFolder: (() -> Unit)? = null,
    mergeHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .height(140.dp)
            .then(
                if (mergeHighlighted) {
                    Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            BookmarkIcon(icon = icon, modifier = Modifier.size(40.dp))
            Text(
                text = bookmark.label,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.Center) {
                if (onRemoveFromFolder != null) {
                    BorderedIconButton(Icons.Filled.Close, "Remove from folder", onRemoveFromFolder)
                }
                BorderedIconButton(Icons.Filled.Edit, "Edit", onEdit)
                BorderedIconButton(Icons.Filled.Delete, "Delete", onDelete)
            }
        }
    }
}

/** Top-level folder row for list view: folder glyph, label, and a live "N items" subtitle. */
@Composable
private fun FolderListRow(
    bookmark: Bookmark,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onAddBookmarks: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookmarkListViewModel = hiltViewModel()
) {
    val children by viewModel.observeChildren(bookmark.id).collectAsState(initial = emptyList())
    ListItem(
        leadingContent = {
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Folder, contentDescription = null)
            }
        },
        headlineContent = { Text(bookmark.label) },
        supportingContent = { Text("${children.size} item${if (children.size == 1) "" else "s"}") },
        trailingContent = {
            Row {
                BorderedIconButton(Icons.AutoMirrored.Filled.PlaylistAdd, "Add bookmarks", onAddBookmarks)
                BorderedIconButton(
                    if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    if (isExpanded) "Collapse" else "Expand",
                    onClick
                )
                BorderedIconButton(Icons.Filled.Delete, "Delete", onDelete)
            }
        },
        modifier = modifier.clickable(onClick = onClick)
    )
}

/** Top-level folder card for grid view, mirroring [FolderListRow]'s affordances. */
@Composable
private fun FolderGridCard(
    bookmark: Bookmark,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onAddBookmarks: () -> Unit,
    onDelete: () -> Unit,
    mergeHighlighted: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: BookmarkListViewModel = hiltViewModel()
) {
    val children by viewModel.observeChildren(bookmark.id).collectAsState(initial = emptyList())
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .height(140.dp)
            .then(
                if (mergeHighlighted) {
                    Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(40.dp))
            Text(
                text = "${bookmark.label} (${children.size})",
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.Center) {
                BorderedIconButton(Icons.AutoMirrored.Filled.PlaylistAdd, "Add bookmarks", onAddBookmarks)
                BorderedIconButton(
                    if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    if (isExpanded) "Collapse" else "Expand",
                    onClick
                )
                BorderedIconButton(Icons.Filled.Delete, "Delete", onDelete)
            }
        }
    }
}

/** Inline expanded contents of a folder in list view, indented beneath its row. */
@Composable
private fun FolderChildrenList(
    folderId: Long,
    viewModel: BookmarkListViewModel,
    onClick: (Bookmark) -> Unit,
    onEdit: (Bookmark) -> Unit,
    onDelete: (Bookmark) -> Unit,
    onRemoveFromFolder: (Bookmark) -> Unit
) {
    val children by viewModel.observeChildren(folderId).collectAsState(initial = emptyList())
    var childIcons by remember(folderId) { mutableStateOf(emptyMap<Long, Bitmap?>()) }
    LaunchedEffect(children) { childIcons = viewModel.resolveIconsFor(children) }

    Column(modifier = Modifier.padding(start = 24.dp)) {
        if (children.isEmpty()) {
            Text(
                text = "This folder is empty",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        children.forEach { child ->
            BookmarkRow(
                bookmark = child,
                icon = childIcons[child.id],
                onClick = { onClick(child) },
                onEdit = { onEdit(child) },
                onDelete = { onDelete(child) },
                onRemoveFromFolder = { onRemoveFromFolder(child) }
            )
        }
    }
}

/** Inline expanded contents of a folder in grid view: a horizontally scrollable row of cards. */
@Composable
private fun FolderChildrenGridRow(
    folderId: Long,
    viewModel: BookmarkListViewModel,
    onClick: (Bookmark) -> Unit,
    onEdit: (Bookmark) -> Unit,
    onDelete: (Bookmark) -> Unit,
    onRemoveFromFolder: (Bookmark) -> Unit
) {
    val children by viewModel.observeChildren(folderId).collectAsState(initial = emptyList())
    var childIcons by remember(folderId) { mutableStateOf(emptyMap<Long, Bitmap?>()) }
    LaunchedEffect(children) { childIcons = viewModel.resolveIconsFor(children) }

    if (children.isEmpty()) {
        Text(
            text = "This folder is empty",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    } else {
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp)) {
            children.forEach { child ->
                Box(modifier = Modifier.width(140.dp)) {
                    BookmarkGridCard(
                        bookmark = child,
                        icon = childIcons[child.id],
                        onClick = { onClick(child) },
                        onEdit = { onEdit(child) },
                        onDelete = { onDelete(child) },
                        onRemoveFromFolder = { onRemoveFromFolder(child) }
                    )
                }
            }
        }
    }
}

/** Dialog for picking existing top-level bookmarks to move into [folder]. */
@Composable
private fun AddToFolderDialog(
    folder: Bookmark,
    candidates: List<Bookmark>,
    icons: Map<Long, Bitmap?>,
    onDismiss: () -> Unit,
    onConfirm: (List<Long>) -> Unit
) {
    var selected by remember { mutableStateOf(setOf<Long>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to \"${folder.label}\"") },
        text = {
            if (candidates.isEmpty()) {
                Text("No other bookmarks available to add.")
            } else {
                Column {
                    candidates.forEach { candidate ->
                        val isChecked = candidate.id in selected
                        ListItem(
                            leadingContent = { BookmarkIcon(icon = icons[candidate.id], modifier = Modifier.size(32.dp)) },
                            headlineContent = { Text(candidate.label) },
                            trailingContent = {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selected = if (checked) selected + candidate.id else selected - candidate.id
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                selected = if (isChecked) selected - candidate.id else selected + candidate.id
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selected.toList()) },
                enabled = selected.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun BookmarkRowPreview() {
    BookmarkRow(
        bookmark = Bookmark(
            id = 1,
            categoryId = 1,
            label = "Example",
            sortOrder = 0,
            action = com.flick.data.model.BookmarkAction.WebUrl("https://example.com")
        ),
        icon = null,
        onClick = {},
        onEdit = {},
        onDelete = {}
    )
}
