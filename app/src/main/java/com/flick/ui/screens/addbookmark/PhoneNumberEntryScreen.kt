package com.flick.ui.screens.addbookmark

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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

    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label") })
            OutlinedTextField(value = phoneNumber, onValueChange = { phoneNumber = it }, label = { Text("Phone number") })
            if (mode == PhoneEntryMode.SEND_SMS) {
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Message (optional)") })
            }
            Button(
                onClick = {
                    val action = when (mode) {
                        PhoneEntryMode.DIAL -> BookmarkAction.DialNumber(phoneNumber)
                        PhoneEntryMode.DIRECT_CALL -> BookmarkAction.DirectCall(phoneNumber)
                        PhoneEntryMode.SEND_SMS -> BookmarkAction.SendSms(phoneNumber, body)
                    }
                    viewModel.addBookmark(categoryId, label.ifBlank { phoneNumber }, action, onAdded)
                },
                enabled = phoneNumber.isNotBlank()
            ) {
                Text("Save")
            }
        }
    }
}
