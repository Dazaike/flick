package com.flick.overlay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flick.data.model.Bookmark
import com.flick.data.model.BookmarkAction
import com.flick.ui.theme.DURATION_SLOW
import com.flick.ui.theme.LocalMotion
import com.flick.ui.theme.flickSpring
import com.flick.ui.theme.flickTween
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
    bottomIconSlideDirection: String = "RIGHT",
    rightIconSlideDirection: String = "RIGHT",
    availability: Map<Long, Boolean> = emptyMap(),
    onBookmarkClick: (OverlayBookmarkItem) -> Unit,
    onDismiss: () -> Unit,
    onReorder: (List<Bookmark>) -> Unit = {},
    onMergeIntoFolder: (Bookmark, Bookmark) -> Unit = { _, _ -> }
) {
    com.flick.ui.theme.FlickTheme {
        val motion = LocalMotion.current
        var visible by remember { mutableStateOf(false) }
        var contentVisible by remember { mutableStateOf(false) }
        var activeFolderId by remember { mutableStateOf<Long?>(null) }
        val targetDimAlpha = if (blurIntensity > 0f) 0.12f else 0.4f
        val dimAnimSpec = remember(motion) { motion.flickTween<Float>(DURATION_SLOW) }
        val dimAlpha by animateFloatAsState(
            targetValue = if (visible) targetDimAlpha else 0f,
            animationSpec = dimAnimSpec,
            label = "scrimDim"
        )

        // `slideAnimation` is a master switch: when enabled it turns on the slide-in/out
        // popup transition even if the direction-specific bottom/right toggles are off.
        val effectiveBottomSlideUp = bottomSlideUp || slideAnimation
        val effectiveRightSlideIn = rightSlideIn || slideAnimation

        BackHandler(enabled = visible) {
            if (activeFolderId != null) {
                activeFolderId = null
            } else {
                visible = false
            }
        }

        LaunchedEffect(Unit) { visible = true }

        LaunchedEffect(visible) {
            if (visible) {
                delay(140)
                contentVisible = true
            } else {
                contentVisible = false
                delay(220)
                onDismiss()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { visible = false }
                ),
            contentAlignment = if (rightPopup) Alignment.CenterEnd else Alignment.BottomCenter
        ) {
            if (rightPopup) {
                val rightEnter = remember(motion, effectiveRightSlideIn, rightBounce) {
                    if (effectiveRightSlideIn) {
                        val slideSpring = if (rightBounce) {
                            motion.flickSpring<IntOffset>(dampingRatio = 0.25f, stiffness = 500f)
                        } else {
                            motion.flickSpring<IntOffset>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                        }
                        fadeIn(motion.flickTween(220)) + slideInHorizontally(animationSpec = slideSpring) { fullWidth -> fullWidth }
                    } else {
                        val springSpec = if (rightBounce) {
                            motion.flickSpring<Float>(dampingRatio = 0.25f, stiffness = 500f)
                        } else {
                            motion.flickSpring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                        }
                        fadeIn(motion.flickTween(300)) + scaleIn(
                            animationSpec = springSpec,
                            initialScale = 0.4f,
                            transformOrigin = TransformOrigin(1f, 0.5f)
                        )
                    }
                }
                val rightExit = remember(motion, effectiveRightSlideIn) {
                    if (effectiveRightSlideIn) {
                        fadeOut(motion.flickTween(150)) + slideOutHorizontally(animationSpec = motion.flickTween(150)) { fullWidth -> fullWidth }
                    } else {
                        fadeOut(motion.flickTween(220)) + slideOutHorizontally(animationSpec = motion.flickTween(220)) { fullWidth -> fullWidth }
                    }
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = rightEnter,
                    exit = rightExit
                ) {
                    Surface(
                        modifier = Modifier
                            .width(150.dp)
                            .wrapContentHeight()
                            .offset(y = rightPopupYOffset.dp)
                            .clickable(enabled = false) {},
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 28.dp,
                            bottomStart = 28.dp
                        ),
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
                            .copy(alpha = popupOpacity)
                    ) {
                        FolderAwareGrid(
                            bookmarks = bookmarks,
                            activeFolderId = activeFolderId,
                            onActiveFolderChange = { activeFolderId = it },
                            showLabels = showLabels,
                            columns = 2,
                            applyNavigationBarPadding = false,
                            contentVisible = contentVisible,
                            iconSpacing = iconSpacing,
                            showIconBorder = showIconBorder,
                            slideAnimation = effectiveRightSlideIn,
                            bounceEnabled = rightBounce,
                            iconSlideDirection = rightIconSlideDirection,
                            availability = availability,
                            onBookmarkClick = onBookmarkClick,
                            onReorder = onReorder,
                            onMergeIntoFolder = onMergeIntoFolder
                        )
                    }
                }
            } else {
                val bottomEnter = remember(motion, effectiveBottomSlideUp, bottomBounce) {
                    if (effectiveBottomSlideUp) {
                        val slideSpring = if (bottomBounce) {
                            motion.flickSpring<IntOffset>(dampingRatio = 0.18f, stiffness = 350f)
                        } else {
                            motion.flickSpring<IntOffset>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                        }
                        fadeIn(motion.flickTween(220)) + slideInVertically(animationSpec = slideSpring) { fullHeight -> fullHeight }
                    } else {
                        val springSpec = if (bottomBounce) {
                            motion.flickSpring<Float>(dampingRatio = 0.18f, stiffness = 350f)
                        } else {
                            motion.flickSpring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                        }
                        fadeIn(motion.flickTween(220)) + scaleIn(
                            animationSpec = springSpec,
                            initialScale = 0.85f
                        )
                    }
                }
                val bottomExit = remember(motion, effectiveBottomSlideUp) {
                    if (effectiveBottomSlideUp) {
                        fadeOut(motion.flickTween(150)) + slideOutVertically(animationSpec = motion.flickTween(150)) { fullHeight -> fullHeight }
                    } else {
                        fadeOut(motion.flickTween(150)) + scaleOut(animationSpec = motion.flickTween(150), targetScale = 0.92f)
                    }
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = bottomEnter,
                    exit = bottomExit
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clickable(enabled = false) {},
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 28.dp,
                            topEnd = 28.dp
                        ),
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
                            .copy(alpha = popupOpacity)
                    ) {
                        FolderAwareGrid(
                            bookmarks = bookmarks,
                            activeFolderId = activeFolderId,
                            onActiveFolderChange = { activeFolderId = it },
                            showLabels = showLabels,
                            contentVisible = contentVisible,
                            iconSpacing = iconSpacing,
                            showIconBorder = showIconBorder,
                            slideAnimation = effectiveBottomSlideUp,
                            bounceEnabled = bottomBounce,
                            iconSlideDirection = bottomIconSlideDirection,
                            availability = availability,
                            onBookmarkClick = onBookmarkClick,
                            onReorder = onReorder,
                            onMergeIntoFolder = onMergeIntoFolder
                        )
                    }
                }
            }
        }
    }
}

