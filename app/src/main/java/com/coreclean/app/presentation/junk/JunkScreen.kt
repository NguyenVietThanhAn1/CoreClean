package com.coreclean.app.presentation.junk

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coreclean.app.domain.model.JunkCategory
import com.coreclean.app.domain.model.JunkItem
import com.coreclean.app.ui.media.toReadableSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JunkScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: JunkViewModel = hiltViewModel()
) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val safFolders   by viewModel.safFolderUris.collectAsStateWithLifecycle()
    val context      = LocalContext.current

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // Persist permission across reboots
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            viewModel.addSafFolder(uri)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Junk Cleaner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lai")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is JunkUiState.Idle -> IdleContent(
                    onScan       = viewModel::scan,
                    safFolders   = safFolders,
                    onPickFolder = { folderPicker.launch(null) },
                    onRemoveFolder = viewModel::removeSafFolder
                )

                is JunkUiState.Scanning -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator()
                        Text("Dang quet...")
                    }
                }

                is JunkUiState.Error -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) { Text(state.message, color = MaterialTheme.colorScheme.error) }

                is JunkUiState.Ready -> {
                    state.lastResult?.let { result ->
                        if (result.appCacheCount > 0) {
                            Surface(
                                color    = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text("${result.appCacheCount} app cache: can xoa thu cong",
                                        style = MaterialTheme.typography.bodySmall)
                                    TextButton(onClick = {
                                        context.startActivity(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
                                    }) { Text("Mo Settings") }
                                }
                            }
                        }
                    }

                    val byCategory = state.items.groupBy { it.category }
                    val totalSelected = state.items.filter { it.path in state.selected }.sumOf { it.sizeBytes }

                    LazyColumn(
                        modifier       = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        JunkCategory.entries.forEach { cat ->
                            val catItems = byCategory[cat] ?: return@forEach
                            item {
                                JunkCategorySection(
                                    category = cat,
                                    items    = catItems,
                                    selected = state.selected,
                                    onToggle = viewModel::toggleItem,
                                    onOpenSettings = {
                                        context.startActivity(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
                                    }
                                )
                            }
                        }
                    }

                    Surface(shadowElevation = 8.dp) {
                        Row(
                            modifier              = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text("Da chon: ${totalSelected.toReadableSize()}", fontWeight = FontWeight.SemiBold)
                            Button(
                                onClick  = viewModel::clean,
                                enabled  = !state.cleaning && state.selected.isNotEmpty()
                            ) {
                                if (state.cleaning) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Text("Don dep")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleContent(
    onScan: () -> Unit,
    safFolders: Set<String> = emptySet(),
    onPickFolder: () -> Unit = {},
    onRemoveFolder: (String) -> Unit = {}
) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // SAF folder section
        Text("Thu muc da chon de quet", style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold)
        if (safFolders.isEmpty()) {
            Text("Chua chon thu muc nao. Nhan \"Chon thu muc\" de bat dau.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            safFolders.forEach { uriStr ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(uriStr.substringAfterLast('%').take(40),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f))
                    IconButton(onClick = { onRemoveFolder(uriStr) }) {
                        Icon(Icons.Default.Close, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        OutlinedButton(onClick = onPickFolder, modifier = Modifier.fillMaxWidth()) {
            Text("Chon thu muc de quet")
        }

        HorizontalDivider()

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Default.CleaningServices, contentDescription = null,
                    modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Quet de tim file rac", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onScan) { Text("Bat dau quet") }
            }
        }
    }
}

@Composable
private fun JunkCategorySection(
    category: JunkCategory,
    items: List<JunkItem>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val totalSize = items.sumOf { it.sizeBytes }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = categoryLabel(category),
                    fontWeight = FontWeight.SemiBold,
                    style      = MaterialTheme.typography.titleSmall
                )
                Text(totalSize.toReadableSize(), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            if (category == JunkCategory.APP_CACHE) {
                Text(
                    "Android 8+ khong cho xoa truc tiep — can mo Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onOpenSettings) { Text("Mo App Manager") }
            } else {
                items.take(5).forEach { item ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked  = item.path in selected,
                            onCheckedChange = { onToggle(item.path) }
                        )
                        Text(
                            item.path.substringAfterLast('/'),
                            modifier = Modifier.weight(1f),
                            style    = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                        Text(item.sizeBytes.toReadableSize(), style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (items.size > 5) {
                    Text("... va ${items.size - 5} file khac",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 40.dp))
                }
            }
        }
    }
}

private fun categoryLabel(cat: JunkCategory) = when (cat) {
    JunkCategory.APP_CACHE    -> "App Cache"
    JunkCategory.EMPTY_FOLDERS -> "Thu muc rong"
    JunkCategory.TEMP_FILES   -> "File tam"
    JunkCategory.RESIDUAL_APK -> "APK con lai"
}
