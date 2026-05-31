package com.coreclean.app.presentation.review

import android.content.IntentSender
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coreclean.app.core.SelectedImagesHolder
import com.coreclean.app.domain.model.MediaImage
import com.coreclean.app.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ReviewUiState {
    data object Idle : ReviewUiState()
    data object Deleting : ReviewUiState()
    data class RequestIntent(val intentSender: IntentSender) : ReviewUiState()
    data class Done(val deleted: Int) : ReviewUiState()
    data class Error(val message: String) : ReviewUiState()
}

@HiltViewModel
class SafetyReviewViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val holder: SelectedImagesHolder
) : ViewModel() {

    val selectedImages: List<MediaImage> get() = holder.images

    private val _uiState = MutableStateFlow<ReviewUiState>(ReviewUiState.Idle)
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    val totalSize: Long get() = selectedImages.sumOf { it.size }

    fun confirmDelete() {
        if (selectedImages.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = ReviewUiState.Deleting
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                runCatching { repository.createDeleteRequest(selectedImages) }
                    .onSuccess { _uiState.value = ReviewUiState.RequestIntent(it) }
                    .onFailure { _uiState.value = ReviewUiState.Error(it.message ?: "Lỗi tạo yêu cầu xoá") }
            } else {
                repository.deleteImages(selectedImages)
                    .onSuccess { _uiState.value = ReviewUiState.Done(it) }
                    .onFailure { _uiState.value = ReviewUiState.Error(it.message ?: "Lỗi xoá ảnh") }
            }
        }
    }

    fun onSystemDeleteDone() {
        _uiState.value = ReviewUiState.Done(selectedImages.size)
        holder.images = emptyList()
    }
}
