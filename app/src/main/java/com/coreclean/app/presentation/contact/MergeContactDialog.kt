package com.coreclean.app.presentation.contact

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.coreclean.app.R
import com.coreclean.app.domain.model.Contact

@Composable
fun MergeContactDialog(
    group: List<Contact>,
    onConfirm: (List<Contact>) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.contact_merge_title),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                Text(stringResource(R.string.contact_merge_subtitle, group.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(group) { contact ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(contact.displayName.ifBlank { "(Khong ten)" },
                                    fontWeight = FontWeight.SemiBold)
                                if (contact.phones.isNotEmpty())
                                    Text(contact.phones.joinToString(", "),
                                        style = MaterialTheme.typography.bodySmall)
                                if (contact.emails.isNotEmpty())
                                    Text(contact.emails.joinToString(", "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Text(stringResource(R.string.contact_merge_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(group) }) {
                        Text(stringResource(R.string.contact_merge_confirm))
                    }
                }
            }
        }
    }
}
