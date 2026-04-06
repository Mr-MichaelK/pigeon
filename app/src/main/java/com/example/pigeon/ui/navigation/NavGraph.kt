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
import com.example.pigeon.ui.screens.map.MapScreen
import com.example.pigeon.ui.screens.onboarding.OnboardingViewModel
import com.example.pigeon.ui.screens.profile.ProfileScreen
import com.example.pigeon.ui.screens.profile.ProfileViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.pigeon.ui.theme.MeshColor
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.launch
import com.example.pigeon.ui.components.MeshDrawerHeader

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
    
    val user by userRepository.getUser().collectAsState(initial = null)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // items to decide when to show the bottom bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        val userVal = userRepository.getUser().first()
        startDestination = if (userVal == null) Screen.Onboarding.route else Screen.Map.route
    }

    startDestination?.let { destination ->
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = currentRoute != Screen.Onboarding.route,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MeshColor.Background,
                    drawerTonalElevation = 0.dp,
                    modifier = Modifier.width(300.dp)
                ) {
                    MeshDrawerHeader(user = user)
                    Divider(color = MeshColor.Border)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val items = listOf(
                        Screen.Map to "MAP",
                        Screen.Radar to "RADAR",
                        Screen.Log to "LOG",
                        Screen.Profile to "PROFILE"
                    )
                    
                    items.forEach { (screen, label) ->
                        NavigationDrawerItem(
                            label = { Text(text = label, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MeshColor.Primary.copy(alpha = 0.1f),
                                selectedTextColor = MeshColor.Primary,
                                unselectedTextColor = MeshColor.TextPrimary
                            )
                        )
                    }
                }
            }
        ) {
            androidx.compose.material3.Scaffold(
                bottomBar = {
                    // Show Bottom Nav on all screens EXCEPT Onboarding
                    if (currentRoute != Screen.Onboarding.route) {
                        MeshBottomNav(navController = navController)
                    }
                },
                containerColor = MeshColor.Background
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = destination,
                    modifier = Modifier.padding(innerPadding)
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
                        MapScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
                    }
                    composable(Screen.Radar.route) {
                        com.example.pigeon.ui.screens.radar.RadarScreen(
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }
                    composable(Screen.Log.route) {
                        val viewModel: com.example.pigeon.ui.screens.log.EventLogViewModel = hiltViewModel()
                        com.example.pigeon.ui.screens.log.EventLogScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
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
}
