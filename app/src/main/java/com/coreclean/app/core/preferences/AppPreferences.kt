package com.coreclean.app.core.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object AppPreferenceKeys {
    val ONBOARDING_DONE      = booleanPreferencesKey("onboarding_done")
    val THEME_MODE           = stringPreferencesKey("theme_mode")    // SYSTEM | LIGHT | DARK
    val DYNAMIC_COLOR        = booleanPreferencesKey("dynamic_color")
    val BACKGROUND_SCAN      = booleanPreferencesKey("background_scan")
    val SCAN_INTERVAL_HOURS  = intPreferencesKey("scan_interval_hours")
    val SAF_FOLDER_URIS      = stringSetPreferencesKey("saf_folder_uris")
    val APP_LANGUAGE         = stringPreferencesKey("app_language")  // system | vi | en
    val CRASH_REPORTING      = booleanPreferencesKey("crash_reporting")
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AppLanguage(val tag: String) { SYSTEM(""), VIETNAMESE("vi"), ENGLISH("en") }
