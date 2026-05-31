package com.coreclean.app.ui.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coreclean.app.core.SelectedImagesHolder
import com.coreclean.app.domain.model.DuplicateGroup
import com.coreclean.app.domain.model.MediaImage
import com.coreclean.app.domain.usecase.media.FindDuplicateImagesUseCase
import com.coreclean.app.domain.usecase.media.GetAllImagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val getAllImagesUseCase: GetAllImagesUseCase,
    private val findDuplicateImagesUseCase: FindDuplicateImagesUseCase,
    private val selectedImagesHolder: SelectedImagesHolder
) : ViewModel() {

    private val _uiState = MutableStateFlow<MediaUiState>(MediaUiState.Idle)
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    private val _duplicateGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val duplicateGroups: StateFlow<List<DuplicateGroup>> = _duplicateGroups.asStateFlow()

    private val _isScanningDuplicates = MutableStateFlow(false)
    val isScanningDuplicates: StateFlow<Boolean> = _isScanningDuplicates.asStateFlow()

    private val _selectedImages = MutableStateFlow<Set<Long>>(emptySet())
    val selectedImages: StateFlow<Set<Long>> = _selectedImages.asStateFlow()

    private var loadJob: Job? = null

    fun loadImages() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = MediaUiState.Loading
            getAllImagesUseCase()
                .catch { e -> _uiState.value = MediaUiState.Error(e.message ?: "Không thể tải ảnh") }
                .collect { images -> _uiState.value = MediaUiState.Success(images = images) }
        }
    }

    fun onPermissionDenied() {
        _uiState.value = MediaUiState.PermissionDenied
    }

    fun findDuplicates() {
        val currentState = _uiState.value
        if (currentState !is MediaUiState.Success) return
        if (_isScanningDuplicates.value) return
        viewModelScope.launch {
            _isScanningDuplicates.value = true
            try {
                _duplicateGroups.value = findDuplicateImagesUseCase(currentState.images)
            } finally {
                _isScanningDuplicates.value = false
            }
        }
    }

    fun toggleImageSelection(imageId: Long) {
        _selectedImages.value = _selectedImages.value.toMutableSet().apply {
            if (contains(imageId)) remove(imageId) else add(imageId)
        }
    }

    fun clearSelection() {
        _selectedImages.value = emptySet()
        selectedImagesHolder.images = emptyList()
    }

    /** Copies current selection into the shared holder before navigating to SafetyReviewScreen. */
    fun prepareReview() {
        val currentState = _uiState.value
        if (currentState !is MediaUiState.Success) return
        val ids = _selectedImages.value
        selectedImagesHolder.images = currentState.images.filter { it.id in ids }
    }
}

// ── UiState ───────────────────────────────────────────────────────────────────
sealed class MediaUiState {
    data object Idle : MediaUiState()
    data object Loading : MediaUiState()
    data object PermissionDenied : MediaUiState()

    data class Success(
        val images: List<MediaImage> = emptyList()
    ) : MediaUiState()

    data class Error(val message: String) : MediaUiState()
}
