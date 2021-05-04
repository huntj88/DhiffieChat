package me.jameshunt.privatechat.compose

import LoadingIndicator
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.compose.navigate
import kotlinx.coroutines.launch
import me.jameshunt.privatechat.DI

@Composable
fun LauncherScreen(navController: NavController) {
    val scope = rememberCoroutineScope()

    LoadingIndicator()

    run {
        scope.launch {
            DI.privateChatService.initialize()
            navController.navigate("home")
        }
    }
}