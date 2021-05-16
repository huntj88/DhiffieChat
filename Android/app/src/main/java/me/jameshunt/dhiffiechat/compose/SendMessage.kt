package me.jameshunt.dhiffiechat.compose

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    var mediaType: MediaType? by rememberSaveable { mutableStateOf(null) }
    var mediaProvided: Boolean by rememberSaveable { mutableStateOf(false) }
    var text: String? by rememberSaveable { mutableStateOf(null) }
    var loading: Boolean by rememberSaveable { mutableStateOf(false) }

    val imageContract = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { tookPicture ->
            mediaProvided = tookPicture
            if (!tookPicture) {
                mediaType = null
            }
        }
    )

    val videoContract = rememberLauncherForActivityResult(
        contract = TakeVideo(),
        onResult = { recordedVideo ->
            mediaProvided = recordedVideo
            if (!recordedVideo) {
                mediaType = null
            }
        }
    )

    Scaffold {
        when {
            mediaType == null -> SelectMediaType { mediaType = it }
            !mediaProvided -> {
                val context = LocalContext.current
                val fp = "${context.applicationInfo.packageName}.fileprovider"
                val fpUri: Uri = FileProvider.getUriForFile(context, fp, viewModel.getInputFile())
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
                        onFinish = {
                            loading = false
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}

class TakeVideo : ActivityResultContract<Uri, Boolean>() {
    @CallSuper
    override fun createIntent(context: Context, input: Uri): Intent {
        return Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, input)
    }

    override fun getSynchronousResult(
        context: Context,
        input: Uri
    ): SynchronousResult<Boolean>? = null

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK && intent != null
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
            }
        )
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
