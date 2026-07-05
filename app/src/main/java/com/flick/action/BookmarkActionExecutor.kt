package com.flick.action

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Process
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.flick.data.model.BookmarkAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkActionExecutor @Inject constructor() {

    fun execute(context: Context, action: BookmarkAction): Boolean =
        try {
            when (action) {
                is BookmarkAction.LaunchApp -> executeLaunchApp(context, action)
                is BookmarkAction.WebUrl -> { executeWebUrl(context, action); true }
                is BookmarkAction.SettingsPanel -> { executeSettingsPanel(context, action); true }
                is BookmarkAction.AppShortcut -> executeAppShortcut(context, action)
                is BookmarkAction.LegacyShortcut -> { executeLegacyShortcut(context, action); true }
                is BookmarkAction.DialNumber -> { executeDial(context, action.phoneNumber); true }
                is BookmarkAction.DirectCall -> { executeCall(context, action.phoneNumber); true }
                is BookmarkAction.CallContact -> { executeCall(context, action.phoneNumber); true }
                is BookmarkAction.MessageContact -> { executeSms(context, action.phoneNumber, action.prefilledBody); true }
                is BookmarkAction.SendSms -> { executeSms(context, action.phoneNumber, action.body); true }
                is BookmarkAction.CustomIntent -> { executeCustomIntent(context, action); true }
                is BookmarkAction.AppWidget -> {
                    Toast.makeText(context, "Widget opens in the popup", Toast.LENGTH_SHORT).show()
                    false
                }
                is BookmarkAction.Folder -> {
                    // Folders are intercepted by the UI layer (expand in place) and should
                    // never reach the executor, but no-op safely just in case.
                    false
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Couldn't launch: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }

    private fun executeDial(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun executeCall(context: Context, phoneNumber: String) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Call permission not granted — opening dialer instead", Toast.LENGTH_SHORT).show()
            executeDial(context, phoneNumber)
        }
    }

    private fun executeSms(context: Context, phoneNumber: String, body: String?) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!body.isNullOrBlank()) intent.putExtra("sms_body", body)
        context.startActivity(intent)
    }

    private fun executeLaunchApp(context: Context, action: BookmarkAction.LaunchApp): Boolean {
        val intent = if (action.activityClassName != null) {
            Intent().setClassName(action.packageName, action.activityClassName)
        } else {
            context.packageManager.getLaunchIntentForPackage(action.packageName)
        }
        if (intent == null) {
            Toast.makeText(context, "App not found", Toast.LENGTH_SHORT).show()
            return false
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }

    private fun executeWebUrl(context: Context, action: BookmarkAction.WebUrl) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun executeSettingsPanel(context: Context, action: BookmarkAction.SettingsPanel) {
        val intent = Intent(action.panelAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun executeAppShortcut(context: Context, action: BookmarkAction.AppShortcut): Boolean =
        try {
            context.getSystemService(LauncherApps::class.java).startShortcut(
                action.packageName,
                action.shortcutId,
                null,
                null,
                Process.myUserHandle()
            )
            true
        } catch (e: SecurityException) {
            Toast.makeText(context, "Shortcut is no longer available", Toast.LENGTH_SHORT).show()
            false
        }

    private fun executeLegacyShortcut(context: Context, action: BookmarkAction.LegacyShortcut) {
        val intent = Intent.parseUri(action.intentUri, Intent.URI_INTENT_SCHEME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun executeCustomIntent(context: Context, action: BookmarkAction.CustomIntent) {
        val intent = Intent(action.action).addFlags(action.flags or Intent.FLAG_ACTIVITY_NEW_TASK)
        action.dataUri?.let { intent.data = Uri.parse(it) }
        action.mimeType?.let { intent.type = it }
        if (action.componentPackage != null && action.componentClass != null) {
            intent.setClassName(action.componentPackage, action.componentClass)
        } else if (action.componentPackage != null) {
            intent.setPackage(action.componentPackage)
        }
        action.categories.forEach { intent.addCategory(it) }
        action.extras.forEach { (key, value) -> intent.putExtra(key, value) }
        context.startActivity(intent)
    }
}
