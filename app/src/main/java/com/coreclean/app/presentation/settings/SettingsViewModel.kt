package com.coreclean.app.presentation.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.coreclean.app.core.preferences.AppLanguage
import com.coreclean.app.core.preferences.AppPreferenceKeys
import com.coreclean.app.core.preferences.ThemeMode
import com.coreclean.app.data.worker.AutoCleanWorker
import com.coreclean.app.data.worker.MediaScanWorker
import com.coreclean.app.domain.CrashReporter
import com.coreclean.app.domain.model.Frequency
import com.coreclean.app.domain.model.JunkCategory
import com.coreclean.app.domain.model.ScheduleConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val amoledEnabled: Boolean = false,
    val backgroundScan: Boolean = true,
    val scanIntervalHours: Int = 12,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val crashReporting: Boolean = false,
    val scheduleConfig: ScheduleConfig = ScheduleConfig(),
    val recommendationsEnabled: Boolean = true,
    val perceptualDedupe: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val workManager: WorkManager,
    private val crashReporter: CrashReporter
) : ViewModel() {

    val state = dataStore.data.map { prefs ->
        val schedJson = prefs[AppPreferenceKeys.SCHEDULE_CONFIG_JSON]
        val schedule  = schedJson?.let {
            runCatching { Json.decodeFromString<ScheduleConfig>(it) }.getOrNull()
        } ?: ScheduleConfig()

        SettingsState(
            themeMode               = ThemeMode.valueOf(prefs[AppPreferenceKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name),
            dynamicColor            = prefs[AppPreferenceKeys.DYNAMIC_COLOR] ?: true,
            amoledEnabled           = prefs[AppPreferenceKeys.AMOLED_ENABLED] ?: false,
            backgroundScan          = prefs[AppPreferenceKeys.BACKGROUND_SCAN] ?: true,
            scanIntervalHours       = prefs[AppPreferenceKeys.SCAN_INTERVAL_HOURS] ?: 12,
            language                = AppLanguage.valueOf(prefs[AppPreferenceKeys.APP_LANGUAGE] ?: AppLanguage.SYSTEM.name),
            crashReporting          = prefs[AppPreferenceKeys.CRASH_REPORTING] ?: false,
            scheduleConfig          = schedule,
            recommendationsEnabled  = prefs[AppPreferenceKeys.RECOMMENDATIONS_ENABLED] ?: true,
            perceptualDedupe        = prefs[AppPreferenceKeys.PERCEPTUAL_DEDUPE] ?: false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        dataStore.edit { it[AppPreferenceKeys.THEME_MODE] = mode.name }
    }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[AppPreferenceKeys.DYNAMIC_COLOR] = enabled }
    }

    fun setAmoledEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[AppPreferenceKeys.AMOLED_ENABLED] = enabled }
    }

    fun setBackgroundScan(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[AppPreferenceKeys.BACKGROUND_SCAN] = enabled }
        if (enabled) rescheduleWorker(state.value.scanIntervalHours)
        else workManager.cancelUniqueWork("media_scan")
    }

    fun setScanInterval(hours: Int) = viewModelScope.launch {
        dataStore.edit { it[AppPreferenceKeys.SCAN_INTERVAL_HOURS] = hours }
        if (state.value.backgroundScan) rescheduleWorker(hours)
    }

    fun setLanguage(lang: AppLanguage) = viewModelScope.launch {
        dataStore.edit { it[AppPreferenceKeys.APP_LANGUAGE] = lang.name }
        val locales = if (lang.tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                      else LocaleListCompat.forLanguageTags(lang.tag)
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun setCrashReporting(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[AppPreferenceKeys.CRASH_REPORTING] = enabled }
        if (!enabled) {
            crashReporter.setEnabled(false)
        }
    }

    fun runScanNow() {
        val request = OneTimeWorkRequestBuilder<MediaScanWorker>().build()
        workManager.enqueue(request)
    }

    fun resetOnboarding() = viewModelScope.launch {
        dataStore.edit { it[AppPreferenceKeys.ONBOARDING_DONE] = false }
    }

    fun setAutoCleanEnabled(enabled: Boolean) = viewModelScope.launch {
        val updated = state.value.scheduleConfig.copy(enabled = enabled)
        saveScheduleConfig(updated)
        if (enabled) scheduleAutoClean(updated)
        else workManager.cancelAllWorkByTag("auto_clean")
    }

    fun setAutoCleanFrequency(freq: Frequency) = viewModelScope.launch {
        val updated = state.value.scheduleConfig.copy(frequency = freq)
        saveScheduleConfig(updated)
    }

    fun toggleAutoCleanCategory(category: JunkCategory) = viewModelScope.launch {
        val current = state.value.scheduleConfig.categories.toMutableSet()
        if (!current.remove(category)) current.add(category)
        saveScheduleConfig(state.value.scheduleConfig.copy(categories = current))
    }

    fun setRecommendationsEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[AppPreferenceKeys.RECOMMENDATIONS_ENABLED] = enabled }
    }

    fun setPerceptualDedupe(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[AppPreferenceKeys.PERCEPTUAL_DEDUPE] = enabled }
    }

    private suspend fun saveScheduleConfig(config: ScheduleConfig) {
        dataStore.edit { it[AppPreferenceKeys.SCHEDULE_CONFIG_JSON] = Json.encodeToString(config) }
    }

    private fun scheduleAutoClean(config: ScheduleConfig) {
        val nowMs   = System.currentTimeMillis()
        val target  = nextRunTimeMs(config)
        val delayMs = (target - nowMs).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<AutoCleanWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag("auto_clean")
            .build()
        workManager.enqueue(request)
    }

    private fun nextRunTimeMs(config: ScheduleConfig): Long {
        val now  = java.time.LocalDateTime.now()
        val next = when (config.frequency) {
            Frequency.DAILY   -> now.plusDays(1).with(java.time.LocalTime.of(config.hour, config.minute))
            Frequency.WEEKLY  -> now.plusWeeks(1).with(java.time.LocalTime.of(config.hour, config.minute))
            Frequency.MONTHLY -> now.plusMonths(1).with(java.time.LocalTime.of(config.hour, config.minute))
        }
        return next.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun rescheduleWorker(hours: Int) {
        val request = PeriodicWorkRequestBuilder<MediaScanWorker>(hours.toLong(), TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork("media_scan", ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
