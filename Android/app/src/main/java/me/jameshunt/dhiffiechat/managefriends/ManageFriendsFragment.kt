package me.jameshunt.dhiffiechat.managefriends

import androidx.compose.runtime.Composable
import me.jameshunt.dhiffiechat.compose.ComposeFragment
import me.jameshunt.dhiffiechat.compose.injectedViewModel

class ManageFriendsFragment : ComposeFragment() {
    private val viewModel: ManageFriendsViewModel by injectedViewModel()

    @Composable
    override fun ScreenComposable() {
        ManageFriendsScreen(viewModel = viewModel)
    }
}