package me.jameshunt.dhiffiechat.service

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import me.jameshunt.dhiffiechat.crypto.AESCrypto
import me.jameshunt.dhiffiechat.crypto.base64ToByteArray
import me.jameshunt.dhiffiechat.crypto.toBase64String
import me.jameshunt.dhiffiechat.crypto.toS3Key
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL

class MessageService(
    private val okHttpClient: OkHttpClient,
    private val authManager: AuthManager,
    private val api: LambdaApi,
    private val userService: UserService,
    private val fileLocationUtil: FileLocationUtil,
) {

    fun getMessageSummaries(): Single<List<LambdaApi.MessageSummary>> {
        return api.getMessageSummaries()
    }

    fun decryptMessageText(message: LambdaApi.Message): Single<LambdaApi.Message> {
        return userService.getUserPublicKey(userId = message.from)
            .map { otherUserPublicKey -> authManager.userToUserMessage(otherUserPublicKey).sharedSecret }
            .map { sharedSecret ->
                val decryptedText = message.text
                    ?.base64ToByteArray()
                    ?.let { AESCrypto.decrypt(it, sharedSecret) }
                    ?.toString(Charsets.UTF_8)

                message.copy(text = decryptedText)
            }
    }

    fun getDecryptedFile(message: LambdaApi.Message): Single<File> {
        return userService.getUserPublicKey(message.from)
            .map { otherUserPublicKey -> authManager.userToUserMessage(otherUserPublicKey) }
            .observeOn(Schedulers.computation())
            .flatMap { userToUserCredentials ->
                val body = LambdaApi.ConsumeMessage(message.messageCreatedAt)
                api
                    .consumeMessage(body = body)
                    .flatMap { download(it.s3Url) }
                    .map {
                        AESCrypto.decrypt(
                            inputStream = it,
                            output = fileLocationUtil.incomingDecryptedFile(),
                            key = userToUserCredentials.sharedSecret
                        )
                    }
            }
            .map { fileLocationUtil.incomingDecryptedFile() }
    }

    fun sendMessage(
        recipientUserId: String,
        text: String?,
        file: File,
        mediaType: MediaType
    ): Single<Unit> {
        // TODO: Remove metadata from files
        return userService.getUserPublicKey(recipientUserId)
            .map { recipientPublicKey -> authManager.userToUserMessage(recipientPublicKey) }
            .observeOn(Schedulers.computation())
            .flatMap { userToUserCredentials ->
                val output = fileLocationUtil.outgoingEncryptedFile()

                AESCrypto.encrypt(
                    file = file,
                    output = output,
                    key = userToUserCredentials.sharedSecret
                )

                val encryptedText = text
                    ?.let {
                        AESCrypto.encrypt(
                            text.toByteArray(),
                            userToUserCredentials.sharedSecret
                        )
                    }
                    ?.toBase64String()

                val body = LambdaApi.SendMessage(
                    recipientUserId = recipientUserId,
                    text = encryptedText,
                    s3Key = output.toS3Key(),
                    mediaType = mediaType
                )

                api.sendMessage(body = body)
                    .flatMap { response -> upload(response.uploadUrl, output) }
                    .doAfterSuccess {
                        file.delete()
                        output.delete()
                    }
            }
    }

    private fun upload(url: URL, file: File): Single<Unit> {
        val request = Request.Builder()
            .url(url)
            .put(file.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
            .build()

        return Single.create<Unit> { continuation ->
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.onError(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        continuation.onSuccess(Unit)
                    } else {
                        continuation.onError(HttpException(response))
                    }
                }
            })
        }.subscribeOn(Schedulers.io())
    }

    private fun download(url: URL): Single<InputStream> {
        val request = Request.Builder().url(url).get().build()

        return Single.create<InputStream> { continuation ->
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.onError(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        continuation.onSuccess(response.body!!.byteStream())
                    } else {
                        continuation.onError(HttpException(response))
                    }
                }
            })
        }.subscribeOn(Schedulers.io())
    }

    class HttpException(val okHttpResponse: Response) : RuntimeException(
        "${okHttpResponse.message} ${okHttpResponse.request.url}"
    )
}

enum class MediaType {
    Image,
    Video
}
