package me.jameshunt.dhiffiechat.ui.sendmessage

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import me.jameshunt.dhiffiechat.R
import me.jameshunt.dhiffiechat.ui.compose.*

class SelectMediaFragment: ComposeFragment() {
    private val viewModel: SendMessageViewModel by navGraphViewModels(R.id.send_message) {
        InjectableViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.recipientUserId = requireArguments().getString("toUserId")!!
    }

    @Composable
    override fun ScreenComposable() {
        BackAppBar("Send Message") {
            SelectMedia(
                sharedViewModel = viewModel,
                onMediaSelected = { findNavController().navigate(R.id.textConfirmationFragment) }
            )
        }
    }
}
