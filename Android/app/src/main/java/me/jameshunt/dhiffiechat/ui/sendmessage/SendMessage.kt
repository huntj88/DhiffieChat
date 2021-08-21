package me.jameshunt.dhiffiechat.ui.sendmessage

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
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.service.FileLocationUtil
import me.jameshunt.dhiffiechat.service.MediaType
import me.jameshunt.dhiffiechat.service.MessageService
import me.jameshunt.dhiffiechat.ui.compose.StyledTextField
import java.io.File

class SendMessageViewModel(
    private val messageService: MessageService,
    private val fileUtil: FileLocationUtil,
    private val applicationScope: CoroutineScope
) : ViewModel() {
    lateinit var recipientUserId: String
    var mediaType: MediaType? = null
    var text: String? = null

    enum class SendState {
        CollectMessageText,
        Loading,
        Finish
    }

    private val _sendState = MutableLiveData(SendState.CollectMessageText)
    val sendState: LiveData<SendState> = _sendState

    fun sendMessage(text: String) {
        _sendState.value = SendState.Loading
        applicationScope.launch {
            messageService.sendMessage(
                recipientUserId = recipientUserId,
                text = text.ifEmpty { null },
                file = fileUtil.getInputFile(),
                mediaType = mediaType!!
            )
            _sendState.value = SendState.Finish
        }
    }

    fun getInputFile(): File = fileUtil.getInputFile()
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
fun SelectMedia(sharedViewModel: SendMessageViewModel, onMediaSelected: () -> Unit) {
    val imageContract = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { capturedMedia ->
            if (capturedMedia) {
                onMediaSelected()
            }
        }
    )

    val videoContract = rememberLauncherForActivityResult(
        contract = TakeVideo(),
        onResult = { capturedMedia ->
            if (capturedMedia) {
                onMediaSelected()
            }
        }
    )
    val context = LocalContext.current
    val fp = "${context.applicationInfo.packageName}.fileprovider"
    val inputFile = sharedViewModel.getInputFile()
    val fpUri: Uri = FileProvider.getUriForFile(context, fp, inputFile)

    SelectMediaType { mediaType ->
        sharedViewModel.mediaType = mediaType
        when (mediaType) {
            MediaType.Image -> imageContract.launch(fpUri)
            MediaType.Video -> videoContract.launch(fpUri)
        }
    }
}

@Composable
fun SelectMediaType(onMediaTypeSelected: (MediaType) -> Unit) {
    Scaffold {
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
}

@Composable
fun TextConfirmation(onConfirm: (String) -> Unit) {
    var providedText: String by rememberSaveable { mutableStateOf("") }

    Scaffold {
        Column {
            StyledTextField(
                value = providedText,
                labelString = "Message Text",
                onValueChange = { providedText = it }
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = {
                    onConfirm(providedText)
                },
                content = { Text(text = "Confirm") }
            )
        }
    }
}
