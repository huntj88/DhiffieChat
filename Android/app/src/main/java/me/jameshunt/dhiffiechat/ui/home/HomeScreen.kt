package me.jameshunt.dhiffiechat.ui.home

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.squareup.moshi.Moshi
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import me.jameshunt.dhiffiechat.DhiffieChatApp
import me.jameshunt.dhiffiechat.R
import me.jameshunt.dhiffiechat.service.MessageService
import me.jameshunt.dhiffiechat.service.UserService
import me.jameshunt.dhiffiechat.ui.compose.ErrorHandlingDialog
import me.jameshunt.dhiffiechat.ui.compose.LoadingIndicator
import me.jameshunt.dhiffiechat.ui.compose.Result
import net.glxn.qrgen.android.MatrixToImageWriter
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import java.util.*

data class QRData(
    val userId: String,
    val alias: String
)

data class FriendMessageData(
    val friendUserId: String,
    val alias: String,
    val count: Int,
    val mostRecentAt: Instant?
)

sealed class DialogState {
    object CameraPermission : DialogState()
    object Scan : DialogState()
    object ScanSuccess : DialogState()
    data class ScanFailure(val t: Throwable) : DialogState()
    object Share : DialogState()
    object Loading : DialogState()
    object None : DialogState()
}

class HomeViewModel(
    private val userService: UserService,
    private val messageService: MessageService,
    moshi: Moshi
) : ViewModel() {

    private val disposables = CompositeDisposable()
    private val qrAdapter = moshi.adapter(QRData::class.java)

    val alias = userService.getAlias().let { LiveDataReactiveStreams.fromPublisher(it) }
    val qrDataShare: LiveData<String?> = alias.map { alias ->
        alias?.orElse(null)
            ?.let { QRData(it.userId, it.alias) }
            ?.let { qrAdapter.toJson(it) }
    }

    val dialogState = MutableLiveData(DialogState.None as DialogState)

    val friendMessageData: LiveData<Result<List<FriendMessageData>>> by lazy {
        userService.getFriends()
            .flatMapSingle { friends ->
                messageService
                    .getMessageSummaries()
                    .map { it.associateBy { it.from } }
                    .map { summaries ->
                        friends.map { friend ->
                            val messageFromUserSummary = summaries[friend.userId]
                            FriendMessageData(
                                friendUserId = friend.userId,
                                alias = friend.alias,
                                count = messageFromUserSummary?.count ?: 0,
                                mostRecentAt = messageFromUserSummary?.mostRecentCreatedAt
                            )
                        }.sortedByDescending { it.mostRecentAt }
                    }
            }
            .map { Result.Success(it) as Result<List<FriendMessageData>> }
            .onErrorResumeNext { Observable.just(Result.Failure(it)) }
            .toFlowable(BackpressureStrategy.LATEST)
            .let { LiveDataReactiveStreams.fromPublisher(it) }
    }

    fun isUserProfileSetup(): Boolean = userService.isUserProfileSetup()

    fun scanSelected() {
        dialogState.value = DialogState.CameraPermission
    }

    fun shareSelected() {
        dialogState.value = DialogState.Share
    }

    fun addFriend(qrJson: String) {
        dialogState.value = DialogState.Loading
        val (userId, alias) = qrAdapter.fromJson(qrJson)!!

        val disposable = userService.addFriend(userId, alias)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { dialogState.value = DialogState.ScanSuccess },
                onError = { dialogState.value = DialogState.ScanFailure(it) }
            )

        disposables.add(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    toUserProfile: () -> Unit,
    toShowNextMessage: (friendUserId: String) -> Unit,
    toSendMessage: (friendUserId: String) -> Unit
) {
    var isProfileSetup by remember { mutableStateOf(viewModel.isUserProfileSetup()) }

    LocalLifecycleOwner.current.lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_RESUME) {
                isProfileSetup = viewModel.isUserProfileSetup()
            }
        }
    })

    if (!isProfileSetup) {
        LaunchedEffect("toUserProfile") {
            // safely call toUserProfile() once with LaunchedEffect, even though its not a coroutine
            toUserProfile()
        }
        return
    }

    Scaffold(
        content = {
            Column(
                Modifier
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                val friendMessageData = viewModel.friendMessageData
                when (val messageSummariesResult = friendMessageData.observeAsState().value) {
                    is Result.Success -> {
                        val summaries = messageSummariesResult.data
                        if (summaries.isEmpty()) {
                            Text(text = "No Friends added yet, please exchange QR codes")
                        }

                        summaries.filter { it.count > 0 }.ShowList(
                            title = "Messages",
                            onItemClick = { toShowNextMessage(it.friendUserId) }
                        )

                        summaries.filter { it.count == 0 }.ShowList(
                            title = "Friends",
                            onItemClick = { toSendMessage(it.friendUserId) }
                        )
                    }
                    is Result.Failure -> {
                        ErrorHandlingDialog(t = messageSummariesResult.throwable)
                    }
                    null -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(Modifier.align(Alignment.CenterHorizontally)) {
                            LoadingIndicator()
                        }
                    }
                }
            }
        }
    )

    DialogStates(viewModel = viewModel)
}

