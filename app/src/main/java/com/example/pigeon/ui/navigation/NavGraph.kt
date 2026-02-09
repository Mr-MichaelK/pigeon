package com.example.pigeon.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pigeon.domain.repository.UserRepository
import com.example.pigeon.ui.screens.onboarding.OnboardingScreen
import com.example.pigeon.ui.screens.onboarding.OnboardingViewModel
import com.example.pigeon.ui.screens.profile.ProfileScreen
import com.example.pigeon.ui.screens.profile.ProfileViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.pigeon.ui.theme.StichColor
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.layout.padding

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Map : Screen("map")
    object Radar : Screen("radar")
    object Log : Screen("log")
    object Profile : Screen("profile")
}

@Composable
fun PigeonNavGraph(
    userRepository: UserRepository
) {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<String?>(null) }
    
    // items to decide when to show the bottom bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        val user = userRepository.getUser().first()
        startDestination = if (user == null) Screen.Onboarding.route else Screen.Map.route
    }

    startDestination?.let { destination ->
        androidx.compose.material3.Scaffold(
            bottomBar = {
                // Show Bottom Nav on all screens EXCEPT Onboarding
                if (currentRoute != Screen.Onboarding.route) {
                    StichBottomNav(navController = navController)
                }
            },
            containerColor = StichColor.Background
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = destination,
                modifier = androidx.compose.ui.Modifier.padding(innerPadding)
            ) {
                composable(Screen.Onboarding.route) {
                    val viewModel: OnboardingViewModel = hiltViewModel()
                    OnboardingScreen(
                        viewModel = viewModel,
                        onJoinComplete = {
                            navController.navigate(Screen.Map.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Screen.Map.route) {
                    com.example.pigeon.ui.screens.map.MapPlaceholderScreen(
                        onOpenProfile = { navController.navigate(Screen.Profile.route) }
                    )
                }
                composable(Screen.Radar.route) {
                    com.example.pigeon.ui.screens.radar.RadarPlaceholderScreen()
                }
                composable(Screen.Log.route) {
                    com.example.pigeon.ui.screens.log.LogPlaceholderScreen()
                }
                composable(Screen.Profile.route) {
                    val viewModel: ProfileViewModel = hiltViewModel()
                    ProfileScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
