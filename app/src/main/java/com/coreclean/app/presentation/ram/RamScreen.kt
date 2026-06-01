package com.coreclean.app.presentation.ram

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coreclean.app.R
import com.coreclean.app.domain.model.RamInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RamScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: RamViewModel = hiltViewModel()
) {
    val ramInfo by viewModel.ramInfo.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ram_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back))
                    }
                }
            )
        }
    ) { padding ->
        if (ramInfo == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        RamContent(info = ramInfo!!, modifier = Modifier.fillMaxSize().padding(padding))
    }
}

@Composable
private fun RamContent(info: RamInfo, modifier: Modifier = Modifier) {
    val usedFraction = if (info.totalMb > 0) info.usedMb.toFloat() / info.totalMb else 0f

    LazyColumn(
        modifier            = modifier,
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.ram_used, info.usedMb),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.ram_total, info.totalMb),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LinearProgressIndicator(
                        progress = { usedFraction },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color    = if (info.isLowMemory) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.ram_available, info.availableMb),
                            style = MaterialTheme.typography.bodySmall)
                        if (info.isLowMemory)
                            Text(stringResource(R.string.ram_low_memory),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (info.topApps.isNotEmpty()) {
            item {
                Text(stringResource(R.string.ram_top_processes),
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.ram_process_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(info.topApps) { app ->
                ListItem(
                    headlineContent   = { Text(app.appName) },
                    trailingContent   = { Text(stringResource(R.string.ram_mb, app.memoryMb)) },
                    leadingContent    = {
                        Icon(Icons.Default.Memory, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                )
                HorizontalDivider()
            }
        }
    }
}
