package me.jameshunt.privatechat

import android.util.Log
import me.jameshunt.privatechat.PrivateChatApi.*
import me.jameshunt.privatechat.crypto.toPublicKey
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.*


class PrivateChatService(private val api: PrivateChatApi, private val authManager: AuthManager) {

    suspend fun getNewMessages(): List<Unit> {
        val message = createIdentity().message
        Log.d("createIdentityMessage", message)
        return emptyList()
    }

    private suspend fun createIdentity(): ResponseMessage {
        val encoder = Base64.getEncoder()

        val serverPublicKey = api.getServerPublicKey().publicKey.toPublicKey()
        val clientHeaders = authManager.getAuthHeaders(serverPublicKey = serverPublicKey)

        return api.createIdentity(
            CreateIdentity(
                publicKey = encoder.encodeToString(authManager.getIdentity().publicKey.encoded),
                iv = encoder.encodeToString(clientHeaders.iv.iv),
                encryptedToken = clientHeaders.encryptedToken
            )
        )
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
}