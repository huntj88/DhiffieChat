package me.jameshunt.privatechat

import android.util.Log
import me.jameshunt.privatechat.PrivateChatApi.*
import me.jameshunt.privatechat.crypto.AESCrypto
import me.jameshunt.privatechat.crypto.toPublicKey
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.security.PublicKey
import java.util.*


class PrivateChatService(private val api: PrivateChatApi, private val authManager: AuthManager) {

    suspend fun getNewMessages(): List<Unit> {
        val message = createIdentity().message
        Log.d("createIdentityMessage", message)
        return emptyList()
    }

    suspend fun scanQR(scannedHashedIdentity: String): ResponseMessage {
        val userToServerCredentials = authManager.userToOtherAuth(getServerPublicKey())
        val qr = QR(
            scannedHashedIdentity = scannedHashedIdentity,
            selfHashedIdentity = authManager.getIdentity().hashedIdentity,
            iv = Base64.getEncoder().encodeToString(userToServerCredentials.iv.iv),
            encryptedToken = userToServerCredentials.encryptedToken
        )

        return api.scanQR(qr)
    }

    suspend fun sendImage(otherUserPublicKey: PublicKey, image: ByteArray) {
        val userToServerCredentials = authManager.userToOtherAuth(getServerPublicKey())
        val userToUserCredentials = authManager.userToOtherAuth(otherUserPublicKey)

        val encryptedImage = AESCrypto.encrypt(image, userToUserCredentials.sharedSecret, userToUserCredentials.iv)

    }

    private suspend fun createIdentity(): ResponseMessage {
        val encoder = Base64.getEncoder()
        val userToServerCredentials = authManager.userToOtherAuth(serverPublicKey = getServerPublicKey())

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
}

interface PrivateChatApi {
    data class ServerPublicKey(val publicKey: String)

    @GET("ServerPublicKey")
    suspend fun getServerPublicKey(): ServerPublicKey

    data class CreateIdentity(
        val publicKey: String,
        val iv: String,
        val encryptedToken: String
    )

    data class ResponseMessage(val message: String)

    @POST("CreateIdentity")
    suspend fun createIdentity(@Body identity: CreateIdentity): ResponseMessage

    data class QR(
        val selfHashedIdentity: String,
        val scannedHashedIdentity: String,
        val iv: String,
        val encryptedToken: String
    )

    @POST("ScanQR")
    suspend fun scanQR(@Body qr: QR): ResponseMessage

//    @POST("SendImage")
//    suspend fun sendImage(@Body ): ResponseMessage
}