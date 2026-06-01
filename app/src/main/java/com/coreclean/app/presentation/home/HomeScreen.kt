package com.coreclean.app.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.coreclean.app.presentation.navigation.*

private data class FeatureItem(
    val title:    String,
    val subtitle: String,
    val icon:     ImageVector,
    val enabled:  Boolean,
    val route:    Any
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val features = listOf(
        FeatureItem("Media Scanner",    "Quet & don anh trung",    Icons.Default.PhotoLibrary,      true,  MediaRoute),
        FeatureItem("Storage Analyzer", "Phan tich bo nho",         Icons.Default.Storage,           true,  StorageRoute),
        FeatureItem("Battery Monitor",  "Giam sat pin",             Icons.Default.BatteryFull,       true,  BatteryRoute),
        FeatureItem("App Usage",        "Thong ke su dung",         Icons.Default.BarChart,          true,  AppUsageRoute),
        FeatureItem("Contacts",         "Quan ly danh ba",          Icons.Default.Contacts,          true,  ContactRoute),
        FeatureItem("Junk Cleaner",     "Don dep file rac",         Icons.Default.CleaningServices,  true,  JunkRoute),
        FeatureItem("RAM Monitor",      "Giam sat bo nho",          Icons.Default.Memory,            true,  RamRoute),
        FeatureItem("App Analyzer",     "Phan tich ung dung",       Icons.Default.Android,           true,  AppAnalyzerRoute),
    )

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text("CoreClean", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall)
                },
                actions = {
                    IconButton(onClick = { navController.navigate(SettingsRoute) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Cai dat")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns               = GridCells.Fixed(2),
            contentPadding        = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement   = Arrangement.spacedBy(12.dp),
            modifier              = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            items(features) { feature ->
                FeatureCard(
                    title    = feature.title,
                    subtitle = feature.subtitle,
                    icon     = feature.icon,
                    enabled  = feature.enabled,
                    onClick  = { if (feature.enabled) navController.navigate(feature.route) }
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String, subtitle: String,
    icon: ImageVector, enabled: Boolean, onClick: () -> Unit
) {
    val containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer
                         else        MaterialTheme.colorScheme.surfaceVariant
    val contentColor   = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                         else        MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier.fillMaxWidth().aspectRatio(0.85f),
        colors   = CardDefaults.cardColors(
            containerColor         = containerColor,
            disabledContainerColor = containerColor.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = contentColor.copy(alpha = if (enabled) 1f else 0.5f),
                modifier           = Modifier.size(36.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor.copy(alpha = if (enabled) 1f else 0.6f))
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = if (enabled) 0.8f else 0.5f))
                if (!enabled) {
                    Spacer(Modifier.height(4.dp))
                    SuggestionChip(onClick = {}, enabled = false,
                        label = { Text("Sap ra mat", style = MaterialTheme.typography.labelSmall) })
                }
            }
        }
    }
}
