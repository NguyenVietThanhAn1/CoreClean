package com.coreclean.app.presentation.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coreclean.app.core.preferences.AppPreferenceKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionOnboardingViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    val isOnboardingDone: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[AppPreferenceKeys.ONBOARDING_DONE] ?: false
    }

    fun completeOnboarding() = viewModelScope.launch {
        dataStore.edit { it[AppPreferenceKeys.ONBOARDING_DONE] = true }
    }
}
