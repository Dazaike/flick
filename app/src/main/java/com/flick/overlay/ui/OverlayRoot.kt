package com.flick.overlay.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flick.data.model.Bookmark
import com.flick.data.model.BookmarkAction
import com.flick.ui.theme.FlickTheme
import com.flick.ui.theme.LocalMotion
import kotlinx.coroutines.delay

@Composable
fun OverlayRoot(
    bookmarks: List<OverlayBookmarkItem>,
    showLabels: Boolean,
    blurIntensity: Float = 0f,
    popupOpacity: Float = 0.92f,
    rightPopup: Boolean = false,
    iconSpacing: Float = 6f,
    showIconBorder: Boolean = false,
    slideAnimation: Boolean = false,
    bottomBounce: Boolean = false,
    bottomSlideUp: Boolean = false,
    rightBounce: Boolean = true,
    rightSlideIn: Boolean = false,
    rightPopupYOffset: Float = 0f,
    panelAnimationSpeed: Float = 1f,
    iconAnimationSpeed: Float = 1f,
    availability: Map<Long, Boolean> = emptyMap(),
    onBookmarkClick: (OverlayBookmarkItem) -> Unit,
    onDismiss: () -> Unit,
    onReorder: (List<Bookmark>) -> Unit = {},
    onMergeIntoFolder: (Bookmark, Bookmark) -> Unit = { _, _ -> }
) {
    FlickTheme {
        val motion = LocalMotion.current
        val panelMotionConfig = remember(motion, panelAnimationSpeed) {
            motion.copy(intensity = panelAnimationSpeed)
        }
        val iconMotionConfig = remember(motion, iconAnimationSpeed) {
            motion.copy(intensity = iconAnimationSpeed)
        }
        val instantOpen = !motion.enabled
        var shown by remember { mutableStateOf(instantOpen) }
        var activeFolderId by remember { mutableStateOf<Long?>(null) }

        val effectiveBottomSlideUp = bottomSlideUp || slideAnimation
        val effectiveRightSlideIn = rightSlideIn || slideAnimation
        val overlayPanelMotion = if (rightPopup) {
            OverlayPanelMotion(slide = effectiveRightSlideIn, bounce = rightBounce)
        } else {
            OverlayPanelMotion(slide = effectiveBottomSlideUp, bounce = bottomBounce)
        }

        val scrimAlpha = if (blurIntensity > 0f) 0.12f else 0.4f

        BackHandler(enabled = shown) {
            if (activeFolderId != null) {
                activeFolderId = null
            } else {
                shown = false
            }
        }

        LaunchedEffect(Unit) {
            if (!instantOpen) shown = true
        }

        LaunchedEffect(shown) {
            if (!shown) {
                delay(220)
                onDismiss()
            }
        }

        OverlayScrimWithPanel(
            shown = shown,
            placement = if (rightPopup) OverlayPanelPlacement.Right else OverlayPanelPlacement.Bottom,
            motionConfig = panelMotionConfig,
            panelMotion = overlayPanelMotion,
            scrimAlpha = scrimAlpha,
            panelOpacity = popupOpacity,
            rightPanelYOffset = rightPopupYOffset.dp,
            onScrimClick = { shown = false }
        ) { iconsReady ->
            CompositionLocalProvider(LocalMotion provides iconMotionConfig) {
                FolderAwareGrid(
                    bookmarks = bookmarks,
                    activeFolderId = activeFolderId,
                    onActiveFolderChange = { activeFolderId = it },
                    showLabels = showLabels,
                    columns = if (rightPopup) 2 else 4,
                    applyNavigationBarPadding = !rightPopup,
                    iconSpacing = iconSpacing,
                    showIconBorder = showIconBorder,
                    slideIcons = if (rightPopup) effectiveRightSlideIn else effectiveBottomSlideUp,
                    bounceEnabled = if (rightPopup) rightBounce else bottomBounce,
                    tilesReady = iconsReady,
                    availability = availability,
                    onBookmarkClick = onBookmarkClick,
                    onReorder = onReorder,
                    onMergeIntoFolder = onMergeIntoFolder
                )
            }
        }
    }
}

/**
 * Folder navigation for the overlay. Uses an instant swap (no scale transitions) so content is
 * never clipped while the panel is opening.
 */
@Composable
private fun FolderAwareGrid(
    bookmarks: List<OverlayBookmarkItem>,
    activeFolderId: Long?,
    onActiveFolderChange: (Long?) -> Unit,
    showLabels: Boolean,
    columns: Int = 4,
    applyNavigationBarPadding: Boolean = true,
    iconSpacing: Float = 6f,
    showIconBorder: Boolean = false,
    slideIcons: Boolean = false,
    bounceEnabled: Boolean = false,
    tilesReady: Boolean = true,
    availability: Map<Long, Boolean> = emptyMap(),
    onBookmarkClick: (OverlayBookmarkItem) -> Unit,
    onReorder: (List<Bookmark>) -> Unit,
    onMergeIntoFolder: (Bookmark, Bookmark) -> Unit
) {
    val activeFolderItem = remember(bookmarks, activeFolderId) {
        activeFolderId?.let { id -> bookmarks.find { it.bookmark.id == id } }
    }
    val displayedItems = remember(bookmarks, activeFolderId) {
        bookmarks.filter { it.bookmark.parentFolderId == activeFolderId }
    }

    Column {
        if (activeFolderItem != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onActiveFolderChange(null) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        }
        BookmarkGrid(
            items = displayedItems,
            showLabels = showLabels,
            columns = columns,
            applyNavigationBarPadding = applyNavigationBarPadding,
            iconSpacing = iconSpacing,
            showIconBorder = showIconBorder,
            slideIcons = slideIcons,
            bounceEnabled = bounceEnabled,
            tilesReady = tilesReady,
            availability = availability,
            eagerLayout = true,
            onBookmarkClick = { item ->
                if (item.bookmark.action is BookmarkAction.Folder) {
                    onActiveFolderChange(item.bookmark.id)
                } else {
                    onBookmarkClick(item)
                }
            },
            onReorder = onReorder,
            onMergeIntoFolder = onMergeIntoFolder
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OverlayRootPreview() {
    OverlayRoot(
        bookmarks = listOf(
            OverlayBookmarkItem(
                bookmark = Bookmark(
                    id = 1,
                    categoryId = 1,
                    label = "Example",
                    sortOrder = 0,
                    action = BookmarkAction.WebUrl("https://example.com")
                ),
                icon = null
            )
        ),
        showLabels = true,
        onBookmarkClick = {},
        onDismiss = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun OverlayRootRightPreview() {
    OverlayRoot(
        bookmarks = listOf(
            OverlayBookmarkItem(
                bookmark = Bookmark(
                    id = 1,
                    categoryId = 1,
                    label = "Example",
                    sortOrder = 0,
                    action = BookmarkAction.WebUrl("https://example.com")
                ),
                icon = null
            )
        ),
        showLabels = true,
        rightPopup = true,
        onBookmarkClick = {},
        onDismiss = {}
    )
}
