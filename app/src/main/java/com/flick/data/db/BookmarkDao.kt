package com.flick.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    /**
     * Returns every bookmark, including folder children. Used by the overlay service, which
     * needs full data preloaded so opening a folder doesn't require another DB round trip.
     */
    @Query("SELECT * FROM bookmarks ORDER BY categoryId, sortOrder")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY categoryId, sortOrder")
    suspend fun getAll(): List<BookmarkEntity>

    /** Top-level bookmarks only (folder children are hidden until their folder is expanded). */
    @Query("SELECT * FROM bookmarks WHERE categoryId = :categoryId AND parentFolderId IS NULL ORDER BY sortOrder")
    fun observeByCategory(categoryId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE parentFolderId = :folderId ORDER BY sortOrder")
    fun observeChildren(folderId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE parentFolderId = :folderId ORDER BY sortOrder")
    suspend fun getChildren(folderId: Long): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    suspend fun getById(id: Long): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Update
    suspend fun update(bookmark: BookmarkEntity)

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)

    @Query("UPDATE bookmarks SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Transaction
    suspend fun updateSortOrders(ids: List<Long>) {
        ids.forEachIndexed { index, id -> updateSortOrder(id, index) }
    }

    @Query("UPDATE bookmarks SET parentFolderId = :folderId, sortOrder = :sortOrder WHERE id = :id")
    suspend fun setParentFolder(id: Long, folderId: Long?, sortOrder: Int)

    @Transaction
    suspend fun setParentFolders(ids: List<Long>, folderId: Long?, startSortOrder: Int = 0) {
        ids.forEachIndexed { index, id -> setParentFolder(id, folderId, startSortOrder + index) }
    }

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM bookmarks WHERE categoryId = :categoryId AND parentFolderId IS NULL")
    suspend fun nextTopLevelSortOrder(categoryId: Long): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM bookmarks WHERE parentFolderId = :folderId")
    suspend fun nextChildSortOrder(folderId: Long): Int

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()
}
