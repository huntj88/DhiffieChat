package me.jameshunt.dhiffiechat.home

import androidx.compose.runtime.Composable
import me.jameshunt.dhiffiechat.compose.ComposeFragment
import me.jameshunt.dhiffiechat.compose.HomeScreen
import me.jameshunt.dhiffiechat.compose.HomeViewModel
import me.jameshunt.dhiffiechat.compose.injectedViewModel

class HomeFragment: ComposeFragment() {
    private val viewModel: HomeViewModel by injectedViewModel()

    @Composable
    override fun ScreenComposable() {

        HomeScreen(
            viewModel = viewModel,
            toManageFriends = { TODO() },
            toShowNextMessage = { TODO() },
            toSendMessage = { TODO() },
        )
    }
}