package com.flick.data.repository

import com.flick.data.db.BookmarkEntity
import com.flick.data.db.CategoryEntity
import com.flick.data.model.Bookmark
import com.flick.data.model.BookmarkActionType
import com.flick.data.model.Category
import com.flick.data.model.actionType
import com.flick.data.model.decodeBookmarkAction
import com.flick.data.model.toPayloadJson

fun BookmarkEntity.toDomain(): Bookmark = Bookmark(
    id = id,
    categoryId = categoryId,
    label = label,
    sortOrder = sortOrder,
    action = decodeBookmarkAction(BookmarkActionType.valueOf(actionType), actionPayloadJson),
    customIconUri = customIconUri,
    iconPackPackage = iconPackPackage,
    showLabel = showLabel,
    parentFolderId = parentFolderId
)

fun Bookmark.toEntity(): BookmarkEntity = BookmarkEntity(
    id = id,
    categoryId = categoryId,
    label = label,
    sortOrder = sortOrder,
    actionType = action.actionType().name,
    actionPayloadJson = action.toPayloadJson(),
    customIconUri = customIconUri,
    iconPackPackage = iconPackPackage,
    showLabel = showLabel,
    parentFolderId = parentFolderId
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    sortOrder = sortOrder,
    colorOrIconRes = colorOrIconRes
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    sortOrder = sortOrder,
    colorOrIconRes = colorOrIconRes
)
