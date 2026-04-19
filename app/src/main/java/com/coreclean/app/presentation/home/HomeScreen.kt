package com.coreclean.app.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.coreclean.app.presentation.navigation.MediaRoute

@Composable
fun HomeScreen(navController: NavController) {
    Box(
        modifier        = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = { navController.navigate(MediaRoute) }) {
            Text("Go to Media Scanner")
        }
    }
}
