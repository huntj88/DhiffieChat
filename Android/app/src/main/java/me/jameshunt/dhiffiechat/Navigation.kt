package me.jameshunt.dhiffiechat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import me.jameshunt.dhiffiechat.compose.*

class NavigationViewModel(val fileLocationUtil: FileLocationUtil): ViewModel()

@Composable
fun Navigation() {
    val navViewModel: NavigationViewModel = injectedViewModel()
    val navController = rememberNavController()

    val mediaContract = rememberLauncherForActivityResult(
        contract = ImageAndCameraContract(navViewModel.fileLocationUtil),
        onResult = { friendUserId -> navController.toSendMessage(friendUserId) }
    )

    NavHost(navController, startDestination = "launcher") {
        composable("launcher") { LauncherScreen(navController) }
        composable("home") {
            HomeScreen(
                navController = navController,
                onSendMessage = { friendUserId -> mediaContract.launch(friendUserId) }
            )
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
                    file = navViewModel.fileLocationUtil.getInputFile(),
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