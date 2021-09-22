package me.jameshunt.dhiffiechat.ui.sendmessage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import me.jameshunt.dhiffiechat.R
import me.jameshunt.dhiffiechat.ErrorHandlingDialog
import me.jameshunt.dhiffiechat.ui.compose.*

class TextConfirmationFragment : ComposeFragment() {

    private val viewModel: SendMessageViewModel by navGraphViewModels(R.id.send_message) {
        InjectableViewModelFactory()
    }

    @Composable
    override fun ScreenComposable() {
        BackAppBar(title = "Send Message") {
            when (val result = viewModel.sendState.observeAsState().value!!) {
                SendMessageViewModel.SendState.CollectMessageText -> TextConfirmation(
                    onConfirm = { msg -> viewModel.sendMessage(msg) }
                )
                SendMessageViewModel.SendState.Loading -> LoadingIndicator()
                is SendMessageViewModel.SendState.Error -> ErrorHandlingDialog(
                    t = result.t,
                    onDismiss = { viewModel.toCollectMessageState() }
                )
                SendMessageViewModel.SendState.Finish -> {
                    findNavController().popBackStack(R.id.send_message, true)
                }
            }
        }
    }
}
