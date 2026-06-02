package com.coreclean.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.coreclean.app.core.preferences.AppPreferenceKeys
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal fun CleanerApp.initializeSentry(dataStore: DataStore<Preferences>) {
    if (BuildConfig.SENTRY_DSN.isEmpty()) return
    MainScope().launch(Dispatchers.IO) {
        val enabled = dataStore.data.map { it[AppPreferenceKeys.CRASH_REPORTING] ?: false }.first()
        if (!enabled) return@launch
        SentryAndroid.init(this@initializeSentry) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.tracesSampleRate = 0.1
            options.isEnableUserInteractionTracing = false
        }
    }
}
