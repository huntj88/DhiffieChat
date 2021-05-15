package me.jameshunt.dhiffiechat.compose

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.*
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.*
import me.jameshunt.dhiffiechat.LambdaApi.*
import java.io.File

class ShowNextMessageViewModel(
    private val s3Service: S3Service,
    private val userService: UserService
) : ViewModel() {
    data class MediaMessage(val message: Message, val file: File)

    private val _media: MutableLiveData<MediaMessage?> = MutableLiveData(null)
    val media: LiveData<MediaMessage?> = _media

    fun loadFile(fromUserId: String) {
        viewModelScope.launch {
            val message = userService
                .getMessageSummaries()
                .first { it.from == fromUserId }
                .next!!

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

fun ByteArray.toBitmap(): Bitmap = BitmapFactory.decodeByteArray(this, 0, this.size)

@Composable
fun VideoMessage(file: File) {
    Player(file = file)
}

@Composable
fun Player(file: File) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    AndroidView(
        factory = { PlayerView(context) },
        modifier = Modifier.fillMaxWidth()
    ) { playerView ->
        val player = SimpleExoPlayer.Builder(context).build()
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
        player.prepare()
        player.playWhenReady = true

        lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun onStart() {
                playerView.onResume()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            fun onPause() {
                playerView.onPause()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                player.release()
            }
        })
        playerView.player = player
    }
}