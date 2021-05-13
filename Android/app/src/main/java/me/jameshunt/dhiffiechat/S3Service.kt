package me.jameshunt.dhiffiechat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import me.jameshunt.dhiffiechat.crypto.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
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
    private val userService: UserService,
    private val fileLocationUtil: FileLocationUtil
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

    suspend fun sendFile(recipientUserId: String, file: File) {
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

        val response = api.sendFile(
            headers = networkHelper.standardHeaders(),
            recipientUserId = recipientUserId,
            iv = userToUserCredentials.iv.toBase64String(),
            s3Key = output.toS3Key()
        )

        upload(output, response.uploadUrl)
        file.delete()
        output.delete()
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
                val isValid = isLocalFriend && userId == userIdFromPublic
                if (!isValid) {
                    throw IllegalStateException("Incorrect public key given for user: $userId")
                }
            }
    }

    private suspend fun upload(file: File, url: URL) {
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
