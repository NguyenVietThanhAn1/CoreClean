package com.coreclean.app.presentation.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coreclean.app.domain.model.BatteryInfo
import com.coreclean.app.domain.repository.BatteryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed class BatteryUiState {
    data object Loading : BatteryUiState()
    data class  Success(val info: BatteryInfo) : BatteryUiState()
    data class  Error(val message: String) : BatteryUiState()
}

@HiltViewModel
class BatteryViewModel @Inject constructor(
    repository: BatteryRepository
) : ViewModel() {

    val uiState: StateFlow<BatteryUiState> = repository
        .observe()
        .map<BatteryInfo, BatteryUiState> { BatteryUiState.Success(it) }
        .catch { emit(BatteryUiState.Error(it.message ?: "Loi doc pin")) }
        .stateIn(
            scope         = viewModelScope,
            started       = SharingStarted.WhileSubscribed(5_000),
            initialValue  = BatteryUiState.Loading
        )
}
