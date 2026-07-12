package com.coreclean.app.presentation.privacy

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coreclean.app.R
import com.coreclean.app.domain.model.UsageRange
import com.coreclean.app.domain.repository.AppUsageRepository
import com.coreclean.app.domain.repository.BatteryRepository
import com.coreclean.app.domain.repository.MediaRepository
import com.coreclean.app.domain.repository.ScanResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrivacyUiState(
    val isLoading: Boolean = true,
    val grantedPermissions: List<String> = emptyList(),
    val deniedPermissions: List<String> = emptyList(),
    val foregroundOpenCount: Int = 0,
    val scanResultCount: Int = 0,
    val pendingReviewCount: Int = 0,
    val dataStoreKeyCount: Int = 0,
    val batteryHistoryCount: Int = 0,
    @StringRes val messageRes: Int? = null
)

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val scanResultRepository: ScanResultRepository,
    private val mediaRepository: MediaRepository,
    private val batteryRepository: BatteryRepository,
    private val appUsageRepository: AppUsageRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PrivacyUiState())
    val state: StateFlow<PrivacyUiState> = _state.asStateFlow()

    private val trackedPermissions = listOf(
        android.Manifest.permission.READ_MEDIA_IMAGES,
        android.Manifest.permission.READ_MEDIA_VIDEO,
        android.Manifest.permission.READ_CONTACTS,
        android.Manifest.permission.POST_NOTIFICATIONS
    )

    init { load() }

    fun load() = viewModelScope.launch {
        _state.value = PrivacyUiState(isLoading = true)
        val pm = context.packageManager

        val (granted, denied) = trackedPermissions.partition { perm ->
            pm.checkPermission(perm, context.packageName) == PackageManager.PERMISSION_GRANTED
        }

        var openCount = 0
        if (appUsageRepository.hasUsageAccessPermission()) {
            runCatching {
                val stats = appUsageRepository.getUsageStats(UsageRange.LAST_7)
                openCount = stats.firstOrNull { it.packageName == context.packageName }
                    ?.totalTimeForegroundMs?.let { 1 }?.toInt() ?: 0
            }
        }

        val scanCount        = scanResultRepository.getCount()
        val pendingCount     = mediaRepository.getPendingReviewCount()
        val dsKeys           = dataStore.data.map { it.asMap().size }.first()
        val batteryCount     = batteryRepository.getHistoryCount()

        _state.value = PrivacyUiState(
            isLoading            = false,
            grantedPermissions   = granted,
            deniedPermissions    = denied,
            foregroundOpenCount  = openCount,
            scanResultCount      = scanCount,
            pendingReviewCount   = pendingCount,
            dataStoreKeyCount    = dsKeys,
            batteryHistoryCount  = batteryCount
        )
    }

    fun clearHistory() = viewModelScope.launch {
        scanResultRepository.clearAll()
        mediaRepository.clearPendingReviewIds()
        _state.value = _state.value.copy(
            scanResultCount    = 0,
            pendingReviewCount = 0,
            messageRes         = R.string.privacy_cleared_scan_history
        )
    }

    fun clearBatteryHistory() = viewModelScope.launch {
        batteryRepository.clearHistory()
        _state.value = _state.value.copy(batteryHistoryCount = 0, messageRes = R.string.privacy_cleared_battery_history)
    }

    fun dismissMessage() { _state.value = _state.value.copy(messageRes = null) }
}
