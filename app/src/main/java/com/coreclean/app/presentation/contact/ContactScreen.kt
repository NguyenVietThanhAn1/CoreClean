package com.coreclean.app.presentation.contact

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coreclean.app.R
import com.coreclean.app.domain.model.Contact
import com.coreclean.app.domain.model.ContactDuplicateGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ContactViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabLabels = listOf(
        stringResource(R.string.contact_tab_all),
        stringResource(R.string.contact_tab_duplicate),
        stringResource(R.string.contact_tab_incomplete)
    )

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.contact_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (!uiState.hasPermission) {
            NoContactPermissionContent(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabLabels.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index },
                        text     = {
                            val badge = when (index) {
                                1    -> uiState.duplicates.size
                                2    -> uiState.incomplete.size
                                else -> uiState.allContacts.size
                            }
                            Text("$title ($badge)")
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> ContactList(uiState.allContacts)
                1 -> DuplicateList(uiState.duplicates, onMerge = { viewModel.startMerge(it) })
                2 -> ContactList(uiState.incomplete, showIncompleteNote = true)
            }
        }

        // Merge dialog
        uiState.mergingGroupIndex?.let { idx ->
            val group = uiState.duplicates.getOrNull(idx)?.contacts ?: emptyList()
            MergeContactDialog(
                group     = group,
                onConfirm = viewModel::confirmMerge,
                onDismiss = viewModel::cancelMerge
            )
        }

        // Snackbar for merge result
        uiState.mergeMessage?.let { msg ->
            androidx.compose.runtime.LaunchedEffect(msg) {
                kotlinx.coroutines.delay(2_000)
                viewModel.dismissMergeMessage()
            }
        }
    }
}

@Composable
private fun ContactList(contacts: List<Contact>, showIncompleteNote: Boolean = false) {
    if (contacts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.contact_no_items), style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        contentPadding    = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(contacts) { contact ->
            ListItem(
                headlineContent   = { Text(contact.displayName.ifBlank { stringResource(R.string.contact_no_name) }, fontWeight = FontWeight.SemiBold) },
                supportingContent = {
                    Column {
                        if (contact.phones.isNotEmpty()) Text(contact.phones.joinToString(", "))
                        if (showIncompleteNote && contact.phones.isEmpty())
                            Text(stringResource(R.string.contact_missing_phone), color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall)
                    }
                },
                leadingContent = {
                    Icon(
                        if (contact.hasPhoto) Icons.Default.Person else Icons.Default.PersonOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun DuplicateList(
    groups: List<ContactDuplicateGroup>,
    onMerge: (Int) -> Unit = {}
) {
    if (groups.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.contact_no_duplicate), style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(groups) { index, group ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.contact_duplicate_group, group.contacts.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { onMerge(index) }) {
                            Text(stringResource(R.string.contact_merge_confirm))
                        }
                    }
                    group.contacts.forEach { c ->
                        val noName = stringResource(R.string.contact_no_name)
                        Text("• ${c.displayName.ifBlank { noName }}" +
                            if (c.phones.isNotEmpty()) " — ${c.phones.first()}" else "")
                    }
                }
            }
        }
    }
}

@Composable
private fun NoContactPermissionContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier            = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.contact_no_permission),
            style     = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
            })
        }) { Text(stringResource(R.string.contact_open_settings)) }
    }
}
