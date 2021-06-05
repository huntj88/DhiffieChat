package me.jameshunt.dhiffiechat.ui.managefriends

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun QRScanner(onScanned: (data: String) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    lateinit var cameraExecutor: ExecutorService
    lateinit var lastCheck: Instant

    fun qrAnalyzer(cameraProviderFuture: ListenableFuture<ProcessCameraProvider>): ImageAnalysis {
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
                        barcodes.firstOrNull()?.rawValue?.let {
                            Log.d("scanned", "result: $it")
                            cameraExecutor.shutdown()
                            cameraProviderFuture.cancel(true)
                            onScanned(it)
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

                val qrAnalyzer = qrAnalyzer(cameraProviderFuture)

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        qrAnalyzer
                    )
                } catch (exc: Exception) {
                    Log.e("Blah", "Use case binding failed", exc)
                }

            }, ContextCompat.getMainExecutor(previewView.context))
            previewView
        },
        modifier = Modifier.fillMaxWidth(0.9f)
    )
}
