package com.flick.ui.screens.bookmarklist

import android.graphics.Bitmap
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.flick.data.model.Bookmark
import com.flick.data.model.BookmarkAction
import kotlin.math.roundToInt

private const val DRAG_MERGE_DWELL_MS = 450L

private data class BookmarkCardLayout(
    val bookmarkId: Long,
    val offset: Offset,
    val size: IntSize
)

private fun hitTestCard(offset: Offset, layouts: Map<Long, BookmarkCardLayout>): BookmarkCardLayout? =
    layouts.values.find { layout ->
        offset.x >= layout.offset.x &&
            offset.x <= layout.offset.x + layout.size.width &&
            offset.y >= layout.offset.y &&
            offset.y <= layout.offset.y + layout.size.height
    }

@Composable
fun BookmarkListDragGrid(
    bookmarks: List<Bookmark>,
    icons: Map<Long, Bitmap?>,
    expandedFolders: Set<Long>,
    contentPadding: PaddingValues,
    viewModel: BookmarkListViewModel,
    onToggleFolderExpanded: (Long) -> Unit,
    onFolderAddBookmarks: (Bookmark) -> Unit,
    onRequestDelete: (Bookmark) -> Unit,
    onBookmarkClick: (Bookmark) -> Unit,
    onEditBookmark: (Bookmark) -> Unit,
    onFolderChildClick: (Bookmark) -> Unit,
    onFolderChildEdit: (Bookmark) -> Unit,
    onFolderChildDelete: (Bookmark) -> Unit,
    onRemoveFromFolder: (Bookmark) -> Unit,
    onReorder: (List<Bookmark>) -> Unit,
    onMergeIntoFolder: (draggedId: Long, targetId: Long) -> Unit
) {
    var orderIds by remember(bookmarks) { mutableStateOf(bookmarks.map { it.id }) }
    var draggedBookmarkId by remember { mutableStateOf<Long?>(null) }
    var dragFingerPosition by remember { mutableStateOf(Offset.Zero) }
    var dragGrabOffset by remember { mutableStateOf(Offset.Zero) }
    var dragSourceBookmark by remember { mutableStateOf<Bookmark?>(null) }
    var mergeCandidateId by remember { mutableStateOf<Long?>(null) }
    var mergeHighlightId by remember { mutableStateOf<Long?>(null) }
    var readyToMerge by remember { mutableStateOf(false) }
    var settledSinceMs by remember { mutableStateOf(0L) }
    val cardLayouts = remember { mutableStateMapOf<Long, BookmarkCardLayout>() }
    var gridCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val density = LocalDensity.current
    val cardHeight = 148.dp

    LaunchedEffect(bookmarks) {
        if (draggedBookmarkId != null) return@LaunchedEffect
        orderIds = bookmarks.map { it.id }
    }

    val bookmarksById = remember(bookmarks) { bookmarks.associateBy { it.id } }
    val orderedBookmarks = remember(orderIds, bookmarksById) {
        orderIds.mapNotNull { bookmarksById[it] }
    }
    val dragTileTopLeft = dragFingerPosition - dragGrabOffset

    fun finishDrag(merged: Boolean, draggedId: Long, targetId: Long?) {
        val reordered = orderIds.mapNotNull { bookmarksById[it] }
        onReorder(reordered)
        if (merged && targetId != null) {
            onMergeIntoFolder(draggedId, targetId)
        }
        draggedBookmarkId = null
        dragGrabOffset = Offset.Zero
        dragFingerPosition = Offset.Zero
        dragSourceBookmark = null
        mergeCandidateId = null
        mergeHighlightId = null
        readyToMerge = false
    }

    fun handleDragMove(change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: Offset) {
        change.consume()
        dragFingerPosition += dragAmount

        val draggedId = draggedBookmarkId ?: return
        val draggedIdx = orderIds.indexOf(draggedId)
        if (draggedIdx < 0) return

        val tileSize = cardLayouts[draggedId]?.size ?: return
        val tileTopLeft = dragFingerPosition - dragGrabOffset
        val centerX = tileTopLeft.x + tileSize.width / 2f
        val centerY = tileTopLeft.y + tileSize.height / 2f
        val targetLayout = cardLayouts.values.find { info ->
            centerX >= info.offset.x &&
                centerX <= info.offset.x + info.size.width &&
                centerY >= info.offset.y &&
                centerY <= info.offset.y + info.size.height
        }

        if (targetLayout == null) {
            mergeCandidateId = null
            mergeHighlightId = null
            readyToMerge = false
        } else if (targetLayout.bookmarkId != draggedId) {
            mergeCandidateId = targetLayout.bookmarkId
            settledSinceMs = System.currentTimeMillis()
            readyToMerge = false
            mergeHighlightId = null
            val targetIdx = orderIds.indexOf(targetLayout.bookmarkId)
            if (targetIdx >= 0) {
                orderIds = orderIds.toMutableList().apply {
                    removeAt(draggedIdx)
                    add(targetIdx, draggedId)
                }
            }
        } else if (mergeCandidateId != null) {
            val elapsed = System.currentTimeMillis() - settledSinceMs
            if (elapsed >= DRAG_MERGE_DWELL_MS) {
                readyToMerge = true
                mergeHighlightId = mergeCandidateId
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .onGloballyPositioned { gridCoordinates = it }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val hit = hitTestCard(offset, cardLayouts) ?: return@detectDragGesturesAfterLongPress
                        val bookmark = bookmarksById[hit.bookmarkId] ?: return@detectDragGesturesAfterLongPress
                        dragSourceBookmark = bookmark
                        draggedBookmarkId = bookmark.id
                        orderIds = bookmarks.map { it.id }
                        dragGrabOffset = offset - hit.offset
                        dragFingerPosition = offset
                        mergeCandidateId = null
                        mergeHighlightId = null
                        readyToMerge = false
                        settledSinceMs = System.currentTimeMillis()
                    },
                    onDrag = ::handleDragMove,
                    onDragEnd = {
                        val draggedId = draggedBookmarkId ?: return@detectDragGesturesAfterLongPress
                        val targetId = mergeCandidateId
                        val shouldMerge = readyToMerge && targetId != null
                        finishDrag(shouldMerge, draggedId, targetId)
                    },
                    onDragCancel = {
                        draggedBookmarkId = null
                        dragGrabOffset = Offset.Zero
                        dragFingerPosition = Offset.Zero
                        dragSourceBookmark = null
                        orderIds = bookmarks.map { it.id }
                        mergeCandidateId = null
                        mergeHighlightId = null
                        readyToMerge = false
                    }
                )
            }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            orderedBookmarks.forEach { bookmark ->
                item(key = bookmark.id) {
                    if (bookmark.id == draggedBookmarkId) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                                .height(cardHeight)
                                .onGloballyPositioned { coordinates ->
                                    gridCoordinates?.let { grid ->
                                        val offset = grid.localPositionOf(coordinates, Offset.Zero)
                                        cardLayouts[bookmark.id] = BookmarkCardLayout(
                                            bookmarkId = bookmark.id,
                                            offset = offset,
                                            size = coordinates.size
                                        )
                                    }
                                }
                        )
                    } else {
                        key(bookmark.id) {
                            val layoutModifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    gridCoordinates?.let { grid ->
                                        val offset = grid.localPositionOf(coordinates, Offset.Zero)
                                        cardLayouts[bookmark.id] = BookmarkCardLayout(
                                            bookmarkId = bookmark.id,
                                            offset = offset,
                                            size = coordinates.size
                                        )
                                    }
                                }

                            if (bookmark.action is BookmarkAction.Folder) {
                                FolderGridCard(
                                    bookmark = bookmark,
                                    isExpanded = bookmark.id in expandedFolders,
                                    mergeHighlighted = mergeHighlightId == bookmark.id,
                                    onClick = { onToggleFolderExpanded(bookmark.id) },
                                    onAddBookmarks = { onFolderAddBookmarks(bookmark) },
                                    onDelete = { onRequestDelete(bookmark) },
                                    modifier = layoutModifier
                                )
                            } else {
                                BookmarkGridCard(
                                    bookmark = bookmark,
                                    icon = icons[bookmark.id],
                                    mergeHighlighted = mergeHighlightId == bookmark.id,
                                    onClick = { onBookmarkClick(bookmark) },
                                    onEdit = { onEditBookmark(bookmark) },
                                    onDelete = { onRequestDelete(bookmark) },
                                    modifier = layoutModifier
                                )
                            }
                        }
                    }
                }

                if (bookmark.action is BookmarkAction.Folder && bookmark.id in expandedFolders) {
                    item(key = "folder_children_grid_${bookmark.id}", span = { GridItemSpan(maxLineSpan) }) {
                        FolderChildrenGridRow(
                            folderId = bookmark.id,
                            viewModel = viewModel,
                            onClick = onFolderChildClick,
                            onEdit = onFolderChildEdit,
                            onDelete = onFolderChildDelete,
                            onRemoveFromFolder = onRemoveFromFolder
                        )
                    }
                }
            }
        }

        val draggedId = draggedBookmarkId
        val draggedLayout = draggedId?.let { cardLayouts[it] }
        val draggedBookmark = dragSourceBookmark ?: draggedId?.let { bookmarksById[it] }
        if (draggedBookmark != null && draggedLayout != null) {
            val cardWidth = with(density) { draggedLayout.size.width.toDp() }
            val cardHeightDp = with(density) { draggedLayout.size.height.toDp() }
            val overlayModifier = Modifier
                .offset {
                    IntOffset(
                        dragTileTopLeft.x.roundToInt(),
                        dragTileTopLeft.y.roundToInt()
                    )
                }
                .width(cardWidth)
                .height(cardHeightDp)
                .zIndex(10f)
                .graphicsLayer {
                    scaleX = 1.08f
                    scaleY = 1.08f
                    alpha = 0.9f
                }

            if (draggedBookmark.action is BookmarkAction.Folder) {
                FolderGridCard(
                    bookmark = draggedBookmark,
                    isExpanded = draggedBookmark.id in expandedFolders,
                    mergeHighlighted = mergeHighlightId == draggedBookmark.id,
                    onClick = {},
                    onAddBookmarks = {},
                    onDelete = {},
                    modifier = overlayModifier
                )
            } else {
                BookmarkGridCard(
                    bookmark = draggedBookmark,
                    icon = icons[draggedBookmark.id],
                    mergeHighlighted = mergeHighlightId == draggedBookmark.id,
                    onClick = {},
                    onEdit = {},
                    onDelete = {},
                    modifier = overlayModifier
                )
            }
        }
    }
}
