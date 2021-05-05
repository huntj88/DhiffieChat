package me.jameshunt.dhiffiechat.compose

import LoadingIndicator
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.DI
import me.jameshunt.dhiffiechat.DhiffieChatService
import me.jameshunt.dhiffiechat.crypto.toIv
import me.jameshunt.dhiffiechat.toBitmap

class ShowNextMessageViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ShowNextMessageViewModel(DI.dhiffieChatService) as T
    }
}

class ShowNextMessageViewModel(
    private val apiService: DhiffieChatService
) : ViewModel() {

    private val _imageByteArray: MutableLiveData<ByteArray?> = MutableLiveData(null)
    val imageByteArray: LiveData<ByteArray?> = _imageByteArray

    fun loadImage(fromUserId: String) {
        viewModelScope.launch {
            val message = apiService
                .getMessageSummaries()
                .first { it.from == fromUserId }
                .next

            _imageByteArray.value = apiService.getDecryptedFile(
                senderUserId = message.from,
                fileKey = message.fileKey!!,
                userUserIv = message.iv.toIv()
            )
        }
    }
}

@Composable
fun ShowNextMessageScreen(fromUserId: String) {
    val viewModel: ShowNextMessageViewModel = viewModel(factory = ShowNextMessageViewModelFactory())
    val imageBytes = viewModel.imageByteArray.observeAsState().value

    if (imageBytes == null) {
        viewModel.loadImage(fromUserId = fromUserId)
    }

    Column(
        Modifier
            .fillMaxHeight()
            .padding(8.dp)
    ) {
        imageBytes?.toBitmap()?.asImageBitmap()
            ?.let { Image(it, contentDescription = "") }
            ?: LoadingIndicator()
    }
}