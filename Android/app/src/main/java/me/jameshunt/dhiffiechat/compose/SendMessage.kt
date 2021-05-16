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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigate
import androidx.navigation.compose.navigation
import androidx.navigation.compose.popUpTo
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.FileLocationUtil
import me.jameshunt.dhiffiechat.MediaType
import me.jameshunt.dhiffiechat.S3Service
import me.jameshunt.dhiffiechat.parentViewModel
import java.io.File

class SendMessageViewModel(
    private val s3Service: S3Service,
    private val fileLocationUtil: FileLocationUtil
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
        // TODO: use text
        _sendState.value = SendState.Loading
        viewModelScope.launch {
            s3Service.sendFile(recipientUserId, fileLocationUtil.getInputFile(), mediaType!!)
            _sendState.value = SendState.Finish
        }
    }

    fun getInputFile(): File {
        return fileLocationUtil.getInputFile()
    }
}

fun NavGraphBuilder.sendMessageSubGraph(navController: NavController) {
    navigation("", "sendMessage/{toUserId}") {
        composable("") {
            val recipientUserId = it.arguments!!.getString("toUserId")!!
            val sharedViewModel: SendMessageViewModel = it.parentViewModel(navController)
            sharedViewModel.recipientUserId = recipientUserId
            navController.navigate("selectMedia") {
                popUpTo("sendMessage/{toUserId}") { inclusive = false }
            }
        }
        composable(
            route = "selectMedia",
            content = {
                val sharedViewModel: SendMessageViewModel = it.parentViewModel(navController)
                SelectMedia(
                    sharedViewModel = sharedViewModel,
                    onMediaSelected = { navController.navigate("confirmMessage") }
                )
            }
        )
        composable(
            route = "confirmMessage",
            content = {
                val sharedViewModel: SendMessageViewModel = it.parentViewModel(navController)

                when (sharedViewModel.sendState.observeAsState().value!!) {
                    SendMessageViewModel.SendState.CollectMessageText -> TextConfirmation { msg ->
                        sharedViewModel.sendMessage(msg)
                    }
                    SendMessageViewModel.SendState.Loading -> LoadingIndicator()
                    SendMessageViewModel.SendState.Finish -> navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            }
        )
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
}
