package com.coreclean.app.ui.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coreclean.app.data.local.dao.PendingReviewDao
import com.coreclean.app.data.local.entity.PendingReviewEntity
import com.coreclean.app.domain.model.DuplicateGroup
import com.coreclean.app.domain.model.MediaImage
import com.coreclean.app.domain.usecase.media.FindDuplicateImagesUseCase
import com.coreclean.app.domain.usecase.media.GetAllImagesUseCase
import com.coreclean.app.presentation.navigation.ReviewRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val ROUTE_ID_LIMIT = 200

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val getAllImagesUseCase: GetAllImagesUseCase,
    private val findDuplicateImagesUseCase: FindDuplicateImagesUseCase,
    private val pendingReviewDao: PendingReviewDao
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
                .catch { e -> _uiState.value = MediaUiState.Error(e.message ?: "Khong the tai anh") }
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
    }

    /**
     * Builds a [ReviewRoute] and calls [onNavigate].
     * If the selection exceeds [ROUTE_ID_LIMIT] the IDs are persisted in Room
     * and an empty-list route is returned (SafetyReviewViewModel reads from DB).
     */
    fun prepareReview(onNavigate: (ReviewRoute) -> Unit) {
        val ids = _selectedImages.value.toList()
        viewModelScope.launch {
            val route = if (ids.size <= ROUTE_ID_LIMIT) {
                ReviewRoute("media", ids)
            } else {
                pendingReviewDao.clearAll()
                pendingReviewDao.insertAll(ids.map { PendingReviewEntity(it) })
                ReviewRoute("media", emptyList())  // signal: read from pending_review DB
            }
            onNavigate(route)
        }
    }
}

sealed class MediaUiState {
    data object Idle             : MediaUiState()
    data object Loading          : MediaUiState()
    data object PermissionDenied : MediaUiState()
    data class  Success(val images: List<MediaImage> = emptyList()) : MediaUiState()
    data class  Error(val message: String) : MediaUiState()
}
