package me.jameshunt.dhiffiechat.ui.managefriends

import androidx.compose.runtime.Composable
import me.jameshunt.dhiffiechat.ui.compose.ComposeFragment
import me.jameshunt.dhiffiechat.ui.compose.injectedViewModel

class ManageFriendsFragment : ComposeFragment() {
    private val viewModel: ManageFriendsViewModel by injectedViewModel()

    @Composable
    override fun ScreenComposable() {
        ManageFriendsScreen(viewModel = viewModel)
    }
}
