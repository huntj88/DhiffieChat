package me.jameshunt.dhiffiechat.compose

import LoadingIndicator
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.*
import me.jameshunt.dhiffiechat.DhiffieChatApi.*
import me.jameshunt.dhiffiechat.R
import net.glxn.qrgen.android.MatrixToImageWriter


class ManageFriendsViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManageFriendsViewModel(DI.dhiffieChatService, DI.identityManager) as T
    }
}

class ManageFriendsViewModel(
    private val apiService: DhiffieChatService,
    identityManager: IdentityManager
) : ViewModel() {

    private val _relationships: MutableLiveData<Relationships?> = MutableLiveData(null)
    val relationships: LiveData<Relationships?> = _relationships
    val userId = identityManager.getIdentity().toUserId()

    init {
        viewModelScope.launch {
            refresh()
        }
    }

    fun acceptFriendRequest(otherUserId: String) {
        viewModelScope.launch {
            _relationships.value = null
            apiService.scanQR(otherUserId)
            refresh()
        }
    }

    private suspend fun refresh() {
        _relationships.value = apiService.getUserRelationships()
    }

}

@Composable
fun ManageFriendsScreen() {
    val viewModel: ManageFriendsViewModel = viewModel(factory = ManageFriendsViewModelFactory())
    var isShareOpen by remember { mutableStateOf(false) }
    var isScanOpen by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxHeight()
            .padding(8.dp)
    ) {
        CallToAction(text = "Share QR", drawableId = R.drawable.ic_baseline_qr_code_scanner_24) {
            isShareOpen = true
            Log.d("clicked", "click")
        }
        Spacer(modifier = Modifier.height(8.dp))
        CallToAction(text = "Scan QR", drawableId = R.drawable.ic_baseline_qr_code_scanner_24) {
            isScanOpen = true
            Log.d("clicked", "click")
        }

        val relationships = viewModel.relationships.observeAsState().value

        relationships?.let {
            RequestList("Received Requests", it.receivedRequests) { userId ->
                viewModel.acceptFriendRequest(userId)
            }
            RequestList("Sent Requests", it.sentRequests) {}
        } ?: run {
            Spacer(modifier = Modifier.height(8.dp))
            Box(Modifier.align(Alignment.CenterHorizontally)) {
                LoadingIndicator()
            }
        }
    }

    if (isShareOpen) {
        Dialog(onDismissRequest = { isShareOpen = false }) {
            Card {
                QRCodeImage(viewModel.userId)
            }
        }
    }

    if (isScanOpen) {
        Dialog(onDismissRequest = { isScanOpen = false }) {
            Card {
                QRScannerWithJob {
                    isScanOpen = false
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
fun QRScannerWithJob(onDone: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var scanned by remember { mutableStateOf<String?>(null) }

    val getUserIdOnScan: (String) -> Unit = { userId ->
        coroutineScope.launch {
            DI.dhiffieChatService.scanQR(userId)
            onDone()
        }
    }

    if (scanned == null) {
        QRScanner {
            scanned = it
        }
    }

    if (scanned != null) {
        getUserIdOnScan(scanned!!)
        LoadingIndicator()
    }
}

@Composable
fun QRCodeImage(data: String) {
    val result1 = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, 400, 400)
    Image(
        bitmap = MatrixToImageWriter.toBitmap(result1).asImageBitmap(),
        contentDescription = null,
        modifier = Modifier.requiredSize(350.dp)
    )
}