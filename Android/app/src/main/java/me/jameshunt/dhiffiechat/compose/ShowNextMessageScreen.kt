package me.jameshunt.dhiffiechat.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.*
import java.io.File

class ShowNextMessageViewModel(
    private val s3Service: S3Service,
    private val userService: UserService
) : ViewModel() {
    data class MediaMessage(val message: DhiffieChatApi.Message, val file: File)

    private val _media: MutableLiveData<MediaMessage?> = MutableLiveData(null)
    val media: LiveData<MediaMessage?> = _media

    fun loadFile(fromUserId: String) {
        viewModelScope.launch {
            val message = userService
                .getMessageSummaries()
                .first { it.from == fromUserId }
                .next

            _media.value = MediaMessage(message, s3Service.getDecryptedFile(message))
        }
    }
}

@Composable
fun ShowNextMessageScreen(fromUserId: String) {
    val viewModel: ShowNextMessageViewModel = injectedViewModel()
    val media = viewModel.media.observeAsState().value

    Scaffold {
        Column(
            Modifier
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            media?.let {
                when (it.message.mediaType) {
                    MediaType.Image -> ImageMessage(file = media.file)
                    MediaType.Video -> VideoMessage(file = media.file)
                }
            } ?: LoadingIndicator().also { viewModel.loadFile(fromUserId = fromUserId) }
        }
    }
}

@Composable
fun ImageMessage(file: File) {
    val image = file.inputStream().readBytes().toBitmap().asImageBitmap()
    Image(image, contentDescription = "")
}

@Composable
fun VideoMessage(file: File) {
    Text("a video")
}