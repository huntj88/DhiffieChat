package me.jameshunt.dhiffiechat.ui.nextmessage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.subscribeBy
import me.jameshunt.dhiffiechat.service.LambdaApi.Message
import me.jameshunt.dhiffiechat.service.MediaType
import me.jameshunt.dhiffiechat.service.MessageService
import me.jameshunt.dhiffiechat.ErrorHandlingDialog
import me.jameshunt.dhiffiechat.ui.compose.LoadingIndicator
import me.jameshunt.dhiffiechat.Result
import java.io.File

class ShowNextMessageViewModel(private val messageService: MessageService) : ViewModel() {
    data class MediaMessage(val message: Message, val file: File)

    private val _media: MutableLiveData<Result<MediaMessage>?> = MutableLiveData(null)
    val media: LiveData<Result<MediaMessage>?> = _media

    private val disposables = CompositeDisposable()

    fun loadFile(fromUserId: String) {
        val disposable = messageService.getMessageSummaries()
            .flatMap {
                val message = it.first { it.from == fromUserId }.next!!
                messageService.decryptMessage(message).map { (decryptedMessage, file) ->
                    MediaMessage(message = decryptedMessage, file = file)
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { _media.value = Result.Failure(it) },
                onSuccess = { _media.value = Result.Success(it) }
            )

        disposables.add(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}

@Composable
fun ShowNextMessageScreen(viewModel: ShowNextMessageViewModel, fromUserId: String) {
    viewModel.media.observeAsState().value
        ?.let { result ->
            when (result) {
                is Result.Failure -> ErrorHandlingDialog(t = result.throwable)
                is Result.Success -> {
                    val media = result.data
                    when (media.message.mediaType) {
                        MediaType.Image -> ImageMessage(
                            text = media.message.text,
                            file = media.file
                        )
                        MediaType.Video -> VideoMessage(
                            text = media.message.text,
                            file = media.file
                        )
                    }
                }
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