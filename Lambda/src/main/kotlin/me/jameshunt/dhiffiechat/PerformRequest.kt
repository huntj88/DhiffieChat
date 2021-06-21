package me.jameshunt.dhiffiechat

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler

class PerformRequest: RequestHandler<Map<String, Any?>, GatewayResponse> {
    /**
     * By making them all go through a single lambda, the lambda doesn't have to do a cold boot
     */
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        val map = request["queryStringParameters"]!! as Map<String, String>

        return when (map["type"] as String) {
            "Init" -> GatewayResponse(body = Singletons.objectMapper.writeValueAsString(mapOf("message" to "success")))
                .also { Singletons.credentials.serverPrivateKey }
            "CreateIdentity" -> CreateIdentity().handleRequest(request, context)
            "ConsumeMessage" -> ConsumeMessage().handleRequest(request, context)
            "ScanQR" -> ScanQR().handleRequest(request, context)
            "SendMessage" -> SendMessage().handleRequest(request, context)
            "GetUserPublicKey" -> GetUserPublicKey().handleRequest(request, context)
            "GetUserRelationships" -> GetUserRelationships().handleRequest(request, context)
            "GetMessageSummaries" -> GetMessageSummaries().handleRequest(request, context)
            else -> TODO()
        }
    }
}