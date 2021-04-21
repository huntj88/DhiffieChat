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
import me.jameshunt.privatechat.crypto.DHCrypto
import net.glxn.qrgen.android.MatrixToImageWriter
import retrofit2.HttpException
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DI.setLifecycleComponents(this)

        lifecycle.coroutineScope.launch {
            try {
                DI.privateChatService.getNewMessages()
                DI.privateChatService.scanQR("aM1bPmKaaSQiOYlC3z16uWNFIoiLlVHKfPTpafnPqF0=")
//                sendImage()
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

        val randomTestPublicKey = DHCrypto.genDHKeyPair().public
        DI.privateChatService.sendFile(randomTestPublicKey, bytes)
    }

    fun barcodeBlah(identity: Identity) {
        val result = QRCodeWriter().encode(identity.hashedIdentity, BarcodeFormat.QR_CODE, 400, 400)
        val image = MatrixToImageWriter.toBitmap(result).let { InputImage.fromBitmap(it, 0) }

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        val client = BarcodeScanning.getClient(options)

        client.process(image).addOnSuccessListener {
            Log.d("before qr", identity.hashedIdentity)
            Log.d("QR SCAN", it.first().rawValue ?: "no raw value")
        }
    }
}
