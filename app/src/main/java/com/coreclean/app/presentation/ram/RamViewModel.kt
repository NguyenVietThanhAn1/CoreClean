package com.coreclean.app.presentation.ram

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coreclean.app.domain.model.RamInfo
import com.coreclean.app.domain.repository.RamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RamViewModel @Inject constructor(
    repository: RamRepository
) : ViewModel() {

    val ramInfo: StateFlow<RamInfo?> = repository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
