package com.coreclean.app.presentation.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.coreclean.app.R
import com.coreclean.app.domain.model.CleaningSuggestion
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
fun HomeScreen(
    navController: NavController,
    windowSizeClass: WindowSizeClass? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val notifPermissionNeeded by viewModel.notifPermissionNeeded.collectAsStateWithLifecycle()

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.clearNotifPermissionFlag() }

    val features = listOf(
        FeatureItem(stringResource(R.string.home_card_media_title),   stringResource(R.string.home_card_media_sub),   Icons.Default.PhotoLibrary,     true, MediaRoute),
        FeatureItem(stringResource(R.string.home_card_storage_title), stringResource(R.string.home_card_storage_sub), Icons.Default.Storage,          true, StorageRoute),
        FeatureItem(stringResource(R.string.home_card_battery_title), stringResource(R.string.home_card_battery_sub), Icons.Default.BatteryFull,      true, BatteryRoute),
        FeatureItem(stringResource(R.string.home_card_usage_title),   stringResource(R.string.home_card_usage_sub),   Icons.Default.BarChart,         true, AppUsageRoute),
        FeatureItem(stringResource(R.string.home_card_contacts_title),stringResource(R.string.home_card_contacts_sub),Icons.Default.Contacts,         true, ContactRoute),
        FeatureItem(stringResource(R.string.home_card_junk_title),    stringResource(R.string.home_card_junk_sub),    Icons.Default.CleaningServices, true, JunkRoute),
        FeatureItem(stringResource(R.string.home_card_ram_title),     stringResource(R.string.home_card_ram_sub),     Icons.Default.Memory,           true, RamRoute),
        FeatureItem(stringResource(R.string.home_card_apk_title),     stringResource(R.string.home_card_apk_sub),     Icons.Default.Android,          true, AppAnalyzerRoute),
    )

    val isExpanded = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded
    val columns = when (windowSizeClass?.widthSizeClass) {
        WindowWidthSizeClass.Medium   -> 3
        WindowWidthSizeClass.Expanded -> 3
        else                          -> 2
    }

    if (isExpanded) {
        // Tablet two-pane: NavigationRail on left + content grid on right
        ExpandedHomeLayout(
            features              = features,
            suggestions           = suggestions,
            columns               = columns,
            navController         = navController,
            showNotifBanner       = notifPermissionNeeded,
            onNotifEnable         = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.clearNotifPermissionFlag()
                }
            },
            onNotifDismiss        = viewModel::clearNotifPermissionFlag
        )
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Text(stringResource(R.string.home_title), fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineSmall)
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(SettingsRoute) }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = stringResource(R.string.home_settings_cd)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            LazyVerticalGrid(
                columns               = GridCells.Fixed(columns),
                contentPadding        = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement   = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.fillMaxSize().padding(paddingValues)
            ) {
                // Notification permission banner (full-width span)
                if (notifPermissionNeeded) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(columns) }) {
                        NotifPermissionBanner(
                            onEnable = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.clearNotifPermissionFlag()
                                }
                            },
                            onDismiss = viewModel::clearNotifPermissionFlag
                        )
                    }
                }

                // Suggestions section (full-width span)
                if (suggestions.isNotEmpty()) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(columns) }) {
                        SuggestionsSection(suggestions = suggestions)
                    }
                }

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
}

