package com.coreclean.app.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.coreclean.app.presentation.navigation.CleanerNavGraph
import com.coreclean.app.presentation.theme.CoreCleanTheme

@Composable
fun CoreCleanApp() {
    CoreCleanTheme {
        val navController = rememberNavController()
        CleanerNavGraph(navController)
    }
}
