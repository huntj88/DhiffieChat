package me.jameshunt.privatechat

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import me.jameshunt.privatechat.crypto.toBase64String

class GetUserPublicKey : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(data: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<Unit, ClientPublicKeyQueryParams, Map<String, String>>(
            request = data,
            context = context,
            handle = { _, queryParams, _ ->
                val userPublicKey = getUserPublicKey(queryParams.userId)
                mapOf("publicKey" to userPublicKey.toBase64String())
            }
        )
    }
}

data class ClientPublicKeyQueryParams(val userId: String)