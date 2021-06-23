package me.jameshunt.dhiffiechat.ui.home

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
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import me.jameshunt.dhiffiechat.R
import me.jameshunt.dhiffiechat.openNavDrawer
import me.jameshunt.dhiffiechat.ui.compose.ComposeFragment
import me.jameshunt.dhiffiechat.ui.compose.injectedViewModel

class HomeFragment : ComposeFragment() {
    private val viewModel: HomeViewModel by injectedViewModel(ownerProducer = { requireActivity() })

    @Composable
    override fun ScreenComposable() {
        Column {
            TopAppBar(
                title = { Text(text = "Home") },
                backgroundColor = MaterialTheme.colors.primaryVariant,
                navigationIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_baseline_menu_24),
                        contentDescription = "Navigation",
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable { openNavDrawer() }
                    )
                }
            )
            HomeScreen(
                viewModel = viewModel,
                toUserProfile = { findNavController().navigate(R.id.profileFragment) },
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
}
