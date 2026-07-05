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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.flick.ui.theme.DURATION_MEDIUM
import com.flick.ui.theme.LocalMotion
import com.flick.ui.theme.flickSpring
import com.flick.ui.theme.flickTween
import androidx.core.graphics.drawable.toBitmap

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${bookmark.label}") },
        text = {
            Column {
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
                            (fadeIn(motion.flickTween(DURATION_MEDIUM)) + scaleIn(motion.flickSpring(), initialScale = 0.6f)) togetherWith
                                (fadeOut(motion.flickTween(120)) + scaleOut(motion.flickTween(120), targetScale = 0.6f))
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
                            Icon(Icons.Filled.Android, contentDescription = null, modifier = Modifier.size(48.dp))
                        }
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(16.dp))
                    Column {
                        TextButton(onClick = { pickIconLauncher.launch(arrayOf("image/*")) }) {
                            Text("Choose custom icon")
                        }
                        AnimatedVisibility(
                            visible = customIconUri != null,
                            enter = fadeIn(motion.flickTween(DURATION_MEDIUM)) + expandHorizontally(motion.flickSpring()),
                            exit = fadeOut(motion.flickTween(120)) + shrinkHorizontally(motion.flickSpring())
                        ) {
                            TextButton(onClick = { customIconUri = null }) {
                                Text("Remove custom icon")
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show label under icon")
                    Switch(checked = showLabel, onCheckedChange = { showLabel = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(bookmark.copy(customIconUri = customIconUri, showLabel = showLabel))
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
