package me.jameshunt.dhiffiechat.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.*

class ShowNextMessageViewModel(
    private val s3Service: S3Service,
    private val userService: UserService
) : ViewModel() {

    private val _imageByteArray: MutableLiveData<ByteArray?> = MutableLiveData(null)
    val imageByteArray: LiveData<ByteArray?> = _imageByteArray

    fun loadImage(fromUserId: String) {
        viewModelScope.launch {
            val message = userService
                .getMessageSummaries()
                .first { it.from == fromUserId }
                .next

            _imageByteArray.value = s3Service.getDecryptedFile(message)
        }
    }
}

@Composable
fun ShowNextMessageScreen(fromUserId: String) {
    val viewModel: ShowNextMessageViewModel = injectedViewModel()
    val imageBytes = viewModel.imageByteArray.observeAsState().value

    if (imageBytes == null) {
        viewModel.loadImage(fromUserId = fromUserId)
    }

    Scaffold {
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
}