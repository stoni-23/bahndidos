package com.stoni.bahndidos.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stoni.bahndidos.ui.screens.MainMenuScreen
import com.stoni.bahndidos.ui.screens.PlayScreen

object Routes {
    const val MAIN_MENU = "main_menu"
    const val PLAY = "play"
}

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.MAIN_MENU) {
        composable(Routes.MAIN_MENU) {
            MainMenuScreen(
                onPlay = { nav.navigate(Routes.PLAY) }
            )
        }
        composable(Routes.PLAY) {
            PlayScreen(
                onBack = {
                    nav.navigate(Routes.MAIN_MENU) {
                        popUpTo(Routes.PLAY) { inclusive = true }
                    }
                }
            )
        }
    }
}
