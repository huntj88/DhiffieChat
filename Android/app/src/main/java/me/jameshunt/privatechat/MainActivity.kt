package me.jameshunt.privatechat

//import androidx.activity.compose.setContent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.jameshunt.privatechat.crypto.toIv
import net.glxn.qrgen.android.MatrixToImageWriter
import retrofit2.HttpException
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private val qrScanner = QRScanner()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DI.setLifecycleComponents(this)
        setContentView(R.layout.activity_main)

        val userId = DI.identityManager.getIdentity().toUserId()
        val result = QRCodeWriter().encode(userId, BarcodeFormat.QR_CODE, 400, 400)
        findViewById<ImageView>(R.id.myQr).setImageBitmap(MatrixToImageWriter.toBitmap(result))

        lifecycle.coroutineScope.launch {
            delay(1000)
            try {
                DI.privateChatService.testStuff()
//                while (true) {
//                    Log.d("scanned", "loop")
//                    val scannedUserId = qrScanner.getUserId(this@MainActivity)
//                    DI.privateChatService.scanQR(scannedUserId)
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
                sendImage(DI.identityManager.getIdentity().toUserId(), imageBitmap)
            }
            false -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun sendImage(recipientUserId: String, image: Bitmap) {
        lifecycle.coroutineScope.launch {
            try {
                val stream = ByteArrayOutputStream()
                image.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                val byteArray = stream.toByteArray()
                image.recycle()

                DI.privateChatService.sendFile(recipientUserId, byteArray)

                Log.d("Relationship", DI.privateChatService.getUserRelationships().toString())

                val messages = DI.privateChatService.getMessages()
                messages.forEach { Log.d("Message", it.toString()) }

                messages.lastOrNull { it.fileKey != null }?.let {
                    val download = DI.privateChatService.getFile(
                        senderUserId = it.from,
                        fileKey = it.fileKey!!,
                        userUserIv = it.iv.toIv()
                    )

                    findViewById<ImageView>(R.id.myQr).setImageBitmap(download)
                }
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
