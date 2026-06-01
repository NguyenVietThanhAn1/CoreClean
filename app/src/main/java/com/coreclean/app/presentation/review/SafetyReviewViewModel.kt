package com.coreclean.app.presentation.review

import android.content.IntentSender
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.coreclean.app.data.local.dao.PendingReviewDao
import com.coreclean.app.domain.model.MediaImage
import com.coreclean.app.domain.repository.MediaRepository
import com.coreclean.app.presentation.navigation.ReviewRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    savedStateHandle: SavedStateHandle,
    private val repository: MediaRepository,
    private val pendingReviewDao: PendingReviewDao
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ReviewRoute>()

    private val _selectedImages = MutableStateFlow<List<MediaImage>>(emptyList())
    val selectedImages: StateFlow<List<MediaImage>> = _selectedImages.asStateFlow()

    private val _uiState = MutableStateFlow<ReviewUiState>(ReviewUiState.Idle)
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    val totalSize: Long get() = _selectedImages.value.sumOf { it.size }

    init {
        // Resolve the image list either from route args or from the DB fallback
        viewModelScope.launch {
            val ids: Set<Long> = if (route.imageIds.isNotEmpty()) {
                route.imageIds.toHashSet()
            } else {
                pendingReviewDao.getAllIds().toHashSet()
            }
            _selectedImages.value = repository.getAllImages()
                .first()
                .filter { it.id in ids }
        }
    }

    fun confirmDelete() {
        val images = _selectedImages.value
        if (images.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = ReviewUiState.Deleting
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                runCatching { repository.createDeleteRequest(images) }
                    .onSuccess { _uiState.value = ReviewUiState.RequestIntent(it) }
                    .onFailure { _uiState.value = ReviewUiState.Error(it.message ?: "Loi tao yeu cau xoa") }
            } else {
                repository.deleteImages(images)
                    .onSuccess { _uiState.value = ReviewUiState.Done(it) }
                    .onFailure { _uiState.value = ReviewUiState.Error(it.message ?: "Loi xoa anh") }
            }
        }
    }

    fun onSystemDeleteDone() {
        _uiState.value = ReviewUiState.Done(_selectedImages.value.size)
        viewModelScope.launch { pendingReviewDao.clearAll() }
    }
}
