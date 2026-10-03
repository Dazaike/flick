package com.flick.ui.screens.bookmarklist

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flick.data.model.Bookmark
import com.flick.ui.prism.ButtonSize
import com.flick.ui.prism.ButtonVariant
import com.flick.ui.prism.GlassButton
import com.flick.ui.prism.GlassDialog
import com.flick.ui.prism.GlassSwitch
import com.flick.ui.prism.PrismIcon
import com.flick.ui.prism.PrismIcons
import com.flick.ui.theme.DURATION_MEDIUM
import com.flick.ui.theme.DURATION_QUICK
import com.flick.ui.theme.LocalMotion
import com.flick.ui.theme.PrismText

@Composable
fun EditBookmarkDialog(
    bookmark: Bookmark,
    onDismiss: () -> Unit,
    onSave: (Bookmark) -> Unit
) {
    val context = LocalContext.current
    val motion = LocalMotion.current
    var customIconUri by remember { mutableStateOf(bookmark.customIconUri) }
    var showLabel by remember { mutableStateOf(bookmark.showLabel) }

    val pickIconLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            customIconUri = uri.toString()
        }
    }

    GlassDialog(
        visible = true,
        title = "Edit ${bookmark.label}",
        onDismiss = onDismiss,
        confirmLabel = "Save",
        onConfirm = {
            onSave(bookmark.copy(customIconUri = customIconUri, showLabel = showLabel))
        }
    ) { cardBackdrop ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            val iconBitmap = remember(customIconUri) {
                customIconUri?.let { uriString ->
                    runCatching {
                        context.contentResolver.openInputStream(Uri.parse(uriString))?.use {
                            android.graphics.BitmapFactory.decodeStream(it)
                        }
                    }.getOrNull()
                }
            }
            AnimatedContent(
                targetState = iconBitmap,
                transitionSpec = {
                    (fadeIn(motion.fade(DURATION_MEDIUM)) + scaleIn(motion.glide(), initialScale = 0.6f)) togetherWith
                        (fadeOut(motion.fade(DURATION_QUICK)) + scaleOut(motion.fade(DURATION_QUICK), targetScale = 0.6f))
                },
                label = "bookmarkIconCrossfade"
            ) { bitmap ->
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    PrismIcon(PrismIcons.App, contentDescription = null, size = 48.dp)
                }
            }
            Spacer(modifier = Modifier.size(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(
                    backdrop = cardBackdrop,
                    text = "Choose custom icon",
                    onClick = { pickIconLauncher.launch(arrayOf("image/*")) },
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Small
                )
                AnimatedVisibility(
                    visible = customIconUri != null,
                    enter = fadeIn(motion.fade(DURATION_MEDIUM)) + expandHorizontally(motion.glide()),
                    exit = fadeOut(motion.fade(DURATION_QUICK)) + shrinkHorizontally(motion.glide())
                ) {
                    GlassButton(
                        backdrop = cardBackdrop,
                        text = "Remove custom icon",
                        onClick = { customIconUri = null },
                        variant = ButtonVariant.Ghost,
                        size = ButtonSize.Small
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PrismText("Show label under icon")
            GlassSwitch(checked = showLabel, onCheckedChange = { showLabel = it })
        }
    }
}
