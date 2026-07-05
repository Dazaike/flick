package com.flick.ui.screens.addbookmark

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flick.data.model.Bookmark
import com.flick.data.model.BookmarkAction
import com.flick.data.repository.BookmarkRepository
import com.flick.iconpack.IconResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Immutable
data class FolderMemberOption(
    val bookmark: Bookmark,
    val icon: Bitmap?
)

@HiltViewModel
class FolderCreateViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val iconResolver: IconResolver,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _options = MutableStateFlow<List<FolderMemberOption>>(emptyList())
    val options: StateFlow<List<FolderMemberOption>> = _options.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _folderName = MutableStateFlow("")
    val folderName: StateFlow<String> = _folderName.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    fun load(categoryId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            // Top-level, non-folder bookmarks only — no nested folders in v1.
            val candidates = bookmarkRepository.observeByCategory(categoryId).first()
                .filter { it.action !is BookmarkAction.Folder }
            val icons = withContext(Dispatchers.IO) { iconResolver.resolveBitmaps(context, candidates) }
            _options.value = candidates.map { FolderMemberOption(it, icons[it.id]) }
            _isLoading.value = false
        }
    }

    fun onNameChange(name: String) {
        _folderName.value = name
    }

    fun toggleSelected(id: Long) {
        _selectedIds.value = _selectedIds.value.let { current ->
            if (id in current) current - id else current + id
        }
    }

    fun createFolder(categoryId: Long, sortOrder: Int, onDone: () -> Unit) {
        val selected = _selectedIds.value
        if (selected.size < 2) return
        val name = _folderName.value.ifBlank { "Folder" }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                bookmarkRepository.createFolder(categoryId, name, selected.toList(), sortOrder)
            }
            onDone()
        }
    }
}
