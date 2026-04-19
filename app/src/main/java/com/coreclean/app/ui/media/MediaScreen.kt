package com.coreclean.app.ui.media

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.coreclean.app.domain.model.MediaImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScreen(
    viewModel: MediaViewModel = hiltViewModel()
) {
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedImages by viewModel.selectedImages.collectAsStateWithLifecycle()
    var activeTab     by remember { mutableIntStateOf(0) }  // 0=All, 1=Duplicates

    // Permission launcher
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_IMAGES
    else
        Manifest.permission.READ_EXTERNAL_STORAGE

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.loadImages()
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionToRequest)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quản lý ảnh",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Tab bar ──────────────────────────────────────────────
            TabRow(selectedTabIndex = activeTab) {
                Tab(
                    selected  = activeTab == 0,
                    onClick   = { activeTab = 0 },
                    text      = { Text("Tất cả ảnh") }
                )
                Tab(
                    selected  = activeTab == 1,
                    onClick   = {
                        activeTab = 1
                        viewModel.findDuplicates()
                    },
                    text      = { Text("Ảnh trùng lặp") }
                )
            }

            // ── Content ──────────────────────────────────────────────
            when (val state = uiState) {
                is MediaUiState.Idle    -> IdleContent()
                is MediaUiState.Loading -> LoadingContent()
                is MediaUiState.Error   -> ErrorContent(message = state.message)
                is MediaUiState.Success -> {
                    if (activeTab == 0) {
                        AllImagesGrid(
                            images         = state.images,
                            selectedImages = selectedImages,
                            onToggleSelect = viewModel::toggleImageSelection
                        )
                    } else {
                        DuplicatesContent(
                            groups         = state.duplicateGroups,
                            selectedImages = selectedImages,
                            onToggleSelect = viewModel::toggleImageSelection
                        )
                    }
                }
            }
        }

        // ── Bottom action bar khi có ảnh được chọn ───────────────────
        if (selectedImages.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.BottomCenter
            ) {
                SelectionBottomBar(
                    count       = selectedImages.size,
                    onCancel    = viewModel::clearSelection,
                    onDelete    = { /* TODO: Safety Review screen */ }
                )
            }
        }
    }
}

// ── All images grid ───────────────────────────────────────────────────────────
@Composable
private fun AllImagesGrid(
    images: List<MediaImage>,
    selectedImages: Set<Long>,
    onToggleSelect: (Long) -> Unit
) {
    if (images.isEmpty()) {
        EmptyContent(message = "Không tìm thấy ảnh nào")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement   = Arrangement.spacedBy(3.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Header tổng số ảnh
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text     = "${images.size} ảnh",
                style    = MaterialTheme.typography.labelMedium,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        items(items = images, key = { it.id }) { image ->
            ImageGridItem(
                image      = image,
                isSelected = image.id in selectedImages,
                onToggle   = { onToggleSelect(image.id) }
            )
        }
    }
}

// ── Single image cell ─────────────────────────────────────────────────────────
@Composable
private fun ImageGridItem(
    image: MediaImage,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onToggle() }
            .then(
                if (isSelected)
                    Modifier.border(
                        width = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(6.dp)
                    )
                else Modifier
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(image.uri)
                .crossfade(true)
                .build(),
            contentDescription = image.name,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize()
        )

        // Overlay khi được chọn
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            )
            Icon(
                imageVector        = Icons.Filled.CheckCircle,
                contentDescription = "Đã chọn",
                tint               = Color.White,
                modifier           = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
            )
        }

        // File size badge ở góc dưới
        Text(
            text     = image.size.toReadableSize(),
            style    = MaterialTheme.typography.labelSmall,
            color    = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

// ── Duplicates tab ────────────────────────────────────────────────────────────
@Composable
private fun DuplicatesContent(
    groups: List<com.coreclean.app.domain.model.DuplicateGroup>,
    selectedImages: Set<Long>,
    onToggleSelect: (Long) -> Unit
) {
    if (groups.isEmpty()) {
        EmptyContent(message = "Không tìm thấy ảnh trùng lặp")
        return
    }

    val totalWasted = groups.sumOf { it.totalWastedSize }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement   = Arrangement.spacedBy(3.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Summary card
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text       = "${groups.size} nhóm ảnh trùng lặp",
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text  = "Có thể giải phóng ${totalWasted.toReadableSize()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        groups.forEachIndexed { groupIdx, group ->
            // Group header
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text     = "Nhóm ${groupIdx + 1} · ${group.images.size} ảnh · ${group.images.first().size.toReadableSize()} mỗi ảnh",
                    style    = MaterialTheme.typography.labelMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            items(items = group.images, key = { it.id }) { image ->
                ImageGridItem(
                    image      = image,
                    isSelected = image.id in selectedImages,
                    onToggle   = { onToggleSelect(image.id) }
                )
            }
        }
    }
}

// ── Bottom bar khi chọn ảnh ───────────────────────────────────────────────────
@Composable
private fun SelectionBottomBar(
    count: Int,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        modifier       = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Bỏ chọn")
            }
            Text(
                text       = "Đã chọn $count ảnh",
                fontWeight = FontWeight.Medium
            )
            Button(
                onClick = onDelete,
                colors  = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Xóa")
            }
        }
    }
}

// ── Trạng thái phụ ────────────────────────────────────────────────────────────
@Composable private fun IdleContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Đang yêu cầu quyền truy cập...", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text("Đang tải ảnh...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable private fun ErrorContent(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
    }
}

@Composable private fun EmptyContent(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Extension ─────────────────────────────────────────────────────────────────
fun Long.toReadableSize(): String = when {
    this >= 1_073_741_824L -> "%.1f GB".format(this / 1_073_741_824.0)
    this >= 1_048_576L     -> "%.1f MB".format(this / 1_048_576.0)
    this >= 1_024L         -> "%.0f KB".format(this / 1_024.0)
    else                   -> "$this B"
}
