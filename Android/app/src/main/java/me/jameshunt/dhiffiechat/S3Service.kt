package me.jameshunt.dhiffiechat

import kotlinx.coroutines.flow.firstOrNull
import me.jameshunt.dhiffiechat.crypto.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URL
import java.security.PublicKey
import java.time.format.DateTimeFormatter
import kotlin.coroutines.suspendCoroutine

class S3Service(
    private val okHttpClient: OkHttpClient,
    private val networkHelper: NetworkHelper,
    private val authManager: AuthManager,
    private val api: DhiffieChatApi,
    private val userService: UserService
) {

    suspend fun getDecryptedFile(message: DhiffieChatApi.Message): ByteArray {
        val otherUserPublicKey = getUserPublicKey(message.from)
        val userToUserCredentials = authManager.userToUserMessage(otherUserPublicKey)

        val timeSent = DateTimeFormatter.ISO_INSTANT.format(message.messageCreatedAt)
        val s3Url = api.getFile(networkHelper.standardHeaders(), message.fileKey, timeSent).s3Url

        val encryptedBody = download(s3Url)
        return AESCrypto.decrypt(
            cipherInput = encryptedBody,
            key = userToUserCredentials.sharedSecret,
            iv = message.iv.toIv()
        )
    }

    suspend fun sendFile(recipientUserId: String, image: ByteArray) {
        val recipientPublicKey = getUserPublicKey(recipientUserId)
        val userToUserCredentials = authManager.userToUserMessage(recipientPublicKey)

        val encryptedImage = AESCrypto.encrypt(
            input = image,
            key = userToUserCredentials.sharedSecret,
            iv = userToUserCredentials.iv
        )

        val response = api.sendFile(
            headers = networkHelper.standardHeaders(),
            recipientUserId = recipientUserId,
            iv = userToUserCredentials.iv.toBase64String(),
            s3Key = encryptedImage.toS3Key()
        )

        upload(encryptedImage, response.uploadUrl)
    }

    private suspend fun getUserPublicKey(userId: String): PublicKey {
        return api.getUserPublicKey(headers = networkHelper.standardHeaders(), userId = userId)
            .publicKey
            .toPublicKey()
            .also { publicKey ->
                val userIdFromPublic = publicKey.toUserId()

                // maintain a list of your friends locally to validate against (MiTM), if not in list then abort.
                val friends = userService.getFriends().firstOrNull()?.map { it.userId }
                val isLocalFriend = friends?.contains(userIdFromPublic) ?: false

                // verify that public key given matches whats expected
                if (userIdFromPublic != userId && isLocalFriend) {
                    throw IllegalStateException("Incorrect public key given for user: $userId")
                }
            }
    }

    private suspend fun upload(byteArray: ByteArray, url: URL) {
        // TODO: probably not the right place for this? Remove metadata from files
        val request = Request.Builder()
            .url(url)
            .put(byteArray.toRequestBody("application/octet-stream".toMediaTypeOrNull()))
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

    private suspend fun download(url: URL): ByteArray {
        val request = Request.Builder().url(url).get().build()

        return suspendCoroutine { continuation ->
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        continuation.resumeWith(Result.success(response.body!!.bytes()))
                    } else {
                        // will crash if file not finished uploading yet
                        TODO()
                    }
                }
            })
        }
    }
}