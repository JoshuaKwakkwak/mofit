package com.mofit.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mofit.app.MofitApplication
import com.mofit.app.ui.CoachingScreen
import com.mofit.app.ui.HistoryScreen
import com.mofit.app.ui.HomeScreen
import com.mofit.app.ui.OnboardingScreen
import com.mofit.app.ui.TrackingScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val TRACKING = "tracking"
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val db = (context.applicationContext as MofitApplication).database
    val profile by db.workoutDao().getProfile().collectAsState(initial = null)
    var profileLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        if (!profileLoaded) profileLoaded = true
    }

    if (!profileLoaded) {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val navController = rememberNavController()
    val startDest = if (profile != null) "main" else Routes.ONBOARDING

    NavHost(navController = navController, startDestination = startDest) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate("main") {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            MainTabScreen(onStartWorkout = { navController.navigate(Routes.TRACKING) })
        }
        composable(Routes.TRACKING) {
            TrackingScreen(onClose = { navController.popBackStack() })
        }
    }
}

@Composable
fun MainTabScreen(onStartWorkout: () -> Unit) {
    val tabIndex = remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.Black,
                contentColor = Color(0xFF33CC66)
            ) {
                NavigationBarItem(
                    selected = tabIndex.value == 0,
                    onClick = { tabIndex.value = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "홈") },
                    label = { Text("홈") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF33CC66),
                        selectedTextColor = Color(0xFF33CC66),
                        unselectedIconColor = Color(0xFF666666),
                        unselectedTextColor = Color(0xFF666666),
                        indicatorColor = Color(0xFF1A1A1A)
                    )
                )
                NavigationBarItem(
                    selected = tabIndex.value == 1,
                    onClick = { tabIndex.value = 1 },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "기록") },
                    label = { Text("기록") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF33CC66),
                        selectedTextColor = Color(0xFF33CC66),
                        unselectedIconColor = Color(0xFF666666),
                        unselectedTextColor = Color(0xFF666666),
                        indicatorColor = Color(0xFF1A1A1A)
                    )
                )
                NavigationBarItem(
                    selected = tabIndex.value == 2,
                    onClick = { tabIndex.value = 2 },
                    icon = { Icon(Icons.Default.Star, contentDescription = "코칭") },
                    label = { Text("코칭") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF33CC66),
                        selectedTextColor = Color(0xFF33CC66),
                        unselectedIconColor = Color(0xFF666666),
                        unselectedTextColor = Color(0xFF666666),
                        indicatorColor = Color(0xFF1A1A1A)
                    )
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tabIndex.value) {
                0 -> HomeScreen(onStartWorkout = onStartWorkout)
                1 -> HistoryScreen()
                2 -> CoachingScreen()
            }
        }
    }
}
