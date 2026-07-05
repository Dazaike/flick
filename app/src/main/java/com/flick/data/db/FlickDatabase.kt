package com.flick.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BookmarkEntity::class, CategoryEntity::class],
    version = 4,
    exportSchema = false
)
abstract class FlickDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun categoryDao(): CategoryDao
}
