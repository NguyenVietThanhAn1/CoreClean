package com.coreclean.app.presentation.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coreclean.app.domain.CrashReporter
import com.coreclean.app.domain.model.AppUsageInfo
import com.coreclean.app.domain.model.UsageRange
import com.coreclean.app.domain.repository.AppUsageRepository
import com.coreclean.app.domain.usecase.usage.GetAppUsageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AppUsageUiState {
    data object Loading : AppUsageUiState
    data object NoPermission : AppUsageUiState
    data class Success(val items: List<AppUsageInfo>, val range: UsageRange) : AppUsageUiState
    data class Error(val message: String) : AppUsageUiState
}

@HiltViewModel
class AppUsageViewModel @Inject constructor(
    private val getAppUsage: GetAppUsageUseCase,
    private val repository: AppUsageRepository,
    private val crashReporter: CrashReporter
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppUsageUiState>(AppUsageUiState.Loading)
    val uiState: StateFlow<AppUsageUiState> = _uiState

    private var currentRange = UsageRange.LAST_7

    init { load() }

    fun load(range: UsageRange = currentRange) {
        currentRange = range
        if (!repository.hasUsageAccessPermission()) {
            _uiState.value = AppUsageUiState.NoPermission
            return
        }
        viewModelScope.launch {
            _uiState.value = AppUsageUiState.Loading
            try {
                val items = getAppUsage(range)
                _uiState.value = AppUsageUiState.Success(items, range)
            } catch (e: Exception) {
                crashReporter.captureException(e)
                _uiState.value = AppUsageUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
