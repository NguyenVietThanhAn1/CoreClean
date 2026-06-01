package com.coreclean.app.presentation.appanalyzer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coreclean.app.domain.model.InstalledApp
import com.coreclean.app.domain.repository.AppListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppSortOrder { SIZE_DESC, INSTALL_DATE_DESC, NAME_ASC }
enum class AppFilter { ALL, USER, SYSTEM }

data class AppAnalyzerUiState(
    val isLoading: Boolean = true,
    val apps: List<InstalledApp> = emptyList(),
    val sortOrder: AppSortOrder = AppSortOrder.SIZE_DESC,
    val filter: AppFilter = AppFilter.ALL,
    val selectedPackages: Set<String> = emptySet()
)

@HiltViewModel
class AppAnalyzerViewModel @Inject constructor(
    private val repository: AppListRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AppAnalyzerUiState())
    val state: StateFlow<AppAnalyzerUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true)
        val apps = repository.getInstalledApps()
        _state.value = _state.value.copy(isLoading = false, apps = apps)
    }

    fun setSortOrder(order: AppSortOrder) { _state.value = _state.value.copy(sortOrder = order) }
    fun setFilter(filter: AppFilter)      { _state.value = _state.value.copy(filter = filter) }

    fun toggleSelection(pkg: String) {
        val sel = _state.value.selectedPackages
        _state.value = _state.value.copy(
            selectedPackages = if (pkg in sel) sel - pkg else sel + pkg
        )
    }

    fun clearSelection() { _state.value = _state.value.copy(selectedPackages = emptySet()) }

    fun displayedApps(): List<InstalledApp> {
        val s = _state.value
        val filtered = when (s.filter) {
            AppFilter.ALL    -> s.apps
            AppFilter.USER   -> s.apps.filter { !it.isSystem }
            AppFilter.SYSTEM -> s.apps.filter { it.isSystem }
        }
        return when (s.sortOrder) {
            AppSortOrder.SIZE_DESC         -> filtered.sortedByDescending { it.apkSizeBytes }
            AppSortOrder.INSTALL_DATE_DESC -> filtered.sortedByDescending { it.installTime }
            AppSortOrder.NAME_ASC          -> filtered.sortedBy { it.appName }
        }
    }
}
