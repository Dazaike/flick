package com.flick.overlay.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.flick.ui.theme.LocalMotion

/** How long the dragged tile must dwell over another tile (without leaving) to trigger a merge. */
private const val MERGE_DWELL_MS = 450L

@Composable
fun BookmarkGrid(
    items: List<OverlayBookmarkItem>,
    showLabels: Boolean,
    columns: Int = 4,
    applyNavigationBarPadding: Boolean = true,
    iconSpacing: Float = 6f,
    showIconBorder: Boolean = false,
    availability: Map<Long, Boolean> = emptyMap(),
    eagerLayout: Boolean = false,
    tilesReady: Boolean = true,
    slideIcons: Boolean = false,
    bounceEnabled: Boolean = false,
    onBookmarkClick: (OverlayBookmarkItem) -> Unit,
    onReorder: (List<com.flick.data.model.Bookmark>) -> Unit = {},
    onMergeIntoFolder: (com.flick.data.model.Bookmark, com.flick.data.model.Bookmark) -> Unit = { _, _ -> }
) {
    val motion = LocalMotion.current
    val localItems = remember(items) { mutableStateListOf<OverlayBookmarkItem>().apply { addAll(items) } }
    var tilesVisible by remember { mutableStateOf(!motion.enabled) }

    LaunchedEffect(items, motion.enabled, motion.intensity, tilesReady) {
        if (!motion.enabled) {
            tilesVisible = true
            return@LaunchedEffect
        }
        if (!tilesReady) {
            tilesVisible = false
            return@LaunchedEffect
        }
        tilesVisible = false
        withFrameNanos { }
        tilesVisible = true
    }

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val gridState = rememberLazyGridState()

    // Dwell-based merge tracking: when the pointer settles over another tile's slot (i.e. no
    // further reorder-swap is needed) for MERGE_DWELL_MS, releasing there merges the dragged
    // bookmark into that tile instead of just leaving it reordered next to it.
    var mergeCandidateId by remember { mutableStateOf<Long?>(null) }
    var settledSinceMs by remember { mutableStateOf(0L) }
    var mergeHighlightId by remember { mutableStateOf<Long?>(null) }
    var readyToMerge by remember { mutableStateOf(false) }
    val eagerTileLayouts = remember { mutableStateMapOf<Long, EagerTileLayout>() }

    val onDragStart: (OverlayBookmarkItem) -> Unit = { item ->
        draggedIndex = localItems.indexOfFirst { it.bookmark.id == item.bookmark.id }.takeIf { it >= 0 }
        dragOffset = Offset.Zero
        mergeCandidateId = null
        mergeHighlightId = null
        readyToMerge = false
        settledSinceMs = System.currentTimeMillis()
    }
    val onDrag: (OverlayBookmarkItem, PointerInputChange, Offset) -> Unit = drag@{ item, change, dragAmount ->
        change.consume()
        dragOffset += dragAmount

        val draggedIdx = draggedIndex ?: return@drag
        val draggedLayout = eagerTileLayouts.values.find { it.index == draggedIdx }
            ?: if (eagerLayout) return@drag else null

        if (eagerLayout) {
            val draggedInfo = draggedLayout ?: return@drag
            val currentCenterX = draggedInfo.offset.x + draggedInfo.size.width / 2f + dragOffset.x
            val currentCenterY = draggedInfo.offset.y + draggedInfo.size.height / 2f + dragOffset.y
            val targetLayout = eagerTileLayouts.values.find { info ->
                val left = info.offset.x
                val right = info.offset.x + info.size.width
                val top = info.offset.y
                val bottom = info.offset.y + info.size.height
                currentCenterX >= left && currentCenterX <= right &&
                    currentCenterY >= top && currentCenterY <= bottom
            }

            if (targetLayout == null) {
                mergeCandidateId = null
                mergeHighlightId = null
                readyToMerge = false
            } else if (targetLayout.index != draggedIdx) {
                mergeCandidateId = targetLayout.bookmarkId
                settledSinceMs = System.currentTimeMillis()
                readyToMerge = false
                mergeHighlightId = null
                localItems.add(targetLayout.index, localItems.removeAt(draggedIdx))
                draggedIndex = targetLayout.index
                dragOffset += Offset(
                    draggedInfo.offset.x - targetLayout.offset.x,
                    draggedInfo.offset.y - targetLayout.offset.y
                )
            } else if (mergeCandidateId != null) {
                val elapsed = System.currentTimeMillis() - settledSinceMs
                if (elapsed >= MERGE_DWELL_MS) {
                    readyToMerge = true
                    mergeHighlightId = mergeCandidateId
                }
            }
            return@drag
        }

        val layoutInfo = gridState.layoutInfo
        val draggedItemInfo = layoutInfo.visibleItemsInfo.find { it.index == draggedIdx }
            ?: return@drag

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
        } else if (targetItem.index != draggedIdx) {
            val targetIdx = targetItem.index
            mergeCandidateId = targetItem.key as? Long
            settledSinceMs = System.currentTimeMillis()
            readyToMerge = false
            mergeHighlightId = null
            localItems.add(targetIdx, localItems.removeAt(draggedIdx))
            draggedIndex = targetIdx
            dragOffset += Offset(
                (draggedItemInfo.offset.x - targetItem.offset.x).toFloat(),
                (draggedItemInfo.offset.y - targetItem.offset.y).toFloat()
            )
        } else if (mergeCandidateId != null) {
            val elapsed = System.currentTimeMillis() - settledSinceMs
            if (elapsed >= MERGE_DWELL_MS) {
                readyToMerge = true
                mergeHighlightId = mergeCandidateId
            }
        }
    }
    val onDragEnd: (OverlayBookmarkItem) -> Unit = { item ->
        draggedIndex = null
        dragOffset = Offset.Zero
        val targetId = mergeCandidateId
        val shouldMerge = readyToMerge && targetId != null
        mergeCandidateId = null
        mergeHighlightId = null
        readyToMerge = false
        val targetBookmark = if (shouldMerge) {
            localItems.find { it.bookmark.id == targetId }?.bookmark
        } else {
            null
        }
        if (targetBookmark != null) {
            onMergeIntoFolder(item.bookmark, targetBookmark)
        } else {
            onReorder(localItems.map { it.bookmark })
        }
    }
    val onDragCancel = {
        draggedIndex = null
        dragOffset = Offset.Zero
        mergeCandidateId = null
        mergeHighlightId = null
        readyToMerge = false
    }

    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(color = MaterialTheme.colorScheme.primary)
    ) {
        val gridModifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 450.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .then(
                if (applyNavigationBarPadding) {
                    Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 12.dp)
                } else {
                    Modifier
                }
            )

        if (eagerLayout) {
            EagerBookmarkGrid(
                items = localItems,
                columns = columns,
                iconSpacing = iconSpacing,
                showLabels = showLabels,
                showIconBorder = showIconBorder,
                availability = availability,
                mergeHighlightId = mergeHighlightId,
                tilesVisible = tilesVisible,
                slideAnimation = slideIcons,
                bounceEnabled = bounceEnabled,
                draggedIndex = draggedIndex,
                dragOffset = dragOffset,
                tileLayouts = eagerTileLayouts,
                modifier = gridModifier,
                onBookmarkClick = onBookmarkClick,
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel
            )
        } else {
            LazyBookmarkGrid(
                localItems = localItems,
                columns = columns,
                iconSpacing = iconSpacing,
                showLabels = showLabels,
                showIconBorder = showIconBorder,
                availability = availability,
                mergeHighlightId = mergeHighlightId,
                tilesVisible = tilesVisible,
                slideAnimation = slideIcons,
                bounceEnabled = bounceEnabled,
                gridState = gridState,
                draggedIndex = draggedIndex,
                dragOffset = dragOffset,
                modifier = gridModifier,
                onBookmarkClick = onBookmarkClick,
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel
            )
        }
    }
}

