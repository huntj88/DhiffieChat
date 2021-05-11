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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.DhiffieChatApp
import retrofit2.HttpException
import java.io.ByteArrayOutputStream

@Composable
fun SendMessage(photoPath: String, recipientUserId: String) {
    val coroutineScope = rememberCoroutineScope()
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
            onClick = { coroutineScope.sendImage(recipientUserId, takenImage) },
            content = { Text(text = "Confirm") }
        )
    }
}

private fun CoroutineScope.sendImage(recipientUserId: String, image: Bitmap) {
    launch {
        try {
            val stream = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val byteArray = stream.toByteArray()
            image.recycle()

            DhiffieChatApp.di.dhiffieChatService.sendFile(recipientUserId, byteArray)
        } catch (e: HttpException) {
            e.printStackTrace()
        }
    }
}