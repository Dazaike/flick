package com.flick.ui.screens.bookmarklist

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.flick.action.BookmarkActionExecutor
import com.flick.data.model.Bookmark
import com.flick.data.model.BookmarkAction
import com.flick.overlay.OverlayService
import com.flick.permissions.OverlayPermissionHelper
import com.flick.ui.prism.ButtonVariant
import com.flick.ui.prism.ConfirmDialog
import com.flick.ui.prism.GlassCheckbox
import com.flick.ui.prism.GlassDialog
import com.flick.ui.prism.GlassIconButton
import com.flick.ui.prism.HapticKind
import com.flick.ui.prism.LocalHaptics
import com.flick.ui.prism.LocalToasts
import com.flick.ui.prism.PrismIcon
import com.flick.ui.prism.PrismIcons
import com.flick.ui.prism.PrismListItem
import com.flick.ui.prism.PrismScreen
import com.flick.ui.prism.ToastKind
import com.flick.ui.prism.pressInput
import com.flick.ui.prism.rememberPressState
import com.flick.ui.theme.DURATION_MEDIUM
import com.flick.ui.theme.LocalMotion
import com.flick.ui.theme.Prism
import com.flick.ui.theme.PrismText
import com.flick.ui.theme.ThemePreferences
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.max

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
    val toasts = LocalToasts.current
    val scope = rememberCoroutineScope()
    val themePreferences = remember { ThemePreferences(context.applicationContext) }
    val motion = LocalMotion.current
    var gridView by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        themePreferences.gridView.collectLatest { gridView = it }
    }

    val launchBookmark: (BookmarkAction) -> Unit = { action ->
        if (!executor.execute(context, action)) {
            toasts.show("Couldn't launch bookmark", ToastKind.Error)
        }
    }

    PrismScreen(
        title = "Flick",
        modifier = modifier,
        actions = { bd ->
            GlassIconButton(
                backdrop = bd,
                icon = if (gridView) PrismIcons.ViewList else PrismIcons.Grid,
                contentDescription = if (gridView) "Switch to list view" else "Switch to grid view",
                onClick = {
                    val newValue = !gridView
                    gridView = newValue
                    scope.launch { themePreferences.setGridView(newValue) }
                },
                size = 44.dp
            )
            GlassIconButton(
                backdrop = bd,
                icon = PrismIcons.Settings,
                contentDescription = "Settings",
                onClick = onOpenSettings,
                size = 44.dp
            )
            GlassIconButton(
                backdrop = bd,
                icon = PrismIcons.Palette,
                contentDescription = "Icon packs",
                onClick = onOpenIconPacks,
                size = 44.dp
            )
            GlassIconButton(
                backdrop = bd,
                icon = PrismIcons.Play,
                contentDescription = "Show overlay (debug)",
                onClick = {
                    if (OverlayPermissionHelper.canDrawOverlays(context)) {
                        runCatching {
                            ContextCompat.startForegroundService(
                                context,
                                android.content.Intent(context, OverlayService::class.java)
                            )
                        }.onFailure {
                            toasts.show("Couldn't show overlay", ToastKind.Error)
                        }
                    } else {
                        context.startActivity(
                            OverlayPermissionHelper.requestOverlayPermissionIntent(context)
                        )
                    }
                },
                size = 44.dp
            )
        },
        floatingAction = { bd ->
            GlassIconButton(
                backdrop = bd,
                icon = PrismIcons.Plus,
                contentDescription = "Add bookmark",
                onClick = { defaultCategoryId?.let { onAddBookmark(it) } },
                variant = ButtonVariant.Primary,
                size = 56.dp
            )
        }
    ) { padding, contentBackdrop ->
        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(motion.fade(DURATION_MEDIUM)) + scaleIn(motion.glide(), initialScale = 0.8f)
                ) {
                    PrismText("No bookmarks yet — tap + to add one", color = Prism.subText)
                }
            }
        } else {
            var editingBookmark by remember { mutableStateOf<Bookmark?>(null) }
            var localBookmarks by remember(bookmarks) { mutableStateOf(bookmarks) }
            var expandedFolders by remember { mutableStateOf(setOf<Long>()) }
            var folderPendingDelete by remember { mutableStateOf<Bookmark?>(null) }
            var folderAddingTo by remember { mutableStateOf<Bookmark?>(null) }

            LaunchedEffect(bookmarks) {
                localBookmarks = bookmarks
            }

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
                BookmarkListDragGrid(
                    bookmarks = localBookmarks,
                    icons = icons,
                    expandedFolders = expandedFolders,
                    contentPadding = padding,
                    backdrop = contentBackdrop,
                    viewModel = viewModel,
                    onToggleFolderExpanded = toggleFolderExpanded,
                    onFolderAddBookmarks = { folderAddingTo = it },
                    onRequestDelete = requestDelete,
                    onBookmarkClick = { bookmark -> launchBookmark(bookmark.action) },
                    onEditBookmark = { editingBookmark = it },
                    onFolderChildClick = { child -> launchBookmark(child.action) },
                    onFolderChildEdit = { editingBookmark = it },
                    onFolderChildDelete = requestDelete,
                    onRemoveFromFolder = { viewModel.removeFromFolder(it) },
                    onReorder = { reordered ->
                        localBookmarks = reordered
                        viewModel.updateAllSortOrders(reordered)
                    },
                    onMergeIntoFolder = { draggedId, targetId ->
                        viewModel.mergeIntoFolder(draggedId, targetId)
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = padding
                ) {
                    bookmarks.forEach { bookmark ->
                        item(key = bookmark.id) {
                            if (bookmark.action is BookmarkAction.Folder) {
                                FolderListRow(
                                    bookmark = bookmark,
                                    isExpanded = bookmark.id in expandedFolders,
                                    backdrop = contentBackdrop,
                                    onClick = { toggleFolderExpanded(bookmark.id) },
                                    onAddBookmarks = { folderAddingTo = bookmark },
                                    onDelete = { requestDelete(bookmark) },
                                    modifier = Modifier.animateItem(placementSpec = motion.glide())
                                )
                            } else {
                                BookmarkRow(
                                    bookmark = bookmark,
                                    icon = icons[bookmark.id],
                                    backdrop = contentBackdrop,
                                    onClick = { launchBookmark(bookmark.action) },
                                    onEdit = { editingBookmark = bookmark },
                                    onDelete = { requestDelete(bookmark) },
                                    onMoveUp = { viewModel.moveUp(bookmark) },
                                    onMoveDown = { viewModel.moveDown(bookmark) },
                                    modifier = Modifier.animateItem(placementSpec = motion.glide())
                                )
                            }
                        }

                        if (bookmark.action is BookmarkAction.Folder && bookmark.id in expandedFolders) {
                            item(key = "folder_children_${bookmark.id}") {
                                FolderChildrenList(
                                    folderId = bookmark.id,
                                    viewModel = viewModel,
                                    backdrop = contentBackdrop,
                                    onClick = { child -> launchBookmark(child.action) },
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
                ConfirmDialog(
                    visible = true,
                    title = "Delete \"${folder.label}\"?",
                    message = "This will also delete everything inside this folder. This can't be undone.",
                    confirmLabel = "Delete",
                    onConfirm = {
                        viewModel.delete(folder)
                        folderPendingDelete = null
                    },
                    onDismiss = { folderPendingDelete = null },
                    destructive = true
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

/** Small flat glass action button used in rows and cards. */
@Composable
private fun RowIconButton(
    backdrop: Backdrop,
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassIconButton(
        backdrop = backdrop,
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        variant = ButtonVariant.Ghost,
        size = 36.dp
    )
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
            PrismIcon(PrismIcons.App, null, size = 24.dp)
        }
    }
}

/** Rounded container card with press highlight and optional merge-target outline. */
@Composable
private fun CardSurface(
    onClick: () -> Unit,
    mergeHighlighted: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = Prism.colors
    val accent = Prism.accent
    val haptics = LocalHaptics.current
    val press = rememberPressState()
    val shape = RoundedRectangle(20.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .height(140.dp)
            .clip(shape)
            .background(colors.container)
            .then(if (mergeHighlighted) Modifier.border(2.dp, accent, shape) else Modifier)
            .drawBehind {
                val a = colors.fillWeak.alpha * max(press.hover, press.progress * 1.6f)
                if (a > 0f) drawRect(colors.fillWeak.copy(alpha = a))
            }
            .pressInput(press)
            .clickable(interactionSource = press.interactionSource, indication = null, role = Role.Button) {
                haptics.perform(HapticKind.Tick)
                onClick()
            },
        content = content
    )
}

@Composable
private fun BookmarkRow(
    bookmark: Bookmark,
    icon: Bitmap?,
    backdrop: Backdrop,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onRemoveFromFolder: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    PrismListItem(
        headline = bookmark.label,
        supporting = bookmark.action::class.simpleName ?: "",
        leading = { BookmarkIcon(icon = icon, modifier = Modifier.size(40.dp)) },
        trailing = {
            Row {
                if (onRemoveFromFolder != null) {
                    RowIconButton(backdrop, PrismIcons.Close, "Remove from folder", onRemoveFromFolder)
                } else {
                    RowIconButton(backdrop, PrismIcons.ChevronUp, "Move up", onMoveUp)
                    RowIconButton(backdrop, PrismIcons.ChevronDown, "Move down", onMoveDown)
                }
                RowIconButton(backdrop, PrismIcons.Edit, "Edit", onEdit)
                RowIconButton(backdrop, PrismIcons.Trash, "Delete", onDelete)
            }
        },
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
internal fun BookmarkGridCard(
    bookmark: Bookmark,
    icon: Bitmap?,
    backdrop: Backdrop,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRemoveFromFolder: (() -> Unit)? = null,
    mergeHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    CardSurface(onClick = onClick, mergeHighlighted = mergeHighlighted, modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            BookmarkIcon(icon = icon, modifier = Modifier.size(40.dp))
            PrismText(
                text = bookmark.label,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.Center) {
                if (onRemoveFromFolder != null) {
                    RowIconButton(backdrop, PrismIcons.Close, "Remove from folder", onRemoveFromFolder)
                }
                RowIconButton(backdrop, PrismIcons.Edit, "Edit", onEdit)
                RowIconButton(backdrop, PrismIcons.Trash, "Delete", onDelete)
            }
        }
    }
}

/** Top-level folder row for list view: folder glyph, label, and a live "N items" subtitle. */
@Composable
private fun FolderListRow(
    bookmark: Bookmark,
    isExpanded: Boolean,
    backdrop: Backdrop,
    onClick: () -> Unit,
    onAddBookmarks: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookmarkListViewModel = hiltViewModel()
) {
    val children by viewModel.observeChildren(bookmark.id).collectAsState(initial = emptyList())
    PrismListItem(
        headline = bookmark.label,
        supporting = "${children.size} item${if (children.size == 1) "" else "s"}",
        leading = {
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                PrismIcon(PrismIcons.Folder, null, size = 24.dp)
            }
        },
        trailing = {
            Row {
                RowIconButton(backdrop, PrismIcons.FolderPlus, "Add bookmarks", onAddBookmarks)
                RowIconButton(
                    backdrop,
                    if (isExpanded) PrismIcons.ChevronUp else PrismIcons.ChevronDown,
                    if (isExpanded) "Collapse" else "Expand",
                    onClick
                )
                RowIconButton(backdrop, PrismIcons.Trash, "Delete", onDelete)
            }
        },
        onClick = onClick,
        modifier = modifier
    )
}

/** Top-level folder card for grid view, mirroring [FolderListRow]'s affordances. */
@Composable
internal fun FolderGridCard(
    bookmark: Bookmark,
    isExpanded: Boolean,
    backdrop: Backdrop,
    onClick: () -> Unit,
    onAddBookmarks: () -> Unit,
    onDelete: () -> Unit,
    mergeHighlighted: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: BookmarkListViewModel = hiltViewModel()
) {
    val children by viewModel.observeChildren(bookmark.id).collectAsState(initial = emptyList())
    CardSurface(onClick = onClick, mergeHighlighted = mergeHighlighted, modifier = modifier) {
        RowIconButton(
            backdrop = backdrop,
            icon = PrismIcons.Trash,
            contentDescription = "Delete folder",
            onClick = onDelete,
            modifier = Modifier.align(Alignment.TopEnd)
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            PrismIcon(PrismIcons.Folder, null, size = 40.dp)
            PrismText(
                text = "${bookmark.label} (${children.size})",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.Center) {
                RowIconButton(backdrop, PrismIcons.FolderPlus, "Add bookmarks", onAddBookmarks)
                RowIconButton(
                    backdrop,
                    if (isExpanded) PrismIcons.ChevronUp else PrismIcons.ChevronDown,
                    if (isExpanded) "Collapse" else "Expand",
                    onClick
                )
            }
        }
    }
}

/** Inline expanded contents of a folder in list view, indented beneath its row. */
@Composable
private fun FolderChildrenList(
    folderId: Long,
    viewModel: BookmarkListViewModel,
    backdrop: Backdrop,
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
            PrismText(
                text = "This folder is empty",
                color = Prism.subText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        children.forEach { child ->
            BookmarkRow(
                bookmark = child,
                icon = childIcons[child.id],
                backdrop = backdrop,
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
internal fun FolderChildrenGridRow(
    folderId: Long,
    viewModel: BookmarkListViewModel,
    backdrop: Backdrop,
    onClick: (Bookmark) -> Unit,
    onEdit: (Bookmark) -> Unit,
    onDelete: (Bookmark) -> Unit,
    onRemoveFromFolder: (Bookmark) -> Unit
) {
    val children by viewModel.observeChildren(folderId).collectAsState(initial = emptyList())
    var childIcons by remember(folderId) { mutableStateOf(emptyMap<Long, Bitmap?>()) }
    LaunchedEffect(children) { childIcons = viewModel.resolveIconsFor(children) }

    if (children.isEmpty()) {
        PrismText(
            text = "This folder is empty",
            color = Prism.subText,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    } else {
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp)) {
            children.forEach { child ->
                Box(modifier = Modifier.width(140.dp)) {
                    BookmarkGridCard(
                        bookmark = child,
                        icon = childIcons[child.id],
                        backdrop = backdrop,
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

    GlassDialog(
        visible = true,
        title = "Add to \"${folder.label}\"",
        onDismiss = onDismiss,
        confirmLabel = "Add",
        onConfirm = { onConfirm(selected.toList()) },
        confirmEnabled = selected.isNotEmpty()
    ) { _ ->
        if (candidates.isEmpty()) {
            PrismText("No other bookmarks available to add.", color = Prism.subText)
        } else {
            candidates.forEach { candidate ->
                val isChecked = candidate.id in selected
                val toggle = {
                    selected = if (isChecked) selected - candidate.id else selected + candidate.id
                }
                PrismListItem(
                    headline = candidate.label,
                    leading = { BookmarkIcon(icon = icons[candidate.id], modifier = Modifier.size(32.dp)) },
                    trailing = {
                        GlassCheckbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + candidate.id else selected - candidate.id
                            }
                        )
                    },
                    onClick = toggle
                )
            }
        }
    }
}
