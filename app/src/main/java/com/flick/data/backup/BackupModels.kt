package com.flick.data.backup

import kotlinx.serialization.Serializable

const val BACKUP_FORMAT_VERSION = 1

@Serializable
data class FlickBackupFile(
    val formatVersion: Int = BACKUP_FORMAT_VERSION,
    val exportedAtEpochMs: Long,
    val appVersionName: String? = null,
    val categories: List<BackupCategory> = emptyList(),
    val bookmarks: List<BackupBookmark> = emptyList(),
    val preferences: BackupPreferences = BackupPreferences()
)

@Serializable
data class BackupCategory(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val colorOrIconRes: Int? = null
)

@Serializable
data class BackupBookmark(
    val id: Long,
    val categoryId: Long,
    val label: String,
    val sortOrder: Int,
    val actionType: String,
    val actionPayloadJson: String,
    val customIconUri: String? = null,
    /** Relative path inside the backup zip, e.g. `icons/icon_12.png`. */
    val customIconFile: String? = null,
    val iconPackPackage: String? = null,
    val showLabel: Boolean = true,
    val createdAt: Long = 0L,
    val parentFolderId: Long? = null
)

@Serializable
data class BackupPreferences(
    val overlay: BackupOverlayPrefs = BackupOverlayPrefs(),
    val theme: BackupThemePrefs = BackupThemePrefs(),
    val iconPack: BackupIconPackPrefs = BackupIconPackPrefs()
)

@Serializable
data class BackupOverlayPrefs(
    val showAppNames: Boolean = true,
    val blurIntensity: Float = 0f,
    val popupOpacity: Float = 0.92f,
    val rightPopup: Boolean = false,
    val iconSpacing: Float = 6f,
    val showIconBorder: Boolean = false,
    val slideAnimation: Boolean = false,
    val bottomBounce: Boolean = false,
    val bottomSlideUp: Boolean = false,
    val rightBounce: Boolean = true,
    val rightSlideIn: Boolean = false,
    val rightPopupYOffset: Float = 0f,
    val panelAnimationSpeed: Float = 1f,
    val iconAnimationSpeed: Float = 1f,
    val panelScale: Float = 1f
)

@Serializable
data class BackupThemePrefs(
    val amoledMode: Boolean = false,
    val gridView: Boolean = false,
    val colorMode: String = "DYNAMIC",
    val animationsEnabled: Boolean = true,
    val animationIntensity: Float = 1f
)

@Serializable
data class BackupIconPackPrefs(
    val activePackPackage: String? = null
)

data class BackupResult(
    val categoryCount: Int,
    val bookmarkCount: Int,
    val iconCount: Int
)
