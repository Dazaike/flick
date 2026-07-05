package com.flick.iconpack

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.flick.iconpack.model.IconPackInfo
import javax.inject.Inject
import javax.inject.Singleton

private val ICON_PACK_INTENT_ACTIONS = listOf(
    "com.novalauncher.THEME",
    "com.anddoes.launcher.THEME",
    "com.teslacoilsw.launcher.THEME",
    "com.fede.launcher.THEME_ICONPACK",
    "org.adw.launcher.THEMES",
    "org.adw.launcher.icons.ACTION_PICK_ICON"
)

@Singleton
class IconPackScanner @Inject constructor() {

    fun scan(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        return ICON_PACK_INTENT_ACTIONS
            .flatMap { action -> queryIntentActivitiesCompat(pm, Intent(action)) }
            .map { resolveInfo ->
                IconPackInfo(
                    packageName = resolveInfo.activityInfo.packageName,
                    label = resolveInfo.loadLabel(pm).toString()
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun queryIntentActivitiesCompat(pm: PackageManager, intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
}
