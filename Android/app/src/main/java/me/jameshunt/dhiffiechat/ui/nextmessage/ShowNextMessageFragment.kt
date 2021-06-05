package me.jameshunt.dhiffiechat.ui.nextmessage

import androidx.compose.runtime.Composable
import me.jameshunt.dhiffiechat.ui.compose.ComposeFragment
import me.jameshunt.dhiffiechat.ui.compose.injectedViewModel

class ShowNextMessageFragment: ComposeFragment() {
    private val viewModel: ShowNextMessageViewModel by injectedViewModel()

    @Composable
    override fun ScreenComposable() {
        ShowNextMessageScreen(
            viewModel = viewModel,
            fromUserId = requireArguments().getString("fromUserId")!!
        )
    }
}
