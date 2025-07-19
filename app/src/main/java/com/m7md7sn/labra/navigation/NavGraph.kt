package com.m7md7sn.labra.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.m7md7sn.labra.screens.HomeScreen
import com.m7md7sn.labra.screens.QRScannerScreen
import com.m7md7sn.labra.screens.VoiceAssistantScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onStartExperiment = {
                    navController.navigate("qr_scanner")
                }
            )
        }

        composable("qr_scanner") {
            QRScannerScreen(
                onQRCodeScanned = { experimentId ->
                    navController.navigate("voice_assistant/$experimentId")
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }

        composable("voice_assistant/{experimentId}") { backStackEntry ->
            val experimentId = backStackEntry.arguments?.getString("experimentId") ?: ""
            VoiceAssistantScreen(
                experimentId = experimentId,
                onExitExperiment = {
                    navController.popBackStack("home", inclusive = false)
                }
            )
        }
    }
}
