package com.flick.data.repository

import com.flick.data.db.BookmarkDao
import com.flick.data.model.Bookmark
import com.flick.data.model.BookmarkAction
import com.flick.data.model.BookmarkActionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepository @Inject constructor(
    private val bookmarkDao: BookmarkDao
) {
    fun observeAll(): Flow<List<Bookmark>> =
        bookmarkDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .distinctUntilChanged()

    fun observeByCategory(categoryId: Long): Flow<List<Bookmark>> =
        bookmarkDao.observeByCategory(categoryId)
            .map { entities -> entities.map { it.toDomain() } }
            .distinctUntilChanged()

    fun observeChildren(folderId: Long): Flow<List<Bookmark>> =
        bookmarkDao.observeChildren(folderId)
            .map { entities -> entities.map { it.toDomain() } }
            .distinctUntilChanged()

    suspend fun getById(id: Long): Bookmark? = bookmarkDao.getById(id)?.toDomain()

    /**
     * Inserts or replaces the bookmark. Preserves the original [BookmarkEntity.createdAt]
     * timestamp on updates since [OnConflictStrategy.REPLACE] would otherwise reset it.
     */
    suspend fun upsert(bookmark: Bookmark): Long {
        val existing = if (bookmark.id != 0L) bookmarkDao.getById(bookmark.id) else null
        val entity = bookmark.toEntity().let { entity ->
            if (existing != null) entity.copy(createdAt = existing.createdAt) else entity
        }
        return bookmarkDao.insert(entity)
    }

    /** Deletes the bookmark. If it's a folder, its children cascade-delete via the DB foreign key. */
    suspend fun delete(bookmark: Bookmark) = bookmarkDao.delete(bookmark.toEntity())

    suspend fun updateSortOrder(id: Long, sortOrder: Int) = bookmarkDao.updateSortOrder(id, sortOrder)

    suspend fun updateSortOrders(ids: List<Long>) = bookmarkDao.updateSortOrders(ids)

    /** Creates a new folder bookmark and moves the given existing bookmarks into it. */
    suspend fun createFolder(categoryId: Long, label: String, memberIds: List<Long>, sortOrder: Int): Long =
        createFolderAt(categoryId, label, parentFolderId = null, memberIds = memberIds, sortOrder = sortOrder)

    suspend fun createFolderAt(
        categoryId: Long,
        label: String,
        parentFolderId: Long? = null,
        memberIds: List<Long> = emptyList(),
        sortOrder: Int? = null
    ): Long {
        val resolvedSortOrder = sortOrder ?: if (parentFolderId == null) {
            bookmarkDao.nextTopLevelSortOrder(categoryId)
        } else {
            bookmarkDao.nextChildSortOrder(parentFolderId)
        }
        val folder = Bookmark(
            categoryId = categoryId,
            label = label,
            sortOrder = resolvedSortOrder,
            action = BookmarkAction.Folder,
            parentFolderId = parentFolderId
        )
        val folderId = bookmarkDao.insert(folder.toEntity())
        if (memberIds.isNotEmpty()) {
            bookmarkDao.setParentFolders(memberIds, folderId)
        }
        return folderId
    }

    /**
     * Drag-to-merge: if [targetId] is already a folder, moves [draggedId] into it. Otherwise
     * creates a new folder at the target's former position containing both bookmarks.
     */
    suspend fun mergeIntoFolder(draggedId: Long, targetId: Long): Long? {
        val target = bookmarkDao.getById(targetId) ?: return null
        if (draggedId == targetId) return null

        return if (BookmarkActionType.valueOf(target.actionType) == BookmarkActionType.FOLDER) {
            val nextOrder = bookmarkDao.nextChildSortOrder(target.id)
            bookmarkDao.setParentFolder(draggedId, target.id, nextOrder)
            target.id
        } else {
            val folderId = bookmarkDao.insert(newFolderEntity(target.categoryId, "Folder", target.sortOrder))
            bookmarkDao.setParentFolder(target.id, folderId, 0)
            bookmarkDao.setParentFolder(draggedId, folderId, 1)
            folderId
        }
    }

    suspend fun addToFolder(folderId: Long, ids: List<Long>) {
        val start = bookmarkDao.nextChildSortOrder(folderId)
        bookmarkDao.setParentFolders(ids, folderId, start)
    }

    suspend fun removeFromFolder(bookmark: Bookmark) {
        val nextOrder = bookmarkDao.nextTopLevelSortOrder(bookmark.categoryId)
        bookmarkDao.setParentFolder(bookmark.id, null, nextOrder)
    }

    private fun newFolderEntity(categoryId: Long, label: String, sortOrder: Int) =
        Bookmark(
            categoryId = categoryId,
            label = label,
            sortOrder = sortOrder,
            action = BookmarkAction.Folder
        ).toEntity()
}
