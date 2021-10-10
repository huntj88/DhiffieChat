package me.jameshunt.dhiffiechat.user

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import me.jameshunt.dhiffiechat.crypto.toBase64String
import me.jameshunt.dhiffiechat.GatewayResponse
import me.jameshunt.dhiffiechat.awsTransformAuthed
import me.jameshunt.dhiffiechat.getUserPublicKey

class GetUserPublicKey : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(data: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<GetUserPublicKeyRequest, Map<String, String>>(
            request = data,
            context = context,
            handle = { body, _ ->
                val userPublicKey = getUserPublicKey(body.userId)
                mapOf("publicKey" to userPublicKey.toBase64String())
            }
        )
    }
}

data class GetUserPublicKeyRequest(val userId: String)