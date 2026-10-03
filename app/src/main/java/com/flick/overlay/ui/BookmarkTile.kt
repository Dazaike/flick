package com.flick.overlay.ui

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flick.data.model.BookmarkAction
import com.flick.ui.prism.PrismIcon
import com.flick.ui.prism.PrismIcons
import com.flick.ui.theme.DURATION_QUICK
import com.flick.ui.theme.LocalMotion
import com.flick.ui.theme.MotionSpec
import com.flick.ui.theme.Prism
import com.flick.ui.theme.PrismText
import com.kyant.shapes.RoundedRectangle

private fun tileSpring(motion: MotionSpec, damping: Float, stiffness: Float): FiniteAnimationSpec<Float> =
    if (motion.reduced) snap() else spring(dampingRatio = damping, stiffness = stiffness * motion.speed.coerceAtLeast(0.1f))

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
    clickEnabled: Boolean = true,
    animateEnter: Boolean = true,
    modifier: Modifier = Modifier
) {
    val unavailable = !isAvailable

    val motion = LocalMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scaleSpec = remember(motion) {
        tileSpring(motion, Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh)
    }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.82f else 1f,
        animationSpec = scaleSpec,
        label = "tileScale"
    )
    val glowSpec = remember(motion) { motion.fade<Float>(DURATION_QUICK) }
    val glowAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.35f else 0f,
        animationSpec = glowSpec,
        label = "tileGlow"
    )

    val delayMs = remember(index) { (index * 12).coerceAtMost(120) }
    val enterSpec = remember(motion, delayMs) { motion.fade<Float>(160, delayMs) }
    val enterProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = enterSpec,
        label = "tileEnter"
    )
    val enterScaleSpring = remember(motion, bounceEnabled) {
        if (bounceEnabled) tileSpring(motion, 0.25f, 500f)
        else tileSpring(motion, Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
    }
    val enterScale by animateFloatAsState(
        targetValue = if (visible) 1f else if (slideAnimation) 1f else 0.6f,
        animationSpec = if (slideAnimation) enterSpec else enterScaleSpring,
        label = "tileEnterScale"
    )
    val density = LocalDensity.current
    val slideOffsetPx = with(density) { 18.dp.toPx() }
    val enterAlpha = when {
        !animateEnter -> 1f
        motion.reduced -> 1f
        slideAnimation -> tileRevealAlpha(enterProgress, invisibleUntil = 0.82f)
        else -> tileRevealAlpha(enterProgress, invisibleUntil = 0.6f)
    }
    val travelProgress = if (animateEnter) easeOutCubic(enterProgress) else 1f
    val displayedEnterScale = if (animateEnter) enterScale else 1f

    Box(
        modifier = modifier
            .height(if (showLabel) 76.dp else 54.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics { contentDescription = item.bookmark.label }
            .clip(RoundedRectangle(16.dp))
            .background(
                if (mergeHighlighted) {
                    Prism.accent.copy(alpha = 0.25f)
                } else if (showIconBorder) {
                    Prism.colors.fill
                } else {
                    androidx.compose.ui.graphics.Color.Transparent
                }
            )
            .clickable(
                enabled = clickEnabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 6.dp)
                .graphicsLayer {
                    clip = false
                    alpha = enterAlpha
                    if (!motion.reduced && animateEnter) {
                        when {
                            slideAnimation -> {
                                val offset = (1f - travelProgress) * slideOffsetPx
                                translationX = offset
                            }
                            else -> {
                                scaleX = displayedEnterScale
                                scaleY = displayedEnterScale
                                translationY = (1f - travelProgress) * slideOffsetPx / 3f
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (unavailable) {
                    PrismIcon(
                        PrismIcons.Alert,
                        contentDescription = null,
                        size = 32.dp,
                        modifier = Modifier
                            .size(42.dp)
                            .background(Prism.accent.copy(alpha = glowAlpha), CircleShape)
                            .padding(5.dp)
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
                            .background(Prism.accent.copy(alpha = glowAlpha), CircleShape)
                    )
                } else {
                    val fallbackIcon = when (item.bookmark.action) {
                        is BookmarkAction.WebUrl -> PrismIcons.Globe
                        is BookmarkAction.SettingsPanel -> PrismIcons.Settings
                        is BookmarkAction.DialNumber -> PrismIcons.Dialpad
                        is BookmarkAction.DirectCall, is BookmarkAction.CallContact -> PrismIcons.Call
                        is BookmarkAction.SendSms, is BookmarkAction.MessageContact -> PrismIcons.Sms
                        else -> PrismIcons.App
                    }
                    PrismIcon(
                        fallbackIcon,
                        contentDescription = null,
                        size = 32.dp,
                        modifier = Modifier
                            .size(42.dp)
                            .background(Prism.accent.copy(alpha = glowAlpha), CircleShape)
                            .padding(5.dp)
                    )
                }
                if (showLabel) {
                    Spacer(modifier = Modifier.height(3.dp))
                    PrismText(
                        text = item.bookmark.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
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
            .background(Prism.accent.copy(alpha = glowAlpha), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (resolvedIcons.isEmpty()) {
            PrismIcon(PrismIcons.Folder, contentDescription = null, size = 32.dp)
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedRectangle(10.dp))
                    .background(Prism.colors.fillStrong)
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
