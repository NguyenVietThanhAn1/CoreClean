package com.coreclean.app.presentation.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coreclean.app.domain.model.MediaImage
import com.coreclean.app.domain.model.StorageInfo
import com.coreclean.app.domain.usecase.files.FindLargeFilesUseCase
import com.coreclean.app.domain.usecase.storage.GetStorageInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class StorageUiState {
    data object Loading : StorageUiState()
    data class  Success(val info: StorageInfo, val largeFiles: List<MediaImage> = emptyList()) : StorageUiState()
    data class  Error(val message: String) : StorageUiState()
}

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val getStorageInfoUseCase: GetStorageInfoUseCase,
    private val findLargeFilesUseCase: FindLargeFilesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<StorageUiState>(StorageUiState.Loading)
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

    init { loadStorageInfo() }

    fun loadStorageInfo() {
        viewModelScope.launch {
            _uiState.value = StorageUiState.Loading
            runCatching {
                val infoDeferred  = async { getStorageInfoUseCase() }
                val filesDeferred = async { runCatching { findLargeFilesUseCase() }.getOrDefault(emptyList()) }
                Pair(infoDeferred.await(), filesDeferred.await())
            }
                .onSuccess { (info, files) -> _uiState.value = StorageUiState.Success(info, files) }
                .onFailure { _uiState.value = StorageUiState.Error(it.message ?: "Loi doc bo nho") }
        }
    }
}
