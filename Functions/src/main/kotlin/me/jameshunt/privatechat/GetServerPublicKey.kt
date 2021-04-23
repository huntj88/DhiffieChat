package me.jameshunt.privatechat

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import me.jameshunt.privatechat.crypto.toBase64String

class GetServerPublicKey : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(data: Map<String, Any?>, context: Context): GatewayResponse {
        val body = mapOf("publicKey" to getServerKeyPair().public.toBase64String())
        return GatewayResponse(body = Singletons.objectMapper.writeValueAsString(body))
    }
}
