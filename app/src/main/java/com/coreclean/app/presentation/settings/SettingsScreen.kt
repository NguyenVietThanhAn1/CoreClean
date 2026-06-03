package com.coreclean.app.presentation.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coreclean.app.BuildConfig
import com.coreclean.app.R
import com.coreclean.app.core.preferences.AppLanguage
import com.coreclean.app.core.preferences.ThemeMode
import com.coreclean.app.domain.model.Frequency
import com.coreclean.app.domain.model.JunkCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back))
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
            SectionHeader(stringResource(R.string.settings_section_theme))
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
                                ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                                ThemeMode.LIGHT  -> stringResource(R.string.settings_theme_light)
                                ThemeMode.DARK   -> stringResource(R.string.settings_theme_dark)
                            })
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingsToggleRow(
                    title           = stringResource(R.string.settings_dynamic_color),
                    checked         = state.dynamicColor,
                    onCheckedChange = viewModel::setDynamicColor
                )
            }

            if (state.themeMode == ThemeMode.DARK ||
                (state.themeMode == ThemeMode.SYSTEM && isSystemInDarkTheme())) {
                SettingsToggleRow(
                    title           = stringResource(R.string.settings_amoled_black),
                    checked         = state.amoledEnabled,
                    onCheckedChange = viewModel::setAmoledEnabled
                )
            }

            // ── Background scan ────────────────────────────────────────
            SectionHeader(stringResource(R.string.settings_section_scan))
            SettingsToggleRow(
                title           = stringResource(R.string.settings_bg_scan),
                checked         = state.backgroundScan,
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
                                Text(stringResource(R.string.settings_interval_hours, h))
                            }
                        }
                    }
                }
            }

            Button(
                onClick  = viewModel::runScanNow,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.settings_scan_now)) }

            // ── Auto-cleaning ──────────────────────────────────────────
            SectionHeader(stringResource(R.string.settings_section_auto_clean))
            SettingsToggleRow(
                title           = stringResource(R.string.settings_auto_clean_enable),
                checked         = state.scheduleConfig.enabled,
                onCheckedChange = { viewModel.setAutoCleanEnabled(it) }
            )

            if (state.scheduleConfig.enabled) {
                // Frequency picker
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.selectableGroup()) {
                        Frequency.entries.forEach { freq ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = state.scheduleConfig.frequency == freq,
                                        onClick  = { viewModel.setAutoCleanFrequency(freq) },
                                        role     = Role.RadioButton
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = state.scheduleConfig.frequency == freq, onClick = null)
                                Spacer(Modifier.width(12.dp))
                                Text(when (freq) {
                                    Frequency.DAILY   -> stringResource(R.string.settings_auto_clean_daily)
                                    Frequency.WEEKLY  -> stringResource(R.string.settings_auto_clean_weekly)
                                    Frequency.MONTHLY -> stringResource(R.string.settings_auto_clean_monthly)
                                })
                            }
                        }
                    }
                }

                // Category multi-select
                SectionHeader(stringResource(R.string.settings_auto_clean_categories))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        val safeCategories = listOf(JunkCategory.TEMP_FILES, JunkCategory.EMPTY_FOLDERS, JunkCategory.RESIDUAL_APK)
                        safeCategories.forEach { cat ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleAutoCleanCategory(cat) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked         = cat in state.scheduleConfig.categories,
                                    onCheckedChange = { viewModel.toggleAutoCleanCategory(cat) }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(when (cat) {
                                    JunkCategory.TEMP_FILES    -> stringResource(R.string.junk_temp_files)
                                    JunkCategory.EMPTY_FOLDERS -> stringResource(R.string.junk_empty_folders)
                                    JunkCategory.RESIDUAL_APK  -> stringResource(R.string.junk_residual_apk)
                                    JunkCategory.APP_CACHE     -> stringResource(R.string.junk_app_cache)
                                })
                            }
                        }
                    }
                }
            }

            // ── Notifications ──────────────────────────────────────────
            SectionHeader(stringResource(R.string.settings_section_notifications))
            SettingsToggleRow(
                title           = stringResource(R.string.settings_recommendations_notif),
                checked         = state.recommendationsEnabled,
                onCheckedChange = viewModel::setRecommendationsEnabled
            )

            // ── AI Dedupe ──────────────────────────────────────────────
            SectionHeader(stringResource(R.string.settings_section_ai))
            SettingsToggleRow(
                title           = stringResource(R.string.settings_perceptual_dedupe),
                checked         = state.perceptualDedupe,
                onCheckedChange = viewModel::setPerceptualDedupe
            )

            // ── Language ───────────────────────────────────────────────
            SectionHeader(stringResource(R.string.settings_section_language))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.selectableGroup()) {
                    AppLanguage.entries.forEach { lang ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = state.language == lang,
                                    onClick  = { viewModel.setLanguage(lang) },
                                    role     = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = state.language == lang, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(when (lang) {
                                AppLanguage.SYSTEM     -> stringResource(R.string.settings_lang_system)
                                AppLanguage.VIETNAMESE -> stringResource(R.string.settings_lang_vi)
                                AppLanguage.ENGLISH    -> stringResource(R.string.settings_lang_en)
                                AppLanguage.FRENCH     -> stringResource(R.string.settings_lang_fr)
                            })
                        }
                    }
                }
            }

            // ── Crash Reporting (gms only) ────────────────────────────
            if (BuildConfig.FLAVOR == "gms") {
                SectionHeader(stringResource(R.string.settings_section_crash))
                SettingsToggleRow(
                    title           = stringResource(R.string.settings_crash_toggle),
                    checked         = state.crashReporting,
                    onCheckedChange = viewModel::setCrashReporting
                )
            }

            // ── Privacy ────────────────────────────────────────────────
            SectionHeader(stringResource(R.string.settings_section_privacy))
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_privacy_dashboard)) },
                    modifier        = Modifier.clickable(onClick = onNavigateToPrivacy)
                )
            }

            // ── Debug ──────────────────────────────────────────────────
            SectionHeader(stringResource(R.string.settings_section_debug))
            OutlinedButton(
                onClick  = viewModel::resetOnboarding,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.settings_reset_onboarding)) }

            // ── App info ───────────────────────────────────────────────
            SectionHeader(stringResource(R.string.settings_section_about))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version)) },
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
