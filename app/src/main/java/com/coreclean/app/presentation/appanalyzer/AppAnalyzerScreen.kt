package com.coreclean.app.presentation.appanalyzer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coreclean.app.R
import com.coreclean.app.domain.model.InstalledApp
import com.coreclean.app.presentation.media.toReadableSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppAnalyzerScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AppAnalyzerViewModel = hiltViewModel()
) {
    val state   by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var sortExpanded   by remember { mutableStateOf(false) }
    var filterExpanded by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_analyzer_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back))
                    }
                },
                actions = {
                    // Sort dropdown
                    Box {
                        TextButton(onClick = { sortExpanded = true }) {
                            Text(sortLabel(state.sortOrder))
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                            AppSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text    = { Text(sortLabel(order)) },
                                    onClick = { viewModel.setSortOrder(order); sortExpanded = false }
                                )
                            }
                        }
                    }
                    // Filter dropdown
                    Box {
                        TextButton(onClick = { filterExpanded = true }) {
                            Text(filterLabel(state.filter))
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                            AppFilter.entries.forEach { f ->
                                DropdownMenuItem(
                                    text    = { Text(filterLabel(f)) },
                                    onClick = { viewModel.setFilter(f); filterExpanded = false }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state.selectedPackages.isNotEmpty()) {
                Surface(shadowElevation = 8.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.app_analyzer_selected, state.selectedPackages.size))
                        Button(onClick = {
                            state.selectedPackages.forEach { pkg ->
                                context.startActivity(
                                    Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg"))
                                )
                            }
                            viewModel.clearSelection()
                        }) {
                            Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.app_analyzer_uninstall))
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val apps = viewModel.displayedApps()
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppRow(
                    app        = app,
                    selected   = app.packageName in state.selectedPackages,
                    onToggle   = { viewModel.toggleSelection(app.packageName) },
                    onUninstall = {
                        context.startActivity(
                            Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    selected: Boolean,
    onToggle: () -> Unit,
    onUninstall: () -> Unit
) {
    ListItem(
        headlineContent   = { Text(app.appName, fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            Column {
                Text(app.versionName, style = MaterialTheme.typography.bodySmall)
                Text(app.apkSizeBytes.toReadableSize(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        leadingContent = {
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
        },
        trailingContent = {
            if (!app.isSystem) {
                IconButton(onClick = onUninstall) {
                    Icon(Icons.Default.Delete, contentDescription = null,
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
    HorizontalDivider()
}

private fun sortLabel(order: AppSortOrder) = when (order) {
    AppSortOrder.SIZE_DESC         -> "Size ↓"
    AppSortOrder.INSTALL_DATE_DESC -> "Install ↓"
    AppSortOrder.NAME_ASC          -> "Name A-Z"
}

private fun filterLabel(f: AppFilter) = when (f) {
    AppFilter.ALL    -> "All"
    AppFilter.USER   -> "User"
    AppFilter.SYSTEM -> "System"
}
