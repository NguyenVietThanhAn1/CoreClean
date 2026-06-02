package com.coreclean.app.core.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object AppPreferenceKeys {
    val ONBOARDING_DONE          = booleanPreferencesKey("onboarding_done")
    val THEME_MODE               = stringPreferencesKey("theme_mode")    // SYSTEM | LIGHT | DARK
    val DYNAMIC_COLOR            = booleanPreferencesKey("dynamic_color")
    val AMOLED_ENABLED           = booleanPreferencesKey("amoled_enabled")
    val BACKGROUND_SCAN          = booleanPreferencesKey("background_scan")
    val SCAN_INTERVAL_HOURS      = intPreferencesKey("scan_interval_hours")
    val SAF_FOLDER_URIS          = stringSetPreferencesKey("saf_folder_uris")
    val APP_LANGUAGE             = stringPreferencesKey("app_language")  // SYSTEM | VIETNAMESE | ENGLISH
    val CRASH_REPORTING          = booleanPreferencesKey("crash_reporting")

    // Auto-cleaning schedule
    val SCHEDULE_CONFIG_JSON     = stringPreferencesKey("schedule_config_json")

    // Notification recommendations
    val RECOMMENDATIONS_ENABLED  = booleanPreferencesKey("recommendations_enabled")
    val NOTIF_LAST_STORAGE_FULL  = stringPreferencesKey("notif_last_storage_full")  // ISO timestamp
    val NOTIF_LAST_DUPLICATE     = stringPreferencesKey("notif_last_duplicate")     // ISO timestamp

    // AI Dedupe — perceptual hash
    val PERCEPTUAL_DEDUPE        = booleanPreferencesKey("perceptual_dedupe")

    // HomeViewModel suggestion cache — epoch-ms of last full (heavy) scan + serialized result
    val HOME_SUGGESTIONS_CACHE_TS   = longPreferencesKey("home_suggestions_cache_ts")
    val HOME_SUGGESTIONS_CACHE_JSON = stringPreferencesKey("home_suggestions_cache_json")

    // Notification permission — set true when POST_NOTIFICATIONS is missing; cleared after user grants
    val NOTIF_PERMISSION_NEEDED = booleanPreferencesKey("notif_permission_needed")
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AppLanguage(val tag: String) { SYSTEM(""), VIETNAMESE("vi"), ENGLISH("en") }
