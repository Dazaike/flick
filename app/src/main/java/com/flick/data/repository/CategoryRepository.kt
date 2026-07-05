package com.flick.data.repository

import com.flick.data.db.CategoryDao
import com.flick.data.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun observeAll(): Flow<List<Category>> =
        categoryDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .distinctUntilChanged()

    suspend fun getById(id: Long): Category? = categoryDao.getById(id)?.toDomain()

    suspend fun upsert(category: Category): Long = categoryDao.insert(category.toEntity())

    suspend fun delete(category: Category) = categoryDao.delete(category.toEntity())

    /** Returns the id of the first category, creating a "Default" one if none exist yet. */
    suspend fun ensureDefaultCategory(): Long {
        val existing = categoryDao.getFirst()
        if (existing != null) return existing.id
        return upsert(Category(name = "Default", sortOrder = 0))
    }
}
