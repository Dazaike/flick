package com.flick.data.model

data class Bookmark(
    val id: Long = 0,
    val categoryId: Long,
    val label: String,
    val sortOrder: Int,
    val action: BookmarkAction,
    val customIconUri: String? = null,
    val iconPackPackage: String? = null,
    val showLabel: Boolean = true,
    /** Non-null when this bookmark lives inside a folder bookmark with this id. */
    val parentFolderId: Long? = null
)
