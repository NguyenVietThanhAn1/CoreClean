package com.coreclean.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.coreclean.app.presentation.home.HomeScreen
import com.coreclean.app.presentation.review.SafetyReviewScreen
import com.coreclean.app.ui.media.MediaScreen
import kotlinx.serialization.Serializable

// ── Type-safe routes ─────────────────────────────────────────────
@Serializable object HomeRoute
@Serializable object StorageRoute
@Serializable object MediaRoute
@Serializable object ContactRoute
@Serializable object BatteryRoute
@Serializable object AppUsageRoute
@Serializable data class ReviewRoute(val moduleId: String)

// ── NavGraph ─────────────────────────────────────────────────────
@Composable
fun CleanerNavGraph(navController: NavHostController) {
    NavHost(
        navController    = navController,
        startDestination = HomeRoute
    ) {
        composable<HomeRoute> {
            HomeScreen(navController = navController)
        }
        composable<StorageRoute> { }
        composable<MediaRoute> {
            MediaScreen(
                onNavigateToReview = { navController.navigate(ReviewRoute("media")) }
            )
        }
        composable<ContactRoute> { }
        composable<BatteryRoute> { }
        composable<AppUsageRoute> { }
        composable<ReviewRoute> {
            SafetyReviewScreen(
                onNavigateBack   = { navController.popBackStack() },
                onDeleteComplete = {
                    navController.popBackStack(MediaRoute, inclusive = false)
                }
            )
        }
    }
}
