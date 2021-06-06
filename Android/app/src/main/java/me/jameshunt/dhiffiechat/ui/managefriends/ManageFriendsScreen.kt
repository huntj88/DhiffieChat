package me.jameshunt.dhiffiechat.ui.managefriends

import android.Manifest
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.service.LambdaApi.UserRelationships
import me.jameshunt.dhiffiechat.R
import me.jameshunt.dhiffiechat.service.UserService
import me.jameshunt.dhiffiechat.ui.compose.CallToAction
import me.jameshunt.dhiffiechat.ui.compose.LoadingIndicator
import net.glxn.qrgen.android.MatrixToImageWriter

class ManageFriendsViewModel(
    private val userService: UserService,
    private val applicationScope: CoroutineScope,
    moshi: Moshi
) : ViewModel() {
    private val qrAdapter = moshi.adapter(QRData::class.java)
    private val _relationships: MutableLiveData<UserRelationships?> = MutableLiveData(null)

    val relationships: LiveData<UserRelationships?> = _relationships
    val qrDataShare: String = qrAdapter
        .toJson(userService.getAlias()
        !!.let { QRData(it.userId, it.alias) })

    init {
        viewModelScope.launch {
            refresh()
        }
    }

    fun addFriend(qrJson: String, onFinish: () -> Unit) {
        val (userId, alias) = qrAdapter.fromJson(qrJson)!!
        applicationScope.launch {
            userService.addFriend(userId, alias)
            onFinish()
        }
    }

    private suspend fun refresh() {
        _relationships.value = userService.getRelationships()
    }
}

data class QRData(
    val userId: String,
    val alias: String
)

@Composable
fun ManageFriendsScreen(viewModel: ManageFriendsViewModel, toUserProfile: () -> Unit) {
    var isShareOpen by rememberSaveable { mutableStateOf(false) }
    var isScanOpen by rememberSaveable { mutableStateOf(false) }
    var isLoadingAddFriend: Boolean by rememberSaveable { mutableStateOf(false) }

    val cameraPermissionContract = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { permissionGranted ->
            if (permissionGranted) {
                isScanOpen = true
            } else {
                Log.e("Camera", "camera permission denied")
            }
        }
    )

    Scaffold {
        if (isLoadingAddFriend) {
            LoadingIndicator()
        } else {
            Column(
                Modifier
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                CallToAction(
                    text = "Change Shared Profile",
                    drawableId = R.drawable.exo_ic_settings,
                    onClick = { toUserProfile() }
                )
                Spacer(modifier = Modifier.height(8.dp))
                CallToAction(
                    text = "Share QR Profile",
                    drawableId = R.drawable.ic_baseline_qr_code_scanner_24,
                    onClick = {
                        isShareOpen = true
                        Log.d("clicked", "click")
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                CallToAction(
                    text = "Scan QR Profile",
                    drawableId = R.drawable.ic_baseline_person_add_24,
                    onClick = {
                        cameraPermissionContract.launch(Manifest.permission.CAMERA)
                        Log.d("clicked", "click")
                    }
                )

                val relationships = viewModel.relationships.observeAsState().value

                relationships?.let {
                    RequestList("Received Requests", it.receivedRequests) { userId ->
                        cameraPermissionContract.launch(Manifest.permission.CAMERA)
                    }
                    RequestList("Sent Requests", it.sentRequests) {}
                } ?: run {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(Modifier.align(Alignment.CenterHorizontally)) {
                        LoadingIndicator()
                    }
                }
            }
        }
    }

    if (isShareOpen) {
        Dialog(onDismissRequest = { isShareOpen = false }) {
            Card {
                QRCodeImage(viewModel.qrDataShare)
            }
        }
    }

    if (isScanOpen) {
        Dialog(onDismissRequest = { isScanOpen = false }) {
            Card {
                val context = LocalContext.current
                QRScanner { qrData ->
                    isScanOpen = false
                    isLoadingAddFriend = true

                    viewModel.addFriend(
                        qrJson = qrData,
                        onFinish = {
                            isLoadingAddFriend = false
                            Toast.makeText(context, "Other User Scanned", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RequestList(title: String, requestList: List<String>, ifItemClicked: (userId: String) -> Unit) {
    Spacer(modifier = Modifier.height(24.dp))
    Text(text = title, fontSize = 30.sp)
    requestList.forEach { userId ->
        CallToAction(text = userId, drawableId = R.drawable.ic_baseline_qr_code_scanner_24) {
            ifItemClicked(userId)
        }
    }

    if (requestList.isEmpty()) {
        Card(
            elevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(width = 1.5.dp, Color.LightGray)
        ) {
            Text(
                text = "No Results",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun QRCodeImage(data: String) {
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
