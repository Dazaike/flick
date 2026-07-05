package com.flick.ui.screens.addbookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flick.data.model.Bookmark
import com.flick.data.model.BookmarkAction
import com.flick.data.repository.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhoneNumberEntryViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    fun addBookmark(categoryId: Long, label: String, action: BookmarkAction, onDone: () -> Unit) {
        viewModelScope.launch {
            bookmarkRepository.upsert(
                Bookmark(categoryId = categoryId, label = label, sortOrder = 0, action = action)
            )
            onDone()
        }
    }
}
