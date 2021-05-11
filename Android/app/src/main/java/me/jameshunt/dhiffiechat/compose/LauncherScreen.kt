package me.jameshunt.dhiffiechat.compose

import LoadingIndicator
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.compose.navigate
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.DhiffieChatApp

@Composable
fun LauncherScreen(navController: NavController) {
    val scope = rememberCoroutineScope()

    LoadingIndicator()

    run {
        scope.launch {
            DhiffieChatApp.di.dhiffieChatService.initialize()
            navController.navigate("home")
        }
    }
}