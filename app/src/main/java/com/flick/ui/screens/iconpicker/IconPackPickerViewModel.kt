package com.flick.ui.screens.iconpicker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flick.iconpack.IconPackPreferences
import com.flick.iconpack.IconPackScanner
import com.flick.iconpack.model.IconPackInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class IconPackPickerViewModel @Inject constructor(
    application: Application,
    private val iconPackScanner: IconPackScanner,
    private val iconPackPreferences: IconPackPreferences
) : AndroidViewModel(application) {

    private val _packs = MutableStateFlow<List<IconPackInfo>>(emptyList())
    val packs: StateFlow<List<IconPackInfo>> = _packs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val activePack: StateFlow<String?> = iconPackPreferences.activePackPackage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            _isLoading.value = true
            _packs.value = withContext(Dispatchers.IO) { iconPackScanner.scan(getApplication()) }
            _isLoading.value = false
        }
    }

    fun selectPack(packageName: String?) {
        viewModelScope.launch { iconPackPreferences.setActivePack(packageName) }
    }
}
