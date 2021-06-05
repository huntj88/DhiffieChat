package me.jameshunt.dhiffiechat.home

import androidx.compose.runtime.Composable
import androidx.navigation.fragment.findNavController
import me.jameshunt.dhiffiechat.R
import me.jameshunt.dhiffiechat.compose.ComposeFragment
import me.jameshunt.dhiffiechat.compose.injectedViewModel

class HomeFragment: ComposeFragment() {
    private val viewModel: HomeViewModel by injectedViewModel()

    @Composable
    override fun ScreenComposable() {
        HomeScreen(
            viewModel = viewModel,
            toManageFriends = { findNavController().navigate(R.id.profileFragment) },
            toShowNextMessage = { TODO() },
            toSendMessage = { TODO() },
        )
    }
}