package com.flick.trigger

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

object AssistantRoleHelper {
    fun isRoleAvailable(context: Context): Boolean {
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
        return roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)
    }

    fun isRoleHeld(context: Context): Boolean {
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
        return roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)
    }

    fun createRequestRoleIntent(context: Context): Intent {
        val roleManager = context.getSystemService(RoleManager::class.java)
            ?: return createAssistantSettingsIntent(context)
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT)
    }

    fun createAssistantSettingsIntent(context: Context): Intent {
        val candidates = listOf(
            Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS"),
            Intent(Settings.ACTION_SETTINGS)
        )
        return candidates.firstOrNull { it.resolveActivity(context.packageManager) != null }
            ?: Intent(Settings.ACTION_SETTINGS)
    }
}
