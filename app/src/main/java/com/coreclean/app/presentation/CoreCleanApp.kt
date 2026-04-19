package com.coreclean.app.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.coreclean.app.presentation.navigation.CleanerNavGraph

@Composable
fun CoreCleanApp() {
    MaterialTheme {
        val navController = rememberNavController()
        CleanerNavGraph(navController)
    }
}