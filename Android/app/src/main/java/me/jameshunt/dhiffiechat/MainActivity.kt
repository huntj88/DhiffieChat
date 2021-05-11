package me.jameshunt.dhiffiechat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import me.jameshunt.dhiffiechat.compose.*
import java.io.File


class MainActivity : AppCompatActivity() {

    private val requestImageCapture = 13423
    private var gotImageCallback: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DhiffieChatApp.di.setLifecycleComponents(this)
        setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "launcher") {
                composable("launcher") { LauncherScreen(navController) }
                composable("home") {
                    HomeScreen(navController) { gotImage: () -> Unit ->
                        gotImageCallback = gotImage
                        dispatchTakePictureIntent()
                    }
                }
                composable("manageFriends") { ManageFriendsScreen() }
                composable(
                    route = "sendMessage/{toUserId}",
                    arguments = listOf(navArgument("toUserId") { type = NavType.StringType }),
                    content = {
                        SendMessage(
                            navController = navController,
                            photoPath = getPhotoFile().absolutePath,
                            recipientUserId = it.arguments!!.getString("toUserId")!!
                        )
                    })
                composable(
                    route = "showNextMessage/{fromUserId}",
                    arguments = listOf(navArgument("fromUserId") { type = NavType.StringType }),
                    content = {
                        val userId = it.arguments!!.getString("fromUserId")!!
                        ShowNextMessageScreen(userId)
                    })
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

fun NavController.navigateToSendMessage(friendUserId: String) {
    this.navigate("sendMessage/$friendUserId")
}

fun NavController.navigateToShowNextMessage(friendUserId: String) {
    this.navigate("showNextMessage/$friendUserId")
}
