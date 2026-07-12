package com.coreclean.app.presentation.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coreclean.app.domain.model.BatteryInfo
import com.coreclean.app.domain.repository.BatteryRepository
import com.coreclean.app.domain.usecase.battery.BatteryPrediction
import com.coreclean.app.domain.usecase.battery.PredictBatteryRemainingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BatteryUiState {
    data object Loading : BatteryUiState()
    data class  Success(val info: BatteryInfo) : BatteryUiState()
    data class  Error(val message: String) : BatteryUiState()
}

@HiltViewModel
class BatteryViewModel @Inject constructor(
    repository: BatteryRepository,
    private val predictBatteryRemaining: PredictBatteryRemainingUseCase
) : ViewModel() {

    private val _prediction = MutableStateFlow<BatteryPrediction?>(null)
    val prediction: StateFlow<BatteryPrediction?> = _prediction.asStateFlow()

    val uiState: StateFlow<BatteryUiState> = repository
        .observe()
        .map<BatteryInfo, BatteryUiState> { info ->
            refreshPrediction(info.levelPercent, info.isCharging)
            BatteryUiState.Success(info)
        }
        .catch { emit(BatteryUiState.Error(it.message ?: "")) }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = BatteryUiState.Loading
        )

    private fun refreshPrediction(level: Int, isCharging: Boolean) {
        viewModelScope.launch {
            _prediction.value = predictBatteryRemaining(level, isCharging)
        }
    }
}
