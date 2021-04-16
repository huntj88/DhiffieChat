package me.jameshunt.privatechat

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import java.util.*

data class GatewayResponse(
    val isBase64Encoded: Boolean = false,
    val statusCode: Int = 200,
    val headers: Map<String, String> = emptyMap(),
    val body: String
)

class GetServerPublicKey : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(data: Map<String, Any?>, context: Context): GatewayResponse {
        val serverPublic = Base64.getEncoder().encodeToString(getServerKeyPair().private.encoded)
        return GatewayResponse(
            body = objectMapper.writeValueAsString(mapOf("publicKey" to serverPublic))
        )
    }
}