@Composable
private fun ExpandedHomeLayout(
    features: List<FeatureItem>,
    suggestions: List<CleaningSuggestion>,
    columns: Int,
    navController: NavController,
    showNotifBanner: Boolean,
    onNotifEnable: () -> Unit,
    onNotifDismiss: () -> Unit
) {
    var selectedIndex by remember { mutableStateOf(0) }
    val navItems = listOf(
        Triple(stringResource(R.string.home_card_media_title), Icons.Default.PhotoLibrary, MediaRoute),
        Triple(stringResource(R.string.home_card_storage_title), Icons.Default.Storage, StorageRoute),
        Triple(stringResource(R.string.home_card_battery_title), Icons.Default.BatteryFull, BatteryRoute),
        Triple(stringResource(R.string.home_card_junk_title), Icons.Default.CleaningServices, JunkRoute),
    )

    Row(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        NavigationRail(
            header = {
                IconButton(onClick = { navController.navigate(SettingsRoute) }) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.home_settings_cd))
                }
            }
        ) {
            navItems.forEachIndexed { index, (label, icon, _) ->
                NavigationRailItem(
                    selected  = selectedIndex == index,
                    onClick   = { selectedIndex = index; navController.navigate(navItems[index].third) },
                    icon      = { Icon(icon, contentDescription = label) },
                    label     = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        LazyVerticalGrid(
            columns               = GridCells.Fixed(columns),
            contentPadding        = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement   = Arrangement.spacedBy(12.dp),
            modifier              = Modifier.fillMaxSize()
        ) {
            if (showNotifBanner) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(columns) }) {
                    NotifPermissionBanner(onEnable = onNotifEnable, onDismiss = onNotifDismiss)
                }
            }
            if (suggestions.isNotEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(columns) }) {
                    SuggestionsSection(suggestions = suggestions)
                }
            }
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
private fun NotifPermissionBanner(onEnable: () -> Unit, onDismiss: () -> Unit) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                modifier              = Modifier.weight(1f),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Notifications,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text  = stringResource(R.string.notif_permission_banner_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEnable) {
                    Text(stringResource(R.string.notif_permission_banner_action))
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector        = Icons.Default.Close,
                        contentDescription = stringResource(R.string.notif_permission_banner_dismiss)
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionsSection(suggestions: List<CleaningSuggestion>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text  = stringResource(R.string.home_suggestions_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggestions) { suggestion ->
                SuggestionCard(suggestion = suggestion)
            }
        }
    }
}

@Composable
private fun SuggestionCard(suggestion: CleaningSuggestion) {
    val (icon, title, saving) = when (suggestion) {
        is CleaningSuggestion.LargeDuplicateGroup -> Triple(
            Icons.Default.PhotoLibrary,
            stringResource(R.string.suggestion_duplicate_title),
            stringResource(R.string.suggestion_save_mb, suggestion.wastedBytes / (1024 * 1024))
        )
        is CleaningSuggestion.UnusedApp -> Triple(
            Icons.Default.Android,
            stringResource(R.string.suggestion_unused_app_title, suggestion.appName),
            stringResource(R.string.suggestion_save_mb, suggestion.sizeBytes / (1024 * 1024))
        )
        is CleaningSuggestion.OversizedDownload -> Triple(
            Icons.Default.Storage,
            stringResource(R.string.suggestion_download_title),
            stringResource(R.string.suggestion_save_mb, suggestion.estimatedWastedBytes / (1024 * 1024))
        )
        is CleaningSuggestion.StaleScreenshot -> Triple(
            Icons.Default.PhotoLibrary,
            stringResource(R.string.suggestion_screenshot_title, suggestion.count),
            stringResource(R.string.suggestion_save_mb, suggestion.totalBytes / (1024 * 1024))
        )
        is CleaningSuggestion.StorageFull -> Triple(
            Icons.Default.Warning,
            stringResource(R.string.suggestion_storage_full_title),
            stringResource(R.string.suggestion_storage_free_pct, (suggestion.freePercent * 100).toInt())
        )
    }

    val cardCd = "$title — $saving"
    ElevatedCard(
        modifier = Modifier
            .width(200.dp)
            .semantics { contentDescription = cardCd }
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(24.dp)
            )
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(saving, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .semantics { contentDescription = title },
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
                        label = { Text(stringResource(R.string.home_card_coming_soon),
                            style = MaterialTheme.typography.labelSmall) })
                }
            }
        }
    }
}
