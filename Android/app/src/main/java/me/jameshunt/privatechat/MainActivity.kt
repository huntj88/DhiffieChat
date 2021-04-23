package me.jameshunt.privatechat

//import androidx.activity.compose.setContent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
            delay(1000)
            try {
                DI.privateChatService.getNewMessages()
//                while (true) {
//                    Log.d("scanned", "loop")
//                    val scannedIdentity = qrScanner.getHashedIdentity(this@MainActivity)
//                    DI.privateChatService.scanQR(scannedIdentity)
//                    delay(1000)
//                }
            } catch (e: HttpException) {
                e.printStackTrace()
            }
            dispatchTakePictureIntent()
        }

//        setContent {
//            MainUI(identity = identityManager.getIdentity().hashedIdentity)
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode == requestImageCapture && resultCode == RESULT_OK) {
            true -> {
                val imageBitmap = data!!.extras!!.get("data") as Bitmap
                sendImage(DI.identityManager.getIdentity().toHashedIdentity(), imageBitmap)
            }
            false -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun sendImage(recipientHashedIdentity: String, image: Bitmap) {
        lifecycle.coroutineScope.launch {
            try {
                val stream = ByteArrayOutputStream()
                image.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val byteArray = stream.toByteArray()
                image.recycle()

                DI.privateChatService.sendFile(recipientHashedIdentity, byteArray)
            } catch (e: HttpException) {
                e.printStackTrace()
            }
        }
    }

    private val requestImageCapture = 13423

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(takePictureIntent, requestImageCapture)
    }

}
