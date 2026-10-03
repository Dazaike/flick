package com.flick.ui.screens.addbookmark

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flick.data.model.BookmarkAction
import com.flick.ui.prism.ButtonVariant
import com.flick.ui.prism.GlassButton
import com.flick.ui.prism.GlassTextField
import com.flick.ui.prism.PrismScreen
import com.flick.ui.theme.DURATION_MEDIUM
import com.flick.ui.theme.DURATION_QUICK
import com.flick.ui.theme.LocalMotion
import com.flick.ui.theme.PrismText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ContactPickerMode { CALL, MESSAGE }

private data class PickedContact(val lookupKey: String?, val phoneNumber: String, val displayName: String)

@Composable
fun ContactPickerScreen(
    categoryId: Long,
    mode: ContactPickerMode,
    onAdded: () -> Unit,
    viewModel: PhoneNumberEntryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var picked by remember { mutableStateOf<PickedContact?>(null) }
    var body by remember { mutableStateOf("") }

    val pickContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                scope.launch {
                    val contact = withContext(Dispatchers.IO) {
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val number = cursor.getString(
                                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                )
                                val name = cursor.getString(
                                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                                ) ?: number
                                val lookupKey = runCatching {
                                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY))
                                }.getOrNull()
                                PickedContact(lookupKey, number, name)
                            } else {
                                null
                            }
                        }
                    }
                    if (contact != null) {
                        picked = contact
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        pickContactLauncher.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
    }

    val title = if (mode == ContactPickerMode.CALL) "Call a contact" else "Message a contact"
    val motion = LocalMotion.current

    PrismScreen(title = title) { padding, contentBackdrop ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            AnimatedContent(
                targetState = picked,
                transitionSpec = {
                    (fadeIn(motion.fade(DURATION_MEDIUM)) + slideInVertically(motion.glide()) { it / 4 }) togetherWith
                        fadeOut(motion.fade(DURATION_QUICK))
                },
                label = "contactPickerCrossfade"
            ) { contact ->
                if (contact == null) {
                    PrismText("Pick a contact to continue")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrismText("Selected: ${contact.displayName} (${contact.phoneNumber})")
                        AnimatedVisibility(
                            visible = mode == ContactPickerMode.MESSAGE,
                            enter = fadeIn(motion.fade(DURATION_MEDIUM)) + expandVertically(motion.glide()),
                            exit = fadeOut(motion.fade(DURATION_QUICK)) + shrinkVertically(motion.glide())
                        ) {
                            GlassTextField(
                                value = body,
                                onValueChange = { body = it },
                                label = "Message (optional)",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        GlassButton(
                            backdrop = contentBackdrop,
                            text = "Save",
                            onClick = {
                                val action = if (mode == ContactPickerMode.CALL) {
                                    BookmarkAction.CallContact(contact.lookupKey ?: "", contact.phoneNumber, contact.displayName)
                                } else {
                                    BookmarkAction.MessageContact(contact.lookupKey, contact.phoneNumber, contact.displayName, body.ifBlank { null })
                                }
                                viewModel.addBookmark(categoryId, contact.displayName, action, onAdded)
                            },
                            variant = ButtonVariant.Primary
                        )
                    }
                }
            }
        }
    }
}
