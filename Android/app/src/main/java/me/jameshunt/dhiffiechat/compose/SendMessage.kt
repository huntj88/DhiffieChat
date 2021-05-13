package me.jameshunt.dhiffiechat.compose

import LoadingIndicator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.InjectableViewModelFactory
import me.jameshunt.dhiffiechat.S3Service
import retrofit2.HttpException
import java.io.ByteArrayOutputStream

class SendMessageViewModel(private val s3Service: S3Service) : ViewModel() {
    fun sendImage(recipientUserId: String, image: Bitmap, text: String, onFinish: () -> Unit) {
        // TODO: use text

        viewModelScope.launch {
            try {
                val stream = ByteArrayOutputStream()
                image.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                val byteArray = stream.toByteArray()
                image.recycle()

                s3Service.sendFile(recipientUserId, byteArray)
                onFinish()
            } catch (e: HttpException) {
                e.printStackTrace()
                throw e
            }
        }
    }
}

@Composable
fun SendMessage(navController: NavController, photoPath: String, recipientUserId: String) {
    val viewModel: SendMessageViewModel = viewModel(factory = InjectableViewModelFactory())
    val takenImage = BitmapFactory.decodeFile(photoPath)
    var text: String by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    Column {
        Image(
            bitmap = takenImage.asImageBitmap(),
            contentDescription = "",
        )
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = {
                Text(text = "Message")
            }
        )
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(100.dp)
                .padding(16.dp),
            onClick = {
                isUploading = true
                viewModel.sendImage(
                    recipientUserId = recipientUserId,
                    image = takenImage,
                    text = text,
                    onFinish = { navController.popBackStack() }
                )
            },
            content = { Text(text = "Confirm") }
        )
    }

    if (isUploading) {
        LoadingIndicator()
    }
}