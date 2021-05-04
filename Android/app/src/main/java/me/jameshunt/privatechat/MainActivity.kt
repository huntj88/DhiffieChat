package me.jameshunt.privatechat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.android.parcel.Parcelize
import me.jameshunt.privatechat.compose.HomeScreen
import me.jameshunt.privatechat.compose.LauncherScreen
import me.jameshunt.privatechat.compose.ManageFriendsScreen
import me.jameshunt.privatechat.compose.SendMessage
import java.io.File


class MainActivity : AppCompatActivity() {

    private val requestImageCapture = 13423
    private var photoFile: File? = null
    private var recipientUserId: String? = null
    private var gotImageCallback: (() -> Unit)? = null

    @Parcelize
    data class State(
        val photoPath: String?,
        val recipientUserId: String?
    ) : Parcelable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getParcelable<State>("state")?.let { state ->
            photoFile = state.photoPath?.let { File(it) }
            recipientUserId = state.recipientUserId
        }

        DI.setLifecycleComponents(this)
        setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "launcher") {
                composable("launcher") { LauncherScreen(navController) }
                composable("home") {
                    HomeScreen(navController) { selectedUserId: String, gotImage: () -> Unit ->
                        gotImageCallback = gotImage
                        recipientUserId = selectedUserId
                        dispatchTakePictureIntent()
                    }
                }
                composable("manageFriends") { ManageFriendsScreen() }
                composable("sendMessage") {
                    SendMessage(
                        photoPath = photoFile!!.absolutePath,
                        recipientUserId = recipientUserId!!
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("state", State(photoFile?.absolutePath, recipientUserId))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode == requestImageCapture && resultCode == RESULT_OK) {
            true -> gotImageCallback?.let { it() }
            false -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun dispatchTakePictureIntent() {
        photoFile = getPhotoFileUri()

        val fpPackage = "me.jameshunt.privatechat.fileprovider"
        val fileProvider: Uri = FileProvider.getUriForFile(this, fpPackage, photoFile!!)
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)

        startActivityForResult(takePictureIntent, requestImageCapture)
    }

    private fun getPhotoFileUri(): File {
        val imageDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "PrivateChat")

        // Create the storage directory if it does not exist
        if (!imageDir.exists() && !imageDir.mkdirs()) {
            Log.d("PrivateChat", "failed to create directory")
        }

        // Return the file target for the photo based on filename
        return File(imageDir.path + File.separator.toString() + "temp.jpg")
    }

}
