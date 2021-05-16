package me.jameshunt.dhiffiechat.compose

import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavController
import me.jameshunt.dhiffiechat.DhiffieChatApp
import me.jameshunt.dhiffiechat.UserService
import me.jameshunt.dhiffiechat.toHome

@Composable
fun LauncherScreen(navController: NavController) {
    val userService by remember { mutableStateOf(getUserService()) }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    Scaffold {
        LoadingIndicator()
    }

    LaunchedEffect(key1 = "init") {
        if (!isLoading) {
            isLoading = true
            userService.createIdentity()
            isLoading = false
            navController.toHome()
        }
    }
}

private fun getUserService(): UserService {
    // normally `createInjected` is used to inject ViewModels within a ViewModelProvider.Factory
    data class LauncherDependencies(val userService: UserService)
    return DhiffieChatApp.di.createInjected(LauncherDependencies::class.java).userService
}