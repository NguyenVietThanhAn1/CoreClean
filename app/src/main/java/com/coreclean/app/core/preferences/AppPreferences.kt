package com.coreclean.app.core.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object AppPreferenceKeys {
    val ONBOARDING_DONE    = booleanPreferencesKey("onboarding_done")
    val THEME_MODE         = stringPreferencesKey("theme_mode")   // SYSTEM | LIGHT | DARK
    val DYNAMIC_COLOR      = booleanPreferencesKey("dynamic_color")
    val BACKGROUND_SCAN    = booleanPreferencesKey("background_scan")
    val SCAN_INTERVAL_HOURS = intPreferencesKey("scan_interval_hours")
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }
