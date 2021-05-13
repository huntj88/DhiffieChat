package me.jameshunt.dhiffiechat.compose

import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.compose.navigate
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.DhiffieChatApp
import me.jameshunt.dhiffiechat.UserService

@Composable
fun LauncherScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val userService = getUserService()

    LoadingIndicator()

    run {
        scope.launch {
            userService.createIdentity()
            navController.navigate("home")
        }
    }
}

private fun getUserService(): UserService {
    // normally `createInjected` is used to inject ViewModels within a ViewModelProvider.Factory
    data class LauncherDependencies(val userService: UserService)
    return DhiffieChatApp.di.createInjected(LauncherDependencies::class.java).userService
}