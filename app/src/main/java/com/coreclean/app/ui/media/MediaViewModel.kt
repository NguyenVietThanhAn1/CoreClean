package com.coreclean.app.ui.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coreclean.app.domain.model.DuplicateGroup
import com.coreclean.app.domain.model.MediaImage
import com.coreclean.app.domain.usecase.media.FindDuplicateImagesUseCase
import com.coreclean.app.domain.usecase.media.GetAllImagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val getAllImagesUseCase: GetAllImagesUseCase,
    private val findDuplicateImagesUseCase: FindDuplicateImagesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MediaUiState>(MediaUiState.Idle)
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    // Ảnh được chọn để xóa (Safety Review)
    private val _selectedImages = MutableStateFlow<Set<Long>>(emptySet())
    val selectedImages: StateFlow<Set<Long>> = _selectedImages.asStateFlow()

    fun loadImages() {
        viewModelScope.launch {
            _uiState.value = MediaUiState.Loading
            getAllImagesUseCase()
                .catch { e ->
                    _uiState.value = MediaUiState.Error(e.message ?: "Không thể tải ảnh")
                }
                .collect { images ->
                    _uiState.value = MediaUiState.Success(images = images)
                }
        }
    }

    fun findDuplicates() {
        val currentState = _uiState.value
        if (currentState !is MediaUiState.Success) return

        viewModelScope.launch {
            _uiState.value = MediaUiState.Loading
            val duplicates = findDuplicateImagesUseCase(currentState.images)
            _uiState.value = currentState.copy(duplicateGroups = duplicates)
        }
    }

    fun toggleImageSelection(imageId: Long) {
        _selectedImages.value = _selectedImages.value.toMutableSet().apply {
            if (contains(imageId)) remove(imageId) else add(imageId)
        }
    }

    fun clearSelection() {
        _selectedImages.value = emptySet()
    }
}

// ── UiState ───────────────────────────────────────────────────────────────────
sealed class MediaUiState {
    object Idle    : MediaUiState()
    object Loading : MediaUiState()

    data class Success(
        val images: List<MediaImage>            = emptyList(),
        val duplicateGroups: List<DuplicateGroup> = emptyList()
    ) : MediaUiState()

    data class Error(val message: String) : MediaUiState()
}
