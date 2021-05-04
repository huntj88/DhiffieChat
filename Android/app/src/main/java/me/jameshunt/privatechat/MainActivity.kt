package me.jameshunt.privatechat

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.coroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import me.jameshunt.privatechat.compose.HomeScreen
import me.jameshunt.privatechat.compose.ManageFriendsScreen
import me.jameshunt.privatechat.crypto.toIv
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.io.File


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DI.setLifecycleComponents(this)
        setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "home") {
                composable("home") { HomeScreen(navController) }
                composable("manageFriends") { ManageFriendsScreen() }
            }
        }
//        setContentView(R.layout.activity_main)
//
//        val userId = DI.identityManager.getIdentity().toUserId()
//        val result = QRCodeWriter().encode(userId, BarcodeFormat.QR_CODE, 400, 400)
//        findViewById<ImageView>(R.id.myQr).setImageBitmap(MatrixToImageWriter.toBitmap(result))

//        lifecycle.coroutineScope.launch {
//            try {
//                DI.privateChatService.testStuff()
//                val relationships = DI.privateChatService.getUserRelationships()
//
//                setContent {
//                    MainUI(
//                        userId = DI.identityManager.getIdentity().toUserId(),
//                        relationships = relationships
//                    )
//                }
//            } catch (e: HttpException) {
//                e.printStackTrace()
//            }
////            dispatchTakePictureIntent()
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode == requestImageCapture && resultCode == RESULT_OK) {
            true -> {
                val takenImage = BitmapFactory.decodeFile(photoFile!!.absolutePath)

                // thumbnail
                // val imageBitmap = data!!.extras!!.get("data") as Bitmap
                sendImage(DI.identityManager.getIdentity().toUserId(), takenImage)
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

                val messages = DI.privateChatService.getMessageSummaries()
                messages.forEach { Log.d("Message", it.toString()) }

                messages.lastOrNull { it.next.fileKey != null }?.let {
                    val download = DI.privateChatService.getFile(
                        senderUserId = it.from,
                        fileKey = it.next.fileKey!!,
                        userUserIv = it.next.iv.toIv()
                    )

//                    findViewById<ImageView>(R.id.myQr).setImageBitmap(download)
                }
            } catch (e: HttpException) {
                e.printStackTrace()
            }
        }
    }

    private val requestImageCapture = 13423
    private var photoFile: File? = null
    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        photoFile = getPhotoFileUri()

        val fileProvider: Uri =
            FileProvider.getUriForFile(this, "me.jameshunt.privatechat.fileprovider", photoFile!!)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
        startActivityForResult(takePictureIntent, requestImageCapture)
    }

    private fun getPhotoFileUri(): File {
        val mediaStorageDir =
            File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "PrivateChat")

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d("PrivateChat", "failed to create directory")
        }

        // Return the file target for the photo based on filename
        return File(mediaStorageDir.path + File.separator.toString() + "temp.jpg")
    }

}
