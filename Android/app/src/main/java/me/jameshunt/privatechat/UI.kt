package me.jameshunt.privatechat

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import net.glxn.qrgen.android.MatrixToImageWriter
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@Preview
@Composable
fun ComposablePreview() {
    CallToActionQR("share") {}
}

class TestViewModel : ViewModel() {
    private val _isShareOpen = MutableLiveData(false)
    val isShareOpen: LiveData<Boolean> = _isShareOpen

    fun onShareOpenChange(isOpen: Boolean) {
        _isShareOpen.value = isOpen
    }

    private val _isScanOpen = MutableLiveData(false)
    val isScanOpen: LiveData<Boolean> = _isScanOpen

    fun onScanOpenChange(isOpen: Boolean) {
        _isScanOpen.value = isOpen
    }
}

@Composable
fun MainUI(testViewModel: TestViewModel = viewModel(), userId: String) {
    Column(
        Modifier
            .fillMaxHeight()
            .padding(8.dp)
    ) {
        CallToActionQR(text = "Share QR") {
            testViewModel.onShareOpenChange(true)
            Log.d("clicked", "click")
        }
        Spacer(modifier = Modifier.height(8.dp))
        CallToActionQR(text = "Scan QR") {
            testViewModel.onScanOpenChange(true)
            Log.d("clicked", "click")
        }
    }

    if (testViewModel.isShareOpen.observeAsState(initial = false).value) {
        Dialog(onDismissRequest = { testViewModel.onShareOpenChange(false) }) {
            Card {
                QRCodeImage(userId)
            }
        }
    }

    if (testViewModel.isScanOpen.observeAsState(initial = false).value) {
        Dialog(onDismissRequest = { testViewModel.onScanOpenChange(false) }) {
            Card {
                QRScanner()
            }
        }
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
fun CallToActionQR(text: String, onClick: () -> Unit) {
    Card(
        elevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        border = BorderStroke(width = 1.5.dp, Color.LightGray)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            Image(
                painter = painterResource(R.drawable.ic_baseline_qr_code_scanner_24),
                contentDescription = "QR Scanner",
                contentScale = ContentScale.Fit,
                modifier = Modifier.requiredSize(50.dp),
                colorFilter = ColorFilter.tint(Color.Black)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun QRScanner() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    lateinit var cameraExecutor: ExecutorService
    lateinit var lastCheck: Instant

    fun qrAnalyzer(
        cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
        onResult: (String) -> Unit
    ): ImageAnalysis {
        val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        val client = BarcodeScanning.getClient(options)

        val analyzer = object : ImageAnalysis.Analyzer {
            @SuppressLint("UnsafeOptInUsageError")
            override fun analyze(image: ImageProxy) {
                val shouldSkipCheck = lastCheck > Instant.now().minus(2, ChronoUnit.SECONDS)
                Log.d("scanned", "should skip check: $shouldSkipCheck")

                val mediaImage = image.image
                if (shouldSkipCheck || mediaImage == null) {
                    image.close()
                    return
                }

                lastCheck = Instant.now()
                Log.d("scanned", "checking qr code at: ${DateTimeFormatter.ISO_INSTANT.format(lastCheck)}")

                client
                    .process(InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees))
                    .addOnSuccessListener { barcodes ->
                        barcodes.firstOrNull()?.rawValue?.let { userId ->
                            Log.d("scanned", "result: $userId")
                            onResult(userId)
                            cameraExecutor.shutdown()
                            cameraProviderFuture.cancel(true)

                        }
                    }
                    .addOnCompleteListener { image.close() }
            }
        }

        return ImageAnalysis.Builder()
            .build()
            .apply { this.setAnalyzer(cameraExecutor, analyzer) }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            cameraExecutor = Executors.newSingleThreadExecutor()
            lastCheck = Instant.now()
            cameraProviderFuture.addListener({

                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val preview = androidx.camera.core.Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                val qrAnalyzer = qrAnalyzer(cameraProviderFuture) {
                    Log.d("scanner", "it")
                }

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, qrAnalyzer)
                } catch (exc: Exception) {
                    Log.e("Blah", "Use case binding failed", exc)
                }

            }, ContextCompat.getMainExecutor(previewView.context))
            previewView
        },
        modifier = Modifier.fillMaxWidth(0.9f)
    )
}

