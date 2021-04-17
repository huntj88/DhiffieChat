//package me.jameshunt.privatechat
//
//import android.util.Log
//import androidx.compose.foundation.BorderStroke
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.Card
//import androidx.compose.material.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.livedata.observeAsState
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.ColorFilter
//import androidx.compose.ui.graphics.asImageBitmap
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.window.Dialog
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.google.zxing.BarcodeFormat
//import com.google.zxing.qrcode.QRCodeWriter
//import net.glxn.qrgen.android.MatrixToImageWriter
//
//
//@Preview
//@Composable
//fun ComposablePreview() {
//    CallToActionQR("share") {}
//}
//
//class TestViewModel : ViewModel() {
//    private val _isShareOpen = MutableLiveData(false)
//    val isShareOpen: LiveData<Boolean> = _isShareOpen
//
//    fun onShareOpenChange(isOpen: Boolean) {
//        _isShareOpen.value = isOpen
//    }
//}
//
//@Composable
//fun MainUI(testViewModel: TestViewModel = viewModel(), identity: String) {
//    Column(
//        Modifier
//            .fillMaxHeight()
//            .padding(8.dp)
//    ) {
//        CallToActionQR(text = "Share QR") {
//            testViewModel.onShareOpenChange(true)
//            Log.d("clicked", "click")
//        }
//        Spacer(modifier = Modifier.height(8.dp))
//        CallToActionQR(text = "Scan QR") {
//            Log.d("clicked", "click")
//        }
//    }
//
//    if (testViewModel.isShareOpen.observeAsState(initial = false).value) {
//        Dialog(onDismissRequest = { testViewModel.onShareOpenChange(false) }) {
//            Card {
//                QRCodeImage(identity)
//            }
//        }
//    }
//}
//
//@Composable
//fun QRCodeImage(data: String) {
//    val result1 = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, 400, 400)
//    Image(
//        bitmap = MatrixToImageWriter.toBitmap(result1).asImageBitmap(),
//        contentDescription = null,
//        modifier = Modifier.requiredSize(350.dp)
//    )
//}
//
//@Composable
//fun CallToActionQR(text: String, onClick: () -> Unit) {
//    Card(
//        elevation = 2.dp,
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable { onClick() },
//        border = BorderStroke(width = 1.5.dp, Color.LightGray)
//    ) {
//        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
//            Image(
//                painter = painterResource(R.drawable.ic_baseline_qr_code_scanner_24),
//                contentDescription = "QR Scanner",
//                contentScale = ContentScale.Fit,
//                modifier = Modifier.requiredSize(50.dp),
//                colorFilter = ColorFilter.tint(Color.Black)
//            )
//            Spacer(modifier = Modifier.size(8.dp))
//            Text(
//                text = text,
//                textAlign = TextAlign.Center,
//                modifier = Modifier.fillMaxWidth()
//            )
//        }
//    }
//}
