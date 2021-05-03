package me.jameshunt.privatechat.compose

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.navigate
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import me.jameshunt.privatechat.DI
import me.jameshunt.privatechat.R
import net.glxn.qrgen.android.MatrixToImageWriter

@Composable
fun MainUI(userId: String, navController: NavController) {
    var isShareOpen by remember { mutableStateOf(false) }
    var isScanOpen by remember { mutableStateOf(false) }
    var isMessagesLoading by remember { mutableStateOf(true) }

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

        Spacer(modifier = Modifier.height(8.dp))
        CallToAction(text = "Friend Requests", drawableId = R.drawable.ic_baseline_person_add_24) {
            Log.d("clicked", "click")
            navController.navigate("friendRequests")
        }

        if (isMessagesLoading) {
            Box(Modifier.align(Alignment.CenterHorizontally)) {
                LoadingIndicator()
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

@Composable
fun SecondScreen() {
    Text(text = "Second screen!")
}

@Composable
fun LoadingIndicator() {
    CircularProgressIndicator(
        modifier = Modifier
            .size(90.dp, 90.dp)
            .padding(16.dp),
        color = Color.Green,
        strokeWidth = 8.dp
    )
}