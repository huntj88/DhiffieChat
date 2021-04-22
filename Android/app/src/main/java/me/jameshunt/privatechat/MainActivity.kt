package me.jameshunt.privatechat

//import androidx.activity.compose.setContent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.glxn.qrgen.android.MatrixToImageWriter
import retrofit2.HttpException
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private val qrScanner = QRScanner()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DI.setLifecycleComponents(this)
        setContentView(R.layout.activity_main)

        val hashedIdentity = DI.identityManager.getIdentity().toHashedIdentity()
        val result = QRCodeWriter().encode(hashedIdentity, BarcodeFormat.QR_CODE, 400, 400)
        findViewById<ImageView>(R.id.myQr).setImageBitmap(MatrixToImageWriter.toBitmap(result))

        lifecycle.coroutineScope.launch {
            try {
                DI.privateChatService.getNewMessages()

                while (true) {
                    Log.d("scanned", "loop")
                    val scannedIdentity = qrScanner.getHashedIdentity(this@MainActivity)
                    DI.privateChatService.scanQR(scannedIdentity)
                    delay(1000)
                }
            } catch (e: HttpException) {
                e.printStackTrace()
            }
        }

//        setContent {
//            MainUI(identity = identityManager.getIdentity().hashedIdentity)
//        }
    }

    suspend fun sendImage() {
        val result = QRCodeWriter().encode("james", BarcodeFormat.QR_CODE, 50, 50)

        val bytes = withContext(Dispatchers.Default) {
            ByteArrayOutputStream()
                .apply { MatrixToImageWriter.writeToStream(result, "jpg", this) }
                .toByteArray()
        }

        val sendToSelfHashedIdentity = "d7CUCMPj0cynTB4D/o7gjNd6LZ1seQ3OTs5gDSu/RGo="
        DI.privateChatService.sendFile(sendToSelfHashedIdentity, bytes)
    }
}
