package me.jameshunt.dhiffiechat.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.S3Service
import retrofit2.HttpException
import java.io.File

class SendMessageViewModel(private val s3Service: S3Service) : ViewModel() {
    fun sendFile(recipientUserId: String, file: File, text: String, onFinish: () -> Unit) {
        // TODO: use text

        viewModelScope.launch {
            try {
                s3Service.sendFile(recipientUserId, file)
                onFinish()
            } catch (e: HttpException) {
                e.printStackTrace()
                throw e
            }
        }
    }
}

@Composable
fun SendMessage(navController: NavController, file: File, recipientUserId: String) {
    val viewModel: SendMessageViewModel = injectedViewModel()
    var text: String by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    Scaffold {
        if (isUploading) {
            LoadingIndicator()
        } else {
            Column {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                        viewModel.sendFile(
                            recipientUserId = recipientUserId,
                            file = file,
                            text = text,
                            onFinish = { navController.popBackStack() }
                        )
                    },
                    content = { Text(text = "Confirm") }
                )
            }
        }
    }
}