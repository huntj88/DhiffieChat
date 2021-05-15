package me.jameshunt.dhiffiechat

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import me.jameshunt.dhiffiechat.compose.*

@Composable
fun Navigation() {
    val navController = rememberNavController()

    NavHost(navController, startDestination = "launcher") {
        composable("launcher") { LauncherScreen(navController) }
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("manageFriends") { ManageFriendsScreen() }
        composable(
            route = "sendMessage/{toUserId}",
            arguments = listOf(navArgument("toUserId") {
                type = NavType.StringType
            }),
            content = {
                SendMessage(
                    navController = navController,
                    recipientUserId = it.arguments!!.getString("toUserId")!!
                )
            }
        )
        composable(
            route = "showNextMessage/{fromUserId}",
            arguments = listOf(navArgument("fromUserId") {
                type = NavType.StringType
            }),
            content = {
                val userId = it.arguments!!.getString("fromUserId")!!
                ShowNextMessageScreen(userId)
            }
        )
    }
}

fun NavController.toSendMessage(friendUserId: String) {
    this.navigate("sendMessage/$friendUserId")
}

fun NavController.toShowNextMessage(friendUserId: String) {
    this.navigate("showNextMessage/$friendUserId")
}

fun NavController.toManageFriends() {
    this.navigate("manageFriends")
}

fun NavController.toHome() {
    this.navigate("home")
}