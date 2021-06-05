package me.jameshunt.dhiffiechat.profile

import androidx.compose.runtime.Composable
import androidx.navigation.fragment.NavHostFragment.findNavController
import me.jameshunt.dhiffiechat.compose.ComposeFragment
import me.jameshunt.dhiffiechat.compose.UserProfile
import me.jameshunt.dhiffiechat.compose.UserProfileViewModel
import me.jameshunt.dhiffiechat.compose.injectedViewModel

class ProfileFragment : ComposeFragment() {
    private val viewModel: UserProfileViewModel by injectedViewModel()

    @Composable
    override fun ScreenComposable() {
        UserProfile(
            viewModel = viewModel,
            onAliasSet = { findNavController(this).popBackStack() }
        )
    }
}