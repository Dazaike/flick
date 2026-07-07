package com.flick.overlay.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.key
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.flick.ui.theme.LocalMotion
import kotlin.math.roundToInt

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
    var orderIds by remember { mutableStateOf(items.map { it.bookmark.id }) }
    var tilesVisible by remember { mutableStateOf(!motion.enabled) }
    var draggedBookmarkId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(items) {
        if (draggedBookmarkId != null) return@LaunchedEffect
        localItems.clear()
        localItems.addAll(items)
        orderIds = items.map { it.bookmark.id }
    }

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

    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragFingerPosition by remember { mutableStateOf(Offset.Zero) }
    var dragGrabOffset by remember { mutableStateOf(Offset.Zero) }
    val gridState = rememberLazyGridState()

    fun draggedItemIndex(): Int? =
        draggedBookmarkId?.let { id ->
            orderIds.indexOf(id).takeIf { it >= 0 }
        }

    fun itemById(id: Long): OverlayBookmarkItem? =
        localItems.find { it.bookmark.id == id }

    // Dwell-based merge tracking: when the pointer settles over another tile's slot (i.e. no
    // further reorder-swap is needed) for MERGE_DWELL_MS, releasing there merges the dragged
    // bookmark into that tile instead of just leaving it reordered next to it.
    var mergeCandidateId by remember { mutableStateOf<Long?>(null) }
    var settledSinceMs by remember { mutableStateOf(0L) }
    var mergeHighlightId by remember { mutableStateOf<Long?>(null) }
    var readyToMerge by remember { mutableStateOf(false) }
    val eagerTileLayouts = remember { mutableStateMapOf<Long, EagerTileLayout>() }

    fun applyOrderToLocalItems() {
        val byId = localItems.associateBy { it.bookmark.id }
        localItems.clear()
        orderIds.forEach { id -> byId[id]?.let { localItems.add(it) } }
    }

    val onDragStart: (OverlayBookmarkItem, Offset) -> Unit = { item, pressOffset ->
        draggedBookmarkId = item.bookmark.id
        orderIds = localItems.map { it.bookmark.id }
        dragOffset = Offset.Zero
        val layout = eagerTileLayouts[item.bookmark.id]
        if (layout != null) {
            dragGrabOffset = pressOffset - layout.offset
            dragFingerPosition = pressOffset
        } else {
            dragGrabOffset = Offset.Zero
            dragFingerPosition = pressOffset
        }
        mergeCandidateId = null
        mergeHighlightId = null
        readyToMerge = false
        settledSinceMs = System.currentTimeMillis()
    }

    fun handleDragMove(change: PointerInputChange, dragAmount: Offset) {
        change.consume()
        dragOffset += dragAmount
        dragFingerPosition += dragAmount

        val draggedId = draggedBookmarkId ?: return
        val draggedIdx = draggedItemIndex() ?: return

        if (eagerLayout) {
            val tileSize = eagerTileLayouts[draggedId]?.size ?: return
            val tileTopLeft = dragFingerPosition - dragGrabOffset
            val currentCenterX = tileTopLeft.x + tileSize.width / 2f
            val currentCenterY = tileTopLeft.y + tileSize.height / 2f
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
                if (elapsed >= MERGE_DWELL_MS) {
                    readyToMerge = true
                    mergeHighlightId = mergeCandidateId
                }
            }
            return
        }

        val draggedIdxLazy = localItems.indexOfFirst { it.bookmark.id == draggedId }
        if (draggedIdxLazy < 0) return

        val layoutInfo = gridState.layoutInfo
        val draggedItemInfo = layoutInfo.visibleItemsInfo.find { it.key == draggedId } ?: return

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
        } else if (targetItem.key != draggedId) {
            val targetIdx = targetItem.index
            mergeCandidateId = targetItem.key as? Long
            settledSinceMs = System.currentTimeMillis()
            readyToMerge = false
            mergeHighlightId = null
            localItems.add(targetIdx, localItems.removeAt(draggedIdxLazy))
            val targetItemInfo = layoutInfo.visibleItemsInfo.find { it.key == targetItem.key }
            if (targetItemInfo != null) {
                dragOffset += Offset(
                    (draggedItemInfo.offset.x - targetItemInfo.offset.x).toFloat(),
                    (draggedItemInfo.offset.y - targetItemInfo.offset.y).toFloat()
                )
            }
        } else if (mergeCandidateId != null) {
            val elapsed = System.currentTimeMillis() - settledSinceMs
            if (elapsed >= MERGE_DWELL_MS) {
                readyToMerge = true
                mergeHighlightId = mergeCandidateId
            }
        }
    }

    val onDragEnd: () -> Unit = {
        val draggedBookmark = draggedBookmarkId?.let { id -> itemById(id)?.bookmark }
        draggedBookmarkId = null
        dragOffset = Offset.Zero
        dragGrabOffset = Offset.Zero
        dragFingerPosition = Offset.Zero
        val targetId = mergeCandidateId
        val shouldMerge = readyToMerge && targetId != null
        mergeCandidateId = null
        mergeHighlightId = null
        readyToMerge = false
        if (draggedBookmark != null) {
            applyOrderToLocalItems()
            val targetBookmark = if (shouldMerge) itemById(targetId!!)?.bookmark else null
            if (targetBookmark != null) {
                onMergeIntoFolder(draggedBookmark, targetBookmark)
            } else {
                onReorder(localItems.map { it.bookmark })
            }
        }
    }

    val onDragCancel = {
        draggedBookmarkId = null
        dragOffset = Offset.Zero
        dragGrabOffset = Offset.Zero
        dragFingerPosition = Offset.Zero
        mergeCandidateId = null
        mergeHighlightId = null
        readyToMerge = false
        orderIds = localItems.map { it.bookmark.id }
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
                orderIds = orderIds,
                itemsById = { id -> itemById(id) },
                columns = columns,
                iconSpacing = iconSpacing,
                showLabels = showLabels,
                showIconBorder = showIconBorder,
                availability = availability,
                mergeHighlightId = mergeHighlightId,
                tilesVisible = tilesVisible,
                slideAnimation = slideIcons,
                bounceEnabled = bounceEnabled,
                draggedBookmarkId = draggedBookmarkId,
                dragFingerPosition = dragFingerPosition,
                dragGrabOffset = dragGrabOffset,
                tileLayouts = eagerTileLayouts,
                modifier = gridModifier,
                onBookmarkClick = onBookmarkClick,
                onDragStart = onDragStart,
                onDragMove = ::handleDragMove,
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
                draggedBookmarkId = draggedBookmarkId,
                dragOffset = dragOffset,
                modifier = gridModifier,
                onBookmarkClick = onBookmarkClick,
                onDragStart = onDragStart,
                onDragMove = ::handleDragMove,
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

private fun hitTestTile(offset: Offset, layouts: Map<Long, EagerTileLayout>): EagerTileLayout? =
    layouts.values.find { layout ->
        offset.x >= layout.offset.x &&
            offset.x <= layout.offset.x + layout.size.width &&
            offset.y >= layout.offset.y &&
            offset.y <= layout.offset.y + layout.size.height
    }

@Composable
private fun EagerBookmarkGrid(
    orderIds: List<Long>,
    itemsById: (Long) -> OverlayBookmarkItem?,
    columns: Int,
    iconSpacing: Float,
    showLabels: Boolean,
    showIconBorder: Boolean,
    availability: Map<Long, Boolean>,
    mergeHighlightId: Long?,
    tilesVisible: Boolean,
    slideAnimation: Boolean,
    bounceEnabled: Boolean,
    draggedBookmarkId: Long?,
    dragFingerPosition: Offset,
    dragGrabOffset: Offset,
    tileLayouts: MutableMap<Long, EagerTileLayout>,
    modifier: Modifier,
    onBookmarkClick: (OverlayBookmarkItem) -> Unit,
    onDragStart: (OverlayBookmarkItem, Offset) -> Unit,
    onDragMove: (PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    var gridCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val density = LocalDensity.current
    val tileHeight = if (showLabels) 76.dp else 54.dp
    var dragSourceItem by remember { mutableStateOf<OverlayBookmarkItem?>(null) }
    val dragTileTopLeft = dragFingerPosition - dragGrabOffset

    Box(
        modifier = modifier
            .onGloballyPositioned { gridCoordinates = it }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (draggedBookmarkId != null) return@detectTapGestures
                    val hit = hitTestTile(offset, tileLayouts) ?: return@detectTapGestures
                    itemsById(hit.bookmarkId)?.let(onBookmarkClick)
                }
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val hit = hitTestTile(offset, tileLayouts) ?: return@detectDragGesturesAfterLongPress
                        val item = itemsById(hit.bookmarkId) ?: return@detectDragGesturesAfterLongPress
                        dragSourceItem = item
                        onDragStart(item, offset)
                    },
                    onDrag = { change, dragAmount -> onDragMove(change, dragAmount) },
                    onDragEnd = {
                        dragSourceItem = null
                        onDragEnd()
                    },
                    onDragCancel = {
                        dragSourceItem = null
                        onDragCancel()
                    }
                )
            }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(iconSpacing.dp)) {
            orderIds.chunked(columns).forEachIndexed { rowIndex, rowIds ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(iconSpacing.dp)
                ) {
                    rowIds.forEachIndexed { colIndex, bookmarkId ->
                        val index = rowIndex * columns + colIndex
                        val item = itemsById(bookmarkId)
                        if (item == null) {
                            Spacer(modifier = Modifier.weight(1f).height(tileHeight))
                            return@forEachIndexed
                        }

                        if (bookmarkId == draggedBookmarkId) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(tileHeight)
                                    .onGloballyPositioned { coordinates ->
                                        gridCoordinates?.let { grid ->
                                            val offset = grid.localPositionOf(coordinates, Offset.Zero)
                                            tileLayouts[bookmarkId] = EagerTileLayout(
                                                bookmarkId = bookmarkId,
                                                index = index,
                                                offset = offset,
                                                size = coordinates.size
                                            )
                                        }
                                    }
                            )
                        } else {
                            key(bookmarkId) {
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
                                    clickEnabled = draggedBookmarkId == null,
                                    onClick = { onBookmarkClick(item) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { coordinates ->
                                            gridCoordinates?.let { grid ->
                                                val offset = grid.localPositionOf(coordinates, Offset.Zero)
                                                tileLayouts[bookmarkId] = EagerTileLayout(
                                                    bookmarkId = bookmarkId,
                                                    index = index,
                                                    offset = offset,
                                                    size = coordinates.size
                                                )
                                            }
                                        }
                                )
                            }
                        }
                    }
                    repeat(columns - rowIds.size) {
                        Spacer(modifier = Modifier.weight(1f).height(tileHeight))
                    }
                }
            }
        }

        val draggedId = draggedBookmarkId
        val draggedLayout = if (draggedId != null) tileLayouts[draggedId] else null
        val draggedItem = dragSourceItem ?: draggedId?.let { itemsById(it) }
        if (draggedItem != null && draggedLayout != null) {
            val tileWidth = with(density) { draggedLayout.size.width.toDp() }
            val tileHeightDp = with(density) { draggedLayout.size.height.toDp() }
            BookmarkTile(
                item = draggedItem,
                showLabel = showLabels && draggedItem.bookmark.showLabel,
                visible = true,
                index = draggedLayout.index,
                isAvailable = availability[draggedItem.bookmark.id] ?: true,
                showIconBorder = showIconBorder,
                slideAnimation = false,
                bounceEnabled = false,
                mergeHighlighted = mergeHighlightId == draggedItem.bookmark.id,
                clickEnabled = false,
                animateEnter = false,
                onClick = {},
                modifier = Modifier
                    .offset {
                        IntOffset(
                            dragTileTopLeft.x.roundToInt(),
                            dragTileTopLeft.y.roundToInt()
                        )
                    }
                    .width(tileWidth)
                    .height(tileHeightDp)
                    .zIndex(10f)
                    .graphicsLayer {
                        scaleX = 1.08f
                        scaleY = 1.08f
                        alpha = 0.9f
                    }
            )
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
    draggedBookmarkId: Long?,
    dragOffset: Offset,
    modifier: Modifier,
    onBookmarkClick: (OverlayBookmarkItem) -> Unit,
    onDragStart: (OverlayBookmarkItem, Offset) -> Unit,
    onDragMove: (PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit,
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
            val isDragging = draggedBookmarkId == item.bookmark.id
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
                clickEnabled = draggedBookmarkId == null,
                onClick = { onBookmarkClick(item) },
                modifier = Modifier
                    .pointerInput(item.bookmark.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart(item, Offset.Zero) },
                            onDrag = { change, dragAmount -> onDragMove(change, dragAmount) },
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel
                        )
                    }
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        if (isDragging) {
                            translationX = dragOffset.x
                            translationY = dragOffset.y
                            scaleX = 1.08f
                            scaleY = 1.08f
                            alpha = 0.9f
                        }
                    }
            )
        }
    }
}
