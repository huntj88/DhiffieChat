package me.jameshunt.dhiffiechat.compose

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
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.DhiffieChatApp
import me.jameshunt.dhiffiechat.S3Service
import retrofit2.HttpException
import java.io.ByteArrayOutputStream

class SendMessageViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val di = DhiffieChatApp.di
        return SendMessageViewModel(di.s3Service) as T
    }
}

class SendMessageViewModel(private val s3Service: S3Service) : ViewModel() {
    fun sendImage(recipientUserId: String, image: Bitmap) {
        viewModelScope.launch {
            try {
                val stream = ByteArrayOutputStream()
                image.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                val byteArray = stream.toByteArray()
                image.recycle()

                s3Service.sendFile(recipientUserId, byteArray)
            } catch (e: HttpException) {
                e.printStackTrace()
                throw e
            }
        }
    }
}

@Composable
fun SendMessage(photoPath: String, recipientUserId: String) {
    val viewModel: SendMessageViewModel = viewModel(factory = SendMessageViewModelFactory())
    val takenImage = BitmapFactory.decodeFile(photoPath)
    var text: String by remember { mutableStateOf("") }

    Column {
        Image(
            bitmap = takenImage.asImageBitmap(),
            contentDescription = ""
        )
        TextField(text, onValueChange = {
            text = it
        })
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(100.dp)
                .padding(16.dp),
            onClick = { viewModel.sendImage(recipientUserId, takenImage) },
            content = { Text(text = "Confirm") }
        )
    }
}