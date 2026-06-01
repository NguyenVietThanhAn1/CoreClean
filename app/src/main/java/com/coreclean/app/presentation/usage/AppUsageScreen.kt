package com.coreclean.app.presentation.usage

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coreclean.app.domain.model.AppUsageInfo
import com.coreclean.app.domain.model.UsageRange
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUsageScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AppUsageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var dropdownExpanded by remember { mutableStateOf(false) }
    val selectedRange = (uiState as? AppUsageUiState.Success)?.range ?: UsageRange.LAST_7

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("App Usage", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lai")
                    }
                },
                actions = {
                    Box {
                        TextButton(onClick = { dropdownExpanded = true }) {
                            Text("${selectedRange.days} ngay")
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                            DropdownMenuItem(
                                text    = { Text("7 ngay") },
                                onClick = { viewModel.load(UsageRange.LAST_7); dropdownExpanded = false }
                            )
                            DropdownMenuItem(
                                text    = { Text("30 ngay") },
                                onClick = { viewModel.load(UsageRange.LAST_30); dropdownExpanded = false }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is AppUsageUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is AppUsageUiState.NoPermission -> NoPermissionContent(Modifier.fillMaxSize().padding(padding))

            is AppUsageUiState.Error -> Box(
                Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center
            ) { Text(state.message, color = MaterialTheme.colorScheme.error) }

            is AppUsageUiState.Success -> LazyColumn(
                modifier        = Modifier.fillMaxSize().padding(padding),
                contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.items) { info ->
                    AppUsageItem(info)
                }
            }
        }
    }
}

@Composable
private fun AppUsageItem(info: AppUsageInfo) {
    ListItem(
        headlineContent   = { Text(info.appName, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(formatRelativeTime(info.lastTimeUsed), style = MaterialTheme.typography.bodySmall) },
        trailingContent   = { Text(formatDuration(info.totalTimeForegroundMs), style = MaterialTheme.typography.labelMedium) },
        leadingContent    = {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp).size(24.dp),
                    tint     = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    )
}

@Composable
private fun NoPermissionContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier            = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Can quyen Usage Stats de xem thong ke su dung app.",
            style     = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) {
            Text("Mo Cai dat")
        }
    }
}

private fun formatDuration(ms: Long): String {
    val hours   = TimeUnit.MILLISECONDS.toHours(ms)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatRelativeTime(epochMs: Long): String {
    val diff = System.currentTimeMillis() - epochMs
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    return when {
        hours < 1  -> "Vua xong"
        hours < 24 -> "$hours gio truoc"
        else       -> "${hours / 24} ngay truoc"
    }
}
