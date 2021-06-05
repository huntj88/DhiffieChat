package me.jameshunt.dhiffiechat.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import me.jameshunt.dhiffiechat.crypto.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.*
import java.net.URL
import java.security.PublicKey
import kotlin.coroutines.suspendCoroutine

class S3Service(
    private val okHttpClient: OkHttpClient,
    private val authManager: AuthManager,
    private val api: LambdaApi,
    private val userService: UserService,
    private val fileLocationUtil: FileLocationUtil,
) {

    suspend fun getDecryptedFile(message: LambdaApi.Message): File {
        val otherUserPublicKey = userService.getUserPublicKey(message.from)
        val userToUserCredentials = authManager.userToUserMessage(otherUserPublicKey)

        val body = LambdaApi.ConsumeMessage(message.fileKey, message.messageCreatedAt)
        val s3Url = api.consumeMessage(body = body).s3Url

        withContext(Dispatchers.Default) {
            AESCrypto.decrypt(
                inputStream = download(s3Url),
                output = fileLocationUtil.incomingDecryptedFile(),
                key = userToUserCredentials.sharedSecret
            )
        }

        return fileLocationUtil.incomingDecryptedFile()
    }

    suspend fun sendMessage(recipientUserId: String, text: String?, file: File, mediaType: MediaType) {
        // TODO: Remove metadata from files
        val recipientPublicKey = userService.getUserPublicKey(recipientUserId)
        val userToUserCredentials = authManager.userToUserMessage(recipientPublicKey)

        val output = fileLocationUtil.outgoingEncryptedFile()

        withContext(Dispatchers.Default) {
            AESCrypto.encrypt(
                file = file,
                output = output,
                key = userToUserCredentials.sharedSecret
            )
        }

        val encryptedText = text
            ?.let { AESCrypto.encrypt(text.toByteArray(), userToUserCredentials.sharedSecret) }
            ?.toBase64String()

        val body = LambdaApi.SendMessage(
            recipientUserId = recipientUserId,
            text = encryptedText,
            s3Key = output.toS3Key(),
            mediaType = mediaType
        )
        val response = api.sendMessage(body = body)

        upload(response.uploadUrl, output)
        file.delete()
        output.delete()
    }

    private suspend fun upload(url: URL, file: File) {
        val request = Request.Builder()
            .url(url)
            .put(file.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
            .build()

        suspendCoroutine<Unit> { continuation ->
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        continuation.resumeWith(Result.success(Unit))
                    } else {
                        TODO()
                    }
                }
            })
        }
    }

    private suspend fun download(url: URL): InputStream {
        val request = Request.Builder().url(url).get().build()

        return suspendCoroutine { continuation ->
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        continuation.resumeWith(Result.success(response.body!!.byteStream()))
                    } else {
                        // will crash if file not finished uploading yet
                        TODO()
                    }
                }
            })
        }
    }
}

enum class MediaType {
    Image,
    Video
}
