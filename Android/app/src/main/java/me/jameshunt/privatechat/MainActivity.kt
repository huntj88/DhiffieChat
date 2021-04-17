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
import kotlinx.coroutines.launch
import net.glxn.qrgen.android.MatrixToImageWriter
import retrofit2.HttpException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DI.setLifecycleComponents(this)

        lifecycle.coroutineScope.launch {
            try {
                DI.privateChatService.getNewMessages()
            } catch (e: HttpException) {
                e.printStackTrace()
            }
        }

//        setContent {
//            MainUI(identity = identityManager.getIdentity().hashedIdentity)
//        }

//        barcodeBlah(identityManager.getIdentity())
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