/**
 * Wraps [BookmarkGrid] with folder navigation: shows only the items belonging to the current
 * level (top-level when [activeFolderId] is null, otherwise that folder's children), a back
 * header when inside a folder, and crossfades between levels via [AnimatedContent]. Tapping a
 * folder tile is intercepted here and never reaches [onBookmarkClick].
 */
@Composable
private fun FolderAwareGrid(
    bookmarks: List<OverlayBookmarkItem>,
    activeFolderId: Long?,
    onActiveFolderChange: (Long?) -> Unit,
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
    onReorder: (List<Bookmark>) -> Unit,
    onMergeIntoFolder: (Bookmark, Bookmark) -> Unit
) {
    val motion = LocalMotion.current
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
        AnimatedContent(
            targetState = activeFolderId,
            transitionSpec = {
                // Zoom in when entering a folder, zoom out when backing out of one, so the
                // transition reads as the folder expanding in place rather than a flat crossfade.
                if (targetState != null) {
                    (fadeIn(motion.flickTween(220)) + scaleIn(
                        initialScale = 0.85f,
                        animationSpec = motion.flickTween(220)
                    )) togetherWith fadeOut(motion.flickTween(120))
                } else {
                    fadeIn(motion.flickTween(180)) togetherWith
                        (fadeOut(motion.flickTween(180)) + scaleOut(
                            targetScale = 0.85f,
                            animationSpec = motion.flickTween(180)
                        ))
                }
            },
            label = "folderContentSwap"
        ) { _ ->
            BookmarkGrid(
                items = displayedItems,
                showLabels = showLabels,
                columns = columns,
                applyNavigationBarPadding = applyNavigationBarPadding,
                contentVisible = contentVisible,
                iconSpacing = iconSpacing,
                showIconBorder = showIconBorder,
                slideAnimation = slideAnimation,
                bounceEnabled = bounceEnabled,
                iconSlideDirection = iconSlideDirection,
                availability = availability,
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
