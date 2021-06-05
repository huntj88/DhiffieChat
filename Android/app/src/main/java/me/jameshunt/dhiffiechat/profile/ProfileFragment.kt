package me.jameshunt.dhiffiechat.profile

import androidx.compose.runtime.Composable
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.fragment.NavHostFragment.findNavController
import me.jameshunt.dhiffiechat.compose.ComposeFragment
import me.jameshunt.dhiffiechat.compose.injectedViewModel

class ProfileFragment : ComposeFragment() {
    private val viewModel: UserProfileViewModel by injectedViewModel()

    @Composable
    override fun ScreenComposable() {
        UserProfile(
            viewModel = viewModel,
            onAliasSet = {
                findNavController(this).popBackStack()
                val window = requireActivity().window
                WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.ime())
            }
        )
    }
}
