package me.jameshunt.dhiffiechat

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.navigation.NavBackStackEntry
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
        composable("manageFriends") { ManageFriendsScreen(navController) }
        sendMessageSubGraph(navController)
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



@Composable
inline fun <reified VM : ViewModel> NavBackStackEntry.parentViewModel(
    navController: NavController
): VM {
    // First, get the parent of the current destination
    // This always exists since every destination in your graph has a parent
    val parentId = destination.parent!!.id

    // Now get the NavBackStackEntry associated with the parent
    val parentBackStackEntry = navController.getBackStackEntry(parentId)

    // And since we can't use viewModel(), we use ViewModelProvider directly
    // to get the ViewModel instance, using the lifecycle-viewmodel-ktx extension
    return ViewModelProvider(parentBackStackEntry, InjectableViewModelFactory()).get()
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