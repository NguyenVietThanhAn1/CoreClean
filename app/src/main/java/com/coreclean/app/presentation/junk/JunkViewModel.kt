package com.coreclean.app.presentation.junk

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coreclean.app.core.preferences.AppPreferenceKeys
import com.coreclean.app.domain.CrashReporter
import com.coreclean.app.domain.model.JunkCategory
import com.coreclean.app.domain.model.JunkItem
import com.coreclean.app.domain.usecase.junk.CleanJunkUseCase
import com.coreclean.app.domain.usecase.junk.CleanResult
import com.coreclean.app.domain.usecase.junk.ScanJunkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface JunkUiState {
    data object Idle : JunkUiState
    data object Scanning : JunkUiState
    data class Ready(
        val items: List<JunkItem>,
        val selected: Set<String>,
        val cleaning: Boolean = false,
        val lastResult: CleanResult? = null
    ) : JunkUiState
    data class Error(val message: String) : JunkUiState
}

@HiltViewModel
class JunkViewModel @Inject constructor(
    private val scanJunk: ScanJunkUseCase,
    private val cleanJunk: CleanJunkUseCase,
    private val dataStore: DataStore<Preferences>,
    private val crashReporter: CrashReporter
) : ViewModel() {

    private val _uiState = MutableStateFlow<JunkUiState>(JunkUiState.Idle)
    val uiState: StateFlow<JunkUiState> = _uiState

    val safFolderUris: StateFlow<Set<String>> = dataStore.data
        .map { prefs -> prefs[AppPreferenceKeys.SAF_FOLDER_URIS] ?: emptySet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun scan() = viewModelScope.launch {
        _uiState.value = JunkUiState.Scanning
        try {
            val items = scanJunk(safFolderUris.value)
            _uiState.value = JunkUiState.Ready(items, items.map { it.path }.toSet())
        } catch (e: Exception) {
            crashReporter.captureException(e)
            _uiState.value = JunkUiState.Error(e.message ?: "Scan failed")
        }
    }

    fun toggleItem(path: String) {
        val state = _uiState.value as? JunkUiState.Ready ?: return
        val selected = if (path in state.selected) state.selected - path
                       else state.selected + path
        _uiState.value = state.copy(selected = selected)
    }

    fun clean() = viewModelScope.launch {
        val state = _uiState.value as? JunkUiState.Ready ?: return@launch
        _uiState.value = state.copy(cleaning = true)
        val toClean   = state.items.filter { it.path in state.selected }
        val result    = cleanJunk(toClean)
        val remaining = state.items.filter { it.path !in state.selected || it.category == JunkCategory.APP_CACHE }
        _uiState.value = JunkUiState.Ready(
            items      = remaining,
            selected   = remaining.map { it.path }.toSet(),
            lastResult = result
        )
    }

    fun addSafFolder(uri: Uri) = viewModelScope.launch {
        dataStore.edit { prefs ->
            val current = prefs[AppPreferenceKeys.SAF_FOLDER_URIS] ?: emptySet()
            prefs[AppPreferenceKeys.SAF_FOLDER_URIS] = current + uri.toString()
        }
    }

    fun removeSafFolder(uriString: String) = viewModelScope.launch {
        dataStore.edit { prefs ->
            val current = prefs[AppPreferenceKeys.SAF_FOLDER_URIS] ?: emptySet()
            prefs[AppPreferenceKeys.SAF_FOLDER_URIS] = current - uriString
        }
    }
}
