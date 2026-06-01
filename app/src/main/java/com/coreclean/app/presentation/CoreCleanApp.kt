package com.coreclean.app.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.coreclean.app.core.preferences.AppPreferenceKeys
import com.coreclean.app.core.preferences.ThemeMode
import com.coreclean.app.presentation.navigation.CleanerNavGraph
import com.coreclean.app.presentation.navigation.HomeRoute
import com.coreclean.app.presentation.navigation.OnboardingRoute
import com.coreclean.app.presentation.settings.SettingsViewModel
import com.coreclean.app.presentation.theme.CoreCleanTheme
import javax.inject.Inject
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.map

@Composable
fun CoreCleanApp(dataStore: DataStore<Preferences>) {
    val systemDark = isSystemInDarkTheme()
    val settingsVm: SettingsViewModel = hiltViewModel()
    val settings by settingsVm.state.collectAsState()

    val darkTheme = when (settings.themeMode) {
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
        ThemeMode.SYSTEM -> systemDark
    }

    val onboardingDone by dataStore.data
        .map { prefs -> prefs[AppPreferenceKeys.ONBOARDING_DONE] ?: false }
        .collectAsState(initial = null)

    // Wait until onboarding flag is determined
    val resolved = onboardingDone ?: return

    CoreCleanTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
        val navController   = rememberNavController()
        val startDestination = if (resolved) HomeRoute else OnboardingRoute
        CleanerNavGraph(navController = navController, startDestination = startDestination)
    }
}