private data class EagerTileLayout(
    val bookmarkId: Long,
    val index: Int,
    val offset: Offset,
    val size: IntSize
)

@Composable
private fun EagerBookmarkGrid(
    items: List<OverlayBookmarkItem>,
    columns: Int,
    iconSpacing: Float,
    showLabels: Boolean,
    showIconBorder: Boolean,
    availability: Map<Long, Boolean>,
    mergeHighlightId: Long?,
    tilesVisible: Boolean,
    slideAnimation: Boolean,
    bounceEnabled: Boolean,
    draggedIndex: Int?,
    dragOffset: Offset,
    tileLayouts: MutableMap<Long, EagerTileLayout>,
    modifier: Modifier,
    onBookmarkClick: (OverlayBookmarkItem) -> Unit,
    onDragStart: (OverlayBookmarkItem) -> Unit,
    onDrag: (OverlayBookmarkItem, PointerInputChange, Offset) -> Unit,
    onDragEnd: (OverlayBookmarkItem) -> Unit,
    onDragCancel: () -> Unit
) {
    var gridCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Column(
        modifier = modifier.onGloballyPositioned { gridCoordinates = it },
        verticalArrangement = Arrangement.spacedBy(iconSpacing.dp)
    ) {
        items.chunked(columns).forEachIndexed { rowIndex, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(iconSpacing.dp)
            ) {
                rowItems.forEachIndexed { colIndex, item ->
                    val index = rowIndex * columns + colIndex
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
                        Modifier
                    }

                    BookmarkTile(
                        item = item,
                        showLabel = showLabels && item.bookmark.showLabel,
                        visible = tilesVisible,
                        index = index,
                        isAvailable = availability[item.bookmark.id] ?: true,
                        showIconBorder = showIconBorder,
                        slideAnimation = slideAnimation,
                        bounceEnabled = bounceEnabled,
                        mergeHighlighted = mergeHighlightId == item.bookmark.id,
                        onClick = { onBookmarkClick(item) },
                        modifier = itemModifier
                            .weight(1f)
                            .onGloballyPositioned { coordinates ->
                                gridCoordinates?.let { grid ->
                                    val offset = grid.localPositionOf(coordinates, Offset.Zero)
                                    tileLayouts[item.bookmark.id] = EagerTileLayout(
                                        bookmarkId = item.bookmark.id,
                                        index = index,
                                        offset = offset,
                                        size = coordinates.size
                                    )
                                }
                            }
                            .pointerInput(item.bookmark.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { onDragStart(item) },
                                    onDrag = { change, dragAmount -> onDrag(item, change, dragAmount) },
                                    onDragEnd = { onDragEnd(item) },
                                    onDragCancel = onDragCancel
                                )
                            }
                    )
                }
                repeat(columns - rowItems.size) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LazyBookmarkGrid(
    localItems: List<OverlayBookmarkItem>,
    columns: Int,
    iconSpacing: Float,
    showLabels: Boolean,
    showIconBorder: Boolean,
    availability: Map<Long, Boolean>,
    mergeHighlightId: Long?,
    tilesVisible: Boolean,
    slideAnimation: Boolean,
    bounceEnabled: Boolean,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    draggedIndex: Int?,
    dragOffset: Offset,
    modifier: Modifier,
    onBookmarkClick: (OverlayBookmarkItem) -> Unit,
    onDragStart: (OverlayBookmarkItem) -> Unit,
    onDrag: (OverlayBookmarkItem, PointerInputChange, Offset) -> Unit,
    onDragEnd: (OverlayBookmarkItem) -> Unit,
    onDragCancel: () -> Unit
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(iconSpacing.dp),
        verticalArrangement = Arrangement.spacedBy(iconSpacing.dp)
    ) {
        itemsIndexed(localItems, key = { _, item -> item.bookmark.id }) { index, item ->
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
                Modifier
            }

            BookmarkTile(
                item = item,
                showLabel = showLabels && item.bookmark.showLabel,
                visible = tilesVisible,
                index = index,
                isAvailable = availability[item.bookmark.id] ?: true,
                showIconBorder = showIconBorder,
                slideAnimation = slideAnimation,
                bounceEnabled = bounceEnabled,
                mergeHighlighted = mergeHighlightId == item.bookmark.id,
                onClick = { onBookmarkClick(item) },
                modifier = itemModifier
                    .pointerInput(item.bookmark.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart(item) },
                            onDrag = { change, dragAmount -> onDrag(item, change, dragAmount) },
                            onDragEnd = { onDragEnd(item) },
                            onDragCancel = onDragCancel
                        )
                    }
            )
        }
    }
}
