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
import com.coreclean.app.data.worker.MediaScanWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val backgroundScan: Boolean = true,
    val scanIntervalHours: Int = 12,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val crashReporting: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val workManager: WorkManager
) : ViewModel() {

    val state = dataStore.data.map { prefs ->
        SettingsState(
            themeMode         = ThemeMode.valueOf(prefs[AppPreferenceKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name),
            dynamicColor      = prefs[AppPreferenceKeys.DYNAMIC_COLOR] ?: true,
            backgroundScan    = prefs[AppPreferenceKeys.BACKGROUND_SCAN] ?: true,
            scanIntervalHours = prefs[AppPreferenceKeys.SCAN_INTERVAL_HOURS] ?: 12,
            language          = AppLanguage.valueOf(prefs[AppPreferenceKeys.APP_LANGUAGE] ?: AppLanguage.SYSTEM.name),
            crashReporting    = prefs[AppPreferenceKeys.CRASH_REPORTING] ?: false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        dataStore.edit { it[AppPreferenceKeys.THEME_MODE] = mode.name }
    }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[AppPreferenceKeys.DYNAMIC_COLOR] = enabled }
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
            runCatching { io.sentry.Sentry.close() }
        }
    }

    fun runScanNow() {
        val request = OneTimeWorkRequestBuilder<MediaScanWorker>().build()
        workManager.enqueue(request)
    }

    fun resetOnboarding() = viewModelScope.launch {
        dataStore.edit { it[AppPreferenceKeys.ONBOARDING_DONE] = false }
    }

    private fun rescheduleWorker(hours: Int) {
        val request = PeriodicWorkRequestBuilder<MediaScanWorker>(hours.toLong(), TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork("media_scan", ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
