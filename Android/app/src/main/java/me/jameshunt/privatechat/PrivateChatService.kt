package me.jameshunt.privatechat

import android.util.Log
import me.jameshunt.privatechat.PrivateChatApi.*
import me.jameshunt.privatechat.crypto.AESCrypto
import me.jameshunt.privatechat.crypto.toBase64String
import me.jameshunt.privatechat.crypto.toHashedIdentity
import me.jameshunt.privatechat.crypto.toPublicKey
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.*
import java.security.PublicKey


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

    suspend fun sendFile(otherUserHashedId: String, image: ByteArray): ResponseMessage {
        val otherUserPublicKey = api.getUserPublicKey(standardHeaders(), otherUserHashedId).publicKey.toPublicKey()
        val userToUserCredentials = authManager.userToUserMessage(otherUserPublicKey)

        val encryptedImage = AESCrypto.encrypt(image, userToUserCredentials.sharedSecret, userToUserCredentials.iv)
        val contentType = "application/octet-stream".toMediaTypeOrNull()

        return api.sendFile(
            headers = standardHeaders(),
            encryptedFile = encryptedImage.toRequestBody(contentType, 0, encryptedImage.size),
            iv = userToUserCredentials.iv.toBase64String()
        )
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

//    private suspend fun userToUserHeaders(): Map<String, String> {
//        val userToServerCredentials = authManager.userToServerAuth(getServerPublicKey())
//        return mapOf(
//            "userUserIv" to Base64.getEncoder().encodeToString(userToServerCredentials.iv.iv),
//            "userUserEncryptedToken" to userToServerCredentials.encryptedToken
//        )
//    }
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
        @Query("userUserIv") iv: String
    ): ResponseMessage
}