package me.jameshunt.privatechat

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.android.synthetic.main.activity_main.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.suspendCoroutine

class QRScanner {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var lastCheck: Instant

    suspend fun getHashedIdentity(mainActivity: MainActivity): String {
        cameraExecutor = Executors.newSingleThreadExecutor()
        lastCheck = Instant.now()

        return suspendCoroutine { continuation ->
            getHashedIdentity(mainActivity) {
                continuation.resumeWith(Result.success(it))
            }
        }
    }

    private fun getHashedIdentity(mainActivity: MainActivity, onResult: (String) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(mainActivity)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(mainActivity.viewFinder.surfaceProvider)
            }

            val qrAnalyzer = qrAnalyzer(cameraProviderFuture, onResult)

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(mainActivity, CameraSelector.DEFAULT_BACK_CAMERA, preview, qrAnalyzer)
            } catch (exc: Exception) {
                Log.e("Blah", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(mainActivity))
    }

    private fun qrAnalyzer(
        cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
        onResult: (String) -> Unit
    ): ImageAnalysis {
        val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        val client = BarcodeScanning.getClient(options)

        val analyzer = object : ImageAnalysis.Analyzer {
            @SuppressLint("UnsafeExperimentalUsageError")
            override fun analyze(image: ImageProxy) {
                val mediaImage = image.image
                val canCheckAgain = lastCheck > Instant.now().minus(2, ChronoUnit.SECONDS)
                if (!canCheckAgain || mediaImage == null) {
                    image.close()
                    return
                }

                Log.d("scanned", DateTimeFormatter.ISO_INSTANT.format(lastCheck))
                lastCheck = Instant.now()

                client
                    .process(InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees))
                    .addOnSuccessListener { barcodes ->
                        barcodes.firstOrNull()?.rawValue?.let { hashedIdentity ->
                            Log.d("scanned", hashedIdentity)
                            onResult(hashedIdentity)
                            cameraExecutor.shutdown()
                            cameraProviderFuture.cancel(true)
                        }
                    }
                    .addOnFailureListener { it.printStackTrace() }
                    .addOnCompleteListener { image.close() }
            }
        }

        return ImageAnalysis.Builder()
            .build()
            .apply { this.setAnalyzer(cameraExecutor, analyzer) }
    }
}