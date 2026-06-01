package com.coreclean.app.presentation.storage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.coreclean.app.R
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coreclean.app.domain.model.MediaImage
import com.coreclean.app.domain.model.StorageCategory
import com.coreclean.app.domain.model.StorageInfo
import com.coreclean.app.presentation.navigation.ReviewRoute
import com.coreclean.app.presentation.media.toReadableSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToReview: ((Any) -> Unit)? = null,
    viewModel: StorageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.storage_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is StorageUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is StorageUiState.Error -> Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(state.message, color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
            }

            is StorageUiState.Success -> StorageContent(
                info               = state.info,
                largeFiles         = state.largeFiles,
                paddingValues      = paddingValues,
                onRefresh          = viewModel::loadStorageInfo,
                onNavigateToReview = onNavigateToReview
            )
        }
    }
}

@Composable
private fun StorageContent(
    info: StorageInfo,
    largeFiles: List<MediaImage> = emptyList(),
    paddingValues: PaddingValues,
    onRefresh: () -> Unit,
    onNavigateToReview: ((Any) -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            StorageDonutChart(
                usedBytes  = info.usedBytes,
                totalBytes = info.totalBytes,
                categories = info.breakdown,
                modifier   = Modifier.size(200.dp)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(info.usedBytes.toReadableSize(),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("/ ${info.totalBytes.toReadableSize()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                StorageStat(stringResource(R.string.storage_used), info.usedBytes.toReadableSize())
                StorageStat(stringResource(R.string.storage_free), info.freeBytes.toReadableSize())
                StorageStat(stringResource(R.string.storage_total), info.totalBytes.toReadableSize())
            }
        }

        Text(stringResource(R.string.storage_breakdown),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        info.breakdown.filter { it.sizeBytes > 0 }.forEach { cat ->
            StorageCategoryRow(category = cat, totalUsed = info.usedBytes)
        }

        // Large files section
        if (largeFiles.isNotEmpty()) {
            var selectedIds by remember { mutableStateOf(emptySet<Long>()) }

            Text(stringResource(R.string.storage_large_files),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            largeFiles.take(20).forEach { file ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked  = file.id in selectedIds,
                        onCheckedChange = {
                            selectedIds = if (it) selectedIds + file.id else selectedIds - file.id
                        }
                    )
                    Column(Modifier.weight(1f)) {
                        Text(file.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        Text(file.size.toReadableSize(), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (selectedIds.isNotEmpty() && onNavigateToReview != null) {
                Button(
                    onClick  = {
                        onNavigateToReview(ReviewRoute("storage", selectedIds.toList()))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.storage_delete_selected, selectedIds.size)) }
            }
        }

        Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.storage_refresh))
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StorageStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StorageCategoryRow(category: StorageCategory, totalUsed: Long) {
    val fraction = if (totalUsed > 0) category.sizeBytes.toFloat() / totalUsed.toFloat() else 0f
    val catColor = Color(category.colorArgb)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Canvas(Modifier.size(12.dp)) { drawCircle(catColor) }
                Text(category.name, style = MaterialTheme.typography.bodyMedium)
            }
            Text("${category.sizeBytes.toReadableSize()} - ${"%.0f".format(fraction * 100)}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress   = { fraction },
            modifier   = Modifier.fillMaxWidth().height(6.dp),
            color      = catColor,
            trackColor = catColor.copy(alpha = 0.15f)
        )
    }
}

@Composable
private fun StorageDonutChart(
    usedBytes: Long,
    totalBytes: Long,
    categories: List<StorageCategory>,
    modifier: Modifier = Modifier
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val usedReadable  = usedBytes.toReadableSize()
    val totalReadable = totalBytes.toReadableSize()
    val cd = stringResource(R.string.cd_storage_donut, usedReadable, totalReadable)
    Canvas(modifier = modifier.semantics { contentDescription = cd }) {
        val strokeWidth = size.minDimension * 0.14f
        val radius  = (size.minDimension / 2f) - strokeWidth
        val topLeft = Offset((size.width / 2f) - radius, (size.height / 2f) - radius)
        val arcSize = Size(radius * 2f, radius * 2f)

        drawArc(color = surfaceVariant, startAngle = -90f, sweepAngle = 360f,
            useCenter = false, style = Stroke(width = strokeWidth), topLeft = topLeft, size = arcSize)

        var startAngle = -90f
        categories.filter { it.sizeBytes > 0 && totalBytes > 0 }.forEach { cat ->
            val sweep = 360f * cat.sizeBytes.toFloat() / totalBytes.toFloat()
            drawArc(color = Color(cat.colorArgb), startAngle = startAngle, sweepAngle = sweep - 1f,
                useCenter = false, style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = topLeft, size = arcSize)
            startAngle += sweep
        }
    }
}
