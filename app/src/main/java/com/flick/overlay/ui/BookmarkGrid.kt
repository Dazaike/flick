package com.flick.overlay.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
    contentVisible: Boolean = true,
    iconSpacing: Float = 6f,
    showIconBorder: Boolean = false,
    slideAnimation: Boolean = false,
    bounceEnabled: Boolean = false,
    iconSlideDirection: String = "RIGHT",
    availability: Map<Long, Boolean> = emptyMap(),
    onBookmarkClick: (OverlayBookmarkItem) -> Unit,
    onReorder: (List<com.flick.data.model.Bookmark>) -> Unit = {},
    onMergeIntoFolder: (com.flick.data.model.Bookmark, com.flick.data.model.Bookmark) -> Unit = { _, _ -> }
) {
    val motion = LocalMotion.current
    val localItems = remember(items) { mutableStateListOf<OverlayBookmarkItem>().apply { addAll(items) } }
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

    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(color = MaterialTheme.colorScheme.primary)
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 450.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .then(
                    if (applyNavigationBarPadding) {
                        Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 12.dp)
                    } else {
                        Modifier
                    }
                ),
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
                    visible = contentVisible,
                    index = index,
                    isAvailable = availability[item.bookmark.id] ?: true,
                    showIconBorder = showIconBorder,
                    slideAnimation = slideAnimation,
                    bounceEnabled = bounceEnabled,
                    iconSlideDirection = iconSlideDirection,
                    mergeHighlighted = mergeHighlightId == item.bookmark.id,
                    onClick = { onBookmarkClick(item) },
                    modifier = itemModifier
                        .pointerInput(item.bookmark.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    // Look up the live index via the grid's layout info rather than
                                    // the composition-time `index` capture, since this pointerInput
                                    // block is keyed on id and won't restart when items reorder.
                                    draggedIndex = gridState.layoutInfo.visibleItemsInfo
                                        .find { it.key == item.bookmark.id }?.index
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
                                    val draggedItemInfo = layoutInfo.visibleItemsInfo.find { it.index == draggedIdx }
                                        ?: return@detectDragGesturesAfterLongPress

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
                                        // Pointer left the grid entirely; cancel any pending merge intent.
                                        mergeCandidateId = null
                                        mergeHighlightId = null
                                        readyToMerge = false
                                    } else if (targetItem.index != draggedIdx) {
                                        // Entering a new slot: perform the usual live reorder-swap and
                                        // restart the dwell timer against the tile we just displaced.
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
                                        // Settled back over the slot of the last displaced tile without
                                        // triggering another swap: accumulate dwell time toward a merge.
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
                )
            }
        }
    }
}
