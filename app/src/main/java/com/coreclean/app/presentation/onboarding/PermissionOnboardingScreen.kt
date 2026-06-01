package com.coreclean.app.presentation.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coreclean.app.R
import kotlinx.coroutines.launch

private data class OnboardingStep(
    val icon: ImageVector,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int
)

private val steps = listOf(
    OnboardingStep(
        icon      = Icons.Default.Storage,
        titleRes  = R.string.onboarding_step_storage_title,
        descRes   = R.string.onboarding_step_storage_desc
    ),
    OnboardingStep(
        icon      = Icons.Default.ContactPage,
        titleRes  = R.string.onboarding_step_usage_title,
        descRes   = R.string.onboarding_step_usage_desc
    ),
    OnboardingStep(
        icon      = Icons.Default.Notifications,
        titleRes  = R.string.onboarding_step_notif_title,
        descRes   = R.string.onboarding_step_notif_desc
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionOnboardingScreen(
    onComplete: () -> Unit,
    viewModel: PermissionOnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState { steps.size }
    val scope      = rememberCoroutineScope()
    val context    = LocalContext.current

    val storagePermissions = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { scope.launch { if (pagerState.currentPage < steps.lastIndex) pagerState.animateScrollToPage(pagerState.currentPage + 1) } }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.completeOnboarding(); onComplete() }

    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Column(
            modifier            = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPage(steps[page])
            }

            // Page indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.padding(bottom = 16.dp)
            ) {
                repeat(steps.size) { i ->
                    val selected = pagerState.currentPage == i
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(if (selected) 24.dp else 8.dp, 8.dp)
                    ) {}
                }
            }

            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick  = { viewModel.completeOnboarding(); onComplete() },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.onboarding_skip)) }

                Button(
                    onClick = {
                        when (pagerState.currentPage) {
                            0 -> storageLauncher.launch(storagePermissions)
                            1 -> {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                scope.launch { pagerState.animateScrollToPage(2) }
                            }
                            2 -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.completeOnboarding(); onComplete()
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(2f)
                ) {
                    Text(
                        when (pagerState.currentPage) {
                            0    -> stringResource(R.string.onboarding_grant_storage)
                            1    -> stringResource(R.string.onboarding_open_settings)
                            else -> stringResource(R.string.onboarding_start)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(step: OnboardingStep) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector        = step.icon,
            contentDescription = null,
            modifier           = Modifier.size(96.dp),
            tint               = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(32.dp))
        Text(
            stringResource(step.titleRes),
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(step.descRes),
            style     = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
