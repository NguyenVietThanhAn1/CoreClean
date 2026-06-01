package com.coreclean.app.presentation.privacy

import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coreclean.app.data.local.dao.BatteryHistoryDao
import com.coreclean.app.data.local.dao.PendingReviewDao
import com.coreclean.app.data.local.dao.ScanResultDao
import com.coreclean.app.domain.model.UsageRange
import com.coreclean.app.domain.repository.AppUsageRepository
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
    val message: String? = null
)

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val scanResultDao: ScanResultDao,
    private val pendingReviewDao: PendingReviewDao,
    private val batteryHistoryDao: BatteryHistoryDao,
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

        val scanCount        = scanResultDao.count()
        val pendingCount     = pendingReviewDao.count()
        val dsKeys           = dataStore.data.map { it.asMap().size }.first()
        val batteryCount     = batteryHistoryDao.count()

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
        scanResultDao.clearAll()
        pendingReviewDao.clearAll()
        _state.value = _state.value.copy(
            scanResultCount    = 0,
            pendingReviewCount = 0,
            message            = "Da xoa lich su quet"
        )
    }

    fun clearBatteryHistory() = viewModelScope.launch {
        batteryHistoryDao.clearAll()
        _state.value = _state.value.copy(batteryHistoryCount = 0, message = "Da xoa lich su pin")
    }

    fun dismissMessage() { _state.value = _state.value.copy(message = null) }
}
