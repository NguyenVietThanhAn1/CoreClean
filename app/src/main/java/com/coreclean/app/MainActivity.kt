package com.coreclean.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.coreclean.app.core.preferences.AppPreferenceKeys
import com.coreclean.app.presentation.CoreCleanApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var dataStore: DataStore<Preferences>

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_CoreClean)
        super.onCreate(savedInstanceState)

        // Skip onboarding for BaselineProfile generation (DEBUG only)
        if (BuildConfig.DEBUG && intent.getBooleanExtra("skip_onboarding", false)) {
            runBlocking {
                dataStore.edit { it[AppPreferenceKeys.ONBOARDING_DONE] = true }
            }
        }

        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            CoreCleanApp(dataStore = dataStore, windowSizeClass = windowSizeClass)
        }
    }
}
