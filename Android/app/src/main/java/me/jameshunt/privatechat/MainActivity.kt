package me.jameshunt.privatechat

//import androidx.activity.compose.setContent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.glxn.qrgen.android.MatrixToImageWriter
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.security.KeyPair

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DI.setLifecycleComponents(this)

        lifecycle.coroutineScope.launch {
            try {
                DI.privateChatService.getNewMessages()
                sendImage()
                DI.privateChatService.scanQR("aM1bPmKaaSQiOYlC3z16uWNFIoiLlVHKfPTpafnPqF0=")
            } catch (e: HttpException) {
                e.printStackTrace()
            }
        }

//        setContent {
//            MainUI(identity = identityManager.getIdentity().hashedIdentity)
//        }

//        barcodeBlah(identityManager.getIdentity())
    }

    suspend fun sendImage() {
        val result = QRCodeWriter().encode("james is cool", BarcodeFormat.QR_CODE, 400, 400)

        val bytes = withContext(Dispatchers.Default) {
            ByteArrayOutputStream()
                .apply { MatrixToImageWriter.writeToStream(result, "jpg", this) }
                .toByteArray()
        }

        val sendToSelfHashedIdentity = "d7CUCMPj0cynTB4D/o7gjNd6LZ1seQ3OTs5gDSu/RGo="
        DI.privateChatService.sendFile(sendToSelfHashedIdentity, bytes)
    }

    fun barcodeBlah(keyPair: KeyPair) {
        val result = QRCodeWriter().encode(keyPair.toHashedIdentity(), BarcodeFormat.QR_CODE, 400, 400)
        val image = MatrixToImageWriter.toBitmap(result).let { InputImage.fromBitmap(it, 0) }

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        val client = BarcodeScanning.getClient(options)

        client.process(image).addOnSuccessListener {
            Log.d("before qr", keyPair.toHashedIdentity())
            Log.d("QR SCAN", it.first().rawValue ?: "no raw value")
        }
    }
}
