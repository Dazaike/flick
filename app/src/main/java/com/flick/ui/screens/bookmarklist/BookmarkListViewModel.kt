package com.flick.ui.screens.bookmarklist

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flick.data.model.Bookmark
import com.flick.data.repository.BookmarkRepository
import com.flick.data.repository.CategoryRepository
import com.flick.iconpack.IconResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookmarkListViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val categoryRepository: CategoryRepository,
    private val iconResolver: IconResolver,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private data class IconCacheKey(val id: Long, val customIconUri: String?, val iconPackPackage: String?)

    /** Guards [iconCache], which is now read/written both by the [icons] flow and folder rows. */
    private val iconCacheMutex = Mutex()
    private val iconCache = mutableMapOf<IconCacheKey, Bitmap?>()

    private val _defaultCategoryId = MutableStateFlow<Long?>(null)
    val defaultCategoryId: StateFlow<Long?> = _defaultCategoryId.asStateFlow()

    val bookmarks: StateFlow<List<Bookmark>> = _defaultCategoryId
        .filterNotNull()
        .flatMapLatest { categoryId -> bookmarkRepository.observeByCategory(categoryId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val icons: StateFlow<Map<Long, Bitmap?>> = bookmarks
        .map { resolveIcons(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            _defaultCategoryId.value = categoryRepository.ensureDefaultCategory()
        }
    }

    private suspend fun resolveIcons(bookmarks: List<Bookmark>): Map<Long, Bitmap?> = iconCacheMutex.withLock {
        val validKeys = bookmarks.mapTo(HashSet()) { it.toIconCacheKey() }
        iconCache.keys.retainAll(validKeys)

        val missing = bookmarks.filter { it.toIconCacheKey() !in iconCache }
        if (missing.isNotEmpty()) {
            val resolved = coroutineScope {
                missing.map { bookmark ->
                    async(Dispatchers.IO) { bookmark to iconResolver.resolveBitmap(context, bookmark) }
                }.awaitAll()
            }
            resolved.forEach { (bookmark, bitmap) -> iconCache[bookmark.toIconCacheKey()] = bitmap }
        }

        bookmarks.associate { it.id to iconCache[it.toIconCacheKey()] }
    }

    /** Resolves icons for a folder's children on demand when its row is expanded. */
    suspend fun resolveIconsFor(bookmarks: List<Bookmark>): Map<Long, Bitmap?> = resolveIcons(bookmarks)

    private fun Bookmark.toIconCacheKey() = IconCacheKey(id, customIconUri, iconPackPackage)

    /** Live children of a folder bookmark, used to render its expanded contents. */
    fun observeChildren(folderId: Long): Flow<List<Bookmark>> = bookmarkRepository.observeChildren(folderId)

    fun removeFromFolder(bookmark: Bookmark) {
        viewModelScope.launch {
            bookmarkRepository.removeFromFolder(bookmark)
        }
    }

    fun addToFolder(folderId: Long, ids: List<Long>) {
        viewModelScope.launch {
            bookmarkRepository.addToFolder(folderId, ids)
        }
    }

    fun mergeIntoFolder(draggedId: Long, targetId: Long) {
        viewModelScope.launch {
            bookmarkRepository.mergeIntoFolder(draggedId, targetId)
        }
    }

    fun delete(bookmark: Bookmark) {
        viewModelScope.launch {
            bookmarkRepository.delete(bookmark)
        }
    }

    fun update(bookmark: Bookmark) {
        viewModelScope.launch {
            bookmarkRepository.upsert(bookmark)
        }
    }

    fun updateAllSortOrders(list: List<Bookmark>) {
        viewModelScope.launch {
            bookmarkRepository.updateSortOrders(list.map { it.id })
        }
    }

    fun moveUp(bookmark: Bookmark) = move(bookmark, -1)

    fun moveDown(bookmark: Bookmark) = move(bookmark, 1)

    private fun move(bookmark: Bookmark, delta: Int) {
        viewModelScope.launch {
            val categoryItems = bookmarks.value
                .filter { it.categoryId == bookmark.categoryId }
                .sortedBy { it.sortOrder }
            val index = categoryItems.indexOfFirst { it.id == bookmark.id }
            val newIndex = index + delta
            if (index !in categoryItems.indices || newIndex !in categoryItems.indices) return@launch
            val reordered = categoryItems.toMutableList().apply { add(newIndex, removeAt(index)) }
            bookmarkRepository.updateSortOrders(reordered.map { it.id })
        }
    }
}
