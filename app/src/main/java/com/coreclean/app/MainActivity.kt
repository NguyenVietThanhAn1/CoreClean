package com.coreclean.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.coreclean.app.presentation.CoreCleanApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // setTheme ensures splash screen + status/nav bar tint are synced with
        // our NoActionBar theme before Compose takes over the window.
        setTheme(R.style.Theme_CoreClean)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoreCleanApp()
        }
    }
}
