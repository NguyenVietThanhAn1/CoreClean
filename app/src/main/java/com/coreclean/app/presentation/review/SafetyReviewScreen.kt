package com.coreclean.app.presentation.review

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.coreclean.app.ui.media.toReadableSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyReviewScreen(
    onNavigateBack:   () -> Unit = {},
    onDeleteComplete: () -> Unit = {},
    viewModel: SafetyReviewViewModel = hiltViewModel()
) {
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedImages by viewModel.selectedImages.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { viewModel.onSystemDeleteDone() }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ReviewUiState.RequestIntent ->
                deleteLauncher.launch(IntentSenderRequest.Builder(state.intentSender).build())
            is ReviewUiState.Done          -> onDeleteComplete()
            else                           -> Unit
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Xac nhan xoa", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lai")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text  = "Se giai phong ${viewModel.totalSize.toReadableSize()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick  = viewModel::confirmDelete,
                        enabled  = uiState is ReviewUiState.Idle,
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState is ReviewUiState.Deleting) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color       = MaterialTheme.colorScheme.onError
                            )
                        } else {
                            Text("Xac nhan xoa ${selectedImages.size} anh")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (selectedImages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(paddingValues), Alignment.Center) {
                Text("Khong co anh nao duoc chon",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        LazyVerticalGrid(
            columns               = GridCells.Adaptive(minSize = 110.dp),
            contentPadding        = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement   = Arrangement.spacedBy(3.dp),
            modifier              = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            items(items = selectedImages, key = { it.id }) { image ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(image.uri).crossfade(true).build(),
                    contentDescription = image.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.aspectRatio(1f).clip(RoundedCornerShape(6.dp))
                )
            }
        }
    }
}
