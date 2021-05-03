package me.jameshunt.privatechat.compose

import LoadingIndicator
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import me.jameshunt.privatechat.DI
import me.jameshunt.privatechat.PrivateChatApi
import me.jameshunt.privatechat.R
import me.jameshunt.privatechat.toUserId
import net.glxn.qrgen.android.MatrixToImageWriter


//@Composable
//fun FriendRequest(userId: String) {
//    val coroutineScope = rememberCoroutineScope()
//
//    Spacer(modifier = Modifier.height(8.dp))
//    CallToAction(text = userId) {
//        coroutineScope.launch {
//            DI.privateChatService.scanQR(userId)
//        }
//        Log.d("clicked", "click")
//    }
//}


@Composable
fun ManageFriendsScreen() {
    var isShareOpen by remember { mutableStateOf(false) }
    var isScanOpen by remember { mutableStateOf(false) }
    var relationships by remember { mutableStateOf<PrivateChatApi.Relationships?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val service = DI.privateChatService

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

        relationships?.let {
            RequestList("Received Requests", it.receivedRequests)
            RequestList("Sent Requests", it.sentRequests)
            RequestList("Friends", it.friends)
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
                // TODO: VIEWMODEL
                val userId = DI.identityManager.getIdentity().toUserId()
                QRCodeImage(userId)
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

    // TODO, jank way to get around lint warnings, there is definitely a better way
    fun load() {
        coroutineScope.launch {
            relationships = service.getUserRelationships()
        }
    }
    load()
}

@Composable
fun RequestList(title: String, requestList: List<String>) {
    Spacer(modifier = Modifier.height(24.dp))
    Text(text = title, fontSize = 30.sp)
    requestList.forEach {
        CallToAction(text = it, drawableId = R.drawable.ic_baseline_qr_code_scanner_24) {

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
                modifier = Modifier.fillMaxWidth().padding(16.dp),
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
            DI.privateChatService.scanQR(userId)
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