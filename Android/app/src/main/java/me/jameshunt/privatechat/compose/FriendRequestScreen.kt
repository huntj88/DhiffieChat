package me.jameshunt.privatechat.compose

import LoadingIndicator
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import me.jameshunt.privatechat.DI
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
fun FriendRequestScreen() {
    var isShareOpen by remember { mutableStateOf(false) }
    var isScanOpen by remember { mutableStateOf(false) }
    var isPendingRequestsLoading by remember { mutableStateOf(true) }

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

        if (isPendingRequestsLoading) {
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