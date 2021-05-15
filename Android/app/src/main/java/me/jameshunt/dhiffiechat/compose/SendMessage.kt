package me.jameshunt.dhiffiechat.compose

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.FileLocationUtil
import me.jameshunt.dhiffiechat.MediaType
import me.jameshunt.dhiffiechat.S3Service
import java.io.File

class SendMessageViewModel(
    private val s3Service: S3Service,
    private val fileLocationUtil: FileLocationUtil
) : ViewModel() {

    fun sendFile(
        recipientUserId: String,
        mediaType: MediaType,
        text: String,
        onFinish: () -> Unit
    ) {
        // TODO: use text
        viewModelScope.launch {
            s3Service.sendFile(
                recipientUserId,
                fileLocationUtil.getInputFile(),
                mediaType
            )
            onFinish()
        }
    }

    fun getInputFile(): File {
        return fileLocationUtil.getInputFile()
    }
}

@Composable
fun SendMessage(navController: NavController, recipientUserId: String) {
    val viewModel: SendMessageViewModel = injectedViewModel()
    var mediaType: MediaType? by remember { mutableStateOf(null) }
    var mediaProvided: Boolean by remember { mutableStateOf(false) }
    var text: String? by remember { mutableStateOf(null) }
    var loading: Boolean by remember { mutableStateOf(false) }

    val imageContract = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { mediaProvided = it }
    )

    val videoContract = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakeVideo(),
        onResult = { mediaProvided = true }
    )

    Scaffold {
        when {
            mediaType == null -> SelectMediaType { mediaType = it }
            !mediaProvided -> {
                val fp = "me.jameshunt.dhiffiechat.fileprovider"
                val fpUri: Uri = FileProvider.getUriForFile(
                    LocalContext.current, fp, viewModel.getInputFile()
                )
                when (mediaType) {
                    MediaType.Image -> imageContract.launch(fpUri)
                    MediaType.Video -> videoContract.launch(fpUri)
                }
            }
            text == null -> TextConfirmation { text = it }
            else -> {
                LoadingIndicator()
                if (!loading) {
                    loading = true
                    viewModel.sendFile(
                        recipientUserId = recipientUserId,
                        mediaType = mediaType!!,
                        text = text!!,
                        onFinish = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun SelectMediaType(onMediaTypeSelected: (MediaType) -> Unit) {
    Column {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            onClick = { onMediaTypeSelected(MediaType.Image) },
            content = {
                Text(text = "Take Picture", fontSize = 24f.sp)
            }
        )
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            onClick = { onMediaTypeSelected(MediaType.Video) },
            content = {
                Text(text = "Record Video", fontSize = 24f.sp)
            })
    }
}

@Composable
fun TextConfirmation(onConfirm: (String) -> Unit) {
    var providedText: String by remember { mutableStateOf("") }

    Column {
        TextField(
            value = providedText,
            onValueChange = { providedText = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = {
                Text(text = "Message")
            }
        )
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(100.dp)
                .padding(16.dp),
            onClick = {
                onConfirm(providedText)
            },
            content = { Text(text = "Confirm") }
        )
    }
}
