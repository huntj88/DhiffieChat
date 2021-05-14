package me.jameshunt.dhiffiechat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.FileProvider

class ImageAndCameraContract(
    private val fileHelper: FileLocationUtil
) : ActivityResultContract<String, String>() {
    private var friendUserId: String? = null

    override fun createIntent(context: Context, input: String): Intent {
        friendUserId = input

        val fp = "me.jameshunt.dhiffiechat.fileprovider"
        val fpUri: Uri = FileProvider.getUriForFile(context, fp, fileHelper.getInputFile())

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fpUri)

        val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, fpUri)

        val chooserIntent = Intent.createChooser(takePictureIntent, "Capture Image or Video")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(takeVideoIntent))
        return chooserIntent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String {
        return friendUserId!!
    }
}