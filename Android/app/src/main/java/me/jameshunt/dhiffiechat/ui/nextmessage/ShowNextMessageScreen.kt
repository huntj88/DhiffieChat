package me.jameshunt.dhiffiechat.ui.nextmessage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.*
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.*
import me.jameshunt.dhiffiechat.service.LambdaApi.*
import me.jameshunt.dhiffiechat.service.MediaType
import me.jameshunt.dhiffiechat.service.S3Service
import me.jameshunt.dhiffiechat.service.UserService
import me.jameshunt.dhiffiechat.ui.compose.LoadingIndicator
import java.io.File

class ShowNextMessageViewModel(
    private val s3Service: S3Service,
    private val userService: UserService
) : ViewModel() {
    data class MediaMessage(val message: Message, val file: File)

    private var downloadEnabled = true
    private val _media: MutableLiveData<MediaMessage?> = MutableLiveData(null)
    val media: LiveData<MediaMessage?> = _media

    fun loadFile(fromUserId: String) {
        if (!downloadEnabled) return
        downloadEnabled = false

        viewModelScope.launch {
            val message = userService
                .getMessageSummaries()
                .first { it.from == fromUserId }
                .next!!

            _media.value = MediaMessage(
                message = userService.decryptMessageText(message),
                file = s3Service.getDecryptedFile(message)
            )
        }
    }
}

@Composable
fun ShowNextMessageScreen(viewModel: ShowNextMessageViewModel, fromUserId: String) {
    viewModel.media.observeAsState().value
        ?.let { media ->
            when (media.message.mediaType) {
                MediaType.Image -> ImageMessage(text = media.message.text, file = media.file)
                MediaType.Video -> VideoMessage(text = media.message.text, file = media.file)
            }
        }
        ?: Scaffold {
            LoadingIndicator().also { viewModel.loadFile(fromUserId = fromUserId) }
        }
}

@Composable
fun ImageMessage(text: String?, file: File) {
    val image = file.inputStream().readBytes().toBitmap().asImageBitmap()
    Box {
        Image(bitmap = image, contentDescription = "", modifier = Modifier.fillMaxSize())
        text?.let { TextWithScrim(text = it) }
    }
}

fun ByteArray.toBitmap(): Bitmap = BitmapFactory.decodeByteArray(this, 0, this.size)

@Composable
fun VideoMessage(text: String?, file: File) {
    Box {
        Player(file = file)
        text?.let { TextWithScrim(text = it) }
    }
}

@Composable
fun Player(file: File) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var position by rememberSaveable { mutableStateOf(0L) }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                val player = SimpleExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                    playWhenReady = true
                    seekTo(position)
                    prepare()
                }
                this.player = player
                lifecycle.addObserver(object : LifecycleObserver {
                    @OnLifecycleEvent(Lifecycle.Event.ON_START)
                    fun onStart() {
                        this@apply.onResume()
                    }

                    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                    fun onPause() {
                        position = player.currentPosition
                        this@apply.onPause()
                    }

                    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                    fun onDestroy() {
                        player.release()
                    }
                })
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun BoxScope.TextWithScrim(text: String) {
    Text(
        text = text,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x80000000))
            .padding(vertical = 20.dp)
            .align(Alignment.Center)
    )
}