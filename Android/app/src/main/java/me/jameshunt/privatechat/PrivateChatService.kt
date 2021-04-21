package me.jameshunt.privatechat

import android.util.Log
import me.jameshunt.privatechat.PrivateChatApi.*
import me.jameshunt.privatechat.crypto.AESCrypto
import me.jameshunt.privatechat.crypto.toPublicKey
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.*
import java.security.PublicKey
import java.util.*


class PrivateChatService(private val api: PrivateChatApi, private val authManager: AuthManager) {

    suspend fun getNewMessages(): List<Unit> {
        val message = createIdentity().message
        Log.d("createIdentityMessage", message)
        val userPublicKey = getUserPublicKey(authManager.getIdentity().hashedIdentity)
        Log.d("user public key", userPublicKey.encoded.toHashedIdentity())
        return emptyList()
    }

    suspend fun scanQR(scannedHashedIdentity: String): ResponseMessage {
        val qr = QR(scannedHashedIdentity = scannedHashedIdentity)
        return api.scanQR(
            headers = standardHeaders(),
            qr = qr
        )
    }

    suspend fun sendFile(otherUserPublicKey: PublicKey, image: ByteArray): ResponseMessage {
        val userToUserCredentials = authManager.userToUserMessage(otherUserPublicKey)
        val encryptedImage = AESCrypto.encrypt(image, userToUserCredentials.sharedSecret, userToUserCredentials.iv)
        val contentType = "application/octet-stream".toMediaTypeOrNull()
        val body: RequestBody = encryptedImage.toRequestBody(contentType, 0, encryptedImage.size)

        return api.sendFile(
            headers = standardHeaders(),
            encryptedFile = body
        )
    }

    private suspend fun createIdentity(): ResponseMessage {
        val encoder = Base64.getEncoder()
        val userToServerCredentials = authManager.userToServerAuth(serverPublicKey = getServerPublicKey())

        return api.createIdentity(
            CreateIdentity(
                publicKey = encoder.encodeToString(authManager.getIdentity().publicKey.encoded),
                iv = encoder.encodeToString(userToServerCredentials.iv.iv),
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
        val standard = mapOf("HashedIdentity" to authManager.getIdentity().hashedIdentity) + userToServerHeaders()
        return additionalHeaders.fold(standard) { acc, next -> acc + next }
    }

    private suspend fun userToServerHeaders(): Map<String, String> {
        val userToServerCredentials = authManager.userToServerAuth(getServerPublicKey())
        return mapOf(
            "userServerIv" to Base64.getEncoder().encodeToString(userToServerCredentials.iv.iv),
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
    suspend fun sendFile(@HeaderMap headers: Map<String, String>, @Body encryptedFile: RequestBody): ResponseMessage
}