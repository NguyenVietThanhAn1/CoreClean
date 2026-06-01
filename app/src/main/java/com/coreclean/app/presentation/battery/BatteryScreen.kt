package com.coreclean.app.presentation.battery

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.coreclean.app.R
import com.coreclean.app.domain.usecase.battery.BatteryPrediction
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
import com.coreclean.app.domain.model.BatteryInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: BatteryViewModel = hiltViewModel()
) {
    val uiState    by viewModel.uiState.collectAsStateWithLifecycle()
    val prediction by viewModel.prediction.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Battery Monitor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lai")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is BatteryUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(paddingValues), Alignment.Center
            ) { CircularProgressIndicator() }

            is BatteryUiState.Error -> Box(
                Modifier.fillMaxSize().padding(paddingValues), Alignment.Center
            ) {
                Text(state.message, color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
            }

            is BatteryUiState.Success -> BatteryContent(
                info          = state.info,
                prediction    = prediction,
                paddingValues = paddingValues
            )
        }
    }
}

@Composable
private fun BatteryContent(
    info: BatteryInfo,
    prediction: BatteryPrediction?,
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement   = Arrangement.spacedBy(16.dp),
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        // Battery prediction card
        BatteryPredictionCard(prediction = prediction)

        // Circular battery level indicator
        Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
            BatteryCircle(levelPercent = info.levelPercent, modifier = Modifier.fillMaxSize())
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text       = "${info.levelPercent}%",
                    style      = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color      = batteryColor(info.levelPercent)
                )
                Text(
                    text  = if (info.isCharging) "Dang sac" else "Khong sac",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Info cards in 2-column grid
        val cards = listOf(
            "Suc khoe"      to info.healthLabel,
            "Nhiet do"      to "${info.temperatureC} oC",
            "Dien ap"       to "${info.voltageMv} mV",
            "Cong nghe"     to info.technology,
            "Nguon sac"     to info.chargePlug,
            "Con lai (uoc)" to if (info.chargeCounterMah > 0) "${info.chargeCounterMah} mAh" else "N/A"
        )

        cards.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, value) ->
                    BatteryInfoCard(label = label, value = value, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BatteryPredictionCard(prediction: BatteryPrediction?) {
    val text = when {
        prediction == null          -> null
        prediction.isCharging       -> null
        prediction.estimated == null ->
            stringResource(R.string.battery_prediction_collecting)
        else -> {
            val hours   = prediction.estimated.toHours()
            val minutes = (prediction.estimated.toMinutes() % 60)
            stringResource(R.string.battery_prediction_estimate, hours, minutes)
        }
    } ?: return

    val cardCd = stringResource(R.string.cd_battery_prediction, text)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = cardCd }
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text  = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (prediction?.estimated != null) {
                Text(
                    text  = stringResource(R.string.battery_prediction_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BatteryInfoCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier  = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun BatteryCircle(levelPercent: Int, modifier: Modifier = Modifier) {
    val color = batteryColor(levelPercent)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val cd    = stringResource(R.string.cd_battery_circle, levelPercent)
    Canvas(modifier = modifier.semantics { contentDescription = cd }) {
        val strokeWidth = size.minDimension * 0.12f
        val radius  = (size.minDimension / 2f) - strokeWidth
        val topLeft = Offset((size.width / 2f) - radius, (size.height / 2f) - radius)
        val arcSize = Size(radius * 2f, radius * 2f)

        drawArc(color = track, startAngle = -90f, sweepAngle = 360f, useCenter = false,
            style = Stroke(width = strokeWidth), topLeft = topLeft, size = arcSize)
        drawArc(color = color, startAngle = -90f,
            sweepAngle = 360f * levelPercent.coerceIn(0, 100) / 100f,
            useCenter = false, style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            topLeft = topLeft, size = arcSize)
    }
}

private fun batteryColor(levelPercent: Int): Color = when {
    levelPercent > 60 -> Color(0xFF4CAF50)  // Green
    levelPercent > 25 -> Color(0xFFFF9800)  // Orange
    else              -> Color(0xFFF44336)  // Red
}
