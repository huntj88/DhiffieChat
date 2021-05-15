package me.jameshunt.dhiffiechat

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
    private val api: SingleEndpointApi,
    private val userService: UserService,
    private val fileLocationUtil: FileLocationUtil,
) {

    suspend fun getDecryptedFile(message: RequestType.GetMessageSummaries.Message): File {
        val otherUserPublicKey = getUserPublicKey(message.from)
        val userToUserCredentials = authManager.userToUserMessage(otherUserPublicKey)

        val requestType = RequestType.ConsumeMessage(message.fileKey, message.messageCreatedAt)
        val s3Url = api.consumeMessage(requestType).s3Url

        withContext(Dispatchers.Default) {
            AESCrypto.decrypt(
                inputStream = download(s3Url),
                output = fileLocationUtil.incomingDecryptedFile(),
                key = userToUserCredentials.sharedSecret,
                iv = message.iv
            )
        }

        return fileLocationUtil.incomingDecryptedFile()
    }

    suspend fun sendFile(recipientUserId: String, file: File, mediaType: MediaType) {
        // TODO: Remove metadata from files
        val recipientPublicKey = getUserPublicKey(recipientUserId)
        val userToUserCredentials = authManager.userToUserMessage(recipientPublicKey)

        val output = fileLocationUtil.outgoingEncryptedFile()

        withContext(Dispatchers.Default) {
            AESCrypto.encrypt(
                file = file,
                output = output,
                key = userToUserCredentials.sharedSecret,
                iv = userToUserCredentials.iv
            )
        }

        val response = api.sendMessage(
            RequestType.SendMessage(
                recipientUserId = recipientUserId,
                s3Key = output.toS3Key(),
                userUserIv = userToUserCredentials.iv,
                mediaType = mediaType
            )
        )
        upload(response.uploadUrl, output)
        file.delete()
        output.delete()
    }

    private suspend fun getUserPublicKey(userId: String): PublicKey {
        return api
            .getUserPublicKey(RequestType.GetUserPublicKey(userId = userId))
            .publicKey
            .toPublicKey()
            .also { publicKey ->
                val userIdFromPublic = publicKey.toUserId()

                // maintain a list of your friends locally to validate against (MiTM), if not in list then abort.
                val friends = userService.getFriends().firstOrNull()?.map { it.userId }
                val isLocalFriend = friends?.contains(userIdFromPublic) ?: false

                // verify that public key given matches whats expected
                val isValid = isLocalFriend && userId == userIdFromPublic
                if (!isValid) {
                    throw IllegalStateException("Incorrect public key given for user: $userId")
                }
            }
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