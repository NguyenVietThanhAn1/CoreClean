package com.coreclean.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.coreclean.app.presentation.CoreCleanApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var dataStore: DataStore<Preferences>

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_CoreClean)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoreCleanApp(dataStore = dataStore)
        }
    }
}
