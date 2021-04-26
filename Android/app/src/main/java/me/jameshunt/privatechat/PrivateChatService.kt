package me.jameshunt.privatechat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import me.jameshunt.privatechat.PrivateChatApi.*
import me.jameshunt.privatechat.crypto.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.http.*
import java.security.PublicKey
import javax.crypto.spec.IvParameterSpec


class PrivateChatService(private val api: PrivateChatApi, private val authManager: AuthManager) {

    suspend fun getNewMessages(): List<Unit> {
        val message = createIdentity().message
        Log.d("createIdentityMessage", message)
        val userPublicKey = getUserPublicKey(authManager.getIdentity().toHashedIdentity())
        Log.d("user public key", userPublicKey.toHashedIdentity())
        return emptyList()
    }

    suspend fun scanQR(scannedHashedIdentity: String): ResponseMessage {
        return api.scanQR(
            headers = standardHeaders(),
            qr = QR(scannedHashedIdentity = scannedHashedIdentity)
        )
    }

    suspend fun sendFile(recipientHashedIdentity: String, image: ByteArray): ResponseMessage {
        val recipientPublicKey = api.getUserPublicKey(standardHeaders(), recipientHashedIdentity).publicKey.toPublicKey()
        val userToUserCredentials = authManager.userToUserMessage(recipientPublicKey)

        val encryptedImage = AESCrypto.encrypt(image, userToUserCredentials.sharedSecret, userToUserCredentials.iv)
        val contentType = "application/octet-stream".toMediaTypeOrNull()

        return api.sendFile(
            headers = standardHeaders(),
            encryptedFile = encryptedImage.toRequestBody(contentType, 0, encryptedImage.size),
            recipientHashedIdentity = recipientHashedIdentity,
            iv = userToUserCredentials.iv.toBase64String()
        )
    }

    suspend fun getFile(senderHashedId: String, fileKey: String, userUserIv: IvParameterSpec): Bitmap {
        val otherUserPublicKey = api.getUserPublicKey(standardHeaders(), senderHashedId).publicKey.toPublicKey()
        val userToUserCredentials = authManager.userToUserMessage(otherUserPublicKey)

        val encryptedBody = api.getFile(standardHeaders(), fileKey).bytes()
        val file = AESCrypto.decrypt(encryptedBody, userToUserCredentials.sharedSecret, userUserIv)
        return BitmapFactory.decodeByteArray(file, 0, file.size)
    }

    private suspend fun createIdentity(): ResponseMessage {
        val userToServerCredentials = authManager.userToServerAuth(serverPublicKey = getServerPublicKey())

        return api.createIdentity(
            CreateIdentity(
                publicKey = authManager.getIdentity().public.toBase64String(),
                iv = userToServerCredentials.iv.toBase64String(),
                encryptedToken = userToServerCredentials.encryptedToken
            )
        )
    }

    // TODO: caching
    private suspend fun getServerPublicKey(): PublicKey {
        return api.getServerPublicKey().publicKey.toPublicKey()
    }

    private suspend fun getUserPublicKey(hashedIdentity: String): PublicKey {
        return api.getUserPublicKey(headers = standardHeaders(),hashedIdentity = hashedIdentity).publicKey.toPublicKey()
    }

    private suspend fun standardHeaders(vararg additionalHeaders: Map<String, String>): Map<String, String> {
        val standard = mapOf("HashedIdentity" to authManager.getIdentity().toHashedIdentity()) + userToServerHeaders()
        return additionalHeaders.fold(standard) { acc, next -> acc + next }
    }

    private suspend fun userToServerHeaders(): Map<String, String> {
        val userToServerCredentials = authManager.userToServerAuth(getServerPublicKey())
        return mapOf(
            "userServerIv" to userToServerCredentials.iv.toBase64String(),
            "userServerEncryptedToken" to userToServerCredentials.encryptedToken
        )
    }
}

interface PrivateChatApi {
    data class PublicKeyResponse(val publicKey: String)

    @GET("ServerPublicKey")
    suspend fun getServerPublicKey(): PublicKeyResponse

    @GET("UserPublicKey")
    suspend fun getUserPublicKey(
        @HeaderMap headers: Map<String, String>,
        @Query("HashedIdentity") hashedIdentity: String
    ): PublicKeyResponse

    data class CreateIdentity(
        val publicKey: String,
        val iv: String,
        val encryptedToken: String
    )

    data class ResponseMessage(val message: String)

    @POST("CreateIdentity")
    suspend fun createIdentity(@Body identity: CreateIdentity): ResponseMessage

    data class QR(val scannedHashedIdentity: String)

    @POST("ScanQR")
    suspend fun scanQR(@HeaderMap headers: Map<String, String>, @Body qr: QR): ResponseMessage

    @POST("SendFile")
    suspend fun sendFile(
        @HeaderMap headers: Map<String, String>,
        @Body encryptedFile: RequestBody,
        @Query("HashedIdentity") recipientHashedIdentity: String,
        @Query("userUserIv") iv: String
    ): ResponseMessage

    @GET("GetFile")
    suspend fun getFile(
        @HeaderMap headers: Map<String, String>,
        @Query("FileKey") fileKey: String
    ): ResponseBody
}