@Composable
fun List<FriendMessageData>.ShowList(
    title: String,
    onItemClick: (FriendMessageData) -> Unit
) {
    if (this.isNotEmpty()) {
        Text(
            text = title,
            fontSize = 22.sp,
            modifier = Modifier.padding(start = 12.dp, top = 24.dp)
        )
        this.forEach { data ->
            FriendCard(data) {
                onItemClick(data)
            }
        }
    }
}

@Composable
fun FriendCard(friendData: FriendMessageData, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(12.dp)
            .clickable { onClick() }
    ) {
        Box(
            Modifier
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colors.secondary, CircleShape)
                .padding(4.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_baseline_qr_code_scanner_24),
                contentDescription = friendData.alias,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .requiredSize(50.dp)
                    .clip(CircleShape),
                colorFilter = ColorFilter.tint(userIdToColor(userId = friendData.friendUserId))
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = friendData.alias,
                fontSize = 22.sp,
                modifier = Modifier.align(Alignment.TopStart)
            )

            friendData.mostRecentAt?.let { mostRecentAt ->
                val duration = Duration.between(mostRecentAt, Instant.now())
                val days = duration.toDays()
                val hours = duration.toHours() % 24
                val minutes = duration.toMinutes() % 60
                val seconds = duration.toMillis() / 1000 % 60

                val daysString = if (days > 0) "$days day ago" else null
                val hoursString = if (hours > 0) "$hours hr ago" else null
                val minutesString = if (minutes > 0) "$minutes min ago" else null
                val secondsString = "$seconds sec ago"

                val elapsedTime = daysString ?: hoursString ?: minutesString ?: secondsString

                Text(
                    text = elapsedTime,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }

            val ctaText = when (friendData.count == 0) {
                true -> "Send a message"
                false -> "${friendData.count} unseen messages"
            }
            Text(
                text = ctaText,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
private fun DialogStates(viewModel: HomeViewModel) {
    val cameraPermissionContract = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { permissionGranted ->
            if (permissionGranted) {
                viewModel.dialogState.value = DialogState.Scan
            } else {
                Log.e("Camera", "camera permission denied")
                viewModel.dialogState.value = DialogState.None
            }
        }
    )

    when (val result = viewModel.dialogState.observeAsState().value!!) {
        DialogState.CameraPermission -> cameraPermissionContract.launch(Manifest.permission.CAMERA)
        DialogState.Scan -> Dialog(
            onDismissRequest = {
                if (viewModel.dialogState.value == DialogState.Scan) {
                    viewModel.dialogState.value = DialogState.None
                }
            },
            content = {
                Card(modifier = Modifier.size(350.dp)) {
                    QRScanner { qrData ->
                        viewModel.addFriend(qrJson = qrData)
                    }
                }
            }
        )
        DialogState.ScanSuccess -> Dialog(
            onDismissRequest = { viewModel.dialogState.value = DialogState.None },
            content = {
                Card(
                    modifier = Modifier.background(DhiffieChatApp.DialogColor),
                    content = {
                        Text(
                            text = "Successfully Scanned other user's QR. Please make sure they scan yours",
                            color = DhiffieChatApp.DialogTextColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                )
            }
        )
        is DialogState.ScanFailure -> ErrorHandlingDialog(t = result.t) {
            viewModel.dialogState.value = DialogState.None
        }
        DialogState.Share -> Dialog(
            onDismissRequest = { viewModel.dialogState.value = DialogState.None },
            content = {
                Card(modifier = Modifier.size(350.dp)) {
                    viewModel.qrDataShare.observeAsState().value?.let {
                        QRCodeImage(it)
                    }
                }
            }
        )
        DialogState.Loading -> Dialog(
            onDismissRequest = {},
            content = {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(MaterialTheme.colors.secondary),
                    contentAlignment = Alignment.Center,
                    content = { LoadingIndicator() }
                )
            }
        )
        DialogState.None -> Unit
    }
}

@Composable
private fun QRCodeImage(data: String) {
    val bitmap = QRCodeWriter()
        .encode(data, BarcodeFormat.QR_CODE, 400, 400)
        .let { MatrixToImageWriter.toBitmap(it) }
        .asImageBitmap()

    Image(
        bitmap = bitmap,
        contentDescription = null
    )
}

private fun userIdToColor(userId: String): Color {
    val bytes = Base64.getDecoder().decode(userId)
    val hex = String.format("%040x", BigInteger(1, bytes))
    val red = Integer.parseInt(hex.substring(0, 2), 16)
    val green = Integer.parseInt(hex.substring(2, 4), 16)
    val blue = Integer.parseInt(hex.substring(4, 6), 16)
    return Color(red, green, blue)
}
