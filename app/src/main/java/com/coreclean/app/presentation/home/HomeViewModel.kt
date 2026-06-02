package com.coreclean.app.presentation.home

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coreclean.app.core.preferences.AppPreferenceKeys
import com.coreclean.app.domain.model.CleaningSuggestion
import com.coreclean.app.domain.model.UsageRange
import com.coreclean.app.domain.repository.AppListRepository
import com.coreclean.app.domain.repository.AppUsageRepository
import com.coreclean.app.domain.repository.MediaRepository
import com.coreclean.app.domain.repository.StorageRepository
import com.coreclean.app.domain.usecase.junk.ScanJunkUseCase
import com.coreclean.app.domain.usecase.suggest.GenerateSuggestionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L // 6 hours

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val generateSuggestions: GenerateSuggestionsUseCase,
    private val mediaRepository: MediaRepository,
    private val storageRepository: StorageRepository,
    private val appListRepository: AppListRepository,
    private val appUsageRepository: AppUsageRepository,
    private val scanJunkUseCase: ScanJunkUseCase,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _suggestions = MutableStateFlow<List<CleaningSuggestion>>(emptyList())
    val suggestions: StateFlow<List<CleaningSuggestion>> = _suggestions.asStateFlow()

    init { loadSuggestions() }

    fun loadSuggestions() = viewModelScope.launch {
        runCatching {
            val nowMs   = System.currentTimeMillis()
            val cacheTs = dataStore.data
                .map { it[AppPreferenceKeys.HOME_SUGGESTIONS_CACHE_TS] ?: 0L }
                .first()
            val cacheStale = (nowMs - cacheTs) > CACHE_TTL_MS

            // Fast data — always fresh.
            val images  = mediaRepository.getAllImages().first()
            val storage = storageRepository.getStorageInfo()
            val dupes   = mediaRepository.findDuplicates(images)

            // Heavy data — rate-limited to once per 6 h.
            val installedApps = if (cacheStale) appListRepository.getInstalledApps()
                                else emptyList()
            val lastUsedDays = if (cacheStale && appUsageRepository.hasUsageAccessPermission()) {
                val nowMs2 = System.currentTimeMillis()
                appUsageRepository.getUsageStats(UsageRange.LAST_30)
                    .associate { info ->
                        info.packageName to
                            ((nowMs2 - info.lastTimeUsed) / (1_000L * 60 * 60 * 24)).toInt()
                    }
            } else emptyMap()
            // SAF folders not relevant for suggestion-level junk scan.
            val junkItems = if (cacheStale) scanJunkUseCase() else emptyList()

            _suggestions.value = generateSuggestions.invoke(
                duplicateGroups = dupes,
                installedApps   = installedApps,
                appLastUsedDays = lastUsedDays,
                junkItems       = junkItems,
                allImages       = images,
                storageInfo     = storage
            )

            if (cacheStale) {
                dataStore.edit { it[AppPreferenceKeys.HOME_SUGGESTIONS_CACHE_TS] = nowMs }
            }
        }
    }
}
