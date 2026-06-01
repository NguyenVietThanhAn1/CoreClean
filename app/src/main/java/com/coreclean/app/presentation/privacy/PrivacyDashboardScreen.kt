package com.coreclean.app.presentation.privacy

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDashboardScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: PrivacyViewModel = hiltViewModel()
) {
    val state   by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    state.message?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(2_000)
            viewModel.dismissMessage()
        }
        Snackbar(modifier = Modifier.padding(16.dp)) { Text(msg) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Permissions section
            SectionTitle(stringResource(R.string.privacy_permissions))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.grantedPermissions.forEach { perm ->
                        PermissionRow(perm.substringAfterLast('.'), granted = true)
                    }
                    state.deniedPermissions.forEach { perm ->
                        PermissionRow(perm.substringAfterLast('.'), granted = false)
                    }
                }
            }

            // Data section
            SectionTitle(stringResource(R.string.privacy_stored_data))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    DataRow(stringResource(R.string.privacy_scan_results), "${state.scanResultCount}")
                    DataRow(stringResource(R.string.privacy_pending_review), "${state.pendingReviewCount}")
                    DataRow(stringResource(R.string.privacy_datastore_keys), "${state.dataStoreKeyCount}")
                    DataRow(
                        stringResource(R.string.privacy_battery_history),
                        stringResource(R.string.privacy_battery_count, state.batteryHistoryCount)
                    )
                }
            }

            // Actions section
            SectionTitle(stringResource(R.string.privacy_actions))

            OutlinedButton(
                onClick  = viewModel::clearHistory,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text(stringResource(R.string.privacy_clear_history)) }

            OutlinedButton(
                onClick  = viewModel::clearBatteryHistory,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text(stringResource(R.string.privacy_clear_battery_history)) }

            Button(
                onClick  = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.privacy_open_app_settings)) }

            // Privacy Policy link
            TextButton(
                onClick  = {
                    val url = "https://raw.githubusercontent.com/NguyenVietThanhAn1/CoreClean/master/docs/PrivacyPolicy.md"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.privacy_policy_link)) }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text,
        style    = MaterialTheme.typography.labelLarge,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun PermissionRow(name: String, granted: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium)
        Icon(
            imageVector        = if (granted) Icons.Default.Check else Icons.Default.Close,
            contentDescription = if (granted)
                stringResource(R.string.cd_permission_granted, name)
            else
                stringResource(R.string.cd_permission_denied, name),
            tint               = if (granted) MaterialTheme.colorScheme.primary
                                 else        MaterialTheme.colorScheme.error,
            modifier           = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun DataRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
