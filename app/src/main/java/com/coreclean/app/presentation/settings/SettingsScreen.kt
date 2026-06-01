package com.coreclean.app.presentation.settings

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coreclean.app.BuildConfig
import com.coreclean.app.core.preferences.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Cai dat", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lai")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Theme ──────────────────────────────────────────────────
            SectionHeader("Giao dien")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.selectableGroup()) {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = state.themeMode == mode,
                                    onClick  = { viewModel.setThemeMode(mode) },
                                    role     = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = state.themeMode == mode, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(when (mode) {
                                ThemeMode.SYSTEM -> "Theo he thong"
                                ThemeMode.LIGHT  -> "Sang"
                                ThemeMode.DARK   -> "Toi"
                            })
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingsToggleRow(
                    title   = "Mau dong (Material You)",
                    checked = state.dynamicColor,
                    onCheckedChange = viewModel::setDynamicColor
                )
            }

            // ── Background scan ────────────────────────────────────────
            SectionHeader("Quet nen")
            SettingsToggleRow(
                title   = "Bat quet nen",
                checked = state.backgroundScan,
                onCheckedChange = viewModel::setBackgroundScan
            )

            if (state.backgroundScan) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.selectableGroup()) {
                        listOf(6, 12, 24).forEach { h ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = state.scanIntervalHours == h,
                                        onClick  = { viewModel.setScanInterval(h) },
                                        role     = Role.RadioButton
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = state.scanIntervalHours == h, onClick = null)
                                Spacer(Modifier.width(12.dp))
                                Text("$h gio")
                            }
                        }
                    }
                }
            }

            Button(
                onClick  = viewModel::runScanNow,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Quet ngay bay gio") }

            // ── Debug ──────────────────────────────────────────────────
            SectionHeader("Debug")
            OutlinedButton(
                onClick  = viewModel::resetOnboarding,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Reset onboarding") }

            // ── App info ───────────────────────────────────────────────
            SectionHeader("Thong tin ung dung")
            ListItem(
                headlineContent = { Text("Phien ban") },
                trailingContent = { Text(BuildConfig.VERSION_NAME) }
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style    = MaterialTheme.typography.labelLarge,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
