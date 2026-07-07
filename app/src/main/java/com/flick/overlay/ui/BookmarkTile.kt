package com.flick.overlay.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flick.data.model.Bookmark
import com.flick.data.model.BookmarkAction
import com.flick.ui.theme.DURATION_QUICK
import com.flick.ui.theme.LocalMotion
import com.flick.ui.theme.flickSpring
import com.flick.ui.theme.flickTween

@Composable
fun BookmarkTile(
    item: OverlayBookmarkItem,
    showLabel: Boolean,
    onClick: () -> Unit,
    visible: Boolean = true,
    index: Int = 0,
    isAvailable: Boolean = true,
    showIconBorder: Boolean = false,
    slideAnimation: Boolean = false,
    bounceEnabled: Boolean = false,
    mergeHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val unavailable = !isAvailable

    val motion = LocalMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scaleSpec = remember(motion) {
        motion.flickSpring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
    }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.82f else 1f,
        animationSpec = scaleSpec,
        label = "tileScale"
    )
    val glowSpec = remember(motion) { motion.flickTween<Float>(DURATION_QUICK) }
    val glowAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.35f else 0f,
        animationSpec = glowSpec,
        label = "tileGlow"
    )

    val delayMs = remember(index) { (index * 12).coerceAtMost(120) }
    val enterSpec = remember(motion, delayMs) { motion.flickTween<Float>(160, delayMs) }
    val enterProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = enterSpec,
        label = "tileEnter"
    )
    val enterScaleSpring = remember(motion, bounceEnabled) {
        if (bounceEnabled) {
            motion.flickSpring<Float>(dampingRatio = 0.25f, stiffness = 500f)
        } else {
            motion.flickSpring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        }
    }
    val enterScale by animateFloatAsState(
        targetValue = if (visible) 1f else if (slideAnimation) 1f else 0.6f,
        animationSpec = if (slideAnimation) enterSpec else enterScaleSpring,
        label = "tileEnterScale"
    )
    val density = LocalDensity.current
    val slideOffsetPx = with(density) { 18.dp.toPx() }
    val enterAlpha = when {
        !motion.enabled -> 1f
        slideAnimation -> tileRevealAlpha(enterProgress, invisibleUntil = 0.82f)
        else -> tileRevealAlpha(enterProgress, invisibleUntil = 0.6f)
    }
    val travelProgress = easeOutCubic(enterProgress)

    Surface(
        modifier = modifier
            .height(if (showLabel) 76.dp else 54.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics { contentDescription = item.bookmark.label }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (mergeHighlighted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        } else if (showIconBorder) {
            MaterialTheme.colorScheme.surface
        } else {
            androidx.compose.ui.graphics.Color.Transparent
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 6.dp)
                .graphicsLayer {
                    clip = false
                    alpha = enterAlpha
                    if (motion.enabled) {
                        when {
                            slideAnimation -> {
                                val offset = (1f - travelProgress) * slideOffsetPx
                                translationX = offset
                            }
                            else -> {
                                scaleX = enterScale
                                scaleY = enterScale
                                translationY = (1f - travelProgress) * slideOffsetPx / 3f
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (unavailable) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha), CircleShape)
                    )
                } else if (item.bookmark.action is BookmarkAction.Folder) {
                    FolderPreviewIcon(
                        childPreview = item.childPreview,
                        glowAlpha = glowAlpha
                    )
                } else if (item.icon != null) {
                    val imageBitmap = remember(item.icon) { item.icon.asImageBitmap() }
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha), CircleShape)
                    )
                } else {
                    val fallbackIcon = when (item.bookmark.action) {
                        is BookmarkAction.WebUrl -> Icons.Filled.Public
                        is BookmarkAction.SettingsPanel -> Icons.Filled.Settings
                        is BookmarkAction.DialNumber -> Icons.Filled.Dialpad
                        is BookmarkAction.DirectCall, is BookmarkAction.CallContact -> Icons.Filled.Call
                        is BookmarkAction.SendSms, is BookmarkAction.MessageContact -> Icons.Filled.Sms
                        else -> Icons.Filled.Android
                    }
                    Icon(
                        fallbackIcon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha), CircleShape)
                    )
                }
                if (showLabel) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.bookmark.label,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Renders a rounded-square tile containing a 2x2 mini-grid of up to 4 child icons, falling back
 * to a folder glyph when there are no children (or their icons haven't resolved yet).
 */
@Composable
private fun FolderPreviewIcon(childPreview: List<android.graphics.Bitmap?>, glowAlpha: Float) {
    val resolvedIcons = remember(childPreview) { childPreview.filterNotNull() }
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (resolvedIcons.isEmpty()) {
            Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(32.dp))
        } else {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(36.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        FolderPreviewCell(resolvedIcons.getOrNull(0), Modifier.weight(1f))
                        Spacer(modifier = Modifier.size(2.dp))
                        FolderPreviewCell(resolvedIcons.getOrNull(1), Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.size(2.dp))
                    Row(modifier = Modifier.fillMaxSize()) {
                        FolderPreviewCell(resolvedIcons.getOrNull(2), Modifier.weight(1f))
                        Spacer(modifier = Modifier.size(2.dp))
                        FolderPreviewCell(resolvedIcons.getOrNull(3), Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun easeOutCubic(progress: Float): Float {
    val remaining = 1f - progress
    return 1f - remaining * remaining * remaining
}

private fun tileRevealAlpha(progress: Float, invisibleUntil: Float): Float {
    if (progress <= invisibleUntil) return 0f
    val t = ((progress - invisibleUntil) / (1f - invisibleUntil)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

@Composable
private fun FolderPreviewCell(icon: android.graphics.Bitmap?, modifier: Modifier = Modifier) {
    if (icon != null) {
        val imageBitmap = remember(icon) { icon.asImageBitmap() }
        Image(bitmap = imageBitmap, contentDescription = null, modifier = modifier.fillMaxSize())
    } else {
        Spacer(modifier = modifier.fillMaxSize())
    }
}

@Preview(showBackground = true)
@Composable
private fun BookmarkTilePreview() {
    BookmarkTile(
        item = OverlayBookmarkItem(
            bookmark = Bookmark(
                id = 1,
                categoryId = 1,
                label = "Example",
                sortOrder = 0,
                action = BookmarkAction.WebUrl("https://example.com")
            ),
            icon = null
        ),
        showLabel = true,
        onClick = {}
    )
}
