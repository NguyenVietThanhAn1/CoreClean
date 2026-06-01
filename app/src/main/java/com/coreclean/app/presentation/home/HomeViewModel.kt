package com.coreclean.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coreclean.app.domain.model.CleaningSuggestion
import com.coreclean.app.domain.usecase.suggest.GenerateSuggestionsUseCase
import com.coreclean.app.domain.repository.MediaRepository
import com.coreclean.app.domain.repository.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val generateSuggestions: GenerateSuggestionsUseCase,
    private val mediaRepository: MediaRepository,
    private val storageRepository: StorageRepository,
) : ViewModel() {

    private val _suggestions = MutableStateFlow<List<CleaningSuggestion>>(emptyList())
    val suggestions: StateFlow<List<CleaningSuggestion>> = _suggestions.asStateFlow()

    init { loadSuggestions() }

    fun loadSuggestions() = viewModelScope.launch {
        runCatching {
            val images   = mediaRepository.getAllImages().first()
            val storage  = storageRepository.getStorageInfo()
            val dupes    = mediaRepository.findDuplicates(images)
            _suggestions.value = generateSuggestions.invoke(
                duplicateGroups = dupes,
                allImages       = images,
                storageInfo     = storage
            )
        }
    }
}
