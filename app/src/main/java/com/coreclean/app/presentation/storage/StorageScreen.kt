package com.coreclean.app.presentation.storage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coreclean.app.domain.model.StorageCategory
import com.coreclean.app.domain.model.StorageInfo
import com.coreclean.app.ui.media.toReadableSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: StorageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Storage Analyzer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lai")
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
                info          = state.info,
                paddingValues = paddingValues,
                onRefresh     = viewModel::loadStorageInfo
            )
        }
    }
}

@Composable
private fun StorageContent(
    info: StorageInfo,
    paddingValues: PaddingValues,
    onRefresh: () -> Unit
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
                StorageStat("Da dung", info.usedBytes.toReadableSize())
                StorageStat("Trong",   info.freeBytes.toReadableSize())
                StorageStat("Tong",    info.totalBytes.toReadableSize())
            }
        }

        Text("Chi tiet theo danh muc",
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        info.breakdown.filter { it.sizeBytes > 0 }.forEach { cat ->
            StorageCategoryRow(category = cat, totalUsed = info.usedBytes)
        }

        Button(onClick = { /* Sprint 4 */ }, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text("Quet chi tiet (Sprint 4)")
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
    Canvas(modifier = modifier) {
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
