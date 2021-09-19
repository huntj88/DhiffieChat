package me.jameshunt.dhiffiechat.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import me.jameshunt.dhiffiechat.DhiffieChatApp
import me.jameshunt.dhiffiechat.service.MessageService
import retrofit2.HttpException
import java.net.UnknownHostException


@Composable
fun ErrorHandlingDialog(t: Throwable, onDismiss: () -> Unit = {}) {
    var shouldShowDialog by remember { mutableStateOf(true) }

    if (!shouldShowDialog) {
        return
    }

    val transformedException = when (t) {
        is UnknownHostException -> HandledException.EnvironmentDestroyedOrNoInternet
        is HttpException -> when (t.code()) {
            // TODO: distinguish between message bodies for actual forbidden, or aws shenanigans
            403 -> HandledException.DeployingNewCode
            401 -> HandledException.Unauthorized
            else -> t
        }
        is MessageService.HttpException -> when (t.okHttpResponse.code) {
            401 -> HandledException.Unauthorized
            else -> t
        }
        else -> t
    }

    FirebaseCrashlytics.getInstance().recordException(t)

    AlertDialog(
        modifier = Modifier.background(DhiffieChatApp.DialogColor),
        onDismissRequest = {
            shouldShowDialog = false
            onDismiss()
        },
        title = {
            val message = when (transformedException) {
                is HandledException.EnvironmentDestroyedOrNoInternet -> "Not connected to the internet or the environment has been destroyed"
                is HandledException.DeployingNewCode -> "Upgrading Servers"
                is HandledException.Unauthorized -> "Unauthorized"
                else -> "Something went wrong"
            }

            Text(
                text = message,
                color = DhiffieChatApp.DialogTextColor,
                textAlign = TextAlign.Start,
                fontSize = 20.sp,
                modifier = Modifier.padding(16.dp)
            )
        }, confirmButton = {
            Button(onClick = {
                shouldShowDialog = false
                onDismiss()
            }) {
                Text(text = "Close")
            }
        })
}

sealed class HandledException : Exception() {
    object EnvironmentDestroyedOrNoInternet : HandledException()
    object DeployingNewCode : HandledException()
    object Unauthorized : HandledException()
}

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Failure(val throwable: Throwable) : Result<Nothing>()
}
