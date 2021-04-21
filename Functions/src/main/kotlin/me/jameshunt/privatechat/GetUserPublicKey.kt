package me.jameshunt.privatechat

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.fasterxml.jackson.annotation.JsonAlias
import java.util.*

class GetUserPublicKey : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(data: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<Unit, ClientPublicKeyQueryParams, Map<String, String>>(
            request = data,
            context = context,
            handle = { _, queryParams, _ ->
                val userPublicKey = getUserPublicKey(queryParams.hashedIdentity)
                mapOf("publicKey" to Base64.getEncoder().encodeToString(userPublicKey.encoded))
            }
        )
    }
}

data class ClientPublicKeyQueryParams(
    @JsonAlias("HashedIdentity", "hashedIdentity")
    val hashedIdentity: String
)