package me.jameshunt.dhiffiechat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import me.jameshunt.dhiffiechat.DhiffieChatApi.*
import me.jameshunt.dhiffiechat.crypto.*
import retrofit2.http.*
import java.net.URL
import java.security.PublicKey
import javax.crypto.spec.IvParameterSpec


class DhiffieChatService(
    private val api: DhiffieChatApi,
    private val authManager: AuthManager,
    private val identityManager: IdentityManager,
    private val s3Service: S3Service
) {

    suspend fun initialize() {
        val message = createIdentity().message
        Log.d("createIdentityMessage", message)

        // TODO: remove
        val userPublicKey = getUserPublicKey(identityManager.getIdentity().toUserId())
        Log.d("user public key", userPublicKey.toUserId())
    }

    suspend fun scanQR(scannedUserId: String): ResponseMessage {
        return api.scanQR(
            headers = standardHeaders(),
            qr = QR(scannedUserId = scannedUserId)
        )
    }

    suspend fun sendFile(recipientUserId: String, image: ByteArray) {
        val recipientPublicKey =
            api.getUserPublicKey(standardHeaders(), recipientUserId).publicKey.toPublicKey()
        val userToUserCredentials = authManager.userToUserMessage(recipientPublicKey)

        val encryptedImage = AESCrypto.encrypt(image, userToUserCredentials.sharedSecret, userToUserCredentials.iv)

        val response = api.sendFile(
            headers = standardHeaders(),
//            encryptedFile = encryptedImage.toRequestBody(contentType, 0, encryptedImage.size),
            recipientUserId = recipientUserId,
            iv = userToUserCredentials.iv.toBase64String(),
            s3Key = encryptedImage.toS3Key()
        )

        @Suppress("BlockingMethodInNonBlockingContext")
        s3Service.upload(encryptedImage, URL(response.uploadUrl))
    }

    suspend fun getDecryptedFile(senderUserId: String, fileKey: String, userUserIv: IvParameterSpec): ByteArray {
        val otherUserPublicKey = api.getUserPublicKey(standardHeaders(), senderUserId).publicKey.toPublicKey()
        val userToUserCredentials = authManager.userToUserMessage(otherUserPublicKey)

        val s3Url = api.getFile(standardHeaders(), fileKey).s3Url
        @Suppress("BlockingMethodInNonBlockingContext")
        val encryptedBody = s3Service.download(URL(s3Url))

        return AESCrypto.decrypt(encryptedBody, userToUserCredentials.sharedSecret, userUserIv)
    }

    suspend fun getMessageSummaries(): List<MessageFromUserSummary> {
        return api.getMessageSummaries(standardHeaders())
    }

    suspend fun getUserRelationships(): Relationships {
        return api.getUserRelationships(standardHeaders())
    }

    private suspend fun createIdentity(): ResponseMessage {
        val userToServerCredentials = authManager.userToServerAuth(serverPublicKey = getServerPublicKey())

        return api.createIdentity(
            CreateIdentity(
                publicKey = identityManager.getIdentity().public.toBase64String(),
                iv = userToServerCredentials.iv.toBase64String(),
                encryptedToken = userToServerCredentials.encryptedToken
            )
        )
    }

    private fun getServerPublicKey(): PublicKey {
        return serverPublic.toPublicKey()
    }

    private suspend fun getUserPublicKey(userId: String): PublicKey {
        return api.getUserPublicKey(headers = standardHeaders(), userId = userId).publicKey.toPublicKey()
    }

    private fun standardHeaders(vararg additionalHeaders: Map<String, String>): Map<String, String> {
        val standard = mapOf("userId" to identityManager.getIdentity().toUserId()) + userToServerHeaders()
        return additionalHeaders.fold(standard) { acc, next -> acc + next }
    }

    private fun userToServerHeaders(): Map<String, String> {
        val userToServerCredentials = authManager.userToServerAuth(getServerPublicKey())
        return mapOf(
            "userServerIv" to userToServerCredentials.iv.toBase64String(),
            "userServerEncryptedToken" to userToServerCredentials.encryptedToken
        )
    }
}

fun ByteArray.toBitmap(): Bitmap = BitmapFactory.decodeByteArray(this, 0, this.size)

interface DhiffieChatApi {
    data class PublicKeyResponse(val publicKey: String)

    @GET("GetUserPublicKey")
    suspend fun getUserPublicKey(
        @HeaderMap headers: Map<String, String>,
        @Query("userId") userId: String
    ): PublicKeyResponse

    data class CreateIdentity(
        val publicKey: String,
        val iv: String,
        val encryptedToken: String
    )

    data class ResponseMessage(val message: String)

    @POST("CreateIdentity")
    suspend fun createIdentity(@Body identity: CreateIdentity): ResponseMessage

    data class QR(val scannedUserId: String)

    @POST("ScanQR")
    suspend fun scanQR(@HeaderMap headers: Map<String, String>, @Body qr: QR): ResponseMessage

    data class SendFileResponse(val uploadUrl: String)

    @POST("SendFile")
    suspend fun sendFile(
        @HeaderMap headers: Map<String, String>,
        @Query("s3Key") s3Key: String,
        @Query("userId") recipientUserId: String,
        @Query("userUserIv") iv: String
    ): SendFileResponse
    
    data class GetFileResponse(val s3Url: String)

    @GET("GetFile")
    suspend fun getFile(
        @HeaderMap headers: Map<String, String>,
        @Query("fileKey") fileKey: String
    ): GetFileResponse

    data class MessageFromUserSummary(
        val from: String,
        val count: Int,
        val next: Message
    )

    data class Message(
        val to: String,
        val from: String,
        val messageCreatedAt: String,
        val text: String?,
        val fileKey: String,
        val iv: String,
        val authedUrl: String?
    )

    @GET("GetMessageSummaries")
    suspend fun getMessageSummaries(@HeaderMap headers: Map<String, String>): List<MessageFromUserSummary>

    data class Relationships(
        val sentRequests: List<String>,
        val receivedRequests: List<String>,
        val friends: List<String>,
    )

    @GET("GetUserRelationships")
    suspend fun getUserRelationships(@HeaderMap headers: Map<String, String>): Relationships
}
