package me.jameshunt.dhiffiechat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import me.jameshunt.dhiffiechat.crypto.*
import retrofit2.http.*
import java.net.URL
import java.security.PublicKey
import java.time.Instant

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

    data class SendFileResponse(val uploadUrl: URL)

    @POST("SendFile")
    suspend fun sendFile(
        @HeaderMap headers: Map<String, String>,
        @Query("userId") recipientUserId: String,
        @Query("s3Key") s3Key: String,
        @Query("userUserIv") iv: String,
        @Query("mediaType") mediaType: MediaType
    ): SendFileResponse

    data class GetFileResponse(val s3Url: URL)

    @GET("GetFile")
    suspend fun getFile(
        @HeaderMap headers: Map<String, String>,
        @Query("fileKey") fileKey: String,
        @Query("timeSent") timeSent: String
    ): GetFileResponse

    data class MessageFromUserSummary(
        val from: String,
        val count: Int,
        val next: Message
    )

    data class Message(
        val to: String,
        val from: String,
        val messageCreatedAt: Instant,
        val text: String?,
        val fileKey: String,
        val iv: String,
        val mediaType: MediaType,
        val signedS3Url: URL?
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

class NetworkHelper(
    private val identityManager: IdentityManager,
    private val authManager: AuthManager
) {
    fun standardHeaders(vararg additionalHeaders: Map<String, String>): Map<String, String> {
        val toUserId = identityManager.getIdentity().toUserId()
        val standard = mapOf("userId" to toUserId) + userToServerHeaders()
        return additionalHeaders.fold(standard) { acc, next -> acc + next }
    }

    private fun userToServerHeaders(): Map<String, String> {
        val userToServerCredentials = authManager.userToServerAuth(getServerPublicKey())
        return mapOf(
            "userServerIv" to userToServerCredentials.iv.toBase64String(),
            "userServerEncryptedToken" to userToServerCredentials.encryptedToken
        )
    }

    fun getServerPublicKey(): PublicKey {
        return serverPublic.toPublicKey()
    }
}

fun ByteArray.toBitmap(): Bitmap = BitmapFactory.decodeByteArray(this, 0, this.size)