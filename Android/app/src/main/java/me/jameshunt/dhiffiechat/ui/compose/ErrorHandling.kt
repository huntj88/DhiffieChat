package me.jameshunt.dhiffiechat.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.jameshunt.dhiffiechat.DhiffieChatApp
import retrofit2.HttpException
import java.net.UnknownHostException


@Composable
fun ErrorHandlingDialog(e: Throwable) {
    var shouldShowDialog by remember { mutableStateOf(true) }

    if (!shouldShowDialog) {
        return
    }

    Dialog(onDismissRequest = { shouldShowDialog = false }) {
        val message = when (e) {
            is HandledException.EnvironmentDestroyedOrNoInternet -> "Not connected to the internet or the environment has been destroyed"
            is HandledException.DeployingNewCode -> "Upgrading Servers"
            is HandledException.Unauthorized -> "Unauthorized"
            else -> "Something went wrong"
        }

        Card(
            modifier = Modifier.background(DhiffieChatApp.DialogColor),
            content = {
                Text(
                    text = message,
                    color = DhiffieChatApp.DialogTextColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        )
    }
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

fun <T> Flow<T>.withErrorHandling(): Flow<Result<T>> {
    return this
        .map { Result.Success(it) as Result<T> }
        .catch { e ->
            val transformedException = when (e) {
                is UnknownHostException -> HandledException.EnvironmentDestroyedOrNoInternet
                is HttpException -> when (e.code()) {
                    // TODO: distinguish between message bodies for actual forbidden, or aws shenanigans
                    403 -> HandledException.DeployingNewCode
                    401 -> HandledException.Unauthorized
                    else -> e
                }
                else -> e
            }

            this@catch.emit(Result.Failure(transformedException))
        }
}