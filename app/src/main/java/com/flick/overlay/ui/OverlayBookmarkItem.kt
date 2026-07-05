package com.flick.overlay.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import com.flick.data.model.Bookmark

/**
 * A resolved overlay entry ready for display: the source [bookmark] plus its precomputed [icon]
 * bitmap (if any) and [isAvailable] flag (whether the target app/shortcut is still installed).
 *
 * Marked [Immutable] so Compose can skip recomposition of [BookmarkTile] when an unrelated item
 * in the grid changes; all fields are effectively read-only value data once constructed.
 */
@Immutable
data class OverlayBookmarkItem(
    val bookmark: Bookmark,
    val icon: Bitmap?,
    val isAvailable: Boolean = true,
    /** Up to 4 child icons for a folder bookmark's stacked mini-preview; empty for non-folders. */
    val childPreview: List<Bitmap?> = emptyList()
)
