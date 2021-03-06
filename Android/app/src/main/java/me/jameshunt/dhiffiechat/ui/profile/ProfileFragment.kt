package me.jameshunt.dhiffiechat.ui.profile

import androidx.compose.runtime.Composable
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.fragment.NavHostFragment.findNavController
import me.jameshunt.dhiffiechat.ui.compose.BackAppBar
import me.jameshunt.dhiffiechat.ui.compose.ComposeFragment
import me.jameshunt.dhiffiechat.ui.compose.injectedViewModel

class ProfileFragment : ComposeFragment() {
    private val viewModel: UserProfileViewModel by injectedViewModel()

    @Composable
    override fun ScreenComposable() {
        BackAppBar("Profile") {
            UserProfile(
                viewModel = viewModel,
                onAliasSet = {
                    findNavController(this@ProfileFragment).popBackStack()
                    val window = requireActivity().window
                    WindowInsetsControllerCompat(
                        window,
                        window.decorView
                    ).hide(WindowInsetsCompat.Type.ime())
                }
            )
        }
    }
}
