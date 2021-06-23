package me.jameshunt.dhiffiechat.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.fragment.NavHostFragment.findNavController
import me.jameshunt.dhiffiechat.R
import me.jameshunt.dhiffiechat.ui.compose.ComposeFragment
import me.jameshunt.dhiffiechat.ui.compose.injectedViewModel

class ProfileFragment : ComposeFragment() {
    private val viewModel: UserProfileViewModel by injectedViewModel()

    @Composable
    override fun ScreenComposable() {
        Column {
            TopAppBar(
                title = { Text(text = "Profile") },
                backgroundColor = MaterialTheme.colors.primaryVariant,
                navigationIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_baseline_arrow_back_24),
                        contentDescription = "Back",
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable {
                                findNavController(this@ProfileFragment).popBackStack()
                            }
                    )
                }
            )
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
