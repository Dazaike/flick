package com.flick.ui.screens.addbookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flick.data.model.Bookmark
import com.flick.data.model.BookmarkAction
import com.flick.data.model.SettingsPanelOption
import com.flick.data.repository.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsPanelPickerViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    fun addBookmark(categoryId: Long, option: SettingsPanelOption, onDone: () -> Unit) {
        viewModelScope.launch {
            bookmarkRepository.upsert(
                Bookmark(
                    categoryId = categoryId,
                    label = option.label,
                    sortOrder = 0,
                    action = BookmarkAction.SettingsPanel(panelAction = option.action)
                )
            )
            onDone()
        }
    }
}
