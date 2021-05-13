package me.jameshunt.dhiffiechat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.core.content.FileProvider
import java.io.File


class MainActivity : ComponentActivity() {

    private val requestImageCapture = 13423
    private var gotImageCallback: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colors = activeColors()) {
                Navigation(
                    setImageCallback = { gotImage ->
                        gotImageCallback = gotImage
                        dispatchTakePictureIntent()
                    },
                    getPhotoFilePath = {
                        getPhotoFile().absolutePath
                    }
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode == requestImageCapture && resultCode == RESULT_OK) {
            true -> gotImageCallback?.let { it() }
            false -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun dispatchTakePictureIntent() {
        val fpPackage = "me.jameshunt.dhiffiechat.fileprovider"
        val fileProvider: Uri = FileProvider.getUriForFile(this, fpPackage, getPhotoFile())
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)

        startActivityForResult(takePictureIntent, requestImageCapture)
    }

    private fun getPhotoFile(): File {
        val imageDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "DhiffieChat")

        // Create the storage directory if it does not exist
        if (!imageDir.exists() && !imageDir.mkdirs()) {
            Log.d("DhiffieChat", "failed to create directory")
        }

        // Return the file target for the photo based on filename
        return File("${imageDir.path}${File.separator}temp.jpg")
    }

}
