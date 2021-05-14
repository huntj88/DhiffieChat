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
import java.io.File

class ShowNextMessageViewModel(
    private val s3Service: S3Service,
    private val userService: UserService
) : ViewModel() {

    private val _file: MutableLiveData<File?> = MutableLiveData(null)
    val file: LiveData<File?> = _file

    fun loadFile(fromUserId: String) {
        viewModelScope.launch {
            val message = userService
                .getMessageSummaries()
                .first { it.from == fromUserId }
                .next

            _file.value = s3Service.getDecryptedFile(message)
        }
    }
}

@Composable
fun ShowNextMessageScreen(fromUserId: String) {
    val viewModel: ShowNextMessageViewModel = injectedViewModel()
    val file = viewModel.file.observeAsState().value

    if (file == null) {
        viewModel.loadFile(fromUserId = fromUserId)
    }

    Scaffold {
        Column(
            Modifier
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            file?.inputStream()?.readBytes()?.toBitmap()?.asImageBitmap()
                ?.let { Image(it, contentDescription = "") }
                ?: LoadingIndicator()
        }
    }
}