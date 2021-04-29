package me.jameshunt.privatechat.compose

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.jameshunt.privatechat.DI
import me.jameshunt.privatechat.PrivateChatApi
import net.glxn.qrgen.android.MatrixToImageWriter

@Composable
fun MainUI(userId: String, relationships: PrivateChatApi.Relationships) {
    var isShareOpen by remember { mutableStateOf(false) }
    var isScanOpen by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxHeight()
            .padding(8.dp)
    ) {
        CallToActionQR(text = "Share QR") {
            isShareOpen = true
            Log.d("clicked", "click")
        }
        Spacer(modifier = Modifier.height(8.dp))
        CallToActionQR(text = "Scan QR") {
            isScanOpen = true
            Log.d("clicked", "click")
        }

        relationships.friends.forEach {
            Spacer(modifier = Modifier.height(8.dp))
            CallToActionQR(text = it) {
                Log.d("clicked", "click")
            }
        }
        relationships.receivedRequests.forEach {
            Spacer(modifier = Modifier.height(8.dp))
            CallToActionQR(text = it) {
                Log.d("clicked", "click")
            }
        }
        relationships.sentRequests.forEach {
            Spacer(modifier = Modifier.height(8.dp))
            CallToActionQR(text = it) {
                Log.d("clicked", "click")
            }
        }

    }



    if (isShareOpen) {
        Dialog(onDismissRequest = { isShareOpen = false }) {
            Card {
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
            delay(2000)
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
        CircularProgressIndicator(
            modifier = Modifier.size(90.dp, 90.dp).padding(16.dp),
            color = Color.Green,
            strokeWidth = 8.dp
        )
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