package com.flick.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    indices = [
        Index(value = ["categoryId", "sortOrder"]),
        Index(value = ["parentFolderId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = BookmarkEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentFolderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val label: String,
    val sortOrder: Int,
    val actionType: String,
    val actionPayloadJson: String,
    val customIconUri: String? = null,
    val iconPackPackage: String? = null,
    val showLabel: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    /** Non-null when this bookmark lives inside a folder bookmark with this id. */
    val parentFolderId: Long? = null
)
