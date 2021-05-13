package me.jameshunt.dhiffiechat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.core.content.FileProvider

class MainActivity : ComponentActivity() {

    private val fileHelper: FileLocationUtil
        get() = FileLocationUtil(this)

    private val requestImageCapture = 13423
    private var gotInputFileCallback: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colors = activeColors()) {
                Navigation(
                    setImageCallback = { gotInputFile ->
                        gotInputFileCallback = gotInputFile
                        dispatchTakePictureIntent()
                    },
                    getInputFilePath = { fileHelper.getInputFile() }
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode == requestImageCapture && resultCode == RESULT_OK) {
            true -> gotInputFileCallback?.let { it() }
            false -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun dispatchTakePictureIntent() {
        val fp = "me.jameshunt.dhiffiechat.fileprovider"
        val fpUri: Uri = FileProvider.getUriForFile(this, fp, fileHelper.getInputFile())

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fpUri)

        val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, fpUri)

        val chooserIntent = Intent.createChooser(takePictureIntent, "Capture Image or Video")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(takeVideoIntent))

        startActivityForResult(chooserIntent, requestImageCapture)
    }
}
