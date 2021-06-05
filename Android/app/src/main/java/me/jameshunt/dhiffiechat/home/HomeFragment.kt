package me.jameshunt.dhiffiechat.home

import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
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
            toUserProfile = { findNavController().navigate(R.id.profileFragment) },
            toManageFriends = { findNavController().navigate(R.id.manageFriendsFragment) },
            toShowNextMessage = { fromUserId ->
                val args = bundleOf("fromUserId" to fromUserId)
                findNavController().navigate(R.id.showNextMessageFragment, args)
            },
            toSendMessage = { toUserId ->
                val args = bundleOf("toUserId" to toUserId)
                findNavController().navigate(R.id.send_message, args)
            },
        )
    }
}