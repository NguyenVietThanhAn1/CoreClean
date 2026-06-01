package com.coreclean.app.presentation.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.coreclean.app.presentation.appanalyzer.AppAnalyzerScreen
import com.coreclean.app.presentation.battery.BatteryScreen
import com.coreclean.app.presentation.contact.ContactScreen
import com.coreclean.app.presentation.home.HomeScreen
import com.coreclean.app.presentation.junk.JunkScreen
import com.coreclean.app.presentation.onboarding.PermissionOnboardingScreen
import com.coreclean.app.presentation.privacy.PrivacyDashboardScreen
import com.coreclean.app.presentation.ram.RamScreen
import com.coreclean.app.presentation.review.SafetyReviewScreen
import com.coreclean.app.presentation.settings.SettingsScreen
import com.coreclean.app.presentation.storage.StorageScreen
import com.coreclean.app.presentation.usage.AppUsageScreen
import com.coreclean.app.ui.media.MediaScreen
import kotlinx.serialization.Serializable

// Type-safe routes
@Serializable object OnboardingRoute
@Serializable object HomeRoute
@Serializable object StorageRoute
@Serializable object MediaRoute
@Serializable object ContactRoute
@Serializable object BatteryRoute
@Serializable object AppUsageRoute
@Serializable object JunkRoute
@Serializable object SettingsRoute
@Serializable object RamRoute
@Serializable object AppAnalyzerRoute
@Serializable object PrivacyRoute
@Serializable data class ReviewRoute(val moduleId: String, val imageIds: List<Long> = emptyList())

@Composable
fun CleanerNavGraph(
    navController: NavHostController,
    startDestination: Any = HomeRoute,
    windowSizeClass: WindowSizeClass? = null
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable<OnboardingRoute> {
            PermissionOnboardingScreen(
                onComplete = {
                    navController.navigate(HomeRoute) {
                        popUpTo(OnboardingRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<HomeRoute> {
            HomeScreen(navController = navController, windowSizeClass = windowSizeClass)
        }

        composable<MediaRoute> {
            MediaScreen(
                onNavigateToReview = { route -> navController.navigate(route) }
            )
        }

        composable<StorageRoute> {
            StorageScreen(
                onNavigateBack     = { navController.popBackStack() },
                onNavigateToReview = { route -> navController.navigate(route) }
            )
        }

        composable<BatteryRoute> {
            BatteryScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<AppUsageRoute> {
            AppUsageScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<ContactRoute> {
            ContactScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<JunkRoute> {
            JunkScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<RamRoute> {
            RamScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<AppAnalyzerRoute> {
            AppAnalyzerScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateBack      = { navController.popBackStack() },
                onNavigateToPrivacy = { navController.navigate(PrivacyRoute) }
            )
        }

        composable<PrivacyRoute> {
            PrivacyDashboardScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<ReviewRoute> {
            SafetyReviewScreen(
                onNavigateBack   = { navController.popBackStack() },
                onDeleteComplete = { navController.popBackStack(MediaRoute, inclusive = false) }
            )
        }
    }
}
