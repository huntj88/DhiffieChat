package me.jameshunt.dhiffiechat.launcher

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import me.jameshunt.dhiffiechat.R
import me.jameshunt.dhiffiechat.compose.ComposeFragment
import me.jameshunt.dhiffiechat.compose.injectedViewModel

class LauncherFragment: ComposeFragment() {
    private val viewModel: LauncherScreenViewModel by injectedViewModel()

    @Composable
    override fun ScreenComposable() {
        LauncherScreen(
            viewModel = viewModel,
            toUserProfile = { TODO() },
            toHome = { getNavController().navigate(R.id.homeFragment) }
        )
    }

    private fun getNavController(): NavController {
        val navHostFragment = requireActivity()
            .supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        return navHostFragment.navController
    }
}

