package com.flick.ui.screens.addbookmark

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.flick.data.model.BookmarkAction
import com.flick.ui.prism.ButtonVariant
import com.flick.ui.prism.GlassButton
import com.flick.ui.prism.GlassTextField
import com.flick.ui.prism.PrismScreen

enum class PhoneEntryMode { DIAL, DIRECT_CALL, SEND_SMS }

@Composable
fun PhoneNumberEntryScreen(
    categoryId: Long,
    mode: PhoneEntryMode,
    onAdded: () -> Unit,
    viewModel: PhoneNumberEntryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var label by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(mode) {
        if (mode == PhoneEntryMode.DIRECT_CALL &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    val title = when (mode) {
        PhoneEntryMode.DIAL -> "Dial a number"
        PhoneEntryMode.DIRECT_CALL -> "Call a number directly"
        PhoneEntryMode.SEND_SMS -> "Text a number"
    }

    PrismScreen(title = title) { padding, contentBackdrop ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassTextField(
                value = label,
                onValueChange = { label = it },
                label = "Label",
                modifier = Modifier.fillMaxWidth()
            )
            GlassTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = "Phone number",
                modifier = Modifier.fillMaxWidth()
            )
            if (mode == PhoneEntryMode.SEND_SMS) {
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
                    val action = when (mode) {
                        PhoneEntryMode.DIAL -> BookmarkAction.DialNumber(phoneNumber)
                        PhoneEntryMode.DIRECT_CALL -> BookmarkAction.DirectCall(phoneNumber)
                        PhoneEntryMode.SEND_SMS -> BookmarkAction.SendSms(phoneNumber, body)
                    }
                    viewModel.addBookmark(categoryId, label.ifBlank { phoneNumber }, action, onAdded)
                },
                variant = ButtonVariant.Primary,
                enabled = phoneNumber.isNotBlank()
            )
        }
    }
}
