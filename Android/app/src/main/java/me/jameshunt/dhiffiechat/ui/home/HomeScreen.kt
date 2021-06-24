package me.jameshunt.dhiffiechat.ui.home

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.DhiffieChatApp
import me.jameshunt.dhiffiechat.R
import me.jameshunt.dhiffiechat.ui.compose.LoadingIndicator
import me.jameshunt.dhiffiechat.service.UserService
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

enum class DialogState {
    CameraPermission,
    Scan,
    ScanSuccess,
    Share,
    Loading,
    None
}

class HomeViewModel(
    private val applicationScope: CoroutineScope,
    private val userService: UserService,
    moshi: Moshi
) : ViewModel() {

    private val qrAdapter = moshi.adapter(QRData::class.java)

    val alias = userService.getAlias().asLiveData()
    val qrDataShare: LiveData<String?> = alias.map { alias ->
        alias
            ?.let { QRData(it.userId, it.alias) }
            ?.let { qrAdapter.toJson(it) }
    }

    val dialogState = MutableLiveData(DialogState.None)

    private val emitOnRefresh = MutableLiveData(Unit)
    val friendMessageData: LiveData<List<FriendMessageData>> by lazy {
        emitOnRefresh.asFlow().combine(userService.getFriends()) { _, friends ->
            val summaries = userService.getMessageSummaries().associateBy { it.from }
            friends.map { friend ->
                val messageFromUserSummary = summaries[friend.userId]
                FriendMessageData(
                    friendUserId = friend.userId,
                    alias = friend.alias,
                    count = messageFromUserSummary?.count ?: 0,
                    mostRecentAt = messageFromUserSummary?.mostRecentCreatedAt
                )
            }.sortedByDescending { it.mostRecentAt }
        }.asLiveData()
    }

    fun isUserProfileSetup(): Boolean = userService.isUserProfileSetup()

    fun onRefreshData() {
        emitOnRefresh.value = Unit
    }

    fun scanSelected() {
        dialogState.value = DialogState.CameraPermission
    }

    fun shareSelected() {
        dialogState.value = DialogState.Share
    }

    fun addFriend(qrJson: String) {
        dialogState.value = DialogState.Loading
        val (userId, alias) = qrAdapter.fromJson(qrJson)!!
        applicationScope.launch {
            userService.addFriend(userId, alias)
            dialogState.value = DialogState.ScanSuccess
        }
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
                viewModel.onRefreshData()
            }
        }
    })

    if (!isProfileSetup) {
        toUserProfile()
        return
    }

    Scaffold(
        content = {
            Column(
                Modifier
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                val messageSummaries = viewModel.friendMessageData.observeAsState().value

                messageSummaries?.let { summaries ->
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
                } ?: run {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(Modifier.align(Alignment.CenterHorizontally)) {
                        LoadingIndicator()
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

    when (viewModel.dialogState.observeAsState().value!!) {
        DialogState.CameraPermission -> cameraPermissionContract.launch(Manifest.permission.CAMERA)
        DialogState.Scan -> Dialog(
            onDismissRequest = {
                if (viewModel.dialogState.value == DialogState.Scan) {
                    viewModel.dialogState.value = DialogState.None
                }
            },
            content = {
                Card {
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
        DialogState.Share -> Dialog(
            onDismissRequest = { viewModel.dialogState.value = DialogState.None },
            content = {
                viewModel.qrDataShare.observeAsState().value?.let {
                    Card {
                        QRCodeImage(it)
                    }
                }
            }
        )
        DialogState.Loading -> Dialog(
            onDismissRequest = { /*TODO*/ },
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
        contentDescription = null,
        modifier = Modifier.requiredSize(350.dp)
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
