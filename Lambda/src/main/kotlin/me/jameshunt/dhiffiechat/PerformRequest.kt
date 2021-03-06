package me.jameshunt.dhiffiechat

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import me.jameshunt.dhiffiechat.ephemeralkeys.GetEphemeralPublicKey
import me.jameshunt.dhiffiechat.ephemeralkeys.RemainingEphemeralReceiveKeys
import me.jameshunt.dhiffiechat.ephemeralkeys.UploadEphemeralReceiveKeys
import me.jameshunt.dhiffiechat.message.ConsumeMessage
import me.jameshunt.dhiffiechat.message.GetMessageSummaries
import me.jameshunt.dhiffiechat.message.SendMessage
import me.jameshunt.dhiffiechat.user.CreateIdentity
import me.jameshunt.dhiffiechat.user.GetUserPublicKey
import me.jameshunt.dhiffiechat.user.GetUserRelationships
import me.jameshunt.dhiffiechat.user.ScanQR

class PerformRequest: RequestHandler<Map<String, Any?>, GatewayResponse> {
    /**
     * By making them all go through a single lambda, the lambda doesn't have to do a cold boot
     */
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        val map = request["queryStringParameters"]!! as Map<String, String>

        return when (map["type"] as String) {
            "Init" -> GatewayResponse(body = Singletons.objectMapper.writeValueAsString(mapOf("message" to "success")))
            "CreateIdentity" -> CreateIdentity().handleRequest(request, context)
            "ConsumeMessage" -> ConsumeMessage().handleRequest(request, context)
            "ScanQR" -> ScanQR().handleRequest(request, context)
            "SendMessage" -> SendMessage().handleRequest(request, context)
            "GetUserPublicKey" -> GetUserPublicKey().handleRequest(request, context)
            "GetUserRelationships" -> GetUserRelationships().handleRequest(request, context)
            "GetMessageSummaries" -> GetMessageSummaries().handleRequest(request, context)
            "RemainingEphemeralReceiveKeys" -> RemainingEphemeralReceiveKeys().handleRequest(request, context)
            "UploadEphemeralReceiveKeys" -> UploadEphemeralReceiveKeys().handleRequest(request, context)
            "GetEphemeralPublicKey" -> GetEphemeralPublicKey().handleRequest(request, context)
            else -> TODO()
        }
    }
}