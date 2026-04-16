package com.example.intelligent_messaging_app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.intelligent_messaging_app.data.repository.UserPreferencesRepository
import com.example.intelligent_messaging_app.ui.chat.ChatScreen
import com.example.intelligent_messaging_app.ui.login.LoginScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Chat : Screen("chat")
}

@Composable
fun NavGraph(
    userPreferencesRepository: UserPreferencesRepository,
    navController: NavHostController = rememberNavController()
) {
    val isLoggedIn by userPreferencesRepository.isLoggedIn.collectAsState(initial = null)

    if (isLoggedIn == null) return // Wait for DataStore to load

    val startDestination = if (isLoggedIn == true) Screen.Chat.route else Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Chat.route) {
            ChatScreen()
        }
    }
}
