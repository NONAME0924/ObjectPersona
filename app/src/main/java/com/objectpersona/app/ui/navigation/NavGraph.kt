package com.objectpersona.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.objectpersona.app.ui.screen.camera.CameraScreen
import com.objectpersona.app.ui.screen.chat.ChatScreen
import com.objectpersona.app.ui.screen.history.HistoryScreen
import com.objectpersona.app.ui.screen.persona.PersonaCardScreen
import com.objectpersona.app.ui.screen.setup.ModelSetupScreen

/**
 * 導航圖 — Camera → Persona → Chat，支援 History 入口。
 */
object Routes {
    const val SETUP = "setup"
    const val CAMERA = "camera"
    const val VISION_RESULT = "vision_result/{description}/{imagePath}"
    const val PERSONA = "persona/{objectDescription}"
    const val CHAT = "chat/{objectId}"
    const val HISTORY = "history"

    fun visionResult(description: String, imagePath: String) =
        "vision_result/${java.net.URLEncoder.encode(description, "UTF-8")}/${java.net.URLEncoder.encode(imagePath, "UTF-8")}"
    fun persona(description: String) = "persona/${java.net.URLEncoder.encode(description, "UTF-8")}"
    fun chat(objectId: String) = "chat/$objectId"
}

@Composable
fun ObjectPersonaNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SETUP) {
        // 模型載入畫面
        composable(Routes.SETUP) {
            ModelSetupScreen(
                onNavigateToCamera = {
                    navController.navigate(Routes.CAMERA) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CAMERA) {
            CameraScreen(
                onNavigateToResult = { description, imagePath ->
                    navController.navigate(Routes.visionResult(description, imagePath))
                },
                onHistoryClick = {
                    navController.navigate(Routes.HISTORY)
                }
            )
        }

        composable(
            route = Routes.VISION_RESULT,
            arguments = listOf(
                navArgument("description") { type = NavType.StringType },
                navArgument("imagePath") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val description = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("description") ?: "", "UTF-8"
            )
            val imagePath = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("imagePath") ?: "", "UTF-8"
            )
            com.objectpersona.app.ui.screen.vision.VisionResultScreen(
                objectDescription = description,
                imagePath = imagePath,
                onConfirm = { desc ->
                    navController.navigate(Routes.persona(desc)) {
                        popUpTo(Routes.CAMERA) { inclusive = false }
                    }
                },
                onRetake = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.PERSONA,
            arguments = listOf(navArgument("objectDescription") { type = NavType.StringType })
        ) { backStackEntry ->
            val description = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("objectDescription") ?: "",
                "UTF-8"
            )
            PersonaCardScreen(
                objectDescription = description,
                onStartChat = { objectId ->
                    navController.navigate(Routes.chat(objectId)) {
                        popUpTo(Routes.CAMERA) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(navArgument("objectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val objectId = backStackEntry.arguments?.getString("objectId") ?: ""
            ChatScreen(
                objectId = objectId,
                onSwitchObject = {
                    navController.popBackStack(Routes.CAMERA, inclusive = false)
                }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                onObjectSelected = { objectId ->
                    navController.navigate(Routes.chat(objectId)) {
                        popUpTo(Routes.CAMERA) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
